/*
 * Copyright 2010-2017 JetBrains s.r.o.
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
package org.jetbrains.kotlin.idea.run

import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.actions.RunConfigurationProducer
import com.intellij.execution.junit.PatternConfigurationProducer
import com.intellij.ide.plugins.PluginManager
import com.intellij.openapi.extensions.PluginId.getId
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemRunConfiguration
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.util.Ref
import com.intellij.openapi.util.component1
import com.intellij.openapi.util.component2
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import org.jetbrains.plugins.gradle.execution.test.runner.TestClassGradleConfigurationProducer
import org.jetbrains.plugins.gradle.execution.test.runner.TestMethodGradleConfigurationProducer
import org.jetbrains.plugins.gradle.util.GradleConstants

private val IS_JUNIT_ENABLED by lazy { isPluginEnabled("JUnit") }
private val IS_TESTNG_ENABLED by lazy { isPluginEnabled("TestNG-J") }
private val IS_TEST_FRAMEWORK_PLUGIN_ENABLED by lazy { IS_JUNIT_ENABLED || IS_TESTNG_ENABLED }

private fun isPluginEnabled(id: String): Boolean {
    return PluginManager.isPluginInstalled(getId(id)) && id !in PluginManager.getDisabledPlugins()
}

private fun getTestClass(leaf: PsiElement): PsiClass? {
    if (IS_JUNIT_ENABLED) {
        KotlinJUnitRunConfigurationProducer.getTestClass(leaf)?.let { return it }
    }
    if (IS_TESTNG_ENABLED) {
        KotlinTestNgConfigurationProducer.getTestClassAndMethod(leaf)?.let { (testClass, testMethod) ->
            return if (testMethod == null) testClass else null
        }
    }
    return null
}

private fun getTestMethod(leaf: PsiElement): PsiMethod? {
    if (IS_JUNIT_ENABLED) {
        KotlinJUnitRunConfigurationProducer.getTestMethodLocation(leaf)?.psiElement?.let { return it }
    }
    if (IS_TESTNG_ENABLED) {
        KotlinTestNgConfigurationProducer.getTestClassAndMethod(leaf)?.second?.let { return it }
    }
    return null
}

class KotlinTestClassGradleConfigurationProducer : TestClassGradleConfigurationProducer() {
    override fun doSetupConfigurationFromContext(
        configuration: ExternalSystemRunConfiguration,
        context: ConfigurationContext,
        sourceElement: Ref<PsiElement>
    ): Boolean {
        if (!IS_TEST_FRAMEWORK_PLUGIN_ENABLED) return false

        val contextLocation = context.location ?: return false
        val module = context.module?.asJvmModule() ?: return false

        if (RunConfigurationProducer.getInstance(PatternConfigurationProducer::class.java).isMultipleElementsSelected(context)) {
            return false
        }
        val leaf = context.location?.psiElement ?: return false
        val testClass = getTestClass(leaf) ?: return false
        sourceElement.set(testClass)

        if (!ExternalSystemApiUtil.isExternalSystemAwareModule(GradleConstants.SYSTEM_ID, module)) return false

        val projectPath = resolveProjectPath(module) ?: return false

        val tasksToRun = getTasksToRun(module)
        if (tasksToRun.isEmpty()) return false

        configuration.settings.externalProjectPath = projectPath
        configuration.settings.taskNames = tasksToRun
        configuration.settings.scriptParameters = String.format("--tests \"%s\"", testClass.qualifiedName)
        configuration.name = testClass.name ?: "unknown"

        JavaRunConfigurationExtensionManagerUtil.getInstance().extendCreatedConfiguration(configuration, contextLocation)
        return true
    }

    override fun doIsConfigurationFromContext(configuration: ExternalSystemRunConfiguration, context: ConfigurationContext): Boolean {
        if (!IS_TEST_FRAMEWORK_PLUGIN_ENABLED) return false

        val leaf = context.location?.psiElement ?: return false
        val module = context.module?.asJvmModule() ?: return false

        if (RunConfigurationProducer.getInstance(PatternConfigurationProducer::class.java).isMultipleElementsSelected(context)) {
            return false
        }

        if (getTestMethod(leaf) != null) return false

        val testClass = getTestClass(leaf)
        if (testClass == null || testClass.qualifiedName == null) return false


        val projectPath = resolveProjectPath(module) ?: return false
        if (projectPath != configuration.settings.externalProjectPath) {
            return false
        }
        if (!configuration.settings.taskNames.containsAll(getTasksToRun(module))) return false

        val scriptParameters = configuration.settings.scriptParameters + ' '
        val i = scriptParameters.indexOf("--tests ")
        if (i == -1) return false

        val str = scriptParameters.substringAfter("--tests ").trim() + ' '
        return str.startsWith("\"" + testClass.qualifiedName + "\"" + ' ') && !str.contains("--tests")
    }
}

class KotlinTestMethodGradleConfigurationProducer : TestMethodGradleConfigurationProducer() {
    override fun doSetupConfigurationFromContext(
        configuration: ExternalSystemRunConfiguration,
        context: ConfigurationContext,
        sourceElement: Ref<PsiElement>
    ): Boolean {
        if (!IS_TEST_FRAMEWORK_PLUGIN_ENABLED) return false

        val contextLocation = context.location ?: return false
        context.module?.asJvmModule() ?: return false

        if (RunConfigurationProducer.getInstance(PatternConfigurationProducer::class.java).isMultipleElementsSelected(context)) {
            return false
        }

        val psiMethod = getTestMethod(contextLocation.psiElement) ?: return false
        sourceElement.set(psiMethod)

        val containingClass = psiMethod.containingClass ?: return false


        if (!applyTestMethodConfiguration(configuration, context, psiMethod, containingClass)) return false

        JavaRunConfigurationExtensionManagerUtil.getInstance().extendCreatedConfiguration(configuration, contextLocation)
        return true
    }

    override fun doIsConfigurationFromContext(configuration: ExternalSystemRunConfiguration, context: ConfigurationContext): Boolean {
        if (!IS_TEST_FRAMEWORK_PLUGIN_ENABLED) return false

        if (RunConfigurationProducer.getInstance(PatternConfigurationProducer::class.java).isMultipleElementsSelected(context)) {
            return false
        }

        val contextLocation = context.location ?: return false
        val module = context.module?.asJvmModule() ?: return false

        val psiMethod = getTestMethod(contextLocation.psiElement) ?: return false

        val containingClass = psiMethod.containingClass ?: return false


        val projectPath = resolveProjectPath(module) ?: return false

        if (projectPath != configuration.settings.externalProjectPath) {
            return false
        }
        if (!configuration.settings.taskNames.containsAll(getTasksToRun(module))) return false

        val scriptParameters = configuration.settings.scriptParameters + ' '
        val testFilter = createTestFilter(containingClass, psiMethod)
        return scriptParameters.contains(testFilter!!)
    }

    private fun applyTestMethodConfiguration(
        configuration: ExternalSystemRunConfiguration,
        context: ConfigurationContext,
        psiMethod: PsiMethod,
        vararg containingClasses: PsiClass
    ): Boolean {
        val module = context.module ?: return false

        if (!ExternalSystemApiUtil.isExternalSystemAwareModule(GradleConstants.SYSTEM_ID, module)) return false

        val projectPath = resolveProjectPath(module) ?: return false

        val tasksToRun = getTasksToRun(module)
        if (tasksToRun.isEmpty()) return false

        configuration.settings.externalProjectPath = projectPath
        configuration.settings.taskNames = tasksToRun

        val params = containingClasses.joinToString("") { aClass -> createTestFilter(aClass, psiMethod) ?: "" }

        configuration.settings.scriptParameters = params.trim()
        configuration.name = (if (containingClasses.size == 1) containingClasses[0].name + "." else "") + psiMethod.name
        return true
    }

    companion object {

        private fun createTestFilter(aClass: PsiClass, psiMethod: PsiMethod): String? {
            return createTestFilter(aClass.qualifiedName, psiMethod.name)
        }

        fun createTestFilter(aClass: String?, method: String?): String? {
            if (aClass == null) return null
            val testFilterPattern = aClass + if (method == null) "" else '.' + method
            return "--tests \"$testFilterPattern\" "
        }
    }
}
