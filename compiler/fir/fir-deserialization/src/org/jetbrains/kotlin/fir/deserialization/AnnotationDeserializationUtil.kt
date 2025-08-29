/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.deserialization

import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirAnnotationArgumentMapping
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirLiteralExpression
import org.jetbrains.kotlin.fir.expressions.buildUnaryArgumentList
import org.jetbrains.kotlin.fir.expressions.builder.buildAnnotation
import org.jetbrains.kotlin.fir.expressions.builder.buildAnnotationArgumentMapping
import org.jetbrains.kotlin.fir.expressions.builder.buildArgumentList
import org.jetbrains.kotlin.fir.expressions.builder.buildArrayLiteral
import org.jetbrains.kotlin.fir.expressions.builder.buildClassReferenceExpression
import org.jetbrains.kotlin.fir.expressions.builder.buildEnumEntryDeserializedAccessExpression
import org.jetbrains.kotlin.fir.expressions.builder.buildGetClassCall
import org.jetbrains.kotlin.fir.expressions.builder.buildLiteralExpression
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.resolve.scope
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.scopes.CallableCopyTypeCalculator
import org.jetbrains.kotlin.fir.scopes.getDeclaredConstructors
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.lazyDeclarationResolver
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.arrayElementType
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.constructClassLikeType
import org.jetbrains.kotlin.fir.types.constructClassType
import org.jetbrains.kotlin.fir.types.constructType
import org.jetbrains.kotlin.fir.types.createArrayType
import org.jetbrains.kotlin.fir.types.toLookupTag
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.ProtoBuf.Annotation.Argument.Value.Type.ANNOTATION
import org.jetbrains.kotlin.metadata.ProtoBuf.Annotation.Argument.Value.Type.BOOLEAN
import org.jetbrains.kotlin.metadata.ProtoBuf.Annotation.Argument.Value.Type.BYTE
import org.jetbrains.kotlin.metadata.ProtoBuf.Annotation.Argument.Value.Type.CHAR
import org.jetbrains.kotlin.metadata.ProtoBuf.Annotation.Argument.Value.Type.CLASS
import org.jetbrains.kotlin.metadata.ProtoBuf.Annotation.Argument.Value.Type.DOUBLE
import org.jetbrains.kotlin.metadata.ProtoBuf.Annotation.Argument.Value.Type.ENUM
import org.jetbrains.kotlin.metadata.ProtoBuf.Annotation.Argument.Value.Type.FLOAT
import org.jetbrains.kotlin.metadata.ProtoBuf.Annotation.Argument.Value.Type.INT
import org.jetbrains.kotlin.metadata.ProtoBuf.Annotation.Argument.Value.Type.LONG
import org.jetbrains.kotlin.metadata.ProtoBuf.Annotation.Argument.Value.Type.SHORT
import org.jetbrains.kotlin.metadata.ProtoBuf.Annotation.Argument.Value.Type.STRING
import org.jetbrains.kotlin.metadata.deserialization.Flags
import org.jetbrains.kotlin.metadata.deserialization.NameResolver
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.protobuf.GeneratedMessageLite
import org.jetbrains.kotlin.protobuf.GeneratedMessageLite.ExtendableMessage
import org.jetbrains.kotlin.serialization.deserialization.getClassId
import org.jetbrains.kotlin.serialization.deserialization.getName
import org.jetbrains.kotlin.types.ConstantValueKind

fun deserializeAnnotation(
    session: FirSession,
    proto: ProtoBuf.Annotation,
    nameResolver: NameResolver,
    useSiteTarget: AnnotationUseSiteTarget? = null
): FirAnnotation {
    val classId = nameResolver.getClassId(proto.id)
    return buildAnnotation {
        annotationTypeRef = buildResolvedTypeRef {
            coneType = classId.toLookupTag().constructClassType()
        }
        session.lazyDeclarationResolver.disableLazyResolveContractChecksInside {
            this.argumentMapping = createArgumentMapping(session, proto, classId, nameResolver)
        }
        useSiteTarget?.let {
            this.useSiteTarget = it
        }
    }
}

fun <T : ExtendableMessage<T>> T.loadAnnotations(
    session: FirSession,
    extension: GeneratedMessageLite.GeneratedExtension<T, List<ProtoBuf.Annotation>>?,
    flags: Int,
    nameResolver: NameResolver,
    useSiteTarget: AnnotationUseSiteTarget? = null
): List<FirAnnotation> {
    if (extension == null || flags >= 0 && !Flags.HAS_ANNOTATIONS.get(flags)) return emptyList()
    val annotations = getExtension(extension)
    return annotations.map { deserializeAnnotation(session, it, nameResolver, useSiteTarget) }
}

fun <T : ExtendableMessage<T>> T.loadAnnotations(
    session: FirSession,
    extension: GeneratedMessageLite.GeneratedExtension<T, List<ProtoBuf.Annotation>>?,
    nameResolver: NameResolver,
    useSiteTarget: AnnotationUseSiteTarget? = null
): List<FirAnnotation> {
    if (extension == null) return emptyList()
    val annotations = getExtension(extension)
    return annotations.map { deserializeAnnotation(session, it, nameResolver, useSiteTarget) }
}

