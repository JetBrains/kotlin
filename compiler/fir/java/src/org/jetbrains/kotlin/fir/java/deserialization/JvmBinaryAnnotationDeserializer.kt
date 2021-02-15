/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.java.deserialization

import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.deserialization.AbstractAnnotationDeserializer
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.builder.buildAnnotationCall
import org.jetbrains.kotlin.fir.resolve.symbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.impl.FirCompositeSymbolProvider
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.load.kotlin.KotlinJvmBinaryClass
import org.jetbrains.kotlin.load.kotlin.MemberSignature
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.deserialization.*
import org.jetbrains.kotlin.metadata.jvm.JvmProtoBuf
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmProtoBufUtil
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.protobuf.MessageLite
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource

class JvmBinaryAnnotationDeserializer(
    val session: FirSession,
    kotlinBinaryClass: KotlinJvmBinaryClass,
    byteContent: ByteArray?
) : AbstractAnnotationDeserializer(session) {
    private val annotationInfo by lazy(LazyThreadSafetyMode.PUBLICATION) {
        session.loadMemberAnnotations(kotlinBinaryClass, byteContent)
    }

    override fun inheritAnnotationInfo(parent: AbstractAnnotationDeserializer) {
        if (parent is JvmBinaryAnnotationDeserializer) {
            annotationInfo.memberAnnotations.putAll(parent.annotationInfo.memberAnnotations)
        }
    }

    override fun loadTypeAnnotations(typeProto: ProtoBuf.Type, nameResolver: NameResolver): List<FirAnnotationCall> {
        val annotations = typeProto.getExtension(JvmProtoBuf.typeAnnotation).orEmpty()
        return annotations.map { deserializeAnnotation(it, nameResolver) }
    }

    override fun loadConstructorAnnotations(
        containerSource: DeserializedContainerSource?,
        constructorProto: ProtoBuf.Constructor,
        nameResolver: NameResolver,
        typeTable: TypeTable
    ): List<FirAnnotationCall> {
        val signature = getCallableSignature(constructorProto, nameResolver, typeTable) ?: return emptyList()
        return findJvmBinaryClassAndLoadMemberAnnotations(containerSource, signature)
    }

    override fun loadFunctionAnnotations(
        containerSource: DeserializedContainerSource?,
        functionProto: ProtoBuf.Function,
        nameResolver: NameResolver,
        typeTable: TypeTable
    ): List<FirAnnotationCall> {
        val signature = getCallableSignature(functionProto, nameResolver, typeTable) ?: return emptyList()
        return findJvmBinaryClassAndLoadMemberAnnotations(containerSource, signature)
    }

    override fun loadPropertyAnnotations(
        containerSource: DeserializedContainerSource?,
        propertyProto: ProtoBuf.Property,
        nameResolver: NameResolver,
        typeTable: TypeTable
    ): List<FirAnnotationCall> {
        val signature = getPropertySignature(propertyProto, nameResolver, typeTable, synthetic = true) ?: return emptyList()
        return findJvmBinaryClassAndLoadMemberAnnotations(containerSource, signature).map {
            buildAnnotationCall {
                annotationTypeRef = it.annotationTypeRef
                argumentList = it.argumentList
                useSiteTarget = AnnotationUseSiteTarget.PROPERTY
                calleeReference = it.calleeReference
            }
        }
    }

    private val MemberSignature.isDelegated: Boolean
        get() = JvmAbi.DELEGATED_PROPERTY_NAME_SUFFIX in this.signature

    override fun loadPropertyBackingFieldAnnotations(
        containerSource: DeserializedContainerSource?,
        propertyProto: ProtoBuf.Property,
        nameResolver: NameResolver,
        typeTable: TypeTable
    ): List<FirAnnotationCall> {
        val signature = getPropertySignature(propertyProto, nameResolver, typeTable, field = true) ?: return emptyList()
        if (signature.isDelegated) {
            return emptyList()
        }
        return findJvmBinaryClassAndLoadMemberAnnotations(containerSource, signature).map {
            buildAnnotationCall {
                annotationTypeRef = it.annotationTypeRef
                argumentList = it.argumentList
                useSiteTarget = AnnotationUseSiteTarget.FIELD
                calleeReference = it.calleeReference
            }
        }
    }

    override fun loadPropertyDelegatedFieldAnnotations(
        containerSource: DeserializedContainerSource?,
        propertyProto: ProtoBuf.Property,
        nameResolver: NameResolver,
        typeTable: TypeTable
    ): List<FirAnnotationCall> {
        val signature = getPropertySignature(propertyProto, nameResolver, typeTable, field = true) ?: return emptyList()
        if (!signature.isDelegated) {
            return emptyList()
        }
        return findJvmBinaryClassAndLoadMemberAnnotations(containerSource, signature).map {
            buildAnnotationCall {
                annotationTypeRef = it.annotationTypeRef
                argumentList = it.argumentList
                useSiteTarget = AnnotationUseSiteTarget.PROPERTY_DELEGATE_FIELD
                calleeReference = it.calleeReference
            }
        }
    }

    override fun loadPropertyGetterAnnotations(
        containerSource: DeserializedContainerSource?,
        propertyProto: ProtoBuf.Property,
        nameResolver: NameResolver,
        typeTable: TypeTable,
        getterFlags: Int
    ): List<FirAnnotationCall> {
        val signature = getCallableSignature(propertyProto, nameResolver, typeTable, CallableKind.PROPERTY_GETTER) ?: return emptyList()
        return findJvmBinaryClassAndLoadMemberAnnotations(containerSource, signature)
    }

    override fun loadPropertySetterAnnotations(
        containerSource: DeserializedContainerSource?,
        propertyProto: ProtoBuf.Property,
        nameResolver: NameResolver,
        typeTable: TypeTable,
        setterFlags: Int
    ): List<FirAnnotationCall> {
        val signature = getCallableSignature(propertyProto, nameResolver, typeTable, CallableKind.PROPERTY_SETTER) ?: return emptyList()
        return findJvmBinaryClassAndLoadMemberAnnotations(containerSource, signature)
    }

    override fun loadValueParameterAnnotations(
        containerSource: DeserializedContainerSource?,
        callableProto: MessageLite,
        valueParameterProto: ProtoBuf.ValueParameter,
        classProto: ProtoBuf.Class?,
        nameResolver: NameResolver,
        typeTable: TypeTable,
        kind: CallableKind,
        parameterIndex: Int,
    ): List<FirAnnotationCall> {
        val methodSignature = getCallableSignature(callableProto, nameResolver, typeTable, kind) ?: return emptyList()
        val index = parameterIndex + computeJvmParameterIndexShift(classProto, callableProto)
        val paramSignature = MemberSignature.fromMethodSignatureAndParameterIndex(methodSignature, index)
        return findJvmBinaryClassAndLoadMemberAnnotations(containerSource, paramSignature)
    }

    override fun loadExtensionReceiverParameterAnnotations(
        containerSource: DeserializedContainerSource?,
        callableProto: MessageLite,
        nameResolver: NameResolver,
        typeTable: TypeTable,
        kind: CallableKind
    ): List<FirAnnotationCall> {
        val methodSignature = getCallableSignature(callableProto, nameResolver, typeTable, kind) ?: return emptyList()
        val paramSignature = MemberSignature.fromMethodSignatureAndParameterIndex(methodSignature, 0)
        return findJvmBinaryClassAndLoadMemberAnnotations(containerSource, paramSignature)
    }

    private fun computeJvmParameterIndexShift(classProto: ProtoBuf.Class?, message: MessageLite): Int {
        return when (message) {
            is ProtoBuf.Function -> if (message.hasReceiver()) 1 else 0
            is ProtoBuf.Property -> if (message.hasReceiver()) 1 else 0
            is ProtoBuf.Constructor -> {
                assert(classProto != null) {
                    "Constructor call without information about enclosing Class: $message"
                }
                val kind = Flags.CLASS_KIND.get(classProto!!.flags) ?: ProtoBuf.Class.Kind.CLASS
                val isInner = Flags.IS_INNER.get(classProto.flags)
                when {
                    kind == ProtoBuf.Class.Kind.ENUM_CLASS -> 2
                    isInner -> 1
                    else -> 0
                }
            }
            else -> throw UnsupportedOperationException("Unsupported message: ${message::class.java}")
        }
    }

    private fun getCallableSignature(
        proto: MessageLite,
        nameResolver: NameResolver,
        typeTable: TypeTable,
        kind: CallableKind = CallableKind.OTHERS,
        requireHasFieldFlagForField: Boolean = false
    ): MemberSignature? {
        return when (proto) {
            is ProtoBuf.Constructor -> {
                MemberSignature.fromJvmMemberSignature(
                    JvmProtoBufUtil.getJvmConstructorSignature(proto, nameResolver, typeTable) ?: return null
                )
            }
            is ProtoBuf.Function -> {
                val signature = JvmProtoBufUtil.getJvmMethodSignature(proto, nameResolver, typeTable) ?: return null
                MemberSignature.fromJvmMemberSignature(signature)
            }
            is ProtoBuf.Property -> {
                val signature = proto.getExtensionOrNull(JvmProtoBuf.propertySignature) ?: return null
                when (kind) {
                    CallableKind.PROPERTY_GETTER ->
                        if (signature.hasGetter()) MemberSignature.fromMethod(nameResolver, signature.getter) else null
                    CallableKind.PROPERTY_SETTER ->
                        if (signature.hasSetter()) MemberSignature.fromMethod(nameResolver, signature.setter) else null
                    CallableKind.PROPERTY ->
                        getPropertySignature(
                            proto, nameResolver, typeTable,
                            field = true,
                            requireHasFieldFlagForField = requireHasFieldFlagForField
                        )
                    else ->
                        null
                }
            }
            else -> null
        }
    }

    private fun getPropertySignature(
        proto: ProtoBuf.Property,
        nameResolver: NameResolver,
        typeTable: TypeTable,
        field: Boolean = false,
        synthetic: Boolean = false,
        requireHasFieldFlagForField: Boolean = true
    ): MemberSignature? {
        val signature = proto.getExtensionOrNull(JvmProtoBuf.propertySignature) ?: return null

        if (field) {
            val fieldSignature =
                JvmProtoBufUtil.getJvmFieldSignature(proto, nameResolver, typeTable, requireHasFieldFlagForField) ?: return null
            return MemberSignature.fromJvmMemberSignature(fieldSignature)
        } else if (synthetic && signature.hasSyntheticMethod()) {
            return MemberSignature.fromMethod(nameResolver, signature.syntheticMethod)
        }

        return null
    }

    private fun findJvmBinaryClassAndLoadMemberAnnotations(
        containerSource: DeserializedContainerSource?,
        memberSignature: MemberSignature
    ): List<FirAnnotationCall> {
        return annotationInfo.memberAnnotations[memberSignature] ?: emptyList()
    }
}

