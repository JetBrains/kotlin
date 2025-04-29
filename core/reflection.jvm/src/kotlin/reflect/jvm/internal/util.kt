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

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.builtins.PrimitiveType
import org.jetbrains.kotlin.builtins.jvm.JavaToKotlinClassMap
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotated
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.descriptors.impl.DeclarationDescriptorVisitorEmptyBodies
import org.jetbrains.kotlin.descriptors.runtime.components.ReflectAnnotationSource
import org.jetbrains.kotlin.descriptors.runtime.components.ReflectKotlinClass
import org.jetbrains.kotlin.descriptors.runtime.components.RuntimeSourceElementFactory
import org.jetbrains.kotlin.descriptors.runtime.components.tryLoadClass
import org.jetbrains.kotlin.descriptors.runtime.structure.ReflectJavaAnnotation
import org.jetbrains.kotlin.descriptors.runtime.structure.ReflectJavaClass
import org.jetbrains.kotlin.descriptors.runtime.structure.safeClassLoader
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.load.kotlin.KotlinJvmBinarySourceElement
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.deserialization.BinaryVersion
import org.jetbrains.kotlin.metadata.deserialization.NameResolver
import org.jetbrains.kotlin.metadata.deserialization.TypeTable
import org.jetbrains.kotlin.metadata.deserialization.VersionRequirementTable
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.protobuf.MessageLite
import org.jetbrains.kotlin.resolve.constants.*
import org.jetbrains.kotlin.resolve.descriptorUtil.annotationClass
import org.jetbrains.kotlin.resolve.descriptorUtil.classId
import org.jetbrains.kotlin.serialization.deserialization.DeserializationContext
import org.jetbrains.kotlin.serialization.deserialization.MemberDeserializer
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.lang.reflect.Type
import kotlin.jvm.internal.FunctionReference
import kotlin.jvm.internal.PropertyReference
import kotlin.jvm.internal.RepeatableContainer
import kotlin.reflect.KType
import kotlin.reflect.KVisibility
import kotlin.reflect.full.IllegalCallableAccessException
import kotlin.reflect.jvm.internal.calls.createAnnotationInstance

internal val JVM_STATIC = FqName("kotlin.jvm.JvmStatic")

internal fun ClassDescriptor.toJavaClass(): Class<*>? {
    return when (val source = source) {
        is KotlinJvmBinarySourceElement -> {
            (source.binaryClass as ReflectKotlinClass).klass
        }
        is RuntimeSourceElementFactory.RuntimeSourceElement -> {
            (source.javaElement as ReflectJavaClass).element
        }
        else -> {
            // If this is neither a Kotlin class nor a Java class, it's likely either a built-in or some fake class descriptor like the one
            // that's created for java.io.Serializable in JvmBuiltInsSettings
            val classId = classId ?: return null
            javaClass.safeClassLoader.loadClass(classId)
        }
    }
}

internal fun ClassLoader.loadClass(kotlinClassId: ClassId, arrayDimensions: Int = 0): Class<*>? {
    val javaClassId = JavaToKotlinClassMap.mapKotlinToJava(kotlinClassId.asSingleFqName().toUnsafe()) ?: kotlinClassId
    // Pseudo-classes like `kotlin/String.Companion` can be accessible from different class loaders. To ensure that we always use the
    // same class, we always load it from the stdlib's class loader.
    val correctClassLoader =
        if (javaClassId != kotlinClassId) Unit::class.java.safeClassLoader else this
    return loadClass(correctClassLoader, javaClassId.packageFqName.asString(), javaClassId.relativeClassName.asString(), arrayDimensions)
}

private fun loadClass(classLoader: ClassLoader, packageName: String, className: String, arrayDimensions: Int): Class<*>? {
    if (packageName == "kotlin") {
        // See mapBuiltInType() in typeSignatureMapping.kt
        when (className) {
            "Array" -> return Array<Any>::class.java
            "BooleanArray" -> return BooleanArray::class.java
            "ByteArray" -> return ByteArray::class.java
            "CharArray" -> return CharArray::class.java
            "DoubleArray" -> return DoubleArray::class.java
            "FloatArray" -> return FloatArray::class.java
            "IntArray" -> return IntArray::class.java
            "LongArray" -> return LongArray::class.java
            "ShortArray" -> return ShortArray::class.java
        }
    }

    val fqName = buildString {
        if (arrayDimensions > 0) {
            repeat(arrayDimensions) {
                append("[")
            }
            append("L")
        }
        if (packageName.isNotEmpty()) {
            append("$packageName.")
        }
        append(className.replace('.', '$'))
        if (arrayDimensions > 0) {
            append(";")
        }
    }

    return classLoader.tryLoadClass(fqName)
}

internal fun Class<*>.createArrayType(): Class<*> =
    java.lang.reflect.Array.newInstance(this, 0)::class.java

