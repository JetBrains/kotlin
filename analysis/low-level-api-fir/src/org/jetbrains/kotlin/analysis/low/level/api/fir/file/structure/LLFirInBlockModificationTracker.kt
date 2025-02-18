/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.file.structure

import com.intellij.openapi.components.service
import com.intellij.openapi.components.serviceIfCreated
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.ModificationTracker
import com.intellij.openapi.util.SimpleModificationTracker
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.psi.KtElement

/**
 * Global in-block modification tracker.
 *
 * This tracker increments each time in-block modification happens somewhere in the code.
 *
 * @see LLFirDeclarationModificationService
 */
@KaImplementationDetail
class LLFirInBlockModificationTracker : SimpleModificationTracker() {
    companion object {
        fun getInstance(project: Project): ModificationTracker = project.service<LLFirInBlockModificationTracker>()
    }

    class Listener(val project: Project) : LLFirInBlockModificationListener {
        override fun afterModification(element: KtElement, module: KaModule) {
            project.serviceIfCreated<LLFirInBlockModificationTracker>()?.incModificationCount()
        }
    }
}
