/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.projectStructure

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.session.useSiteModule

/**
 * Returns a [KaModule] for a given [this] in the context of the [useSiteModule].
 *
 * @see KaModuleProvider.module
 */
public fun PsiElement.kaModule(useSiteModule: KaModule?): KaModule {
    return KaModuleProvider.getInstance(project).module(this, useSiteModule)
}

/**
 * A [KaModule] for a given [this] in the context of the session's [useSiteModule].
 *
 * @see KaModuleProvider.module
 */
context(session: KaSession)
public val PsiElement.kaModule: KaModule
    get() = kaModule(useSiteModule)
