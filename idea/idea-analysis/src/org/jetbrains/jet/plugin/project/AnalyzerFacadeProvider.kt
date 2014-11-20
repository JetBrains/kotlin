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

package org.jetbrains.jet.plugin.project

import org.jetbrains.jet.analyzer.AnalyzerFacade
import org.jetbrains.jet.lang.resolve.java.JvmPlatformParameters
import org.jetbrains.jet.analyzer.ResolverForModule
import org.jetbrains.jet.lang.resolve.java.JvmAnalyzerFacade
import org.jetbrains.jet.plugin.caches.resolve.JsAnalyzerFacade

public object AnalyzerFacadeProvider {
    //NOTE: it's convenient that JS backend doesn't have platform parameters (for now)
    // otherwise we would be forced to add casts on the call site of setupResolverForProject
    public fun getAnalyzerFacade(targetPlatform: TargetPlatform): AnalyzerFacade<out ResolverForModule, JvmPlatformParameters> {
        return when (targetPlatform) {
            TargetPlatform.JVM -> JvmAnalyzerFacade
            TargetPlatform.JS -> JsAnalyzerFacade
            else -> throw IllegalArgumentException("Unsupported platfrom: $targetPlatform")
        }
    }
}