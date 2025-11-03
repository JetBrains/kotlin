/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.diagnostics

import org.jetbrains.kotlin.diagnostics.DiagnosticBaseContext
import org.jetbrains.kotlin.diagnostics.rendering.RenderingContext
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.SessionHolder
import org.jetbrains.kotlin.fir.declarations.utils.isInner
import org.jetbrains.kotlin.fir.declarations.utils.isLocal
import org.jetbrains.kotlin.fir.diagnostics.ConeCannotInferTypeParameterType
import org.jetbrains.kotlin.fir.renderer.ConeIdShortRenderer
import org.jetbrains.kotlin.fir.renderer.ConeTypeRendererForReadability
import org.jetbrains.kotlin.fir.resolve.getContainingClassSymbol
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.symbols.ConeTypeParameterLookupTag
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.types.ConeClassLikeErrorLookupTag
import org.jetbrains.kotlin.fir.types.ConeClassLikeLookupTag
import org.jetbrains.kotlin.fir.types.ConeIntersectionType
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.forEachType
import org.jetbrains.kotlin.fir.types.getConstructor
import org.jetbrains.kotlin.fir.types.lowerBoundIfFlexible
import org.jetbrains.kotlin.fir.types.renderReadableWithFqNames
import org.jetbrains.kotlin.types.model.TypeConstructorMarker

/**
 * Used with [org.jetbrains.kotlin.diagnostics.rendering.RenderingContext.get] inside a [org.jetbrains.kotlin.diagnostics.rendering.ContextDependentRenderer] to render [org.jetbrains.kotlin.fir.types.ConeKotlinType]s.
 *
 * Features on top of regular rendering with [org.jetbrains.kotlin.fir.renderer.ConeTypeRenderer]:
 *
 * - prints fully qualified class names when they are ambiguous
 * - adds `#1`, `#2`, etc. to captured types or type parameters with they are ambiguous
 * - adds `(of fun/class ...)` to type parameters
 * - prints inner classes with generic outer types in the correct way, i.e., with the type arguments on the outer types when necessary
 */
object FirAdaptiveTypeRenderingKey : RenderingContext.Key<Map<ConeKotlinType, String>>("ADAPTIVE_RENDERED_TYPES") {
    override fun compute(objectsToRender: Collection<Any?>, diagnosticContext: DiagnosticBaseContext): Map<ConeKotlinType, String> {
        val coneTypes = objectsToRender.filterIsInstance<ConeKotlinType>() +
                objectsToRender.filterIsInstance<Iterable<*>>().flatMap { it.filterIsInstance<ConeKotlinType>() }

        val constructors = buildSet {
            coneTypes.forEach {
                it.forEachType { typeWithinIt ->
                    val lowerBound = typeWithinIt.lowerBoundIfFlexible()

                    if (lowerBound !is ConeIntersectionType) {
                        add(lowerBound.getConstructor())
                    }
                }
            }
        }

        val simpleRepresentationsByConstructor: Map<TypeConstructorMarker, String> = constructors.associateWith {
            buildString { ConeTypeRendererForReadability(this) { ConeIdShortRenderer() }.renderConstructor(it.delegatedConstructorOrSelf()) }
        }

        val constructorsByRepresentation: Map<String, List<TypeConstructorMarker>> =
            simpleRepresentationsByConstructor.entries.groupBy({ it.value }, { it.key })

        val session = (diagnosticContext as SessionHolder).session

        val finalRepresentationsByConstructor: Map<TypeConstructorMarker, String?> = constructors.associateWith {
            it.toFinalRepresentation(
                representation = simpleRepresentationsByConstructor.getValue(it),
                typesWithSameRepresentation = constructorsByRepresentation.getValue(simpleRepresentationsByConstructor.getValue(it)),
                session = session
            )
        }

        return coneTypes.associateWith {
            it.renderReadableWithFqNames(finalRepresentationsByConstructor)
        }
    }

