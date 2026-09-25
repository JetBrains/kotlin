/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.providers.impl

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.EffectiveVisibility
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.NoMutableState
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.builder.buildRegularClass
import org.jetbrains.kotlin.fir.declarations.impl.FirResolvedDeclarationStatusImpl
import org.jetbrains.kotlin.fir.languageVersionSettings
import org.jetbrains.kotlin.fir.scopes.FirScopeProvider
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.utils.addToStdlib.runIf

@RequiresOptIn("Creating FirValueSymbolProvider unconditionally is only available for Analysis API")
annotation class AnalysisApiOnly

/**
 * The symbol provider for the synthetic `kotlin.Value` class.
 */
@NoMutableState
class FirValueSymbolProvider private constructor(
    session: FirSession,
    moduleData: FirModuleData,
    scopeProvider: FirScopeProvider,
) : FirSingleClassSymbolProvider(buildValueClass(moduleData, session, scopeProvider), session) {
    companion object {
        /**
         * Returns a [FirValueSymbolProvider] if [LanguageFeature.RichErrors] is enabled in the given [session] or null otherwise.
         */
        fun createIfRichErrorsEnabled(
            session: FirSession,
            moduleData: FirModuleData,
            scopeProvider: FirScopeProvider,
        ): FirValueSymbolProvider? = runIf(session.languageVersionSettings.supportsFeature(LanguageFeature.RichErrors)) {
            FirValueSymbolProvider(session, moduleData, scopeProvider)
        }

        @AnalysisApiOnly
        fun create(
            session: FirSession,
            moduleData: FirModuleData,
            scopeProvider: FirScopeProvider,
        ): FirValueSymbolProvider = FirValueSymbolProvider(session, moduleData, scopeProvider)
    }
}

private fun buildValueClass(
    moduleData: FirModuleData,
    session: FirSession,
    scopeProvider: FirScopeProvider,
): FirRegularClass {
    return buildRegularClass {
        resolvePhase = FirResolvePhase.ANALYZED_DEPENDENCIES
        origin = FirDeclarationOrigin.Library
        this.moduleData = moduleData
        status = FirResolvedDeclarationStatusImpl(
            Visibilities.Public,
            Modality.ABSTRACT,
            EffectiveVisibility.Public
        )

        classKind = ClassKind.INTERFACE
        val classSymbol = FirRegularClassSymbol(StandardClassIds.Value)
        symbol = classSymbol
        superTypeRefs += buildResolvedTypeRef {
            coneType = session.builtinTypes.anyType.coneType
        }

        this.scopeProvider = scopeProvider
        name = StandardClassIds.Value.shortClassName
    }
}
