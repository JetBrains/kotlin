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

package org.jetbrains.kotlin.load.kotlin

import org.jetbrains.kotlin.SpecialJvmAnnotations
import org.jetbrains.kotlin.builtins.UnsignedTypes
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
import org.jetbrains.kotlin.serialization.deserialization.AnnotationAndConstantLoader
import org.jetbrains.kotlin.serialization.deserialization.ProtoContainer
import org.jetbrains.kotlin.storage.StorageManager
import org.jetbrains.kotlin.types.KotlinType
import java.util.*

abstract class AbstractBinaryClassAnnotationAndConstantLoader<A : Any, C : Any>(
    storageManager: StorageManager,
    private val kotlinClassFinder: KotlinClassFinder
) : AnnotationAndConstantLoader<A, C> {
    private val storage = storageManager.createMemoizedFunction<KotlinJvmBinaryClass, Storage<A, C>> { kotlinClass ->
        loadAnnotationsAndInitializers(kotlinClass)
    }

    protected abstract fun loadConstant(desc: String, initializer: Any): C?

    protected abstract fun transformToUnsignedConstant(constant: C): C?

    protected abstract fun loadAnnotation(
        annotationClassId: ClassId,
        source: SourceElement,
        result: MutableList<A>
    ): KotlinJvmBinaryClass.AnnotationArgumentVisitor?

    protected abstract fun loadTypeAnnotation(proto: ProtoBuf.Annotation, nameResolver: NameResolver): A

    private fun loadAnnotationIfNotSpecial(
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

        return storage(kotlinClass).memberAnnotations[signature] ?: listOf()
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

    override fun loadPropertyConstant(container: ProtoContainer, proto: ProtoBuf.Property, expectedType: KotlinType): C? {
        val specialCase = getSpecialCaseContainerClass(
            container,
            property = true,
            field = true,
            isConst = Flags.IS_CONST.get(proto.flags),
            isMovedFromInterfaceCompanion = JvmProtoBufUtil.isMovedFromInterfaceCompanion(proto)
        )
        val kotlinClass = findClassWithAnnotationsAndInitializers(container, specialCase) ?: return null

        val requireHasFieldFlag = kotlinClass.classHeader.metadataVersion.isAtLeast(
            DeserializedDescriptorResolver.KOTLIN_1_3_RC_METADATA_VERSION
        )
        val signature =
            getCallableSignature(
                proto, container.nameResolver, container.typeTable, AnnotatedCallableKind.PROPERTY, requireHasFieldFlag
            ) ?: return null

        val constant = storage(kotlinClass).propertyConstants[signature] ?: return null
        return if (UnsignedTypes.isUnsignedType(expectedType)) transformToUnsignedConstant(constant) else constant
    }

    private fun findClassWithAnnotationsAndInitializers(
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
    private fun getSpecialCaseContainerClass(
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

    private fun loadAnnotationsAndInitializers(kotlinClass: KotlinJvmBinaryClass): Storage<A, C> {
        val memberAnnotations = HashMap<MemberSignature, MutableList<A>>()
        val propertyConstants = HashMap<MemberSignature, C>()

        kotlinClass.visitMembers(object : KotlinJvmBinaryClass.MemberVisitor {
            override fun visitMethod(name: Name, desc: String): KotlinJvmBinaryClass.MethodAnnotationVisitor? {
                return AnnotationVisitorForMethod(MemberSignature.fromMethodNameAndDesc(name.asString(), desc))
            }

            override fun visitField(name: Name, desc: String, initializer: Any?): KotlinJvmBinaryClass.AnnotationVisitor? {
                val signature = MemberSignature.fromFieldNameAndDesc(name.asString(), desc)

                if (initializer != null) {
                    val constant = loadConstant(desc, initializer)
                    if (constant != null) {
                        propertyConstants[signature] = constant
                    }
                }
                return MemberAnnotationVisitor(signature)
            }

            inner class AnnotationVisitorForMethod(signature: MemberSignature) : MemberAnnotationVisitor(signature),
                KotlinJvmBinaryClass.MethodAnnotationVisitor {

                override fun visitParameterAnnotation(
                    index: Int, classId: ClassId, source: SourceElement
                ): KotlinJvmBinaryClass.AnnotationArgumentVisitor? {
                    val paramSignature = MemberSignature.fromMethodSignatureAndParameterIndex(signature, index)
                    var result = memberAnnotations[paramSignature]
                    if (result == null) {
                        result = ArrayList()
                        memberAnnotations[paramSignature] = result
                    }
                    return loadAnnotationIfNotSpecial(classId, source, result)
                }
            }

            open inner class MemberAnnotationVisitor(protected val signature: MemberSignature) : KotlinJvmBinaryClass.AnnotationVisitor {
                private val result = ArrayList<A>()

                override fun visitAnnotation(classId: ClassId, source: SourceElement): KotlinJvmBinaryClass.AnnotationArgumentVisitor? {
                    return loadAnnotationIfNotSpecial(classId, source, result)
                }

                override fun visitEnd() {
                    if (result.isNotEmpty()) {
                        memberAnnotations[signature] = result
                    }
                }
            }
        }, getCachedFileContent(kotlinClass))

        return Storage(memberAnnotations, propertyConstants)
    }

    private fun getPropertySignature(
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

    private fun getCallableSignature(
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

    private class Storage<out A, out C>(
        val memberAnnotations: Map<MemberSignature, List<A>>,
        val propertyConstants: Map<MemberSignature, C>
    )
}
