/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.utils

import com.intellij.psi.SmartPsiElementPointer
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.fir.symbols.cameFromKotlinLibrary
import org.jetbrains.kotlin.analysis.api.impl.base.symbols.pointers.createCompatibleSmartPointer
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getOrBuildFirSafe
import org.jetbrains.kotlin.descriptors.isAnnotationClass
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.StandardTypes
import org.jetbrains.kotlin.fir.analysis.checkers.classKind
import org.jetbrains.kotlin.fir.declarations.getTargetType
import org.jetbrains.kotlin.fir.declarations.toAnnotationClassIdSafe
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.builder.*
import org.jetbrains.kotlin.fir.expressions.impl.FirEmptyAnnotationArgumentMapping
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.types.constructClassLikeType
import org.jetbrains.kotlin.fir.types.createOutArrayType
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.types.ConstantValueKind

/**
 * A pointer to a single type [FirAnnotation].
 * Does not hold references to internal compiler abstractions, so it can be restored in a different
 * [KaSession][org.jetbrains.kotlin.analysis.api.KaSession].
 *
 * @see ConeAnnotationPointer
 */
internal interface FirAnnotationPointer {
    /**
     * Restores the original annotation when possible, and returns `null` otherwise.
     */
    fun restore(session: KaFirSession): FirAnnotation?

    companion object {
        /**
         * Creates a pointer for the given [annotation], or returns `null` if the annotation cannot be represented by a pointer.
         *
         * An annotation written in source code is pointed to by its [KtAnnotationEntry], which is both cheap and lossless.
         * Everything else (annotations of library types, of compiler- and plugin-generated declarations, and of the types created by
         * [KaTypeCreator][org.jetbrains.kotlin.analysis.api.types.typeCreation.KaTypeCreator]) is recreated from scratch on restoration.
         */
        fun create(annotation: FirAnnotation, session: FirSession): FirAnnotationPointer? {
            val annotationEntry = (annotation.psi as? KtAnnotationEntry)?.takeUnless { it.cameFromKotlinLibrary }
            if (annotationEntry != null) {
                return PsiFirAnnotationPointer(createCompatibleSmartPointer(annotationEntry))
            }

            return createFirBasedPointer(annotation, session)
        }

        private fun createFirBasedPointer(annotation: FirAnnotation, session: FirSession): RecreatedFirAnnotationPointer? {
            val classId = annotation.toAnnotationClassIdSafe(session) ?: return null
            val argumentPointers = annotation.argumentMapping.mapping.mapValues { [_, argument] ->
                createArgumentPointer(argument, session) ?: return null
            }

            return RecreatedFirAnnotationPointer(classId, argumentPointers)
        }

        /**
         * Creates a pointer for a single annotation argument, or returns `null` if the argument is not supported.
         *
         * Only the expressions which annotation deserializers produce are supported, as those are the only ones a PSI-less annotation
         * may consist of. Arbitrary source expressions are handled by [PsiFirAnnotationPointer] instead.
         */
        private fun createArgumentPointer(argument: FirExpression, session: FirSession): FirAnnotationArgumentPointer? = when (argument) {
            is FirLiteralExpression -> LiteralArgumentPointer(argument.kind, argument.value, argument.prefix)

            is FirEnumEntryDeserializedAccessExpression -> EnumEntryArgumentPointer(argument.enumClassId, argument.enumEntryName)

            is FirGetClassCall -> {
                val referencedType = argument.getTargetType() as? ConeClassLikeType ?: return null
                val classId = referencedType.lookupTag.classId
                ClassLiteralArgumentPointer(classId)
            }

            is FirCollectionLiteral -> {
                val elementPointers = argument.arguments.map { createArgumentPointer(it, session) ?: return null }
                ArrayArgumentPointer(elementPointers)
            }

            is FirAnnotation -> createFirBasedPointer(argument, session)

            else -> null
        }
    }
}

