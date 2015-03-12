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

package org.jetbrains.kotlin.resolve

import org.jetbrains.kotlin.resolve.lazy.KotlinCodeAnalyzer
import com.intellij.openapi.project.Project
import com.intellij.openapi.components.ServiceManager
import org.jetbrains.kotlin.descriptors.ModuleDescriptor

public trait CodeAnalyzerInitializer {
    public fun initialize(trace: BindingTrace, module: ModuleDescriptor, codeAnalyzer: KotlinCodeAnalyzer?)
    public fun createTrace(): BindingTrace
    
    default object {
        public fun getInstance(project: Project): CodeAnalyzerInitializer =
                ServiceManager.getService<CodeAnalyzerInitializer>(project, javaClass<CodeAnalyzerInitializer>())!!
    }
}

public class DummyCodeAnalyzerInitializer: CodeAnalyzerInitializer {
    public override fun initialize(trace: BindingTrace, module: ModuleDescriptor, codeAnalyzer: KotlinCodeAnalyzer?) {
        // Do nothing
    }

    public override fun createTrace(): BindingTrace = BindingTraceContext()
}
