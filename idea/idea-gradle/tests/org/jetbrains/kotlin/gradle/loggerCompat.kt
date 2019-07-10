/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import com.intellij.openapi.diagnostic.Logger
import java.lang.reflect.Modifier

// BUNCH: 182
internal fun getLoggerFactory(): Class<out Logger.Factory> {
    return Logger.getFactory().javaClass
}