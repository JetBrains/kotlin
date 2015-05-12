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

package org.jetbrains.kotlin.idea.findUsages.handlers

import com.intellij.find.findUsages.AbstractFindUsagesDialog
import com.intellij.find.findUsages.FindUsagesOptions
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.psi.search.PsiElementProcessor
import com.intellij.psi.search.PsiElementProcessorAdapter
import com.intellij.usageView.UsageInfo
import com.intellij.util.Processor
import org.jetbrains.kotlin.asJava.LightClassUtil
import org.jetbrains.kotlin.psi.JetClassOrObject
import org.jetbrains.kotlin.idea.findUsages.KotlinClassFindUsagesOptions
import org.jetbrains.kotlin.idea.findUsages.KotlinFindUsagesHandlerFactory
import org.jetbrains.kotlin.idea.findUsages.dialogs.KotlinFindClassUsagesDialog
import org.jetbrains.kotlin.idea.search.declarationsSearch.HierarchySearchRequest
import org.jetbrains.kotlin.idea.search.declarationsSearch.searchInheritors
import org.jetbrains.kotlin.idea.findUsages.toSearchTarget
import org.jetbrains.kotlin.idea.findUsages.toClassHelper
import org.jetbrains.kotlin.idea.findUsages.toClassDeclarationsHelper
import org.jetbrains.kotlin.idea.search.usagesSearch.search
import org.jetbrains.kotlin.idea.util.application.runReadAction
import java.util.Collections
import com.intellij.find.findUsages.JavaFindUsagesHandler
import com.intellij.find.findUsages.JavaFindUsagesHandlerFactory
import com.intellij.psi.*
import com.intellij.psi.search.searches.MethodReferencesSearch
import com.intellij.util.CommonProcessors
import org.jetbrains.kotlin.asJava.KotlinLightMethod
import org.jetbrains.kotlin.asJava.toLightClass
import org.jetbrains.kotlin.idea.search.usagesSearch.processDelegationCallConstructorUsages
import org.jetbrains.kotlin.psi.JetClass
import org.jetbrains.kotlin.psi.psiUtil.isInheritable

public class KotlinFindClassUsagesHandler(
        jetClass: JetClassOrObject,
        factory: KotlinFindUsagesHandlerFactory
) : KotlinFindUsagesHandler<JetClassOrObject>(jetClass, factory) {
    public override fun getFindUsagesDialog(
            isSingleFile: Boolean, toShowInNewTab: Boolean, mustOpenInNewTab: Boolean
    ): AbstractFindUsagesDialog {
        return KotlinFindClassUsagesDialog(getElement(),
                                           getProject(),
                                           getFactory().findClassOptions,
                                           toShowInNewTab,
                                           mustOpenInNewTab,
                                           isSingleFile,
                                           this)
    }

    protected override fun searchReferences(element: PsiElement, processor: Processor<UsageInfo>, options: FindUsagesOptions): Boolean {
        val kotlinOptions = options as KotlinClassFindUsagesOptions

        fun processInheritors(): Boolean {
            val request = HierarchySearchRequest(element, options.searchScope, options.isCheckDeepInheritance)
            return request.searchInheritors().forEach(
                    PsiElementProcessorAdapter(
                            object : PsiElementProcessor<PsiClass> {
                                public override fun execute(element: PsiClass): Boolean {
                                    val traitOrInterface = element.isInterface()
                                    return when {
                                        traitOrInterface && options.isDerivedInterfaces, !traitOrInterface && options.isDerivedClasses ->
                                            KotlinFindUsagesHandler.processUsage(processor, element.getNavigationElement())
                                        else -> true
                                    }
                                }
                            }
                    )
            )
        }

        val classOrObject = element as JetClassOrObject

        val uniqueProcessor = CommonProcessors.UniqueProcessor(processor)

        return runReadAction {
            val target = kotlinOptions.toSearchTarget(classOrObject, true)
            val classUsages = kotlinOptions.toClassHelper().newRequest(target).search()
            val declarationUsages = kotlinOptions.toClassDeclarationsHelper().newRequest(target).search()

            if (kotlinOptions.searchConstructorUsages) {
                val constructors = classOrObject.toLightClass()?.getConstructors() ?: PsiMethod.EMPTY_ARRAY
                for (constructor in constructors) {
                    if (constructor !is KotlinLightMethod) continue
                    constructor.processDelegationCallConstructorUsages(constructor.getUseScope()) {
                        it.getCalleeExpression()?.getReference()?.let { KotlinFindUsagesHandler.processUsage(uniqueProcessor, it) }
                    }
                }
            }

            (classUsages + declarationUsages).all { KotlinFindUsagesHandler.processUsage(uniqueProcessor, it)} && processInheritors()
        }
    }

    protected override fun getStringsToSearch(element: PsiElement): Collection<String> {
        val psiClass = when (element) {
                           is PsiClass -> element
                           is JetClassOrObject -> LightClassUtil.getPsiClass(getElement())
                           else -> null
                       } ?: return Collections.emptyList()

        // Work around the protected method in JavaFindUsagesHandler
        // todo: Use JavaFindUsagesHelper.getElementNames() when it becomes public in IDEA
        var stringsToSearch: Collection<String>
        object: JavaFindUsagesHandler(psiClass, JavaFindUsagesHandlerFactory.getInstance(element.getProject())) {
            init {
                stringsToSearch = getStringsToSearch(psiClass)
            }
        }
        return stringsToSearch
    }

    protected override fun isSearchForTextOccurencesAvailable(psiElement: PsiElement, isSingleFile: Boolean): Boolean {
        return !isSingleFile
    }

    public override fun getFindUsagesOptions(dataContext: DataContext?): FindUsagesOptions {
        return getFactory().findClassOptions
    }
}
