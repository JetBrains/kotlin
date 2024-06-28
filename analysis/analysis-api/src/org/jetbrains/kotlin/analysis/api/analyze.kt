/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(KaAnalysisApiInternals::class)

package org.jetbrains.kotlin.analysis.api

import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.analysis.api.session.KaSessionProvider
import org.jetbrains.kotlin.analysis.api.projectStructure.KaDanglingFileResolutionMode
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.analysis.api.projectStructure.withDanglingFileResolutionMode
import org.jetbrains.kotlin.psi.KtElement

/**
 * Executes the given [action] in a [KaSession] context.
 *
 * The project will be analyzed from the perspective of [useSiteElement]'s module, also called the use-site module.
 *
 * @see KaSession
 */
public inline fun <R> analyze(
    useSiteElement: KtElement,
    action: KaSession.() -> R
): R =
    KaSessionProvider.getInstance(useSiteElement.project)
        .analyze(useSiteElement, action)

/**
 * Executes the given [action] in a [KaSession] context.
 *
 * The project will be analyzed from the perspective of the given [useSiteModule].
 *
 * @see KaSession
 */
public inline fun <R> analyze(
    useSiteModule: KaModule,
    crossinline action: KaSession.() -> R
): R {
    val sessionProvider = KaSessionProvider.getInstance(useSiteModule.project)
    return sessionProvider.analyze(useSiteModule, action)
}

/**
 * Executes the given [action] in a [KaSession] context.
 * Depending on the passed [resolutionMode], declarations inside a file copy will be treated in a specific way.
 *
 * Note that the [useSiteElement] must be inside a dangling file copy.
 * Specifically, [PsiFile.getOriginalFile] must point to the copy source.
 *
 * The project will be analyzed from the perspective of [useSiteElement]'s module, also called the use-site module.
 */
public inline fun <R> analyzeCopy(
    useSiteElement: KtElement,
    resolutionMode: KaDanglingFileResolutionMode,
    crossinline action: KaSession.() -> R,
): R {
    val containingFile = useSiteElement.containingKtFile
    return withDanglingFileResolutionMode(containingFile, resolutionMode) {
        analyze(containingFile, action)
    }
}
