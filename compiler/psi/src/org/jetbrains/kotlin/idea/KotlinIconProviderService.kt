/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.idea

import com.intellij.openapi.application.ApplicationManager
import javax.swing.Icon

abstract class KotlinIconProviderService {
    abstract val fileIcon: Icon?
    abstract val builtInFileIcon: Icon?

    class CompilerKotlinFileIconProviderService : KotlinIconProviderService() {
        override val fileIcon: Icon? = null
        override val builtInFileIcon: Icon? = null
    }

    companion object {
        val instance: KotlinIconProviderService
            get() {
                val service =
                    ApplicationManager.getApplication()
                        .getService(KotlinIconProviderService::class.java)
                return service ?: CompilerKotlinFileIconProviderService()
            }
    }
}
