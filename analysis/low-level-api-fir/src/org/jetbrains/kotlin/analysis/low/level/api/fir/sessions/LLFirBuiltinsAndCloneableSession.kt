/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.sessions

import com.intellij.openapi.util.ModificationTracker
import org.jetbrains.kotlin.analysis.project.structure.KtModule
import org.jetbrains.kotlin.fir.BuiltinTypes
import org.jetbrains.kotlin.fir.PrivateSessionConstructor

class LLFirBuiltinsAndCloneableSession @PrivateSessionConstructor constructor(
    ktModule: KtModule,
    dependencyTracker: ModificationTracker,
    builtinTypes: BuiltinTypes,
) : LLFirLibraryLikeSession(ktModule, dependencyTracker, builtinTypes)