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

package org.jetbrains.kotlin.idea.caches.resolve

import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootModificationTracker
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import org.jetbrains.kotlin.idea.project.TargetPlatformDetector
import org.jetbrains.kotlin.js.resolve.JsPlatform

//TODO: this should go away to support cross-platform projects
object JsProjectDetector {
    @JvmStatic
    fun isJsProject(project: Project): Boolean {
        return CachedValuesManager.getManager(project).getCachedValue(project) {
            val result = ModuleManager.getInstance(project).modules.any { TargetPlatformDetector.getPlatform(it) == JsPlatform }
            CachedValueProvider.Result(result, ProjectRootModificationTracker.getInstance(project))
        }
    }
}
