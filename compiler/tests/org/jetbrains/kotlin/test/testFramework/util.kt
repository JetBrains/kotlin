/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.test.testFramework

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.ComponentManager
import com.intellij.openapi.vfs.VirtualFile
import org.picocontainer.MutablePicoContainer

fun <T> ComponentManager.registerServiceInstance(interfaceClass: Class<T>, instance: T) {
    val picoContainer = picoContainer as MutablePicoContainer
    val key = interfaceClass.name
    picoContainer.unregisterComponent(key)
    picoContainer.registerComponentInstance(key, instance)
}

fun deleteFile(file: VirtualFile) {
    runInEdtAndWait { runWriteAction { file.delete(null) } }
}

fun <T> runWriteAction(action: () -> T): T {
    return ApplicationManager.getApplication().runWriteAction<T>(action)
}