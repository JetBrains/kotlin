/*
 * Copyright 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.plugins.kotlin.compiler.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.peek
import org.jetbrains.kotlin.backend.common.pop
import org.jetbrains.kotlin.backend.common.push
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.IrAnonymousInitializer
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrEnumEntry
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrSymbolOwner
import org.jetbrains.kotlin.ir.declarations.IrTypeAlias
import org.jetbrains.kotlin.ir.declarations.IrTypeParameter
import org.jetbrains.kotlin.ir.declarations.IrValueDeclaration
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.declarations.impl.IrVariableImpl
import org.jetbrains.kotlin.ir.descriptors.IrTemporaryVariableDescriptorImpl
import org.jetbrains.kotlin.ir.expressions.IrBreak
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrConstKind
import org.jetbrains.kotlin.ir.expressions.IrContinue
import org.jetbrains.kotlin.ir.expressions.IrDoWhileLoop
import org.jetbrains.kotlin.ir.expressions.IrElseBranch
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrLoop
import org.jetbrains.kotlin.ir.expressions.IrReturn
import org.jetbrains.kotlin.ir.expressions.IrWhen
import org.jetbrains.kotlin.ir.expressions.IrWhileLoop
import org.jetbrains.kotlin.ir.expressions.impl.IrBlockImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrBranchImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrDoWhileLoopImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrElseBranchImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrLoopBase
import org.jetbrains.kotlin.ir.expressions.impl.IrWhenImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrWhileLoopImpl
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.isUnitOrNullableUnit
import org.jetbrains.kotlin.ir.types.toKotlinType
import org.jetbrains.kotlin.ir.util.DeepCopySymbolRemapper
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi2ir.findFirstFunction
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

/**
 * This IR Transform will insert groups inside of the bodies of Composable functions depending on
 * the control-flow structures that exist inside of them.
 *
 * There are 4 types of groups in Compose:
 *
 * 1. Restart Groups
 * 2. Container Groups
 * 3. Replaceable Groups
 * 4. Movable Groups
 *
 * Generally speaking, every composable function should emit a single group when it executes.
 * Additionally, we analyze each executable block and apply the following rules:
 *
 * 1. If a block executes exactly 1 time always, no groups are needed
 * 2. If a set of blocks are such that exactly one of them is executed exactly once (for example,
 * the result blocks of a when clause), then we insert a replaceable group around each block.
 * 3. If a set of blocks executes a variable or unknown number of times, a Container group must
 * be placed around the set of blocks, and each block must be wrapped with a replaceable or
 * movable group (most commonly used for looping control flow structures).
 * 4. A movable group is only needed if the immediate composable call in the group has a Pivotal
 * property.
 * 5. Replaceable and Movable groups have a statically constant number of groups beneath them
 * (for each source key).
 * 6. Container groups can only have replaceable or moveable groups as children.
 * 7. Moveable groups are only valid as children of Container groups.
 */
