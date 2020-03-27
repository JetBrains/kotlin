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

import androidx.compose.plugins.kotlin.KtxNameConventions
import androidx.compose.plugins.kotlin.isEmitInline
import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.backend.common.pop
import org.jetbrains.kotlin.backend.common.push
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.AnonymousFunctionDescriptor
import org.jetbrains.kotlin.descriptors.impl.ValueParameterDescriptorImpl
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irReturn
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
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.declarations.impl.IrFunctionImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrVariableImpl
import org.jetbrains.kotlin.ir.descriptors.IrTemporaryVariableDescriptorImpl
import org.jetbrains.kotlin.ir.expressions.IrBlock
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.expressions.IrBreakContinue
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrConstKind
import org.jetbrains.kotlin.ir.expressions.IrDoWhileLoop
import org.jetbrains.kotlin.ir.expressions.IrElseBranch
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.IrLoop
import org.jetbrains.kotlin.ir.expressions.IrReturn
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.IrWhen
import org.jetbrains.kotlin.ir.expressions.IrWhileLoop
import org.jetbrains.kotlin.ir.expressions.getValueArgument
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
import org.jetbrains.kotlin.ir.symbols.impl.IrSimpleFunctionSymbolImpl
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.isUnitOrNullableUnit
import org.jetbrains.kotlin.ir.types.makeNullable
import org.jetbrains.kotlin.ir.types.toKotlinType
import org.jetbrains.kotlin.ir.util.DeepCopySymbolRemapper
import org.jetbrains.kotlin.ir.util.hasDefaultValue
import org.jetbrains.kotlin.ir.util.statements
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.js.resolve.diagnostics.findPsi
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtFunctionLiteral
import org.jetbrains.kotlin.psi2ir.findFirstFunction
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.inline.InlineUtil
import org.jetbrains.kotlin.types.typeUtil.isUnit
import org.jetbrains.kotlin.types.typeUtil.makeNullable
import org.jetbrains.kotlin.utils.addToStdlib.cast
import org.jetbrains.kotlin.utils.addToStdlib.lastIsInstanceOrNull

/**
 * An enum of the different "states" a parameter of a composable function can have relating to
 * comparison propagation. Each state is represented by two bits in the `$changed` bitmask.
 */
enum class ParamState(private val bits: Int) {
    /**
     * Indicates that nothing is certain about the current state of the parameter. It could be
     * different than it was during the last execution, or it could be the same, but it is not
     * known so the current function looking at it must call equals on it in order to find out.
     * This is the only state that can cause the function to spend slot table space in order to
     * look at it.
     */
    Uncertain(0b00),
    /**
     * This indicates that the value is known to be different since the last time the function
     * was executed. There is no need to store the value in the slot table in this case because
     * the calling function will *always* know whether the value was the same or different as it
     * was in the previous execution.
     */
    Different(0b01),
    /**
     * This indicates that the value is known to be the same since the last time the function was
     * executed. There is no need to store the value in the slot table in this case because the
     * calling function will *always* know whether the value was the same or different as it was
     * in the previous execution.
     */
    Same(0b10),
    /**
     * This indicates that the value is known to *never change* for the duration of the running
     * program.
     */
    Static(0b11);

    fun bitsForSlot(slot: Int): Int = bitsForSlot(bits, slot)
}

fun bitsForSlot(bits: Int, slot: Int): Int = bits shl (slot * 2 + 1)

