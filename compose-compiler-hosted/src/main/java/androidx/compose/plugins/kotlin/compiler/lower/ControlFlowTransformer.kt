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
import org.jetbrains.kotlin.ir.declarations.IrTypeAlias
import org.jetbrains.kotlin.ir.declarations.IrTypeParameter
import org.jetbrains.kotlin.ir.declarations.IrValueDeclaration
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.declarations.impl.IrVariableImpl
import org.jetbrains.kotlin.ir.descriptors.IrTemporaryVariableDescriptorImpl
import org.jetbrains.kotlin.ir.expressions.IrBlock
import org.jetbrains.kotlin.ir.expressions.IrBreakContinue
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrConstKind
import org.jetbrains.kotlin.ir.expressions.IrDoWhileLoop
import org.jetbrains.kotlin.ir.expressions.IrElseBranch
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrLoop
import org.jetbrains.kotlin.ir.expressions.IrReturn
import org.jetbrains.kotlin.ir.expressions.IrWhen
import org.jetbrains.kotlin.ir.expressions.IrWhileLoop
import org.jetbrains.kotlin.ir.expressions.impl.IrBlockBodyImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrBlockImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrBranchImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrElseBranchImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrLoopBase
import org.jetbrains.kotlin.ir.expressions.impl.IrReturnImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrWhenImpl
import org.jetbrains.kotlin.ir.symbols.IrReturnTargetSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.isUnitOrNullableUnit
import org.jetbrains.kotlin.ir.types.toKotlinType
import org.jetbrains.kotlin.ir.util.DeepCopySymbolRemapper
import org.jetbrains.kotlin.ir.util.statements
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi2ir.findFirstFunction
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.utils.addToStdlib.lastIsInstanceOrNull

