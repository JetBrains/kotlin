/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.session

import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.SessionConfiguration
import org.jetbrains.kotlin.fir.analysis.checkers.FirIdentityLessPlatformDeterminer
import org.jetbrains.kotlin.fir.analysis.checkers.FirPlatformDiagnosticSuppressor
import org.jetbrains.kotlin.fir.analysis.js.checkers.FirJsIdentityLessPlatformDeterminer
import org.jetbrains.kotlin.fir.analysis.js.checkers.FirJsModuleKind
import org.jetbrains.kotlin.fir.analysis.js.checkers.FirJsPlatformDiagnosticSuppressor
import org.jetbrains.kotlin.fir.checkers.registerJsCheckers
import org.jetbrains.kotlin.fir.declarations.FirTypeSpecificityComparatorProvider
import org.jetbrains.kotlin.fir.deserialization.FirTypeDeserializer
import org.jetbrains.kotlin.fir.scopes.FirDefaultImportsProviderHolder
import org.jetbrains.kotlin.fir.scopes.impl.FirEnumEntriesSupport
import org.jetbrains.kotlin.fir.types.typeContext
import org.jetbrains.kotlin.js.config.JSConfigurationKeys
import org.jetbrains.kotlin.js.config.ModuleKind
import org.jetbrains.kotlin.js.resolve.JsDefaultImportsProvider
import org.jetbrains.kotlin.js.resolve.JsTypeSpecificityComparatorWithoutDelegate

@OptIn(SessionConfiguration::class)
object FirJsSessionFactory : AbstractFirKlibSessionFactory<FirJsSessionFactory.Context, FirJsSessionFactory.Context>() {

    // ==================================== Library session ====================================

    override fun createLibraryContext(configuration: CompilerConfiguration): Context {
        return Context(configuration)
    }

    override fun createFlexibleTypeFactory(session: FirSession): FirTypeDeserializer.FlexibleTypeFactory {
        return JsFlexibleTypeFactory(session)
    }

    override fun FirSession.registerLibrarySessionComponents(c: Context) {
        registerJsComponents(c.moduleKind)
    }

    // ==================================== Platform session ====================================

    override fun createSourceContext(configuration: CompilerConfiguration): Context {
        return Context(configuration)
    }

    override fun FirSessionConfigurator.registerPlatformCheckers() {
        registerJsCheckers()
    }

    override fun FirSessionConfigurator.registerExtraPlatformCheckers() {}

    override fun FirSession.registerSourceSessionComponents(c: Context) {
        registerJsComponents(c.moduleKind)
    }

    // ==================================== Common parts ====================================

    fun FirSession.registerJsComponents(moduleKind: ModuleKind?) {
        register(FirEnumEntriesSupport(this))
        register(FirTypeSpecificityComparatorProvider.of(JsTypeSpecificityComparatorWithoutDelegate(typeContext)))
        register(FirPlatformDiagnosticSuppressor::class, FirJsPlatformDiagnosticSuppressor())
        register(FirIdentityLessPlatformDeterminer::class, FirJsIdentityLessPlatformDeterminer)

        if (moduleKind != null) {
            register(FirJsModuleKind::class, FirJsModuleKind(moduleKind))
        }
        register(FirDefaultImportsProviderHolder.of(JsDefaultImportsProvider))
    }

    // ==================================== Utilities ====================================

    class Context(val moduleKind: ModuleKind?) {
        constructor(
            compilerConfiguration: CompilerConfiguration
        ) : this(compilerConfiguration.get(JSConfigurationKeys.MODULE_KIND, ModuleKind.PLAIN))
    }
}
