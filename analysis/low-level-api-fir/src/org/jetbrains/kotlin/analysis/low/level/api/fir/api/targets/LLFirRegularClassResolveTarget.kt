/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.api.targets

import org.jetbrains.kotlin.analysis.low.level.api.fir.api.FirDesignation
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.withFirDesignationEntry
import org.jetbrains.kotlin.fir.FirElementWithResolveState
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.utils.exceptions.requireWithAttachment

internal sealed class LLFirRegularClassResolveTarget(designation: FirDesignation) : LLFirResolveTarget(designation) {
    init {
        requireWithAttachment(
            target is FirRegularClass,
            { "Expected type of '${::target.name}' is ${FirRegularClass::class.simpleName}, but ${target::class.simpleName} is found" },
        ) {
            withFirDesignationEntry("designation", this@LLFirRegularClassResolveTarget.designation)
        }
    }

    abstract fun visitMembers(visitor: LLFirResolveTargetVisitor, firRegularClass: FirRegularClass)

    final override fun visitTargetElement(
        element: FirElementWithResolveState,
        visitor: LLFirResolveTargetVisitor,
    ) {
        visitor.performAction(element)
        visitor.withRegularClass(element as FirRegularClass) {
            visitMembers(visitor, element)
        }
    }
}
