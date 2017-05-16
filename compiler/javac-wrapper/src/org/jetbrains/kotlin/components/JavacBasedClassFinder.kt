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

import org.jetbrains.kotlin.javac.JavacWrapper
import org.jetbrains.kotlin.load.java.AbstractJavaClassFinder
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.CodeAnalyzerInitializer
import org.jetbrains.kotlin.resolve.lazy.KotlinCodeAnalyzer
import javax.annotation.PostConstruct

class JavacBasedClassFinder : AbstractJavaClassFinder() {

    private lateinit var javac: JavacWrapper

    @PostConstruct
    fun initialize(trace: BindingTrace, codeAnalyzer: KotlinCodeAnalyzer) {
        javaSearchScope = FilterOutKotlinSourceFilesScope(baseScope)
        javac = JavacWrapper.getInstance(proj)
        CodeAnalyzerInitializer.getInstance(proj).initialize(trace, codeAnalyzer.moduleDescriptor, codeAnalyzer)
    }

    override fun findClass(classId: ClassId) = javac.findClass(classId.asSingleFqName(), javaSearchScope)

    override fun findPackage(fqName: FqName) = javac.findPackage(fqName, javaSearchScope)

    override fun knownClassNamesInPackage(packageFqName: FqName): Set<String> = javac.findClassesFromPackage(packageFqName)
            .mapNotNullTo(hashSetOf()) {
                it.fqName?.let {
                    if (it.isRoot) it.asString() else it.shortName().asString()
                }
            }

}