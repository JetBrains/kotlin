/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.caches.resolve

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.ControlFlowException
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.module.ModuleUtil
import com.intellij.openapi.project.IndexNotReadyException
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiNamedElement
import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.analyzer.ModuleInfo
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.idea.KotlinPluginUtil
import org.jetbrains.kotlin.idea.caches.project.IdeaModuleInfo
import org.jetbrains.kotlin.idea.caches.project.getNullableModuleInfo
import org.jetbrains.kotlin.idea.resolve.ResolutionFacade
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.TargetPlatform
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.utils.KotlinExceptionWithAttachments

/**
 * Temporary allow resolve in write action.
 *
 * All resolve should be banned from write action. This method is needed for the transition period to document
 * places that are not fixed yet.
 */
fun <T> allowResolveInWriteAction(runnable: () -> T): T {
    return ResolveInWriteActionManager.runWithResolveAllowedInWriteAction(runnable)
}

internal object ResolveInWriteActionManager {
    private val LOG = Logger.getInstance(ResolveInWriteActionManager::class.java)

    private val isResolveInWriteActionAllowed: ThreadLocal<Boolean> = ThreadLocal.withInitial { false }

    fun assertNoResolveUnderWriteAction() {
        if (isResolveInWriteActionAllowed.get()) return

        val application = ApplicationManager.getApplication() ?: return

        if (!application.isWriteAccessAllowed) return

        if (application.isUnitTestMode) return
        if (application.isInternal && !KotlinPluginUtil.isSnapshotVersion()) return

        LOG.error("Resolve is not allowed under the write action!")
    }

    inline fun <T> runWithResolveAllowedInWriteAction(runnable: () -> T): T {
        val wasSet =
            if (ApplicationManager.getApplication()?.isWriteAccessAllowed == true && isResolveInWriteActionAllowed.get() == false) {
                isResolveInWriteActionAllowed.set(true)
                true
            } else {
                false
            }

        try {
            return runnable()
        } finally {
            if (wasSet) {
                isResolveInWriteActionAllowed.set(false)
            }
        }
    }
}