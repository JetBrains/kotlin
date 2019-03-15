/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.platform

import com.intellij.openapi.components.ServiceManager
import org.jetbrains.kotlin.config.isJps
import org.jetbrains.kotlin.platform.impl.JvmIdePlatformKind
import org.jetbrains.kotlin.resolve.TargetPlatform

interface DefaultIdeTargetPlatformKindProvider {
    val defaultPlatform: IdePlatform<*, *>

    companion object {
        val defaultPlatform: IdePlatform<*, *>
            get() {
                if (isJps) {
                    // TODO support passing custom platforms in JPS
                    return JvmIdePlatformKind.defaultPlatform
                }

                return ServiceManager.getService(DefaultIdeTargetPlatformKindProvider::class.java).defaultPlatform
            }

        val defaultCompilerPlatform: TargetPlatform
            get() = defaultPlatform.kind.compilerPlatform
    }
}