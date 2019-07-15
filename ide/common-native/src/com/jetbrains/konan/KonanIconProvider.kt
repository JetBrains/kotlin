/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.jetbrains.konan

import com.intellij.openapi.application.ApplicationManager
import javax.swing.Icon

interface KonanIconProvider {
    fun getExecutableIcon(): Icon

    companion object {
        fun getInstance(): KonanIconProvider = ApplicationManager.getApplication().getComponent(KonanIconProvider::class.java)
    }
}