// TODO: Rename this once property constants are recorded as well
private data class MemberAnnotations(val memberAnnotations: MutableMap<MemberSignature, MutableList<FirAnnotationCall>>)

// TODO: better to be in KotlinDeserializedJvmSymbolsProvider?
private fun FirSession.loadMemberAnnotations(kotlinBinaryClass: KotlinJvmBinaryClass, byteContent: ByteArray?): MemberAnnotations {
    val memberAnnotations = hashMapOf<MemberSignature, MutableList<FirAnnotationCall>>()
    val annotationsLoader = AnnotationsLoader(this)

    kotlinBinaryClass.visitMembers(object : KotlinJvmBinaryClass.MemberVisitor {
        override fun visitMethod(name: Name, desc: String): KotlinJvmBinaryClass.MethodAnnotationVisitor? {
            return AnnotationVisitorForMethod(MemberSignature.fromMethodNameAndDesc(name.asString(), desc))
        }

        override fun visitField(name: Name, desc: String, initializer: Any?): KotlinJvmBinaryClass.AnnotationVisitor? {
            val signature = MemberSignature.fromFieldNameAndDesc(name.asString(), desc)
            if (initializer != null) {
                // TODO: load constant
            }
            return MemberAnnotationVisitor(signature)
        }

        inner class AnnotationVisitorForMethod(signature: MemberSignature) : MemberAnnotationVisitor(signature),
            KotlinJvmBinaryClass.MethodAnnotationVisitor {

            override fun visitParameterAnnotation(
                index: Int,
                classId: ClassId,
                source: SourceElement
            ): KotlinJvmBinaryClass.AnnotationArgumentVisitor? {
                val paramSignature = MemberSignature.fromMethodSignatureAndParameterIndex(signature, index)
                var result = memberAnnotations[paramSignature]
                if (result == null) {
                    result = arrayListOf()
                    memberAnnotations[paramSignature] = result
                }
                return annotationsLoader.loadAnnotationIfNotSpecial(classId, result)
            }
        }

        open inner class MemberAnnotationVisitor(protected val signature: MemberSignature) : KotlinJvmBinaryClass.AnnotationVisitor {
            private val result = arrayListOf<FirAnnotationCall>()

            override fun visitAnnotation(classId: ClassId, source: SourceElement): KotlinJvmBinaryClass.AnnotationArgumentVisitor? {
                return annotationsLoader.loadAnnotationIfNotSpecial(classId, result)
            }

            override fun visitEnd() {
                if (result.isNotEmpty()) {
                    memberAnnotations[signature] = result
                }
            }
        }
    }, byteContent)

    return MemberAnnotations(memberAnnotations)
}
