/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.extensions.impl

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSessionComponent
import org.jetbrains.kotlin.fir.extensions.FirExtensionApiInternals
import org.jetbrains.kotlin.fir.extensions.PluginTypeResolver
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol

// TODO(KT-91604): collapse abstraction when semantics and resolve modules are reorganized.
@FirExtensionApiInternals
abstract class PluginTypeResolverFactory : FirSessionComponent {
    abstract fun getTypeResolver(klass: FirClassSymbol<*>): PluginTypeResolver?
}

@FirExtensionApiInternals
val FirSession.pluginTypeResolverFactory: PluginTypeResolverFactory by FirSession.Companion.sessionComponentAccessor()
