/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirCallableDeclaration
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction

/**
 * That class is expected to work just the same as FirStandardOverrideChecker for regular members,
 * but when it comes to the platform originated member, it's more correct to use platform specific rules, too
 *
 * Initial reason for that checker introduction is smart-cast to raw type:
 * it makes initial type's member incompatible with raw-type's one when regular overridability rules are used,
 * while in Java class they would be treated equally.
 */
class FirIntersectionScopeOverrideChecker(session: FirSession) : FirOverrideChecker {
    private val standardOverrideChecker = session.firOverrideChecker
    private val platformSpecificOverridabilityRules = session.platformSpecificOverridabilityRules

    override fun isOverriddenFunction(overrideCandidate: FirSimpleFunction, baseDeclaration: FirSimpleFunction): Boolean {
        platformSpecificOverridabilityRules?.isOverriddenFunction(overrideCandidate, baseDeclaration)?.let { return it }
        return standardOverrideChecker.isOverriddenFunction(overrideCandidate, baseDeclaration)
    }

    override fun isOverriddenProperty(overrideCandidate: FirCallableDeclaration, baseDeclaration: FirProperty): Boolean {
        platformSpecificOverridabilityRules?.isOverriddenProperty(overrideCandidate, baseDeclaration)?.let { return it }
        return standardOverrideChecker.isOverriddenProperty(overrideCandidate, baseDeclaration)
    }
}
