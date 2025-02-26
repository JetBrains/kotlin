/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(KaImplementationDetail::class)

package org.jetbrains.kotlin.analysis.api

import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.analysis.api.session.KaSessionProvider
import org.jetbrains.kotlin.analysis.api.projectStructure.KaDanglingFileResolutionMode
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.analysis.api.projectStructure.copyOrigin
import org.jetbrains.kotlin.analysis.api.projectStructure.withDanglingFileResolutionMode
import org.jetbrains.kotlin.psi.KtElement

/**
 * Executes the given [action] in an [analysis session][KaSession] context.
 *
 * The project will be analyzed from the perspective of [useSiteElement]'s module, also called the use-site module.
 *
 * Neither the analysis session nor any other [lifetime owners][org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeOwner] may be leaked
 * outside the [analyze] block. Please consult the documentation of [KaSession] for important information about lifetime management.
 */
public inline fun <R> analyze(
    useSiteElement: KtElement,
    action: KaSession.() -> R
): R =
    KaSessionProvider.getInstance(useSiteElement.project)
        .analyze(useSiteElement, action)

/**
 * Executes the given [action] in an [analysis session][KaSession] context.
 *
 * The project will be analyzed from the perspective of the given [useSiteModule].
 *
 * Neither the analysis session nor any other [lifetime owners][org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeOwner] may be leaked
 * outside the [analyze] block. Please consult the documentation of [KaSession] for important information about lifetime management.
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
 *
 * The [useSiteElement] must be inside a dangling file copy (specifically, [PsiFile.copyOrigin] must point to the copy source).
 * Depending on the passed [resolutionMode], declarations inside the file copy will be treated in a specific way.
 *
 * The project will be analyzed from the perspective of [useSiteElement]'s module, also called the use-site module.
 *
 * Neither the analysis session nor any other [lifetime owners][org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeOwner] may be leaked
 * outside the [analyze] block. Please consult the documentation of [KaSession] for important information about lifetime management.
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
