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

import org.jetbrains.kotlin.builtins.jvm.JavaToKotlinClassMap
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotated
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.descriptors.annotations.isEffectivelyInlineOnly
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.load.kotlin.KotlinJvmBinarySourceElement
import org.jetbrains.kotlin.load.kotlin.header.KotlinClassHeader
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.deserialization.*
import org.jetbrains.kotlin.metadata.jvm.JvmProtoBuf
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmProtoBufUtil
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.protobuf.MessageLite
import org.jetbrains.kotlin.resolve.constants.*
import org.jetbrains.kotlin.resolve.descriptorUtil.annotationClass
import org.jetbrains.kotlin.resolve.descriptorUtil.classId
import org.jetbrains.kotlin.serialization.deserialization.DeserializationContext
import org.jetbrains.kotlin.serialization.deserialization.MemberDeserializer
import kotlin.jvm.internal.FunctionReference
import kotlin.jvm.internal.PropertyReference
import kotlin.reflect.KVisibility
import kotlin.reflect.full.IllegalCallableAccessException
import kotlin.reflect.jvm.internal.calls.createAnnotationInstance
import kotlin.reflect.jvm.internal.components.ReflectAnnotationSource
import kotlin.reflect.jvm.internal.components.ReflectKotlinClass
import kotlin.reflect.jvm.internal.components.RuntimeSourceElementFactory
import kotlin.reflect.jvm.internal.components.tryLoadClass
import kotlin.reflect.jvm.internal.structure.ReflectJavaAnnotation
import kotlin.reflect.jvm.internal.structure.ReflectJavaClass
import kotlin.reflect.jvm.internal.structure.safeClassLoader

internal val JVM_STATIC = FqName("kotlin.jvm.JvmStatic")

internal fun ClassDescriptor.toJavaClass(): Class<*>? {
    val source = source
    return when (source) {
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
            loadClass(javaClass.safeClassLoader, classId, 0)
        }
    }
}

private fun loadClass(classLoader: ClassLoader, kotlinClassId: ClassId, arrayDimensions: Int = 0): Class<*>? {
    val javaClassId = JavaToKotlinClassMap.mapKotlinToJava(kotlinClassId.asSingleFqName().toUnsafe()) ?: kotlinClassId
    // All pseudo-classes like kotlin.String.Companion must be accessible from the current class loader
    return loadClass(classLoader, javaClassId.packageFqName.asString(), javaClassId.relativeClassName.asString(), arrayDimensions)
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

    var fqName = "$packageName.${className.replace('.', '$')}"
    repeat(arrayDimensions) {
        fqName = "[$fqName"
    }

    return classLoader.tryLoadClass(fqName)
}

internal fun Visibility.toKVisibility(): KVisibility? =
    when (this) {
        Visibilities.PUBLIC -> KVisibility.PUBLIC
        Visibilities.PROTECTED -> KVisibility.PROTECTED
        Visibilities.INTERNAL -> KVisibility.INTERNAL
        Visibilities.PRIVATE, Visibilities.PRIVATE_TO_THIS -> KVisibility.PRIVATE
        else -> null
    }

internal fun Annotated.computeAnnotations(): List<Annotation> =
    annotations.mapNotNull {
        val source = it.source
        when (source) {
            is ReflectAnnotationSource -> source.annotation
            is RuntimeSourceElementFactory.RuntimeSourceElement -> (source.javaElement as? ReflectJavaAnnotation)?.annotation
            else -> it.toAnnotationInstance()
        }
    }

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
    is ArrayValue -> value.map { it.toRuntimeValue(classLoader) }.toTypedArray()
    is EnumValue -> {
        val (enumClassId, entryName) = value
        loadClass(classLoader, enumClassId)?.let { enumClass ->
            @Suppress("UNCHECKED_CAST")
            Util.getEnumConstantByName(enumClass as Class<out Enum<*>>, entryName.asString())
        }
    }
    is KClassValue -> {
        val (classId, arrayDimensions) = value
        loadClass(classLoader, classId, arrayDimensions)
    }
    is ErrorValue, is NullValue -> null
    else -> value  // Primitives and strings
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

internal val ReflectKotlinClass.packageModuleName: String?
    get() {
        val header = classHeader
        if (!header.metadataVersion.isCompatible()) return null

        return when (header.kind) {
            KotlinClassHeader.Kind.FILE_FACADE, KotlinClassHeader.Kind.MULTIFILE_CLASS_PART -> {
                // TODO: avoid reading and parsing metadata twice (here and later in KPackageImpl#descriptor)
                val (nameResolver, proto) = JvmProtoBufUtil.readPackageDataFrom(header.data!!, header.strings!!)
                // If no packageModuleName extension is written, the name is assumed to be JvmAbi.DEFAULT_MODULE_NAME
                // (see JvmSerializerExtension.serializePackage)
                proto.getExtensionOrNull(JvmProtoBuf.packageModuleName)?.let(nameResolver::getString) ?: JvmAbi.DEFAULT_MODULE_NAME
            }
            KotlinClassHeader.Kind.MULTIFILE_CLASS -> {
                val partName = header.multifilePartNames.firstOrNull() ?: return null
                ReflectKotlinClass.create(klass.classLoader.loadClass(partName.replace('/', '.')))?.packageModuleName
            }
            else -> null
        }
    }

internal val CallableMemberDescriptor.isPublicInBytecode: Boolean
    get() {
        val visibility = visibility
        return (visibility == Visibilities.PUBLIC || visibility == Visibilities.INTERNAL) && !isEffectivelyInlineOnly()
    }

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
): D? {
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
