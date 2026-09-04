/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.decompiler.psi

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.serviceOrNull
import org.jetbrains.kotlin.psi.KtPlatformInterface

/**
 * Supplies human-readable, localizable descriptions for the binary Kotlin file types.
 *
 * The file types fall back to hard-coded English descriptions when no service is registered, so a platform only needs to implement
 * this service if it presents the file types in a user interface.
 */
@KtPlatformInterface
abstract class KotlinLabelProviderService {
    /**
     * Returns the description of [KotlinBuiltInFileType].
     */
    abstract fun getLabelForBuiltInFileType(): String

    /**
     * Returns the description of [org.jetbrains.kotlin.analysis.decompiler.konan.KlibMetaFileType].
     */
    abstract fun getLabelForKlibMetaFileType(): String

    @Deprecated("The function is unused. Its implementation should be dropped")
    open fun getLabelForKotlinJavaScriptMetaFileType(): String = ""

    @KtPlatformInterface
    companion object {
        /**
         * Returns the [KotlinLabelProviderService] registered by the current platform, or `null` if the platform provides none.
         */
        fun getService(): KotlinLabelProviderService? = ApplicationManager.getApplication().serviceOrNull()
    }
}
