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

package org.jetbrains.kotlin.idea.caches

import com.intellij.util.containers.HashSet
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiField
import com.intellij.psi.PsiClass
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstance
import org.jetbrains.kotlin.idea.stubindex.JetClassShortNameIndex
import org.jetbrains.kotlin.idea.stubindex.PackageIndexUtil
import org.jetbrains.kotlin.idea.stubindex.JetFunctionShortNameIndex

// used in Upsource, what's why in idea-analysis module
public class JetShortNamesCache(private val project: com.intellij.openapi.project.Project) : com.intellij.psi.search.PsiShortNamesCache() {
    default object {
        public fun getKotlinInstance(project: com.intellij.openapi.project.Project): org.jetbrains.kotlin.idea.caches.JetShortNamesCache
                = com.intellij.openapi.extensions.Extensions.getArea(project).getExtensionPoint<com.intellij.psi.search.PsiShortNamesCache>(com.intellij.psi.search.PsiShortNamesCache.EP_NAME).getExtensions()
                .firstIsInstance<JetShortNamesCache>()
    }

    /**
     * Return kotlin class names from project sources which should be visible from java.
     */
    override fun getAllClassNames(): Array<String> {
        val classNames = JetClassShortNameIndex.getInstance().getAllKeys(project)

        // package classes can not be indexed, since they have no explicit declarations
        val packageClassShortNames = PackageIndexUtil.getAllPossiblePackageClasses(project).keySet()
        classNames.addAll(packageClassShortNames)

        return com.intellij.util.ArrayUtil.toStringArray(classNames)
    }

    /**
     * Return class names form kotlin sources in given scope which should be visible as Java classes.
     */
    override fun getClassesByName(org.jetbrains.annotations.NonNls name: String, scope: com.intellij.psi.search.GlobalSearchScope): Array<PsiClass> {
        val result = java.util.ArrayList<com.intellij.psi.PsiClass>()

        val packageClasses = PackageIndexUtil.getAllPossiblePackageClasses(project)

        // package classes can not be indexed, since they have no explicit declarations
        val fqNames = packageClasses.get(name)
        if (!fqNames.isEmpty()) {
            for (fqName in fqNames) {
                val psiClass = org.jetbrains.kotlin.asJava.JavaElementFinder.getInstance(project).findClass(fqName.asString(), scope)
                if (psiClass != null) {
                    result.add(psiClass)
                }
            }
        }

        // Quick check for classes from getAllClassNames()
        val classOrObjects = org.jetbrains.kotlin.idea.stubindex.JetClassShortNameIndex.getInstance().get(name, project, scope)
        if (classOrObjects.isEmpty()) {
            return result.copyToArray()
        }

        for (classOrObject in classOrObjects) {
            val fqName = classOrObject.getFqName()
            if (fqName != null) {
                assert(fqName.shortName().asString() == name) { "A declaration obtained from index has non-matching name:\n" + "in index: " + name + "\n" + "declared: " + fqName.shortName() + "(" + fqName + ")" }
                val psiClass = org.jetbrains.kotlin.asJava.JavaElementFinder.getInstance(project).findClass(fqName.asString(), scope)
                if (psiClass != null) {
                    result.add(psiClass)
                }
            }
        }

        return result.copyToArray()
    }

    override fun getAllClassNames(dest: HashSet<String>) {
        dest.addAll(java.util.Arrays.asList<String>(*getAllClassNames()))
    }

    override fun getMethodsByName(org.jetbrains.annotations.NonNls name: String, scope: com.intellij.psi.search.GlobalSearchScope): Array<PsiMethod>
            = array()

    override fun getMethodsByNameIfNotMoreThan(org.jetbrains.annotations.NonNls name: String, scope: com.intellij.psi.search.GlobalSearchScope, maxCount: Int): Array<PsiMethod>
            = array()

    override fun getFieldsByNameIfNotMoreThan(org.jetbrains.annotations.NonNls s: String, scope: com.intellij.psi.search.GlobalSearchScope, i: Int): Array<PsiField>
            = array()

    override fun processMethodsWithName(org.jetbrains.annotations.NonNls name: String, scope: com.intellij.psi.search.GlobalSearchScope, processor: com.intellij.util.Processor<PsiMethod>): Boolean
            = com.intellij.util.containers.ContainerUtil.process(getMethodsByName(name, scope), processor)

    override fun getAllMethodNames(): Array<String>
            = JetFunctionShortNameIndex.getInstance().getAllKeys(project).copyToArray()

    override fun getAllMethodNames(set: HashSet<String>) {
        set.addAll(JetFunctionShortNameIndex.getInstance().getAllKeys(project))
    }

    override fun getFieldsByName(org.jetbrains.annotations.NonNls name: String, scope: com.intellij.psi.search.GlobalSearchScope): Array<PsiField>
            = array()

    override fun getAllFieldNames(): Array<String>
            = array()

    override fun getAllFieldNames(set: HashSet<String>) {
    }
}