internal fun DescriptorVisibility.toKVisibility(): KVisibility? =
    when (this) {
        DescriptorVisibilities.PUBLIC -> KVisibility.PUBLIC
        DescriptorVisibilities.PROTECTED -> KVisibility.PROTECTED
        DescriptorVisibilities.INTERNAL -> KVisibility.INTERNAL
        DescriptorVisibilities.PRIVATE, DescriptorVisibilities.PRIVATE_TO_THIS -> KVisibility.PRIVATE
        else -> null
    }

internal fun Annotated.computeAnnotations(): List<Annotation> =
    annotations.mapNotNull {
        when (val source = it.source) {
            is ReflectAnnotationSource -> source.annotation
            is RuntimeSourceElementFactory.RuntimeSourceElement -> (source.javaElement as? ReflectJavaAnnotation)?.annotation
            else -> it.toAnnotationInstance()
        }
    }.unwrapRepeatableAnnotations()

fun List<Annotation>.unwrapRepeatableAnnotations(): List<Annotation> =
    if (any { it.annotationClass.java.simpleName == JvmAbi.REPEATABLE_ANNOTATION_CONTAINER_NAME })
        flatMap {
            val klass = it.annotationClass.java
            if (klass.simpleName == JvmAbi.REPEATABLE_ANNOTATION_CONTAINER_NAME &&
                klass.getAnnotation(RepeatableContainer::class.java) != null
            )
                @Suppress("UNCHECKED_CAST")
                (klass.getDeclaredMethod("value").invoke(it) as Array<out Annotation>).asList()
            else
                listOf(it)
        }
    else
        this

private fun AnnotationDescriptor.toAnnotationInstance(): Annotation? {
    @Suppress("UNCHECKED_CAST")
    val annotationClass = annotationClass?.toJavaClass() as? Class<out Annotation> ?: return null

    return createAnnotationInstance(
        annotationClass,
        allValueArguments.entries
            .mapNotNull { (name, value) -> value.toRuntimeValue(annotationClass.classLoader)?.let(name.asString()::to) }
            .toMap()
    )
}

// TODO: consider throwing exceptions such as AnnotationFormatError/AnnotationTypeMismatchException if a value of unexpected type is found
private fun ConstantValue<*>.toRuntimeValue(classLoader: ClassLoader): Any? = when (this) {
    is AnnotationValue -> value.toAnnotationInstance()
    is ArrayValue -> arrayToRuntimeValue(classLoader)
    is EnumValue -> {
        val (enumClassId, entryName) = value
        classLoader.loadClass(enumClassId)?.let { enumClass ->
            @Suppress("UNCHECKED_CAST")
            Util.getEnumConstantByName(enumClass as Class<out Enum<*>>, entryName.asString())
        }
    }
    is KClassValue -> when (val classValue = value) {
        is KClassValue.Value.NormalClass ->
            classLoader.loadClass(classValue.classId, classValue.arrayDimensions)
        is KClassValue.Value.LocalClass -> {
            // TODO: this doesn't work because of KT-30013
            (classValue.type.constructor.declarationDescriptor as? ClassDescriptor)?.toJavaClass()
        }
    }
    is ErrorValue, is NullValue -> null
    else -> value  // Primitives and strings
}

private fun ArrayValue.arrayToRuntimeValue(classLoader: ClassLoader): Any? {
    val type = (this as? TypedArrayValue)?.type ?: return null
    val values = value.map { it.toRuntimeValue(classLoader) }

    return when (KotlinBuiltIns.getPrimitiveArrayElementType(type)) {
        PrimitiveType.BOOLEAN -> BooleanArray(value.size) { values[it] as Boolean }
        PrimitiveType.CHAR -> CharArray(value.size) { values[it] as Char }
        PrimitiveType.BYTE -> ByteArray(value.size) { values[it] as Byte }
        PrimitiveType.SHORT -> ShortArray(value.size) { values[it] as Short }
        PrimitiveType.INT -> IntArray(value.size) { values[it] as Int }
        PrimitiveType.FLOAT -> FloatArray(value.size) { values[it] as Float }
        PrimitiveType.LONG -> LongArray(value.size) { values[it] as Long }
        PrimitiveType.DOUBLE -> DoubleArray(value.size) { values[it] as Double }
        null -> {
            check(KotlinBuiltIns.isArray(type)) { "Not an array type: $type" }
            val argType = type.arguments.single().type
            val classifier = argType.constructor.declarationDescriptor as? ClassDescriptor ?: error("Not a class type: $argType")
            when {
                KotlinBuiltIns.isString(argType) -> Array(value.size) { values[it] as String }
                KotlinBuiltIns.isKClass(classifier) -> Array(value.size) { values[it] as Class<*> }
                else -> {
                    val argClass = classifier.classId?.let(classLoader::loadClass) ?: return null

                    @Suppress("UNCHECKED_CAST")
                    val array = java.lang.reflect.Array.newInstance(argClass, value.size) as Array<in Any?>
                    repeat(values.size) { array[it] = values[it] }
                    array
                }
            }
        }
    }
}

