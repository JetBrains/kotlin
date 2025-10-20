/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers.mpp

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expectActualMatchingContextFactory
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.isEnabled
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.transformers.FirAbstractTreeTransformer
import org.jetbrains.kotlin.fir.resolve.transformers.FirTransformerBasedResolveProcessor
import org.jetbrains.kotlin.fir.visitors.FirTransformer

class FirExpectActualMatcherProcessor(
    session: FirSession,
    scopeSession: ScopeSession,
) : FirTransformerBasedResolveProcessor(session, scopeSession, FirResolvePhase.EXPECT_ACTUAL_MATCHING) {
    private val enabled = LanguageFeature.MultiPlatformProjects.isEnabled()

    override val transformer: FirTransformer<Nothing?> = FirExpectActualMatcherTransformer(session, scopeSession)

    override fun processFile(file: FirFile) {
        if (!enabled) return
        super.processFile(file)
    }
}

/**
 * This transformer populates [expectForActual] mapping for actual declarations.
 * Also, populates it [memberExpectForActual] mapping
 *
 * Should run before any kind of body resolution, since [expectForActual] is used there.
 *
 * See `/docs/fir/k2_kmp.md`
 */
open class FirExpectActualMatcherTransformer(
    final override val session: FirSession,
    actualScopeSession: ScopeSession,
) : FirAbstractTreeTransformer<Nothing?>(FirResolvePhase.EXPECT_ACTUAL_MATCHING) {

    private val expectActualMatchingContext = session.expectActualMatchingContextFactory.create(
        session, actualScopeSession,
        allowedWritingMemberExpectForActualMapping = true,
    )

    // --------------------------- classifiers ---------------------------
    override fun transformTypeAlias(typeAlias: FirTypeAlias, data: Nothing?): FirStatement {
        transformMemberDeclaration(typeAlias)
        return typeAlias
    }

    override fun transformRegularClass(regularClass: FirRegularClass, data: Nothing?): FirStatement {
        transformMemberDeclaration(regularClass)
        regularClass.transformDeclarations(this, data)
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

    override fun transformErrorPrimaryConstructor(errorPrimaryConstructor: FirErrorPrimaryConstructor, data: Nothing?): FirStatement =
        transformConstructor(errorPrimaryConstructor, data)

    override fun transformSimpleFunction(simpleFunction: FirNamedFunction, data: Nothing?): FirStatement {
        transformMemberDeclaration(simpleFunction)
        return simpleFunction
    }

    // --------------------------- other ---------------------------

    override fun transformFile(file: FirFile, data: Nothing?): FirFile {
        file.transformDeclarations(this, data)
        return file
    }

    override fun <E : FirElement> transformElement(element: E, data: Nothing?): E {
        // To prevent entering anonymous initializer's body, delegate field's body,
        // and other unnecessary elements
        return element
    }

    // ------------------------------------------------------

    fun transformMemberDeclaration(memberDeclaration: FirMemberDeclaration) {
        val actualSymbol = memberDeclaration.symbol

        // Regardless of whether any `expect` symbols are found for `memberDeclaration`, it must be assigned an `expectForActual` map.
        // Otherwise, `FirExpectActualDeclarationChecker` will assume that the symbol needs no checking and not report an
        // `EXPECT_WITHOUT_ACTUAL` error.
        val expectForActualData = FirExpectActualResolver.findExpectForActual(
            actualSymbol,
            session,
            expectActualMatchingContext,
        )
        memberDeclaration.expectForActual = expectForActualData
    }
}
