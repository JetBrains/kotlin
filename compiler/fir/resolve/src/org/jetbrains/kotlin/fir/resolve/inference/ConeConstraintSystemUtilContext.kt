/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.inference

import org.jetbrains.kotlin.fir.declarations.FirAnonymousFunction
import org.jetbrains.kotlin.fir.resolve.calls.ConeLambdaWithTypeVariableAsExpectedTypeAtom
import org.jetbrains.kotlin.fir.resolve.calls.ConePostponedResolvedAtom
import org.jetbrains.kotlin.fir.resolve.inference.model.ConeArgumentConstraintPosition
import org.jetbrains.kotlin.fir.resolve.inference.model.ConeFixVariableConstraintPosition
import org.jetbrains.kotlin.fir.symbols.ConeTypeParameterLookupTag
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.resolve.calls.inference.components.ConstraintSystemUtilContext
import org.jetbrains.kotlin.resolve.calls.inference.components.PostponedArgumentInputTypesResolver
import org.jetbrains.kotlin.resolve.calls.inference.model.ArgumentConstraintPosition
import org.jetbrains.kotlin.resolve.calls.inference.model.FixVariableConstraintPosition
import org.jetbrains.kotlin.resolve.calls.model.PostponedAtomWithRevisableExpectedType
import org.jetbrains.kotlin.types.model.KotlinTypeMarker
import org.jetbrains.kotlin.types.model.TypeVariableMarker

object ConeConstraintSystemUtilContext : ConstraintSystemUtilContext {
    override fun TypeVariableMarker.shouldBeFlexible(): Boolean {
        if (this !is ConeTypeVariable) return false
        val typeParameter =
            (this.typeConstructor.originalTypeParameter as? ConeTypeParameterLookupTag)?.typeParameterSymbol?.fir ?: return false

        // TODO: Take a look at org.jetbrains.kotlin.resolve.calls.components.CreateFreshVariablesSubstitutor.shouldBeFlexible
        return typeParameter.bounds.any { it.coneType is ConeFlexibleType }
    }

    override fun TypeVariableMarker.hasOnlyInputTypesAttribute(): Boolean {
        if (this !is ConeTypeParameterBasedTypeVariable) return false
        return typeParameterSymbol.resolvedAnnotationClassIds.any { it == StandardClassIds.Annotations.OnlyInputTypes }
    }

    /**
     * This function is intended to unwrap captured types, converting e.g. `Captured(in T)` to just `T`.
     *
     * K2 does not implement this logic deliberately.
     *
     * It influences code like /compiler/testData/diagnostics/testsWithStdLib/inference/annotationsForResolve/onlyInputTypesUpperBound.kt
     * Without uncapturing, we approximate types like `Captured(in T)` to something like `Any?`, keeping the code green.
     * With uncapturing, we unwrap `Captured(in T)` to simply `T` and report `TYPE_INFERENCE_ONLY_INPUT_TYPES_ERROR`,
     * in this situation (it's red in K1) and in some others (green in K1),
     * e.g. compiler/fir/analysis-tests/testData/resolve/inference/onlyInputTypesCapturedTypeWithRecursiveBounds.kt.
     * Taking into account differences in captured types usage in K1 & K2, reimplementing K1 logic here will break some code.
     * As we are not sure should it really be red or not, we decided to go without uncapturing.
     *
     * The aforementioned onlyInputTypesUpperBound.kt (K1 red -> K2 green) is a K2 potential feature.
     * However, as the `@OnlyInputTypes` annotation is internal, this test itself does not change anything for us.
     * An equivalent test with a stdlib function does not change behavior:
     *
     * ```
     * fun <T> foo(i: Map<in T, *>, o: T) {
     *     i.bar(o) // K1: TYPE_INFERENCE_ONLY_INPUT_TYPES_ERROR, K2: OK
     *     i.containsKey(o) // K1 & K2: Ok, as the member containsKey (not an extension) is called here
     * }
     *
     * @Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
     * fun <@kotlin.internal.OnlyInputTypes K> Map<out K, *>.bar(o: K): K = TODO()
     * // (similar Map<out K, *>.containsKey is declared in the stdlib)
     * ```
     */
    override fun KotlinTypeMarker.unCapture(): KotlinTypeMarker {
        require(this is ConeKotlinType)
        return this
    }

