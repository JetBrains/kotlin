/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.extensions

import org.jetbrains.kotlin.psi.KtxElement
import org.jetbrains.kotlin.resolve.calls.CallResolver
import org.jetbrains.kotlin.types.expressions.ExpressionTypingContext
import org.jetbrains.kotlin.types.expressions.ExpressionTypingFacade

interface KtxTypeResolutionExtension {
    companion object : ProjectExtensionDescriptor<KtxTypeResolutionExtension>(
            "org.jetbrains.kotlin.ktxTypeResolutionExtension",
            KtxTypeResolutionExtension::class.java
    )

    fun visitKtxElement(
        element: KtxElement,
        context: ExpressionTypingContext,
        facade: ExpressionTypingFacade,
        callResolver: CallResolver
    )
}

