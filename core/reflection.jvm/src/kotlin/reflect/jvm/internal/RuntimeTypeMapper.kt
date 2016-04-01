/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package kotlin.reflect.jvm.internal

import org.jetbrains.kotlin.builtins.DefaultBuiltIns
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.builtins.PrimitiveType
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.load.java.descriptors.JavaConstructorDescriptor
import org.jetbrains.kotlin.load.java.descriptors.JavaMethodDescriptor
import org.jetbrains.kotlin.load.java.descriptors.JavaPropertyDescriptor
import org.jetbrains.kotlin.load.java.sources.JavaSourceElement
import org.jetbrains.kotlin.load.java.structure.reflect.*
import org.jetbrains.kotlin.load.kotlin.JvmPackagePartSource
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.platform.JavaToKotlinClassMap
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.descriptorUtil.classId
import org.jetbrains.kotlin.resolve.jvm.JvmPrimitiveType
import org.jetbrains.kotlin.serialization.ProtoBuf
import org.jetbrains.kotlin.serialization.deserialization.NameResolver
import org.jetbrains.kotlin.serialization.deserialization.TypeTable
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedCallableMemberDescriptor
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedClassDescriptor
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedPropertyDescriptor
import org.jetbrains.kotlin.serialization.jvm.JvmProtoBuf
import org.jetbrains.kotlin.serialization.jvm.JvmProtoBufUtil
import java.lang.reflect.Constructor
import java.lang.reflect.Field
import java.lang.reflect.Member
import java.lang.reflect.Method
import kotlin.reflect.KotlinReflectionInternalError

internal sealed class JvmFunctionSignature {
    abstract fun asString(): String

    class KotlinFunction(val signature: String) : JvmFunctionSignature() {
        val methodName: String get() = signature.substringBefore('(')
        val methodDesc: String get() = signature.substring(signature.indexOf('('))

        override fun asString(): String = signature
    }

    class KotlinConstructor(val signature: String) : JvmFunctionSignature() {
        val constructorDesc: String get() = signature.substring(signature.indexOf('('))

        override fun asString(): String = signature
    }

    class JavaMethod(val method: Method) : JvmFunctionSignature() {
        override fun asString(): String = method.signature
    }

    class JavaConstructor(val constructor: Constructor<*>) : JvmFunctionSignature() {
        override fun asString(): String =
                "<init>" +
                constructor.parameterTypes.joinToString(separator = "", prefix = "(", postfix = ")") { it.desc } +
                "V"
    }

    open class BuiltInFunction(private val signature: String) : JvmFunctionSignature() {
        open fun getMember(container: KDeclarationContainerImpl): Member? = null

        override fun asString(): String = signature

        class Predefined(signature: String, private val member: Member): BuiltInFunction(signature) {
            override fun getMember(container: KDeclarationContainerImpl): Member = member
        }
    }
}

internal sealed class JvmPropertySignature {
    /**
     * Returns the JVM signature of the getter of this property. In case the property doesn't have a getter,
     * constructs the signature of its imaginary default getter. See CallableReference#getSignature for more information
     */
    abstract fun asString(): String

    class KotlinProperty(
            val descriptor: PropertyDescriptor,
            val proto: ProtoBuf.Property,
            val signature: JvmProtoBuf.JvmPropertySignature,
            val nameResolver: NameResolver,
            val typeTable: TypeTable
    ) : JvmPropertySignature() {
        private val string: String

        init {
            if (signature.hasGetter()) {
                string = nameResolver.getString(signature.getter.name) + nameResolver.getString(signature.getter.desc)
            }
            else {
                val (name, desc) =
                        JvmProtoBufUtil.getJvmFieldSignature(proto, nameResolver, typeTable) ?:
                        throw KotlinReflectionInternalError("No field signature for property: $descriptor")
                string = JvmAbi.getterName(name) + getManglingSuffix() + "()" + desc
            }
        }

        private fun getManglingSuffix(): String {
            val containingDeclaration = descriptor.containingDeclaration
            if (descriptor.visibility == Visibilities.INTERNAL && containingDeclaration is DeserializedClassDescriptor) {
                val classProto = containingDeclaration.classProto
                val moduleName =
                        if (classProto.hasExtension(JvmProtoBuf.classModuleName))
                            nameResolver.getString(classProto.getExtension(JvmProtoBuf.classModuleName))
                        else JvmAbi.DEFAULT_MODULE_NAME
                return "$" + JvmAbi.sanitizeAsJavaIdentifier(moduleName)
            }
            if (descriptor.visibility == Visibilities.PRIVATE && containingDeclaration is PackageFragmentDescriptor) {
                val packagePartSource = (descriptor as DeserializedPropertyDescriptor).containerSource
                if (packagePartSource is JvmPackagePartSource && packagePartSource.facadeClassName != null) {
                    return "$" + packagePartSource.simpleName.asString()
                }
            }

            return ""
        }

        override fun asString(): String = string
    }

    class JavaMethodProperty(val getterMethod: Method, val setterMethod: Method?) : JvmPropertySignature() {
        override fun asString(): String = getterMethod.signature
    }

    class JavaField(val field: Field) : JvmPropertySignature() {
        override fun asString(): String =
                JvmAbi.getterName(field.name) +
                "()" +
                field.type.desc
    }
}

private val Method.signature: String
    get() = name +
            parameterTypes.joinToString(separator = "", prefix = "(", postfix = ")") { it.desc } +
            returnType.desc

