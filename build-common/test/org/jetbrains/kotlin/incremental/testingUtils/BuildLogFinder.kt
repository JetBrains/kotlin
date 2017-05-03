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

package org.jetbrains.kotlin.incremental.testingUtils

import java.io.File

class BuildLogFinder(
        private val isDataContainerBuildLogEnabled: Boolean = false,
        private val isGradleEnabled: Boolean = false
) {
    companion object {
        private const val GRADLE_LOG = "gradle-build.log"
        private const val DATA_CONTAINER_LOG = "data-container-version-build.log"
        private const val SIMPLE_LOG = "build.log"
    }

    fun findBuildLog(dir: File): File? {
        val names = dir.list() ?: arrayOf()
        val files = names.filter { File(dir, it).isFile }.toSet()
        val matchedName = when {
            isGradleEnabled && GRADLE_LOG in files -> GRADLE_LOG
            isDataContainerBuildLogEnabled && DATA_CONTAINER_LOG in files -> DATA_CONTAINER_LOG
            SIMPLE_LOG in files -> SIMPLE_LOG
            else -> null
        }

        return File(dir, matchedName ?: return null)
    }
}

