/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.api.targets

import org.jetbrains.kotlin.fir.FirElementWithResolveState
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.FirScript

/**
 * This interface describes how to process nested declarations.
 *
 * @see LLFirResolveTarget
 */
internal interface LLFirResolveTargetVisitor {
    /**
     * Access to [FirFile] declaration will be performed inside [action].
     */
    fun withFile(firFile: FirFile, action: () -> Unit): Unit = action()

    /**
     * Access to elements inside [FirRegularClass] will be performed inside [action].
     * Will be called for each nested [FirRegularClass] on the path.
     */
    fun withRegularClass(firClass: FirRegularClass, action: () -> Unit): Unit = action()

    /**
     * Access to elements inside [FirScript] will be performed inside [action].
     */
    fun withScript(firScript: FirScript, action: () -> Unit): Unit = action()

    /**
     * This method will be performed on some target element depends on [LLFirResolveTarget] implementation.
     */
    fun performAction(element: FirElementWithResolveState)
}