/**
 * This IR Transform is responsible for the main transformations of the body of a composable
 * function.
 *
 * 1. Control-Flow Group Generation
 * 2. Default arguments
 * 3. Composable Function Skipping
 * 4. Comparison Propagation
 * 5. Recomposability
 *
 * Control-Flow Group Generation
 * =============================
 *
 * This transform will insert groups inside of the bodies of Composable functions
 * depending on the control-flow structures that exist inside of them.
 *
 * There are 3 types of groups in Compose:
 *
 * 1. Replaceable Groups
 * 2. Movable Groups
 * 3. Restart Groups
 *
 * Generally speaking, every composable function *must* emit a single group when it executes.
 * Every group can have any number of children groups. Additionally, we analyze each executable
 * block and apply the following rules:
 *
 * 1. If a block executes exactly 1 time always, no groups are needed
 * 2. If a set of blocks are such that exactly one of them is executed exactly once (for example,
 * the result blocks of a when clause), then we insert a replaceable group around each block.
 * 3. A movable group is only needed if the immediate composable call in the group has a Pivotal
 * property.
 *
 * Default Arguments
 * =================
 *
 * Composable functions need to have the default expressions executed inside of the group of the
 * function. In order to accomplish this, composable functions handle default arguments
 * themselves, instead of using the default handling of kotlin. This is also a win because we can
 * handle the default arguments without generating an additional function since we do not need to
 * worry about callers from java. Generally speaking though, compose handles default arguments
 * similarly to kotlin in that we generate a $default bitmask parameter which maps each parameter
 * index to a bit on the int. A value of "1" for a given parameter index indicated that that
 * value was *not* provided at the callsite, and the default expression should be used instead.
 *
 *     @Composable fun A(x: Int = 0) {
 *       f(x)
 *     }
 *
 * gets transformed into
 *
 *     @Composable fun A(x: Int, $default: Int) {
 *       val x = if ($default and 0b1 != 0) 0 else x
 *       f(x)
 *     }
 *
 * Note: This transform requires [ComposerParamTransformer] to also be run in order to work
 * properly.
 *
 * Composable Function Skipping
 * ============================
 *
 * Composable functions can "skip" their execution if certain conditions are met. This is done by
 * appealing to the composer and storing previous values of functions and determining if we can
 * skip based on whether or not they have changed.
 *
 *     @Composable fun A(x: Int) {
 *       f(x)
 *     }
 *
 * gets transformed into
 *
 *     @Composable fun A(x: Int, $composer: Composer<*>, $changed: Int) {
 *       var $dirty = $changed
 *       if ($changed and 0b0110 === 0) {
 *         $dirty = $dirty or if ($composer.changed(x)) 0b0010 else 0b0100
 *       }
 *      if (%dirty and 0b1011 xor 0b1010 !== 0 || !$composer.skipping) {
 *        f(x)
 *      } else {
 *        $composer.skipCurrentGroup()
 *      }
 *     }
 *
 * Note that this makes use of bitmasks for the $changed and $dirty values. These bitmasks work
 * in a different bit-space than the $default bitmask because two bits are needed to hold the
 * four different possible states of each parameter. Additionally, the lowest bit of the bitmask
 * is a special bit which forces execution of the function.
 *
 * This means that for the ith parameter of a composable function, the bit range of i*2 + 1 to
 * i*2 + 2 are used to store the state of the parameter.
 *
 * The states are outlines by the [ParamState] class.
 *
 * Comparison Propagation
 * ======================
 *
 * Because we detect changes in parameters of composable functions and have that data available
 * in the body of a composable function, if we pass values to another composable function, it
 * makes sense for us to pass on whatever information about that value we can determine at the
 * time. This type of propagation of information through composable functions is called
 * Comparison Propagation.
 *
 * Essentially, this comes down to us passing in useful values into the `$changed` parameter of
 * composable functions.
 *
 * When a composable function executes, we have the current known states of all of the function's
 * parameters in the $dirty variable. We can take bits off of this variable and pass them into a
 * composable function in order to tell that function what we know.
 *
 *     @Composable fun A(x: Int) {
 *       B(x, 123)
 *     }
 *
 * gets transformed into
 *
 *     @Composable fun A(x: Int, $composer: Composer<*>, $changed: Int) {
 *       var $dirty = ...
 *       // ...
 *       B(
 *           x,
 *           123,
 *           $composer,
 *           (0b110 and $dirty) or   // 1st param has same state that our 1st param does
 *           0b11000                 // 2nd parameter is "static"
 *       )
 *     }
 *
 * Recomposability
 * ===============
 *
 * Restartable composable functions get wrapped with "restart groups". Restart groups are like
 * other groups except the end call is more complicated, as it returns a null value if and
 * only if a subscription to that scope could not have occurred. If the value returned is
 * non-null, we generate a lambda that teaches the runtime how to "restart" that group. At a high
 * level, this transform comes down to:
 *
 *     @Composable fun A(x: Int) {
 *       f(x)
 *     }
 *
 * getting transformed into
 *
 *     @Composable fun A(x: Int, $composer: Composer<*>, $changed: Int) {
 *       $composer.startRestartGroup()
 *       // ...
 *       f(x)
 *       $composer.endRestartGroup()?.updateScope { next -> A(x, next, $changed or 0b1) }
 *     }
 */
