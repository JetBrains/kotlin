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

import com.intellij.execution.actions.RunConfigurationProducer
import com.intellij.execution.lineMarker.ExecutorAction
import com.intellij.execution.lineMarker.RunLineMarkerContributor
import com.intellij.icons.AllIcons
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.idea.MainFunctionDetector
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptorIfAny
import org.jetbrains.kotlin.idea.js.KotlinJSRunConfigurationDataProvider
import org.jetbrains.kotlin.idea.project.TargetPlatformDetector
import org.jetbrains.kotlin.js.resolve.JsPlatform
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.resolve.TargetPlatform
import org.jetbrains.kotlin.resolve.jvm.platform.JvmPlatform

class KotlinRunLineMarkerContributor : RunLineMarkerContributor() {
    override fun getInfo(element: PsiElement): Info? {
        val function = element.parent as? KtNamedFunction ?: return null

        if (function.nameIdentifier != element) return null

        val detector = MainFunctionDetector { someFunction ->
            someFunction.resolveToDescriptorIfAny()
        }

        if (detector.isMain(function)) {
            val platform = TargetPlatformDetector.getPlatform(function.containingKtFile)
            val isAvailable = when (platform) {
                is JvmPlatform -> true
                is JsPlatform, is TargetPlatform.Common -> {
                    RunConfigurationProducer
                        .getProducers(function.project)
                        .asSequence()
                        .filterIsInstance<KotlinJSRunConfigurationDataProvider<*>>()
                        .filter { !it.isForTests }
                        .mapNotNull { it.getConfigurationData(function) }
                        .firstOrNull() != null
                }
                else -> false
            }
            if (!isAvailable) return null

            return RunLineMarkerContributor.Info(AllIcons.RunConfigurations.TestState.Run, null, ExecutorAction.getActions(0))
        }

        return null
    }
}