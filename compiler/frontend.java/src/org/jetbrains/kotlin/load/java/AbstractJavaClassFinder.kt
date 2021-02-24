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

package org.jetbrains.kotlin.load.java

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.DelegatingGlobalSearchScope
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.CodeAnalyzerInitializer
import org.jetbrains.kotlin.resolve.jvm.JvmCodeAnalyzerInitializer
import org.jetbrains.kotlin.resolve.jvm.TopPackageNamesProvider
import org.jetbrains.kotlin.resolve.lazy.KotlinCodeAnalyzer
import javax.annotation.PostConstruct
import javax.inject.Inject

abstract class AbstractJavaClassFinder : JavaClassFinder {
    protected lateinit var project: Project
    protected lateinit var javaSearchScope: GlobalSearchScope

    @Inject
    fun setScope(scope: GlobalSearchScope) {
        javaSearchScope =
            if (scope == GlobalSearchScope.EMPTY_SCOPE) {
                GlobalSearchScope.EMPTY_SCOPE
            } else {
                FilterOutKotlinSourceFilesScope(scope)
            }
    }

    @Inject
    open fun setProjectInstance(project: Project) {
        this.project = project
    }

    @PostConstruct
    open fun initialize(
        trace: BindingTrace,
        codeAnalyzer: KotlinCodeAnalyzer,
        languageVersionSettings: LanguageVersionSettings,
        jvmTarget: JvmTarget,
    ) {
        (CodeAnalyzerInitializer.getInstance(project) as? JvmCodeAnalyzerInitializer)?.initialize(
            trace, codeAnalyzer.moduleDescriptor, codeAnalyzer, languageVersionSettings, jvmTarget
        )
    }

    inner class FilterOutKotlinSourceFilesScope(baseScope: GlobalSearchScope) : DelegatingGlobalSearchScope(baseScope),
        TopPackageNamesProvider {

        override val topPackageNames: Set<String>?
            get() = (myBaseScope as? TopPackageNamesProvider)?.topPackageNames

        override fun contains(file: VirtualFile) =
            (file.isDirectory || file.fileType !== KotlinFileType.INSTANCE) &&
                    myBaseScope.contains(file)

        val base: GlobalSearchScope = myBaseScope

        //NOTE: expected by class finder to be not null
        override fun getProject(): Project = this@AbstractJavaClassFinder.project

        override fun toString() = "JCFI: $myBaseScope"

    }
}