private fun createArgumentMapping(
    session: FirSession,
    proto: ProtoBuf.Annotation,
    classId: ClassId,
    nameResolver: NameResolver
): FirAnnotationArgumentMapping {
    return buildAnnotationArgumentMapping build@{
        if (proto.argumentCount == 0) return@build
        // Used only for annotation parameters of array types
        // Avoid triggering it in other cases, since it's quite expensive
        val parameterByName: Map<Name, FirValueParameter>? by lazy(LazyThreadSafetyMode.NONE) {
            val lookupTag = classId.toLookupTag()
            val symbol = lookupTag.toSymbol(session)
            val firAnnotationClass = (symbol as? FirRegularClassSymbol)?.fir ?: return@lazy null

            val classScope = firAnnotationClass.defaultType().scope(
                useSiteSession = session,
                scopeSession = ScopeSession(),
                callableCopyTypeCalculator = CallableCopyTypeCalculator.DoNothing,
                requiredMembersPhase = null,
            ) ?: error("Null scope for $classId")

            val constructor = classScope.getDeclaredConstructors()
                .singleOrNull()
                ?.fir
                ?: error("No single constructor found for $classId")

            constructor.valueParameters.associateBy { it.name }
        }

        proto.argumentList.mapNotNull {
            val name = nameResolver.getName(it.nameId)
            val value = resolveValue(session, it.value, nameResolver) { parameterByName?.get(name)?.returnTypeRef?.coneType }
            name to value
        }.toMap(mapping)
    }
}

private fun resolveValue(
    session: FirSession,
    value: ProtoBuf.Annotation.Argument.Value,
    nameResolver: NameResolver,
    expectedType: () -> ConeKotlinType?,
): FirExpression {
    val isUnsigned = Flags.IS_UNSIGNED.get(value.flags)

    return when (value.type) {
        BYTE -> {
            val kind = if (isUnsigned) ConstantValueKind.UnsignedByte else ConstantValueKind.Byte
            const(kind, value.intValue.toByte(), session.builtinTypes.byteType)
        }

        SHORT -> {
            val kind = if (isUnsigned) ConstantValueKind.UnsignedShort else ConstantValueKind.Short
            const(kind, value.intValue.toShort(), session.builtinTypes.shortType)
        }

        INT -> {
            val kind = if (isUnsigned) ConstantValueKind.UnsignedInt else ConstantValueKind.Int
            const(kind, value.intValue.toInt(), session.builtinTypes.intType)
        }

        LONG -> {
            val kind = if (isUnsigned) ConstantValueKind.UnsignedLong else ConstantValueKind.Long
            const(kind, value.intValue, session.builtinTypes.longType)
        }

        CHAR -> const(ConstantValueKind.Char, value.intValue.toInt().toChar(), session.builtinTypes.charType)
        FLOAT -> const(ConstantValueKind.Float, value.floatValue, session.builtinTypes.floatType)
        DOUBLE -> const(ConstantValueKind.Double, value.doubleValue, session.builtinTypes.doubleType)
        BOOLEAN -> const(ConstantValueKind.Boolean, (value.intValue != 0L), session.builtinTypes.booleanType)
        STRING -> const(ConstantValueKind.String, nameResolver.getString(value.stringValue), session.builtinTypes.stringType)
        ANNOTATION -> deserializeAnnotation(session, value.annotation, nameResolver)
        CLASS -> buildGetClassCall {
            val classId = nameResolver.getClassId(value.classId)
            val lookupTag = classId.toLookupTag()
            val referencedType = lookupTag.constructType()
            val resolvedType = StandardClassIds.KClass.constructClassLikeType(arrayOf(referencedType), false)
            argumentList = buildUnaryArgumentList(
                buildClassReferenceExpression {
                    classTypeRef = buildResolvedTypeRef { coneType = referencedType }
                    coneTypeOrNull = resolvedType
                }
            )
            coneTypeOrNull = resolvedType
        }
        ENUM -> buildEnumEntryDeserializedAccessExpression {
            enumClassId = nameResolver.getClassId(value.classId)
            enumEntryName = nameResolver.getName(value.enumValueId)
        }
        ProtoBuf.Annotation.Argument.Value.Type.ARRAY -> {
            val expectedArrayElementType = expectedType()?.arrayElementType() ?: session.builtinTypes.anyType.coneType
            buildArrayLiteral {
                argumentList = buildArgumentList {
                    value.arrayElementList.mapTo(arguments) { resolveValue(session, it, nameResolver) { expectedArrayElementType } }
                }
                coneTypeOrNull = expectedArrayElementType.createArrayType()
            }
        }

        else -> error("Unsupported annotation argument type: ${value.type} (expected $expectedType)")
    }
}

private fun const(kind: ConstantValueKind, value: Any?, typeRef: FirResolvedTypeRef): FirLiteralExpression {
    return buildLiteralExpression(null, kind, value, setType = true).apply { this.replaceConeTypeOrNull(typeRef.coneType) }
}
