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

package org.jetbrains.kotlin.cli.common.script

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.script.ScriptContentLoader
import org.jetbrains.kotlin.script.ScriptDefinitionProvider
import org.jetbrains.kotlin.script.ScriptDependenciesProvider
import java.io.File
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write
import kotlin.script.experimental.dependencies.ScriptDependencies

class CliScriptDependenciesProvider(
        project: Project,
        private val scriptDefinitionProvider: ScriptDefinitionProvider
) : ScriptDependenciesProvider {

    private val cacheLock = ReentrantReadWriteLock()
    private val cache = hashMapOf<String, ScriptDependencies?>()
    private val scriptContentLoader = ScriptContentLoader(project)

    override fun getScriptDependencies(file: VirtualFile): ScriptDependencies? = cacheLock.read {
        calculateExternalDependencies(file)
    }

    private fun calculateExternalDependencies(file: VirtualFile): ScriptDependencies? {
        val path = file.path
        val cached = cache[path]
        return if (cached != null) cached
        else {
            val scriptDef = scriptDefinitionProvider.findScriptDefinition(file)
            if (scriptDef != null) {
                val deps = scriptContentLoader.loadContentsAndResolveDependencies(scriptDef, file)

                if (deps != null) {
                    log.info("[kts] new cached deps for $path: ${deps.classpath.joinToString(File.pathSeparator)}")
                }
                cacheLock.write {
                    cache.put(path, deps)
                }
                deps
            }
            else null
        }
    }
}

private val log = Logger.getInstance(ScriptDependenciesProvider::class.java)