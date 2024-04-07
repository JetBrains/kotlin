/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.java.scopes

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.java.JavaTypeParameterStack
import org.jetbrains.kotlin.fir.languageVersionSettings
import org.jetbrains.kotlin.fir.scopes.PlatformSpecificOverridabilityRules
import org.jetbrains.kotlin.fir.scopes.firOverrideChecker
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.unwrapFakeOverrides

class JavaOverridabilityRules(private val session: FirSession) : PlatformSpecificOverridabilityRules {
    // Note: return types (considerReturnTypeKinds) look not important when attempting intersection
    // From the other side, they can break relevant tests like intersectionWithJavaVoidNothing.kt
    // The similar case exists in bootstrap (see IrSimpleBuiltinOperatorDescriptorImpl)
    private val javaOverrideChecker =
        JavaOverrideChecker(session, JavaTypeParameterStack.EMPTY, baseScopes = null, considerReturnTypeKinds = false)
    private val standardOverrideChecker = session.firOverrideChecker

    override fun isOverriddenFunction(overrideCandidate: FirSimpleFunction, baseDeclaration: FirSimpleFunction): Boolean? {
        return if (shouldApplyJavaChecker(overrideCandidate, baseDeclaration)) {
            when {
                // Only returning the negative result is questionable here (JavaOverrideChecker can forbid overriding, but it cannot allow it on own behalf)
                // Known influenced tests: supertypeDifferentParameterNullability.kt became partially green without it
                !javaOverrideChecker.isOverriddenFunction(overrideCandidate, baseDeclaration) -> false
                standardOverrideChecker.isOverriddenFunction(overrideCandidate, baseDeclaration) -> true
                shouldDoReverseCheck(overrideCandidate) -> standardOverrideChecker.isOverriddenFunction(baseDeclaration, overrideCandidate)
                else -> false
            }
        } else {
            null
        }
    }

    /**
     * Without [LanguageFeature.JavaTypeParameterDefaultRepresentationWithDNN] enabled,
     * the check is unfortunately not symmetrical in a case when the declarations are generic, DNNs are used,
     * and one of them has a flexible upper bound while the other one doesn't.
     *
     * See compiler/testData/diagnostics/tests/j+k/overrideWithTypeParameter.kt
     */
    private fun shouldDoReverseCheck(overrideCandidate: FirSimpleFunction): Boolean {
        return !session.languageVersionSettings.supportsFeature(LanguageFeature.JavaTypeParameterDefaultRepresentationWithDNN) &&
                overrideCandidate.typeParameters.isNotEmpty()
    }

    override fun isOverriddenProperty(overrideCandidate: FirCallableDeclaration, baseDeclaration: FirProperty): Boolean? {
        return if (shouldApplyJavaChecker(overrideCandidate, baseDeclaration)) {
            javaOverrideChecker.isOverriddenProperty(overrideCandidate, baseDeclaration)
        } else {
            null
        }
    }

    private fun shouldApplyJavaChecker(overrideCandidate: FirCallableDeclaration, baseDeclaration: FirCallableDeclaration): Boolean {
        // One candidate with Java original is enough to apply Java checker,
        // otherwise e.g. primitive type comparisons do not work.
        return overrideCandidate.isOriginallyFromJava() || baseDeclaration.isOriginallyFromJava()
    }

    private fun FirCallableDeclaration.isOriginallyFromJava(): Boolean = unwrapFakeOverrides().origin == FirDeclarationOrigin.Enhancement

    override fun chooseIntersectionVisibility(
        overrides: Collection<FirCallableSymbol<*>>,
        dispatchClassSymbol: FirRegularClassSymbol?,
    ): Visibility = javaOverrideChecker.chooseIntersectionVisibility(overrides, dispatchClassSymbol)
}
