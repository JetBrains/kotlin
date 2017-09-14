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

import com.intellij.codeInsight.TestFrameworks
import com.intellij.execution.TestStateStorage
import com.intellij.execution.lineMarker.ExecutorAction
import com.intellij.execution.lineMarker.RunLineMarkerContributor
import com.intellij.execution.testframework.TestIconMapper
import com.intellij.execution.testframework.sm.runner.states.TestStateInfo
import com.intellij.icons.AllIcons
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.asJava.classes.KtLightClass
import org.jetbrains.kotlin.asJava.toLightClass
import org.jetbrains.kotlin.asJava.toLightMethods
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptorWithResolutionScopes
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptorIfAny
import org.jetbrains.kotlin.idea.js.getJsOutputFilePath
import org.jetbrains.kotlin.idea.project.TargetPlatformDetector
import org.jetbrains.kotlin.idea.util.string.joinWithEscape
import org.jetbrains.kotlin.js.resolve.JsPlatform
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.psi.psiUtil.parentsWithSelf
import org.jetbrains.kotlin.resolve.jvm.platform.JvmPlatform
import javax.swing.Icon

class KotlinTestRunLineMarkerContributor : RunLineMarkerContributor() {
    companion object {
        private val TEST_FQ_NAME = FqName("kotlin.test.Test")
        private val IGNORE_FQ_NAME = FqName("kotlin.test.Ignore")
    }

    private fun getTestStateIcon(url: String, project: Project): Icon? {
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

    private fun getJavaTestIcon(declaration: KtNamedDeclaration): Icon? {
        val (url, framework) = when (declaration) {
            is KtClassOrObject -> {
                val lightClass = declaration.toLightClass() ?: return null
                val framework = TestFrameworks.detectFramework(lightClass) ?: return null
                if (!framework.isTestClass(lightClass)) return null
                val qualifiedName = lightClass.qualifiedName ?: return null

                "java:suite://$qualifiedName" to framework
            }

            is KtNamedFunction -> {
                val lightMethod = declaration.toLightMethods().firstOrNull() ?: return null
                val lightClass = lightMethod.containingClass as? KtLightClass ?: return null
                val framework = TestFrameworks.detectFramework(lightClass) ?: return null
                if (!framework.isTestMethod(lightMethod)) return null

                "java:test://${lightClass.qualifiedName}.${lightMethod.name}" to framework
            }

            else -> return null
        }
        return getTestStateIcon(url, declaration.project) ?: framework.icon
    }

    private fun DeclarationDescriptor.isIgnored(): Boolean =
            annotations.any { it.fqName == IGNORE_FQ_NAME } || ((containingDeclaration as? ClassDescriptor)?.isIgnored() ?: false)

    private fun DeclarationDescriptor.isTest(): Boolean {
        if (isIgnored()) return false

        if (annotations.any { it.fqName == TEST_FQ_NAME }) return true
        if (this is ClassDescriptorWithResolutionScopes) {
            return declaredCallableMembers.any { it.isTest() }
        }
        return false
    }

    private fun getJavaScriptTestIcon(declaration: KtNamedDeclaration, descriptor: DeclarationDescriptor): Icon? {
        if (!descriptor.isTest()) return null

        val module = ModuleUtilCore.findModuleForPsiElement(declaration) ?: return null
        val testFilePath = getJsOutputFilePath(module, true, false) ?: return null

        val locations = ArrayList<String>()

        locations += FileUtil.toSystemDependentName(testFilePath)

        val klass = when (declaration) {
            is KtClassOrObject -> declaration
            is KtNamedFunction -> declaration.containingClassOrObject ?: return null
            else -> return null
        }
        locations += klass.parentsWithSelf.filterIsInstance<KtNamedDeclaration>().mapNotNull { it.name }.toList().asReversed()

        val testName = (declaration as? KtNamedFunction)?.name
        if (testName != null) {
            locations += "$testName"
        }

        val prefix = if (testName != null) "test://" else "suite://"

        val url = prefix + locations.joinWithEscape('.')

        return getTestStateIcon(url, declaration.project)
    }

    override fun getInfo(element: PsiElement): RunLineMarkerContributor.Info? {
        val declaration = element.getStrictParentOfType<KtNamedDeclaration>() ?: return null
        if (declaration.nameIdentifier != element) return null

        if (declaration !is KtClassOrObject && declaration !is KtNamedFunction) return null

        // To prevent IDEA failing on red code
        val descriptor = declaration.resolveToDescriptorIfAny() ?: return null

        val platform = TargetPlatformDetector.getPlatform(declaration.containingKtFile)
        val icon = when (platform) {
            is JvmPlatform -> getJavaTestIcon(declaration)
            is JsPlatform -> getJavaScriptTestIcon(declaration, descriptor)
            else -> return null
        }
        return RunLineMarkerContributor.Info(icon, { "Run Test" }, ExecutorAction.getActions())
    }
}
