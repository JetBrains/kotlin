/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api.sessions

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.fir.BuiltinTypes
import org.jetbrains.kotlin.fir.PrivateSessionConstructor
import org.jetbrains.kotlin.idea.caches.project.ModuleSourceInfo
import org.jetbrains.kotlin.idea.fir.low.level.api.file.builder.FirFileBuilder
import org.jetbrains.kotlin.idea.fir.low.level.api.providers.firIdeProvider

@OptIn(PrivateSessionConstructor::class)
internal class FirIdeSourcesSession @PrivateSessionConstructor constructor(
    val dependencies: List<ModuleSourceInfo>,
    override val project: Project,
    override val scope: GlobalSearchScope,
    val firFileBuilder: FirFileBuilder,
    builtinTypes: BuiltinTypes,
) : FirIdeModuleSession( builtinTypes) {
    val cache get() = firIdeProvider.cache
}

