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

package org.jetbrains.kotlin.idea.util

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project

fun <T> Project.runReadActionInSmartMode(action: () -> T): T {
    if (ApplicationManager.getApplication().isReadAccessAllowed) return action()
    return DumbService.getInstance(this).runReadActionInSmartMode<T>(action)
}

fun <T> Project.runWithAlternativeResolveEnabled(action: () -> T): T {
    var result: T = null as T
    DumbService.getInstance(this).withAlternativeResolveEnabled { result = action() }
    @Suppress("USELESS_CAST")
    return result as T
}

fun Project.runWhenSmart(action: () -> Unit) {
    DumbService.getInstance(this).runWhenSmart(action)
}