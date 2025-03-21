/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.util

import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.name.FqName

class CollectNotVisibleFromOriginalModuleIrVisitor(
    private val notVisible: MutableList<IrSimpleFunction>,
    private val originalModule: IrModuleFragment,
) : IrVisitorVoid() {
    override fun visitElement(element: IrElement) {
        element.acceptChildren(this, null)
    }

    // TODO: Not just calls
    override fun visitCall(expression: IrCall) {
        val functionDeclaration = expression.symbol.owner
        val module = functionDeclaration.fileOrNull?.module ?: return
        if (originalModule == module && !isVisible(functionDeclaration)) {
            notVisible.add(functionDeclaration)
        }
        super.visitCall(expression)
    }

    private fun isVisible(functionDeclaration: IrSimpleFunction): Boolean {
        return functionDeclaration.fqNameWhenAvailable in FQ_NAMES_EXCLUDED_FROM_VISIBILITY_CHECKS ||
                functionDeclaration.visibility == DescriptorVisibilities.PUBLIC ||
                (functionDeclaration.visibility == DescriptorVisibilities.INTERNAL &&
                        functionDeclaration.annotations.hasAnnotation(publishedApiAnnotation))
    }

    private val publishedApiAnnotation = FqName("kotlin.PublishedApi")

    // TODO: Discuss relation between KT-70295 and KT-69947
    private val FQ_NAMES_EXCLUDED_FROM_VISIBILITY_CHECKS: Set<FqName> = listOf(
        "kotlin.js.sharedBoxCreate",
        "kotlin.js.sharedBoxWrite",
        "kotlin.js.sharedBoxRead",
        "kotlin.wasm.internal.ClosureBoxBoolean",
        "kotlin.wasm.internal.ClosureBoxByte",
        "kotlin.wasm.internal.ClosureBoxShort",
        "kotlin.wasm.internal.ClosureBoxChar",
        "kotlin.wasm.internal.ClosureBoxInt",
        "kotlin.wasm.internal.ClosureBoxLong",
        "kotlin.wasm.internal.ClosureBoxFloat",
        "kotlin.wasm.internal.ClosureBoxDouble",
        "kotlin.wasm.internal.ClosureBoxAny",
        "kotlin.wasm.internal.wasmTypeId",
        "kotlin.coroutines.CoroutineImpl",
        "kotlin.native.internal.KClassImpl",
        "kotlin.native.internal.KTypeImpl",
        "kotlin.native.internal.KTypeProjectionList",
        "kotlin.native.internal.KTypeParameterImpl",
    ).mapTo(hashSetOf(), ::FqName)
}