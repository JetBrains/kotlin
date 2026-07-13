/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(KaImplementationDetail::class, ExperimentalContracts::class)

package org.jetbrains.kotlin.analysis.api.session

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.projectStructure.KaDanglingFileResolutionMode
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.analysis.api.projectStructure.copyOrigin
import org.jetbrains.kotlin.analysis.api.projectStructure.withDanglingFileResolutionMode
import org.jetbrains.kotlin.psi.KtElement
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * Executes the given [action] in an [analysis session][KaSession] context.
 *
 * The [KaSession] is provided as a context parameter, so members of the Analysis API are available inside [action] without an explicit
 * receiver.
 *
 * The project will be analyzed from the perspective of [useSiteElement]'s [module][KaModule], also called the use-site module.
 *
 * Neither the analysis session nor any other [lifetime owners][org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeOwner] may be leaked
 * outside the [analyze] block. Please consult the documentation of [KaSession] for important information about lifetime management.
 *
 * @see org.jetbrains.kotlin.analysis.api.projectStructure.KaModuleProvider.getModule
 */
public inline fun <R> analyze(
    useSiteElement: PsiElement,
    action: context(KaSession) () -> R,
): R {
    contract { callsInPlace(action, InvocationKind.EXACTLY_ONCE) }
    return KaSessionProvider.getInstance(useSiteElement.project)
        .analyze(useSiteElement, action)
}

/**
 * Executes the given [action] in an [analysis session][KaSession] context.
 *
 * The [KaSession] is provided as a context parameter, so members of the Analysis API are available inside [action] without an explicit
 * receiver.
 *
 * The project will be analyzed from the perspective of the given [useSiteModule].
 *
 * Neither the analysis session nor any other [lifetime owners][org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeOwner] may be leaked
 * outside the [analyze] block. Please consult the documentation of [KaSession] for important information about lifetime management.
 */
public inline fun <R> analyze(
    useSiteModule: KaModule,
    action: context(KaSession) () -> R,
): R {
    contract { callsInPlace(action, InvocationKind.EXACTLY_ONCE) }
    return KaSessionProvider.getInstance(useSiteModule.project)
        .analyze(useSiteModule, action)
}

/**
 * Executes the given [action] in a [KaSession] context.
 *
 * The [KaSession] is provided as a context parameter, so members of the Analysis API are available inside [action] without an explicit
 * receiver.
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
    crossinline action: context(KaSession) () -> R,
): R {
    val containingFile = useSiteElement.containingKtFile
    return withDanglingFileResolutionMode(containingFile, resolutionMode) {
        analyze(containingFile, action)
    }
}
