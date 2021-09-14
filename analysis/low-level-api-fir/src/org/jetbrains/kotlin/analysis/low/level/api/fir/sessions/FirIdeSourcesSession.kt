/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.sessions

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analyzer.ModuleSourceInfoBase
import org.jetbrains.kotlin.fir.BuiltinTypes
import org.jetbrains.kotlin.fir.PrivateSessionConstructor
import org.jetbrains.kotlin.analysis.low.level.api.fir.file.builder.FirFileBuilder
import org.jetbrains.kotlin.analysis.low.level.api.fir.providers.firIdeProvider

@OptIn(PrivateSessionConstructor::class)
internal class FirIdeSourcesSession @PrivateSessionConstructor constructor(
    val dependencies: List<ModuleSourceInfoBase>,
    override val project: Project,
    override val scope: GlobalSearchScope,
    val firFileBuilder: FirFileBuilder,
    builtinTypes: BuiltinTypes,
) : FirIdeModuleSession(builtinTypes, Kind.Source) {
    val cache get() = firIdeProvider.cache
}

