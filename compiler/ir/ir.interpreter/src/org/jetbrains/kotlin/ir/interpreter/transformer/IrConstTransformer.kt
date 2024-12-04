/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.interpreter.transformer

import org.jetbrains.kotlin.constant.EvaluatedConstTracker
import org.jetbrains.kotlin.incremental.components.InlineConstTracker
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.path
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrErrorExpression
import org.jetbrains.kotlin.ir.interpreter.IrInterpreter
import org.jetbrains.kotlin.ir.interpreter.checker.EvaluationMode
import org.jetbrains.kotlin.ir.interpreter.checker.IrInterpreterCommonChecker
import org.jetbrains.kotlin.ir.interpreter.preprocessor.IrInterpreterConstGetterPreprocessor
import org.jetbrains.kotlin.ir.interpreter.preprocessor.IrInterpreterKCallableNamePreprocessor
import org.jetbrains.kotlin.ir.interpreter.preprocessor.IrInterpreterPreprocessorData
import org.jetbrains.kotlin.ir.util.classId
import org.jetbrains.kotlin.ir.util.parentAsClass

fun IrElement.transformConst(
    irFile: IrFile,
    interpreter: IrInterpreter,
    mode: EvaluationMode,
    evaluatedConstTracker: EvaluatedConstTracker? = null,
    inlineConstTracker: InlineConstTracker? = null,
    onWarning: (IrFile, IrElement, IrErrorExpression) -> Unit = { _, _, _ -> },
    onError: (IrFile, IrElement, IrErrorExpression) -> Unit = { _, _, _ -> },
    suppressExceptions: Boolean = false,
): IrElement {
    val checker = IrInterpreterCommonChecker()

    val constEvaluationContext = IrConstEvaluationContext(
        interpreter,
        irFile,
        mode,
        checker,
        evaluatedConstTracker,
        inlineConstTracker,
        onWarning,
        onError,
        suppressExceptions,
    )

    val irConstExpressionTransformer = IrConstOnlyNecessaryTransformer(constEvaluationContext)
    val irConstDeclarationAnnotationTransformer = IrConstDeclarationAnnotationTransformer(constEvaluationContext)
    val irConstTypeAnnotationTransformer = IrConstTypeAnnotationTransformer(constEvaluationContext)

    return this.transform(irConstExpressionTransformer, IrConstExpressionTransformer.Data()).apply {
        irConstDeclarationAnnotationTransformer.visitAnnotations(this)
        irConstTypeAnnotationTransformer.visitAnnotations(this)
    }
}

fun IrFile.runConstOptimizations(
    interpreter: IrInterpreter,
    mode: EvaluationMode,
    evaluatedConstTracker: EvaluatedConstTracker? = null,
    inlineConstTracker: InlineConstTracker? = null,
    suppressExceptions: Boolean = false,
) {
    val preprocessedFile = this.preprocessForConstTransformer(interpreter, mode)

    val checker = IrInterpreterCommonChecker()
    val irConstExpressionTransformer = IrConstAllTransformer(
        IrConstEvaluationContext(
            interpreter, preprocessedFile, mode, checker, evaluatedConstTracker, inlineConstTracker,
            { _, _, _ -> }, { _, _, _ -> },
            suppressExceptions
        )
    )
    preprocessedFile.transform(irConstExpressionTransformer, IrConstExpressionTransformer.Data())
}

private fun IrFile.preprocessForConstTransformer(
    interpreter: IrInterpreter,
    mode: EvaluationMode,
): IrFile {
    val preprocessors = setOf(IrInterpreterKCallableNamePreprocessor(), IrInterpreterConstGetterPreprocessor())
    val preprocessedFile = preprocessors.fold(this) { file, preprocessor ->
        preprocessor.preprocess(file, IrInterpreterPreprocessorData(mode, interpreter.irBuiltIns))
    }
    return preprocessedFile
}

fun InlineConstTracker.reportOnIr(irFile: IrFile, field: IrField, value: IrConst) {
    if (field.origin != IrDeclarationOrigin.IR_EXTERNAL_JAVA_DECLARATION_STUB) return

    val path = irFile.path
    val owner = field.parentAsClass.classId?.asString()?.replace(".", "$")?.replace("/", ".") ?: return
    val name = field.name.asString()
    val constType = value.kind.asString

    report(path, owner, name, constType)
}
