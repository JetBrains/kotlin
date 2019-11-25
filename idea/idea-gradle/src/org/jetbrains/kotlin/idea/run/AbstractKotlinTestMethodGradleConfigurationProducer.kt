/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.run

import com.intellij.execution.Location
import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.actions.ConfigurationFromContext
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.junit.InheritorChooser
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemRunConfiguration
import com.intellij.openapi.module.Module
import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import org.jetbrains.kotlin.idea.caches.project.isNewMPPModule
import org.jetbrains.kotlin.idea.project.platform
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.plugins.gradle.execution.test.runner.TestMethodGradleConfigurationProducer
import org.jetbrains.plugins.gradle.execution.test.runner.applyTestConfiguration
import org.jetbrains.plugins.gradle.service.execution.GradleRunConfiguration
import org.jetbrains.plugins.gradle.util.GradleConstants
import org.jetbrains.plugins.gradle.util.GradleExecutionSettingsUtil.createTestFilterFrom

abstract class AbstractKotlinMultiplatformTestMethodGradleConfigurationProducer : AbstractKotlinTestMethodGradleConfigurationProducer() {
    override val forceGradleRunner: Boolean get() = true
    override val hasTestFramework: Boolean get() = true

    private val mppTestTasksChooser = MultiplatformTestTasksChooser()

    abstract fun isApplicable(module: Module, platform: TargetPlatform): Boolean

    final override fun isApplicable(module: Module): Boolean {
        if (!module.isNewMPPModule) {
            return false
        }

        val platform = module.platform ?: return false
        return isApplicable(module, platform)
    }

    override fun isPreferredConfiguration(self: ConfigurationFromContext, other: ConfigurationFromContext): Boolean {
        return other.isJpsJunitConfiguration() || super.isPreferredConfiguration(self, other)
    }

    override fun shouldReplace(self: ConfigurationFromContext, other: ConfigurationFromContext): Boolean {
        return other.isJpsJunitConfiguration() || super.shouldReplace(self, other)
    }

    override fun onFirstRun(fromContext: ConfigurationFromContext, context: ConfigurationContext, performRunnable: Runnable) {
        val psiMethod = fromContext.sourceElement as PsiMethod
        val psiClass = psiMethod.containingClass ?: return

        val inheritorChooser = object : InheritorChooser() {
            override fun runForClasses(classes: List<PsiClass>, method: PsiMethod?, context: ConfigurationContext, runnable: Runnable) {
                chooseTestClassConfiguration(fromContext, context, runnable, psiMethod, *classes.toTypedArray())
            }

            override fun runForClass(aClass: PsiClass, psiMethod: PsiMethod, context: ConfigurationContext, runnable: Runnable) {
                chooseTestClassConfiguration(fromContext, context, runnable, psiMethod, aClass)
            }
        }
        if (inheritorChooser.runMethodInAbstractClass(context, performRunnable, psiMethod, psiClass)) return
        chooseTestClassConfiguration(fromContext, context, performRunnable, psiMethod, psiClass)
    }

    private fun chooseTestClassConfiguration(
        fromContext: ConfigurationFromContext,
        context: ConfigurationContext,
        performRunnable: Runnable,
        psiMethod: PsiMethod,
        vararg classes: PsiClass
    ) {
        val dataContext = MultiplatformTestTasksChooser.createContext(context.dataContext, psiMethod.name)

        mppTestTasksChooser.multiplatformChooseTasks(context.project, dataContext, classes.asList()) { tasks ->
            val configuration = fromContext.configuration as ExternalSystemRunConfiguration
            val settings = configuration.settings

            val result = settings.applyTestConfiguration(context.module, tasks, *classes) {
                createTestFilterFrom(context.location, it, psiMethod, true)
            }

            if (result) {
                configuration.name = (if (classes.size == 1) classes[0].name!! + "." else "") + psiMethod.name
                performRunnable.run()
            } else {
                LOG.warn("Cannot apply method test configuration, uses raw run configuration")
                performRunnable.run()
            }
        }
    }
}

abstract class AbstractKotlinTestMethodGradleConfigurationProducer
    : TestMethodGradleConfigurationProducer(), KotlinGradleConfigurationProducer
{
    override fun getConfigurationFactory(): ConfigurationFactory {
        return KotlinGradleExternalTaskConfigurationType.instance.factory
    }

    override fun isConfigurationFromContext(configuration: ExternalSystemRunConfiguration, context: ConfigurationContext): Boolean {
        if (!context.check()) {
            return false
        }

        if (!forceGradleRunner) {
            return super.isConfigurationFromContext(configuration, context)
        }

        if (GradleConstants.SYSTEM_ID != configuration.settings.externalSystemId) return false
        return doIsConfigurationFromContext(configuration, context)
    }

    override fun setupConfigurationFromContext(
        configuration: ExternalSystemRunConfiguration,
        context: ConfigurationContext,
        sourceElement: Ref<PsiElement>
    ): Boolean {
        if (!context.check()) {
            return false
        }

        if (!forceGradleRunner) {
            return super.setupConfigurationFromContext(configuration, context, sourceElement)
        }

        if (GradleConstants.SYSTEM_ID != configuration.settings.externalSystemId) return false
        if (sourceElement.isNull) return false

        (configuration as? GradleRunConfiguration)?.isScriptDebugEnabled = false
        return doSetupConfigurationFromContext(configuration, context, sourceElement)
    }

    private fun ConfigurationContext.check(): Boolean {
        return hasTestFramework && module != null && isApplicable(module)
    }

    override fun getPsiMethodForLocation(contextLocation: Location<*>) = getTestMethodForKotlinTest(contextLocation)

    override fun isPreferredConfiguration(self: ConfigurationFromContext, other: ConfigurationFromContext): Boolean {
        return checkShouldReplace(self, other) || super.isPreferredConfiguration(self, other)
    }

    override fun shouldReplace(self: ConfigurationFromContext, other: ConfigurationFromContext): Boolean {
        return checkShouldReplace(self, other) || super.shouldReplace(self, other)
    }

    private fun checkShouldReplace(self: ConfigurationFromContext, other: ConfigurationFromContext): Boolean {
        if (self.isProducedBy(javaClass)) {
            if (other.isProducedBy(TestMethodGradleConfigurationProducer::class.java) ||
                other.isProducedBy(AbstractKotlinTestClassGradleConfigurationProducer::class.java)
            ) {
                return true
            }
        }

        return false
    }
}
