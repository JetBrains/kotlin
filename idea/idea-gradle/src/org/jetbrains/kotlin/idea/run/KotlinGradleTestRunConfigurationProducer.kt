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

import com.intellij.execution.Location
import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.actions.ConfigurationFromContext
import com.intellij.ide.plugins.PluginManager
import com.intellij.openapi.extensions.PluginId.getId
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemRunConfiguration
import com.intellij.openapi.util.Ref
import com.intellij.openapi.util.component1
import com.intellij.openapi.util.component2
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import org.jetbrains.kotlin.idea.caches.project.isMPPModule
import org.jetbrains.plugins.gradle.execution.test.runner.TestClassGradleConfigurationProducer
import org.jetbrains.plugins.gradle.execution.test.runner.TestMethodGradleConfigurationProducer

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
        KotlinJUnitRunConfigurationProducer.getTestMethod(leaf)?.let { return it }
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
        context.module?.asJvmModule() ?: return false

        return super.doSetupConfigurationFromContext(configuration, context, sourceElement)
    }

    override fun getPsiClassForLocation(contextLocation: Location<*>): PsiClass? {
        val leaf = contextLocation.psiElement ?: return null
        return getTestClass(leaf)
    }

    override fun getPsiMethodForLocation(contextLocation: Location<*>): PsiMethod? {
        val leaf = contextLocation.psiElement ?: return null
        return getTestMethod(leaf)
    }

    override fun onFirstRun(fromContext: ConfigurationFromContext, context: ConfigurationContext, performRunnable: Runnable) {
        if (context.location?.module?.isMPPModule == true) {
            // TODO: remove hack when IDEA has new API
            performRunnable.run()
        } else {
            super.onFirstRun(fromContext, context, performRunnable)
        }
    }

    override fun doIsConfigurationFromContext(configuration: ExternalSystemRunConfiguration, context: ConfigurationContext): Boolean {
        if (!IS_TEST_FRAMEWORK_PLUGIN_ENABLED) return false
        context.module?.asJvmModule() ?: return false

        return super.doIsConfigurationFromContext(configuration, context)
    }
}

class KotlinTestMethodGradleConfigurationProducer : TestMethodGradleConfigurationProducer() {
    override fun doSetupConfigurationFromContext(
        configuration: ExternalSystemRunConfiguration,
        context: ConfigurationContext,
        sourceElement: Ref<PsiElement>
    ): Boolean {
        if (!IS_TEST_FRAMEWORK_PLUGIN_ENABLED) return false
        context.module?.asJvmModule() ?: return false

        return super.doSetupConfigurationFromContext(configuration, context, sourceElement)
    }

    override fun getPsiMethodForLocation(contextLocation: Location<*>): PsiMethod? {
        val leaf = contextLocation.psiElement ?: return null
        return getTestMethod(leaf)
    }

    override fun onFirstRun(fromContext: ConfigurationFromContext, context: ConfigurationContext, performRunnable: Runnable) {
        if (context.location?.module?.isMPPModule == true) {
            // TODO: remove hack when IDEA has new API
            performRunnable.run()
        } else {
            super.onFirstRun(fromContext, context, performRunnable)
        }
    }

    override fun doIsConfigurationFromContext(configuration: ExternalSystemRunConfiguration, context: ConfigurationContext): Boolean {
        if (!IS_TEST_FRAMEWORK_PLUGIN_ENABLED) return false
        context.module?.asJvmModule() ?: return false

        return super.doIsConfigurationFromContext(configuration, context)
    }
}
