/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.ir.annotations

import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrAnnotation
import org.jetbrains.kotlin.ir.util.getConstArgument
import org.jetbrains.kotlin.ir.util.isAnnotation
import org.jetbrains.kotlin.name.NativeRuntimeNames

/**
 * The non-empty value of the `nonVirtualTargetMethod` argument of the `@ExportedBridge` annotation on
 * this function, or `null` if the function is not an exported bridge or does not request non-virtual
 * dispatch. See [kotlin.native.internal.ExportedBridge] and `ExportedBridgeNonVirtualLowering`.
 */
val IrSimpleFunction.exportedBridgeNonVirtualTargetMethod: String?
    get() = annotations
        .firstOrNull { it.isExportedBridge() }
        ?.getConstArgument<String>("nonVirtualTargetMethod")
        ?.takeIf { it.isNotEmpty() }

private fun IrAnnotation.isExportedBridge(): Boolean =
    isAnnotation(NativeRuntimeNames.Annotations.exportedBridgeClassId.asSingleFqName())
