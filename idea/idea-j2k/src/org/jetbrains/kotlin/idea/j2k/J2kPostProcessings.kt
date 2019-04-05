/*
 * Copyright 2000-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.j2k

import com.intellij.openapi.components.ServiceManager
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.resolve.diagnostics.Diagnostics

interface J2kPostProcessing {
    fun createAction(element: KtElement, diagnostics: Diagnostics): (() -> Unit)?
    val writeActionNeeded: Boolean
}

interface J2KPostProcessingRegistrar {
    val processings: Collection<J2kPostProcessing>

    fun priority(processing: J2kPostProcessing): Int

    companion object {
        val instance: J2KPostProcessingRegistrar
            get() = ServiceManager.getService(J2KPostProcessingRegistrar::class.java)
    }
}