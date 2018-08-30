/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

import com.intellij.execution.TestStateStorage
import com.intellij.execution.lineMarker.ExecutorAction
import com.intellij.execution.lineMarker.RunLineMarkerContributor
import com.intellij.execution.testframework.TestIconMapper
import com.intellij.execution.testframework.sm.runner.states.TestStateInfo
import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptorIfAny
import org.jetbrains.kotlin.idea.platform.tooling
import org.jetbrains.kotlin.idea.project.platform
import org.jetbrains.kotlin.idea.util.projectStructure.module
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import javax.swing.Icon

class KotlinTestRunLineMarkerContributor : RunLineMarkerContributor() {
    companion object {
        fun getTestStateIcon(url: String, project: Project): Icon? {
            val defaultIcon = AllIcons.RunConfigurations.TestState.Run
            val state = TestStateStorage.getInstance(project).getState(url) ?: return defaultIcon
            val magnitude = TestIconMapper.getMagnitude(state.magnitude)
            return when (magnitude) {
                TestStateInfo.Magnitude.ERROR_INDEX,
                TestStateInfo.Magnitude.FAILED_INDEX -> AllIcons.RunConfigurations.TestState.Red2
                TestStateInfo.Magnitude.PASSED_INDEX,
                TestStateInfo.Magnitude.COMPLETE_INDEX -> AllIcons.RunConfigurations.TestState.Green2
                else -> defaultIcon
            }
        }
    }

    override fun getInfo(element: PsiElement): RunLineMarkerContributor.Info? {
        val declaration = element.getStrictParentOfType<KtNamedDeclaration>() ?: return null
        if (declaration.nameIdentifier != element) return null

        if (declaration !is KtClassOrObject && declaration !is KtNamedFunction) return null

        if (declaration is KtNamedFunction && declaration.containingClassOrObject == null) return null

        // To prevent IDEA failing on red code
        val descriptor = declaration.resolveToDescriptorIfAny() ?: return null

        val targetPlatform = declaration.module?.platform ?: return null
        val icon = targetPlatform.kind.tooling.getTestIcon(declaration, descriptor) ?: return null
        return RunLineMarkerContributor.Info(icon, { "Run Test" }, ExecutorAction.getActions())
    }
}
