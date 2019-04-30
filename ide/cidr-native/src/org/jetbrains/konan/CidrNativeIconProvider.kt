/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.konan

import com.intellij.openapi.application.ApplicationManager
import javax.swing.Icon

interface CidrNativeIconProvider {
    fun getExecutableIcon(): Icon

    companion object {
        fun getInstance(): CidrNativeIconProvider = ApplicationManager.getApplication().getComponent(CidrNativeIconProvider::class.java)
    }
}