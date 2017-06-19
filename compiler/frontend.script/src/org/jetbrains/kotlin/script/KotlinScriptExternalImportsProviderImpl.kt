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

package org.jetbrains.kotlin.script

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import java.io.File
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write
import kotlin.script.dependencies.KotlinScriptExternalDependencies

class KotlinScriptExternalImportsProviderImpl(
        val project: Project,
        private val scriptDefinitionProvider: KotlinScriptDefinitionProvider
) : KotlinScriptExternalImportsProviderBase(project) {

    private val cacheLock = ReentrantReadWriteLock()
    private val cache = hashMapOf<String, KotlinScriptExternalDependencies?>()

    override fun <TF : Any> getScriptDependencies(file: TF): KotlinScriptExternalDependencies? = cacheLock.read {
        calculateExternalDependencies(file)
    }

    private fun <TF : Any> calculateExternalDependencies(file: TF): KotlinScriptExternalDependencies? {
        val path = getFilePath(file)
        val cached = cache[path]
        return if (cached != null) cached
        else {
            val scriptDef = scriptDefinitionProvider.findScriptDefinition(file)
            if (scriptDef != null) {
                val deps = resolveDependencies(scriptDef, file)

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

private val log = Logger.getInstance(KotlinScriptExternalImportsProvider::class.java)