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

import com.intellij.psi.PsiMethod
import com.intellij.psi.search.PsiElementProcessor
import com.intellij.psi.search.searches.OverridingMethodsSearch
import com.intellij.psi.PsiModifier
import com.intellij.codeInsight.daemon.DaemonBundle
import com.intellij.ide.util.MethodCellRenderer
import com.intellij.codeInsight.daemon.impl.GutterIconTooltipHelper
import java.awt.event.MouseEvent
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.progress.ProgressManager
import javax.swing.JComponent
import com.intellij.psi.util.PsiUtil
import com.intellij.codeInsight.daemon.impl.PsiElementListNavigator
import com.intellij.codeInsight.navigation.ListBackgroundUpdaterTask
import com.intellij.ide.util.PsiElementListCellRenderer
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.util.CommonProcessors
import com.intellij.psi.search.PsiElementProcessorAdapter
import com.intellij.psi.PsiElement
import gnu.trove.THashSet
import com.intellij.psi.PsiClass
import com.intellij.psi.search.searches.AllOverridingMethodsSearch
import com.intellij.openapi.util.Pair
import java.util.HashSet
import com.intellij.util.Processor
import org.jetbrains.kotlin.asJava.KotlinLightMethodForTraitFakeOverride

private fun <T> getOverriddenDeclarations(mappingToJava: MutableMap<PsiMethod, T>, classes: Set<PsiClass>): Set<T> {
    val overridden = HashSet<T>()
    for (aClass in classes) {
        AllOverridingMethodsSearch.search(aClass)!!.forEach(object : Processor<Pair<PsiMethod, PsiMethod>> {
            override fun process(pair: Pair<PsiMethod, PsiMethod>?): Boolean {
                ProgressManager.checkCanceled()

                if (pair!!.getSecond() !is KotlinLightMethodForTraitFakeOverride) {
                    val superMethod = pair.getFirst()

                    val declaration = mappingToJava.get(superMethod)
                    if (declaration != null) {
                        mappingToJava.remove(superMethod)
                        overridden.add(declaration)
                    }
                }

                return !mappingToJava.isEmpty()
            }
        })
    }

    return overridden
}

public fun getOverriddenMethodTooltip(method: PsiMethod): String? {
    val processor = PsiElementProcessor.CollectElementsWithLimit<PsiMethod>(5)
    OverridingMethodsSearch.search(method, true).forEach(PsiElementProcessorAdapter(processor))

    val isAbstract = method.hasModifierProperty(PsiModifier.ABSTRACT)

    if (processor.isOverflow()) {
        return if (isAbstract) DaemonBundle.message("method.is.implemented.too.many") else DaemonBundle.message("method.is.overridden.too.many")
    }

    val comparator = MethodCellRenderer(false).getComparator()

    val overridingJavaMethods = processor.getCollection().filter { it !is KotlinLightMethodForTraitFakeOverride } sortBy(comparator)
    if (overridingJavaMethods.isEmpty()) return null

    val start = if (isAbstract) DaemonBundle.message("method.is.implemented.header") else DaemonBundle.message("method.is.overriden.header")

    return GutterIconTooltipHelper.composeText(overridingJavaMethods, start, "&nbsp;&nbsp;&nbsp;&nbsp;{1}")
}

public fun navigateToOverriddenMethod(e: MouseEvent?, method: PsiMethod) {
    if (DumbService.isDumb(method.getProject())) {
        DumbService.getInstance(method.getProject())?.showDumbModeNotification("Navigation to overriding classes is not possible during index update")
        return
    }

    val processor = PsiElementProcessor.CollectElementsWithLimit<PsiMethod>(2, THashSet<PsiMethod>())
    if (!ProgressManager.getInstance().runProcessWithProgressSynchronously(
            {
                OverridingMethodsSearch.search(method, true).forEach(PsiElementProcessorAdapter(processor))
            },
            "Searching for overriding declarations", true, method.getProject(), e?.getComponent() as JComponent?)) {
        return
    }

    var overridingJavaMethods = processor.getCollection().filter { it !is KotlinLightMethodForTraitFakeOverride }
    if (overridingJavaMethods.isEmpty()) return

    val showMethodNames = !PsiUtil.allMethodsHaveSameSignature(overridingJavaMethods.toTypedArray())

    val renderer = MethodCellRenderer(showMethodNames)
    overridingJavaMethods = overridingJavaMethods.sortBy(renderer.getComparator())

    val methodsUpdater = OverridingMethodsUpdater(method, renderer)
    PsiElementListNavigator.openTargets(
            e,
            overridingJavaMethods.toTypedArray(),
            methodsUpdater.getCaption(overridingJavaMethods.size()),
            "Overriding declarations of " + method.getName(),
            renderer,
            methodsUpdater)
}

private class OverridingMethodsUpdater(
        private val myMethod: PsiMethod,
        private val myRenderer: PsiElementListCellRenderer<out PsiElement>) :
        ListBackgroundUpdaterTask(myMethod.getProject(), "Searching for overriding methods") {
    override fun getCaption(size: Int): String {
        return if (myMethod.hasModifierProperty(PsiModifier.ABSTRACT))
            DaemonBundle.message("navigation.title.implementation.method", myMethod.getName(), size)!!
        else
            DaemonBundle.message("navigation.title.overrider.method", myMethod.getName(), size)!!
    }

    override fun run(indicator: ProgressIndicator) {
        super.run(indicator)
        OverridingMethodsSearch.search(myMethod, true).forEach(object : CommonProcessors.CollectProcessor<PsiMethod>() {
            override fun process(psiMethod: PsiMethod?): Boolean {
                if (!updateComponent(psiMethod, myRenderer.getComparator())) {
                    indicator.cancel()
                }
                indicator.checkCanceled()
                return super.process(psiMethod)
            }
        })
    }
}
