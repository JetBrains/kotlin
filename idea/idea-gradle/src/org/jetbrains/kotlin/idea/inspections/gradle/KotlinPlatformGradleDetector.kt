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

package org.jetbrains.kotlin.idea.inspections.gradle

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.model.ProjectKeys
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil

interface KotlinPlatformGradleDetector {
    companion object {
        val EP_NAME: ExtensionPointName<KotlinPlatformGradleDetector> = ExtensionPointName.create("org.jetbrains.kotlin.platformGradleDetector")
    }

    fun getResolvedKotlinStdlibVersionByModuleData(moduleData: DataNode<*>, libraryIds: List<String>): String?
}

class DefaultPlatformGradleDetector : KotlinPlatformGradleDetector {
    override fun getResolvedKotlinStdlibVersionByModuleData(moduleData: DataNode<*>, libraryIds: List<String>): String? {
        for (libraryDependencyData in ExternalSystemApiUtil.findAllRecursively(moduleData, ProjectKeys.LIBRARY_DEPENDENCY)) {
            for (libraryId in libraryIds) {
                val libraryNameMarker = "org.jetbrains.kotlin:$libraryId:"
                if (libraryDependencyData.data.externalName.startsWith(libraryNameMarker)) {
                    return libraryDependencyData.data.externalName.substringAfter(libraryNameMarker)
                }
            }
        }
        return null
    }
}