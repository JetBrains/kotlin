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

package org.jetbrains.jet.plugin.caches

import com.intellij.openapi.extensions.Extensions
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.PsiShortNamesCache
import com.intellij.util.ArrayUtil
import com.intellij.util.Processor
import com.intellij.util.containers.ContainerUtil
import org.jetbrains.annotations.NonNls
import org.jetbrains.jet.asJava.JavaElementFinder
import org.jetbrains.jet.lang.descriptors.*
import org.jetbrains.jet.lang.psi.*
import org.jetbrains.jet.lang.psi.psiUtil.*
import org.jetbrains.jet.lang.resolve.BindingContext
import org.jetbrains.jet.lang.resolve.BindingTraceContext
import org.jetbrains.jet.lang.resolve.ImportPath
import org.jetbrains.jet.lang.resolve.QualifiedExpressionResolver
import org.jetbrains.jet.lang.resolve.lazy.KotlinCodeAnalyzer
import org.jetbrains.jet.lang.resolve.lazy.ResolveSessionUtils
import org.jetbrains.jet.lang.resolve.name.FqName
import org.jetbrains.jet.lang.resolve.name.Name
import org.jetbrains.jet.lang.resolve.scopes.JetScope
import org.jetbrains.jet.lang.types.JetType
import org.jetbrains.jet.lang.types.expressions.ExpressionTypingUtils
import org.jetbrains.jet.plugin.caches.resolve.IDELightClassGenerationSupport
import org.jetbrains.jet.plugin.project.ResolveSessionForBodies
import org.jetbrains.jet.plugin.stubindex.*

import java.util.*
import com.intellij.util.containers

/**
 * Will provide both java elements from kotlin context and some declarations special to kotlin.
 * All those declaration are planned to be used in completion.
 */
public class JetShortNamesCache(private val project: Project) : PsiShortNamesCache() {
    class object {
        public fun getKotlinInstance(project: Project): JetShortNamesCache {
            val extensions = Extensions.getArea(project).getExtensionPoint<PsiShortNamesCache>(PsiShortNamesCache.EP_NAME).getExtensions()
            for (extension in extensions) {
                if (extension is JetShortNamesCache) {
                    return extension as JetShortNamesCache
                }
            }
            throw IllegalStateException(javaClass<JetShortNamesCache>().getSimpleName() + " is not found for project " + project)
        }
    }

    /**
     * Return kotlin class names from project sources which should be visible from java.
     */
    override fun getAllClassNames(): Array<String> {
        val classNames = JetClassShortNameIndex.getInstance().getAllKeys(project)

        // package classes can not be indexed, since they have no explicit declarations
        val lightClassGenerationSupport = IDELightClassGenerationSupport.getInstanceForIDE(project)
        val packageClassShortNames = lightClassGenerationSupport.getAllPossiblePackageClasses(GlobalSearchScope.allScope(project)).keySet()
        classNames.addAll(packageClassShortNames)

        return ArrayUtil.toStringArray(classNames)
    }

    /**
     * Return class names form kotlin sources in given scope which should be visible as Java classes.
     */
    override fun getClassesByName(NonNls name: String, scope: GlobalSearchScope): Array<PsiClass> {
        val result = ArrayList<PsiClass>()

        val lightClassGenerationSupport = IDELightClassGenerationSupport.getInstanceForIDE(project)
        val packageClasses = lightClassGenerationSupport.getAllPossiblePackageClasses(scope)

        // package classes can not be indexed, since they have no explicit declarations
        val fqNames = packageClasses.get(name)
        if (!fqNames.isEmpty()) {
            for (fqName in fqNames) {
                val psiClass = JavaElementFinder.getInstance(project).findClass(fqName.asString(), scope)
                if (psiClass != null) {
                    result.add(psiClass)
                }
            }
        }

        // Quick check for classes from getAllClassNames()
        val classOrObjects = JetClassShortNameIndex.getInstance().get(name, project, scope)
        if (classOrObjects.isEmpty()) {
            return result.copyToArray()
        }

        for (classOrObject in classOrObjects) {
            val fqName = classOrObject.getFqName()
            if (fqName != null) {
                assert(fqName.shortName().asString() == name) { "A declaration obtained from index has non-matching name:\n" + "in index: " + name + "\n" + "declared: " + fqName.shortName() + "(" + fqName + ")" }
                val psiClass = JavaElementFinder.getInstance(project).findClass(fqName.asString(), scope)
                if (psiClass != null) {
                    result.add(psiClass)
                }
            }
        }

        return result.copyToArray()
    }

    override fun getAllClassNames(destination: containers.HashSet<String>) {
        destination.addAll(Arrays.asList<String>(*getAllClassNames()))
    }

    override fun getMethodsByName(NonNls name: String, scope: GlobalSearchScope): Array<PsiMethod>
            = array()

    override fun getMethodsByNameIfNotMoreThan(NonNls name: String, scope: GlobalSearchScope, maxCount: Int): Array<PsiMethod>
            = array()

    override fun getFieldsByNameIfNotMoreThan(NonNls s: String, scope: GlobalSearchScope, i: Int): Array<PsiField>
            = array()

    override fun processMethodsWithName(NonNls name: String, scope: GlobalSearchScope, processor: Processor<PsiMethod>): Boolean
            = ContainerUtil.process(getMethodsByName(name, scope), processor)

    override fun getAllMethodNames(): Array<String>
            = array()

    override fun getAllMethodNames(set: containers.HashSet<String>) {
        set.addAll(JetTopLevelNonExtensionFunctionShortNameIndex.getInstance().getAllKeys(project))
    }

    override fun getFieldsByName(NonNls name: String, scope: GlobalSearchScope): Array<PsiField>
            = array()

    override fun getAllFieldNames(): Array<String>
            = array()

    override fun getAllFieldNames(set: containers.HashSet<String>) {
    }
}
