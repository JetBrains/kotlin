/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

package org.jetbrains.jet.plugin.findUsages.handlers

import com.intellij.find.findUsages.AbstractFindUsagesDialog
import com.intellij.find.findUsages.FindUsagesOptions
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.util.Computable
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.search.PsiElementProcessor
import com.intellij.psi.search.PsiElementProcessorAdapter
import com.intellij.usageView.UsageInfo
import com.intellij.util.Processor
import org.jetbrains.annotations.Nullable
import org.jetbrains.jet.asJava.LightClassUtil
import org.jetbrains.jet.lang.psi.JetClassOrObject
import org.jetbrains.jet.plugin.findUsages.KotlinClassFindUsagesOptions
import org.jetbrains.jet.plugin.findUsages.KotlinFindUsagesHandlerFactory
import org.jetbrains.jet.plugin.findUsages.dialogs.KotlinFindClassUsagesDialog
import org.jetbrains.jet.plugin.search.declarationsSearch.HierarchySearchRequest
import org.jetbrains.jet.plugin.search.declarationsSearch.KotlinClassInheritorsSearch
import org.jetbrains.jet.plugin.search.usagesSearch.UsagesSearch
import org.jetbrains.jet.plugin.search.usagesSearch.UsagesSearchRequest
import org.jetbrains.jet.plugin.search.usagesSearch.UsagesSearchTarget
import org.jetbrains.jet.plugin.search.declarationsSearch.searchInheritors
import org.jetbrains.jet.plugin.search.declarationsSearch.isTraitOrInterface
import org.jetbrains.jet.plugin.findUsages.toSearchTarget
import org.jetbrains.jet.plugin.findUsages.toClassHelper
import org.jetbrains.jet.plugin.findUsages.toClassDeclarationsHelper
import org.jetbrains.jet.plugin.search.usagesSearch.search

public class KotlinFindClassUsagesHandler(
        jetClass: JetClassOrObject,
        factory: KotlinFindUsagesHandlerFactory
) : KotlinFindUsagesHandler<JetClassOrObject>(jetClass, factory) {
    public override fun getFindUsagesDialog(
            isSingleFile: Boolean, toShowInNewTab: Boolean, mustOpenInNewTab: Boolean
    ): AbstractFindUsagesDialog {
        val lightClass = LightClassUtil.getPsiClass(getElement())
        if (lightClass != null) {
            return KotlinFindClassUsagesDialog(lightClass, getProject(), getFactory().findClassOptions, toShowInNewTab, mustOpenInNewTab, isSingleFile, this)
        }

        return super.getFindUsagesDialog(isSingleFile, toShowInNewTab, mustOpenInNewTab)
    }
    protected override fun searchReferences(element: PsiElement, processor: Processor<UsageInfo>, options: FindUsagesOptions): Boolean {
        val kotlinOptions = options as KotlinClassFindUsagesOptions

        fun processInheritors(): Boolean {
            val request = HierarchySearchRequest(element, options.searchScope!!, options.isCheckDeepInheritance)
            return request.searchInheritors().forEach(
                    PsiElementProcessorAdapter<PsiElement>(
                            object : PsiElementProcessor<PsiElement> {
                                public override fun execute(element: PsiElement): Boolean {
                                    val traitOrInterface = element.isTraitOrInterface()
                                    return when {
                                        traitOrInterface && options.isDerivedInterfaces, !traitOrInterface && options.isDerivedClasses ->
                                            KotlinFindUsagesHandler.processUsage(processor, element)
                                        else -> true
                                    }
                                }
                            }
                    )
            )
        }

        val classOrObject = element as JetClassOrObject

        return ApplicationManager.getApplication()!!.runReadAction<Boolean> {
            val target = kotlinOptions.toSearchTarget(classOrObject, true)
            val classUsages = kotlinOptions.toClassHelper().newRequest(target).search()
            val declarationUsages = kotlinOptions.toClassDeclarationsHelper().newRequest(target).search()

            (classUsages + declarationUsages).all { ref -> KotlinFindUsagesHandler.processUsage(processor, ref)} && processInheritors()
        }!!
    }

    protected override fun isSearchForTextOccurencesAvailable(psiElement: PsiElement, isSingleFile: Boolean): Boolean {
        if (isSingleFile)
            return false

        return psiElement is JetClassOrObject
    }

    public override fun getFindUsagesOptions(dataContext: DataContext?): FindUsagesOptions {
        return getFactory().findClassOptions
    }
}
