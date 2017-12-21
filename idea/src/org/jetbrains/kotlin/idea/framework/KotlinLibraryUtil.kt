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

package org.jetbrains.kotlin.idea.framework

import com.intellij.openapi.externalSystem.model.ProjectSystemId
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.roots.libraries.Library

private val MAVEN_SYSTEM_ID = ProjectSystemId("MAVEN")
val GRADLE_SYSTEM_ID = ProjectSystemId("GRADLE")

fun isExternalLibrary(library: Library): Boolean {
    return ExternalSystemApiUtil.isExternalSystemLibrary(library, ProjectSystemId.IDE) ||
           ExternalSystemApiUtil.isExternalSystemLibrary(library, GRADLE_SYSTEM_ID) ||
           ExternalSystemApiUtil.isExternalSystemLibrary(library, MAVEN_SYSTEM_ID)
}

