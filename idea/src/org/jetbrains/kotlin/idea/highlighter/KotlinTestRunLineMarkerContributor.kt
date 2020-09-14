/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
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
import com.intellij.util.Function
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptorIfAny
import org.jetbrains.kotlin.idea.platform.tooling
import org.jetbrains.kotlin.idea.project.platform
import org.jetbrains.kotlin.idea.util.projectStructure.module
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.platform.SimplePlatform
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.idePlatformKind
import org.jetbrains.kotlin.platform.konan.NativePlatformWithTarget
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.psiUtil.containingClass
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import javax.swing.Icon

class KotlinTestRunLineMarkerContributor : RunLineMarkerContributor() {
    companion object {
        fun getTestStateIcon(url: String, project: Project, strict: Boolean): Icon? {
            val defaultIcon = AllIcons.RunConfigurations.TestState.Run
            val state = TestStateStorage.getInstance(project).getState(url)
                ?: return if (strict) null else defaultIcon

            return when (TestIconMapper.getMagnitude(state.magnitude)) {
                TestStateInfo.Magnitude.ERROR_INDEX,
                TestStateInfo.Magnitude.FAILED_INDEX -> AllIcons.RunConfigurations.TestState.Red2
                TestStateInfo.Magnitude.PASSED_INDEX,
                TestStateInfo.Magnitude.COMPLETE_INDEX -> AllIcons.RunConfigurations.TestState.Green2
                else -> defaultIcon
            }
        }

        fun SimplePlatform.providesRunnableTests(): Boolean {
            if (this is NativePlatformWithTarget) {
                return when {
                    HostManager.hostIsMac -> target in listOf(
                        KonanTarget.IOS_X64,
                        KonanTarget.MACOS_X64,
                        KonanTarget.WATCHOS_X64,
                        KonanTarget.TVOS_X64
                    )
                    HostManager.hostIsLinux -> target == KonanTarget.LINUX_X64
                    HostManager.hostIsMingw -> target in listOf(KonanTarget.MINGW_X86, KonanTarget.MINGW_X64)
                    else -> false
                }
            }

            return true
        }

        fun TargetPlatform.providesRunnableTests(): Boolean = componentPlatforms.any { it.providesRunnableTests() }
    }

    override fun getInfo(element: PsiElement): Info? {
        val declaration = element.getStrictParentOfType<KtNamedDeclaration>() ?: return null
        if (declaration.nameIdentifier != element) return null

        if (declaration !is KtClass && declaration !is KtNamedFunction) return null

        if (declaration is KtNamedFunction && declaration.containingClass() == null) return null

        // To prevent IDEA failing on red code
        val descriptor = declaration.resolveToDescriptorIfAny() ?: return null

        val targetPlatform = declaration.module?.platform ?: return null
        if (!targetPlatform.providesRunnableTests()) return null
        val icon = targetPlatform.idePlatformKind.tooling.getTestIcon(declaration, descriptor) ?: return null
        return Info(icon, Function { KotlinBundle.message("highlighter.tool.tip.text.run.test") }, *ExecutorAction.getActions())
    }
}