class ComposableFunctionBodyTransformer(
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

    private val changedDescriptor = composerTypeDescriptor
        .unsubstitutedMemberScope
        .findFirstFunction("changed") {
            // this is the changed(value: T) variant.
            // TODO(lmr): Add handling for different primitive types
            it.typeParameters.size == 1
        }

    private val skipCurrentGroupDescriptor = composerTypeDescriptor
        .unsubstitutedMemberScope
        .findFirstFunction("skipCurrentGroup") { it.valueParameters.size == 0 }

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

    private val startRestartGroupDescriptor = composerTypeDescriptor
        .unsubstitutedMemberScope
        .findFirstFunction(KtxNameConventions.STARTRESTARTGROUP.identifier) {
            it.valueParameters.size == 1
        }

    private val endRestartGroupDescriptor = composerTypeDescriptor
        .unsubstitutedMemberScope
        .findFirstFunction(KtxNameConventions.ENDRESTARTGROUP.identifier) {
            it.valueParameters.size == 0
        }

    private val updateScopeDescriptor =
        endRestartGroupDescriptor.returnType?.memberScope?.getContributedFunctions(
            KtxNameConventions.UPDATE_SCOPE,
            NoLookupLocation.FROM_BACKEND
        )?.singleOrNull { it.valueParameters.first().type.arguments.size == 2 }
            ?: error("new updateScope not found in result type of endRestartGroup")

    private val updateScopeBlockType = updateScopeDescriptor.valueParameters.single().type

    private val isSkippingDescriptor = composerTypeDescriptor
        .unsubstitutedMemberScope
        .getContributedDescriptors { it.asString() == "skipping" }
        .first { it is PropertyDescriptor && it.name.asString() == "skipping" }
        .cast<PropertyDescriptor>()

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
        // if the function isn't composable, there's nothing to do
        if (!scope.isComposable) return super.visitFunction(declaration)
        val restartable = declaration.shouldBeRestartable()
        val isLambda = declaration.isLambda()

        if (declaration.body == null) return declaration

        val changedParam = scope.changedParameter!!
        val defaultParam = scope.defaultParameter

        val numSlots = declaration.valueParameters.size - (
            if (defaultParam != null) 3 else 2
        )

        // restartable functions get extra logic and different types of groups from
        // non-restartable functions, and lambdas get no groups at all.
        return when {
            isLambda -> visitComposableLambda(
                declaration,
                scope,
                changedParam,
                numSlots
            )
            restartable -> visitRestartableComposableFunction(
                declaration,
                scope,
                changedParam,
                defaultParam,
                numSlots
            )
            else -> visitNonRestartableComposableFunction(
                declaration,
                scope,
                changedParam,
                defaultParam,
                numSlots
            )
        }
    }

    // Currently, we make all composable functions restartable by default, unless:
    // 1. They are inline
    // 2. They have a return value (may get relaxed in the future)
    // 3. They are a lambda (we use RestartableFunction<...> class for this instead)
    private fun IrFunction.shouldBeRestartable(): Boolean {
        // Only insert observe scopes in non-empty composable function
        if (body == null)
            return false

        val descriptor = descriptor

        // Do not insert observe scope in an inline function
        if (descriptor.isInline)
            return false

        // Do not insert an observe scope in an inline composable lambda
        descriptor.findPsi()?.let { psi ->
            (psi as? KtFunctionLiteral)?.let {
                if (InlineUtil.isInlinedArgument(
                        it,
                        context.state.bindingContext,
                        false
                    )
                )
                    return false
                if (it.isEmitInline(context.state.bindingContext)) {
                    return false
                }
            }
        }

        // Do not insert an observe scope if the function has a return result
        if (descriptor.returnType.let { it == null || !it.isUnit() })
            return false

        // Do not insert an observe scope if the function hasn't been transformed by the
        // ComposerParamTransformer and has a synthetic "composer param" as its last parameter
        if (composerParam() == null) return false

        // Check if the descriptor has restart scope calls resolved
        if (descriptor is SimpleFunctionDescriptor &&
            // Lambdas should be ignored. All composable lambdas are wrapped by a restartable
            // function wrapper by ComposerLambdaMemoization which supplies the startRestartGroup/
            // endRestartGroup pair on behalf of the lambda.
            origin != IrDeclarationOrigin.LOCAL_FUNCTION_FOR_LAMBDA &&
            origin != IrDeclarationOrigin.LOCAL_FUNCTION_NO_CLOSURE) {

            return true
        }

        return false
    }

    private fun IrFunction.isLambda(): Boolean {
        // There is probably a better way to determine this, but if there is, it isn't obvious
        return descriptor.name.asString() == "<anonymous>"
    }

    // At a high level, a non-restartable composable function
    // 1. gets a replaceable group placed around the body
    // 2. never calls `$composer.changed(...)` with its parameters
    // 3. can have default parameters, so needs to add the defaults preamble if defaults present
    // 4. proper groups around control flow structures in the body
    private fun visitNonRestartableComposableFunction(
        declaration: IrFunction,
        scope: Scope.FunctionScope,
        changedParam: IrValueDeclaration,
        defaultParam: IrValueParameter?,
        numSlots: Int
    ): IrStatement {
        val body = declaration.body!!

        val preamble = IrBlockImpl(
            body.startOffset,
            body.endOffset,
            declaration.returnType,
            null
        )

        scope.dirty = changedParam

        val realParams = declaration.valueParameters.take(numSlots)

        val defaultStatements = buildStatementsForDefault(
            realParams,
            scope,
            defaultParam
        )

        preamble.statements.addAll(defaultStatements.filterNotNull())

        realParams.forEach {
            // we want to remove the default expression from the function. This will prevent
            // the kotlin compiler from doing its own default handling, which we don't need.
            it.defaultValue = null
        }

        var (transformed, returnVar) = IrBlockImpl(
            body.startOffset,
            body.endOffset,
            context.irBuiltIns.nothingType,
            null,
            body.statements
        )
            .withReturnAsVar()

        transformed = transformed.transformChildren()

        val end = { irEndReplaceableGroup() }

        scope.pushEnd(end)

        declaration.body = IrBlockBodyImpl(
            body.startOffset,
            body.endOffset,
            listOfNotNull(
                irStartReplaceableGroup(declaration.irSourceKey()),
                *preamble.statements.toTypedArray(),
                transformed,
                end(),
                returnVar?.let { irReturn(declaration.symbol, irGet(it)) }
            )
        )

        return declaration
    }

    // Composable lambdas are always wrapped with a RestartableFunction class, which has its own
    // group in the invoke call. As a result, composable lambdas:
    // 1. receive no group at the root of their body
    // 2. cannot have default parameters, so have no default handling
    // 3. they cannot be skipped since we do not know their capture scope, so no skipping logic
    // 4. proper groups around control flow structures in the body
    private fun visitComposableLambda(
        declaration: IrFunction,
        scope: Scope.FunctionScope,
        changedParam: IrValueParameter,
        numSlots: Int
    ): IrStatement {
        // no group, since restartableFunction should already create one
        // no composer.changed calls
        // no skipping logic
        // no default logic
        scope.dirty = changedParam

        val realParams = declaration.valueParameters.take(numSlots)

        realParams.forEachIndexed { index, param ->
            scope.remappedParams[param] = param
            scope.paramsToSlots[param] = index
        }

        val body = declaration.body
        if (body is IrBlockBody) {
            if (body.statements.size == 1) {
                val first = body.statements[0]
                if (first is IrReturn) {
                    first.transformChildrenVoid()
                } else {
                    body.transformChildrenVoid()
                }
            } else {
                body.transformChildrenVoid()
            }
        } else {
            body?.transformChildrenVoid()
        }

        return declaration
    }

    // Most composable function declarations will be restartable. At a high level, this means
    // that for this function we:
    // 1. generate a startRestartGroup and endRestartGroup call around its body
    // 2. generate an updateScope lambda and call
    // 3. generate handling of default parameters if necessary
    // 4. generate skipping logic based on parameters passed into the function
    // 5. generate groups around control flow structures in the body
    private fun visitRestartableComposableFunction(
        declaration: IrFunction,
        scope: Scope.FunctionScope,
        changedParam: IrValueParameter,
        defaultParam: IrValueParameter?,
        numSlots: Int
    ): IrStatement {
        val body = declaration.body!!
        val preamble = IrBlockImpl(
            body.startOffset,
            body.endOffset,
            declaration.returnType,
            null
        )

        // these are the parameters excluding the synthetic ones that we generate for compose.
        // These are the only parameters we want to consider in skipping calculations
        val realParams = declaration.valueParameters.take(numSlots)

        // we start off assuming that we *can* skip execution of the function
        var canSkipExecution = true

        // param bitmask (same bit-space as default mask). 1 indicates that the
        // type is unstable
        val unstableMask = bitMask(
            *realParams.map {
                val isStable = it.type.toKotlinType().isStable()
                if (!isStable && !it.hasDefaultValue()) {
                    // if it has non-optional unstable params, the function can never skip
                    canSkipExecution = false
                }
                !isStable
            }.toBooleanArray()
        )

        val defaultStatements = buildStatementsForDefault(
            realParams,
            scope,
            defaultParam
        )

        // if the function can never skip, or there are no parameters to test, then we
        // don't need to have the dirty parameter locally since it will never be different from
        // the passed in `changed` parameter.
        val skippingStatements = if (canSkipExecution && realParams.isNotEmpty()) {
            val dirty = irTemporary(
                irGet(changedParam),
                isVar = true,
                nameHint = "\$dirty",
                exactName = true
            )

            scope.dirty = dirty

            preamble.statements.add(dirty)

            buildStatementsForSkipping(
                realParams,
                scope,
                dirty,
                changedParam,
                defaultParam,
                unstableMask
            )
        } else {
            scope.dirty = changedParam
            realParams.map { null }
        }

        realParams.forEach {
            // we want to remove the default expression from the function. This will prevent
            // the kotlin compiler from doing its own default handling, which we don't need.

            // NOTE: we are doing this AFTER buildStatementsForSkipping, because the default
            // value is used in those calculations
            it.defaultValue = null
        }

        // we want the default handling of a given parameter to be next to the skipping handling of
        // the same parameter, so we iterate through them together and add them to the preamble
        forEachWith(defaultStatements, skippingStatements) { default, skipping ->
            default?.let { preamble.statements.add(it) }
            skipping?.let { preamble.statements.add(it) }
        }

        var (transformed, returnVar) = IrBlockImpl(
            body.startOffset,
            body.endOffset,
            context.irBuiltIns.nothingType,
            null,
            body.statements
        )
            .transformChildren()
            .withReturnAsVar()

        val end = { irEndRestartGroupAndUpdateScope(declaration, changedParam, defaultParam) }

        scope.pushEnd(end)

        // if it has non-optional unstable params, the function can never skip, so we always
        // execute the body. Otherwise, we wrap the body in an if and only skip when certain
        // conditions are met.
        if (canSkipExecution) {
            // We CANNOT skip if any of the following conditions are met
            // 1. if any of the stable parameters have *differences* from last execution.
            // 2. if the composer.skipping call returns false
            // 3. if any of the provided parameters to the function were unstable

            // (3) is only necessary to check if we actually have unstable params, so we only
            // generate that check if we need to.

            var shouldExecute = irOrOr(
                irHasDifferences(scope.dirty!!, numSlots),
                irNot(irIsSkipping())
            )
            val hasAnyUnstableParams = unstableMask != 0

            // if there are unstable params, then we fence the whole expression with a check to
            // see if any of the unstable params were the ones that were provided to the
            // function. If they were, then we short-circuit and always execute
            if (hasAnyUnstableParams && defaultParam != null) {
                shouldExecute = irOrOr(
                    irNotEqual(
                        // ~$default and unstableMask will be non-zero if any parameters were
                        // *provided* AND *unstable*
                        irAnd(
                            irInv(irGet(defaultParam)),
                            irConst(unstableMask)
                        ),
                        irConst(0)
                    ),
                    shouldExecute
                )
            }

            transformed = irIfThenElse(
                condition = shouldExecute,
                thenPart = transformed,
                elsePart = irSkipCurrentGroup()
            )
        }

        declaration.body = IrBlockBodyImpl(
            body.startOffset,
            body.endOffset,
            listOfNotNull(
                irStartRestartGroup(declaration.irSourceKey()),
                *preamble.statements.toTypedArray(),
                transformed,
                if (returnVar == null) end() else null,
                returnVar?.let { irReturn(declaration.symbol, irGet(it)) }
            )
        )

        return declaration
    }

    private fun buildStatementsForDefault(
        parameters: List<IrValueParameter>,
        scope: Scope.FunctionScope,
        defaultParam: IrValueParameter?
    ): List<IrStatement?> {
        // if there is no default param, there are no defaults. In this case we just want to make
        // sure that that the parameters and their corresponding slots are populated in the scope
        if (defaultParam == null) return parameters.mapIndexed { index, param ->
            scope.remappedParams[param] = param
            scope.paramsToSlots[param] = index
            null
        }
        return parameters.mapIndexed { index, param ->
            val defaultExpr = param.defaultValue
            if (defaultExpr != null) {
                // create a new temporary variable with the same name as the parameter itself
                // initialized to be either the parameter, or the result of the default expression
                val varSymbol = irTemporary(
                    irIfThenElse(
                        param.type,
                        condition = irGetBit(defaultParam, index),
                        thenPart = defaultExpr.expression,
                        elsePart = irGet(param)
                    ),
                    param.name.identifier,
                    param.type,
                    exactName = true
                )
                // we need to ensure that this transform runs on the default expression. It could
                // contain conditional logic as well as composable calls
                varSymbol.transformChildrenVoid()

                // semantically, any reference to the parameter symbol now needs to be remapped
                // to the temporary variable.
                scope.remappedParams[param] = varSymbol

                // in order to propagate the change detection we might perform on this parameter,
                // we need to know which "slot" it is in
                scope.paramsToSlots[varSymbol] = index
                varSymbol
            } else {
                scope.remappedParams[param] = param
                scope.paramsToSlots[param] = index
                null
            }
        }
    }

    private fun buildStatementsForSkipping(
        parameters: List<IrValueParameter>,
        scope: Scope.FunctionScope,
        dirty: IrVariable,
        changedParam: IrValueParameter,
        defaultParam: IrValueParameter?,
        unstableMask: Int
    ): List<IrStatement?> {
        return parameters.mapIndexed { index, param ->
            val defaultValue = param.defaultValue
            val modifyDirtyFromChangedResult = irSet(
                dirty,
                irOr(
                    irGet(dirty),
                    irIfThenElse(
                        context.irBuiltIns.intType,
                        irChanged(irGet(scope.remappedParams[param]!!)),
                        // if the value has changed, update the bits in the slot to be
                        // "Different"
                        thenPart = irConst(ParamState.Different.bitsForSlot(index)),
                        // if the value has not changed, update the bits in the slot to
                        // be "Same"
                        elsePart = irConst(ParamState.Same.bitsForSlot(index))
                    )
                )
            )

            if (unstableMask[index])
                // if the value is unstable, there is no reason for us to store it in the slot table
                null
            else if (
                defaultParam != null &&
                defaultValue != null &&
                defaultValue.expression.isStatic()
            )
                // if the default expression is "static", then we know that if we are using the
                // default expression, the parameter can be considered "static".
                irWhen(
                    origin = IrStatementOrigin.IF,
                    branches = listOf(
                        irBranch(
                            condition = irGetBit(defaultParam, index),
                            result = irSet(
                                dirty,
                                irOr(
                                    irGet(dirty),
                                    irConst(ParamState.Static.bitsForSlot(index))
                                )
                            )
                        ),
                        irBranch(
                            condition = irIsUncertain(changedParam, index),
                            result = modifyDirtyFromChangedResult
                        )
                    )
                )
            else
                irIf(
                    // we only call `$composer.changed(...)` on a parameter if the value came in
                    // with an "Uncertain" state AND the value was provided. This is safe to do
                    // because this will remain true or false for *every* execution of the
                    // function, so we will never get a slot table misalignment as a result.
                    condition = irIsUncertainAndProvided(changedParam, defaultParam, index),
                    body = modifyDirtyFromChangedResult
                )
        }
    }

    private fun irEndRestartGroupAndUpdateScope(
        function: IrFunction,
        changedParam: IrValueDeclaration,
        defaultParam: IrValueDeclaration?
    ): IrExpression {

        // Save the dispatch receiver into a temporary created in
        // the outer scope because direct references to the
        // receiver sometimes cause an invalid name, "$<this>", to
        // be generated.
        val dispatchReceiverParameter = function.dispatchReceiverParameter
        val outerReceiver = if (dispatchReceiverParameter != null) irTemporary(
            value = irGet(dispatchReceiverParameter),
            nameHint = "rcvr"
        ) else null

        // Create self-invoke lambda
        val lambdaDescriptor = AnonymousFunctionDescriptor(
            function.descriptor,
            Annotations.EMPTY,
            CallableMemberDescriptor.Kind.DECLARATION,
            SourceElement.NO_SOURCE,
            false
        )

        val passedInComposerParameter = ValueParameterDescriptorImpl(
            containingDeclaration = lambdaDescriptor,
            original = null,
            index = 0,
            annotations = Annotations.EMPTY,
            name = KtxNameConventions.COMPOSER_PARAMETER,
            outType = composerTypeDescriptor.defaultType.makeNullable(),
            declaresDefaultValue = false,
            isCrossinline = false,
            isNoinline = false,
            varargElementType = null,
            source = SourceElement.NO_SOURCE
        )

        lambdaDescriptor.apply {
            initialize(
                null,
                null,
                emptyList(),
                listOf(passedInComposerParameter),
                updateScopeBlockType,
                Modality.FINAL,
                Visibilities.LOCAL
            )
        }

        val lastIndex = function.symbol.descriptor.valueParameters.size - 1
        val defaultIndex = lastIndex
        val changedIndex = if (defaultParam != null) lastIndex - 1 else lastIndex
        val composerIndex = changedIndex - 1

        val lambda = IrFunctionImpl(
            UNDEFINED_OFFSET, UNDEFINED_OFFSET,
            IrDeclarationOrigin.LOCAL_FUNCTION_FOR_LAMBDA,
            IrSimpleFunctionSymbolImpl(lambdaDescriptor),
            context.irBuiltIns.unitType
        ).also { fn ->
            fn.parent = function
            val localIrBuilder = DeclarationIrBuilder(context, fn.symbol)
            fn.addValueParameter(
                KtxNameConventions.COMPOSER_PARAMETER.identifier,
                composerTypeDescriptor.defaultType.toIrType().makeNullable()
            )
            fn.body = localIrBuilder.irBlockBody {
                // Call the function again with the same parameters
                +irReturn(irCall(function.symbol).apply {
                    symbol.descriptor
                        .valueParameters
                        .filter {
                            !it.isComposerParam()
                        }
                        .forEachIndexed { index, _ ->
                            // NOTE(lmr): should we be using the parameter here, or the temporary
                            // with the default value?
                            putValueArgument(index, irGet(function.valueParameters[index]))
                        }

                    putValueArgument(
                        composerIndex,
                        irGet(fn.valueParameters[0])
                    )

                    putValueArgument(
                        changedIndex,
                        // the call in updateScope needs to *always* have the low bit set to 1.
                        // This ensures that the body of the function is actually executed.
                        irOr(irGet(changedParam), irConst(0b1))
                    )

                    defaultParam?.let {
                        putValueArgument(
                            defaultIndex,
                            irGet(it.symbol.owner)
                        )
                    }

                    dispatchReceiver = outerReceiver?.let { irGet(it) }
                    extensionReceiver = function.extensionReceiverParameter?.let { irGet(it) }
                    symbol.descriptor.typeParameters.forEachIndexed { index, descriptor ->
                        putTypeArgument(index, descriptor.defaultType.toIrType())
                    }
                })
            }
        }

        // $composer.endRestartGroup()?.updateScope { next -> TheFunction(..., next) }
        return irBlock(
            statements = listOfNotNull(
                outerReceiver,
                irSafeCall(
                    irEndRestartGroup(),
                    updateScopeDescriptor,
                    irLambda(lambda, updateScopeBlockType.toIrType())
                )
            )
        )
    }

    private fun irIsSkipping(): IrExpression {
        return irMethodCall(irCurrentComposer(), isSkippingDescriptor.getter!!)
    }

    private fun irHasDifferences(dirty: IrValueDeclaration, slotCount: Int): IrExpression {
        if (slotCount == 0) {
            // for 0 slots (no params), we can create a shortcut expression of just checking the
            // low-bit for non-zero. Since all of the higher bits will also be 0, we can just
            // simplify this to check if dirty is non-zero
            return irNotEqual(
                irGet(dirty),
                irConst(0)
            )
        }

        // makes an int with each slot having 0b01 mask and the low bit being 0.
        // so for 3 slots, we would get 0b 01 01 01 0.
        // This pattern is useful because we can and + xor it with our $changed bitmask and it
        // will only be non-zero if any of the slots were DIFFERENT or UNCERTAIN.
        val bitPattern = (0..slotCount).fold(0) { mask, slot ->
            mask or bitsForSlot(0b01, slot)
        }

        // we use this pattern with the low bit set to 1 in the "and", and the low bit set to 0
        // for the "xor". This means that if the low bit was set, we will get 1 in the resulting
        // low bit. Since we use this calculation to determine if we need to run the body of the
        // function, this is exactly what we want.

        // $dirty and (0b 01 ... 01 1) xor (0b 01 ... 01 0)
        return irNotEqual(
            irXor(
                irAnd(
                    irGet(dirty),
                    irConst(bitPattern or 0b1)
                ),
                irConst(bitPattern or 0b0)
            ),
            irConst(0) // anything non-zero means we have differences
        )
    }

    private fun irIsUncertainAndProvided(
        changed: IrValueParameter,
        default: IrValueParameter?,
        slot: Int
    ): IrExpression {
        // we want to call %composer.changed if the value is uncertain *and* it was
        // provided. If the value wasn't provided, we don't want to waste slot space on
        // it *and* we don't want to let it impact our ability to skip if the type of the
        // unprovided parameter is not "stable"
        return if (default != null) {
            // (%default and 0b1) == 0 && (%changed and 0b11) == 0
            irAndAnd(
                irEqual(
                    irAnd(
                        irGet(default), // a value of 1 in default means it was NOT provided
                        irConst(0b1 shl slot)
                    ),
                    irConst(0)
                ),
                irIsUncertain(changed, slot)
            )
        } else {
            // %changed and 0b11 == 0
            irIsUncertain(changed, slot)
        }
    }

    private fun irIsUncertain(
        changed: IrValueParameter,
        slot: Int
    ): IrExpression {
        // %changed and 0b11 == 0
        return irEqual(
            irAnd(
                irGet(changed),
                irBitsForSlot(0b11, slot)
            ),
            irConst(0)
        )
    }

    @Suppress("SameParameterValue")
    private fun irBitsForSlot(bits: Int, slot: Int): IrExpression {
        return irConst(bitsForSlot(bits, slot))
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

    private fun IrExpression.withReturnAsVar(): Pair<IrExpression, IrVariable?> {
        if (this is IrReturn) return irTemporary(value).let {
            irBlock(statements = listOf(it)) to it
        }
        var block = this as? IrBlock
        var expr: IrStatement? = block?.statements?.lastOrNull()
        while (expr != null && block != null) {
            if (expr is IrReturn) {
                val temp = irTemporary(expr.value)
                block.statements.pop()
                block.statements.add(temp)
                return this to temp
            }
            if (expr !is IrBlock) return this to null
            block = expr
            expr = block.statements.lastOrNull()
        }
        return this to null
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
        return currentFunctionScope.composerParameter
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

    private fun irStartRestartGroup(key: IrExpression): IrExpression {
        return irMethodCall(irCurrentComposer(), startRestartGroupDescriptor).also {
            it.putValueArgument(0, key)
        }
    }

    private fun irEndRestartGroup(): IrExpression {
        return irMethodCall(irCurrentComposer(), endRestartGroupDescriptor)
    }

    private fun irChanged(value: IrExpression): IrExpression {
        return irMethodCall(irCurrentComposer(), changedDescriptor).also {
            it.putValueArgument(0, value)
            it.putTypeArgument(0, value.type)
        }
    }

    private fun irSkipCurrentGroup(): IrExpression {
        return irMethodCall(irCurrentComposer(), skipCurrentGroupDescriptor)
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

    private fun irSafeCall(
        target: IrExpression,
        descriptor: FunctionDescriptor,
        vararg args: IrExpression
    ): IrExpression {
        val tmpVal = irTemporary(target, nameHint = "safe_receiver")
        return irBlock(
            origin = IrStatementOrigin.SAFE_CALL,
            statements = listOf(
                tmpVal,
                irIfThenElse(
                    condition = irEqual(irGet(tmpVal), irNull()),
                    thenPart = irNull(),
                    elsePart = irCall(descriptor).apply {
                        dispatchReceiver = irGet(tmpVal)
                        args.forEachIndexed { i, arg ->
                            putValueArgument(i, arg)
                        }
                    }
                )
            )
        )
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
        isVar: Boolean = false,
        exactName: Boolean = false
    ): IrVariableImpl {
        val scope = currentFunctionScope
        val name = if (exactName && nameHint != null)
            nameHint
        else
            scope.getNameForTemporary(nameHint)
        val tempVarDescriptor = IrTemporaryVariableDescriptorImpl(
            scope.function.symbol.descriptor,
            Name.identifier(name),
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
        start: IrExpression?,
        wrapped: IrExpression,
        end: IrExpression?
    ): IrExpression {
        if (wrapped.type.isUnitOrNullableUnit()) {
            return IrBlockImpl(
                wrapped.startOffset,
                wrapped.endOffset,
                wrapped.type,
                null,
                listOfNotNull(
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
                listOfNotNull(
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
        loop@ for (scope in scopeStack.asReversed()) {
            when (scope) {
                is Scope.FunctionScope -> {
                    scope.markReturn(pushEndCall)
                    if (scope.function == symbol.owner) {
                        break@loop
                    } else {
                        TODO("Need to handle nested returns!")
                    }
                }
                is Scope.BlockScope -> {
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
            val result = transform(this@ComposableFunctionBodyTransformer, null)
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

    class ParamMeta(
        var isProvided: Boolean = false,
        var isStatic: Boolean = false,
        var isCertain: Boolean = false,
        var maskSlot: Int = -1,
        var maskParam: IrValueDeclaration? = null
    )

    override fun visitBlock(expression: IrBlock): IrExpression {
        return when (expression.origin) {
            IrStatementOrigin.ARGUMENTS_REORDERING_FOR_CALL -> {
                val varsToTemps = expression
                    .statements
                    .mapNotNull { it as? IrVariable }
                    .mapNotNull { temp ->
                        val ref = temp.initializer as? IrGetValue
                        ref?.let { it.symbol.owner to temp }
                    }

                for (scope in scopeStack.asReversed()) {
                    when (scope) {
                        is Scope.FunctionScope -> {
                            for ((ref, temp) in varsToTemps) {
                                if (scope.remappedParams.containsValue(ref)) {
                                    scope.remappedParams[temp] = ref
                                    scope.paramsToSlots[temp] = scope.paramsToSlots[ref]!!
                                }
                            }
                        }
                    }
                }
                super.visitBlock(expression)
            }
            else -> super.visitBlock(expression)
        }
    }

    override fun visitCall(expression: IrCall): IrExpression {
        if (!expression.isTransformedComposableCall() && !expression.isSyntheticComposableCall()) {
            return super.visitCall(expression)
        }
        encounteredComposableCall()

        // it's important that we transform all of the parameters here since this will cause the
        // IrGetValue's of remapped default parameters to point to the right variable.
        expression.transformChildrenVoid()

        val numParams = expression.descriptor.valueParameters.size
        val lastParam = expression.descriptor.valueParameters[numParams - 1]
        val hasDefault = lastParam.name == KtxNameConventions.DEFAULT_PARAMETER
        val numRealParams = if (hasDefault) numParams - 3 else numParams - 2
        val defaultArg = if (hasDefault) expression.getValueArgument(lastParam) else null
        val changedArgIndex = if (hasDefault) numParams - 2 else numParams - 1

        val defaultMask = when {
            !hasDefault -> 0b0
            defaultArg !is IrConst<*> -> error("Expected default mask to be a const")
            else -> defaultArg.value as? Int ?: error("Expected default mask to be an Int")
        }

        val paramMeta = mutableListOf<ParamMeta>()

        for (index in 0 until numRealParams) {
            val arg = expression.getValueArgument(index)
                // ComposerParamTransformer should not allow for any null arguments on a composable
                // invocation. If this is null here, we have missed something.
                ?: error("Unexpected null argument for composable call")
            val meta = ParamMeta(
                isProvided = defaultMask and (0b1 shl index) == 0
            )

            when {
                arg.isStatic() -> meta.isStatic = true
                arg is IrGetValue -> {
                    extractParamMetaFromScopes(meta, arg.symbol.owner)
                }
            }

            paramMeta.add(meta)
        }

        expression.putValueArgument(
            changedArgIndex,
            buildChangedParamForCall(paramMeta)
        )

        return expression
    }

    private fun IrExpression.isStatic(): Boolean {
        // TODO: expand static detection beyond constant literals
        return when (this) {
            is IrConst<*> -> true
            else -> false
        }
    }

    private fun extractParamMetaFromScopes(meta: ParamMeta, param: IrValueDeclaration) {
        for (scope in scopeStack.asReversed()) {
            when (scope) {
                is Scope.FunctionScope -> {
                    if (scope.remappedParams.containsValue(param)) {
                        meta.isCertain = true
                        meta.maskParam = scope.dirty
                        meta.maskSlot = scope.paramsToSlots[param]!!
                    }
                }
            }
        }
    }

    private fun buildChangedParamForCall(params: List<ParamMeta>): IrExpression {
        // The general pattern here is:
        //
        // $changed = bitMaskConstant or
        // (0b11 and someMask shl y) or
        // (0b1100 and someMask shl x) or
        // ...
        // (0b11000000 and someMask shr z)
        //
        // where `bitMaskConstant` is created in this function based on
        // all of the static (constant) params and uncertain params (not direct parameter pass
        // throughs). The other params have had their state made "certain" by the preamble checks
        // in a composable function in scope. We can extract that state directly by pulling out
        // the specific slot state from that function's dirty parameter (represented as
        // `someMask` here, and then shifting the resulting bit mask over to the correct slot
        // (the shift amount represented here by `x`, `y`, and `z`).

        // TODO: we could make some small optimization here if we have multiple values passed
        //  from one function into another in the same order. This may not happen commonly enough
        //  to be worth the complication though.

        // NOTE: we start with 0b0 because it is important that the low bit is always 0
        var bitMaskConstant = 0b0
        val orExprs = mutableListOf<IrExpression>()

        params.forEachIndexed { slot, meta ->
            if (!meta.isProvided) {
                bitMaskConstant = bitMaskConstant or ParamState.Uncertain.bitsForSlot(slot)
            } else if (meta.isStatic) {
                bitMaskConstant = bitMaskConstant or ParamState.Static.bitsForSlot(slot)
            } else if (!meta.isCertain) {
                bitMaskConstant = bitMaskConstant or ParamState.Uncertain.bitsForSlot(slot)
            } else {
                val someMask = meta.maskParam ?: error("Mask param required if param is Certain")
                val parentSlot = meta.maskSlot
                require(parentSlot != -1) { "invalid parent slot for Certain param" }

                // if parentSlot is lower than slot, we shift left a positive amount of bits
                val bitsToShiftLeft = (slot - parentSlot) * 2
                orExprs.add(
                    irAnd(
                        irConst(bitsForSlot(0b11, slot)),
                        irShiftBits(irGet(someMask), bitsToShiftLeft)
                    )
                )
            }
        }
        return when {
            // if there are no orExprs, then we can just use the constant
            orExprs.isEmpty() -> irConst(bitMaskConstant)
            // if the constant is still 0, then we can just use the or expressions. This is safe
            // because the low bit will still be 0 regardless of the result of the or expressions.
            bitMaskConstant == 0 -> orExprs.reduce { lhs, rhs ->
                irOr(lhs, rhs)
            }
            // otherwise, we do (bitMaskConstant or a or b ... or z)
            else -> orExprs.fold<IrExpression, IrExpression>(irConst(bitMaskConstant)) { lhs, rhs ->
                irOr(lhs, rhs)
            }
        }
    }

    override fun visitGetValue(expression: IrGetValue): IrExpression {
        for (scope in scopeStack.asReversed()) {
            if (scope is Scope.FunctionScope) {
                val remapped = scope.remappedParams[expression.symbol.owner]
                if (remapped != null) {
                    return irGet(remapped)
                }
            }
        }
        return expression
    }

    override fun visitReturn(expression: IrReturn): IrExpression {
        if (!currentFunctionScope.isComposable) return super.visitReturn(expression)
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
        if (!currentFunctionScope.isComposable) return super.visitBreakContinue(jump)
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
        if (!currentFunctionScope.isComposable) return super.visitDoWhileLoop(loop)
        return handleLoop(loop as IrLoopBase)
    }

    override fun visitWhileLoop(loop: IrWhileLoop): IrExpression {
        if (!currentFunctionScope.isComposable) return super.visitWhileLoop(loop)
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
        if (!currentFunctionScope.isComposable) return super.visitWhen(expression)

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

        forEachWith(transformed.branches, condScopes, resultScopes) { it, condScope, resultScope ->
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
            val remappedParams = mutableMapOf<IrValueDeclaration, IrValueDeclaration>()
            val paramsToSlots = mutableMapOf<IrValueDeclaration, Int>()

            private var lastTemporaryIndex: Int = 0

            private fun nextTemporaryIndex(): Int = lastTemporaryIndex++

            var composerParameter: IrValueParameter? = null
                private set

            var defaultParameter: IrValueParameter? = null
                private set

            var changedParameter: IrValueParameter? = null
                private set

            var dirty: IrValueDeclaration? = null

            init {
                loop@ for (param in function.valueParameters.asReversed()) {
                    when (param.name.identifier) {
                        KtxNameConventions.COMPOSER_PARAMETER.identifier ->
                            composerParameter = param
                        KtxNameConventions.DEFAULT_PARAMETER.identifier ->
                            defaultParameter = param
                        KtxNameConventions.CHANGED_PARAMETER.identifier ->
                            changedParameter = param
                        else -> break@loop
                    }
                }
            }

            val isComposable = composerParameter != null

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

inline fun <A, B> forEachWith(a: List<A>, b: List<B>, fn: (A, B) -> Unit) {
    for (i in a.indices) {
        fn(a[i], b[i])
    }
}

inline fun <A, B, C> forEachWith(a: List<A>, b: List<B>, c: List<C>, fn: (A, B, C) -> Unit) {
    for (i in a.indices) {
        fn(a[i], b[i], c[i])
    }
}