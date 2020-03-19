/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.jetbrains.mpp.debugger

import com.intellij.lang.Language
import com.jetbrains.cidr.execution.debugger.CidrDebuggerEditorsExtensionBase
import com.jetbrains.mpp.KonanLanguage

class MPPDebuggerEditorsExtension : CidrDebuggerEditorsExtensionBase() {
    override fun getSupportedLanguage(): Language = KonanLanguage.instance
}