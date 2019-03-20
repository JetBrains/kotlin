/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.run

import com.intellij.openapi.module.Module
import org.jetbrains.kotlin.idea.project.platform
import org.jetbrains.kotlin.platform.impl.isJvm
import org.jetbrains.kotlin.resolve.isJvm

fun Module.asJvmModule(): Module? {
    if (platform.isJvm()) return this

    return null
}

