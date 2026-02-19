/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components

import org.jetbrains.kotlin.analysis.api.components.KaSubtypingErrorTypePolicy
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.fir.symbols.KaFirTypeParameterSymbol
import org.jetbrains.kotlin.analysis.api.fir.types.KaFirType
import org.jetbrains.kotlin.analysis.api.fir.utils.firSymbol
import org.jetbrains.kotlin.analysis.api.fir.utils.isSubclassOf
import org.jetbrains.kotlin.analysis.api.impl.base.components.KaBaseTypeRelationChecker
import org.jetbrains.kotlin.analysis.api.lifetime.assertIsValidAndAccessible
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KaCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaClassLikeSymbol
import org.jetbrains.kotlin.analysis.api.symbols.receiverType
import org.jetbrains.kotlin.analysis.api.symbols.typeParameters
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.analysis.api.types.symbol
import org.jetbrains.kotlin.fir.declarations.fullyExpandedClass
import org.jetbrains.kotlin.fir.resolve.calls.overloads.ConeSimpleConstraintSystemImpl
import org.jetbrains.kotlin.fir.resolve.inference.inferenceComponents
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.resolve.toRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.ConeTypeParameterLookupTag
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.types.AbstractTypeChecker
import org.jetbrains.kotlin.utils.exceptions.checkWithAttachment

