/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.framework

import com.intellij.openapi.externalSystem.model.ProjectSystemId
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.module.Module
import com.intellij.openapi.roots.libraries.Library

val MAVEN_SYSTEM_ID = ProjectSystemId("Maven")
val GRADLE_SYSTEM_ID = ProjectSystemId("GRADLE")
val KOBALT_SYSTEM_ID = ProjectSystemId("KOBALT")

fun isExternalLibrary(library: Library): Boolean {
    return ExternalSystemApiUtil.isExternalSystemLibrary(library, ProjectSystemId.IDE) ||
           ExternalSystemApiUtil.isExternalSystemLibrary(library, GRADLE_SYSTEM_ID) ||
           ExternalSystemApiUtil.isExternalSystemLibrary(library, KOBALT_SYSTEM_ID) ||
           ExternalSystemApiUtil.isExternalSystemLibrary(library, MAVEN_SYSTEM_ID)
}


fun Module.isGradleModule() =
    ExternalSystemApiUtil.isExternalSystemAwareModule(GRADLE_SYSTEM_ID, this)
