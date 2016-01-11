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
import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.io.URLUtil
import org.jetbrains.kotlin.idea.util.application.runReadAction
import java.util.concurrent.atomic.AtomicBoolean

object JarUserDataManager {
    enum class State {
        INIT,
        HAS_FILE,
        NO_FILE
    }

    val version = 2

    val fileAttributeService: FileAttributeService? = ServiceManager.getService(FileAttributeService::class.java)

    fun register(counter: JarBooleanPropertyCounter) {
        fileAttributeService?.register(counter.key.toString(), version)
    }

    fun hasFileWithProperty(counter: JarBooleanPropertyCounter, file: VirtualFile): Boolean? {
        val localJarFile = JarFileSystemUtil.findLocalJarFile(file) ?: return null

        val stored = localJarFile.getUserData(counter.key)
        if (stored != null) {
            if (localJarFile.timeStamp == stored.timestamp) {
                return stored.hasFileWithProperty
            }
        }

        if (stored == null && fileAttributeService != null) {
            val savedData = fileAttributeService.readEnumAttribute(counter.key.toString(), localJarFile, State::class.java)
            if (savedData != null) {
                val hasFileWithProperty = savedData.value == State.HAS_FILE

                storeUserData(counter, localJarFile, hasFileWithProperty, savedData.timeStamp)

                if (localJarFile.timeStamp == savedData.timeStamp) {
                    return hasFileWithProperty
                }
            }
        }

        val jarFileRoot = JarFileSystemUtil.findJarFileRoot(file) ?: return null
        scheduleJarProcessing(counter, jarFileRoot, localJarFile)

        return null
    }

    private fun scheduleJarProcessing(counter: JarBooleanPropertyCounter, jarFile: VirtualFile, localJarFile: VirtualFile) {
        val userData = localJarFile.getUserData(counter.key)
        if (userData != null && localJarFile.timeStamp == userData.timestamp) return

        storeUserData(counter, localJarFile, null)

        ApplicationManager.getApplication().executeOnPooledThread {
            runReadAction {
                val data = localJarFile.getUserData(counter.key)
                if (data != null &&
                        ((data.hasFileWithProperty != null && localJarFile.timeStamp == data.timestamp) || !data.isProcessStarted.compareAndSet(false, true))) {
                    // Processing has started in some other thread or is already finished
                    return@runReadAction
                }

                val hasFileWithProperty = !VfsUtilCore.processFilesRecursively(jarFile) { file ->
                    !counter.hasProperty(file)
                }

                val state = if (hasFileWithProperty) State.HAS_FILE else State.NO_FILE

                val savedData = fileAttributeService?.writeEnumAttribute(counter.key.toString(), localJarFile, state)

                storeUserData(counter, localJarFile, hasFileWithProperty, (savedData?.timeStamp ?: localJarFile.timeStamp))
            }
        }
    }

    private fun storeUserData(counter: JarBooleanPropertyCounter, localJarFile: VirtualFile,
                              hasFileWithProperty: Boolean?, timestamp: Long? = null) {
        assert((timestamp == null) == (hasFileWithProperty == null)) { "Using empty timestamp is only allowed for storing not counted value" }

        localJarFile.putUserData(counter.key,
                                 PropertyData(hasFileWithProperty, timestamp ?: localJarFile.timeStamp, isProcessStarted = AtomicBoolean(false)))
    }

    object JarFileSystemUtil {
        fun findJarFileRoot(inJarFile: VirtualFile): VirtualFile? {
            if (!inJarFile.url.startsWith("jar://")) return null

            var jarFile = inJarFile
            while (jarFile.parent != null) jarFile = jarFile.parent

            return jarFile
        }

        fun findLocalJarFile(inJarFile: VirtualFile): VirtualFile? {
            if (!inJarFile.url.startsWith("jar://")) return null

            val path = inJarFile.path

            val jarSeparatorIndex = path.indexOf(URLUtil.JAR_SEPARATOR)
            assert(jarSeparatorIndex >= 0) { "Path passed to JarFileSystem must have jar separator '!/': $path" }
            val localPath = path.substring(0, jarSeparatorIndex)

            return StandardFileSystems.local().findFileByPath(localPath)
        }
    }

    data class PropertyData(val hasFileWithProperty: Boolean?, val timestamp: Long, val isProcessStarted: AtomicBoolean)

    abstract class JarBooleanPropertyCounter(keyName: String) {
        val key: Key<PropertyData> = Key.create<PropertyData>(keyName)

        abstract fun hasProperty(file: VirtualFile): Boolean

        override fun toString() = "Counter: $key"
    }
}
