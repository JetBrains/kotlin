/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.deserialization

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.StandardTypes
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
    annotations: List<ProtoBuf.Annotation>,
    nameResolver: NameResolver,
    languageFeature: LanguageFeature,
    useSiteTarget: AnnotationUseSiteTarget? = null,
): List<FirAnnotation>? =
    runIf(session.languageVersionSettings.supportsFeature(languageFeature)) {
        loadAnnotationsFromMetadataIfNotEmpty(session, annotations, nameResolver, useSiteTarget)
    }

/**
 * Loads annotations from metadata only when the given metadata [annotations] list is not empty.
 * Otherwise, returns null instead of an empty list to signal that the fallback annotation loading function should be called.
 *
 * Implemented for handling migration to the new approach of storing annotations both for klib and JVM backends.
 */
fun loadAnnotationsFromMetadataIfNotEmpty(
    session: FirSession,
    annotations: List<ProtoBuf.Annotation>,
    nameResolver: NameResolver,
    useSiteTarget: AnnotationUseSiteTarget? = null,
): List<FirAnnotation>? =
    annotations.ifNotEmpty {
        loadAnnotationsFromMetadata(session, annotations, nameResolver, useSiteTarget)
    }

fun loadAnnotationsFromMetadata(
    session: FirSession,
    annotations: List<ProtoBuf.Annotation>,
    nameResolver: NameResolver,
    useSiteTarget: AnnotationUseSiteTarget? = null,
): List<FirAnnotation> =
    annotations.map { deserializeAnnotation(session, it, nameResolver, useSiteTarget) }

fun <T : ExtendableMessage<T>> T.loadAnnotationsFromProtocol(
    session: FirSession,
    extension: GeneratedMessageLite.GeneratedExtension<T, List<ProtoBuf.Annotation>>?,
    nameResolver: NameResolver,
    useSiteTarget: AnnotationUseSiteTarget? = null
): List<FirAnnotation> {
    if (extension == null) return emptyList()
    val annotations = getExtension(extension)
    return annotations.map { deserializeAnnotation(session, it, nameResolver, useSiteTarget) }
}

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
            val value = it.value.toFirExpression(session, nameResolver)
            name to value
        }.toMap(mapping)
    }
}

internal fun ProtoBuf.Annotation.Argument.Value.toFirExpression(session: FirSession, nameResolver: NameResolver): FirExpression {
    val isUnsigned = Flags.IS_UNSIGNED.get(this.flags)

    return when (this.type) {
        BYTE -> {
            val kind = if (isUnsigned) ConstantValueKind.UnsignedByte else ConstantValueKind.Byte
            const(kind, this.intValue.toByte(), session.builtinTypes.byteType)
        }

        SHORT -> {
            val kind = if (isUnsigned) ConstantValueKind.UnsignedShort else ConstantValueKind.Short
            const(kind, this.intValue.toShort(), session.builtinTypes.shortType)
        }

        INT -> {
            val kind = if (isUnsigned) ConstantValueKind.UnsignedInt else ConstantValueKind.Int
            const(kind, this.intValue.toInt(), session.builtinTypes.intType)
        }

        LONG -> {
            val kind = if (isUnsigned) ConstantValueKind.UnsignedLong else ConstantValueKind.Long
            const(kind, this.intValue, session.builtinTypes.longType)
        }

        CHAR -> const(ConstantValueKind.Char, this.intValue.toInt().toChar(), session.builtinTypes.charType)
        FLOAT -> const(ConstantValueKind.Float, this.floatValue, session.builtinTypes.floatType)
        DOUBLE -> const(ConstantValueKind.Double, this.doubleValue, session.builtinTypes.doubleType)
        BOOLEAN -> const(ConstantValueKind.Boolean, (this.intValue != 0L), session.builtinTypes.booleanType)
        STRING -> const(ConstantValueKind.String, nameResolver.getString(this.stringValue), session.builtinTypes.stringType)
        ANNOTATION -> deserializeAnnotation(session, this.annotation, nameResolver)
        CLASS -> buildGetClassCall {
            val classId = nameResolver.getClassId(this@toFirExpression.classId)
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
            enumClassId = nameResolver.getClassId(this@toFirExpression.classId)
            enumEntryName = nameResolver.getName(this@toFirExpression.enumValueId)
        }
        ARRAY -> buildCollectionLiteral {
            // For the array literal type, we use `Array<Any>` as an approximation. Later FIR2IR will calculate a more precise
            // type. See KT-62598.
            // FIR provides no guarantees on having the exact type of deserialized array literals in annotations, including
            // non-empty ones.
            coneTypeOrNull = StandardTypes.Any.createOutArrayType()
            argumentList = buildArgumentList {
                this@toFirExpression.arrayElementList.mapTo(arguments) { it.toFirExpression(session, nameResolver) }
            }
        }
        else -> error("Unsupported annotation argument type: ${this.type}")
    }
}

private fun const(kind: ConstantValueKind, value: Any?, typeRef: FirResolvedTypeRef): FirLiteralExpression {
    return buildLiteralExpression(null, kind, value, setType = true).apply { this.replaceConeTypeOrNull(typeRef.coneType) }

}
