/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common

import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrParameterKind
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.util.getAllSuperclasses
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.name.StandardClassIds

fun IrClass?.isRestrictedSuspension(): Boolean {
    if (this == null) return false
    return hasAnnotation(StandardClassIds.Annotations.RestrictsSuspension) ||
            getAllSuperclasses().any { hasAnnotation(StandardClassIds.Annotations.RestrictsSuspension) }
}

fun IrFunction.isRestrictedSuspensionFunction(): Boolean {
    return parameters.any {
        return it.kind == IrParameterKind.ExtensionReceiver && it.type.classOrNull?.owner.isRestrictedSuspension()
    }
}
