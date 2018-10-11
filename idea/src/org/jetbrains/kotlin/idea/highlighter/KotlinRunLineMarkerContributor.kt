/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.idea.highlighter

import com.intellij.execution.lineMarker.ExecutorAction
import com.intellij.execution.lineMarker.RunLineMarkerContributor
import com.intellij.icons.AllIcons
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.idea.isMainFunction
import org.jetbrains.kotlin.idea.platform.tooling
import org.jetbrains.kotlin.idea.project.platform
import org.jetbrains.kotlin.idea.util.module
import org.jetbrains.kotlin.psi.KtNamedFunction

class KotlinRunLineMarkerContributor : RunLineMarkerContributor() {
    override fun getInfo(element: PsiElement): Info? {
        val function = element.parent as? KtNamedFunction ?: return null

        if (function.nameIdentifier != element) return null

        if (function.isMainFunction()) {
            val platform = function.containingKtFile.module?.platform ?: return null
            if (!platform.kind.tooling.acceptsAsEntryPoint(function)) return null

            return RunLineMarkerContributor.Info(AllIcons.RunConfigurations.TestState.Run, null, ExecutorAction.getActions(0))
        }

        return null
    }
}
