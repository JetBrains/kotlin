/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.load.kotlin

import org.jetbrains.kotlin.SpecialJvmAnnotations
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.deserialization.*
import org.jetbrains.kotlin.metadata.jvm.JvmProtoBuf
import org.jetbrains.kotlin.metadata.jvm.JvmProtoBuf.propertySignature
import org.jetbrains.kotlin.metadata.jvm.deserialization.ClassMapperLite
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmProtoBufUtil
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.protobuf.MessageLite
import org.jetbrains.kotlin.serialization.deserialization.AnnotatedCallableKind
import org.jetbrains.kotlin.serialization.deserialization.AnnotationLoader
import org.jetbrains.kotlin.serialization.deserialization.ProtoContainer

abstract class AbstractBinaryClassAnnotationLoader<A : Any, S : AbstractBinaryClassAnnotationLoader.AnnotationsContainer<A>>(
    protected val kotlinClassFinder: KotlinClassFinder
) : AnnotationLoader<A> {
    protected abstract fun getAnnotationsContainer(binaryClass: KotlinJvmBinaryClass): S

    protected abstract fun loadAnnotation(
        annotationClassId: ClassId,
        source: SourceElement,
        result: MutableList<A>
    ): KotlinJvmBinaryClass.AnnotationArgumentVisitor?

    protected abstract fun loadTypeAnnotation(proto: ProtoBuf.Annotation, nameResolver: NameResolver): A

    protected fun loadAnnotationIfNotSpecial(
        annotationClassId: ClassId,
        source: SourceElement,
        result: MutableList<A>
    ): KotlinJvmBinaryClass.AnnotationArgumentVisitor? {
        if (annotationClassId in SpecialJvmAnnotations.SPECIAL_ANNOTATIONS) return null

        return loadAnnotation(annotationClassId, source, result)
    }

    private fun ProtoContainer.Class.toBinaryClass(): KotlinJvmBinaryClass? =
        (source as? KotlinJvmBinarySourceElement)?.binaryClass

    protected open fun getCachedFileContent(kotlinClass: KotlinJvmBinaryClass): ByteArray? = null

    override fun loadClassAnnotations(container: ProtoContainer.Class): List<A> {
        val kotlinClass = container.toBinaryClass() ?: error("Class for loading annotations is not found: ${container.debugFqName()}")

        val result = ArrayList<A>(1)

        kotlinClass.loadClassAnnotations(object : KotlinJvmBinaryClass.AnnotationVisitor {
            override fun visitAnnotation(classId: ClassId, source: SourceElement): KotlinJvmBinaryClass.AnnotationArgumentVisitor? {
                return loadAnnotationIfNotSpecial(classId, source, result)
            }

            override fun visitEnd() {
            }
        }, getCachedFileContent(kotlinClass))

        return result
    }

    override fun loadCallableAnnotations(container: ProtoContainer, proto: MessageLite, kind: AnnotatedCallableKind): List<A> {
        if (kind == AnnotatedCallableKind.PROPERTY) {
            return loadPropertyAnnotations(container, proto as ProtoBuf.Property, PropertyRelatedElement.PROPERTY)
        }

        val signature = getCallableSignature(proto, container.nameResolver, container.typeTable, kind) ?: return emptyList()
        return findClassAndLoadMemberAnnotations(container, signature)
    }

    override fun loadPropertyBackingFieldAnnotations(container: ProtoContainer, proto: ProtoBuf.Property): List<A> =
        loadPropertyAnnotations(container, proto, PropertyRelatedElement.BACKING_FIELD)

    override fun loadPropertyDelegateFieldAnnotations(container: ProtoContainer, proto: ProtoBuf.Property): List<A> =
        loadPropertyAnnotations(container, proto, PropertyRelatedElement.DELEGATE_FIELD)

    private enum class PropertyRelatedElement {
        PROPERTY,
        BACKING_FIELD,
        DELEGATE_FIELD,
    }

    private fun loadPropertyAnnotations(container: ProtoContainer, proto: ProtoBuf.Property, element: PropertyRelatedElement): List<A> {
        val isConst = Flags.IS_CONST.get(proto.flags)
        val isMovedFromInterfaceCompanion = JvmProtoBufUtil.isMovedFromInterfaceCompanion(proto)
        if (element == PropertyRelatedElement.PROPERTY) {
            val syntheticFunctionSignature =
                getPropertySignature(proto, container.nameResolver, container.typeTable, synthetic = true) ?: return emptyList()
            return findClassAndLoadMemberAnnotations(
                container, syntheticFunctionSignature, property = true, isConst = isConst,
                isMovedFromInterfaceCompanion = isMovedFromInterfaceCompanion
            )
        }

        val fieldSignature =
            getPropertySignature(proto, container.nameResolver, container.typeTable, field = true) ?: return emptyList()

        // TODO: check delegate presence in some other way
        val isDelegated = JvmAbi.DELEGATED_PROPERTY_NAME_SUFFIX in fieldSignature.signature
        if (isDelegated != (element == PropertyRelatedElement.DELEGATE_FIELD)) return emptyList()

        return findClassAndLoadMemberAnnotations(
            container, fieldSignature, property = true, field = true, isConst = isConst,
            isMovedFromInterfaceCompanion = isMovedFromInterfaceCompanion
        )
    }

    override fun loadEnumEntryAnnotations(container: ProtoContainer, proto: ProtoBuf.EnumEntry): List<A> {
        val signature = MemberSignature.fromFieldNameAndDesc(
            container.nameResolver.getString(proto.name),
            ClassMapperLite.mapClass((container as ProtoContainer.Class).classId.asString())
        )
        return findClassAndLoadMemberAnnotations(container, signature)
    }

    private fun findClassAndLoadMemberAnnotations(
        container: ProtoContainer,
        signature: MemberSignature,
        property: Boolean = false,
        field: Boolean = false,
        isConst: Boolean? = null,
        isMovedFromInterfaceCompanion: Boolean = false
    ): List<A> {
        val kotlinClass =
            findClassWithAnnotationsAndInitializers(
                container,
                getSpecialCaseContainerClass(
                    container,
                    property,
                    field,
                    isConst,
                    isMovedFromInterfaceCompanion
                )
            )
                ?: return listOf()

        return getAnnotationsContainer(kotlinClass).memberAnnotations[signature] ?: listOf()
    }

    override fun loadValueParameterAnnotations(
        container: ProtoContainer,
        callableProto: MessageLite,
        kind: AnnotatedCallableKind,
        parameterIndex: Int,
        proto: ProtoBuf.ValueParameter
    ): List<A> {
        val methodSignature = getCallableSignature(callableProto, container.nameResolver, container.typeTable, kind)
        if (methodSignature != null) {
            val index = parameterIndex + computeJvmParameterIndexShift(container, callableProto)
            val paramSignature = MemberSignature.fromMethodSignatureAndParameterIndex(methodSignature, index)
            return findClassAndLoadMemberAnnotations(container, paramSignature)
        }

        return listOf()
    }

    private fun computeJvmParameterIndexShift(container: ProtoContainer, message: MessageLite): Int {
        return when (message) {
            is ProtoBuf.Function -> if (message.hasReceiver()) 1 else 0
            is ProtoBuf.Property -> if (message.hasReceiver()) 1 else 0
            is ProtoBuf.Constructor -> when {
                (container as ProtoContainer.Class).kind == ProtoBuf.Class.Kind.ENUM_CLASS -> 2
                container.isInner -> 1
                else -> 0
            }
            else -> throw UnsupportedOperationException("Unsupported message: ${message::class.java}")
        }
    }

    override fun loadExtensionReceiverParameterAnnotations(
        container: ProtoContainer,
        proto: MessageLite,
        kind: AnnotatedCallableKind
    ): List<A> {
        val methodSignature = getCallableSignature(proto, container.nameResolver, container.typeTable, kind)
        if (methodSignature != null) {
            val paramSignature = MemberSignature.fromMethodSignatureAndParameterIndex(methodSignature, 0)
            return findClassAndLoadMemberAnnotations(container, paramSignature)
        }

        return emptyList()
    }

    override fun loadTypeAnnotations(proto: ProtoBuf.Type, nameResolver: NameResolver): List<A> {
        return proto.getExtension(JvmProtoBuf.typeAnnotation).map { loadTypeAnnotation(it, nameResolver) }
    }

    override fun loadTypeParameterAnnotations(proto: ProtoBuf.TypeParameter, nameResolver: NameResolver): List<A> {
        return proto.getExtension(JvmProtoBuf.typeParameterAnnotation).map { loadTypeAnnotation(it, nameResolver) }
    }

    protected fun findClassWithAnnotationsAndInitializers(
        container: ProtoContainer, specialCase: KotlinJvmBinaryClass?
    ): KotlinJvmBinaryClass? {
        return when {
            specialCase != null -> specialCase
            container is ProtoContainer.Class -> container.toBinaryClass()
            else -> null
        }
    }

    // TODO: do not use KotlinClassFinder#findKotlinClass here because it traverses the file system in the compiler
    // Introduce an API in KotlinJvmBinaryClass to find a class nearby instead
    protected fun getSpecialCaseContainerClass(
        container: ProtoContainer,
        property: Boolean,
        field: Boolean,
        isConst: Boolean?,
        isMovedFromInterfaceCompanion: Boolean
    ): KotlinJvmBinaryClass? {
        if (property) {
            checkNotNull(isConst) { "isConst should not be null for property (container=$container)" }
            if (container is ProtoContainer.Class && container.kind == ProtoBuf.Class.Kind.INTERFACE) {
                return kotlinClassFinder.findKotlinClass(
                    container.classId.createNestedClassId(Name.identifier(JvmAbi.DEFAULT_IMPLS_CLASS_NAME))
                )
            }
            if (isConst && container is ProtoContainer.Package) {
                // Const properties in multifile classes are generated into the facade class
                val facadeClassName = (container.source as? JvmPackagePartSource)?.facadeClassName
                if (facadeClassName != null) {
                    // Converting '/' to '.' is fine here because the facade class has a top level ClassId
                    return kotlinClassFinder.findKotlinClass(ClassId.topLevel(FqName(facadeClassName.internalName.replace('/', '.'))))
                }
            }
        }
        if (field && container is ProtoContainer.Class && container.kind == ProtoBuf.Class.Kind.COMPANION_OBJECT) {
            val outerClass = container.outerClass
            if (outerClass != null &&
                (outerClass.kind == ProtoBuf.Class.Kind.CLASS || outerClass.kind == ProtoBuf.Class.Kind.ENUM_CLASS ||
                        (isMovedFromInterfaceCompanion &&
                                (outerClass.kind == ProtoBuf.Class.Kind.INTERFACE ||
                                        outerClass.kind == ProtoBuf.Class.Kind.ANNOTATION_CLASS)))
            ) {
                // Backing fields of properties of a companion object in a class are generated in the outer class
                return outerClass.toBinaryClass()
            }
        }
        if (container is ProtoContainer.Package && container.source is JvmPackagePartSource) {
            val jvmPackagePartSource = container.source as JvmPackagePartSource

            return jvmPackagePartSource.knownJvmBinaryClass
                ?: kotlinClassFinder.findKotlinClass(jvmPackagePartSource.classId)
        }
        return null
    }

    protected fun getCallableSignature(
        proto: MessageLite,
        nameResolver: NameResolver,
        typeTable: TypeTable,
        kind: AnnotatedCallableKind,
        requireHasFieldFlagForField: Boolean = false
    ): MemberSignature? {
        return when (proto) {
            is ProtoBuf.Constructor -> {
                MemberSignature.fromJvmMemberSignature(
                    JvmProtoBufUtil.getJvmConstructorSignature(proto, nameResolver, typeTable) ?: return null
                )
            }
            is ProtoBuf.Function -> {
                MemberSignature.fromJvmMemberSignature(JvmProtoBufUtil.getJvmMethodSignature(proto, nameResolver, typeTable) ?: return null)
            }
            is ProtoBuf.Property -> {
                val signature = proto.getExtensionOrNull(propertySignature) ?: return null
                when (kind) {
                    AnnotatedCallableKind.PROPERTY_GETTER ->
                        if (signature.hasGetter()) MemberSignature.fromMethod(nameResolver, signature.getter) else null
                    AnnotatedCallableKind.PROPERTY_SETTER ->
                        if (signature.hasSetter()) MemberSignature.fromMethod(nameResolver, signature.setter) else null
                    AnnotatedCallableKind.PROPERTY ->
                        getPropertySignature(proto, nameResolver, typeTable, true, true, requireHasFieldFlagForField)
                    else -> null
                }
            }
            else -> null
        }
    }

    protected fun isImplicitRepeatableContainer(classId: ClassId): Boolean {
        if (classId.outerClassId == null ||
            classId.shortClassName.asString() != JvmAbi.REPEATABLE_ANNOTATION_CONTAINER_NAME
        ) return false

        val klass = kotlinClassFinder.findKotlinClass(classId)
        return klass != null && SpecialJvmAnnotations.isAnnotatedWithContainerMetaAnnotation(klass)
    }

    abstract class AnnotationsContainer<out A> {
        abstract val memberAnnotations: Map<MemberSignature, List<A>>
    }
}

fun getPropertySignature(
    proto: ProtoBuf.Property,
    nameResolver: NameResolver,
    typeTable: TypeTable,
    field: Boolean = false,
    synthetic: Boolean = false,
    requireHasFieldFlagForField: Boolean = true
): MemberSignature? {
    val signature = proto.getExtensionOrNull(propertySignature) ?: return null

    if (field) {
        val fieldSignature =
            JvmProtoBufUtil.getJvmFieldSignature(proto, nameResolver, typeTable, requireHasFieldFlagForField) ?: return null
        return MemberSignature.fromJvmMemberSignature(fieldSignature)
    } else if (synthetic && signature.hasSyntheticMethod()) {
        return MemberSignature.fromMethod(nameResolver, signature.syntheticMethod)
    }

    return null
}
