/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.jvm.modules

import com.intellij.openapi.util.SystemInfo

fun isAtLeastJava9(): Boolean {
    return SystemInfo.isJavaVersionAtLeast(9, 0, 0)
}
