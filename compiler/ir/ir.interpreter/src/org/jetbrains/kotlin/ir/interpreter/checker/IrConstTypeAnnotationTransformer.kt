/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.interpreter.checker

import org.jetbrains.kotlin.constant.EvaluatedConstTracker
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.interpreter.IrInterpreter
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.visitors.IrTypeTransformerVoid

internal class IrConstTypeAnnotationTransformer(
    interpreter: IrInterpreter,
    irFile: IrFile,
    mode: EvaluationMode,
    evaluatedConstTracker: EvaluatedConstTracker?,
    onWarning: (IrFile, IrElement, IrErrorExpression) -> Unit,
    onError: (IrFile, IrElement, IrErrorExpression) -> Unit,
    suppressExceptions: Boolean,
) : IrConstAnnotationTransformer(interpreter, irFile, mode, evaluatedConstTracker, onWarning, onError, suppressExceptions),
    IrTypeTransformerVoid<Nothing?> {

    override fun <Type : IrType?> transformType(container: IrElement, type: Type, data: Nothing?): Type {
        if (type == null) return type

        transformAnnotations(type)
        if (type is IrSimpleType) {
            type.arguments.mapNotNull { it.typeOrNull }.forEach { transformType(container, it, data) }
        }
        return type
    }
}
