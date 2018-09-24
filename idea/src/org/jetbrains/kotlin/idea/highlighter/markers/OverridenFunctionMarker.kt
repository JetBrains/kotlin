/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

import com.intellij.codeInsight.daemon.DaemonBundle
import com.intellij.codeInsight.daemon.impl.GutterIconTooltipHelper
import com.intellij.codeInsight.navigation.ListBackgroundUpdaterTask
import com.intellij.ide.util.MethodCellRenderer
import com.intellij.ide.util.PsiElementListCellRenderer
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.IdeActions
import com.intellij.openapi.keymap.KeymapUtil
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.DumbService
import com.intellij.psi.*
import com.intellij.psi.presentation.java.ClassPresentationUtil
import com.intellij.psi.search.PsiElementProcessor
import com.intellij.psi.search.PsiElementProcessorAdapter
import com.intellij.psi.search.searches.ClassInheritorsSearch
import com.intellij.psi.search.searches.FunctionalExpressionSearch
import com.intellij.util.CommonProcessors
import gnu.trove.THashSet
import org.jetbrains.kotlin.asJava.classes.KtLightClass
import org.jetbrains.kotlin.asJava.elements.KtLightMethod
import org.jetbrains.kotlin.asJava.elements.isTraitFakeOverride
import org.jetbrains.kotlin.idea.presentation.DeclarationByModuleRenderer
import org.jetbrains.kotlin.idea.search.declarationsSearch.forEachDeclaredMemberOverride
import org.jetbrains.kotlin.idea.search.declarationsSearch.forEachOverridingMethod
import org.jetbrains.kotlin.idea.search.declarationsSearch.toPossiblyFakeLightMethods
import org.jetbrains.kotlin.idea.util.application.runReadAction
import org.jetbrains.kotlin.idea.util.isExpectDeclaration
import org.jetbrains.kotlin.idea.util.projectStructure.module
import org.jetbrains.kotlin.psi.psiUtil.hasActualModifier
import java.awt.event.MouseEvent
import java.util.*
import javax.swing.JComponent

private fun PsiMethod.isMethodWithDeclarationInOtherClass(): Boolean {
    return this is KtLightMethod && this.isTraitFakeOverride()
}

internal fun <T> getOverriddenDeclarations(mappingToJava: MutableMap<PsiElement, T>, classes: Set<PsiClass>): Set<T> {
    val overridden = HashSet<T>()
    for (aClass in classes) {
        aClass.forEachDeclaredMemberOverride { superMember, overridingMember ->
            ProgressManager.checkCanceled()
            if (overridingMember.toPossiblyFakeLightMethods().any { !it.isMethodWithDeclarationInOtherClass() }) {
                val declaration = mappingToJava[superMember]
                if (declaration != null) {
                    mappingToJava.remove(superMember)
                    overridden.add(declaration)
                }
            }

            !mappingToJava.isEmpty()
        }
    }

    return overridden
}

// Module-specific version of MarkerType.getSubclassedClassTooltip
fun getSubclassedClassTooltip(klass: PsiClass): String? {
    val processor = PsiElementProcessor.CollectElementsWithLimit(5, THashSet<PsiClass>())
    ClassInheritorsSearch.search(klass).forEach(PsiElementProcessorAdapter(processor))

    if (processor.isOverflow) {
        return DaemonBundle.message(if (klass.isInterface) "interface.is.implemented.too.many" else "class.is.subclassed.too.many")
    }

    val subclasses = processor.toArray(PsiClass.EMPTY_ARRAY)
    if (subclasses.isEmpty()) {
        val functionalImplementations = PsiElementProcessor.CollectElementsWithLimit(2, THashSet<PsiFunctionalExpression>())
        FunctionalExpressionSearch.search(klass).forEach(PsiElementProcessorAdapter(functionalImplementations))
        return if (functionalImplementations.collection.isNotEmpty()) "Has functional implementations" else null
    }

    val start = DaemonBundle.message(if (klass.isInterface) "interface.is.implemented.by.header" else "class.is.subclassed.by.header")
    val shortcuts = ActionManager.getInstance().getAction(IdeActions.ACTION_GOTO_IMPLEMENTATION).shortcutSet.shortcuts
    val shortcut = shortcuts.firstOrNull()
    var postfix = "<br><div style='margin-top: 5px'><font size='2'>Click"
    if (shortcut != null) postfix += " or press " + KeymapUtil.getShortcutText(shortcut)
    postfix += " to navigate</font></div>"

    val renderer = DeclarationByModuleRenderer()
    val comparator = renderer.comparator
    return subclasses.toList().sortedWith(comparator).joinToString(
        prefix = "<html><body>$start", postfix = "$postfix</body</html>", separator = "<br>"
    ) {
        val moduleNameRequired = if (it is KtLightClass) {
            val origin = it.kotlinOrigin
            origin?.hasActualModifier() == true || origin?.isExpectDeclaration() == true
        } else false
        val moduleName = it.module?.name
        val elementText = renderer.getElementText(it) + (moduleName?.takeIf { moduleNameRequired }?.let { " [$it]" } ?: "")
        val refText = (moduleName?.let { "$it:" } ?: "") + ClassPresentationUtil.getNameForClass(it, /* qualified = */ true)
        "&nbsp;&nbsp;&nbsp;&nbsp;<a href=\"#kotlinClass/$refText\">$elementText</a>"
    }
}

