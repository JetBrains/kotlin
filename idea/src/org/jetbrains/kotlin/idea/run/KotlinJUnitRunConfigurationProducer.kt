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

package org.jetbrains.kotlin.idea.run

import com.intellij.execution.*
import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.actions.RunConfigurationProducer
import com.intellij.execution.configurations.ModuleBasedConfiguration
import com.intellij.execution.junit.JUnitConfiguration
import com.intellij.execution.junit.JUnitConfigurationType
import com.intellij.execution.junit.JUnitUtil
import com.intellij.execution.junit.PatternConfigurationProducer
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.asJava.LightClassUtil
import org.jetbrains.kotlin.idea.project.ProjectStructureUtil
import org.jetbrains.kotlin.idea.util.ProjectRootsUtil
import org.jetbrains.kotlin.psi.JetClass
import org.jetbrains.kotlin.psi.JetFile
import org.jetbrains.kotlin.psi.JetFunction
import org.jetbrains.kotlin.psi.JetNamedFunction
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType

public class KotlinJUnitRunConfigurationProducer : RunConfigurationProducer<JUnitConfiguration>(JUnitConfigurationType.getInstance()) {
    override fun isConfigurationFromContext(configuration: JUnitConfiguration,
                                            context: ConfigurationContext): Boolean {
        if (RunConfigurationProducer.getInstance(javaClass<PatternConfigurationProducer>()).isMultipleElementsSelected(context)) {
            return false
        }

        val leaf = context.getLocation()?.getPsiElement() ?: return false
        val methodLocation = getTestMethodLocation(leaf)
        val testClass = getTestClass(leaf)
        val testObject = configuration.getTestObject()

        if (!testObject.isConfiguredByElement(configuration, testClass, methodLocation?.getPsiElement(), null, null)) {
            return false
        }

        return settingsMatchTemplate(configuration, context)
    }

    // copied from JUnitConfigurationProducer in IDEA
    private fun settingsMatchTemplate(configuration: JUnitConfiguration, context: ConfigurationContext): Boolean {
        val predefinedConfiguration = context.getOriginalConfiguration(JUnitConfigurationType.getInstance())

        val vmParameters = (predefinedConfiguration as? CommonJavaRunConfigurationParameters)?.getVMParameters()
        if (vmParameters != null && configuration.getVMParameters() != vmParameters) return false

        val template = RunManager.getInstance(configuration.getProject()).getConfigurationTemplate(getConfigurationFactory())
        val predefinedModule = (template.getConfiguration() as ModuleBasedConfiguration<*>).getConfigurationModule().getModule()
        val configurationModule = configuration.getConfigurationModule().getModule()
        return configurationModule == context.getLocation()?.getModule() || configurationModule == predefinedModule
    }

    override fun setupConfigurationFromContext(configuration: JUnitConfiguration,
                                               context: ConfigurationContext,
                                               sourceElement: Ref<PsiElement>): Boolean {
        if (DumbService.getInstance(context.getProject()).isDumb()) return false

        val location = context.getLocation() ?: return false
        val leaf = location.getPsiElement()

        if (!ProjectRootsUtil.isInProjectOrLibSource(leaf)) {
            return false
        }

        if (leaf.getContainingFile() !is JetFile) {
            return false
        }

        val jetFile = leaf.getContainingFile() as JetFile

        if (ProjectStructureUtil.isJsKotlinModule(jetFile)) {
            return false
        }

        val methodLocation = getTestMethodLocation(leaf)
        if (methodLocation != null) {
            val originalModule = configuration.getConfigurationModule().getModule()
            configuration.beMethodConfiguration(methodLocation)
            configuration.restoreOriginalModule(originalModule)
            JavaRunConfigurationExtensionManager.getInstance().extendCreatedConfiguration(configuration, location)
            return true
        }

        val testClass = getTestClass(leaf)
        if (testClass != null) {
            val originalModule = configuration.getConfigurationModule().getModule()
            configuration.beClassConfiguration(testClass)
            configuration.restoreOriginalModule(originalModule)
            JavaRunConfigurationExtensionManager.getInstance().extendCreatedConfiguration(configuration, location)
            return true
        }

        return false
    }

    private fun getTestMethodLocation(leaf: PsiElement): Location<PsiMethod>? {
        val function = leaf.getParentOfType<JetNamedFunction>(false) ?: return null
        val owner = PsiTreeUtil.getParentOfType(function, javaClass<JetFunction>(), javaClass<JetClass>())

        if (owner is JetClass) {
            val delegate = LightClassUtil.getPsiClass(owner) ?: return null
            val method = delegate.getMethods().firstOrNull() { it.getNavigationElement() == function } ?: return null
            val methodLocation = PsiLocation.fromPsiElement(method)
            if (JUnitUtil.isTestMethod(methodLocation, false)) {
                return methodLocation
            }
        }
        return null
    }

    private fun getTestClass(leaf: PsiElement): PsiClass? {
        val containingFile = leaf.getContainingFile() as? JetFile ?: return null
        var jetClass = leaf.getParentOfType<JetClass>(false)
        if (!jetClass.isJUnitTestClass()) {
            jetClass = getTestClassInFile(containingFile)
        }
        if (jetClass != null) {
            return LightClassUtil.getPsiClass(jetClass)
        }
        return null
    }

    private fun JetClass?.isJUnitTestClass() =
            LightClassUtil.getPsiClass(this)?.let { JUnitUtil.isTestClass(it) } ?: false

    private fun getTestClassInFile(jetFile: JetFile) =
            jetFile.getDeclarations().filterIsInstance<JetClass>().singleOrNull { it.isJUnitTestClass() }
}
