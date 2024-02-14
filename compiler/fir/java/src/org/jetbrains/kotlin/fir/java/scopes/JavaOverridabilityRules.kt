/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.java.scopes

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirCallableDeclaration
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.java.JavaTypeParameterStack
import org.jetbrains.kotlin.fir.scopes.PlatformSpecificOverridabilityRules
import org.jetbrains.kotlin.fir.unwrapFakeOverrides

class JavaOverridabilityRules(session: FirSession) : PlatformSpecificOverridabilityRules {
    // Note: return types (considerReturnTypeKinds) look not important when attempting intersection
    // From the other side, they can break relevant tests like intersectionWithJavaVoidNothing.kt
    // The similar case exists in bootstrap (see IrSimpleBuiltinOperatorDescriptorImpl)
    private val javaOverrideChecker =
        JavaOverrideChecker(session, JavaTypeParameterStack.EMPTY, baseScopes = null, considerReturnTypeKinds = false)

    override fun isOverriddenFunction(overrideCandidate: FirSimpleFunction, baseDeclaration: FirSimpleFunction): Boolean? {
        return if (shouldApplyJavaChecker(overrideCandidate, baseDeclaration)) {
            // takeIf is questionable here (JavaOverrideChecker can forbid overriding, but it cannot allow it on own behalf)
            // Known influenced tests: supertypeDifferentParameterNullability.kt became partially green without it
            javaOverrideChecker.isOverriddenFunction(overrideCandidate, baseDeclaration).takeIf { !it }
        } else {
            null
        }
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
}
