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

package org.jetbrains.jet.lang.resolve.java

import com.intellij.openapi.project.Project
import org.jetbrains.jet.lang.resolve.lazy.KotlinCodeAnalyzer
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import javax.inject.Inject
import javax.annotation.PostConstruct
import org.jetbrains.jet.lang.resolve.BindingTrace
import org.jetbrains.jet.lang.resolve.CodeAnalyzerInitializer

public open class JavaClassFinderPostConstruct {
    PostConstruct public open fun postCreate() {}
}

public class JavaLazyAnalyzerPostConstruct : JavaClassFinderPostConstruct() {
    public var project: Project? = null
        [Inject] set

    public var trace: BindingTrace? = null
        [Inject] set

    public var codeAnalyzer: KotlinCodeAnalyzer? = null
        [Inject] set

    [PostConstruct] override fun postCreate() {
        CodeAnalyzerInitializer.getInstance(project!!).initialize(trace!!, codeAnalyzer!!.getModuleDescriptor(), codeAnalyzer)
    }
}

public class JavaDescriptorResolverPostConstruct : JavaClassFinderPostConstruct() {
    public var project: Project? = null
        [Inject] set

    public var trace: BindingTrace? = null
        [Inject] set

    public var module: ModuleDescriptor? = null
        [Inject] set

    [PostConstruct] override fun postCreate() {
        CodeAnalyzerInitializer.getInstance(project!!).initialize(trace!!, module!!, null)
    }
}

