/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.core.script

import com.intellij.openapi.project.Project

// BUNCH: 182
fun initializeScriptModificationListener(project: Project) {

}

// This service was a workaround for the bug in the platform: the notification 'Gradle project needs to be imported' wasn't shown
// after the changes in .gradle.kts files
// It was fixed in >183 (see GradleAutoImportAware.getAffectedExternalProjectFiles)