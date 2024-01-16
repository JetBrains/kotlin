/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend.native

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.backend.native.interop.decodeObjCMethodAnnotation
import org.jetbrains.kotlin.fir.declarations.FirCallableDeclaration
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.scopes.FirOverrideChecker
import org.jetbrains.kotlin.fir.scopes.impl.FirStandardOverrideChecker

/**
 * The same code, as in this class also exists in [org.jetbrains.kotlin.ir.objcinterop.ObjCOverridabilityCondition]
 * and in [org.jetbrains.kotlin.ir.objcinterop.IrObjCOverridabilityCondition].
 *
 * When modifying, all three copies should be synchronized.
 */
class FirNativeOverrideChecker(private val session: FirSession) : FirOverrideChecker {
    private val standardOverrideChecker = FirStandardOverrideChecker(session)

    override fun isOverriddenFunction(overrideCandidate: FirSimpleFunction, baseDeclaration: FirSimpleFunction): Boolean =
            overrideCandidate.isPlatformOverriddenFunction(session, baseDeclaration)
                    ?: standardOverrideChecker.isOverriddenFunction(overrideCandidate, baseDeclaration)

    override fun isOverriddenProperty(overrideCandidate: FirCallableDeclaration, baseDeclaration: FirProperty): Boolean =
        // KT-57640: There's no necessity to implement platform-dependent overridability check for properties
        standardOverrideChecker.isOverriddenProperty(overrideCandidate, baseDeclaration)

    /**
     * mimics ObjCOverridabilityCondition.isOverridable
     */
    private fun FirSimpleFunction.isPlatformOverriddenFunction(session: FirSession, baseDeclaration: FirSimpleFunction): Boolean? {
        if (this.name != baseDeclaration.name) {
            return null
        }
        val superInfo = baseDeclaration.symbol.decodeObjCMethodAnnotation(session) ?: return null
        val subInfo = symbol.decodeObjCMethodAnnotation(session)
        return if (subInfo != null) {
            // Overriding Objective-C method by Objective-C method in interop stubs.
            // Don't even check method signatures, so this check is weaker than the standard one
            superInfo.selector == subInfo.selector
        } else {
            // Overriding Objective-C method by Kotlin method.
            if (!parameterNamesMatch(this, baseDeclaration)) false else null
        }
    }

    /**
     * mimics ObjCInteropKt.parameterNamesMatch
     */
    private fun parameterNamesMatch(first: FirSimpleFunction, second: FirSimpleFunction): Boolean {
        // The original Objective-C method selector is represented as
        // function name and parameter names (except first).

        if (first.valueParameters.size != second.valueParameters.size) {
            return false
        }

        first.valueParameters.forEachIndexed { index, parameter ->
            if (index > 0 && parameter.name != second.valueParameters[index].name) {
                return false
            }
        }

        return true
    }
}