/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.factory.components

import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirSession
import org.jetbrains.kotlin.fir.SessionConfiguration
import org.jetbrains.kotlin.fir.backend.jvm.FirJvmTypeMapper
import org.jetbrains.kotlin.fir.java.JavaSymbolProvider
import org.jetbrains.kotlin.fir.resolve.FirJvmDefaultImportsProvider
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProvider
import org.jetbrains.kotlin.fir.session.registerJavaComponents
import org.jetbrains.kotlin.resolve.DefaultImportsProvider
import org.jetbrains.kotlin.resolve.jvm.modules.JavaModuleResolver

@OptIn(SessionConfiguration::class)
internal object LLJvmSessionComponentRegistration : LLPlatformSessionComponentRegistration {
    override fun registerComponents(session: LLFirSession, platformSpecificSymbolProviders: List<FirSymbolProvider>) = with(session) {
        registerJavaComponents(JavaModuleResolver.getInstance(project))
        platformSpecificSymbolProviders.filterIsInstance<JavaSymbolProvider>().firstOrNull()?.let {
            register(JavaSymbolProvider::class, it)
        }
        register(FirJvmTypeMapper::class, FirJvmTypeMapper(this))
    }

    override val defaultImportsProvider: DefaultImportsProvider
        get() = FirJvmDefaultImportsProvider
}
