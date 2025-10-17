/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSessionComponent
import org.jetbrains.kotlin.resolve.DefaultImportsProvider

class FirDefaultImportsProviderHolder(val provider: DefaultImportsProvider) : FirSessionComponent

private val FirSession.defaultImportsProviderHolder: FirDefaultImportsProviderHolder by FirSession.sessionComponentAccessor()
val FirSession.defaultImportsProvider: DefaultImportsProvider get() = defaultImportsProviderHolder.provider
