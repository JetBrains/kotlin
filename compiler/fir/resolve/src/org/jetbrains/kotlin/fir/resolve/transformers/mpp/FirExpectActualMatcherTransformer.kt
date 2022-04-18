/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers.mpp

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.isActual
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.languageVersionSettings
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.transformers.FirAbstractTreeTransformer
import org.jetbrains.kotlin.fir.resolve.transformers.FirTransformerBasedResolveProcessor
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.resolve.multiplatform.compatible

class FirExpectActualMatcherProcessor(
    session: FirSession,
    scopeSession: ScopeSession
) : FirTransformerBasedResolveProcessor(session, scopeSession) {
    private val enabled = session.languageVersionSettings.supportsFeature(LanguageFeature.MultiPlatformProjects)

    override val transformer: FirTransformer<Nothing?> = FirExpectActualMatcherTransformer(session, scopeSession)

    override fun processFile(file: FirFile) {
        if (!enabled) return
        super.processFile(file)
    }
}

open class FirExpectActualMatcherTransformer(
    override val session: FirSession,
    private val scopeSession: ScopeSession
) : FirAbstractTreeTransformer<Nothing?>(FirResolvePhase.EXPECT_ACTUAL_MATCHING) {

    // --------------------------- classifiers ---------------------------
    override fun transformTypeAlias(typeAlias: FirTypeAlias, data: Nothing?): FirStatement {
        transformMemberDeclaration(typeAlias)
        return typeAlias
    }

    override fun transformRegularClass(regularClass: FirRegularClass, data: Nothing?): FirStatement {
        transformMemberDeclaration(regularClass)
        regularClass.transformChildren(this, data)
        return regularClass
    }

    // --------------------------- callable declaration ---------------------------
    override fun transformEnumEntry(enumEntry: FirEnumEntry, data: Nothing?): FirStatement {
        transformMemberDeclaration(enumEntry)
        return enumEntry
    }

    override fun transformProperty(property: FirProperty, data: Nothing?): FirStatement {
        transformMemberDeclaration(property)
        return property
    }

    override fun transformConstructor(constructor: FirConstructor, data: Nothing?): FirStatement {
        transformMemberDeclaration(constructor)
        return constructor
    }

    override fun transformSimpleFunction(simpleFunction: FirSimpleFunction, data: Nothing?): FirStatement {
        transformMemberDeclaration(simpleFunction)
        return simpleFunction
    }

    // --------------------------- other ---------------------------
    override fun transformAnonymousInitializer(anonymousInitializer: FirAnonymousInitializer, data: Nothing?): FirAnonymousInitializer {
        return anonymousInitializer
    }

    // ------------------------------------------------------

    private fun transformMemberDeclaration(memberDeclaration: FirMemberDeclaration) {
        if (!memberDeclaration.isActual) return
        val actualSymbol = memberDeclaration.symbol
        val expectForActualData = FirExpectActualResolver.findExpectForActual(
            actualSymbol,
            session,
            scopeSession
        ) ?: return
        memberDeclaration.expectForActual = expectForActualData
        for ((compatibility, expectSymbols) in expectForActualData) {
            if (compatibility.compatible) {
                for (expectSymbol in expectSymbols) {
                    expectSymbol.fir.setActualForExpect(session, actualSymbol)
                }
            }
        }
    }
}
