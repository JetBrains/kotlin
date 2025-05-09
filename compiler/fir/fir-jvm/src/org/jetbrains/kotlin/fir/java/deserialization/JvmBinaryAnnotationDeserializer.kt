/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.java.deserialization

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.deserialization.AbstractAnnotationDeserializer
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirLiteralExpression
import org.jetbrains.kotlin.fir.expressions.builder.buildAnnotation
import org.jetbrains.kotlin.fir.java.createConstantIfAny
import org.jetbrains.kotlin.fir.languageVersionSettings
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.isUnsignedType
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.load.kotlin.KotlinClassFinder
import org.jetbrains.kotlin.load.kotlin.KotlinJvmBinaryClass
import org.jetbrains.kotlin.load.kotlin.MemberSignature
import org.jetbrains.kotlin.load.kotlin.getPropertySignature
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.deserialization.*
import org.jetbrains.kotlin.metadata.jvm.JvmProtoBuf
import org.jetbrains.kotlin.metadata.jvm.deserialization.ClassMapperLite
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmFlags
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmProtoBufUtil
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.protobuf.MessageLite
import org.jetbrains.kotlin.serialization.deserialization.builtins.BuiltInSerializerProtocol
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource
import org.jetbrains.kotlin.types.ConstantValueKind
import org.jetbrains.kotlin.util.openAddressHashTable
import org.jetbrains.kotlin.util.toJvmMetadataVersion
import org.jetbrains.kotlin.utils.addToStdlib.runIf

