/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.test

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.projectRoots.ProjectJdkTable
import com.intellij.openapi.projectRoots.Sdk
import org.jetbrains.kotlin.idea.framework.KotlinSdkType

class KotlinSdkCreationChecker {

    private val sdksBefore: Array<out Sdk> = ProjectJdkTable.getInstance().allJdks

    fun getKotlinSdks() = ProjectJdkTable.getInstance().allJdks.filter { it.sdkType is KotlinSdkType }

    private fun getCreatedKotlinSdks() =
        ProjectJdkTable.getInstance().allJdks.filter { !sdksBefore.contains(it) && it.sdkType is KotlinSdkType }

    fun isKotlinSdkCreated() = getCreatedKotlinSdks().isNotEmpty()

    fun removeNewKotlinSdk() {
        val jdkTable = ProjectJdkTable.getInstance()
        ApplicationManager.getApplication().invokeAndWait {
            runWriteAction {
                getCreatedKotlinSdks().forEach { jdkTable.removeJdk(it) }
            }
        }
    }
}