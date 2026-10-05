/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.internal

import org.jetbrains.kotlin.buildtools.api.trackers.CompilerLookupTracker

internal class LookupMessageVisitor(private val lookupTracker: CompilerLookupTracker, private val operationId: Int) : MessageVisitor {

    override fun accept(message: Any): Boolean {
//        lookupTracker.accept(message)
        return false
    }
}
