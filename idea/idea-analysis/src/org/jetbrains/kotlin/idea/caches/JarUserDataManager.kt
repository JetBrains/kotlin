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
    enum class State {
        INIT,
        HAS_FILE,
        NO_FILE
    }

    val version = 1

    val fileAttributeService: FileAttributeService? = ServiceManager.getService(javaClass<FileAttributeService>())

    public fun register(counter: JarBooleanPropertyCounter) {
        fileAttributeService?.register(counter.key.toString(), version)
    }

    public fun hasFileWithProperty(counter: JarBooleanPropertyCounter, file: VirtualFile): Boolean? {
        val jarFile = findJarRoot(file) ?: return null

        val stored = jarFile.getUserData(counter.key)
        if (stored != null && jarFile.timeStamp == stored.timestamp) {
            return stored.hasFileWithProperty
        }

        if (stored == null && fileAttributeService != null) {
            val savedData = fileAttributeService.readAttribute(counter.key.toString(), jarFile, javaClass<State>())
            if (savedData != null && savedData.value != null) {
                val hasFileWithProperty = savedData.value == State.HAS_FILE

                jarFile.putUserData(counter.key, PropertyData(hasFileWithProperty, savedData.timeStamp))

                if (jarFile.timeStamp == savedData.timeStamp) {
                    return hasFileWithProperty
                }
            }
        }

        scheduleJarProcessing(counter, jarFile)

        return null
    }

    private fun findJarRoot(file: VirtualFile): VirtualFile? {
        if (!file.getUrl().startsWith("jar://")) return null

        var jarFile = file
        while (jarFile.getParent() != null) jarFile = jarFile.getParent()

        return jarFile
    }

    private fun scheduleJarProcessing(counter: JarBooleanPropertyCounter, jarFile: VirtualFile) {
        if (jarFile.getUserData(counter.key) != null) return

        jarFile.putUserData(counter.key, PropertyData(null, jarFile.timeStamp))

        ApplicationManager.getApplication().executeOnPooledThread {
            runReadAction {
                val hasFileWithProperty = !VfsUtilCore.processFilesRecursively(jarFile) { file ->
                    !counter.hasProperty(file)
                }

                val state = if (hasFileWithProperty) State.HAS_FILE else State.NO_FILE

                val savedData = fileAttributeService?.writeAttribute(counter.key.toString(), jarFile, state)

                jarFile.putUserData(
                        counter.key,
                        PropertyData(hasFileWithProperty, (savedData?.timeStamp ?: jarFile.timeStamp)))
            }
        }
    }

    data class PropertyData(val hasFileWithProperty: Boolean?, val timestamp: Long)

    abstract class JarBooleanPropertyCounter(keyName: String) {
        val key: Key<PropertyData> = Key.create<PropertyData>(keyName)

        abstract fun hasProperty(file: VirtualFile): Boolean

        override fun toString() = "Counter: $key"
    }
}
