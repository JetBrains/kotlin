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

package org.jetbrains.kotlin.idea.highlighter.markers

import com.intellij.psi.search.PsiElementProcessor
import com.intellij.psi.PsiClass
import org.jetbrains.kotlin.idea.JetBundle
import com.intellij.psi.search.searches.OverridingMethodsSearch
import org.jetbrains.kotlin.asJava.LightClassUtil
import com.intellij.psi.PsiMethod
import com.intellij.util.AdapterProcessor
import com.intellij.util.CommonProcessors
import com.intellij.psi.search.PsiElementProcessorAdapter
import com.intellij.codeInsight.daemon.impl.GutterIconTooltipHelper
import com.intellij.psi.PsiElement
import java.awt.event.MouseEvent
import com.intellij.openapi.project.DumbService
import org.jetbrains.kotlin.idea.search.ideaExtensions.KotlinDefinitionsSearcher
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.openapi.progress.ProgressManager
import com.intellij.codeInsight.daemon.impl.MarkerType
import com.intellij.ide.util.DefaultPsiElementCellRenderer
import com.intellij.ide.util.PsiClassListCellRenderer
import javax.swing.JComponent
import com.intellij.codeInsight.daemon.impl.PsiElementListNavigator
import com.intellij.psi.NavigatablePsiElement
import com.intellij.util.Function
import org.jetbrains.kotlin.lexer.JetTokens
import org.jetbrains.kotlin.psi.*

fun getOverriddenPropertyTooltip(property: JetProperty): String? {
    val overriddenInClassesProcessor = PsiElementProcessor.CollectElementsWithLimit<PsiClass>(5)

    val consumer = AdapterProcessor<PsiMethod, PsiClass>(
            CommonProcessors.UniqueProcessor<PsiClass>(PsiElementProcessorAdapter(overriddenInClassesProcessor)),
            Function { method: PsiMethod? -> method?.getContainingClass() }
    )

    for (method in LightClassUtil.getLightClassPropertyMethods(property)) {
        if (!overriddenInClassesProcessor.isOverflow()) {
            OverridingMethodsSearch.search(method, true).forEach(consumer)
        }
    }

    val isImplemented = isImplemented(property)
    if (overriddenInClassesProcessor.isOverflow()) {
        return if (isImplemented)
            JetBundle.message("property.is.implemented.too.many")
        else
            JetBundle.message("property.is.overridden.too.many")
    }

    val collectedClasses = overriddenInClassesProcessor.getCollection()
    if (collectedClasses.isEmpty()) return null

    val start = if (isImplemented)
        JetBundle.message("property.is.implemented.header")
    else
        JetBundle.message("property.is.overridden.header")

    val pattern = "&nbsp;&nbsp;&nbsp;&nbsp;{0}"
    return GutterIconTooltipHelper.composeText(collectedClasses.sortBy(PsiClassListCellRenderer().getComparator()), start, pattern)
}

fun navigateToPropertyOverriddenDeclarations(e: MouseEvent?, property: JetProperty) {
    val project = property.getProject()

    if (DumbService.isDumb(project)) {
        DumbService.getInstance(project)?.showDumbModeNotification("Navigation to overriding classes is not possible during index update")
        return
    }

    val psiPropertyMethods = LightClassUtil.getLightClassPropertyMethods(property)

    val elementProcessor = CommonProcessors.CollectUniquesProcessor<PsiElement>()
    val jetPsiMethodProcessor = Runnable {
        KotlinDefinitionsSearcher.processPropertyImplementationsMethods(
                psiPropertyMethods,
                GlobalSearchScope.allScope(project),
                elementProcessor)
    }

    if (!ProgressManager.getInstance().runProcessWithProgressSynchronously(
            /* runnable */ jetPsiMethodProcessor,
            MarkerType.SEARCHING_FOR_OVERRIDING_METHODS,
            /* can be canceled */ true,
            project,
            e?.getComponent() as JComponent?)) {
        return
    }

    val renderer = DefaultPsiElementCellRenderer()
    val navigatingOverrides = elementProcessor.getResults()
            .sortBy(renderer.getComparator())
            .filterIsInstance<NavigatablePsiElement>()

    PsiElementListNavigator.openTargets(e,
                                        navigatingOverrides.toTypedArray(),
                                        JetBundle.message("navigation.title.overriding.property", property.getName()),
                                        JetBundle.message("navigation.findUsages.title.overriding.property", property.getName()), renderer)
}


public fun isImplemented(declaration: JetNamedDeclaration): Boolean {
    if (declaration.hasModifier(JetTokens.ABSTRACT_KEYWORD)) return true

    var parent = declaration.getParent()
    parent = if (parent is JetClassBody) parent.getParent() else parent

    if (parent !is JetClass) return false

    return (parent as JetClass).isInterface() && (declaration !is JetDeclarationWithBody || !declaration.hasBody()) && (declaration !is JetWithExpressionInitializer || !declaration.hasInitializer())
}

