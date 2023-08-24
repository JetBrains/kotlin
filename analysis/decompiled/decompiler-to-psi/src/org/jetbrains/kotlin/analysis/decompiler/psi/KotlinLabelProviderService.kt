/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.decompiler.psi

import com.intellij.openapi.application.ApplicationManager

abstract class KotlinLabelProviderService {
    abstract fun getLabelForBuiltInFileType(): String
    abstract fun getLabelForKlibMetaFileType(): String

    companion object {
        fun getService(): KotlinLabelProviderService? =
            ApplicationManager.getApplication().getServiceIfCreated(KotlinLabelProviderService::class.java)
    }
}