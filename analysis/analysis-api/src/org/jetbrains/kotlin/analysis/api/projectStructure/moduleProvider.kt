/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.projectStructure

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.session.useSiteModule

/**
 * Returns a [KaModule] for a given [element] in the context of the session's use-site module.
 *
 * @see KaModuleProvider.getModule
 */
context(session: KaSession)
public fun getModule(element: PsiElement): KaModule =
    KaModuleProvider.getModule(useSiteModule.project, element, useSiteModule)
