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

import com.intellij.execution.JavaRunConfigurationExtensionManager
import com.intellij.execution.Location
import com.intellij.execution.PsiLocation
import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.junit.JUnitConfiguration
import com.intellij.execution.junit.JUnitConfigurationType
import com.intellij.execution.junit.JUnitUtil
import com.intellij.execution.junit.RuntimeConfigurationProducer
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.DumbService
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.asJava.LightClassUtil
import org.jetbrains.kotlin.idea.project.ProjectStructureUtil
import org.jetbrains.kotlin.idea.util.ProjectRootsUtil
import org.jetbrains.kotlin.psi.*

public class KotlinJUnitRunConfigurationProducer : RuntimeConfigurationProducer(JUnitConfigurationType.getInstance()) {
    private var myElement: JetElement? = null

    override fun getSourceElement(): PsiElement? {
        return myElement
    }

    override fun createConfigurationByElement(location: Location<*>, context: ConfigurationContext): RunnerAndConfigurationSettings? {
        if (DumbService.getInstance(location.getProject()).isDumb()) return null

        val leaf = location.getPsiElement()

        if (!ProjectRootsUtil.isInProjectOrLibSource(leaf)) {
            return null
        }

        if (leaf.getContainingFile() !is JetFile) {
            return null
        }

        val jetFile = leaf.getContainingFile() as JetFile

        if (ProjectStructureUtil.isJsKotlinModule(jetFile)) {
            return null
        }

        val function = PsiTreeUtil.getParentOfType(leaf, javaClass<JetNamedFunction>(), false)
        if (function != null) {
            myElement = function

            @SuppressWarnings("unchecked")
            val owner = PsiTreeUtil.getParentOfType(function, javaClass<JetFunction>(), javaClass<JetClass>())

            if (owner is JetClass) {
                val delegate = LightClassUtil.getPsiClass(owner)
                if (delegate != null) {
                    for (method in delegate.getMethods()) {
                        if (method.getNavigationElement() === function) {
                            val methodLocation = PsiLocation.fromPsiElement(method)
                            if (JUnitUtil.isTestMethod(methodLocation, false)) {
                                val settings = cloneTemplateConfiguration(context.getProject(), context)
                                val configuration = settings.getConfiguration() as JUnitConfiguration

                                val originalModule = configuration.getConfigurationModule().getModule()
                                configuration.beMethodConfiguration(methodLocation)
                                configuration.restoreOriginalModule(originalModule)
                                JavaRunConfigurationExtensionManager.getInstance().extendCreatedConfiguration(configuration, location)

                                return settings
                            }
                            break
                        }
                    }
                }
            }
        }

        var jetClass = PsiTreeUtil.getParentOfType(leaf, javaClass<JetClass>(), false)

        if (jetClass == null) {
            jetClass = getClassDeclarationInFile(jetFile)
        }

        if (jetClass != null) {
            myElement = jetClass
            val delegate = LightClassUtil.getPsiClass(jetClass)

            if (delegate != null && JUnitUtil.isTestClass(delegate)) {
                val settings = cloneTemplateConfiguration(context.getProject(), context)
                val configuration = settings.getConfiguration() as JUnitConfiguration

                val originalModule = configuration.getConfigurationModule().getModule()
                configuration.beClassConfiguration(delegate)
                configuration.restoreOriginalModule(originalModule)
                JavaRunConfigurationExtensionManager.getInstance().extendCreatedConfiguration(configuration, location)

                return settings
            }
        }

        return null
    }

    override fun compareTo(o: Any?): Int {
        return 0
    }

    companion object {

        fun getClassDeclarationInFile(jetFile: JetFile): JetClass? {
            var tempSingleDeclaration: JetClass? = null

            for (jetDeclaration in jetFile.getDeclarations()) {
                if (jetDeclaration is JetClass) {

                    if (tempSingleDeclaration == null) {
                        tempSingleDeclaration = jetDeclaration
                    }
                    else {
                        // There are several class declarations in file
                        return null
                    }
                }
            }

            return tempSingleDeclaration
        }
    }
}