class ControlFlowTransformer(
    context: JvmBackendContext,
    symbolRemapper: DeepCopySymbolRemapper,
    bindingTrace: BindingTrace
) :
    AbstractComposeLowering(context, symbolRemapper, bindingTrace),
    FileLoweringPass,
    ModuleLoweringPass {

    override fun lower(module: IrModuleFragment) {
        module.transformChildrenVoid(this)
    }

    override fun lower(irFile: IrFile) {
        irFile.transformChildrenVoid(this)
    }

    private val startContainerDescriptor = composerTypeDescriptor
        .unsubstitutedMemberScope
        .findFirstFunction("startContainerGroup") { it.valueParameters.size == 0 }

    private val endContainerDescriptor = composerTypeDescriptor
        .unsubstitutedMemberScope
        .findFirstFunction("endContainerGroup") { it.valueParameters.size == 0 }

    private val startReplaceableDescriptor = composerTypeDescriptor
        .unsubstitutedMemberScope
        .findFirstFunction("startReplaceableGroup") { it.valueParameters.size == 1 }

    private val endReplaceableDescriptor = composerTypeDescriptor
        .unsubstitutedMemberScope
        .findFirstFunction("endReplaceableGroup") { it.valueParameters.size == 0 }

    private val startMovableDescriptor = composerTypeDescriptor
        .unsubstitutedMemberScope
        .findFirstFunction("startMovableGroup") { it.valueParameters.size == 1 }

    private val endMovableDescriptor = composerTypeDescriptor
        .unsubstitutedMemberScope
        .findFirstFunction("endMovableGroup") { it.valueParameters.size == 0 }

    private val scopeStack = mutableListOf<ComposableScope>()
    private val currentScope get() = scopeStack.peek() ?: error("Expected a scope but none exist")

    override fun visitDeclaration(declaration: IrDeclaration): IrStatement {
        when (declaration) {
            is IrFunction,
            is IrClass -> {
                try {
                    scopeStack.push(ComposableScope(declaration as IrSymbolOwner))
                    return super.visitDeclaration(declaration)
                } finally {
                    scopeStack.pop()
                }
            }
            is IrField,
            is IrProperty,
            is IrTypeAlias,
            is IrEnumEntry,
            is IrAnonymousInitializer,
            is IrTypeParameter,
            is IrValueDeclaration -> {
                return super.visitDeclaration(declaration)
            }
            else -> error("Unhandled declaration! ${declaration::class.java.simpleName}")
        }
    }

    private fun nearestComposer(): IrValueParameter {
        for (scope in scopeStack.asReversed()) {
            scope.composerParameter()?.let { return it }
        }
        error("Couldn't find composer parameter")
    }

    private fun irCurrentComposer(): IrExpression {
        return IrGetValueImpl(
            UNDEFINED_OFFSET,
            UNDEFINED_OFFSET,
            nearestComposer().symbol
        )
    }

    private fun IrElement.sourceKey(): Int {
        var hash = currentScope
            .symbolOwner
            .symbol
            .descriptor
            .fqNameSafe
            .toString()
            .hashCode()
        hash = 31 * hash + startOffset
        return hash
    }

    private fun IrElement.irSourceKey(): IrConst<Int> {
        return IrConstImpl(
            UNDEFINED_OFFSET,
            UNDEFINED_OFFSET,
            context.irBuiltIns.intType,
            IrConstKind.Int,
            sourceKey()
        )
    }

    private fun irStartReplaceableGroup(key: IrExpression): IrExpression {
        return irMethodCall(irCurrentComposer(), startReplaceableDescriptor).also {
            it.putValueArgument(0, key)
        }
    }

    private fun irEndReplaceableGroup(): IrExpression {
        return irMethodCall(irCurrentComposer(), endReplaceableDescriptor)
    }

    private fun irStartMovableGroup(key: IrExpression): IrExpression {
        return irMethodCall(irCurrentComposer(), startMovableDescriptor).also {
            it.putValueArgument(0, key)
        }
    }

    private fun irEndMovableGroup(): IrExpression {
        return irMethodCall(irCurrentComposer(), endMovableDescriptor)
    }

    private fun irStartContainerGroup(): IrExpression {
        return irMethodCall(irCurrentComposer(), startContainerDescriptor)
    }

    private fun irEndContainerGroup(): IrExpression {
        return irMethodCall(irCurrentComposer(), endContainerDescriptor)
    }

    private fun irCall(descriptor: FunctionDescriptor): IrCall {
        val type = descriptor.returnType?.toIrType() ?: error("Expected a return type")
        val symbol = referenceFunction(descriptor)
        return IrCallImpl(
            UNDEFINED_OFFSET,
            UNDEFINED_OFFSET,
            type,
            symbol,
            descriptor
        )
    }

    private fun irMethodCall(target: IrExpression, descriptor: FunctionDescriptor): IrCall {
        return irCall(descriptor).apply {
            dispatchReceiver = target
        }
    }

    private fun irTemporary(
        value: IrExpression,
        nameHint: String? = null,
        irType: IrType = value.type,
        isVar: Boolean = false
    ): IrVariableImpl {
        val scope = currentScope
        val tempVarDescriptor = IrTemporaryVariableDescriptorImpl(
            scope.symbolOwner.symbol.descriptor,
            Name.identifier(scope.getNameForTemporary(nameHint)),
            irType.toKotlinType(),
            isVar
        )
        return IrVariableImpl(
            value.startOffset,
            value.endOffset,
            IrDeclarationOrigin.IR_TEMPORARY_VARIABLE,
            tempVarDescriptor,
            irType,
            value
        )
    }

    private fun createGroup(
        start: IrExpression,
        wrapped: IrExpression,
        end: IrExpression
    ): IrExpression {
        if (wrapped.type.isUnitOrNullableUnit()) {
            return IrBlockImpl(
                wrapped.startOffset,
                wrapped.endOffset,
                wrapped.type,
                null,
                listOf(
                    start,
                    wrapped,
                    end
                )
            )
        } else {
            // create temporary for result
            val tempVar = irTemporary(wrapped, nameHint = "group")

            return IrBlockImpl(
                wrapped.startOffset,
                wrapped.endOffset,
                wrapped.type,
                null,
                listOf(
                    start,
                    tempVar,
                    end,
                    IrGetValueImpl(
                        UNDEFINED_OFFSET,
                        UNDEFINED_OFFSET,
                        tempVar.symbol
                    )
                )
            )
        }
    }

    private fun IrExpression.asReplaceableGroup() = createGroup(
        irStartReplaceableGroup(irSourceKey()),
        this,
        irEndReplaceableGroup()
    )

    // TODO: joined key
    @Suppress("unused")
    private fun IrExpression.asMovableGroup() = createGroup(
        irStartMovableGroup(irSourceKey()),
        this,
        irEndMovableGroup()
    )

    private fun IrExpression.asContainerGroup() = createGroup(
        irStartContainerGroup(),
        this,
        irEndContainerGroup()
    )

    private val contextStack = mutableListOf(GroupContext())

    private fun encounteredComposableCall() {
        for (context in contextStack.asReversed()) {
            // if we ran into a context that has composable calls, we can stop propagating
            // upwards, since it has already happened
            if (context.hasComposableCalls) break
            context.hasComposableCalls = true
        }
    }

    private fun encounteredReturn() {
        for (context in contextStack.asReversed()) {
            if (context.hasReturn) break
            context.hasReturn = true
        }
    }

    private fun encounteredBreak() {
        for (context in contextStack.asReversed()) {
            if (context.hasBreak) break
            context.hasBreak = true
        }
    }

    private fun encounteredContinue() {
        for (context in contextStack.asReversed()) {
            if (context.hasContinue) break
            context.hasContinue = true
        }
    }

    private fun IrExpression.transformWithContext(): Pair<GroupContext, IrExpression> {
        val context = GroupContext()
        contextStack.push(context)
        val result = transform(this@ControlFlowTransformer, null)
        require(contextStack.pop() === context)
        return context to result
    }

    override fun visitCall(expression: IrCall): IrExpression {
        if (expression.isTransformedComposableCall() || expression.isSyntheticComposableCall()) {
            encounteredComposableCall()
        }
        return super.visitCall(expression)
    }

    override fun visitReturn(expression: IrReturn): IrExpression {
        encounteredReturn()
        return super.visitReturn(expression)
    }

    override fun visitBreak(jump: IrBreak): IrExpression {
        encounteredBreak()
        return super.visitBreak(jump)
    }

    override fun visitContinue(jump: IrContinue): IrExpression {
        encounteredContinue()
        return super.visitContinue(jump)
    }

    override fun visitDoWhileLoop(loop: IrDoWhileLoop): IrExpression {
        if (!currentScope.isComposable()) return super.visitDoWhileLoop(loop)
        return handleLoop(loop, IrDoWhileLoopImpl(
            loop.startOffset,
            loop.endOffset,
            loop.type,
            loop.origin
        ))
    }

    override fun visitWhileLoop(loop: IrWhileLoop): IrExpression {
        if (!currentScope.isComposable()) return super.visitWhileLoop(loop)
        return handleLoop(loop, IrWhileLoopImpl(
            loop.startOffset,
            loop.endOffset,
            loop.type,
            loop.origin
        ))
    }

    private fun handleLoop(loop: IrLoop, transformed: IrLoopBase): IrExpression {
        val (conditionContext, condition) = loop.condition.transformWithContext()
        val (bodyContext, body) = loop.body?.transformWithContext() ?: null to null
        transformed.condition = condition
        transformed.body = body
        if (!conditionContext.hasComposableCalls && bodyContext?.hasComposableCalls != true) {
            return transformed
        }
        // TODO: Handle breaks, continues, and early returns
        if (conditionContext.hasComposableCalls) {
            transformed.condition = condition.asReplaceableGroup()
        }
        if (bodyContext?.hasComposableCalls == true) {
            transformed.body = body?.asReplaceableGroup()
        }
        return transformed.asContainerGroup()
    }

    override fun visitWhen(expression: IrWhen): IrExpression {
        if (!currentScope.isComposable()) return super.visitWhen(expression)

        // Composable calls in conditions are more expensive than composable calls in the different
        // result branches of the when clause. This is because if we have N branches of a when
        // clause, we will always execute exactly 1 result branch, but we will execute 0-N of the
        // conditions. This means that if only the results have composable calls, we can use
        // replaceable groups to represent the entire expression. If a condition has a composable
        // call in it, we need to place the whole expression in a Container group, since a variable
        // number of them will be created. The exception here is the first branch's condition,
        // since it will *always* be executed. As a result, if only the first conditional has a
        // composable call in it, we can avoid creating a group for it since it is not
        // conditionally executed.
        var condsNeedGroups = false
        var resultsNeedGroups = false
        var hasElseBranch = false

        val transformed = IrWhenImpl(
            expression.startOffset,
            expression.endOffset,
            expression.type,
            expression.origin
        )
        expression.branches.forEachIndexed { index, it ->
            if (it is IrElseBranch) {
                hasElseBranch = true
                val (resultContext, result) = it.result.transformWithContext()
                resultsNeedGroups = resultsNeedGroups || resultContext.hasComposableCalls
                transformed.branches.add(IrElseBranchImpl(
                    it.startOffset,
                    it.endOffset,
                    it.condition,
                    result
                ))
            } else {
                val (conditionContext, condition) = it.condition.transformWithContext()
                val (resultContext, result) = it.result.transformWithContext()

                // the first condition is always executed so if it has a composable call in it, it
                // doesn't necessitate a group
                condsNeedGroups = condsNeedGroups || (
                    index != 0 && conditionContext.hasComposableCalls
                )
                resultsNeedGroups = resultsNeedGroups || resultContext.hasComposableCalls
                transformed.branches.add(IrBranchImpl(
                    it.startOffset,
                    it.endOffset,
                    condition,
                    result
                ))
            }
        }

        // If we are putting groups around the result branches, we need to guarantee that exactly
        // one result branch is executed. We do this by adding an else branch if it there is not
        // one already.
        // NOTE: we might also be able to assume that the when is exhaustive if it has a non-unit
        // resulting type, since the type system should enforce that.
        if (!hasElseBranch && resultsNeedGroups) {
            transformed.branches.add(
                IrElseBranchImpl(
                    UNDEFINED_OFFSET,
                    UNDEFINED_OFFSET,
                    condition = IrConstImpl(
                        UNDEFINED_OFFSET,
                        UNDEFINED_OFFSET,
                        context.irBuiltIns.booleanType,
                        IrConstKind.Boolean,
                        true
                    ),
                    result = IrBlockImpl(
                        UNDEFINED_OFFSET,
                        UNDEFINED_OFFSET,
                        context.irBuiltIns.unitType,
                        null,
                        emptyList()
                    )
                )
            )
        }
        // TODO: Handle early returns
        return if (!condsNeedGroups && !resultsNeedGroups) {
            // no need to put any of the branches in groups
            transformed
        } else if (condsNeedGroups) {
            transformed.branches.forEach {
                it.condition = it.condition.asReplaceableGroup()
                if (resultsNeedGroups) {
                    it.result = it.result.asReplaceableGroup()
                }
            }
            transformed.asContainerGroup()
        } else {
            // only results need groups
            assert(resultsNeedGroups)
            transformed.branches.forEach {
                it.result = it.result.asReplaceableGroup()
            }
            transformed
        }
    }

    inner class ComposableScope(
        val symbolOwner: IrSymbolOwner
    ) {
        private var lastTemporaryIndex: Int = 0

        private fun nextTemporaryIndex(): Int = lastTemporaryIndex++

        fun isComposable(): Boolean {
            return composerParameter() != null
        }

        fun composerParameter(): IrValueParameter? {
            if (symbolOwner is IrFunction) {
                val param = symbolOwner.valueParameters.lastOrNull()
                if (param != null && param.isComposerParam()) {
                    return param
                }
            }
            return null
        }

        fun getNameForTemporary(nameHint: String?): String {
            val index = nextTemporaryIndex()
            return if (nameHint != null) "tmp${index}_$nameHint" else "tmp$index"
        }
    }

    // TODO: we might have to combine GroupContext and ComposableScope, since these values should
    //  only be propagated up to the function boundary.
    class GroupContext(
        var hasComposableCalls: Boolean = false,
        var hasReturn: Boolean = false,
        var hasBreak: Boolean = false,
        var hasContinue: Boolean = false
    )
}
