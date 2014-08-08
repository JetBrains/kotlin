/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

import com.intellij.find.findUsages.FindUsagesHandler
import com.intellij.psi.PsiElement
import com.intellij.usageView.UsageInfo
import com.intellij.util.Processor
import com.intellij.find.findUsages.FindUsagesOptions
import org.jetbrains.jet.plugin.findUsages.KotlinFindUsagesHandlerFactory
import kotlin.properties.Delegates
import org.jetbrains.jet.lang.psi.JetNamedDeclaration
import com.intellij.psi.PsiMethod
import com.intellij.find.findUsages.JavaFindUsagesHandler
import org.jetbrains.jet.plugin.findUsages.KotlinFunctionFindUsagesOptions
import org.jetbrains.jet.plugin.findUsages.toJavaMethodOptions
import com.intellij.openapi.actionSystem.DataContext
import org.jetbrains.jet.lang.resolve.android.isAndroidSyntheticElement
import com.intellij.openapi.components.ServiceManager
import org.jetbrains.jet.lang.resolve.android.AndroidUIXmlProcessor
import com.intellij.psi.xml.XmlAttribute
import org.jetbrains.jet.lang.psi.JetProperty
import org.jetbrains.android.util.AndroidResourceUtil
import java.util.ArrayList
import com.intellij.find.findUsages.JavaVariableFindUsagesOptions

class DelegatingFindMemberUsagesHandler(
        val declaration: JetNamedDeclaration,
        val elementsToSearch: Collection<PsiElement>,
        val factory: KotlinFindUsagesHandlerFactory
) : FindUsagesHandler(declaration) {
    private val kotlinHandler = KotlinFindMemberUsagesHandler.getInstance(declaration, elementsToSearch, factory)

    private val javaHandler: JavaFindUsagesHandler by Delegates.lazy {
        JavaFindUsagesHandler(declaration, elementsToSearch.copyToArray(), factory.javaHandlerFactory)
    }

    fun getHandler(element: PsiElement): FindUsagesHandler? =
            when (element) {
                is JetNamedDeclaration ->
                    KotlinFindMemberUsagesHandler.getInstance(element, elementsToSearch, factory)

                is PsiMethod ->
                    JavaFindUsagesHandler(element, elementsToSearch.copyToArray(), factory.javaHandlerFactory)

                else -> null
            }

    override fun getPrimaryElements(): Array<PsiElement> {
        val primaryElements = kotlinHandler.getPrimaryElements()
        if (isAndroidSyntheticElement(declaration)) {
            val name = (declaration as JetProperty).getName()!!
            val parser = ServiceManager.getService(declaration.getProject(), javaClass<AndroidUIXmlProcessor>())
            val psiElement = parser?.resourceManager?.idToXmlAttribute(name) as? XmlAttribute
            if (psiElement != null && psiElement.getValueElement() != null) {
                return array(psiElement.getValueElement()!!)
            }
        }
        return primaryElements
    }

    override fun getFindUsagesOptions(dataContext: DataContext?): FindUsagesOptions {
        return kotlinHandler.getFindUsagesOptions(dataContext)
    }

    override fun getSecondaryElements(): Array<PsiElement> {
        if (isAndroidSyntheticElement(declaration)) {
            val name = (declaration as JetProperty).getName()!!
            val parser = ServiceManager.getService(declaration.getProject(), javaClass<AndroidUIXmlProcessor>())
            val psiElement = parser?.resourceManager?.idToXmlAttribute(name) as? XmlAttribute
            if (psiElement != null) {
                val res = ArrayList<PsiElement>()
                val fields = AndroidResourceUtil.findIdFields(psiElement)
                for (field in fields) {
                    res.add(field)
                }
                return res.copyToArray()
            }
        }
        return array()
    }

    override fun processElementUsages(element: PsiElement, processor: Processor<UsageInfo>, options: FindUsagesOptions): Boolean {
        val handler = getHandler(element)
        if (handler == null || isAndroidSyntheticElement(element)) {
            val findUsagesOptions = JavaVariableFindUsagesOptions(element.getProject())
            findUsagesOptions.isSearchForTextOccurrences = false
            findUsagesOptions.isSkipImportStatements = true
            findUsagesOptions.isUsages = true
            findUsagesOptions.isReadAccess = true
            findUsagesOptions.isWriteAccess = true
            return super.processElementUsages(element, processor, findUsagesOptions)
        }

        val handlerOptions = when (handler) {
            /* Can't have KotlinPropertyFindUsagesOptions here since Kotlin properties do not override java methods, so
             * elementsToSearch contains property declarations only */
            is JavaFindUsagesHandler -> (options as KotlinFunctionFindUsagesOptions).toJavaMethodOptions(element.getProject())
            else -> options
        }
        return handler.processElementUsages(element, processor, handlerOptions)
    }
}