/**
 * This IR Transform will insert groups inside of the bodies of Composable functions depending on
 * the control-flow structures that exist inside of them.
 *
 * There are 4 types of groups in Compose:
 *
 * 1. Replaceable Groups
 * 2. Movable Groups
 * 3. Restart Groups
 *
 * Generally speaking, every composable function should emit a single group when it executes.
 * Every group can have any number of children groups. Additionally, we analyze each executable
 * block and apply the following rules:
 *
 * 1. If a block executes exactly 1 time always, no groups are needed
 * 2. If a set of blocks are such that exactly one of them is executed exactly once (for example,
 * the result blocks of a when clause), then we insert a replaceable group around each block.
 * 3. A movable group is only needed if the immediate composable call in the group has a Pivotal
 * property.
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

    private val scopeStack = mutableListOf<Scope>()

    private val currentFunctionScope
        get() = scopeStack.lastIsInstanceOrNull<Scope.FunctionScope>()
            ?: error("Expected a FunctionScope but none exist")

    override fun visitClass(declaration: IrClass): IrStatement {
        val scope = Scope.ClassScope()
        try {
            scopeStack.push(scope)
            return super.visitDeclaration(declaration)
        } finally {
            require(scopeStack.pop() == scope) { "Unbalanced scope stack" }
        }
    }

    override fun visitFunction(declaration: IrFunction): IrStatement {
        val scope = Scope.FunctionScope(declaration)
        try {
            scopeStack.push(scope)
            return visitFunctionInScope(declaration)
        } finally {
            val popped = scopeStack.pop()
            require(popped == scope) { "Unbalanced scope stack" }
        }
    }

    private fun visitFunctionInScope(declaration: IrFunction): IrStatement {
        val scope = currentFunctionScope
        if (!scope.isComposable()) return super.visitFunction(declaration)
        declaration.transformChildren()

        val body = declaration.body ?: return declaration

        var transformed: IrExpression = IrBlockImpl(
            body.startOffset,
            body.endOffset,
            declaration.returnType,
            null,
            body.statements
        )

        // right now this is a replaceable group, but we will turn it into a "restart group" in the
        // future. It is important for us to simulate a group here at the moment though as many
        // of the invariants this transform rely on is the fact that every composable function body
        // results in one group
        val start = irStartReplaceableGroup(declaration.irSourceKey())
        val end = { irEndReplaceableGroup() }

        transformed = when {
            transformed.endsWithReturnOrJump() -> {
                scope.pushEnd(end)
                prependSimple(start, transformed)
            }
            else -> {
                scope.pushEnd(end)
                wrapSimple(start, transformed, end())
            }
        }

        declaration.body = IrBlockBodyImpl(
            body.startOffset,
            body.endOffset,
            when (transformed) {
                is IrBlock -> transformed.statements
                else -> listOf(transformed)
            }
        )

        return declaration
    }

    private fun IrExpression.endsWithReturnOrJump(): Boolean {
        var expr: IrStatement? = this
        while (expr != null) {
            if (expr is IrReturn) return true
            if (expr is IrBreakContinue) return true
            if (expr !is IrBlock) return false
            expr = expr.statements.lastOrNull()
        }
        return false
    }

    override fun visitDeclaration(declaration: IrDeclaration): IrStatement {
        when (declaration) {
            is IrFunction,
            is IrClass -> {
                // these declarations get scopes, but they are handled individually
                return super.visitDeclaration(declaration)
            }
            is IrField,
            is IrProperty,
            is IrTypeAlias,
            is IrEnumEntry,
            is IrAnonymousInitializer,
            is IrTypeParameter,
            is IrValueDeclaration -> {
                // these declarations do not create new "scopes", so we do nothing
                return super.visitDeclaration(declaration)
            }
            else -> error("Unhandled declaration! ${declaration::class.java.simpleName}")
        }
    }

    private fun nearestComposer(): IrValueParameter {
        return currentFunctionScope.composerParameter()
            ?: error("Not in a composable function")
    }

    private fun irCurrentComposer(): IrExpression {
        return IrGetValueImpl(
            UNDEFINED_OFFSET,
            UNDEFINED_OFFSET,
            nearestComposer().symbol
        )
    }

    private fun IrElement.sourceKey(): Int {
        var hash = currentFunctionScope
            .function
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
        val scope = currentFunctionScope
        val tempVarDescriptor = IrTemporaryVariableDescriptorImpl(
            scope.function.symbol.descriptor,
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
        end: () -> IrExpression,
        scope: Scope.BlockScope
    ): IrExpression {
        return when {
            // if the scope has no composable calls, then the only important thing is that a
            // start/end call gets executed. as a result, we can just put them both at the top of
            // the group, and we don't have to deal with any of the complicated jump logic that
            // could be inside of the block
            !scope.hasComposableCalls && !scope.hasReturn && !scope.hasJump -> {
                IrBlockImpl(
                    wrapped.startOffset,
                    wrapped.endOffset,
                    wrapped.type,
                    null,
                    listOf(
                        start,
                        end(),
                        wrapped
                    )
                )
            }
            // if the scope ends with a return call, then it will get properly ended if we
            // just push the end call on the scope because of the way returns get transformed in
            // this class. As a result, here we can safely just "prepend" the start call
            wrapped.endsWithReturnOrJump() -> {
                scope.pushEnd(end)
                prependSimple(start, wrapped)
            }
            // otherwise, we want to push an end call for any early returns/jumps, but also add
            // an end call to the end of the group
            else -> {
                scope.pushEnd(end)
                wrapSimple(start, wrapped, end())
            }
        }
    }

    private fun prependSimple(
        start: IrExpression,
        wrapped: IrExpression
    ): IrExpression {
        return IrBlockImpl(
            wrapped.startOffset,
            wrapped.endOffset,
            wrapped.type,
            null,
            listOf(
                start,
                wrapped
            )
        )
    }

    private fun wrapSimple(
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

    private fun IrExpression.asReplaceableGroup(scope: Scope.BlockScope) = createGroup(
        irStartReplaceableGroup(irSourceKey()),
        this,
        ::irEndReplaceableGroup,
        scope
    )

    // TODO: joined key
    @Suppress("unused")
    private fun IrExpression.asMovableGroup(scope: Scope.BlockScope) = createGroup(
        irStartMovableGroup(irSourceKey()),
        this,
        ::irEndMovableGroup,
        scope
    )

    private fun encounteredComposableCall() {
        loop@ for (scope in scopeStack.asReversed()) {
            when (scope) {
                is Scope.FunctionScope -> {
                    scope.markComposableCall()
                    break@loop
                }
                is Scope.BlockScope -> {
                    scope.markComposableCall()
                }
                is Scope.ClassScope -> {
                    break@loop
                }
            }
        }
    }

    private fun encounteredReturn(
        symbol: IrReturnTargetSymbol,
        pushEndCall: (IrExpression) -> Unit
    ) {
        var isEarly = false
        loop@ for (scope in scopeStack.asReversed()) {
            when (scope) {
                is Scope.FunctionScope -> {
                    scope.markReturn(pushEndCall)
                    if (isEarly) {
                        scope.hasEarlyReturn = true
                    }
                    if (scope.function == symbol.owner) {
                        break@loop
                    } else {
                        TODO("Need to handle nested returns!")
                    }
                }
                is Scope.BlockScope -> {
                    isEarly = true
                    scope.markReturn(pushEndCall)
                }
            }
        }
    }

    private fun encounteredContinue(jump: IrBreakContinue, pushEndCall: (IrExpression) -> Unit) {
        loop@ for (scope in scopeStack.asReversed()) {
            when (scope) {
                is Scope.FunctionScope -> error("Unexpected Function Scope encountered")
                is Scope.ClassScope -> error("Unexpected Function Scope encountered")
                is Scope.LoopScope -> {
                    scope.markJump(pushEndCall)
                    if (jump.loop == scope.loop) {
                        break@loop
                    }
                }
                is Scope.BlockScope -> {
                    scope.markJump(pushEndCall)
                }
            }
        }
    }

    private fun <T : Scope> IrExpression.transformWithScope(scope: T): Pair<T, IrExpression> {
        try {
            scopeStack.push(scope)
            val result = transform(this@ControlFlowTransformer, null)
            return scope to result
        } finally {
            require(scopeStack.pop() === scope)
        }
    }

    private inline fun <T : Scope> withScope(scope: T, block: () -> Unit): T {
        scopeStack.push(scope)
        try {
            block()
        } finally {
            require(scopeStack.pop() === scope)
        }
        return scope
    }

    override fun visitCall(expression: IrCall): IrExpression {
        if (expression.isTransformedComposableCall() || expression.isSyntheticComposableCall()) {
            encounteredComposableCall()
        }
        return super.visitCall(expression)
    }

    override fun visitReturn(expression: IrReturn): IrExpression {
        if (!currentFunctionScope.isComposable()) return super.visitReturn(expression)
        val endBlock = IrBlockImpl(
            UNDEFINED_OFFSET,
            UNDEFINED_OFFSET,
            context.irBuiltIns.unitType
        )
        encounteredReturn(expression.returnTargetSymbol) { endBlock.statements.add(0, it) }
        val returnScope = withScope(Scope.BranchScope()) { expression.transformChildren() }
        return IrBlockImpl(
            UNDEFINED_OFFSET,
            UNDEFINED_OFFSET,
            expression.type,
            null,
            if (returnScope.hasComposableCalls) {
                // If there is a composable call in the return expression, we want to make sure
                // the call itself gets called before the end expressions do. To ensure this, we
                // move the result of the call into a temporary variable and then change the
                // return value to just be the temporary.
                // ie, `return ComposableCall()` -> `val tmp = ComposableCall(); end; return tmp;`
                val temp = irTemporary(expression.value, nameHint = "return")
                listOf<IrStatement>(
                    temp,
                    endBlock,
                    IrReturnImpl(
                        expression.startOffset,
                        expression.endOffset,
                        expression.type,
                        expression.returnTargetSymbol,
                        IrGetValueImpl(
                            UNDEFINED_OFFSET,
                            UNDEFINED_OFFSET,
                            temp.symbol
                        )
                    )
                )
            } else {
                listOf(
                    endBlock,
                    expression
                )
            }
        )
    }

    override fun visitBreakContinue(jump: IrBreakContinue): IrExpression {
        if (!currentFunctionScope.isComposable()) return super.visitBreakContinue(jump)
        val endBlock = IrBlockImpl(
            UNDEFINED_OFFSET,
            UNDEFINED_OFFSET,
            context.irBuiltIns.unitType
        )
        encounteredContinue(jump) { endBlock.statements.add(0, it) }
        return IrBlockImpl(
            UNDEFINED_OFFSET,
            UNDEFINED_OFFSET,
            jump.type,
            null,
            listOf(
                endBlock,
                jump
            )
        )
    }

    override fun visitDoWhileLoop(loop: IrDoWhileLoop): IrExpression {
        if (!currentFunctionScope.isComposable()) return super.visitDoWhileLoop(loop)
        return handleLoop(loop as IrLoopBase)
    }

    override fun visitWhileLoop(loop: IrWhileLoop): IrExpression {
        if (!currentFunctionScope.isComposable()) return super.visitWhileLoop(loop)
        return handleLoop(loop as IrLoopBase)
    }

    private fun handleLoop(loop: IrLoopBase): IrExpression {
        withScope(Scope.LoopScope(loop)) {
            val (condScope, condition) = loop.condition.transformWithScope(Scope.BranchScope())
            val (bodyScope, body) = loop.body?.transformWithScope(Scope.BranchScope())
                ?: null to null
            loop.condition = condition
            loop.body = body
            if (!condScope.hasComposableCalls && bodyScope?.hasComposableCalls != true) {
                return loop
            }
            if (condScope.hasComposableCalls) {
                loop.condition = condition.asReplaceableGroup(condScope)
            }
            if (bodyScope?.hasComposableCalls == true) {
                loop.body = body?.asReplaceableGroup(bodyScope)
            }
        }
        return loop
    }

    override fun visitWhen(expression: IrWhen): IrExpression {
        if (!currentFunctionScope.isComposable()) return super.visitWhen(expression)

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
        val resultScopes = mutableListOf<Scope.BranchScope>()
        val condScopes = mutableListOf<Scope.BranchScope>()
        withScope(Scope.WhenScope()) {
            expression.branches.forEachIndexed { index, it ->
                if (it is IrElseBranch) {
                    hasElseBranch = true
                    val (resultScope, result) = it.result.transformWithScope(Scope.BranchScope())

                    condScopes.add(Scope.BranchScope())
                    resultScopes.add(resultScope)

                    resultsNeedGroups = resultsNeedGroups || resultScope.hasComposableCalls
                    transformed.branches.add(
                        IrElseBranchImpl(
                            it.startOffset,
                            it.endOffset,
                            it.condition,
                            result
                        )
                    )
                } else {
                    val (condScope, condition) = it
                        .condition
                        .transformWithScope(Scope.BranchScope())
                    val (resultScope, result) = it
                        .result
                        .transformWithScope(Scope.BranchScope())

                    condScopes.add(condScope)
                    resultScopes.add(resultScope)

                    // the first condition is always executed so if it has a composable call in it, it
                    // doesn't necessitate a group
                    condsNeedGroups =
                        condsNeedGroups || (index != 0 && condScope.hasComposableCalls)
                    resultsNeedGroups = resultsNeedGroups || resultScope.hasComposableCalls
                    transformed.branches.add(
                        IrBranchImpl(
                            it.startOffset,
                            it.endOffset,
                            condition,
                            result
                        )
                    )
                }
            }
        }

        // If we are putting groups around the result branches, we need to guarantee that exactly
        // one result branch is executed. We do this by adding an else branch if it there is not
        // one already.
        // NOTE: we might also be able to assume that the when is exhaustive if it has a non-unit
        // resulting type, since the type system should enforce that.
        if (!hasElseBranch && resultsNeedGroups) {
            condScopes.add(Scope.BranchScope())
            resultScopes.add(Scope.BranchScope())
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

        zip(transformed.branches, condScopes, resultScopes) { it, condScope, resultScope ->
            if (condsNeedGroups) {
                it.condition = it.condition.asReplaceableGroup(condScope)
            }
            if (resultsNeedGroups) {
                it.result = it.result.asReplaceableGroup(resultScope)
            }
        }

        return transformed
    }

    sealed class Scope {
        class FunctionScope(val function: IrFunction) : BlockScope() {
            private var lastTemporaryIndex: Int = 0

            private fun nextTemporaryIndex(): Int = lastTemporaryIndex++

            fun isComposable(): Boolean {
                return composerParameter() != null
            }

            var hasEarlyReturn = false

            fun composerParameter(): IrValueParameter? {
                val param = function.valueParameters.lastOrNull()
                if (param != null && param.isComposerParam()) {
                    return param
                }
                return null
            }

            fun getNameForTemporary(nameHint: String?): String {
                val index = nextTemporaryIndex()
                return if (nameHint != null) "tmp${index}_$nameHint" else "tmp$index"
            }
        }

        abstract class BlockScope : Scope() {
            private val endCallHandlers = mutableListOf<(IrExpression) -> Unit>()

            fun pushEnd(makeEnd: () -> IrExpression) {
                endCallHandlers.forEach {
                    it(makeEnd())
                }
            }

            fun markComposableCall() {
                hasComposableCalls = true
            }

            fun markReturn(endCallHandler: (IrExpression) -> Unit) {
                endCallHandlers.push(endCallHandler)
                hasReturn = true
            }

            fun markJump(endCallHandler: (IrExpression) -> Unit) {
                hasJump = true
                endCallHandlers.push(endCallHandler)
            }

            var hasComposableCalls = false
                private set
            var hasReturn = false
                private set
            var hasJump = false
                private set
        }

        class ClassScope : Scope()
        class LoopScope(val loop: IrLoop) : BlockScope()
        class WhenScope : BlockScope()
        class BranchScope : BlockScope()
    }
}

inline fun <A, B, C> zip(a: List<A>, b: List<B>, c: List<C>, fn: (A, B, C) -> Unit) {
    for (i in a.indices) {
        fn(a[i], b[i], c[i])
    }
}