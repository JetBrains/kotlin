/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.factory.components

import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirSession
import org.jetbrains.kotlin.fir.SessionConfiguration
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProvider
import org.jetbrains.kotlin.fir.scopes.FirOverrideChecker
import org.jetbrains.kotlin.fir.scopes.impl.FirStandardOverrideChecker
import org.jetbrains.kotlin.fir.session.FirNativeSessionFactory
import org.jetbrains.kotlin.resolve.DefaultImportsProvider
import org.jetbrains.kotlin.resolve.konan.platform.NativeDefaultImportsProvider

@OptIn(SessionConfiguration::class)
internal object LLNativeSessionComponentRegistration : LLPlatformSessionComponentRegistration {
    override fun registerComponents(session: LLFirSession, platformSpecificSymbolProviders: List<FirSymbolProvider>) = with(session) {
        // For session component registration, there is currently no practical difference between the default and the `ForMetadata` session
        // factory. However, given that the `ForMetadata` factory exists and might change in the future, we have to take it into account.
        val firSessionFactory = if (session.isMetadataSession) {
            FirNativeSessionFactory.ForMetadata
        } else {
            FirNativeSessionFactory
        }

        with(firSessionFactory) {
            registerNativeComponents()
        }
    }

    override fun registerResolvableLibraryComponents(session: LLFirSession) = with(session) {
        // A resolvable library session for decompiled libraries can miss annotation arguments that are necessary for the correct behavior
        // of the native overload checker. Hence, we use the standard override checker instead.
        register(FirOverrideChecker::class, FirStandardOverrideChecker(this))
    }

    override val defaultImportsProvider: DefaultImportsProvider
        get() = NativeDefaultImportsProvider
}
