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

import org.jetbrains.kotlin.load.java.structure.JavaClass
import org.jetbrains.kotlin.load.java.structure.impl.JavaPackageImpl
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.jvm.KotlinJavaPsiFacade
import org.jetbrains.kotlin.resolve.lazy.KotlinCodeAnalyzer

class JavaClassFinderImpl : AbstractJavaClassFinder() {

    private lateinit var javaFacade: KotlinJavaPsiFacade

    override fun initialize(trace: BindingTrace, codeAnalyzer: KotlinCodeAnalyzer) {
        javaFacade = KotlinJavaPsiFacade.getInstance(project)
        super.initialize(trace, codeAnalyzer)
    }

    override fun findClass(request: JavaClassFinder.Request): JavaClass? = javaFacade.findClass(request, javaSearchScope)

    override fun findPackage(fqName: FqName) = javaFacade.findPackage(fqName.asString(), javaSearchScope)?.let { JavaPackageImpl(it, javaSearchScope) }

    override fun knownClassNamesInPackage(packageFqName: FqName): Set<String>? = javaFacade.knownClassNamesInPackage(packageFqName)

}
