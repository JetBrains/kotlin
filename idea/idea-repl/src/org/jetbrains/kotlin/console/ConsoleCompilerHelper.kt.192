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

package org.jetbrains.kotlin.console

import com.intellij.execution.ExecutionManager
import com.intellij.execution.Executor
import com.intellij.execution.ui.RunContentDescriptor
import com.intellij.openapi.compiler.CompilerManager
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.task.ProjectTaskManager

class ConsoleCompilerHelper(
    private val project: Project,
    private val module: Module,
    private val executor: Executor,
    private val contentDescriptor: RunContentDescriptor
) {

    fun moduleIsUpToDate(): Boolean {
        val compilerManager = CompilerManager.getInstance(project)
        val compilerScope = compilerManager.createModuleCompileScope(module, true)
        return compilerManager.isUpToDate(compilerScope)
    }

    fun compileModule() {
        if (ExecutionManager.getInstance(project).contentManager.removeRunContent(executor, contentDescriptor)) {
            ProjectTaskManager.getInstance(project).build(arrayOf(module)) { result ->
                if (!module.isDisposed) {
                    KotlinConsoleKeeper.getInstance(project).run(module, previousCompilationFailed = result.errors > 0)
                }
            }
        }
    }
}