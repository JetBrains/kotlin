/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.utils

import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.fir.KaSymbolByFirBuilder
import org.jetbrains.kotlin.fir.StandardTypes
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.builder.*
import org.jetbrains.kotlin.fir.expressions.impl.FirEmptyAnnotationArgumentMapping
import org.jetbrains.kotlin.fir.references.toResolvedCallableSymbol
import org.jetbrains.kotlin.fir.resolve.getContainingClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirEnumEntrySymbol
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.types.constructClassLikeType
import org.jetbrains.kotlin.fir.types.createOutArrayType
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.StandardClassIds
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
    fun restore(session: KaFirSession, guard: ConeTypeRecursionGuard): FirAnnotation?

    companion object {
        /**
         * Creates a pointer for the given [annotation], or returns `null` if the annotation cannot be represented by a pointer.
         *
         * The annotation is recreated from scratch on restoration, so its arguments are stored as values. To get them, a source
         * annotation is first resolved to [FirResolvePhase.ANNOTATION_ARGUMENTS] in the context of its containing declaration, which
         * may differ from the declaration the type was taken from (e.g., when the type is propagated via an implicit return type).
         * After that phase, the compiler leaves the [evaluated][FirExpressionEvaluator.evaluateAnnotationArguments] arguments in
         * [FirAnnotation.argumentMapping].
         */
        fun create(
            annotation: FirAnnotation,
            builder: KaSymbolByFirBuilder,
            guard: ConeTypeRecursionGuard
        ): FirAnnotationPointer? {
            if (annotation is FirAnnotationCall && annotation.arguments.isNotEmpty()) {
                annotation.containingDeclarationSymbol.lazyResolveToPhase(FirResolvePhase.ANNOTATION_ARGUMENTS)
            }

            val classId = annotation.toAnnotationNonErrorClassId(builder.rootSession) ?: return null
            val argumentPointers = annotation.argumentMapping.mapping.mapValues { [_, argument] ->
                FirAnnotationArgumentPointer.create(argument, builder, guard) ?: return null
            }

            return RecreatedFirAnnotationPointer(classId, argumentPointers)
        }
    }
}

/**
 * A pointer to a single annotation argument value.
 *
 * @see FirAnnotationPointer.create
 */
private interface FirAnnotationArgumentPointer {
    fun restore(session: KaFirSession, guard: ConeTypeRecursionGuard): FirExpression?

    companion object {
        /**
         * Creates a pointer for a single annotation argument, or returns `null` if the argument is not supported.
         *
         * The supported expression kinds are the ones which the compiler leaves in [FirAnnotation.argumentMapping] for valid arguments:
         * results of [FirExpressionEvaluator] for source annotations, and the expressions built for deserialized and Java annotations.
         * Constant expressions (`@Anno(myConst)`, `@Anno(1 + 2)`, `@Anno("a$myConst")`, `@Anno(4u)`) are already evaluated to literals,
         * and `arrayOf` calls to collection literals. Any other expression means the argument is invalid.
         */
        fun create(
            argument: FirExpression,
            builder: KaSymbolByFirBuilder,
            guard: ConeTypeRecursionGuard,
        ): FirAnnotationArgumentPointer? = when (argument) {
            // '@Anno(1)'
            is FirLiteralExpression -> LiteralArgumentPointer(argument.kind, argument.value, argument.prefix)

            // An enum entry in an already deserialized annotation
            is FirEnumEntryDeserializedAccessExpression -> EnumEntryArgumentPointer(argument.enumClassId, argument.enumEntryName)

            // A vararg parameter value ('@Anno("a", "b")')
            is FirVarargArgumentsExpression -> ArrayArgumentPointer.create(argument.arguments, builder, guard)

            // A collection literal ('@Anno(["a", "b"])'), which is also how the compiler represents an evaluated 'arrayOf("a", "b")' call
            is FirCollectionLiteral -> ArrayArgumentPointer.create(argument.arguments, builder, guard)

            // '@Anno(String::class)'
            is FirGetClassCall -> ClassLiteralArgumentPointer.create(argument, builder, guard)

            // A nested annotation which is already represented as an annotation and not as a constructor call
            is FirAnnotation -> FirAnnotationPointer.create(argument, builder, guard) as? FirAnnotationArgumentPointer

            // A source enum entry ('@Anno(MyEnum.A)') or a nested annotation in a source annotation ('@Anno(Nested(1))')
            is FirQualifiedAccessExpression -> when (val symbol = argument.calleeReference.toResolvedCallableSymbol()) {
                is FirEnumEntrySymbol -> EnumEntryArgumentPointer.create(symbol.callableId)
                is FirConstructorSymbol -> (argument as? FirFunctionCall)?.let { createNestedAnnotationPointer(it, symbol, builder, guard) }
                else -> null
            }

            else -> null
        }

        /**
         * Creates a pointer for a nested annotation written as a constructor call.
         *
         * The nested annotation is restored as a [FirAnnotation], which is indistinguishable from the original call for the annotation
         * value purposes.
         */
        private fun createNestedAnnotationPointer(
            argument: FirFunctionCall,
            constructorSymbol: FirConstructorSymbol,
            builder: KaSymbolByFirBuilder,
            guard: ConeTypeRecursionGuard,
        ): RecreatedFirAnnotationPointer? {
            val annotationSymbol = constructorSymbol.getContainingClassSymbol()?.fullyExpandedClass(builder.rootSession) ?: return null

            val argumentMapping = argument.resolvedArgumentMapping ?: return null
            val argumentPointers = argumentMapping.entries.associate { [expression, valueParameter] ->
                valueParameter.name to (create(expression, builder, guard) ?: return null)
            }

            return RecreatedFirAnnotationPointer(annotationSymbol.classId, argumentPointers)
        }
    }
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
    override fun restore(session: KaFirSession, guard: ConeTypeRecursionGuard): FirAnnotation? {
        val classSymbol = findAnnotationClassSymbol(classId, session) ?: return null

        val argumentMapping = if (argumentPointers.isEmpty()) {
            FirEmptyAnnotationArgumentMapping
        } else {
            buildAnnotationArgumentMapping {
                for ([name, argumentPointer] in argumentPointers) {
                    mapping[name] = argumentPointer.restore(session, guard) ?: return null
                }
            }
        }

        return buildFirAnnotation(classSymbol, argumentMapping)
    }
}

