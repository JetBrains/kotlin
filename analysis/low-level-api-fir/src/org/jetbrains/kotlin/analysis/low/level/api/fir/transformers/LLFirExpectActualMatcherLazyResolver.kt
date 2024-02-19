/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.transformers

import org.jetbrains.kotlin.analysis.low.level.api.fir.api.targets.LLFirResolveTarget
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.checkExpectForActualIsResolved
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.fir.FirElementWithResolveState
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.isCopyCreatedInScope
import org.jetbrains.kotlin.fir.languageVersionSettings
import org.jetbrains.kotlin.fir.resolve.transformers.mpp.FirExpectActualMatcherTransformer
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase

internal object LLFirExpectActualMatcherLazyResolver : LLFirLazyResolver(FirResolvePhase.EXPECT_ACTUAL_MATCHING) {
    override fun createTargetResolver(target: LLFirResolveTarget): LLFirTargetResolver = LLFirExpectActualMatchingTargetResolver(target)

    override fun phaseSpecificCheckIsResolved(target: FirElementWithResolveState) {
        if (target.moduleData.session.languageVersionSettings.supportsFeature(LanguageFeature.MultiPlatformProjects) &&
            target is FirMemberDeclaration &&
            target.canHaveExpectCounterPart()
        ) {
            checkExpectForActualIsResolved(target)
        }
    }
}

private class LLFirExpectActualMatchingTargetResolver(target: LLFirResolveTarget) : LLFirTargetResolver(
    target,
    FirResolvePhase.EXPECT_ACTUAL_MATCHING
) {
    private val enabled = resolveTargetSession.languageVersionSettings.supportsFeature(LanguageFeature.MultiPlatformProjects)

    @Deprecated("Should never be called directly, only for override purposes, please use withRegularClass", level = DeprecationLevel.ERROR)
    override fun withContainingRegularClass(firClass: FirRegularClass, action: () -> Unit) {
        if (enabled) {
            // Resolve outer classes before resolving inner declarations. It's the requirement of FirExpectActualResolver
            firClass.lazyResolveToPhase(resolverPhase.previous)
            performResolve(firClass)
        }
        action()
    }

    private val transformer = object : FirExpectActualMatcherTransformer(resolveTargetSession, resolveTargetScopeSession) {
        override fun transformRegularClass(regularClass: FirRegularClass, data: Nothing?): FirStatement {
            transformMemberDeclaration(regularClass)
            return regularClass
        }
    }

    override fun doLazyResolveUnderLock(target: FirElementWithResolveState) {
        if (enabled && target is FirMemberDeclaration && target.canHaveExpectCounterPart()) {
            transformer.transformMemberDeclaration(target)
        }
    }
}

private fun FirMemberDeclaration.canHaveExpectCounterPart(): Boolean = when {
    // We shouldn't try to calculate expect/actual mapping for fake declarations
    this is FirCallableDeclaration && isCopyCreatedInScope -> false
    this is FirEnumEntry -> true
    this is FirProperty -> true
    this is FirConstructor -> true
    this is FirSimpleFunction -> true
    this is FirRegularClass -> true
    this is FirTypeAlias -> true
    else -> false
}
