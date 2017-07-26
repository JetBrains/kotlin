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

package org.jetbrains.kotlin.idea.project

import org.jetbrains.kotlin.analyzer.AnalyzerFacade
import org.jetbrains.kotlin.analyzer.common.DefaultAnalyzerFacade
import org.jetbrains.kotlin.idea.caches.resolve.JsAnalyzerFacade
import org.jetbrains.kotlin.js.resolve.JsPlatform
import org.jetbrains.kotlin.resolve.TargetPlatform
import org.jetbrains.kotlin.resolve.jvm.JvmAnalyzerFacade
import org.jetbrains.kotlin.resolve.jvm.platform.JvmPlatform

object AnalyzerFacadeProvider {
    //NOTE: it's convenient that JS backend doesn't have platform parameters (for now)
    // otherwise we would be forced to add casts on the call site of setupResolverForProject
    fun getAnalyzerFacade(targetPlatform: TargetPlatform): AnalyzerFacade {
        return when (targetPlatform) {
            JvmPlatform -> JvmAnalyzerFacade
            JsPlatform -> JsAnalyzerFacade
            TargetPlatform.Default -> DefaultAnalyzerFacade
            else -> throw IllegalArgumentException("Unsupported platform: $targetPlatform")
        }
    }
}