private class LiteralArgumentPointer(
    private val kind: ConstantValueKind,
    private val value: Any?,
    private val prefix: String?,
) : FirAnnotationArgumentPointer {
    override fun restore(session: KaFirSession, guard: ConeTypeRecursionGuard): FirExpression {
        return buildLiteralExpression(source = null, kind = kind, value = value, setType = true, prefix = prefix)
    }
}

private class EnumEntryArgumentPointer(
    private val enumClassId: ClassId,
    private val enumEntryName: Name,
) : FirAnnotationArgumentPointer {
    override fun restore(session: KaFirSession, guard: ConeTypeRecursionGuard): FirExpression {
        return buildEnumEntryDeserializedAccessExpression {
            enumClassId = this@EnumEntryArgumentPointer.enumClassId
            enumEntryName = this@EnumEntryArgumentPointer.enumEntryName
        }
    }

    companion object {
        fun create(callableId: CallableId): EnumEntryArgumentPointer? {
            val enumClassId = callableId.classId ?: return null
            return EnumEntryArgumentPointer(enumClassId, callableId.callableName)
        }
    }
}

/**
 * A pointer to a class literal argument (`@Anno(Array<String>::class)`).
 *
 * The referenced type is stored as a [ConeTypePointer], so type arguments, type aliases, and other type details survive restoration.
 */
private class ClassLiteralArgumentPointer(
    private val typePointer: ConeTypePointer<ConeKotlinType>,
) : FirAnnotationArgumentPointer {
    override fun restore(session: KaFirSession, guard: ConeTypeRecursionGuard): FirExpression? {
        val referencedType = guard.restorePointer(typePointer, session) ?: return null
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

    companion object {
        fun create(
            argument: FirGetClassCall,
            builder: KaSymbolByFirBuilder,
            guard: ConeTypeRecursionGuard,
        ): ClassLiteralArgumentPointer? {
            val referencedType = argument.getTargetType() ?: return null
            return ClassLiteralArgumentPointer(referencedType.createPointer(builder, guard))
        }
    }
}

private class ArrayArgumentPointer(
    private val elementPointers: List<FirAnnotationArgumentPointer>,
) : FirAnnotationArgumentPointer {
    override fun restore(session: KaFirSession, guard: ConeTypeRecursionGuard): FirExpression? {
        val elements = elementPointers.map { it.restore(session, guard) ?: return null }

        return buildCollectionLiteral {
            // The compiler does not preserve the exact array literal type for deserialized annotations either, see KT-62598
            coneTypeOrNull = StandardTypes.Any.createOutArrayType()
            argumentList = buildArgumentList {
                arguments += elements
            }
        }
    }

    companion object {
        /**
         * Creates a pointer for an array argument, such as a collection literal (`@Anno(["a"])`)
         * or a vararg parameter value (`@Anno("a", "b")`).
         */
        fun create(
            arguments: List<FirExpression>,
            builder: KaSymbolByFirBuilder,
            guard: ConeTypeRecursionGuard,
        ): ArrayArgumentPointer? {
            val elementPointers = arguments.map { FirAnnotationArgumentPointer.create(it, builder, guard) ?: return null }
            return ArrayArgumentPointer(elementPointers)
        }
    }
}
