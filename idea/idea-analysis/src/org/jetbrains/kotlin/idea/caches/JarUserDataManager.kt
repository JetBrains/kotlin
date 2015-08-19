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

package org.jetbrains.kotlin.idea.caches

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.idea.util.application.runReadAction

public object JarUserDataManager {
    val fileAttributeService: FileAttributeService? = ServiceManager.getService(javaClass<FileAttributeService>())

    public fun register(collector: JarUserDataCollector<*>) {
        fileAttributeService?.register(collector.key.toString(), collector.version)
    }

    public fun <T: Enum<T>> getValue(collector: JarUserDataCollector<T>, file: VirtualFile): T? {
        val jarFile = findJarRoot(file) ?: return null

        val stored = jarFile.getUserData(collector.key)
        if (stored != null && jarFile.timeStamp == stored.second) {
            return stored.first
        }

        if (stored == null && fileAttributeService != null) {
            val savedData = fileAttributeService.readAttribute(collector.key.toString(), jarFile, collector.stateClass)
            if (savedData != null && savedData.value != null) {
                jarFile.putUserData(collector.key, savedData.value to savedData.timeStamp)

                if (jarFile.timeStamp == savedData.timeStamp) {
                    return savedData.value
                }
            }
        }

        scheduleJarProcessing(collector, jarFile)

        return null
    }

    private fun findJarRoot(file: VirtualFile): VirtualFile? {
        if (!file.getUrl().startsWith("jar://")) return null

        var jarFile = file
        while (jarFile.getParent() != null) jarFile = jarFile.getParent()

        return jarFile
    }

    private fun <T: Enum<T>> scheduleJarProcessing(collector: JarUserDataCollector<T>, jarFile: VirtualFile) {
        if (jarFile.getUserData(collector.key) != null) return

        jarFile.putUserData(collector.key, collector.init to jarFile.timeStamp)

        ApplicationManager.getApplication().executeOnPooledThread {
            runReadAction {
                var result = collector.notFoundState

                VfsUtilCore.processFilesRecursively(jarFile) { file ->
                    if (collector.process(file) == collector.stopState) {
                        result = collector.stopState

                        // stop processing
                        false
                    }
                    else {
                        // continue processing
                        true
                    }
                }

                val savedData = fileAttributeService?.writeAttribute(collector.key.toString(), jarFile, result)
                jarFile.putUserData(collector.key, result to (savedData?.timeStamp ?: jarFile.timeStamp))
            }
        }
    }

    interface JarUserDataCollector<State: Enum<State>> {
        val key: Key<Pair<State, Long>>
        val stateClass: Class<State>

        val version: Int get() = 1

        val init: State
        val stopState: State
        val notFoundState: State

        fun process(file: VirtualFile): State
    }
}
