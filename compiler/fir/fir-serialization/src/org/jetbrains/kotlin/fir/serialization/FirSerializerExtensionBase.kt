/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.serialization

import org.jetbrains.kotlin.config.AnalysisFlags
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.constant.ConstantValue
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.fir.FirAnnotationContainer
import org.jetbrains.kotlin.fir.FirEvaluatorResult
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.evaluatedInitializer
import org.jetbrains.kotlin.fir.declarations.utils.isConst
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.languageVersionSettings
import org.jetbrains.kotlin.fir.render
import org.jetbrains.kotlin.fir.serialization.constant.toConstantValue
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.ProtoBuf.Class.Builder
import org.jetbrains.kotlin.metadata.deserialization.Flags
import org.jetbrains.kotlin.metadata.serialization.MutableVersionRequirementTable
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.protobuf.GeneratedMessageLite
import org.jetbrains.kotlin.serialization.SerializerExtensionProtocol

abstract class FirSerializerExtensionBase(
    val protocol: SerializerExtensionProtocol,
    val annotationsInMetadataLanguageFeature: LanguageFeature? = null,
) : FirSerializerExtension() {
    final override val stringTable: FirElementAwareSerializableStringTable = FirElementAwareSerializableStringTable()

    override fun serializePackage(
        packageFqName: FqName,
        proto: ProtoBuf.Package.Builder,
        versionRequirementTable: MutableVersionRequirementTable?,
        childSerializer: FirElementSerializer
    ) {
        proto.setExtension(protocol.packageFqName, stringTable.getPackageFqNameIndex(packageFqName))
    }

    override fun serializeClass(
        klass: FirClass,
        proto: ProtoBuf.Class.Builder,
        versionRequirementTable: MutableVersionRequirementTable,
        childSerializer: FirElementSerializer
    ) {
        klass.serializeAnnotations(proto, protocol.classAnnotation, proto::addAnnotation)
    }

    override fun serializeScript(
        script: FirScript,
        proto: Builder,
        versionRequirementTable: MutableVersionRequirementTable,
        childSerializer: FirElementSerializer,
    ) {}

    override fun serializeConstructor(
        constructor: FirConstructor,
        proto: ProtoBuf.Constructor.Builder,
        childSerializer: FirElementSerializer
    ) {
        constructor.serializeAnnotations(proto, protocol.constructorAnnotation, proto::addAnnotation)
    }

    override fun serializeFunction(
        function: FirFunction,
        proto: ProtoBuf.Function.Builder,
        versionRequirementTable: MutableVersionRequirementTable?,
        childSerializer: FirElementSerializer
    ) {
        function.serializeAnnotations(proto, protocol.functionAnnotation, proto::addAnnotation)
        function.receiverParameter?.serializeAnnotations(
            proto,
            protocol.functionExtensionReceiverAnnotation,
            proto::addExtensionReceiverAnnotation,
        )
    }

    override fun serializeProperty(
        property: FirProperty,
        proto: ProtoBuf.Property.Builder,
        versionRequirementTable: MutableVersionRequirementTable?,
        childSerializer: FirElementSerializer
    ) {
        val fieldPropertyAnnotations = mutableListOf<FirAnnotation>()
        val delegatePropertyAnnotations = mutableListOf<FirAnnotation>()

        for (annotation in property.backingField?.allRequiredAnnotations(session, additionalMetadataProvider).orEmpty()) {
            val destination = when (annotation.useSiteTarget) {
                AnnotationUseSiteTarget.PROPERTY_DELEGATE_FIELD -> delegatePropertyAnnotations
                else -> fieldPropertyAnnotations
            }
            destination += annotation
        }

        property.serializeAnnotations(proto, protocol.propertyAnnotation, proto::addAnnotation)
        fieldPropertyAnnotations.serializeAnnotations(
            proto,
            protocol.propertyBackingFieldAnnotation,
            proto::addBackingFieldAnnotation,
        )
        delegatePropertyAnnotations.serializeAnnotations(
            proto,
            protocol.propertyDelegatedFieldAnnotation,
            proto::addDelegateFieldAnnotation,
        )

        property.getter?.serializeAnnotations(proto, protocol.propertyGetterAnnotation, proto::addGetterAnnotation)
        property.setter?.serializeAnnotations(proto, protocol.propertySetterAnnotation, proto::addSetterAnnotation)
        property.receiverParameter?.serializeAnnotations(
            proto,
            protocol.propertyExtensionReceiverAnnotation,
            proto::addExtensionReceiverAnnotation,
        )

        if (!Flags.HAS_CONSTANT.get(proto.flags)) return
        val evaluatedInitializer = (property.evaluatedInitializer as? FirEvaluatorResult.Evaluated)?.result as? FirExpression
        evaluatedInitializer?.toConstantValue<ConstantValue<*>>(constValueProvider)?.let {
            proto.setExtension(protocol.compileTimeValue, annotationSerializer.valueProto(it).build())
        } ?: absentInitializerGuard(property)
    }

    private fun absentInitializerGuard(property: FirProperty) {
        if (property.isConst) {
            // KT-49303: TODO Refine the condition below, when empty initializer is allowed in metadata section of platform Klib
            require(!session.languageVersionSettings.getFlag(AnalysisFlags.metadataCompilation) &&
                        session.languageVersionSettings.supportsFeature(LanguageFeature.IntrinsicConstEvaluation)
            ) { "Const property has no const initializer expression. Got ${property.render()}" }
        }
    }

    override fun serializeEnumEntry(enumEntry: FirEnumEntry, proto: ProtoBuf.EnumEntry.Builder) {
        enumEntry.serializeAnnotations(proto, protocol.enumEntryAnnotation, proto::addAnnotation)
    }

    override fun serializeValueParameter(parameter: FirValueParameter, proto: ProtoBuf.ValueParameter.Builder) {
        parameter.serializeAnnotations(proto, protocol.parameterAnnotation, proto::addAnnotation)
    }

    override fun serializeTypeAnnotations(annotations: List<FirAnnotation>, proto: ProtoBuf.Type.Builder) {
        annotations.serializeAnnotations(proto, protocol.typeAnnotation, proto::addAnnotation)
    }

    override fun serializeTypeParameter(typeParameter: FirTypeParameter, proto: ProtoBuf.TypeParameter.Builder) {
        typeParameter.serializeAnnotations(proto, protocol.typeParameterAnnotation, proto::addAnnotation)
    }

    private fun <
            MessageType : GeneratedMessageLite.ExtendableMessage<MessageType>,
            BuilderType : GeneratedMessageLite.ExtendableBuilder<MessageType, BuilderType>,
            > FirAnnotationContainer.serializeAnnotations(
        proto: GeneratedMessageLite.ExtendableBuilder<MessageType, BuilderType>,
        extension: GeneratedMessageLite.GeneratedExtension<MessageType, List<ProtoBuf.Annotation>>?,
        addAnnotation: (ProtoBuf.Annotation) -> Unit,
    ) {
        serializeAnnotations(
            session,
            additionalMetadataProvider,
            annotationSerializer,
            proto,
            extension,
            addAnnotation,
            annotationsInMetadataLanguageFeature,
        )
    }

    private fun <
            MessageType : GeneratedMessageLite.ExtendableMessage<MessageType>,
            BuilderType : GeneratedMessageLite.ExtendableBuilder<MessageType, BuilderType>,
            > List<FirAnnotation>.serializeAnnotations(
        proto: GeneratedMessageLite.ExtendableBuilder<MessageType, BuilderType>,
        extension: GeneratedMessageLite.GeneratedExtension<MessageType, List<ProtoBuf.Annotation>>?,
        addAnnotation: (ProtoBuf.Annotation) -> Unit,
    ) {
        serializeAnnotations(
            session,
            annotationSerializer,
            proto,
            extension,
            addAnnotation,
            annotationsInMetadataLanguageFeature,
        )
    }
}