// TODO: wrap other exceptions
internal inline fun <R> reflectionCall(block: () -> R): R =
    try {
        block()
    } catch (e: IllegalAccessException) {
        throw IllegalCallableAccessException(e)
    }

internal fun Any?.asKFunctionImpl(): KFunctionImpl? =
    this as? KFunctionImpl ?: (this as? FunctionReference)?.compute() as? KFunctionImpl

internal fun Any?.asKPropertyImpl(): KPropertyImpl<*>? =
    this as? KPropertyImpl<*> ?: (this as? PropertyReference)?.compute() as? KPropertyImpl

internal fun Any?.asKCallableImpl(): KCallableImpl<*>? =
    this as? KCallableImpl<*> ?: asKFunctionImpl() ?: asKPropertyImpl()

internal val CallableDescriptor.instanceReceiverParameter: ReceiverParameterDescriptor?
    get() =
        if (dispatchReceiverParameter != null) (containingDeclaration as ClassDescriptor).thisAsReceiverParameter
        else null

internal fun <M : MessageLite, D : CallableDescriptor> deserializeToDescriptor(
    moduleAnchor: Class<*>,
    proto: M,
    nameResolver: NameResolver,
    typeTable: TypeTable,
    metadataVersion: BinaryVersion,
    createDescriptor: MemberDeserializer.(M) -> D
): D {
    val moduleData = moduleAnchor.getOrCreateModule()

    val typeParameters = when (proto) {
        is ProtoBuf.Function -> proto.typeParameterList
        is ProtoBuf.Property -> proto.typeParameterList
        else -> error("Unsupported message: $proto")
    }

    val context = DeserializationContext(
        moduleData.deserialization, nameResolver, moduleData.module, typeTable, VersionRequirementTable.EMPTY, metadataVersion,
        containerSource = null, parentTypeDeserializer = null, typeParameters = typeParameters
    )
    return MemberDeserializer(context).createDescriptor(proto)
}

internal val KType.isInlineClassType: Boolean
    get() = (classifier as? KClassImpl<*>)?.isInline == true

internal val KType.needsMultiFieldValueClassFlattening: Boolean
    get() = (classifier as? KClassImpl<*>)?.run { isValue && !isInline } == true

internal fun defaultPrimitiveValue(type: Type): Any? =
    if (type is Class<*> && type.isPrimitive) {
        when (type) {
            Boolean::class.java -> false
            Char::class.java -> 0.toChar()
            Byte::class.java -> 0.toByte()
            Short::class.java -> 0.toShort()
            Int::class.java -> 0
            Float::class.java -> 0f
            Long::class.java -> 0L
            Double::class.java -> 0.0
            Void.TYPE -> throw IllegalStateException("Parameter with void type is illegal")
            else -> throw UnsupportedOperationException("Unknown primitive: $type")
        }
    } else null

internal open class CreateKCallableVisitor(private val container: KDeclarationContainerImpl) :
    DeclarationDescriptorVisitorEmptyBodies<KCallableImpl<*>, Unit>() {
    override fun visitPropertyDescriptor(descriptor: PropertyDescriptor, data: Unit): KCallableImpl<*> {
        val receiverCount =
            if (descriptor.contextReceiverParameters.isNotEmpty())
                -1
            else
                (descriptor.dispatchReceiverParameter?.let { 1 } ?: 0) + (descriptor.extensionReceiverParameter?.let { 1 } ?: 0)

        when {
            descriptor.isVar -> when (receiverCount) {
                -1 -> return KMutablePropertyNImpl<Any?>(container, descriptor)
                0 -> return KMutableProperty0Impl<Any?>(container, descriptor)
                1 -> return KMutableProperty1Impl<Any?, Any?>(container, descriptor)
                2 -> return KMutableProperty2Impl<Any?, Any?, Any?>(container, descriptor)
            }
            else -> when (receiverCount) {
                -1 -> return KPropertyNImpl<Any?>(container, descriptor)
                0 -> return KProperty0Impl<Any?>(container, descriptor)
                1 -> return KProperty1Impl<Any?, Any?>(container, descriptor)
                2 -> return KProperty2Impl<Any?, Any?, Any?>(container, descriptor)
            }
        }

        throw KotlinReflectionInternalError("Unsupported property: $descriptor")
    }

    override fun visitFunctionDescriptor(descriptor: FunctionDescriptor, data: Unit): KCallableImpl<*> =
        KFunctionImpl(container, descriptor)
}

internal fun Class<*>.getDeclaredMethodOrNull(name: String, vararg parameterTypes: Class<*>): Method? =
    try {
        getDeclaredMethod(name, *parameterTypes)
    } catch (e: NoSuchMethodException) {
        null
    }

internal fun Class<*>.getDeclaredFieldOrNull(name: String): Field? =
    try {
        getDeclaredField(name)
    } catch (e: NoSuchFieldException) {
        null
    }
