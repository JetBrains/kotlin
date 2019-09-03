/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import org.jetbrains.kotlin.analyzer.ModuleInfo
import org.jetbrains.kotlin.fir.resolve.*
import org.jetbrains.kotlin.fir.resolve.impl.*
import org.jetbrains.kotlin.fir.scopes.impl.FirClassDeclaredMemberScopeProvider

abstract class FirModuleBasedSession(override val moduleInfo: ModuleInfo, sessionProvider: FirSessionProvider?) :
    FirSessionBase(sessionProvider) {
    init {
        registerComponent(FirQualifierResolver::class, FirQualifierResolverImpl(this))
        registerComponent(FirTypeResolver::class, FirTypeResolverImpl(this))
        registerComponent(FirClassDeclaredMemberScopeProvider::class, FirClassDeclaredMemberScopeProvider())
    }
}