fun getOverriddenMethodTooltip(method: PsiMethod): String? {
    val processor = PsiElementProcessor.CollectElementsWithLimit<PsiMethod>(5)
    method.forEachOverridingMethod(processor = PsiElementProcessorAdapter(processor)::process)

    val isAbstract = method.hasModifierProperty(PsiModifier.ABSTRACT)

    if (processor.isOverflow) {
        return if (isAbstract) DaemonBundle.message("method.is.implemented.too.many") else DaemonBundle.message("method.is.overridden.too.many")
    }

    val comparator = MethodCellRenderer(false).comparator

    val overridingJavaMethods = processor.collection.filter { !it.isMethodWithDeclarationInOtherClass() }.sortedWith(comparator)
    if (overridingJavaMethods.isEmpty()) return null

    val start = if (isAbstract) DaemonBundle.message("method.is.implemented.header") else DaemonBundle.message("method.is.overriden.header")

    return GutterIconTooltipHelper.composeText(overridingJavaMethods, start, "&nbsp;&nbsp;&nbsp;&nbsp;{1}")
}

fun buildNavigateToOverriddenMethodPopup(e: MouseEvent?, element: PsiElement?): NavigationPopupDescriptor? {
    val method = getPsiMethod(element) ?: return null

    if (DumbService.isDumb(method.project)) {
        DumbService.getInstance(method.project)
            ?.showDumbModeNotification("Navigation to overriding classes is not possible during index update")
        return null
    }

    val processor = PsiElementProcessor.CollectElementsWithLimit<PsiMethod>(2, THashSet<PsiMethod>())
    if (!ProgressManager.getInstance().runProcessWithProgressSynchronously(
            {
                method.forEachOverridingMethod {
                    runReadAction {
                        processor.execute(it)
                    }
                }
            },
            "Searching for overriding declarations", true, method.project, e?.component as JComponent?
        )
    ) {
        return null
    }

    var overridingJavaMethods = processor.collection.filter { !it.isMethodWithDeclarationInOtherClass() }
    if (overridingJavaMethods.isEmpty()) return null

    val renderer = MethodCellRenderer(false)
    overridingJavaMethods = overridingJavaMethods.sortedWith(renderer.comparator)

    val methodsUpdater = OverridingMethodsUpdater(method, renderer)
    return NavigationPopupDescriptor(
        overridingJavaMethods,
        methodsUpdater.getCaption(overridingJavaMethods.size),
        "Overriding declarations of " + method.name,
        renderer,
        methodsUpdater
    )
}

private class OverridingMethodsUpdater(
    private val myMethod: PsiMethod,
    private val myRenderer: PsiElementListCellRenderer<out PsiElement>
) :
    ListBackgroundUpdaterTask(myMethod.project, "Searching for overriding methods") {
    override fun getCaption(size: Int): String {
        return if (myMethod.hasModifierProperty(PsiModifier.ABSTRACT))
            DaemonBundle.message("navigation.title.implementation.method", myMethod.name, size)!!
        else
            DaemonBundle.message("navigation.title.overrider.method", myMethod.name, size)!!
    }

    override fun run(indicator: ProgressIndicator) {
        super.run(indicator)
        val processor = object : CommonProcessors.CollectProcessor<PsiMethod>() {
            override fun process(psiMethod: PsiMethod): Boolean {
                if (!updateComponent(psiMethod, myRenderer.comparator)) {
                    indicator.cancel()
                }
                indicator.checkCanceled()
                return super.process(psiMethod)
            }
        }
        myMethod.forEachOverridingMethod { processor.process(it) }
    }
}
