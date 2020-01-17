/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.util

import java.util.concurrent.Future

object ProgressIndicatorUtils {
    @JvmStatic
    fun <T> awaitWithCheckCanceled(future: Future<T>) =
        com.intellij.openapi.progress.util.ProgressIndicatorUtils.awaitWithCheckCanceled(future)
}