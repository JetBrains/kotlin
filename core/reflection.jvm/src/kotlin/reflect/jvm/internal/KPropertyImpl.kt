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

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.resolve.DescriptorFactory
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.serialization.jvm.JvmProtoBufUtil
import org.jetbrains.kotlin.types.TypeUtils
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import kotlin.reflect.KFunction
import kotlin.reflect.KMutableProperty
import kotlin.reflect.KProperty
import kotlin.reflect.KotlinReflectionInternalError
import kotlin.reflect.jvm.internal.JvmPropertySignature.*

internal abstract class KPropertyImpl<out R> private constructor(
        override val container: KDeclarationContainerImpl,
        name: String,
        val signature: String,
        descriptorInitialValue: PropertyDescriptor?
) : KProperty<R>, KCallableImpl<R> {
    constructor(container: KDeclarationContainerImpl, name: String, signature: String) : this(
            container, name, signature, null
    )

    constructor(container: KDeclarationContainerImpl, descriptor: PropertyDescriptor) : this(
            container,
            descriptor.name.asString(),
            RuntimeTypeMapper.mapPropertySignature(descriptor).asString(),
            descriptor
    )

    private val javaField_ = ReflectProperties.lazySoft {
        val jvmSignature = RuntimeTypeMapper.mapPropertySignature(descriptor)
        when (jvmSignature) {
            is KotlinProperty -> {
                val descriptor = jvmSignature.descriptor
                JvmProtoBufUtil.getJvmFieldSignature(jvmSignature.proto, jvmSignature.nameResolver, jvmSignature.typeTable)?.let {
                    val owner = if (JvmAbi.isCompanionObjectWithBackingFieldsInOuter(descriptor.containingDeclaration)) {
                        container.jClass.enclosingClass
                    }
                    else descriptor.containingDeclaration.let { containingDeclaration ->
                        if (containingDeclaration is ClassDescriptor) containingDeclaration.toJavaClass()
                        else container.jClass
                    }

                    try {
                        owner?.getDeclaredField(it.name)
                    }
                    catch (e: NoSuchFieldException) {
                        null
                    }
                }
            }
            is JavaField -> jvmSignature.field
            is JavaMethodProperty -> null
        }
    }

    val javaField: Field? get() = javaField_()

    override abstract val getter: Getter<R>

    private val descriptor_ = ReflectProperties.lazySoft<PropertyDescriptor>(descriptorInitialValue) {
        container.findPropertyDescriptor(name, signature)
    }

    override val descriptor: PropertyDescriptor get() = descriptor_()

    override val name: String get() = descriptor.name.asString()

    override val caller: FunctionCaller<*> get() = getter.caller

    override val defaultCaller: FunctionCaller<*>? get() = getter.defaultCaller

    override val isLateinit: Boolean get() = descriptor.isLateInit

    override val isConst: Boolean get() = descriptor.isConst

    override fun equals(other: Any?): Boolean {
        val that = other.asKPropertyImpl() ?: return false
        return container == that.container && name == that.name && signature == that.signature
    }

    override fun hashCode(): Int =
            (container.hashCode() * 31 + name.hashCode()) * 31 + signature.hashCode()

    override fun toString(): String =
            ReflectionObjectRenderer.renderProperty(descriptor)

    abstract class Accessor<out PropertyType, out ReturnType> :
            KCallableImpl<ReturnType>, KProperty.Accessor<PropertyType>, KFunction<ReturnType> {
        abstract override val property: KPropertyImpl<PropertyType>

        abstract override val descriptor: PropertyAccessorDescriptor

        override val container: KDeclarationContainerImpl get() = property.container

        override val defaultCaller: FunctionCaller<*>? get() = null

        override val isInline: Boolean get() = descriptor.isInline
        override val isExternal: Boolean get() = descriptor.isExternal
        override val isOperator: Boolean get() = descriptor.isOperator
        override val isInfix: Boolean get() = descriptor.isInfix
        override val isTailrec: Boolean get() = descriptor.isTailrec
        override val isSuspend: Boolean get() = descriptor.isSuspend
    }

    abstract class Getter<out R> : Accessor<R, R>(), KProperty.Getter<R> {
        override val name: String get() = "<get-${property.name}>"

        override val descriptor: PropertyGetterDescriptor by ReflectProperties.lazySoft {
            // TODO: default getter created this way won't have any source information
            property.descriptor.getter ?: DescriptorFactory.createDefaultGetter(property.descriptor, Annotations.EMPTY)
        }

        override val caller: FunctionCaller<*> by ReflectProperties.lazySoft {
            computeCallerForAccessor(isGetter = true)
        }
    }

    abstract class Setter<R> : Accessor<R, Unit>(), KMutableProperty.Setter<R> {
        override val name: String get() = "<set-${property.name}>"

        override val descriptor: PropertySetterDescriptor by ReflectProperties.lazySoft {
            // TODO: default setter created this way won't have any source information
            property.descriptor.setter ?: DescriptorFactory.createDefaultSetter(property.descriptor, Annotations.EMPTY)
        }

        override val caller: FunctionCaller<*> by ReflectProperties.lazySoft {
            computeCallerForAccessor(isGetter = false)
        }
    }
}


