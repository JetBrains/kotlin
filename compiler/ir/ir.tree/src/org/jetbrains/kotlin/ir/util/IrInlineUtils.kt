/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.util

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrFileEntry
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrDeclarationParent
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrPackageFragment
import org.jetbrains.kotlin.ir.declarations.IrParameterKind
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.declarations.lazy.IrLazyDeclarationBase
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.irAttribute

/**
 * There is some compiler info from IR inliner that may not be available in non-JVM backends due to serialization in KLIBs.
 * For example, in the JVM backend it is safe to check the original call of an inlined function, and on other backends it's not.
 * To discourage usages of such APIs in non-JVM backends, this opt-in annotation was introduced.
 */
@RequiresOptIn(
    message = "This API is supposed to be used only inside JVM backend.",
)
annotation class JvmIrInlineExperimental

@JvmIrInlineExperimental
var IrInlinedFunctionBlock.inlineCall: IrFunctionAccessExpression? by irAttribute(followAttributeOwner = true)
@JvmIrInlineExperimental
var IrInlinedFunctionBlock.inlinedElement: IrElement? by irAttribute(followAttributeOwner = true)

@OptIn(JvmIrInlineExperimental::class)
fun IrInlinedFunctionBlock.isFunctionInlining(): Boolean {
    return this.inlinedElement is IrFunction
}

fun IrInlinedFunctionBlock.isLambdaInlining(): Boolean {
    return !isFunctionInlining()
}

val IrContainerExpression.innerInlinedBlockOrThis: IrContainerExpression
    get() = (this as? IrReturnableBlock)?.statements?.singleOrNull() as? IrInlinedFunctionBlock ?: this

fun IrValueParameter.isInlineParameter() =
    kind == IrParameterKind.Regular
            && !isNoinline
            && !type.isNullable()
            && (type.isFunction() || type.isSuspendFunction())
            && parent.isInlineFunction()

private fun IrDeclarationParent.isInlineFunction(): Boolean {
    if (this !is IrFunction) return false
    return this.isInline || this.isInlineArrayConstructor()
}

fun IrExpression.isAdaptedFunctionReference() =
    this is IrBlock && this.origin == IrStatementOrigin.ADAPTED_FUNCTION_REFERENCE

fun IrExpression.isLambdaBlock() =
    this is IrBlock && this.origin == IrStatementOrigin.LAMBDA

/**
 * Lazy IR declarations do not have information about the exact [IrFileEntry] they belong to.
 *
 * However, such information could be useful when inlining a Lazy IR `inline fun` at the first
 * stage of compilation, cause a new [IrInlinedFunctionBlock] with the proper [IrFileEntry]
 * needs to be created.
 *
 * This attribute allows memorizing file entry per top-level Lazy IR declaration.
 */
private var IrLazyDeclarationBase.fileEntryOfTopLevelLazyIrDeclaration: IrFileEntry? by irAttribute(followAttributeOwner = true)

/**
 * Get the [IrFileEntry] from [IrFile].
 *
 * If no IR file is available (i.e., in case of top-level Lazy IR declaration), get the file entry
 * previously cached in [fileEntryOfTopLevelLazyIrDeclaration].
 */
tailrec fun IrDeclaration.computeFileEntry(): IrFileEntry {
    return when (val parent = parent) {
        is IrFile -> parent.fileEntry

        is IrPackageFragment -> {
            // `this` is the top-level declaration not belonging to IrFile.
            // Try to extract the previously cached IrFileEntry (if there is any).
            (this as? IrLazyDeclarationBase)?.fileEntryOfTopLevelLazyIrDeclaration
                ?: error("Can not determine IR file for top-level declaration ${render()}")
        }

        is IrDeclaration -> parent.computeFileEntry()

        else -> error("Unexpected declaration parent: ${parent.render()}")
    }
}

fun IrFunction.setFileEntryIfTopLevelDeclarationIsLazyIr(fileEntry: () -> IrFileEntry) {
    val topLevelDeclaration = parentsWithSelf.last()
    if (topLevelDeclaration is IrLazyDeclarationBase && topLevelDeclaration.fileEntryOfTopLevelLazyIrDeclaration == null) {
        topLevelDeclaration.fileEntryOfTopLevelLazyIrDeclaration = fileEntry()
    }
}
