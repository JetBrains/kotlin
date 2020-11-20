/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import org.jetbrains.kotlin.analyzer.ModuleInfo

@OptIn(PrivateSessionConstructor::class)
abstract class FirModuleBasedSession @PrivateSessionConstructor constructor(
    override val moduleInfo: ModuleInfo,
    sessionProvider: FirSessionProvider?
) : FirSession(sessionProvider)
