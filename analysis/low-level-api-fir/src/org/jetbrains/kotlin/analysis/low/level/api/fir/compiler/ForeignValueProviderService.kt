/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.compiler

import com.intellij.openapi.application.ApplicationManager
import org.jetbrains.kotlin.psi.KtCodeFragment

interface ForeignValueProviderService {
    fun getForeignValues(codeFragment: KtCodeFragment): Map<String, String>

    companion object {
        fun getInstance(): ForeignValueProviderService? {
            return ApplicationManager.getApplication().getService(ForeignValueProviderService::class.java)
        }
    }
}