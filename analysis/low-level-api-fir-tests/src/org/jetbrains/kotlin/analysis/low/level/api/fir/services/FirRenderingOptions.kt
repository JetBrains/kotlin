/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.services

import org.jetbrains.kotlin.test.services.TestService
import org.jetbrains.kotlin.test.services.TestServices

internal class FirRenderingOptions(
    val renderKtText: Boolean = false,
    val renderKtFileName: Boolean = false,
    val renderContainerSource: Boolean = false,
) : TestService {
    companion object {
        val DEFAULT = FirRenderingOptions()
    }

    class Builder {
        var renderKtText: Boolean = false
        var renderKtFileName: Boolean = false
        var renderContainerSource: Boolean = false

        fun build(): FirRenderingOptions = FirRenderingOptions(
            renderKtText,
            renderKtFileName,
            renderContainerSource,
        )
    }
}

internal val TestServices.firRenderingOptionsIfRegistered: FirRenderingOptions? by TestServices.nullableTestServiceAccessor()
internal val TestServices.firRenderingOptions: FirRenderingOptions
    get() = firRenderingOptionsIfRegistered ?: FirRenderingOptions.DEFAULT