private class PsiFirAnnotationPointer(
    private val psiPointer: SmartPsiElementPointer<out KtElement>,
) : FirAnnotationPointer {
    override fun restore(session: KaFirSession): FirAnnotation? {
        val annotationEntry = psiPointer.element as? KtAnnotationEntry ?: return null
        return annotationEntry.getOrBuildFirSafe<FirAnnotation>(session.resolutionFacade)
    }
}

/**
 * A pointer to a single annotation argument value.
 *
 * @see FirAnnotationPointer.create
 */
private interface FirAnnotationArgumentPointer {
    fun restore(session: KaFirSession): FirExpression?
}

/**
 * A pointer which stores the annotation class id together with its argument value and restores FIR annotation from scratch.
 *
 * As [FirAnnotation] is a [FirExpression], the same pointer also represents a nested annotation argument.
 */
private class RecreatedFirAnnotationPointer(
    private val classId: ClassId,
    private val argumentPointers: Map<Name, FirAnnotationArgumentPointer>,
) : FirAnnotationPointer, FirAnnotationArgumentPointer {
    override fun restore(session: KaFirSession): FirAnnotation? {
        val classSymbol = session.firSession.symbolProvider.getClassLikeSymbolByClassId(classId) ?: return null
        if (classSymbol.classKind?.isAnnotationClass != true) {
            return null
        }

        val argumentMapping = if (argumentPointers.isEmpty()) {
            FirEmptyAnnotationArgumentMapping
        } else {
            buildAnnotationArgumentMapping {
                for ([name, argumentPointer] in argumentPointers) {
                    mapping[name] = argumentPointer.restore(session) ?: return null
                }
            }
        }

        return buildAnnotation {
            annotationTypeRef = buildResolvedTypeRef {
                coneType = classSymbol.defaultType()
            }

            this.argumentMapping = argumentMapping
        }
    }
}

private class LiteralArgumentPointer(
    private val kind: ConstantValueKind,
    private val value: Any?,
    private val prefix: String?,
) : FirAnnotationArgumentPointer {
    override fun restore(session: KaFirSession): FirExpression {
        return buildLiteralExpression(source = null, kind = kind, value = value, setType = true, prefix = prefix)
    }
}

private class EnumEntryArgumentPointer(
    private val enumClassId: ClassId,
    private val enumEntryName: Name,
) : FirAnnotationArgumentPointer {
    override fun restore(session: KaFirSession): FirExpression {
        return buildEnumEntryDeserializedAccessExpression {
            enumClassId = this@EnumEntryArgumentPointer.enumClassId
            enumEntryName = this@EnumEntryArgumentPointer.enumEntryName
        }
    }
}

private class ClassLiteralArgumentPointer(private val classId: ClassId) : FirAnnotationArgumentPointer {
    override fun restore(session: KaFirSession): FirExpression {
        val referencedType = classId.constructClassLikeType()
        val kClassType = StandardClassIds.KClass.constructClassLikeType(arrayOf(referencedType))

        return buildGetClassCall {
            argumentList = buildUnaryArgumentList(
                buildClassReferenceExpression {
                    classTypeRef = buildResolvedTypeRef { coneType = referencedType }
                    coneTypeOrNull = kClassType
                }
            )

            coneTypeOrNull = kClassType
        }
    }
}

private class ArrayArgumentPointer(
    private val elementPointers: List<FirAnnotationArgumentPointer>,
) : FirAnnotationArgumentPointer {
    override fun restore(session: KaFirSession): FirExpression? {
        val elements = elementPointers.map { it.restore(session) ?: return null }

        return buildCollectionLiteral {
            // The compiler does not preserve the exact array literal type for deserialized annotations either, see KT-62598
            coneTypeOrNull = StandardTypes.Any.createOutArrayType()
            argumentList = buildArgumentList {
                arguments += elements
            }
        }
    }
}
