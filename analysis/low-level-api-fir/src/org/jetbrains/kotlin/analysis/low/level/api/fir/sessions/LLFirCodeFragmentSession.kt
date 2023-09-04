/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.sessions

import org.jetbrains.kotlin.analysis.low.level.api.fir.LLFirModuleResolveComponents
import org.jetbrains.kotlin.analysis.project.structure.KtCodeFragmentModule
import org.jetbrains.kotlin.fir.BuiltinTypes
import org.jetbrains.kotlin.fir.PrivateSessionConstructor

internal class LLFirCodeFragmentSession @PrivateSessionConstructor constructor(
    ktModule: KtCodeFragmentModule,
    override val moduleComponents: LLFirModuleResolveComponents,
    builtinTypes: BuiltinTypes,
) : LLFirResolvableModuleSession(ktModule, builtinTypes)