class JvmBinaryAnnotationDeserializer(
    val session: FirSession,
    private val kotlinBinaryClass: KotlinJvmBinaryClass,
    kotlinClassFinder: KotlinClassFinder,
    private val byteContent: ByteArray?
) : AbstractAnnotationDeserializer(session, BuiltInSerializerProtocol) {
    private val annotationInfo by lazy(LazyThreadSafetyMode.PUBLICATION) {
        session.loadMemberAnnotations(kotlinBinaryClass, byteContent, kotlinClassFinder)
    }
    private val annotationsLoader = AnnotationsLoader(session, kotlinClassFinder)

    private val annotationInfoForDefaultImpls by lazy(LazyThreadSafetyMode.PUBLICATION) {
        val defaultImplsClassId = kotlinBinaryClass.classId.createNestedClassId(Name.identifier(JvmAbi.DEFAULT_IMPLS_CLASS_NAME))
        val (defaultImplsClass, defaultImplsByteContent) = kotlinClassFinder.findKotlinClassOrContent(
            defaultImplsClassId, session.languageVersionSettings.languageVersion.toJvmMetadataVersion()
        ) as? KotlinClassFinder.Result.KotlinClass ?: return@lazy null
        session.loadMemberAnnotations(defaultImplsClass, defaultImplsByteContent, kotlinClassFinder)
    }

    override fun inheritAnnotationInfo(parent: AbstractAnnotationDeserializer) {
        if (parent is JvmBinaryAnnotationDeserializer) {
            annotationInfo.memberAnnotations.putAll(parent.annotationInfo.memberAnnotations)
        }
    }

    override fun loadClassAnnotations(classProto: ProtoBuf.Class, nameResolver: NameResolver): List<FirAnnotation> {
        // Note that HAS_ANNOTATIONS flag has incorrect value for inline classes in the old syntax (`inline class ...`).
        // For inline classes in the old syntax, JVM backend adds a `@JvmInline` annotation, but HAS_ANNOTATIONS flag is still false.
        // So, we disable the optimization that avoids loading annotations, for inline classes.
        loadAnnotationsFromMetadata(
            classProto.flags.takeUnless(Flags.IS_VALUE_CLASS::get),
            classProto.annotationList,
            nameResolver,
        )?.let { return it }

        val annotations = mutableListOf<FirAnnotation>()
        kotlinBinaryClass.loadClassAnnotations(
            object : KotlinJvmBinaryClass.AnnotationVisitor {
                override fun visitAnnotation(classId: ClassId, source: SourceElement): KotlinJvmBinaryClass.AnnotationArgumentVisitor? =
                    annotationsLoader.loadAnnotationIfNotSpecial(classId, annotations)

                override fun visitEnd() {}
            },
            byteContent,
        )
        return annotations
    }

    override fun loadTypeAnnotations(typeProto: ProtoBuf.Type, nameResolver: NameResolver): List<FirAnnotation> {
        val annotations = typeProto.getExtension(JvmProtoBuf.typeAnnotation).orEmpty()
        return annotations.map { deserializeAnnotation(it, nameResolver) }
    }

    override fun loadTypeParameterAnnotations(typeParameterProto: ProtoBuf.TypeParameter, nameResolver: NameResolver): List<FirAnnotation> {
        val annotations = typeParameterProto.getExtension(JvmProtoBuf.typeParameterAnnotation).orEmpty()
        return annotations.map { deserializeAnnotation(it, nameResolver) }
    }

    private fun loadAnnotationsFromMetadata(
        flags: Int?, annotations: List<ProtoBuf.Annotation>, nameResolver: NameResolver, useSiteTarget: AnnotationUseSiteTarget? = null,
    ): List<FirAnnotation>? =
        when {
            flags != null && !Flags.HAS_ANNOTATIONS.get(flags) -> emptyList()
            session.languageVersionSettings.supportsFeature(LanguageFeature.AnnotationsInMetadata) && annotations.isNotEmpty() ->
                annotations.map { deserializeAnnotation(it, nameResolver, useSiteTarget) }
            else -> null
        }

    override fun loadConstructorAnnotations(
        containerSource: DeserializedContainerSource?,
        constructorProto: ProtoBuf.Constructor,
        nameResolver: NameResolver,
        typeTable: TypeTable
    ): List<FirAnnotation> {
        loadAnnotationsFromMetadata(constructorProto.flags, constructorProto.annotationList, nameResolver)?.let { return it }

        val signature = getCallableSignature(constructorProto, nameResolver, typeTable) ?: return emptyList()
        return findJvmBinaryClassAndLoadMemberAnnotations(signature)
    }

    override fun loadFunctionAnnotations(
        containerSource: DeserializedContainerSource?,
        functionProto: ProtoBuf.Function,
        nameResolver: NameResolver,
        typeTable: TypeTable
    ): List<FirAnnotation> {
        loadAnnotationsFromMetadata(functionProto.flags, functionProto.annotationList, nameResolver)?.let { return it }

        val signature = getCallableSignature(functionProto, nameResolver, typeTable) ?: return emptyList()
        return findJvmBinaryClassAndLoadMemberAnnotations(signature)
    }

    override fun loadPropertyAnnotations(
        containerSource: DeserializedContainerSource?,
        propertyProto: ProtoBuf.Property,
        containingClassProto: ProtoBuf.Class?,
        nameResolver: NameResolver,
        typeTable: TypeTable
    ): List<FirAnnotation> {
        loadAnnotationsFromMetadata(
            propertyProto.flags, propertyProto.annotationList, nameResolver, AnnotationUseSiteTarget.PROPERTY,
        )?.let { return it }

        val signature = getPropertySignature(propertyProto, nameResolver, typeTable, synthetic = true) ?: return emptyList()
        val classIsInterface = containingClassProto?.let { Flags.CLASS_KIND.get(it.flags) == ProtoBuf.Class.Kind.INTERFACE } ?: false
        val jvmClassFlags = runIf(containingClassProto?.hasExtension(JvmProtoBuf.jvmClassFlags) == true) {
            containingClassProto?.getExtension(JvmProtoBuf.jvmClassFlags)
        }
        val allCompatibilityModeIsEnabled = jvmClassFlags?.let { JvmFlags.IS_COMPILED_IN_COMPATIBILITY_MODE.get(it) } ?: true
        return findJvmBinaryClassAndLoadMemberAnnotations(
            signature,
            searchInDefaultImpls = classIsInterface && allCompatibilityModeIsEnabled
        ).map {
            buildAnnotation {
                annotationTypeRef = it.annotationTypeRef
                argumentMapping = it.argumentMapping
                useSiteTarget = AnnotationUseSiteTarget.PROPERTY
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
    ): List<FirAnnotation> {
        val signature = getPropertySignature(propertyProto, nameResolver, typeTable, field = true) ?: return emptyList()
        if (signature.isDelegated) {
            return emptyList()
        }

        loadAnnotationsFromMetadata(
            propertyProto.flags, propertyProto.backingFieldAnnotationList, nameResolver, AnnotationUseSiteTarget.FIELD,
        )?.let { return it }

        return findJvmBinaryClassAndLoadMemberAnnotations(signature).map {
            buildAnnotation {
                annotationTypeRef = it.annotationTypeRef
                argumentMapping = it.argumentMapping
                useSiteTarget = AnnotationUseSiteTarget.FIELD
            }
        }
    }

    override fun loadPropertyDelegatedFieldAnnotations(
        containerSource: DeserializedContainerSource?,
        propertyProto: ProtoBuf.Property,
        nameResolver: NameResolver,
        typeTable: TypeTable
    ): List<FirAnnotation> {
        val signature = getPropertySignature(propertyProto, nameResolver, typeTable, field = true) ?: return emptyList()
        if (!signature.isDelegated) {
            return emptyList()
        }

        loadAnnotationsFromMetadata(
            propertyProto.flags, propertyProto.delegateFieldAnnotationList, nameResolver, AnnotationUseSiteTarget.PROPERTY_DELEGATE_FIELD,
        )?.let { return it }

        return findJvmBinaryClassAndLoadMemberAnnotations(signature).map {
            buildAnnotation {
                annotationTypeRef = it.annotationTypeRef
                argumentMapping = it.argumentMapping
                useSiteTarget = AnnotationUseSiteTarget.PROPERTY_DELEGATE_FIELD
            }
        }
    }

    override fun loadPropertyGetterAnnotations(
        containerSource: DeserializedContainerSource?,
        propertyProto: ProtoBuf.Property,
        nameResolver: NameResolver,
        typeTable: TypeTable,
        getterFlags: Int
    ): List<FirAnnotation> {
        loadAnnotationsFromMetadata(getterFlags, propertyProto.getterAnnotationList, nameResolver)?.let { return it }

        val signature = getCallableSignature(propertyProto, nameResolver, typeTable, CallableKind.PROPERTY_GETTER) ?: return emptyList()
        return findJvmBinaryClassAndLoadMemberAnnotations(signature)
    }

    override fun loadPropertySetterAnnotations(
        containerSource: DeserializedContainerSource?,
        propertyProto: ProtoBuf.Property,
        nameResolver: NameResolver,
        typeTable: TypeTable,
        setterFlags: Int
    ): List<FirAnnotation> {
        loadAnnotationsFromMetadata(setterFlags, propertyProto.setterAnnotationList, nameResolver)?.let { return it }

        val signature = getCallableSignature(propertyProto, nameResolver, typeTable, CallableKind.PROPERTY_SETTER) ?: return emptyList()
        return findJvmBinaryClassAndLoadMemberAnnotations(signature)
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
    ): List<FirAnnotation> {
        loadAnnotationsFromMetadata(valueParameterProto.flags, valueParameterProto.annotationList, nameResolver)?.let { return it }

        val methodSignature = getCallableSignature(callableProto, nameResolver, typeTable, kind) ?: return emptyList()
        val index = parameterIndex + computeJvmParameterIndexShift(classProto, callableProto)
        val paramSignature = MemberSignature.fromMethodSignatureAndParameterIndex(methodSignature, index)
        return findJvmBinaryClassAndLoadMemberAnnotations(paramSignature)
    }

    override fun loadEnumEntryAnnotations(
        classId: ClassId,
        enumEntryProto: ProtoBuf.EnumEntry,
        nameResolver: NameResolver,
    ): List<FirAnnotation> {
        loadAnnotationsFromMetadata(flags = null, enumEntryProto.annotationList, nameResolver)?.let { return it }

        val signature = MemberSignature.fromFieldNameAndDesc(
            nameResolver.getString(enumEntryProto.name),
            ClassMapperLite.mapClass(classId.asString())
        )
        return findJvmBinaryClassAndLoadMemberAnnotations(signature)
    }

    override fun loadExtensionReceiverParameterAnnotations(
        containerSource: DeserializedContainerSource?,
        callableProto: MessageLite,
        nameResolver: NameResolver,
        typeTable: TypeTable,
        kind: CallableKind
    ): List<FirAnnotation> {
        when (callableProto) {
            is ProtoBuf.Function ->
                loadAnnotationsFromMetadata(flags = null, callableProto.extensionReceiverAnnotationList, nameResolver)
                    ?.let { return it }
            is ProtoBuf.Property ->
                loadAnnotationsFromMetadata(flags = null, callableProto.extensionReceiverAnnotationList, nameResolver)
                    ?.let { return it }
        }
        val methodSignature = getCallableSignature(callableProto, nameResolver, typeTable, kind) ?: return emptyList()
        val paramSignature = MemberSignature.fromMethodSignatureAndParameterIndex(methodSignature, 0)
        return findJvmBinaryClassAndLoadMemberAnnotations(paramSignature)
    }

    override fun loadAnnotationPropertyDefaultValue(
        containerSource: DeserializedContainerSource?,
        propertyProto: ProtoBuf.Property,
        expectedPropertyType: FirTypeRef,
        nameResolver: NameResolver,
        typeTable: TypeTable
    ): FirExpression? {
        val signature = getCallableSignature(propertyProto, nameResolver, typeTable, CallableKind.PROPERTY_GETTER) ?: return null
        val firExpr = annotationInfo.annotationMethodsDefaultValues[signature]
        return if (firExpr is FirLiteralExpression && expectedPropertyType.coneType.isUnsignedType && firExpr.kind.isSignedNumber)
            firExpr.value.createConstantIfAny(session, unsigned = true)
        else
            firExpr
    }

    private val ConstantValueKind.isSignedNumber: Boolean
        get() = this is ConstantValueKind.Byte || this is ConstantValueKind.Short || this is ConstantValueKind.Int || this is ConstantValueKind.Long

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

    private fun findJvmBinaryClassAndLoadMemberAnnotations(
        memberSignature: MemberSignature, searchInDefaultImpls: Boolean = false
    ): List<FirAnnotation> {
        val info = if (searchInDefaultImpls) {
            annotationInfoForDefaultImpls ?: return emptyList()
        } else {
            annotationInfo
        }
        return info.memberAnnotations[memberSignature] ?: emptyList()
    }
}

// TODO: Rename this once property constants are recorded as well
private data class MemberAnnotations(
    val memberAnnotations: MutableMap<MemberSignature, MutableList<FirAnnotation>>,
    val annotationMethodsDefaultValues: Map<MemberSignature, FirExpression>
)

// TODO: better to be in KotlinDeserializedJvmSymbolsProvider?
private fun FirSession.loadMemberAnnotations(
    kotlinBinaryClass: KotlinJvmBinaryClass,
    byteContent: ByteArray?,
    kotlinClassFinder: KotlinClassFinder,
): MemberAnnotations {
    val memberAnnotations = openAddressHashTable<MemberSignature, MutableList<FirAnnotation>>()
    val annotationsLoader = AnnotationsLoader(this, kotlinClassFinder)
    val annotationMethodsDefaultValues = openAddressHashTable<MemberSignature, FirExpression>()

    kotlinBinaryClass.visitMembers(object : KotlinJvmBinaryClass.MemberVisitor {
        override fun visitMethod(name: Name, desc: String): KotlinJvmBinaryClass.MethodAnnotationVisitor {
            return AnnotationVisitorForMethod(MemberSignature.fromMethodNameAndDesc(name.asString(), desc))
        }

        override fun visitField(name: Name, desc: String, initializer: Any?): KotlinJvmBinaryClass.AnnotationVisitor {
            val signature = MemberSignature.fromFieldNameAndDesc(name.asString(), desc)
            if (initializer != null) {
                // TODO: load constant
                // TODO: Given there is FirConstDeserializer, maybe this comment is obsolete?
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

            override fun visitAnnotationMemberDefaultValue(): KotlinJvmBinaryClass.AnnotationArgumentVisitor? {
                return annotationsLoader.loadAnnotationMethodDefaultValue() { annotationMethodsDefaultValues[signature] = it }
            }
        }

        open inner class MemberAnnotationVisitor(protected val signature: MemberSignature) : KotlinJvmBinaryClass.AnnotationVisitor {
            private val result = arrayListOf<FirAnnotation>()

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


    return MemberAnnotations(
        memberAnnotations,
        annotationMethodsDefaultValues
    )
}
