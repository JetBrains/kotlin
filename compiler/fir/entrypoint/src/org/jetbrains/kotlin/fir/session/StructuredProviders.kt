/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.session

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSessionComponent
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProvider

/**
 * @property sourceProviders contains providers of declarations from exactly this session, which includes the following:
 * - provider of real source declarations
 * - provider of plugin-generated declarations for this specific module
 * - provider of precompiled classes from this specific module came from incremental compilation
 *
 * @property dependencyProviders contains providers of declarations from dependencies of this session, which includes the following:
 * - binary dependencies, such as .class files, .jar files, .klib files
 * - source providers of dependency mpp modules
 *
 * @property sharedProvider contains a provider, which is shared between sessions. This provider includes fallback builtins,
 * synthetic functional interfaces provider and similar stuff.
 * The important note here is that [sharedProvider] differs for source session in case of regular and hmpp compilation schemes:
 * - in the regular scheme there is a single shared provider, which is shared between all source and library sessions
 * - in the HMPP scheme shared provider is shared only between library sessions. Source sessions don't have one, and each such
 *   session uses its own list of providers, which usually belong to the shared provider.
 */
class StructuredProviders(
    val sourceProviders: List<FirSymbolProvider>,
    val dependencyProviders: List<FirSymbolProvider>,
    val sharedProvider: FirSymbolProvider
) : FirSessionComponent

val FirSession.structuredProviders: StructuredProviders by FirSession.sessionComponentAccessor()
