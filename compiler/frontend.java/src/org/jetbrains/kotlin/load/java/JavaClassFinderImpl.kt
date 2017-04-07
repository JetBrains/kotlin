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

package org.jetbrains.kotlin.load.java

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.DelegatingGlobalSearchScope
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.asJava.KtLightClassMarker
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.load.java.structure.JavaClass
import org.jetbrains.kotlin.load.java.structure.impl.JavaClassImpl
import org.jetbrains.kotlin.load.java.structure.impl.JavaPackageImpl
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.CodeAnalyzerInitializer
import org.jetbrains.kotlin.resolve.jvm.KotlinJavaPsiFacade
import org.jetbrains.kotlin.resolve.lazy.KotlinCodeAnalyzer

import javax.annotation.PostConstruct
import javax.inject.Inject

class JavaClassFinderImpl : JavaClassFinder {

    private lateinit var project: Project
    private lateinit var baseScope: GlobalSearchScope
    private lateinit var javaSearchScope: GlobalSearchScope
    private lateinit var javaFacade: KotlinJavaPsiFacade

    @Inject
    fun setProject(project: Project) {
        this.project = project
    }

    @Inject
    fun setScope(scope: GlobalSearchScope) {
        this.baseScope = scope
    }

    inner class FilterOutKotlinSourceFilesScope(baseScope: GlobalSearchScope) : DelegatingGlobalSearchScope(baseScope) {

        override fun contains(file: VirtualFile) = myBaseScope.contains(file) && (file.isDirectory || file.fileType !== KotlinFileType.INSTANCE)

        val base: GlobalSearchScope = myBaseScope

        //NOTE: expected by class finder to be not null
        override fun getProject() = this@JavaClassFinderImpl.project

        override fun toString() = "JCFI: $myBaseScope"

    }

    @PostConstruct
    fun initialize(trace: BindingTrace, codeAnalyzer: KotlinCodeAnalyzer) {
        javaSearchScope = FilterOutKotlinSourceFilesScope(baseScope)
        javaFacade = KotlinJavaPsiFacade.getInstance(project)
        CodeAnalyzerInitializer.getInstance(project).initialize(trace, codeAnalyzer.moduleDescriptor, codeAnalyzer)
    }


    override fun findClass(classId: ClassId) = javaFacade.findClass(classId, javaSearchScope)

    override fun findPackage(fqName: FqName) = javaFacade.findPackage(fqName.asString(), javaSearchScope)?.let { JavaPackageImpl(it, javaSearchScope) }

    override fun knownClassNamesInPackage(packageFqName: FqName): Set<String>? = javaFacade.knownClassNamesInPackage(packageFqName)

}
