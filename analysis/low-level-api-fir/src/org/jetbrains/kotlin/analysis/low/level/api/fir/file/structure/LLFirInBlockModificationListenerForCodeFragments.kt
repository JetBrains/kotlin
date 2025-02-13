/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.file.structure

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.analysis.api.platform.modification.publishCodeFragmentContextModificationEvent
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.psi.KtElement

internal class LLFirInBlockModificationListenerForCodeFragments(val project: Project) : LLFirInBlockModificationListener {
    override fun afterModification(element: KtElement, module: KaModule) {
        module.publishCodeFragmentContextModificationEvent()
    }
}