private fun KPropertyImpl.Accessor<*, *>.computeCallerForAccessor(isGetter: Boolean): FunctionCaller<*> {
    fun isInsideClassCompanionObject(): Boolean {
        val possibleCompanionObject = property.descriptor.containingDeclaration
        if (DescriptorUtils.isCompanionObject(possibleCompanionObject) && !DescriptorUtils.isInterface(possibleCompanionObject.containingDeclaration)) {
            return true
        }
        return false
    }

    fun isJvmStaticProperty() =
            property.descriptor.annotations.findAnnotation(JVM_STATIC) != null

    fun isNotNullProperty() =
            !TypeUtils.isNullableType(property.descriptor.type)

    fun computeFieldCaller(field: Field): FunctionCaller<Field> = when {
        isInsideClassCompanionObject() -> {
            val klass = (descriptor.containingDeclaration as ClassDescriptor).toJavaClass()!!
            if (isGetter) FunctionCaller.ClassCompanionFieldGetter(field, klass)
            else FunctionCaller.ClassCompanionFieldSetter(field, klass)
        }
        !Modifier.isStatic(field.modifiers) ->
            if (isGetter) FunctionCaller.InstanceFieldGetter(field)
            else FunctionCaller.InstanceFieldSetter(field, isNotNullProperty())
        isJvmStaticProperty() ->
            if (isGetter) FunctionCaller.JvmStaticInObjectFieldGetter(field)
            else FunctionCaller.JvmStaticInObjectFieldSetter(field, isNotNullProperty())
        else ->
            if (isGetter) FunctionCaller.StaticFieldGetter(field)
            else FunctionCaller.StaticFieldSetter(field, isNotNullProperty())
    }

    val jvmSignature = RuntimeTypeMapper.mapPropertySignature(property.descriptor)
    return when (jvmSignature) {
        is KotlinProperty -> {
            val accessorSignature = jvmSignature.signature.run {
                when {
                    isGetter -> if (hasGetter()) getter else null
                    else -> if (hasSetter()) setter else null
                }
            }

            val accessor = accessorSignature?.let { signature ->
                property.container.findMethodBySignature(
                        jvmSignature.nameResolver.getString(signature.name),
                        jvmSignature.nameResolver.getString(signature.desc),
                        Visibilities.isPrivate(descriptor.visibility)
                )
            }

            when {
                accessor == null -> computeFieldCaller(property.javaField!!)
                !Modifier.isStatic(accessor.modifiers) -> FunctionCaller.InstanceMethod(accessor)
                isJvmStaticProperty() -> FunctionCaller.JvmStaticInObject(accessor)
                else -> FunctionCaller.StaticMethod(accessor)
            }
        }
        is JavaField -> {
            computeFieldCaller(jvmSignature.field)
        }
        is JavaMethodProperty -> {
            val method =
                    if (isGetter) jvmSignature.getterMethod
                    else jvmSignature.setterMethod ?: throw KotlinReflectionInternalError(
                            "No source found for setter of Java method property: ${jvmSignature.getterMethod}"
                    )
            FunctionCaller.InstanceMethod(method)
        }
    }
}
