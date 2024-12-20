/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.lower.LocalDeclarationsLowering
import org.jetbrains.kotlin.backend.common.phaser.PhaseDescription
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.codegen.inline.AnonymousObjectTransformationInfo
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrCompositeImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrConstructorCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.fromSymbolOwner
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrTransformer
import org.jetbrains.kotlin.name.NameUtils

/**
 * Drops excess local classes that were copied by IR inliner.
 *
 * There are three types of inlined local classes:
 * 1. MUST BE regenerated according to set of rules in [AnonymousObjectTransformationInfo].
 *    They all have `originalBeforeInline != null`.
 * 2. MUST NOT BE regenerated and MUST BE CREATED only once because they are copied from call site, for example, from lambda.
 *    This lambda will not exist after inline, so we copy declaration into new temporary inline call `singleArgumentInlineFunction`.
 * 3. MUST NOT BE created at all because will be created at callee site.
 *    This lowering drops declarations that correspond to second and third type.
 */
@PhaseDescription(
    name = "RemoveDuplicatedInlinedLocalClasses",
    prerequisite = [/* JvmIrInliner::class, */ JvmLocalDeclarationsLowering::class]
)
internal class RemoveDuplicatedInlinedLocalClassesLowering(private val context: JvmBackendContext) : FileLoweringPass {
    override fun lower(irFile: IrFile) {
        if (context.config.enableIrInliner) {
            irFile.transformChildren(RemoveDuplicatedInlinedLocalClassesTransformer(context), Data())
        }
    }
}

private data class Data(
    var classDeclaredOnCallSiteOrIsDefaultLambda: Boolean = false,
    var insideInlineBlock: Boolean = false,
    var modifyTree: Boolean = true,
)

private class RemoveDuplicatedInlinedLocalClassesTransformer(val context: JvmBackendContext) : IrTransformer<Data>() {
    private val visited = mutableSetOf<IrElement>()
    private val capturedConstructors = context.mapping.capturedConstructors

    private fun removeUselessDeclarationsFromCapturedConstructors(irClass: IrClass, data: Data) {
        irClass.parents.first { it !is IrFunction || it.origin != IrDeclarationOrigin.INLINE_LAMBDA }
            .accept(this, data.copy(classDeclaredOnCallSiteOrIsDefaultLambda = false, modifyTree = false))
    }

    override fun visitCall(expression: IrCall, data: Data): IrElement {
        if (expression.symbol == context.ir.symbols.singleArgumentInlineFunction) return expression
        return super.visitCall(expression, data)
    }

    override fun visitInlinedFunctionBlock(inlinedBlock: IrInlinedFunctionBlock, data: Data): IrExpression {
        val newData = data.copy(insideInlineBlock = true, classDeclaredOnCallSiteOrIsDefaultLambda = true)
        inlinedBlock.transformChildren(this, newData)
        return inlinedBlock
    }

    override fun visitBlock(expression: IrBlock, data: Data): IrExpression {
        if (expression.origin == IrStatementOrigin.INLINE_ARGS_CONTAINER) {
            return super.visitBlock(expression, data.copy(insideInlineBlock = true, classDeclaredOnCallSiteOrIsDefaultLambda = false))
        }

        val anonymousClass = expression.statements.firstOrNull()
        val result = super.visitBlock(expression, data)
        if (anonymousClass is IrClass && result is IrBlock && result.statements.firstOrNull() is IrComposite) {
            reuseConstructorFromOriginalClass(result, anonymousClass, data)
        }
        return result
    }

    private fun reuseConstructorFromOriginalClass(block: IrBlock, anonymousClass: IrClass, data: Data) {
        val lastStatement = block.statements.last()
        val constructorCall = (lastStatement as? IrConstructorCall)
            ?: (lastStatement as IrBlock).statements.last() as IrConstructorCall
        val constructorParent = constructorCall.symbol.owner.parentAsClass

        // It is possible that inlined class will be lowered before original. In that case we must launch `LocalDeclarationsLowering` and
        // lower original declaration to get correct captured constructor.
        val container = anonymousClass.attributeOwnerId.extractRelatedDeclaration()?.parents
            ?.filterIsInstance<IrFunction>()?.firstOrNull()?.takeIf { it.body != null }
        container?.let {
            LocalDeclarationsLowering(
                context, NameUtils::sanitizeAsJavaIdentifier, JvmVisibilityPolicy,
                compatibilityModeForInlinedLocalDelegatedPropertyAccessors = true, forceFieldsForInlineCaptures = true
            ).lowerWithoutActualChange(it.body!!, it)
        }

        // We must remove all constructors from `capturedConstructors` that belong to the classes that will be removed
        capturedConstructors.keys.forEach {
            RemoveDuplicatedInlinedLocalClassesTransformer(context)
                .removeUselessDeclarationsFromCapturedConstructors(it.parentAsClass, data)
        }

        val originalConstructor = capturedConstructors.keys.single {
            it.parentAsClass.attributeOwnerId == constructorParent.attributeOwnerId && it.parentAsClass != constructorParent
        }

        val loweredConstructor = capturedConstructors[originalConstructor]!!
        val newConstructorCall = IrConstructorCallImpl.fromSymbolOwner(
            constructorCall.startOffset, constructorCall.endOffset,
            loweredConstructor.parentAsClass.defaultType, loweredConstructor.symbol, constructorCall.origin
        )
        newConstructorCall.copyTypeAndValueArgumentsFrom(constructorCall)

        if (lastStatement is IrConstructorCall) {
            block.statements[block.statements.lastIndex] = newConstructorCall
        } else if (lastStatement is IrBlock) {
            lastStatement.statements[lastStatement.statements.lastIndex] = newConstructorCall
        }
    }

    // Basically we want to remove all anonymous classes after inline. Exceptions are:
    // 1. classes that must be regenerated (declaration.originalBeforeInline != null)
    // 2. classes that are originally declared on call site or are default lambdas (data.classDeclaredOnCallSiteOrIsDefaultLambda == false)
    override fun visitClass(declaration: IrClass, data: Data): IrStatement {
        if (!data.insideInlineBlock || declaration.originalBeforeInline != null || !data.classDeclaredOnCallSiteOrIsDefaultLambda) {
            return super.visitClass(declaration, data)
        }

        val constructor = declaration.primaryConstructor!!
        capturedConstructors[constructor] = null // this action will do actual `remove`
        capturedConstructors.keys.map { key -> // for each value in map
            capturedConstructors[key]?.let {
                if (it == constructor) {
                    capturedConstructors[key] = null
                }
            }
        }

        return if (data.modifyTree) {
            IrCompositeImpl(declaration.startOffset, declaration.endOffset, context.irBuiltIns.unitType)
        } else {
            super.visitClass(declaration, data)
        }
    }

    override fun visitFunctionReference(expression: IrFunctionReference, data: Data): IrElement {
        if (!visited.add(expression.symbol.owner)) return expression
        expression.symbol.owner.accept(this, data)
        return super.visitFunctionReference(expression, data)
    }
}