internal class KaFirTypeRelationChecker(
    override val analysisSessionProvider: () -> KaFirSession
) : KaBaseTypeRelationChecker<KaFirSession>(), KaFirSessionComponent {
    override fun KaType.semanticallyEquals(other: KaType, errorTypePolicy: KaSubtypingErrorTypePolicy): Boolean = withValidityAssertion {
        other.assertIsValidAndAccessible()
        check(this is KaFirType)
        check(other is KaFirType)
        return AbstractTypeChecker.equalTypes(
            createTypeCheckerContext(errorTypePolicy),
            this.coneType,
            other.coneType,
        )
    }

    override fun KaType.isSubtypeOf(supertype: KaType, errorTypePolicy: KaSubtypingErrorTypePolicy): Boolean = withValidityAssertion {
        supertype.assertIsValidAndAccessible()
        check(this is KaFirType)
        check(supertype is KaFirType)
        return AbstractTypeChecker.isSubtypeOf(
            createTypeCheckerContext(errorTypePolicy),
            this.coneType,
            supertype.coneType,
        )
    }

    override fun KaType.isClassSubtypeOf(classId: ClassId, errorTypePolicy: KaSubtypingErrorTypePolicy): Boolean {
        val superclassSymbol = analysisSession.firSession.symbolProvider.getClassLikeSymbolByClassId(classId)
            ?: return errorTypePolicy == KaSubtypingErrorTypePolicy.LENIENT

        return isClassSubtypeOf(superclassSymbol, errorTypePolicy)
    }

    override fun KaType.isClassSubtypeOf(symbol: KaClassLikeSymbol, errorTypePolicy: KaSubtypingErrorTypePolicy): Boolean =
        isClassSubtypeOf(symbol.firSymbol, errorTypePolicy)

    private fun KaType.isClassSubtypeOf(
        superclassSymbol: FirClassLikeSymbol<*>,
        errorTypePolicy: KaSubtypingErrorTypePolicy,
    ): Boolean {
        require(this is KaFirType)

        // We have to prepare the type to e.g. expand type aliases to be in line with `equalTypes` and `isSubtypeOf`.
        val preparedType = AbstractTypeChecker.prepareType(analysisSession.firSession.typeContext, coneType)
        checkWithAttachment(
            preparedType is ConeKotlinType,
            { "Expected ${ConeKotlinType::class.simpleName}, but got ${preparedType::class.simpleName}." },
        ) {
            withEntry("type", preparedType.toString())
        }

        // See the subtyping rules for flexible types: https://kotlinlang.org/spec/type-system.html#subtyping-for-flexible-types.
        val classType = preparedType.lowerBoundIfFlexible() as? ConeClassLikeType

        return if (classType != null) {
            classType.isSubtypeOf(superclassSymbol, errorTypePolicy)
        } else {
            // If the left-hand side is not a class-like type, we have to fall back to full subtyping. For example, a type parameter
            // `T : List<String>` would still be a subtype of `Iterable<*>`, as would an intersection type `Interface & List<String>`.
            preparedType.isSubtypeOf(superclassSymbol, errorTypePolicy)
        }
    }

    private fun ConeClassLikeType.isSubtypeOf(
        superclassSymbol: FirClassLikeSymbol<*>,
        errorTypePolicy: KaSubtypingErrorTypePolicy,
    ): Boolean {
        val useSiteSession = analysisSession.firSession

        val classSymbol = lookupTag.toRegularClassSymbol(useSiteSession)
            ?: return errorTypePolicy == KaSubtypingErrorTypePolicy.LENIENT

        val expandedSuperclassSymbol = superclassSymbol.fullyExpandedClass(useSiteSession)
            ?: return errorTypePolicy == KaSubtypingErrorTypePolicy.LENIENT

        return classSymbol == expandedSuperclassSymbol || isSubclassOf(
            classSymbol.fir,
            expandedSuperclassSymbol.fir,
            useSiteSession,
            allowIndirectSubtyping = true,
        )
    }

    private fun ConeKotlinType.isSubtypeOf(
        superclassSymbol: FirClassLikeSymbol<*>,
        errorTypePolicy: KaSubtypingErrorTypePolicy,
    ): Boolean {
        val superclassType = analysisSession.firSession.typeContext.createSimpleType(
            superclassSymbol.toLookupTag(),
            superclassSymbol.typeParameterSymbols.map { ConeStarProjection },
            nullable = true,
        ) as ConeClassLikeType

        return AbstractTypeChecker.isSubtypeOf(
            createTypeCheckerContext(errorTypePolicy),
            this,
            superclassType,
        )
    }

    override fun KaCallableSymbol.canBeCalledAsExtensionOn(receiverType: KaType): Boolean = withValidityAssertion {
        with(analysisSession) {
            if (!this@canBeCalledAsExtensionOn.isExtension) {
                return false
            }

            val expectedReceiverType = this@canBeCalledAsExtensionOn.receiverType ?: return false
            val actualReceiverTypeParameters = receiverType.symbol?.typeParameters ?: emptyList()

            /**
             * If both [expectedReceiverType] and [receiverType] do not depend on any type parameters,
             * a regular [org.jetbrains.kotlin.analysis.api.components.KaTypeRelationChecker.isSubtypeOf] is called.
             *
             * However, `isSubtypeOf` cannot properly handle complex type parameters in these receiver types.
             * That's why otherwise we have to perform a more complex check that requires using a constraint system.
             */
            if (actualReceiverTypeParameters.isEmpty() && expectedReceiverType.symbol?.typeParameters?.isEmpty() == true
            ) {
                return receiverType.isSubtypeOf(expectedReceiverType)
            }

            /**
             * Contains all type parameters from the candidate extension function and from the actual receiver.
             * We cannot consider just type parameters from the expected receiver type,
             * as these type parameters might depend on other type parameters of the extension.
             */
            val typeParametersList =
                (this@canBeCalledAsExtensionOn.typeParameters + actualReceiverTypeParameters).map { kaTypeParameter ->
                    require(kaTypeParameter is KaFirTypeParameterSymbol)
                    ConeTypeParameterLookupTag(kaTypeParameter.firSymbol)
                }

            val constraintSystem = ConeSimpleConstraintSystemImpl(firSession.inferenceComponents.createConstraintSystem(), firSession)
            val typeSubstitutor = constraintSystem.registerTypeVariables(typeParametersList)

            with(constraintSystem.context) {
                val actual = AbstractTypeChecker.prepareType(
                    constraintSystem.context,
                    typeSubstitutor.safeSubstitute(receiverType.coneType)
                ).let {
                    /**
                     * Here it's important to use [org.jetbrains.kotlin.types.model.TypeSystemInferenceExtensionContext.captureFromExpression] whenever possible.
                     * It sets the capture status to [org.jetbrains.kotlin.types.model.CaptureStatus.FROM_EXPRESSION] for captured types
                     * instead of [org.jetbrains.kotlin.types.model.CaptureStatus.FOR_SUBTYPING],
                     * which helps to avoid approximation of captured types during the constraint calculation.
                     *
                     * Otherwise, it can produce unwanted constraints like UPPER(Nothing) and LOWER(Any?) (from CapturedType(*) <:> TypeVariable(E))
                     * in the type inference context due to the approximation of captured types.
                     *
                     * @see org.jetbrains.kotlin.resolve.calls.inference.components.ConstraintInjector.TypeCheckerStateForConstraintInjector.addNewIncorporatedConstraint
                     */
                    constraintSystem.context.captureFromExpression(it) ?: it
                }
                val expected = typeSubstitutor.safeSubstitute(expectedReceiverType.coneType)

                constraintSystem.addSubtypeConstraint(actual, expected)
            }

            return !constraintSystem.hasContradiction()
        }
    }
}
