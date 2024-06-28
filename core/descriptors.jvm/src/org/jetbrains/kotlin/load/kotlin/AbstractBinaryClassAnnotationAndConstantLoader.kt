/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.load.kotlin

import org.jetbrains.kotlin.SpecialJvmAnnotations
import org.jetbrains.kotlin.builtins.UnsignedTypes
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.deserialization.Flags
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmProtoBufUtil
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.constants.ConstantValue
import org.jetbrains.kotlin.resolve.constants.KClassValue
import org.jetbrains.kotlin.serialization.deserialization.AnnotatedCallableKind
import org.jetbrains.kotlin.serialization.deserialization.AnnotationAndConstantLoader
import org.jetbrains.kotlin.serialization.deserialization.ProtoContainer
import org.jetbrains.kotlin.storage.StorageManager
import org.jetbrains.kotlin.types.KotlinType

abstract class AbstractBinaryClassAnnotationAndConstantLoader<A : Any, C : Any>(
    storageManager: StorageManager,
    kotlinClassFinder: KotlinClassFinder
) : AnnotationAndConstantLoader<A, C>,
    AbstractBinaryClassAnnotationLoader<A, AnnotationsContainerWithConstants<A, C>>(kotlinClassFinder) {

    private val storage =
        storageManager.createMemoizedFunction<KotlinJvmBinaryClass, AnnotationsContainerWithConstants<A, C>> { kotlinClass ->
            loadAnnotationsAndInitializers(kotlinClass)
        }

    override fun getAnnotationsContainer(binaryClass: KotlinJvmBinaryClass): AnnotationsContainerWithConstants<A, C> = storage(binaryClass)

    protected abstract fun loadConstant(desc: String, initializer: Any): C?

    protected abstract fun transformToUnsignedConstant(constant: C): C?

    protected abstract fun loadAnnotationMethodDefaultValue(
        annotationClass: KotlinJvmBinaryClass,
        methodSignature: MemberSignature,
        visitResult: (C) -> Unit
    ): KotlinJvmBinaryClass.AnnotationArgumentVisitor?

    override fun loadAnnotationDefaultValue(
        container: ProtoContainer,
        proto: ProtoBuf.Property,
        expectedType: KotlinType
    ): C? {
        return loadConstantFromProperty(
            container,
            proto,
            AnnotatedCallableKind.PROPERTY_GETTER,
            expectedType
        ) { annotationParametersDefaultValues[it] }
    }

    override fun loadPropertyConstant(container: ProtoContainer, proto: ProtoBuf.Property, expectedType: KotlinType): C? {
        return loadConstantFromProperty(container, proto, AnnotatedCallableKind.PROPERTY, expectedType) { propertyConstants[it] }
    }

    private fun loadConstantFromProperty(
        container: ProtoContainer,
        proto: ProtoBuf.Property,
        annotatedCallableKind: AnnotatedCallableKind,
        expectedType: KotlinType,
        loader: AnnotationsContainerWithConstants<A, C>.(MemberSignature) -> C?
    ): C? {
        val specialCase = getSpecialCaseContainerClass(
            container,
            property = true,
            field = true,
            isConst = Flags.IS_CONST.get(proto.flags),
            isMovedFromInterfaceCompanion = JvmProtoBufUtil.isMovedFromInterfaceCompanion(proto),
            kotlinClassFinder = kotlinClassFinder, jvmMetadataVersion = jvmMetadataVersion
        )
        val kotlinClass = findClassWithAnnotationsAndInitializers(container, specialCase) ?: return null

        val requireHasFieldFlag = kotlinClass.classHeader.metadataVersion.isAtLeast(
            DeserializedDescriptorResolver.KOTLIN_1_3_RC_METADATA_VERSION
        )
        val signature =
            getCallableSignature(
                proto, container.nameResolver, container.typeTable, annotatedCallableKind, requireHasFieldFlag
            ) ?: return null

        val result = storage(kotlinClass).loader(signature) ?: return null
        return if (UnsignedTypes.isUnsignedType(expectedType)) transformToUnsignedConstant(result) else result
    }

    private fun loadAnnotationsAndInitializers(kotlinClass: KotlinJvmBinaryClass): AnnotationsContainerWithConstants<A, C> {
        val memberAnnotations = HashMap<MemberSignature, MutableList<A>>()
        val propertyConstants = HashMap<MemberSignature, C>()
        val annotationParametersDefaultValues = HashMap<MemberSignature, C>()

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

                override fun visitAnnotationMemberDefaultValue(): KotlinJvmBinaryClass.AnnotationArgumentVisitor? {
                    return loadAnnotationMethodDefaultValue(kotlinClass, signature) {
                        annotationParametersDefaultValues[signature] = it
                    }
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

        return AnnotationsContainerWithConstants(memberAnnotations, propertyConstants, annotationParametersDefaultValues)
    }


    protected fun isRepeatableWithImplicitContainer(annotationClassId: ClassId, arguments: Map<Name, ConstantValue<*>>): Boolean {
        if (annotationClassId != SpecialJvmAnnotations.JAVA_LANG_ANNOTATION_REPEATABLE) return false

        val containerKClassValue = arguments[Name.identifier("value")] as? KClassValue ?: return false
        val normalClass = containerKClassValue.value as? KClassValue.Value.NormalClass ?: return false
        return isImplicitRepeatableContainer(normalClass.classId)
    }
}

