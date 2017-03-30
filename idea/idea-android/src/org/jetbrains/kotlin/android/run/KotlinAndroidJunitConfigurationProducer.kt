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

package org.jetbrains.kotlin.android.run

import com.android.tools.idea.testartifacts.junit.TestClassAndroidConfigurationProducer
import com.intellij.execution.CommonJavaRunConfigurationParameters
import com.intellij.execution.JavaRunConfigurationExtensionManager
import com.intellij.execution.RunManager
import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.actions.ConfigurationFromContext
import com.intellij.execution.actions.RunConfigurationProducer
import com.intellij.execution.configurations.ModuleBasedConfiguration
import com.intellij.execution.junit.JUnitConfiguration
import com.intellij.execution.junit.JUnitConfigurationProducer
import com.intellij.execution.junit.JUnitConfigurationType
import com.intellij.execution.junit.PatternConfigurationProducer
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.idea.project.TargetPlatformDetector
import org.jetbrains.kotlin.idea.run.KotlinJUnitRunConfigurationProducer
import org.jetbrains.kotlin.idea.util.ProjectRootsUtil
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.jvm.platform.JvmPlatform


class KotlinAndroidJUnitRunConfigurationProducer : TestClassAndroidConfigurationProducer() {

    override fun shouldReplace(self: ConfigurationFromContext, other: ConfigurationFromContext): Boolean {
        return other.isProducedBy(JUnitConfigurationProducer::class.java) || other.isProducedBy(PatternConfigurationProducer::class.java)
    }

    override fun isConfigurationFromContext(configuration: JUnitConfiguration,
                                            context: ConfigurationContext): Boolean {
        if (RunConfigurationProducer.getInstance(PatternConfigurationProducer::class.java).isMultipleElementsSelected(context)) {
            return false
        }

        val leaf = context.location?.psiElement ?: return false
        val methodLocation = KotlinJUnitRunConfigurationProducer.getTestMethodLocation(leaf)
        val testClass = KotlinJUnitRunConfigurationProducer.getTestClass(leaf)
        val testObject = configuration.testObject

        if (!testObject.isConfiguredByElement(configuration, testClass, methodLocation?.psiElement, null, null)) {
            return false
        }

        return settingsMatchTemplate(configuration, context)
    }

    // copied from JUnitConfigurationProducer in IDEA
    private fun settingsMatchTemplate(configuration: JUnitConfiguration, context: ConfigurationContext): Boolean {
        val predefinedConfiguration = context.getOriginalConfiguration(JUnitConfigurationType.getInstance())

        val vmParameters = (predefinedConfiguration as? CommonJavaRunConfigurationParameters)?.vmParameters
        if (vmParameters != null && configuration.vmParameters != vmParameters) return false

        val template = RunManager.getInstance(configuration.project).getConfigurationTemplate(configurationFactory)
        val predefinedModule = (template.configuration as ModuleBasedConfiguration<*>).configurationModule.module
        val configurationModule = configuration.configurationModule.module
        return configurationModule == context.location?.module || configurationModule == predefinedModule
    }

    override fun setupConfigurationFromContext(configuration: JUnitConfiguration,
                                               context: ConfigurationContext,
                                               sourceElement: Ref<PsiElement>): Boolean {
        if (DumbService.getInstance(context.project).isDumb) return false

        val location = context.location ?: return false
        val leaf = location.psiElement

        if (!ProjectRootsUtil.isInProjectOrLibSource(leaf)) {
            return false
        }

        if (leaf.containingFile !is KtFile) {
            return false
        }

        val ktFile = leaf.containingFile as KtFile

        if (TargetPlatformDetector.getPlatform(ktFile) != JvmPlatform) {
            return false
        }

        val methodLocation = KotlinJUnitRunConfigurationProducer.getTestMethodLocation(leaf)
        if (methodLocation != null) {
            val originalModule = configuration.configurationModule.module
            configuration.beMethodConfiguration(methodLocation)
            configuration.restoreOriginalModule(originalModule)
            JavaRunConfigurationExtensionManager.getInstance().extendCreatedConfiguration(configuration, location)
            return true
        }

        val testClass = KotlinJUnitRunConfigurationProducer.getTestClass(leaf)
        if (testClass != null) {
            val originalModule = configuration.configurationModule.module
            configuration.beClassConfiguration(testClass)
            configuration.restoreOriginalModule(originalModule)
            JavaRunConfigurationExtensionManager.getInstance().extendCreatedConfiguration(configuration, location)
            return true
        }

        return false
    }

    override fun onFirstRun(fromContext: ConfigurationFromContext, context: ConfigurationContext, performRunnable: Runnable) {
        val leaf = fromContext.sourceElement
        val testClass = KotlinJUnitRunConfigurationProducer.getTestClass(leaf)
        val fromContextSubstitute = if (testClass != null) {
            object : ConfigurationFromContext() {
                override fun getConfigurationSettings() = fromContext.configurationSettings
                override fun getSourceElement() = testClass!!  // TODO: remove !! when smartcast will work here
                override fun setConfigurationSettings(configurationSettings: RunnerAndConfigurationSettings) {
                    fromContext.configurationSettings = configurationSettings
                }
            }
        }
        else {
            fromContext
        }

        super.onFirstRun(fromContextSubstitute, context, performRunnable)
    }
}