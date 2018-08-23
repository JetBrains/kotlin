/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.platform

import com.intellij.openapi.components.ServiceManager

interface DefaultIdeTargetPlatformKindProvider {
    val defaultPlatform: IdePlatform<*, *>

    companion object {
        val defaultPlatform: IdePlatform<*, *>
            get() = ServiceManager.getService(DefaultIdeTargetPlatformKindProvider::class.java).defaultPlatform
    }
}