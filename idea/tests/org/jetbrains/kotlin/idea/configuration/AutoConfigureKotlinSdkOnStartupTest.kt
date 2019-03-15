/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.configuration

import com.intellij.openapi.projectRoots.ProjectJdkTable
import org.jetbrains.kotlin.idea.framework.KotlinSdkType
import org.junit.Assert

// Do not add new tests here since application is initialized only once
class AutoConfigureKotlinSdkOnStartupTest : AbstractConfigureKotlinInTempDirTest() {
    fun testKotlinSdkAdded() {
        Assert.assertTrue(ProjectJdkTable.getInstance().allJdks.any { it.sdkType is KotlinSdkType })
    }
}