internal object RuntimeTypeMapper {
    fun mapSignature(possiblySubstitutedFunction: FunctionDescriptor): JvmFunctionSignature {
        // Fake overrides don't have a source element, so we need to take a declaration.
        // TODO: support the case when a fake override overrides several declarations with different signatures
        val function = DescriptorUtils.unwrapFakeOverride(possiblySubstitutedFunction).original

        when (function) {
            is DeserializedCallableMemberDescriptor -> {
                mapIntrinsicFunctionSignature(function)?.let {
                    return it
                }

                val proto = function.proto
                if (proto is ProtoBuf.Function) {
                    JvmProtoBufUtil.getJvmMethodSignature(proto, function.nameResolver, function.typeTable)?.let { signature ->
                        return JvmFunctionSignature.KotlinFunction(signature)
                    }
                }
                if (proto is ProtoBuf.Constructor) {
                    JvmProtoBufUtil.getJvmConstructorSignature(proto, function.nameResolver, function.typeTable)?.let { signature ->
                        return JvmFunctionSignature.KotlinConstructor(signature)
                    }
                }
                // If it's a deserialized function but has no JVM signature, it must be from built-ins
                throw KotlinReflectionInternalError("Reflection on built-in Kotlin types is not yet fully supported. " +
                                                    "No metadata found for $function")
            }
            is JavaMethodDescriptor -> {
                val method = ((function.source as? JavaSourceElement)?.javaElement as? ReflectJavaMethod)?.member ?:
                             throw KotlinReflectionInternalError("Incorrect resolution sequence for Java method $function")

                return JvmFunctionSignature.JavaMethod(method)
            }
            is JavaConstructorDescriptor -> {
                val constructor = ((function.source as? JavaSourceElement)?.javaElement as? ReflectJavaConstructor)?.member ?:
                                  throw KotlinReflectionInternalError("Incorrect resolution sequence for Java constructor $function")

                return JvmFunctionSignature.JavaConstructor(constructor)
            }
            else -> throw KotlinReflectionInternalError("Unknown origin of $function (${function.javaClass})")
        }
    }

    fun mapPropertySignature(possiblyOverriddenProperty: PropertyDescriptor): JvmPropertySignature {
        val property = DescriptorUtils.unwrapFakeOverride(possiblyOverriddenProperty).original
        if (property is DeserializedPropertyDescriptor) {
            val proto = property.proto
            if (!proto.hasExtension(JvmProtoBuf.propertySignature)) {
                // If this property has no JVM signature, it must be from built-ins
                throw KotlinReflectionInternalError("Reflection on built-in Kotlin types is not yet fully supported. " +
                                                    "No metadata found for $property")
            }
            return JvmPropertySignature.KotlinProperty(
                    property, proto, proto.getExtension(JvmProtoBuf.propertySignature), property.nameResolver, property.typeTable
            )
        }
        else if (property is JavaPropertyDescriptor) {
            val element = (property.source as? JavaSourceElement)?.javaElement
            when (element) {
                is ReflectJavaField -> return JvmPropertySignature.JavaField(element.member)
                is ReflectJavaMethod -> return JvmPropertySignature.JavaMethodProperty(
                        element.member,
                        ((property.setter?.source as? JavaSourceElement)?.javaElement as? ReflectJavaMethod)?.member
                )
                else -> throw KotlinReflectionInternalError("Incorrect resolution sequence for Java field $property (source = $element)")
            }
        }
        else throw KotlinReflectionInternalError("Unknown origin of $property (${property.javaClass})")
    }

    private fun mapIntrinsicFunctionSignature(function: FunctionDescriptor): JvmFunctionSignature? {
        val parameters = function.valueParameters

        when (function.name.asString()) {
            "equals" -> if (parameters.size == 1 && KotlinBuiltIns.isNullableAny(parameters.single().type)) {
                return JvmFunctionSignature.BuiltInFunction.Predefined("equals(Ljava/lang/Object;)Z",
                                                                       Any::class.java.getDeclaredMethod("equals", Any::class.java))
            }
            "hashCode" -> if (parameters.isEmpty()) {
                return JvmFunctionSignature.BuiltInFunction.Predefined("hashCode()I",
                                                                       Any::class.java.getDeclaredMethod("hashCode"))
            }
            "toString" -> if (parameters.isEmpty()) {
                return JvmFunctionSignature.BuiltInFunction.Predefined("toString()Ljava/lang/String;",
                                                                       Any::class.java.getDeclaredMethod("toString"))
            }
            // TODO: generalize and support other functions from built-ins
        }

        return null
    }

    fun mapJvmClassToKotlinClassId(klass: Class<*>): ClassId {
        if (klass.isArray) {
            klass.componentType.primitiveType?.let {
                return ClassId(KotlinBuiltIns.BUILT_INS_PACKAGE_FQ_NAME, it.arrayTypeName)
            }
            return ClassId.topLevel(KotlinBuiltIns.FQ_NAMES.array.toSafe())
        }

        klass.primitiveType?.let {
            return ClassId(KotlinBuiltIns.BUILT_INS_PACKAGE_FQ_NAME, it.typeName)
        }

        val classId = klass.classId
        if (!classId.isLocal) {
            JavaToKotlinClassMap.INSTANCE.mapJavaToKotlin(classId.asSingleFqName(), DefaultBuiltIns.Instance)?.let { return it.classId }
        }

        return classId
    }

    private val Class<*>.primitiveType: PrimitiveType?
        get() = if (isPrimitive) JvmPrimitiveType.get(simpleName).primitiveType else null
}
