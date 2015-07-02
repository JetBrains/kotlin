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

import com.intellij.execution.Location
import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.junit.RuntimeConfigurationProducer
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.util.Comparing
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.ElementBase
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.NotNullFunction
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.idea.MainFunctionDetector
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.idea.project.ProjectStructureUtil
import org.jetbrains.kotlin.idea.util.ProjectRootsUtil
import org.jetbrains.kotlin.load.kotlin.PackageClassUtils
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.*

public class KotlinRunConfigurationProducer : RuntimeConfigurationProducer(JetRunConfigurationType.getInstance()), Cloneable {
    override fun clone(): KotlinRunConfigurationProducer {
        return super<RuntimeConfigurationProducer>.clone() as KotlinRunConfigurationProducer
    }

    private var mySourceElement: PsiElement? = null

    override fun getSourceElement(): PsiElement? {
        return mySourceElement
    }

    override fun createConfigurationByElement(location: Location<*>, configurationContext: ConfigurationContext): RunnerAndConfigurationSettings? {
        val container = getEntryPointContainer(location) ?: return null

        mySourceElement = container as PsiElement?

        val startClassFQName = getStartClassFqName(container)
        if (startClassFQName == null) return null

        val module = location.getModule()
        assert(module != null)

        return createConfigurationByQName(module, configurationContext, startClassFQName)
    }

    private fun getStartClassFqName(container: JetDeclarationContainer?): FqName? {
        if (container == null) return null
        if (container is JetFile) return PackageClassUtils.getPackageClassFqName((container as JetFile?).getPackageFqName())
        if (container is JetClassOrObject) {
            if (container is JetObjectDeclaration && container.isCompanion()) {
                val containerClass = PsiTreeUtil.getParentOfType<JetClass>(container, javaClass<JetClass>())
                return containerClass?.getFqName()
            }
            return container?.getFqName()
        }
        throw IllegalArgumentException("Invalid entry-point container: " + (container as PsiElement?).getText())
    }

    private fun getEntryPointContainer(location: Location<*>): JetDeclarationContainer? {
        if (DumbService.getInstance(location.getProject()).isDumb()) return null

        val module = location.getModule() ?: return null

        if (ProjectStructureUtil.isJsKotlinModule(module)) return null

        val locationElement = location.getPsiElement()

        val psiFile = locationElement.getContainingFile()
        if (!(psiFile is JetFile && ProjectRootsUtil.isInProjectOrLibSource(psiFile))) return null

        val resolutionFacade = psiFile.getResolutionFacade()
        val mainFunctionDetector = MainFunctionDetector(object : NotNullFunction<JetNamedFunction, FunctionDescriptor> {
            override fun `fun`(function: JetNamedFunction): FunctionDescriptor {
                return resolutionFacade.resolveToDescriptor(function) as FunctionDescriptor
            }
        })

        var currentElement = PsiTreeUtil.getNonStrictParentOfType<PsiElement>(locationElement, javaClass<JetClassOrObject>(), javaClass<JetFile>()) as JetDeclarationContainer
        while (currentElement != null) {
            var entryPointContainer = currentElement
            if (entryPointContainer is JetClass) {
                entryPointContainer = entryPointContainer.getCompanionObjects().singleOrNull() as JetDeclarationContainer
            }
            if (entryPointContainer != null && mainFunctionDetector.hasMain(entryPointContainer.getDeclarations())) return entryPointContainer
            currentElement = PsiTreeUtil.getParentOfType<PsiElement>(currentElement as PsiElement?, javaClass<JetClassOrObject>(), javaClass<JetFile>()) as JetDeclarationContainer
        }

        return null
    }

    private fun createConfigurationByQName(module: Module, context: ConfigurationContext, fqName: FqName): RunnerAndConfigurationSettings {
        val settings = cloneTemplateConfiguration(module.getProject(), context)
        val configuration = settings.getConfiguration() as JetRunConfiguration
        configuration.setModule(module)
        configuration.setName(StringUtil.trimEnd(fqName.asString(), "." + PackageClassUtils.getPackageClassName(fqName)))
        configuration.setRunClass(fqName.asString())
        return settings
    }

    override fun findExistingByElement(location: Location<*>?, existingConfigurations: List<RunnerAndConfigurationSettings>, context: ConfigurationContext?): RunnerAndConfigurationSettings? {
        val startClassFQName = getStartClassFqName(getEntryPointContainer(location)) ?: return null

        for (existingConfiguration in existingConfigurations) {
            if (existingConfiguration.getType() is JetRunConfigurationType) {
                val jetConfiguration = existingConfiguration.getConfiguration() as JetRunConfiguration
                if (Comparing.equal(jetConfiguration.getRunClass(), startClassFQName.asString())) {
                    if (Comparing.equal<Module>(location!!.getModule(), jetConfiguration.getConfigurationModule().getModule())) {
                        return existingConfiguration
                    }
                }
            }
        }
        return null
    }

    override fun compareTo(o: Any?): Int {
        return RuntimeConfigurationProducer.PREFERED
    }
}
