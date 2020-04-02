/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.util

import com.intellij.DynamicBundle

abstract class AbstractKotlinBundle protected constructor(pathToBundle: String) : DynamicBundle(pathToBundle) {
    protected fun String.withHtml(): String = "<html>$this</html>"
}