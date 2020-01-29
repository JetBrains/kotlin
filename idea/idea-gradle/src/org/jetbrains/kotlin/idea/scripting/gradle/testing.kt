/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.scripting.gradle

import com.intellij.openapi.application.Application

// this property is used in tests to avoid full gradle project import and to make those tests more lightweight
var testAffectedGradleProjectFiles: Boolean = false

internal val Application.isUnitTestModeWithoutAffectedGradleProjectFilesCheck: Boolean
    get() {
        if (isUnitTestMode) {
            return !testAffectedGradleProjectFiles
        }
        return false
    }