    override fun TypeVariableMarker.isReified(): Boolean {
        return this is ConeTypeParameterBasedTypeVariable && typeParameterSymbol.fir.isReified
    }

    override fun KotlinTypeMarker.refineType(): KotlinTypeMarker {
        return this
    }

    override fun createArgumentConstraintPosition(argument: PostponedAtomWithRevisableExpectedType): ArgumentConstraintPosition<*> {
        require(argument is ConePostponedResolvedAtom) {
            "${argument::class}"
        }
        return ConeArgumentConstraintPosition(argument.expression)
    }

    override fun <T> createFixVariableConstraintPosition(variable: TypeVariableMarker, atom: T): FixVariableConstraintPosition<T> {
        require(atom == null)
        @Suppress("UNCHECKED_CAST")
        return ConeFixVariableConstraintPosition(variable) as FixVariableConstraintPosition<T>
    }

    override fun extractLambdaParameterTypesFromDeclaration(declaration: PostponedAtomWithRevisableExpectedType): List<ConeKotlinType?>? {
        require(declaration is ConePostponedResolvedAtom)
        return when (declaration) {
            is ConeLambdaWithTypeVariableAsExpectedTypeAtom -> {
                val anonymousFunction = declaration.anonymousFunction
                return if (anonymousFunction.isLambda) { // lambda - must return null in case of absent parameters
                    if (anonymousFunction.valueParameters.isNotEmpty())
                        anonymousFunction.collectDeclaredValueParameterTypes()
                    else null
                } else { // function expression - all types are explicit, shouldn't return null
                    buildList {
                        anonymousFunction.receiverParameter?.typeRef?.coneType?.let { add(it) }
                        addAll(anonymousFunction.collectDeclaredValueParameterTypes())
                    }
                }
            }
            else -> null
        }
    }

    private fun FirAnonymousFunction.collectDeclaredValueParameterTypes(): List<ConeKotlinType?> =
        valueParameters.map { it.returnTypeRef.coneTypeSafe() }

    override fun PostponedAtomWithRevisableExpectedType.isFunctionExpression(): Boolean {
        require(this is ConePostponedResolvedAtom)
        return this is ConeLambdaWithTypeVariableAsExpectedTypeAtom && !this.anonymousFunction.isLambda
    }

    override fun PostponedAtomWithRevisableExpectedType.isFunctionExpressionWithReceiver(): Boolean {
        require(this is ConePostponedResolvedAtom)
        return this is ConeLambdaWithTypeVariableAsExpectedTypeAtom &&
                !this.anonymousFunction.isLambda &&
                this.anonymousFunction.receiverParameter?.typeRef?.coneType != null
    }

    override fun PostponedAtomWithRevisableExpectedType.isLambda(): Boolean {
        require(this is ConePostponedResolvedAtom)
        return this is ConeLambdaWithTypeVariableAsExpectedTypeAtom && this.anonymousFunction.isLambda
    }

    override fun createTypeVariableForLambdaReturnType(): TypeVariableMarker {
        return ConeTypeVariableForPostponedAtom(PostponedArgumentInputTypesResolver.TYPE_VARIABLE_NAME_FOR_LAMBDA_RETURN_TYPE)
    }

    override fun createTypeVariableForLambdaParameterType(
        argument: PostponedAtomWithRevisableExpectedType,
        index: Int
    ): TypeVariableMarker {
        return ConeTypeVariableForLambdaParameterType(
            PostponedArgumentInputTypesResolver.TYPE_VARIABLE_NAME_PREFIX_FOR_LAMBDA_PARAMETER_TYPE + index
        )
    }

    override fun createTypeVariableForCallableReferenceParameterType(
        argument: PostponedAtomWithRevisableExpectedType,
        index: Int
    ): TypeVariableMarker {
        return ConeTypeVariableForPostponedAtom(
            PostponedArgumentInputTypesResolver.TYPE_VARIABLE_NAME_PREFIX_FOR_CR_PARAMETER_TYPE + index
        )
    }

    override fun createTypeVariableForCallableReferenceReturnType(): TypeVariableMarker {
        return ConeTypeVariableForPostponedAtom(PostponedArgumentInputTypesResolver.TYPE_VARIABLE_NAME_FOR_LAMBDA_RETURN_TYPE)
    }

    override val isForcedConsiderExtensionReceiverFromConstrainsInLambda: Boolean
        get() = true

    override val isForcedAllowForkingInferenceSystem: Boolean
        get() = true
}
