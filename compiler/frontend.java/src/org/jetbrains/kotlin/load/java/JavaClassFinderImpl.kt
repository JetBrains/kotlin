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
import com.intellij.psi.PsiPackage
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.load.java.structure.JavaClass
import org.jetbrains.kotlin.load.java.structure.JavaPackage
import org.jetbrains.kotlin.load.java.structure.impl.JavaPackageImpl
import org.jetbrains.kotlin.load.java.structure.impl.source.JavaElementSourceFactory
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.jvm.KotlinJavaPsiFacade
import javax.inject.Inject

fun Project.createJavaClassFinder(scope: GlobalSearchScope): JavaClassFinder =
    JavaClassFinderImpl().apply {
        setProjectInstance(this@createJavaClassFinder)
        setScope(scope)
    }

class JavaClassFinderImpl : AbstractJavaClassFinder() {
    private lateinit var javaFacade: KotlinJavaPsiFacade

    @Inject
    override fun setProjectInstance(project: Project) {
        super.setProjectInstance(project)
        javaFacade = KotlinJavaPsiFacade.getInstance(project)
    }

    override fun findClass(request: JavaClassFinder.Request): JavaClass? {
        return javaFacade.findClass(request, javaSearchScope)
    }

    override fun findClasses(request: JavaClassFinder.Request): List<JavaClass> {
        return javaFacade.findClasses(request, javaSearchScope)
    }

    override fun findPackage(fqName: FqName, mayHaveAnnotations: Boolean): JavaPackage? {
        return javaFacade.findPackage(fqName.asString(), javaSearchScope)
            ?.let { createJavaPackage(it, mayHaveAnnotations) }
    }

    private fun createJavaPackage(
        psiPackage: PsiPackage,
        mayHaveAnnotations: Boolean,
    ): JavaPackageImpl {
        val project = javaFacade.project
        val sourceFactory = JavaElementSourceFactory.getInstance(project)
        return JavaPackageImpl(sourceFactory.createPsiSource(psiPackage), javaSearchScope, mayHaveAnnotations)
    }

    override fun knownClassNamesInPackage(packageFqName: FqName): Set<String>? {
        return javaFacade.knownClassNamesInPackage(packageFqName, javaSearchScope)
    }

    override fun canComputeKnownClassNamesInPackage(): Boolean {
        return javaFacade.canComputeKnownClassNamesInPackage()
    }
}
