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

package org.jetbrains.jet.plugin.caches.resolve

import org.jetbrains.jet.lang.resolve.java.JavaDescriptorResolver
import org.jetbrains.jet.analyzer.AnalyzerFacade
import org.jetbrains.jet.lang.resolve.java.AnalyzerFacadeForJVM.JvmSetup
import org.jetbrains.jet.plugin.project.TargetPlatform
import com.intellij.openapi.project.Project

object JavaResolveExtension : CacheExtension<JavaDescriptorResolver> {
    override val platform: TargetPlatform = TargetPlatform.JVM

    override fun getData(setup: AnalyzerFacade.Setup): JavaDescriptorResolver {
        return (setup as JvmSetup).getJavaDescriptorResolver()
    }

    public fun get(project: Project): JavaDescriptorResolver = KotlinCacheService.getInstance(project)[this]
}