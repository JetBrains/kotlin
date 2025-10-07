/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.deserialization

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.builder.*
import org.jetbrains.kotlin.fir.languageVersionSettings
import org.jetbrains.kotlin.fir.symbols.lazyDeclarationResolver
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.ProtoBuf.Annotation.Argument.Value.Type.*
import org.jetbrains.kotlin.metadata.deserialization.Flags
import org.jetbrains.kotlin.metadata.deserialization.NameResolver
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.protobuf.GeneratedMessageLite
import org.jetbrains.kotlin.protobuf.GeneratedMessageLite.ExtendableMessage
import org.jetbrains.kotlin.serialization.deserialization.getClassId
import org.jetbrains.kotlin.serialization.deserialization.getName
import org.jetbrains.kotlin.types.ConstantValueKind
import org.jetbrains.kotlin.utils.addToStdlib.ifNotEmpty
import org.jetbrains.kotlin.utils.addToStdlib.runIf

/**
 * Loads annotations from metadata only when the given [languageFeature] is enabled and metadata [annotations] list is not empty.
 * Otherwise, returns null instead of an empty list to signal that the fallback annotation loading function should be called.
 */
fun loadAnnotationsFromMetadataGuarded(
    session: FirSession,
    flags: Int?,
    annotations: List<ProtoBuf.Annotation>,
    nameResolver: NameResolver,
    languageFeature: LanguageFeature,
    useSiteTarget: AnnotationUseSiteTarget? = null,
): List<FirAnnotation>? =
    runIf(session.languageVersionSettings.supportsFeature(languageFeature)) {
        loadAnnotationsFromMetadataIfNotEmpty(session, flags, annotations, nameResolver, useSiteTarget)
    }

/**
 * Loads annotations from metadata only when the given metadata [annotations] list is not empty.
 * Otherwise, returns null instead of an empty list to signal that the fallback annotation loading function should be called.
 *
 * Implemented for handling migration to the new approach of storing annotations both for klib and JVM backends.
 */
fun loadAnnotationsFromMetadataIfNotEmpty(
    session: FirSession,
    flags: Int?,
    annotations: List<ProtoBuf.Annotation>,
    nameResolver: NameResolver,
    useSiteTarget: AnnotationUseSiteTarget? = null,
): List<FirAnnotation>? =
    annotations.ifNotEmpty {
        loadAnnotationsFromMetadata(session, flags, annotations, nameResolver, useSiteTarget)
    }

fun loadAnnotationsFromMetadata(
    session: FirSession,
    flags: Int?,
    annotations: List<ProtoBuf.Annotation>,
    nameResolver: NameResolver,
    useSiteTarget: AnnotationUseSiteTarget? = null,
): List<FirAnnotation> =
    if (isAnnotationsFlagNotSet(flags))
        emptyList()
    else
        annotations.map { deserializeAnnotation(session, it, nameResolver, useSiteTarget) }

fun <T : ExtendableMessage<T>> T.loadAnnotationsFromProtocol(
    session: FirSession,
    extension: GeneratedMessageLite.GeneratedExtension<T, List<ProtoBuf.Annotation>>?,
    flags: Int?,
    nameResolver: NameResolver,
    useSiteTarget: AnnotationUseSiteTarget? = null
): List<FirAnnotation> {
    if (extension == null || isAnnotationsFlagNotSet(flags)) return emptyList()
    val annotations = getExtension(extension)
    return annotations.map { deserializeAnnotation(session, it, nameResolver, useSiteTarget) }
}

private fun isAnnotationsFlagNotSet(flags: Int?): Boolean =
    flags != null && !Flags.HAS_ANNOTATIONS.get(flags)

private fun deserializeAnnotation(
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
            this.argumentMapping = createArgumentMapping(session, proto, nameResolver)
        }
        useSiteTarget?.let {
            this.useSiteTarget = it
        }
    }
}

private fun createArgumentMapping(
    session: FirSession, proto: ProtoBuf.Annotation, nameResolver: NameResolver,
): FirAnnotationArgumentMapping {
    return buildAnnotationArgumentMapping build@{
        if (proto.argumentCount == 0) return@build
        proto.argumentList.mapNotNull {
            val name = nameResolver.getName(it.nameId)
            val value = resolveValue(session, it.value, nameResolver)
            name to value
        }.toMap(mapping)
    }
}

private fun resolveValue(session: FirSession, value: ProtoBuf.Annotation.Argument.Value, nameResolver: NameResolver): FirExpression {
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
        ARRAY -> buildCollectionLiteral {
            // For the array literal type, we use `Array<Any>` as an approximation. Later FIR2IR will calculate a more precise
            // type. See KT-62598.
            // FIR provides no guarantees on having the exact type of deserialized array literals in annotations, including
            // non-empty ones.
            coneTypeOrNull = StandardClassIds.Any.constructClassLikeType().createOutArrayType()
            argumentList = buildArgumentList {
                value.arrayElementList.mapTo(arguments) { resolveValue(session, it, nameResolver) }
            }
        }
        else -> error("Unsupported annotation argument type: ${value.type}")
    }
}

private fun const(kind: ConstantValueKind, value: Any?, typeRef: FirResolvedTypeRef): FirLiteralExpression {
    return buildLiteralExpression(null, kind, value, setType = true).apply { this.replaceConeTypeOrNull(typeRef.coneType) }

}
