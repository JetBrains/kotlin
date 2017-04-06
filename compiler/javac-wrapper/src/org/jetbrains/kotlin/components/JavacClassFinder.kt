/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.components

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.DelegatingGlobalSearchScope
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.javac.Javac
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.load.java.JavaClassFinder
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.CodeAnalyzerInitializer
import org.jetbrains.kotlin.resolve.lazy.KotlinCodeAnalyzer
import javax.annotation.PostConstruct
import javax.inject.Inject

class JavacClassFinder : JavaClassFinder {

    private lateinit var project: Project
    private lateinit var baseScope: GlobalSearchScope
    private lateinit var javaSearchScope: GlobalSearchScope
    private lateinit var javac: Javac

    @Inject
    fun setProject(project: Project) {
        this.project = project
    }

    @Inject
    fun setScope(scope: GlobalSearchScope) {
        baseScope = scope
    }

    inner class FilterOutKotlinSourceFilesScope(baseScope: GlobalSearchScope) : DelegatingGlobalSearchScope(baseScope) {

        override fun contains(file: VirtualFile) = myBaseScope.contains(file) && (file.isDirectory || file.fileType !== KotlinFileType.INSTANCE)

        val base: GlobalSearchScope = myBaseScope

        //NOTE: expected by class finder to be not null
        override fun getProject() = this@JavacClassFinder.project

        override fun toString() = "$myBaseScope"
    }

    @PostConstruct
    fun initialize(trace: BindingTrace, codeAnalyzer: KotlinCodeAnalyzer) {
        javaSearchScope = FilterOutKotlinSourceFilesScope(baseScope)
        javac = Javac.getInstance(project)
        CodeAnalyzerInitializer.getInstance(project).initialize(trace, codeAnalyzer.moduleDescriptor, codeAnalyzer)
    }

    override fun findClass(classId: ClassId) = javac.findClass(classId.asSingleFqName(), javaSearchScope)

    override fun findPackage(fqName: FqName) = javac.findPackage(fqName, javaSearchScope)

    override fun knownClassNamesInPackage(packageFqName: FqName) = javac.findClassesFromPackage(packageFqName)
            .mapNotNull {
                it.fqName?.let {
                    if (it.isRoot) it.asString() else it.shortName().asString()
                }
            }
            .toSet()

}