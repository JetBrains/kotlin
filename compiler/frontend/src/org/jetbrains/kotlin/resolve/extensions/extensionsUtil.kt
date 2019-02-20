/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.extensions

import com.intellij.openapi.diagnostic.Logger

internal inline fun <T : Any, R> withLinkageErrorLogger(receiver: T, block: T.() -> R): R {
    try {
        return receiver.block()
    } catch (e: LinkageError) {
        val logger = Logger.getInstance(receiver::class.java)
        logger.error("${receiver::class.java.name} caused LinkageError", e)
        throw e
    }
}

