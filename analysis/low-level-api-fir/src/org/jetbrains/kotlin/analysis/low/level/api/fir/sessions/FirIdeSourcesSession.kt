/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.sessions

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.low.level.api.fir.file.builder.FirFileBuilder
import org.jetbrains.kotlin.analysis.low.level.api.fir.providers.firIdeProvider
import org.jetbrains.kotlin.analysis.project.structure.KtModule
import org.jetbrains.kotlin.analysis.project.structure.KtSourceModule
import org.jetbrains.kotlin.fir.BuiltinTypes
import org.jetbrains.kotlin.fir.PrivateSessionConstructor

@OptIn(PrivateSessionConstructor::class)
internal class FirIdeSourcesSession @PrivateSessionConstructor constructor(
    override val module: KtSourceModule,
    override val project: Project,
    val firFileBuilder: FirFileBuilder,
    builtinTypes: BuiltinTypes,
) : FirIdeModuleSession(builtinTypes, Kind.Source) {
    val cache get() = firIdeProvider.cache
}