    private fun TypeConstructorMarker.toFinalRepresentation(
        representation: String,
        typesWithSameRepresentation: List<TypeConstructorMarker>,
        session: FirSession,
    ): String? {
        val isAmbiguous = typesWithSameRepresentation.size > 1
        val isError = this is ConeClassLikeErrorLookupTag
        val isClassLike = this is ConeClassLikeLookupTag && !isError

        return buildString {
            if (isError && diagnostic is ConeCannotInferTypeParameterType) {
                append("uninferred ")
            }

            if (isClassLike) {
                if (isAmbiguous) {
                    append(if (classId.packageFqName.isRoot) "<root>" else classId.packageFqName.asString())
                    append(".")
                }

                val symbol = toSymbol(session) ?: return null
                appendClassLikeTemplate(symbol)
            } else {
                append(representation)
            }

            if (!isClassLike && !isError && isAmbiguous) {
                append('#')
                append(typesWithSameRepresentation.indexOf(this@toFinalRepresentation) + 1)
            }
            // Placeholder for nullability marker, like "", "?", "!", or maybe something else in future
            val nullabilityPlaceHolderIndex = if (isClassLike) {
                toSymbol(session)?.typeParameterSymbols?.size ?: 0
            } else {
                0
            }
            append("{$nullabilityPlaceHolderIndex}")

            val typeParameterSymbol =
                ((this@toFinalRepresentation as? ConeClassLikeErrorLookupTag)?.delegatedType?.lowerBoundIfFlexible()
                    ?.getConstructor() as? ConeTypeParameterLookupTag)?.typeParameterSymbol
                    ?: (this@toFinalRepresentation as? ConeTypeParameterLookupTag)?.typeParameterSymbol

            if (typeParameterSymbol != null) {
                append(" (of ")
                append(FirDiagnosticRenderers.TYPE_PARAMETER_OWNER_SYMBOL.render(typeParameterSymbol.containingDeclarationSymbol))
                append(')')
            }
        }
    }

    private fun StringBuilder.appendClassLikeTemplate(symbol: FirClassLikeSymbol<*>) {
        data class ClassInfo(
            val symbol: FirClassLikeSymbol<*>,
            val genericsStartingIndex: Int,
            val typeArgumentCount: Int,
        )

        // Type arguments of inner classes with generic outer classes are mapped in the following way:
        // Foo<A, B>.Bar<C, D>.Baz<E, F> => Baz<E, F, C, D, A, B>
        // We produce the pattern `Foo<{0}, {1}>.Bar<{2}, {3}>.Baz<{4}, {5}>{6}`
        // ({6} being the placeholder for the nullability marker which is added by the caller).
        // The actual type arguments are inserted by ConeTypeRendererForReadability.

        val stack = mutableListOf<ClassInfo>()
        var current: FirClassLikeSymbol<*>? = symbol
        var renderTypeArguments = true
        var placeholderIndex = 0

        while (current != null) {
            val parent = current.getContainingClassSymbol()
            val typeArgumentCount = when {
                !renderTypeArguments -> 0
                // Most outer class of local class can capture additional type arguments from the containing function.
                // For simplicity, we treat them like they're the classes own type parameters.
                parent == null && current.isLocal -> current.typeParameterSymbols.size
                else -> current.ownTypeParameterSymbols.size
            }
            stack.add(ClassInfo(current, placeholderIndex, typeArgumentCount))
            placeholderIndex += typeArgumentCount
            // Type argument placeholders mustn't be added for outer classes of non-inner types.
            renderTypeArguments = current.isInner
            current = parent
        }

        for ((symbol, genericsStartingIndex, typeArgumentCount) in stack.asReversed()) {
            append(symbol.classId.shortClassName)
            if (typeArgumentCount != 0) {
                append("<")
                for (i in 0..<typeArgumentCount) {
                    if (i != 0) append(", ")
                    append("{${i + genericsStartingIndex}}")
                }
                append(">")
            }

            if (symbol != stack.first().symbol) {
                append(".")
            }
        }
    }

    private fun TypeConstructorMarker.delegatedConstructorOrSelf(): TypeConstructorMarker {
        return if (this is ConeClassLikeErrorLookupTag && this.diagnostic is ConeCannotInferTypeParameterType) {
            this.delegatedType?.lowerBoundIfFlexible()?.getConstructor() ?: this
        } else {
            this
        }
    }
}