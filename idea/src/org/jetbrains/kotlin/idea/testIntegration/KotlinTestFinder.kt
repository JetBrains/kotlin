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

package org.jetbrains.kotlin.idea.testIntegration

import com.intellij.codeInsight.TestFrameworks
import com.intellij.openapi.util.Pair
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.search.PsiShortNamesCache
import com.intellij.testIntegration.JavaTestFinder
import com.intellij.testIntegration.TestFinderHelper
import com.intellij.util.CommonProcessors
import org.jetbrains.kotlin.asJava.classes.KtLightClassForFacade
import org.jetbrains.kotlin.asJava.classes.KtLightClassForSourceDeclaration
import org.jetbrains.kotlin.asJava.findFacadeClass
import org.jetbrains.kotlin.asJava.toLightClass
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptorIfAny
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.psiUtil.parentsWithSelf
import java.util.*
import java.util.regex.Pattern

// Based on com.intellij.testIntegration.JavaTestFinder.JavaTestFinder implementation
// TODO: We can reuse JavaTestFinder if Kotlin classes have their isPhysical() return true
class KotlinTestFinder : JavaTestFinder() {
    override fun findSourceElement(from: PsiElement): PsiClass? {
        super.findSourceElement(from)?.let { return it }

        from.parentsWithSelf.filterIsInstance<KtClassOrObject>().firstOrNull { !it.isLocal }?.let {
            return if (it.resolveToDescriptorIfAny() == null) null else it.toLightClass()
        }

        return (from.containingFile as? KtFile)?.findFacadeClass()
    }

    override fun isTest(element: PsiElement): Boolean {
        val sourceElement = findSourceElement(element) ?: return false
        return super.isTest(sourceElement)
    }

    override fun findClassesForTest(element: PsiElement): Collection<PsiElement> {
        val klass = findSourceElement(element) ?: return emptySet()

        val scope = getSearchScope(element, true)

        val cache = PsiShortNamesCache.getInstance(element.project)

        val frameworks = TestFrameworks.getInstance()
        val classesWithWeights = ArrayList<Pair<out PsiNamedElement, Int>>()
        for (candidateNameWithWeight in TestFinderHelper.collectPossibleClassNamesWithWeights(klass.name)) {
            for (eachClass in cache.getClassesByName(candidateNameWithWeight.first, scope)) {
                if (eachClass.isAnnotationType || frameworks.isTestClass(eachClass)) continue

                if (eachClass is KtLightClassForFacade) {
                    eachClass.files.mapTo(classesWithWeights) { Pair.create(it, candidateNameWithWeight.second) }
                } else if (eachClass.isPhysical || eachClass is KtLightClassForSourceDeclaration) {
                    classesWithWeights.add(Pair.create(eachClass, candidateNameWithWeight.second))
                }
            }
        }

        return TestFinderHelper.getSortedElements(classesWithWeights, false)
    }

    override fun findTestsForClass(element: PsiElement): Collection<PsiElement> {
        val klass = findSourceElement(element) ?: return emptySet()

        val classesWithProximities = ArrayList<Pair<out PsiNamedElement, Int>>()
        val processor = CommonProcessors.CollectProcessor(classesWithProximities)

        val klassName = klass.name!!
        val pattern = Pattern.compile(".*" + StringUtil.escapeToRegexp(klassName) + ".*", Pattern.CASE_INSENSITIVE)

        val scope = getSearchScope(klass, false)
        val frameworks = TestFrameworks.getInstance()

        val cache = PsiShortNamesCache.getInstance(klass.project)
        cache.processAllClassNames { candidateName ->
            if (!pattern.matcher(candidateName).matches()) return@processAllClassNames true
            for (candidateClass in cache.getClassesByName(candidateName, scope)) {
                if (!(frameworks.isTestClass(candidateClass) || frameworks.isPotentialTestClass(candidateClass))) {
                    return@processAllClassNames true
                }
                if (!candidateClass.isPhysical && candidateClass !is KtLightClassForSourceDeclaration) {
                    return@processAllClassNames true
                }

                if (!processor.process(Pair.create(candidateClass, TestFinderHelper.calcTestNameProximity(klassName, candidateName)))) {
                    return@processAllClassNames false
                }
            }

            return@processAllClassNames true
        }

        return TestFinderHelper.getSortedElements(classesWithProximities, true)
    }
}