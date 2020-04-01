/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.project

import com.intellij.openapi.module.Module
import com.intellij.openapi.util.Key

object SecondaryModuleUserDataKey : Key<Boolean>("secondary module")

val Module.isSecondary: Boolean
    get() = getUserData(SecondaryModuleUserDataKey) == true

fun Module.markAsSecondary() {
    putUserData(SecondaryModuleUserDataKey, true)
}