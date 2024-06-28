/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.serialization

import org.jetbrains.kotlin.builtins.functions.FunctionTypeKind
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.FirAnnotationContainer
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.isExpect
import org.jetbrains.kotlin.fir.declarations.utils.isInterface
import org.jetbrains.kotlin.fir.declarations.utils.visibility
import org.jetbrains.kotlin.fir.diagnostics.ConeIntermediateDiagnostic
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.languageVersionSettings
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.protobuf.GeneratedMessageLite
import org.jetbrains.kotlin.types.AbstractTypeApproximator
import org.jetbrains.kotlin.types.model.SimpleTypeMarker

class TypeApproximatorForMetadataSerializer(session: FirSession) :
    AbstractTypeApproximator(session.typeContext, session.languageVersionSettings) {

    override fun createErrorType(debugName: String, delegatedType: SimpleTypeMarker?): SimpleTypeMarker {
        return ConeErrorType(ConeIntermediateDiagnostic(debugName))
    }
}

fun ConeKotlinType.suspendFunctionTypeToFunctionTypeWithContinuation(session: FirSession, continuationClassId: ClassId): ConeClassLikeType {
    require(this.isSuspendOrKSuspendFunctionType(session))
    val kind =
        if (isReflectFunctionType(session)) FunctionTypeKind.KFunction
        else FunctionTypeKind.Function
    val fullyExpandedType = type.fullyExpandedType(session)
    val typeArguments = fullyExpandedType.typeArguments
    val functionTypeId = ClassId(kind.packageFqName, kind.numberedClassName(typeArguments.size))
    val lastTypeArgument = typeArguments.last()
    return ConeClassLikeTypeImpl(
        functionTypeId.toLookupTag(),
        typeArguments = (typeArguments.dropLast(1) + continuationClassId.toLookupTag().constructClassType(
            arrayOf(lastTypeArgument),
            isNullable = false
        ) + session.builtinTypes.nullableAnyType.type).toTypedArray(),
        isNullable = fullyExpandedType.isNullable,
        attributes = fullyExpandedType.attributes
    )
}

fun FirMemberDeclaration.isNotExpectOrShouldBeSerialized(actualizedExpectDeclaration: Set<FirDeclaration>?): Boolean {
    return !isExpect || actualizedExpectDeclaration == null || this !in actualizedExpectDeclaration
}

fun FirMemberDeclaration.isNotPrivateOrShouldBeSerialized(produceHeaderKlib: Boolean): Boolean {
    return !produceHeaderKlib || visibility.isPublicAPI || visibility == Visibilities.Internal
            // Always keep private interfaces as they can be part of public type hierarchies.
            // We also keep private type aliases as they leak into public signatures (KT-17229).
            // TODO: stop preserving private type aliases once KT-17229 is fixed.
            || (this as? FirClass)?.isInterface == true || this is FirTypeAlias
}

fun <
        MessageType : GeneratedMessageLite.ExtendableMessage<MessageType>,
        BuilderType : GeneratedMessageLite.ExtendableBuilder<MessageType, BuilderType>,
        > FirAnnotationContainer.serializeAnnotations(
    session: FirSession,
    additionalMetadataProvider: FirAdditionalMetadataProvider?,
    annotationSerializer: FirAnnotationSerializer,
    proto: GeneratedMessageLite.ExtendableBuilder<MessageType, BuilderType>,
    extension: GeneratedMessageLite.GeneratedExtension<MessageType, List<ProtoBuf.Annotation>>?,
) {
    if (extension == null) return
    allRequiredAnnotations(session, additionalMetadataProvider).serializeAnnotations(annotationSerializer, proto, extension)
}

fun FirAnnotationContainer.allRequiredAnnotations(
    session: FirSession,
    additionalMetadataProvider: FirAdditionalMetadataProvider?,
): List<FirAnnotation> {
    val nonSourceAnnotations = nonSourceAnnotations(session)
    return if (this is FirDeclaration && additionalMetadataProvider != null) {
        nonSourceAnnotations + additionalMetadataProvider.findGeneratedAnnotationsFor(this)
    } else {
        nonSourceAnnotations
    }
}

fun <
        MessageType : GeneratedMessageLite.ExtendableMessage<MessageType>,
        BuilderType : GeneratedMessageLite.ExtendableBuilder<MessageType, BuilderType>,
        > List<FirAnnotation>.serializeAnnotations(
    annotationSerializer: FirAnnotationSerializer,
    proto: GeneratedMessageLite.ExtendableBuilder<MessageType, BuilderType>,
    extension: GeneratedMessageLite.GeneratedExtension<MessageType, List<ProtoBuf.Annotation>>?,
) {
    if (extension == null) return
    for (annotation in this) {
        proto.addExtensionOrNull(extension, annotationSerializer.serializeAnnotation(annotation))
    }
}

fun <
        MessageType : GeneratedMessageLite.ExtendableMessage<MessageType>,
        BuilderType : GeneratedMessageLite.ExtendableBuilder<MessageType, BuilderType>,
        Type,
        > GeneratedMessageLite.ExtendableBuilder<MessageType, BuilderType>.addExtensionOrNull(
    extension: GeneratedMessageLite.GeneratedExtension<MessageType, List<Type>>,
    value: Type?,
) {
    if (value != null) {
        addExtension(extension, value)
    }
}
