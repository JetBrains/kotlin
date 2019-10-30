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

package org.jetbrains.kotlin.idea.core.script.dependencies

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiField
import com.intellij.psi.PsiMethod
import com.intellij.psi.impl.java.stubs.index.JavaShortClassNameIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.PsiShortNamesCache
import com.intellij.psi.stubs.StubIndex
import com.intellij.util.Processor
import com.intellij.util.containers.HashSet
import org.jetbrains.kotlin.idea.core.script.ScriptConfigurationManager

// Allow searching java classes in jars in script dependencies, this is needed for stuff like completion and autoimport
class JavaClassesInScriptDependenciesShortNameCache(private val project: Project) : PsiShortNamesCache() {
    override fun getAllClassNames() = emptyArray<String>()

    override fun getAllClassNames(dest: HashSet<String>) {}

    override fun getClassesByName(name: String, scope: GlobalSearchScope): Array<out PsiClass> {
        val classpathScope = ScriptConfigurationManager.getInstance(project).getAllScriptsDependenciesClassFilesScope()
        val classes = StubIndex.getElements(
                JavaShortClassNameIndex.getInstance().key, name, project, classpathScope.intersectWith(scope), PsiClass::class.java
        )
        return classes.toTypedArray()
    }

    override fun getMethodsByName(name: String, scope: GlobalSearchScope) = PsiMethod.EMPTY_ARRAY

    override fun getAllMethodNames() = emptyArray<String>()

    override fun getFieldsByName(name: String, scope: GlobalSearchScope) = PsiField.EMPTY_ARRAY

    override fun getMethodsByNameIfNotMoreThan(name: String, scope: GlobalSearchScope, maxCount: Int) = PsiMethod.EMPTY_ARRAY

    override fun processMethodsWithName(name: String, scope: GlobalSearchScope, processor: Processor<PsiMethod>) = true

    override fun getAllFieldNames() = emptyArray<String>()

    override fun getFieldsByNameIfNotMoreThan(name: String, scope: GlobalSearchScope, maxCount: Int) = PsiField.EMPTY_ARRAY
}