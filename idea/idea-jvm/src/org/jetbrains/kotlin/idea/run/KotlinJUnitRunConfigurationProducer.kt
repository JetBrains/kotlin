/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.run

import com.intellij.execution.*
import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.actions.ConfigurationFromContext
import com.intellij.execution.actions.RunConfigurationProducer
import com.intellij.execution.configurations.ModuleBasedConfiguration
import com.intellij.execution.junit.*
import com.intellij.execution.testframework.AbstractPatternBasedConfigurationProducer
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.asJava.toLightClass
import org.jetbrains.kotlin.idea.util.ProjectRootsUtil
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType

class KotlinJUnitRunConfigurationProducer : RunConfigurationProducer<JUnitConfiguration>(JUnitConfigurationType.getInstance()) {
    override fun shouldReplace(self: ConfigurationFromContext, other: ConfigurationFromContext): Boolean {
        return other.isProducedBy(JUnitConfigurationProducer::class.java) || other.isProducedBy(AbstractPatternBasedConfigurationProducer::class.java)
    }

    override fun isConfigurationFromContext(
        configuration: JUnitConfiguration,
        context: ConfigurationContext
    ): Boolean {
        if (RunConfigurationProducer.getInstance(PatternConfigurationProducer::class.java).isMultipleElementsSelected(context)) {
            return false
        }

        val leaf = context.location?.psiElement ?: return false
        val methodLocation = getTestMethodLocation(leaf)
        val testClass = getTestClass(leaf)
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
        val predefinedModule = (template.configuration as ModuleBasedConfigurationAny).configurationModule.module
        val configurationModule = configuration.configurationModule.module
        return configurationModule == context.location?.module?.asJvmModule() || configurationModule == predefinedModule
    }

    override fun setupConfigurationFromContext(
        configuration: JUnitConfiguration,
        context: ConfigurationContext,
        sourceElement: Ref<PsiElement>
    ): Boolean {
        if (DumbService.getInstance(context.project).isDumb) return false

        val location = context.location ?: return false
        val leaf = location.psiElement
        val module = context.module?.asJvmModule() ?: return false

        if (!ProjectRootsUtil.isInProjectOrLibSource(leaf)) {
            return false
        }

        if (leaf.containingFile !is KtFile) {
            return false
        }

        val methodLocation = getTestMethodLocation(leaf)
        if (methodLocation != null) {
            configuration.beMethodConfiguration(methodLocation)
            JavaRunConfigurationExtensionManagerUtil.getInstance().extendCreatedConfiguration(configuration, location)
            configuration.setModule(module)
            return true
        }

        val testClass = getTestClass(leaf)
        if (testClass != null) {
            configuration.beClassConfiguration(testClass)
            JavaRunConfigurationExtensionManagerUtil.getInstance().extendCreatedConfiguration(configuration, location)
            configuration.setModule(module)
            return true
        }

        return false
    }

    override fun onFirstRun(fromContext: ConfigurationFromContext, context: ConfigurationContext, performRunnable: Runnable) {
        val leaf = fromContext.sourceElement
        getTestClass(leaf)?.let { testClass ->
            val fromContextSubstitute = object : ConfigurationFromContext() {
                override fun getConfigurationSettings() = fromContext.configurationSettings

                override fun setConfigurationSettings(configurationSettings: RunnerAndConfigurationSettings) {
                    fromContext.configurationSettings = configurationSettings
                }

                override fun getSourceElement() = testClass
            }
            // TODO: use TestClassConfigurationProducer when constructor becomes public
            return object : AbstractTestClassConfigurationProducer(JUnitConfigurationType.getInstance()){}
                .onFirstRun(fromContextSubstitute, context, performRunnable)
        }

        super.onFirstRun(fromContext, context, performRunnable)
    }

    companion object {
        fun getTestClass(leaf: PsiElement): PsiClass? {
            val containingFile = leaf.containingFile as? KtFile ?: return null
            var ktClass = leaf.getParentOfType<KtClass>(false)
            if (!ktClass.isJUnitTestClass()) {
                ktClass = getTestClassInFile(containingFile)
            }
            return ktClass?.toLightClass()
        }

        fun getTestMethodLocation(leaf: PsiElement): Location<PsiMethod>? {
            val function = leaf.getParentOfType<KtNamedFunction>(false) ?: return null
            val owner = PsiTreeUtil.getParentOfType(function, KtFunction::class.java, KtClass::class.java)

            if (owner is KtClass) {
                val delegate = owner.toLightClass() ?: return null
                val method = delegate.methods.firstOrNull() { it.navigationElement == function } ?: return null
                val methodLocation = PsiLocation.fromPsiElement(method)
                if (JUnitUtil.isTestMethod(methodLocation, false)) {
                    return methodLocation
                }
            }
            return null
        }

        private fun KtClass?.isJUnitTestClass() =
            this?.toLightClass()?.let { JUnitUtil.isTestClass(it, false, true) } ?: false

        private fun getTestClassInFile(ktFile: KtFile) =
            ktFile.declarations.filterIsInstance<KtClass>().singleOrNull { it.isJUnitTestClass() }
    }
}
