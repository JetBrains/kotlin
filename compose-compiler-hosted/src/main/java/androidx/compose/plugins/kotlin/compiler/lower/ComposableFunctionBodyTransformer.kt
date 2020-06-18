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

import androidx.compose.plugins.kotlin.ComposeFqNames
import androidx.compose.plugins.kotlin.KtxNameConventions
import androidx.compose.plugins.kotlin.composableReadonlyContract
import androidx.compose.plugins.kotlin.composableRestartableContract
import androidx.compose.plugins.kotlin.hasUntrackedAnnotation
import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.backend.common.pop
import org.jetbrains.kotlin.backend.common.push
import org.jetbrains.kotlin.builtins.PrimitiveType
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
import org.jetbrains.kotlin.fir.java.topLevelName
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.backend.js.utils.OperatorNames
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
import org.jetbrains.kotlin.ir.declarations.IrLocalDelegatedProperty
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.IrTypeAlias
import org.jetbrains.kotlin.ir.declarations.IrTypeParameter
import org.jetbrains.kotlin.ir.declarations.IrValueDeclaration
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.declarations.impl.IrFunctionImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrVariableImpl
import org.jetbrains.kotlin.ir.expressions.IrBlock
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrBreakContinue
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrConstKind
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrDoWhileLoop
import org.jetbrains.kotlin.ir.expressions.IrElseBranch
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionExpression
import org.jetbrains.kotlin.ir.expressions.IrGetEnumValue
import org.jetbrains.kotlin.ir.expressions.IrGetObjectValue
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.IrLoop
import org.jetbrains.kotlin.ir.expressions.IrReturn
import org.jetbrains.kotlin.ir.expressions.IrStatementContainer
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.IrVararg
import org.jetbrains.kotlin.ir.expressions.IrWhen
import org.jetbrains.kotlin.ir.expressions.IrWhileLoop
import org.jetbrains.kotlin.ir.expressions.impl.IrBlockBodyImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrBlockImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrBranchImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrCompositeImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrContainerExpressionBase
import org.jetbrains.kotlin.ir.expressions.impl.IrElseBranchImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrLoopBase
import org.jetbrains.kotlin.ir.expressions.impl.IrReturnImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrSpreadElementImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrVarargImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrWhenImpl
import org.jetbrains.kotlin.ir.symbols.IrReturnTargetSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrSimpleFunctionSymbolImpl
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.types.isNothing
import org.jetbrains.kotlin.ir.types.isUnit
import org.jetbrains.kotlin.ir.types.isUnitOrNullableUnit
import org.jetbrains.kotlin.ir.types.makeNullable
import org.jetbrains.kotlin.ir.types.toKotlinType
import org.jetbrains.kotlin.ir.util.DeepCopySymbolRemapper
import org.jetbrains.kotlin.ir.util.getArguments
import org.jetbrains.kotlin.ir.util.getPropertyGetter
import org.jetbrains.kotlin.ir.util.isInlined
import org.jetbrains.kotlin.ir.util.isVararg
import org.jetbrains.kotlin.ir.util.patchDeclarationParents
import org.jetbrains.kotlin.ir.util.statements
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.js.resolve.diagnostics.findPsi
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtFunctionLiteral
import org.jetbrains.kotlin.psi2ir.findFirstFunction
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.inline.InlineUtil
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.typeUtil.isBoolean
import org.jetbrains.kotlin.types.typeUtil.isByte
import org.jetbrains.kotlin.types.typeUtil.isChar
import org.jetbrains.kotlin.types.typeUtil.isDouble
import org.jetbrains.kotlin.types.typeUtil.isFloat
import org.jetbrains.kotlin.types.typeUtil.isInt
import org.jetbrains.kotlin.types.typeUtil.isLong
import org.jetbrains.kotlin.types.typeUtil.isNullableAny
import org.jetbrains.kotlin.types.typeUtil.isShort
import org.jetbrains.kotlin.types.typeUtil.isUnit
import org.jetbrains.kotlin.types.typeUtil.makeNullable
import org.jetbrains.kotlin.types.typeUtil.replaceArgumentsWithStarProjections
import org.jetbrains.kotlin.utils.addToStdlib.cast
import org.jetbrains.kotlin.utils.addToStdlib.lastIsInstanceOrNull
import org.jetbrains.kotlin.utils.ifEmpty
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.min

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
     * This indicates that the value is known to be the same since the last time the function was
     * executed. There is no need to store the value in the slot table in this case because the
     * calling function will *always* know whether the value was the same or different as it was
     * in the previous execution.
     */
    Same(0b01),
    /**
     * This indicates that the value is known to be different since the last time the function
     * was executed. There is no need to store the value in the slot table in this case because
     * the calling function will *always* know whether the value was the same or different as it
     * was in the previous execution.
     */
    Different(0b10),
    /**
     * This indicates that the value is known to *never change* for the duration of the running
     * program.
     */
    Static(0b11);

    fun bitsForSlot(slot: Int): Int = bitsForSlot(bits, slot)
}

const val BITS_PER_INT = 31
const val SLOTS_PER_INT = 15

fun bitsForSlot(bits: Int, slot: Int): Int {
    val realSlot = slot.rem(SLOTS_PER_INT)
    return bits shl (realSlot * 2 + 1)
}

fun defaultsParamIndex(index: Int): Int = index / BITS_PER_INT
fun defaultsBitIndex(index: Int): Int = index.rem(BITS_PER_INT)

val IrFunction.thisParamCount
    get() = (
        if (dispatchReceiverParameter != null) 1 else 0
    ) + (
        if (extensionReceiverParameter != null) 1 else 0
    )

fun changedParamCount(realValueParams: Int, thisParams: Int): Int {
    if (realValueParams == 0) return 1
    val totalParams = realValueParams + thisParams
    return ceil(
        totalParams.toDouble() / SLOTS_PER_INT.toDouble()
    ).toInt()
}

fun changedParamCountFromTotal(totalParamsIncludingThisParams: Int): Int {
    var realParams = totalParamsIncludingThisParams
    realParams-- // composer param
    realParams-- // key param
    realParams-- // first changed param (always present)
    var changedParams = 0
    do {
        realParams -= SLOTS_PER_INT
        changedParams++
    } while (realParams > 0)
    return changedParams
}

fun defaultParamCount(realValueParams: Int): Int {
    return ceil(
        realValueParams.toDouble() / BITS_PER_INT.toDouble()
    ).toInt()
}

fun composeSyntheticParamCount(
    realValueParams: Int,
    thisParams: Int = 0,
    hasDefaults: Boolean = false
): Int {
    return 1 + // composer param
            1 + // key param
            changedParamCount(realValueParams, thisParams) +
            if (hasDefaults) defaultParamCount(realValueParams) else 0
}

interface IrChangedBitMaskValue {
    fun irLowBit(): IrExpression
    fun irIsolateBitsAtSlot(slot: Int): IrExpression
    fun irHasDifferences(): IrExpression
    fun irCopyToTemporary(
        nameHint: String? = null,
        isVar: Boolean = false,
        exactName: Boolean = false
    ): IrChangedBitMaskVariable
    fun putAsValueArgumentInWithLowBit(
        fn: IrFunctionAccessExpression,
        startIndex: Int,
        lowBit: Boolean
    )
    fun irShiftBits(fromSlot: Int, toSlot: Int): IrExpression
}

interface IrDefaultBitMaskValue {
    fun irIsolateBitAtIndex(index: Int): IrExpression
    fun irHasAnyProvidedAndUnstable(unstable: BooleanArray): IrExpression
    fun putAsValueArgumentIn(fn: IrFunctionAccessExpression, startIndex: Int)
}

interface IrChangedBitMaskVariable : IrChangedBitMaskValue {
    fun asStatements(): List<IrStatement>
    fun irOrSetBitsAtSlot(slot: Int, value: IrExpression): IrExpression
}

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
 *        $composer.skipToGroupEnd()
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
    context: IrPluginContext,
    symbolRemapper: DeepCopySymbolRemapper,
    bindingTrace: BindingTrace
) :
    AbstractComposeLowering(context, symbolRemapper, bindingTrace),
    FileLoweringPass,
    ModuleLoweringPass {

    override fun lower(module: IrModuleFragment) {
        module.transformChildrenVoid(this)
        module.patchDeclarationParents()
    }

    override fun lower(irFile: IrFile) {
        irFile.transformChildrenVoid(this)
    }

    private val changedDescriptor = composerTypeDescriptor
        .unsubstitutedMemberScope
        .findFirstFunction("changed") {
            it.valueParameters.first().type.isNullableAny()
        }

    fun KotlinType.toPrimitiveType(): PrimitiveType? = when {
        isInt() -> PrimitiveType.INT
        isBoolean() -> PrimitiveType.BOOLEAN
        isFloat() -> PrimitiveType.FLOAT
        isLong() -> PrimitiveType.LONG
        isDouble() -> PrimitiveType.DOUBLE
        isByte() -> PrimitiveType.BYTE
        isChar() -> PrimitiveType.CHAR
        isShort() -> PrimitiveType.SHORT
        else -> null
    }

    private val changedDescriptors = composerTypeDescriptor
        .unsubstitutedMemberScope
        .getContributedFunctions(Name.identifier("changed"), NoLookupLocation.FROM_BACKEND)
        .filter { it.name.identifier == "changed" }
        .mapNotNull { desc ->
            desc.valueParameters.first().type.toPrimitiveType()?.let { primitive ->
                primitive to desc
            }
        }
        .toMap()

    private val skipToGroupEndDescriptor = composerTypeDescriptor
        .unsubstitutedMemberScope
        .findFirstFunction("skipToGroupEnd") { it.valueParameters.size == 0 }

    private val skipCurrentGroupDescriptor = composerTypeDescriptor
        .unsubstitutedMemberScope
        .findFirstFunction("skipCurrentGroup") { it.valueParameters.size == 0 }

    private val startReplaceableDescriptor = composerTypeDescriptor
        .unsubstitutedMemberScope
        .findFirstFunction("startReplaceableGroup") { it.valueParameters.size == 1 }

    private val endReplaceableDescriptor = composerTypeDescriptor
        .unsubstitutedMemberScope
        .findFirstFunction("endReplaceableGroup") { it.valueParameters.size == 0 }

    private val startDefaultsDescriptor = composerTypeDescriptor
        .unsubstitutedMemberScope
        .findFirstFunction("startDefaults") { it.valueParameters.size == 0 }

    private val endDefaultsDescriptor = composerTypeDescriptor
        .unsubstitutedMemberScope
        .findFirstFunction("endDefaults") { it.valueParameters.size == 0 }

    private val startMovableDescriptor = composerTypeDescriptor
        .unsubstitutedMemberScope
        .findFirstFunction("startMovableGroup") { it.valueParameters.size == 2 }

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
        )?.singleOrNull { it.valueParameters.first().type.arguments.size == 4 }
            ?: error("new updateScope not found in result type of endRestartGroup")

    private val updateScopeBlockType = updateScopeDescriptor.valueParameters.single().type

    private val isSkippingDescriptor = composerTypeDescriptor
        .unsubstitutedMemberScope
        .getContributedDescriptors { it.asString() == "skipping" }
        .first { it is PropertyDescriptor && it.name.asString() == "skipping" }
        .cast<PropertyDescriptor>()

    private val defaultsInvalidDescriptor = composerTypeDescriptor
        .unsubstitutedMemberScope
        .getContributedDescriptors { it.asString() == "defaultsInvalid" }
        .first { it is PropertyDescriptor && it.name.asString() == "defaultsInvalid" }
        .cast<PropertyDescriptor>()

    private val joinKeyDescriptor = composerTypeDescriptor
        .unsubstitutedMemberScope
        .findFirstFunction(KtxNameConventions.JOINKEY.identifier) {
            it.valueParameters.size == 2
        }

    private val scopeStack = mutableListOf<Scope>()

    private fun printScopeStack(): String {
        return buildString {
            for (scope in scopeStack) {
                appendln(scope.name)
            }
        }
    }

    private val isInComposableScope: Boolean
        get() {
            loop@ for (scope in scopeStack.asReversed()) {
                return when (scope) {
                    is Scope.FunctionScope -> {
                        if (scope.isInlinedLambda) {
                            continue@loop
                        }
                        scope.isComposable
                    }
                    is Scope.BlockScope -> continue@loop
                    else -> false
                }
            }
            return false
        }

    private val currentFunctionScope
        get() = scopeStack.lastIsInstanceOrNull<Scope.FunctionScope>()
            ?: error("Expected a FunctionScope but none exist. \n${printScopeStack()}")

    override fun visitClass(declaration: IrClass): IrStatement {
        val scope = Scope.ClassScope(declaration.name)
        try {
            scopeStack.push(scope)
            return super.visitDeclaration(declaration)
        } finally {
            require(scopeStack.pop() == scope) { "Unbalanced scope stack" }
        }
    }

    override fun visitFunction(declaration: IrFunction): IrStatement {
        val scope = Scope.FunctionScope(declaration, this)
        try {
            scopeStack.push(scope)
            return visitFunctionInScope(declaration)
        } finally {
            val popped = scopeStack.pop()
            require(popped == scope) { "Unbalanced scope stack" }
            if (scope.isInlinedLambda && !scope.isComposable && scope.hasComposableCalls) {
                encounteredCapturedComposableCall()
            }
        }
    }

    private fun visitFunctionInScope(declaration: IrFunction): IrStatement {
        val scope = currentFunctionScope
        // if the function isn't composable, there's nothing to do
        if (!scope.isComposable) return super.visitFunction(declaration)
        val restartable = declaration.shouldBeRestartable()
        val isLambda = declaration.isLambda()
        // if the lambda is untracked, we generate the body like a non-restartable function since
        // the group/update scope is not going to be handled by the RestartableFunction class
        val isTracked = !declaration.descriptor.hasUntrackedAnnotation()

        if (declaration.body == null) return declaration

        val changedParam = scope.changedParameter!!
        val defaultParam = scope.defaultParameter

        // restartable functions get extra logic and different types of groups from
        // non-restartable functions, and lambdas get no groups at all.
        return when {
            isLambda && isTracked -> visitComposableLambda(
                declaration,
                scope,
                changedParam
            )
            restartable && isTracked -> visitRestartableComposableFunction(
                declaration,
                scope,
                changedParam,
                defaultParam
            )
            else -> visitNonRestartableComposableFunction(
                declaration,
                scope,
                changedParam,
                defaultParam
            )
        }
    }

    // Currently, we make all composable functions restartable by default, unless:
    // 1. They are inline
    // 2. They have a return value (may get relaxed in the future)
    // 3. They are a lambda (we use RestartableFunction<...> class for this instead)
    // 4. They are annotated as @ComposableContract(restartable = false)
    private fun IrFunction.shouldBeRestartable(): Boolean {
        // Only insert observe scopes in non-empty composable function
        if (body == null)
            return false

        val descriptor = descriptor

        // Do not insert observe scope in an inline function
        if (descriptor.isInline)
            return false

        if (descriptor.composableRestartableContract() == false)
            return false

        // Do not insert an observe scope in an inline composable lambda
        descriptor.findPsi()?.let { psi ->
            (psi as? KtFunctionLiteral)?.let {
                if (InlineUtil.isInlinedArgument(
                        it,
                        context.bindingContext,
                        false
                    )
                )
                    return false
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

    private fun IrFunction.shouldElideGroups(): Boolean {
        var readOnly = descriptor.composableReadonlyContract()
        if (readOnly == null && this is IrSimpleFunction) {
            readOnly = correspondingPropertySymbol
                ?.owner
                ?.descriptor
                ?.composableReadonlyContract()
        }
        return readOnly == true
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
        changedParam: IrChangedBitMaskValue,
        defaultParam: IrDefaultBitMaskValue?
    ): IrStatement {
        val body = declaration.body!!

        val elideGroups = declaration.shouldElideGroups()

        val skipPreamble = mutableStatementContainer()
        val bodyPreamble = mutableStatementContainer()

        scope.dirty = changedParam

        val realParams = declaration.valueParameters.take(scope.realValueParamCount)

        buildStatementsForSkippingAndDefaults(
            body,
            skipPreamble,
            bodyPreamble,
            false,
            realParams,
            scope,
            changedParam,
            changedParam,
            defaultParam,
            booleanArrayOf()
        )

        realParams.forEach {
            // we want to remove the default expression from the function. This will prevent
            // the kotlin compiler from doing its own default handling, which we don't need.
            it.defaultValue = null
        }

        var (transformed, returnVar) = body.asBodyAndResultVar()

        transformed = transformed.transformChildren()

        if (!elideGroups) scope.realizeGroup(::irEndReplaceableGroup)

        declaration.body = IrBlockBodyImpl(
            body.startOffset,
            body.endOffset,
            listOfNotNull(
                if (!elideGroups)
                    irStartReplaceableGroup(body, irGet(scope.keyParameter!!))
                else
                    null,
                *skipPreamble.statements.toTypedArray(),
                *bodyPreamble.statements.toTypedArray(),
                *transformed.statements.toTypedArray(),
                if (!elideGroups) irEndReplaceableGroup() else null,
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
        changedParam: IrChangedBitMaskValue
    ): IrStatement {
        // no group, since restartableFunction should already create one
        // no default logic
        val body = declaration.body!!
        val skipPreamble = mutableStatementContainer()
        val bodyPreamble = mutableStatementContainer()

        val realParams = declaration.valueParameters.take(scope.realValueParamCount)

        val realParamsIncludingThis = realParams + listOfNotNull(
            declaration.extensionReceiverParameter
        )

        // boolean array mapped to parameters. true indicates that the type is unstable
        val unstableMask = realParams.map {
            !it.type.toKotlinType().isStable()
        }.toBooleanArray()

        // we start off assuming that we *can* skip execution of the function
        var canSkipExecution = unstableMask.none { it } && declaration.returnType.isUnit()

        // if the function can never skip, or there are no parameters to test, then we
        // don't need to have the dirty parameter locally since it will never be different from
        // the passed in `changed` parameter.
        val dirty = if (canSkipExecution && realParamsIncludingThis.isNotEmpty())
            // NOTE(lmr): Technically, dirty is a mutable variable, but we don't want to mark it
            // as one since that will cause a `Ref<Int>` to get created if it is captured. Since
            // we know we will never be mutating this variable _after_ it gets captured, we can
            // safely mark this as `isVar = false`.
            changedParam.irCopyToTemporary(
                isVar = false,
                nameHint = "\$dirty",
                exactName = true
            ).also {
                skipPreamble.statements.addAll(it.asStatements())
            }
        else
            changedParam

        scope.dirty = dirty

        buildStatementsForSkippingAndDefaults(
            body,
            skipPreamble,
            bodyPreamble,
            canSkipExecution,
            realParams,
            scope,
            dirty,
            changedParam,
            null,
            unstableMask
        )

        val (nonReturningBody, returnVar) = body.asBodyAndResultVar()

        // we must transform the body first, since that will allow us to see whether or not we
        // are using the dispatchReceiverParameter or the extensionReceiverParameter
        val transformed = nonReturningBody.transformChildren()

        if (declaration.extensionReceiverParameter != null) {
            canSkipExecution = buildStatementsForSkippingThisParameter(
                declaration.extensionReceiverParameter!!,
                scope.extensionReceiverUsed,
                canSkipExecution,
                skipPreamble,
                changedParam,
                dirty,
                scope.realValueParamCount
            )
        }

        if (canSkipExecution) {
            // We CANNOT skip if any of the following conditions are met
            // 1. if any of the stable parameters have *differences* from last execution.
            // 2. if the composer.skipping call returns false
            val shouldExecute = irOrOr(
                scope.dirty!!.irHasDifferences(),
                irNot(irIsSkipping())
            )

            val transformedBody = irIfThenElse(
                condition = shouldExecute,
                thenPart = irBlock(
                    type = context.irBuiltIns.unitType,
                    statements = transformed.statements
                ),
                elsePart = irSkipToGroupEnd()
            )
            declaration.body = IrBlockBodyImpl(
                body.startOffset,
                body.endOffset,
                listOfNotNull(
                    *skipPreamble.statements.toTypedArray(),
                    *bodyPreamble.statements.toTypedArray(),
                    transformedBody,
                    returnVar?.let { irReturn(declaration.symbol, irGet(it)) }
                )
            )
        } else {
            declaration.body = IrBlockBodyImpl(
                body.startOffset,
                body.endOffset,
                listOfNotNull(
                    *skipPreamble.statements.toTypedArray(),
                    *bodyPreamble.statements.toTypedArray(),
                    transformed,
                    returnVar?.let { irReturn(declaration.symbol, irGet(it)) }
                )
            )
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
        changedParam: IrChangedBitMaskValue,
        defaultParam: IrDefaultBitMaskValue?
    ): IrStatement {
        val body = declaration.body!!
        val skipPreamble = mutableStatementContainer()
        val bodyPreamble = mutableStatementContainer()

        // these are the parameters excluding the synthetic ones that we generate for compose.
        // These are the only parameters we want to consider in skipping calculations
        val realParams = declaration.valueParameters.take(scope.realValueParamCount)

        val thisParams = listOfNotNull(
            declaration.extensionReceiverParameter,
            declaration.dispatchReceiverParameter
        )

        val realParamsIncludingThis = realParams + thisParams

        // we start off assuming that we *can* skip execution of the function
        var canSkipExecution = true

        // boolean array mapped to parameters. true indicates that the type is unstable
        val unstableMask = realParams.map {
            val isStable = (it.varargElementType ?: it.type).toKotlinType().isStable()
            if (!isStable && !it.hasDefaultValueSafe()) {
                // if it has non-optional unstable params, the function can never skip
                canSkipExecution = false
            }
            !isStable
        }.toBooleanArray()

        // if the function can never skip, or there are no parameters to test, then we
        // don't need to have the dirty parameter locally since it will never be different from
        // the passed in `changed` parameter.
        val dirty = if (canSkipExecution && realParamsIncludingThis.isNotEmpty())
            // NOTE(lmr): Technically, dirty is a mutable variable, but we don't want to mark it
            // as one since that will cause a `Ref<Int>` to get created if it is captured. Since
            // we know we will never be mutating this variable _after_ it gets captured, we can
            // safely mark this as `isVar = false`.
            changedParam.irCopyToTemporary(
                isVar = false,
                nameHint = "\$dirty",
                exactName = true
            ).also {
                skipPreamble.statements.addAll(it.asStatements())
            }
        else
            changedParam

        scope.dirty = dirty

        buildStatementsForSkippingAndDefaults(
            body,
            skipPreamble,
            bodyPreamble,
            canSkipExecution,
            realParams,
            scope,
            dirty,
            changedParam,
            defaultParam,
            unstableMask
        )

        realParams.forEach {
            // we want to remove the default expression from the function. This will prevent
            // the kotlin compiler from doing its own default handling, which we don't need.

            // NOTE: we are doing this AFTER buildStatementsForSkipping, because the default
            // value is used in those calculations
            it.defaultValue = null
        }

        val (nonReturningBody, returnVar) = body.asBodyAndResultVar()

        val end = {
            irEndRestartGroupAndUpdateScope(
                scope,
                changedParam,
                defaultParam,
                scope.realValueParamCount
            )
        }

        // we must transform the body first, since that will allow us to see whether or not we
        // are using the dispatchReceiverParameter or the extensionReceiverParameter
        val transformed = nonReturningBody.transformChildren()

        var slotIndex = scope.realValueParamCount

        if (declaration.extensionReceiverParameter != null) {
            canSkipExecution = buildStatementsForSkippingThisParameter(
                declaration.extensionReceiverParameter!!,
                scope.extensionReceiverUsed,
                canSkipExecution,
                skipPreamble,
                changedParam,
                dirty,
                slotIndex++
            )
        }

        if (declaration.dispatchReceiverParameter != null) {
            canSkipExecution = buildStatementsForSkippingThisParameter(
                declaration.dispatchReceiverParameter!!,
                scope.dispatchReceiverUsed,
                canSkipExecution,
                skipPreamble,
                changedParam,
                dirty,
                slotIndex
            )
        }

        // if it has non-optional unstable params, the function can never skip, so we always
        // execute the body. Otherwise, we wrap the body in an if and only skip when certain
        // conditions are met.
        val transformedBody = if (canSkipExecution) {
            // We CANNOT skip if any of the following conditions are met
            // 1. if any of the stable parameters have *differences* from last execution.
            // 2. if the composer.skipping call returns false
            // 3. if any of the provided parameters to the function were unstable

            // (3) is only necessary to check if we actually have unstable params, so we only
            // generate that check if we need to.

            var shouldExecute = irOrOr(
                scope.dirty!!.irHasDifferences(),
                irNot(irIsSkipping())
            )
            val hasAnyUnstableParams = unstableMask.any { it }

            // if there are unstable params, then we fence the whole expression with a check to
            // see if any of the unstable params were the ones that were provided to the
            // function. If they were, then we short-circuit and always execute
            if (hasAnyUnstableParams && defaultParam != null) {
                shouldExecute = irOrOr(
                    defaultParam.irHasAnyProvidedAndUnstable(unstableMask),
                    shouldExecute
                )
            }

            irIfThenElse(
                condition = shouldExecute,
                thenPart = irBlock(
                    statements = bodyPreamble.statements + transformed.statements
                ),
                elsePart = irSkipToGroupEnd()
            )
        } else irComposite(
            statements = bodyPreamble.statements + transformed.statements
        )

        scope.realizeGroup(end)

        declaration.body = IrBlockBodyImpl(
            body.startOffset,
            body.endOffset,
            listOfNotNull(
                irStartRestartGroup(body, irGet(scope.keyParameter!!)),
                *skipPreamble.statements.toTypedArray(),
                transformedBody,
                if (returnVar == null) end() else null,
                returnVar?.let { irReturn(declaration.symbol, irGet(it)) }
            )
        )

        return declaration
    }

    private fun buildStatementsForSkippingThisParameter(
        thisParam: IrValueParameter,
        isUsed: Boolean,
        canSkipExecution: Boolean,
        preamble: IrStatementContainer,
        changedParam: IrChangedBitMaskValue,
        dirty: IrChangedBitMaskValue,
        index: Int
    ): Boolean {
        val type = thisParam.type
        val isStable = type.toKotlinType().isStable()

        return when {
            !isStable && isUsed -> false
            isStable && isUsed && canSkipExecution && dirty is IrChangedBitMaskVariable -> {
                preamble.statements.add(irIf(
                    // we only call `$composer.changed(...)` on a parameter if the value came in
                    // with an "Uncertain" state AND the value was provided. This is safe to do
                    // because this will remain true or false for *every* execution of the
                    // function, so we will never get a slot table misalignment as a result.
                    condition = irIsUncertain(changedParam, index),
                    body = dirty.irOrSetBitsAtSlot(
                        index,
                        irIfThenElse(
                            context.irBuiltIns.intType,
                            irChanged(irGet(thisParam)),
                            // if the value has changed, update the bits in the slot to be
                            // "Different"
                            thenPart = irConst(ParamState.Different.bitsForSlot(index)),
                            // if the value has not changed, update the bits in the slot to
                            // be "Same"
                            elsePart = irConst(ParamState.Same.bitsForSlot(index))
                        )
                    )
                ))
                true
            }
            !isUsed && canSkipExecution && dirty is IrChangedBitMaskVariable -> {
                // if the param isn't used we can safely ignore it, but if we can skip the
                // execution of the function, then we need to make sure that we are only
                // considering the not-ignored parameters. to do this, we set the changed slot bits
                // to Static
                preamble.statements.add(dirty.irOrSetBitsAtSlot(
                    index,
                    irConst(ParamState.Static.bitsForSlot(index))
                ))
            }
            // nothing changes
            else -> canSkipExecution
        }
    }

    private fun buildStatementsForSkippingAndDefaults(
        sourceElement: IrElement,
        skipPreamble: IrStatementContainer,
        bodyPreamble: IrStatementContainer,
        canSkipExecution: Boolean,
        parameters: List<IrValueParameter>,
        scope: Scope.FunctionScope,
        dirty: IrChangedBitMaskValue,
        changedParam: IrChangedBitMaskValue,
        defaultParam: IrDefaultBitMaskValue?,
        unstableMask: BooleanArray
    ) {
        // we default to true because the absence of a default expression we want to consider as
        // "static"
        val defaultExprIsStatic = BooleanArray(parameters.size) { true }
        val defaultExpr = Array<IrExpression?>(parameters.size) { null }

        // first we create the necessary local variables for default handling.
        val setDefaults = mutableStatementContainer()
        parameters.forEachIndexed { index, param ->
            val defaultValue = param.defaultValue
            if (defaultParam != null && defaultValue != null) {
                val transformedDefault = defaultValue.expression.transform(this, null)

                // we want to call this on the transformed version.
                defaultExprIsStatic[index] = transformedDefault.isStatic()
                defaultExpr[index] = transformedDefault

                // create a new temporary variable with the same name as the parameter itself
                // initialized to the parameter value.
                val varSymbol = if (!canSkipExecution) {
                    // If we can't skip execution, or if the expression is static, there's no need
                    // to separate the assignment of the temporary and the declaration.
                    irTemporary(
                        irIfThenElse(
                            param.type,
                            condition = irGetBit(defaultParam, index),
                            // we need to ensure that this transform runs on the default expression. It
                            // could contain conditional logic as well as composable calls
                            thenPart = transformedDefault,
                            elsePart = irGet(param)
                        ),
                        param.name.identifier,
                        param.type,
                        isVar = false,
                        exactName = true
                    )
                } else {
                    // If we can skip execution, we want to only execute the default expression
                    // in certain cases. as a result, we first create the temp variable, and then
                    // add the logic to set it in the "setDefaults" container.
                    irTemporary(
                        irGet(param),
                        param.name.identifier,
                        param.type,
                        // NOTE(lmr): technically, we end up mutating this variable in the body of
                        // the function. It turns out that the isVar doesn't validate this, but
                        // it does cause the variable to be wrapped in a `Ref` object if it is
                        // captured by a closure. We do NOT want that, and we know that the code
                        // will be correct without it, so we set `isVar = false` here.
                        isVar = false,
                        exactName = true
                    ).also {
                        setDefaults.statements.add(
                            irIf(
                                condition = irGetBit(defaultParam, index),
                                body = irSet(it, transformedDefault)
                            )
                        )
                    }
                }

                // semantically, any reference to the parameter symbol now needs to be remapped
                // to the temporary variable.
                scope.remappedParams[param] = varSymbol

                // in order to propagate the change detection we might perform on this parameter,
                // we need to know which "slot" it is in
                scope.paramsToSlots[varSymbol] = index
                skipPreamble.statements.add(varSymbol)
            } else {
                scope.remappedParams[param] = param
                scope.paramsToSlots[param] = index
            }
        }
        // we start the skipPreamble with all of the changed calls. These need to go at the top
        // of the function's group. Note that these end up getting called *before* default
        // expressions, but this is okay because it will only ever get called on parameters that
        // are provided to the function
        parameters.forEachIndexed { index, param ->
            // varargs get handled separately because they will require their own groups
            if (param.isVararg) return@forEachIndexed
            val defaultValue = param.defaultValue
            if (canSkipExecution && dirty is IrChangedBitMaskVariable) {
                if (unstableMask[index]) {
                    if (defaultParam != null && defaultValue != null) {
                        skipPreamble.statements.add(
                            irIf(
                                condition = irGetBit(defaultParam, index),
                                body = dirty.irOrSetBitsAtSlot(
                                    index,
                                    irConst(ParamState.Same.bitsForSlot(index))
                                )
                            )
                        )
                    }

                    // if the value is unstable, there is no reason for us to store it in the slot table
                    return@forEachIndexed
                }

                val defaultValueIsStatic = defaultExprIsStatic[index]
                val modifyDirtyFromChangedResult = dirty.irOrSetBitsAtSlot(
                    index,
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

                val stmt = if (defaultParam != null && defaultValueIsStatic) {
                    // if the default expression is "static", then we know that if we are using the
                    // default expression, the parameter can be considered "static".
                    irWhen(
                        origin = IrStatementOrigin.IF,
                        branches = listOf(
                            irBranch(
                                condition = irGetBit(defaultParam, index),
                                result = dirty.irOrSetBitsAtSlot(
                                    index,
                                    irConst(ParamState.Static.bitsForSlot(index))
                                )
                            ),
                            irBranch(
                                condition = irIsUncertain(changedParam, index),
                                result = modifyDirtyFromChangedResult
                            )
                        )
                    )
                } else {
                    // we only call `$composer.changed(...)` on a parameter if the value came in
                    // with an "Uncertain" state AND the value was provided. This is safe to do
                    // because this will remain true or false for *every* execution of the
                    // function, so we will never get a slot table misalignment as a result.
                    val condition = if (defaultParam != null) irAndAnd(
                        irIsProvided(defaultParam, index),
                        irIsUncertain(changedParam, index)
                    ) else irIsUncertain(changedParam, index)
                    irIf(
                        condition = condition,
                        body = modifyDirtyFromChangedResult
                    )
                }
                skipPreamble.statements.add(stmt)
            }
        }
        // now we handle the vararg parameters specially since it needs to create a group
        parameters.forEachIndexed { index, param ->
            val varargElementType = param.varargElementType ?: return@forEachIndexed
            if (canSkipExecution && dirty is IrChangedBitMaskVariable) {
                if (unstableMask[index]) {
                    // if the value is unstable, there is no reason for us to store it in the slot table
                    return@forEachIndexed
                }

                // for vararg parameters of stable type, we can store each value in the slot
                // table, but need to generate a group since the size of the array could change
                // over time. In the future, we may want to make an optimization where whether or
                // not the call site had a spread or not and only create groups if it did.

                // composer.startReplaceableGroup(values.size)
                val irGetParamSize = irMethodCall(
                    irGet(param),
                    param.type.classOrNull!!.getPropertyGetter("size")!!.descriptor
                )
                // TODO(lmr): verify this works with default vararg expressions!
                skipPreamble.statements.add(irStartReplaceableGroup(param, irGetParamSize))

                // for (value in values) {
                //     dirty = dirty or if (composer.changed(value)) 0b0100 else 0b0000
                // }
                skipPreamble.statements.add(irForLoop(
                    scope.function.symbol.descriptor,
                    varargElementType,
                    irGet(param)
                ) { loopVar ->
                    dirty.irOrSetBitsAtSlot(
                        index,
                        irIfThenElse(
                            context.irBuiltIns.intType,
                            irChanged(irGet(loopVar)),
                            // if the value has changed, update the bits in the slot to be
                            // "Different".
                            thenPart = irConst(ParamState.Different.bitsForSlot(index)),
                            // if the value has not changed, we are still uncertain if the entire
                            // list of values has gone unchanged or not, so we use Uncertain
                            elsePart = irConst(ParamState.Uncertain.bitsForSlot(index))
                        )
                    )
                })

                // composer.endReplaceableGroup()
                skipPreamble.statements.add(irEndReplaceableGroup())

                // if (dirty and 0b0110 === 0) {
                //   dirty = dirty or 0b0010
                // }
                skipPreamble.statements.add(irIf(
                    condition = irIsUncertain(dirty, index),
                    body = dirty.irOrSetBitsAtSlot(
                        index,
                        irConst(ParamState.Same.bitsForSlot(index))
                    )
                ))
            }
        }
        // after all of this, we need to potentially wrap the default setters in a group and if
        // statement, to make sure that defaults are only executed when they need to be.
        if (!canSkipExecution || defaultExprIsStatic.all { it }) {
            // if we don't skip execution ever, then we don't need these groups at all.
            // Additionally, if all of the defaults are static, we can avoid creating the groups
            // as well.
            // NOTE(lmr): should we still wrap this in an if statement to be safe???
            bodyPreamble.statements.addAll(setDefaults.statements)
        } else if (setDefaults.statements.isNotEmpty()) {
            // otherwise, we wrap the whole thing in an if expression with a skip
            bodyPreamble.statements.add(
                irIfThenElse(
                    // this prevents us from re-executing the defaults if this function is getting
                    // executed from a recomposition
                    // if (%changed and 0b0001 === 0 || %composer.defaultsInvalid) {
                    condition = irOrOr(
                        irEqual(changedParam.irLowBit(), irConst(0)),
                        irDefaultsInvalid()
                    ),
                    // set all of the default temp vars
                    thenPart = irBlock(
                        statements = listOf(
                            irStartDefaults(sourceElement),
                            *setDefaults.statements.toTypedArray(),
                            irEndDefaults()
                        )
                    ),
                    // composer.skipCurrentGroup()
                    elsePart = irSkipCurrentGroup()
                )
            )
        }
    }

    private fun irEndRestartGroupAndUpdateScope(
        scope: Scope.FunctionScope,
        changedParam: IrChangedBitMaskValue,
        defaultParam: IrDefaultBitMaskValue?,
        numRealValueParameters: Int
    ): IrExpression {
        val function = scope.function

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

        val keyParameter = ValueParameterDescriptorImpl(
            containingDeclaration = lambdaDescriptor,
            original = null,
            index = 1,
            annotations = Annotations.EMPTY,
            name = KtxNameConventions.KEY_PARAMETER,
            outType = builtIns.int,
            declaresDefaultValue = false,
            isCrossinline = false,
            isNoinline = false,
            varargElementType = null,
            source = SourceElement.NO_SOURCE
        )

        val ignoredChangedParameter = ValueParameterDescriptorImpl(
            containingDeclaration = lambdaDescriptor,
            original = null,
            index = 2,
            annotations = Annotations.EMPTY,
            name = KtxNameConventions.CHANGED_PARAMETER,
            outType = builtIns.int,
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
                listOf(passedInComposerParameter, keyParameter, ignoredChangedParameter),
                updateScopeBlockType,
                Modality.FINAL,
                Visibilities.LOCAL
            )
        }

        val parameterCount = function.symbol.descriptor.valueParameters.size
        val keyIndex = numRealValueParameters + 1
        val changedIndex = keyIndex + 1
        val defaultIndex = changedIndex + changedParamCount(
            numRealValueParameters,
            function.thisParamCount
        )

        if (defaultParam == null) {
            require(parameterCount == defaultIndex) // param count is 1-based, index is 0-based
        } else {
            require(parameterCount == defaultIndex + defaultParamCount(numRealValueParameters))
        }

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
                composerTypeDescriptor
                    .defaultType
                    .replaceArgumentsWithStarProjections()
                    .toIrType()
                    .makeNullable()
            )
            fn.addValueParameter(
                KtxNameConventions.KEY_PARAMETER.identifier,
                context.irBuiltIns.intType
            )
            fn.addValueParameter(
                "\$force",
                context.irBuiltIns.intType
            )
            fn.body = localIrBuilder.irBlockBody {

                fun remappedParam(index: Int) = function.valueParameters[index].let {
                    scope.remappedParams[it] ?: it
                }

                // Call the function again with the same parameters
                +irReturn(irCall(function.symbol).apply {
                    symbol.owner
                        .valueParameters
                        .forEachIndexed { index, param ->
                            if (param.isVararg) {
                                putValueArgument(
                                    index,
                                    IrVarargImpl(
                                        UNDEFINED_OFFSET,
                                        UNDEFINED_OFFSET,
                                        param.type,
                                        param.varargElementType!!,
                                        elements = listOf(
                                            IrSpreadElementImpl(
                                                UNDEFINED_OFFSET,
                                                UNDEFINED_OFFSET,
                                                irGet(remappedParam(index))
                                            )
                                        )
                                    )
                                )
                            } else {
                                // NOTE(lmr): should we be using the parameter here, or the temporary
                                // with the default value?
                                putValueArgument(index, irGet(remappedParam(index)))
                            }
                        }

                    // new composer
                    putValueArgument(
                        numRealValueParameters,
                        irGet(fn.valueParameters[0])
                    )

                    putValueArgument(
                        keyIndex,
                        irGet(fn.valueParameters[1])
                    )

                    // the call in updateScope needs to *always* have the low bit set to 1.
                    // This ensures that the body of the function is actually executed.
                    changedParam.putAsValueArgumentInWithLowBit(
                        this,
                        changedIndex,
                        lowBit = true
                    )

                    defaultParam?.putAsValueArgumentIn(this, defaultIndex)

                    extensionReceiver = function.extensionReceiverParameter?.let { irGet(it) }
                    dispatchReceiver = outerReceiver?.let { irGet(it) }
                    function.typeParameters.forEachIndexed { index, parameter ->
                        putTypeArgument(index, parameter.defaultType)
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

    private fun irIsSkipping() =
        irMethodCall(irCurrentComposer(), isSkippingDescriptor.getter!!)
    private fun irDefaultsInvalid() =
        irMethodCall(irCurrentComposer(), defaultsInvalidDescriptor.getter!!)

    private fun irIsProvided(default: IrDefaultBitMaskValue, slot: Int) =
        irEqual(default.irIsolateBitAtIndex(slot), irConst(0))

    // %changed and 0b11 == 0
    private fun irIsUncertain(changed: IrChangedBitMaskValue, slot: Int) = irEqual(
        changed.irIsolateBitsAtSlot(slot),
        irConst(0)
    )

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

    private fun IrBody.asBodyAndResultVar(): Pair<IrContainerExpressionBase, IrVariable?> {
        val original = IrCompositeImpl(
            startOffset,
            endOffset,
            context.irBuiltIns.unitType,
            null,
            statements
        )
        var block: IrStatementContainer? = original
        var expr: IrStatement? = block?.statements?.lastOrNull()
        while (expr != null && block != null) {
            if (expr is IrReturn) {
                block.statements.pop()
                return if (expr.value.type.isUnitOrNullableUnit()) {
                    block.statements.add(expr.value)
                    original to null
                } else {
                    val temp = irTemporary(expr.value)
                    block.statements.add(temp)
                    original to temp
                }
            }
            if (expr !is IrBlock)
                return original to null
            block = expr
            expr = block.statements.lastOrNull()
        }
        return original to null
    }

    override fun visitProperty(declaration: IrProperty): IrStatement {
        val scope = Scope.PropertyScope(declaration.name)
        try {
            scopeStack.push(scope)
            return super.visitProperty(declaration)
        } finally {
            val popped = scopeStack.pop()
            require(popped == scope) { "Unbalanced scope stack" }
        }
    }

    override fun visitField(declaration: IrField): IrStatement {
        val scope = Scope.FieldScope(declaration.name)
        try {
            scopeStack.push(scope)
            return super.visitField(declaration)
        } finally {
            val popped = scopeStack.pop()
            require(popped == scope) { "Unbalanced scope stack" }
        }
    }

    override fun visitFile(declaration: IrFile): IrFile {
        val scope = Scope.FileScope(declaration.fqName)
        try {
            scopeStack.push(scope)
            return super.visitFile(declaration)
        } finally {
            val popped = scopeStack.pop()
            require(popped == scope) { "Unbalanced scope stack" }
        }
    }

    override fun visitDeclaration(declaration: IrDeclaration): IrStatement {
        when (declaration) {
            is IrField,
            is IrProperty,
            is IrFunction,
            is IrClass -> {
                // these declarations get scopes, but they are handled individually
                return super.visitDeclaration(declaration)
            }
            is IrTypeAlias,
            is IrEnumEntry,
            is IrAnonymousInitializer,
            is IrTypeParameter,
            is IrLocalDelegatedProperty,
            is IrValueDeclaration -> {
                // these declarations do not create new "scopes", so we do nothing
                return super.visitDeclaration(declaration)
            }
            else -> error("Unhandled declaration! ${declaration::class.java.simpleName}")
        }
    }

    private fun nearestComposer(): IrValueParameter {
        loop@ for (scope in scopeStack.asReversed()) {
            when (scope) {
                is Scope.FunctionScope -> {
                    val result = scope.composerParameter
                    if (result != null) return result
                }
            }
        }
        error("Not in a composable function \n${printScopeStack()}")
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

    private fun irStartReplaceableGroup(
        element: IrElement,
        key: IrExpression = element.irSourceKey()
    ): IrExpression {
        return irMethodCall(
            irCurrentComposer(),
            startReplaceableDescriptor,
            element.startOffset,
            element.endOffset
        ).also {
            it.putValueArgument(0, key)
        }
    }

    private fun irStartDefaults(element: IrElement): IrExpression {
        return irMethodCall(
            irCurrentComposer(),
            startDefaultsDescriptor,
            element.startOffset,
            element.endOffset
        )
    }

    private fun irStartRestartGroup(
        element: IrElement,
        key: IrExpression = element.irSourceKey()
    ): IrExpression {
        return irMethodCall(
            irCurrentComposer(),
            startRestartGroupDescriptor,
            element.startOffset,
            element.endOffset
        ).also {
            it.putValueArgument(0, key)
        }
    }

    private fun irEndRestartGroup(): IrExpression {
        return irMethodCall(irCurrentComposer(), endRestartGroupDescriptor)
    }

    private fun irChanged(value: IrExpression): IrExpression {
        // compose has a unique opportunity to avoid inline class boxing for changed calls, since
        // we know that the only thing that we are detecting here is "changed or not", we can
        // just as easily pass in the underlying value, which will avoid boxing to check for
        // equality on recompositions. As a result here we want to pass in the underlying
        // property value for inline classes, not the instance itself. The inline class lowering
        // will turn this into just passing the wrapped value later on. If the type is already
        // boxed, then we don't want to unnecessarily _unbox_ it. Note that if Kotlin allows for
        // an overridden equals method of inline classes in the future, we may have to avoid the
        // boxing in a different way.
        val type = value.type.unboxInlineClass()
        val expr = value.unboxValueIfInline()
        val descriptor = type
            .toKotlinType()
            .toPrimitiveType()
            .let { changedDescriptors[it] } ?: changedDescriptor
        return irMethodCall(irCurrentComposer(), descriptor).also {
            it.putValueArgument(0, expr)
        }
    }

    private fun irSkipToGroupEnd(): IrExpression {
        return irMethodCall(irCurrentComposer(), skipToGroupEndDescriptor)
    }

    private fun irSkipCurrentGroup(): IrExpression {
        return irMethodCall(irCurrentComposer(), skipCurrentGroupDescriptor)
    }

    private fun irEndReplaceableGroup(): IrExpression {
        return irMethodCall(irCurrentComposer(), endReplaceableDescriptor)
    }

    private fun irEndDefaults(): IrExpression {
        return irMethodCall(irCurrentComposer(), endDefaultsDescriptor)
    }

    private fun irStartMovableGroup(element: IrElement, joinedData: IrExpression): IrExpression {
        return irMethodCall(
            irCurrentComposer(),
            startMovableDescriptor,
            element.startOffset,
            element.endOffset
        ).also {
            it.putValueArgument(0, element.irSourceKey())
            it.putValueArgument(1, joinedData)
        }
    }

    private fun irEndMovableGroup(): IrExpression {
        return irMethodCall(irCurrentComposer(), endMovableDescriptor)
    }

    private fun irJoinKeyChain(keyExprs: List<IrExpression>): IrExpression {
        return keyExprs.reduce { accumulator, value ->
            irMethodCall(irCurrentComposer(), joinKeyDescriptor).apply {
                putValueArgument(0, accumulator)
                putValueArgument(1, value)
            }
        }
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

    private fun irCall(
        descriptor: FunctionDescriptor,
        startOffset: Int = UNDEFINED_OFFSET,
        endOffset: Int = UNDEFINED_OFFSET
    ): IrCall {
        val type = descriptor.returnType?.toIrType() ?: error("Expected a return type")
        val symbol = referenceFunction(descriptor)
        return IrCallImpl(
            startOffset,
            endOffset,
            type,
            symbol
        )
    }

    private fun irMethodCall(
        target: IrExpression,
        descriptor: FunctionDescriptor,
        startOffset: Int = UNDEFINED_OFFSET,
        endOffset: Int = UNDEFINED_OFFSET
    ): IrCall {
        return irCall(descriptor, startOffset, endOffset).apply {
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
        return irTemporary(
            scope.function.symbol.descriptor,
            value,
            name,
            irType,
            isVar
        )
    }

    private fun IrExpression.asReplaceableGroup(scope: Scope.BlockScope): IrExpression {
        // if the scope has no composable calls, then the only important thing is that a
        // start/end call gets executed. as a result, we can just put them both at the top of
        // the group, and we don't have to deal with any of the complicated jump logic that
        // could be inside of the block
        if (!scope.hasComposableCalls && !scope.hasReturn && !scope.hasJump) {
            return wrap(
                before = listOf(irStartReplaceableGroup(this), irEndReplaceableGroup())
            )
        }
        scope.realizeGroup(::irEndReplaceableGroup)
        return when {
            // if the scope ends with a return call, then it will get properly ended if we
            // just push the end call on the scope because of the way returns get transformed in
            // this class. As a result, here we can safely just "prepend" the start call
            endsWithReturnOrJump() -> {
                wrap(before = listOf(irStartReplaceableGroup(this)))
            }
            // otherwise, we want to push an end call for any early returns/jumps, but also add
            // an end call to the end of the group
            else -> {
                wrap(
                    before = listOf(irStartReplaceableGroup(this)),
                    after = listOf(irEndReplaceableGroup())
                )
            }
        }
    }

    private fun IrExpression.wrap(
        before: List<IrExpression> = emptyList(),
        after: List<IrExpression> = emptyList()
    ): IrExpression {
        return if (after.isEmpty() || type.isNothing() || type.isUnitOrNullableUnit()) {
            wrap(type, before, after)
        } else {
            val tmpVar = irTemporary(this, nameHint = "group")
            tmpVar.wrap(
                type,
                before,
                after + irGet(tmpVar)
            )
        }
    }

    private fun IrExpression.wrapDeferred(
        before: List<IrExpression> = emptyList(),
        after: List<IrExpression> = emptyList()
    ): IrExpression {
        return if (type.isNothing() || type.isUnitOrNullableUnit()) {
            wrap(type, before, after)
        } else {
            val tmpVar = irTemporary(this, nameHint = "group")
            tmpVar.wrap(
                type,
                before,
                after + irGet(tmpVar)
            )
        }
    }

    private fun IrStatement.wrap(
        type: IrType,
        before: List<IrExpression> = emptyList(),
        after: List<IrExpression> = emptyList()
    ): IrExpression {
        return IrBlockImpl(
            startOffset,
            endOffset,
            type,
            null,
            before + this + after
        )
    }

    private fun IrExpression.asCoalescableGroup(scope: Scope.BlockScope): IrExpression {
        val before = mutableStatementContainer()
        val after = mutableStatementContainer()

        // Since this expression produces a dynamic number of groups, we may need to wrap it with
        // a group directly. We don't know that for sure yet, so we provide the parent scope with
        // handlers to do that if it ends up needing to.
        encounteredCoalescableGroup(
            scope,
            realizeGroup = {
                before.statements.add(irStartReplaceableGroup(this))
                after.statements.add(irEndReplaceableGroup())
            },
            makeEnd = ::irEndReplaceableGroup
        )
        return wrapDeferred(
            listOf(before),
            listOf(after)
        )
    }

    private fun mutableStatementContainer(): IrContainerExpressionBase {
        // NOTE(lmr): It's important to use IrComposite here so that we don't introduce any new
        // scopes
        return IrCompositeImpl(
            UNDEFINED_OFFSET,
            UNDEFINED_OFFSET,
            context.irBuiltIns.unitType
        )
    }

    private fun encounteredComposableCall() {
        loop@ for (scope in scopeStack.asReversed()) {
            when (scope) {
                is Scope.FunctionScope -> {
                    scope.markComposableCall()
                    if (scope.isInlinedLambda) {
                        continue@loop
                    }
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

    private fun encounteredCapturedComposableCall() {
        loop@ for (scope in scopeStack.asReversed()) {
            when (scope) {
                is Scope.CaptureScope -> {
                    scope.markCapturedComposableCall()
                    break@loop
                }
            }
        }
    }

    private fun encounteredCoalescableGroup(
        coalescableScope: Scope.BlockScope,
        realizeGroup: () -> Unit,
        makeEnd: () -> IrExpression
    ) {
        loop@ for (scope in scopeStack.asReversed()) {
            when (scope) {
                is Scope.FunctionScope -> {
                    scope.markCoalescableGroup(coalescableScope, realizeGroup, makeEnd)
                    if (scope.isInlinedLambda) {
                        continue@loop
                    }
                    break@loop
                }
                is Scope.BlockScope -> {
                    scope.markCoalescableGroup(coalescableScope, realizeGroup, makeEnd)
                    break@loop
                }
                else -> error("Unexpected scope type")
            }
        }
    }

    private fun encounteredReturn(
        symbol: IrReturnTargetSymbol,
        extraEndLocation: (IrExpression) -> Unit
    ) {
        loop@ for (scope in scopeStack.asReversed()) {
            when (scope) {
                is Scope.FunctionScope -> {
                    if (scope.function == symbol.owner) {
                        scope.markReturn(extraEndLocation)
                        break@loop
                    } else {
                        continue@loop
                    }
                }
                is Scope.BlockScope -> {
                    scope.markReturn(extraEndLocation)
                }
            }
        }
    }

    private fun encounteredJump(jump: IrBreakContinue, extraEndLocation: (IrExpression) -> Unit) {
        loop@ for (scope in scopeStack.asReversed()) {
            when (scope) {
                is Scope.ClassScope -> error("Unexpected Class Scope encountered")
                is Scope.FunctionScope -> {
                    if (scope.isInlinedLambda) {
                        continue@loop
                    }
                    error("Unexpected Function Scope encountered")
                }
                is Scope.LoopScope -> {
                    if (jump.loop == scope.loop) {
                        break@loop
                    }
                    scope.markJump(extraEndLocation)
                }
                is Scope.BlockScope -> {
                    scope.markJump(extraEndLocation)
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

    data class ParamMeta(
        var isVararg: Boolean = false,
        var isProvided: Boolean = false,
        var isStatic: Boolean = false,
        var isCertain: Boolean = false,
        var maskSlot: Int = -1,
        var maskParam: IrChangedBitMaskValue? = null
    )

    fun paramMetaOf(arg: IrExpression, isProvided: Boolean): ParamMeta {
        val meta = ParamMeta(isProvided = isProvided)
        populateParamMeta(arg, meta)
        return meta
    }

    private fun populateParamMeta(arg: IrExpression, meta: ParamMeta) {
        when {
            arg.isStatic() -> meta.isStatic = true
            arg is IrGetValue -> {
                val owner = arg.symbol.owner
                val found = extractParamMetaFromScopes(meta, owner)
                if (!found) {
                    when (owner) {
                        is IrVariable -> {
                            if (owner.isConst) {
                                meta.isStatic = true
                            } else if (!owner.isVar && owner.initializer != null) {
                                populateParamMeta(owner.initializer!!, meta)
                            }
                        }
                    }
                }
            }
        }
    }

    override fun visitBlock(expression: IrBlock): IrExpression {
        return when (expression.origin) {
            IrStatementOrigin.FOR_LOOP -> {
                // The psi2ir phase will turn for loops into a block, so:
                //
                //   for (loopVar in <someIterable>)
                //
                // gets transformed into
                //
                //   // #1: The "header"
                //   val it = <someIterable>.iterator()
                //
                //   // #2: The inner while loop
                //   while (it.hasNext()) {
                //     val loopVar = it.next()
                //     // Loop body
                //   }
                //
                // Additionally, the IR lowering phase will take this block and optimize it
                // for some shapes of for loops. What we want to do is keep this original
                // shape in tact so that we don't ruin some of these optimizations.
                val statements = expression.statements

                require(statements.size == 2) {
                    "Expected 2 statements in for-loop block"
                }
                val oldVar = statements[0] as IrVariable
                require(oldVar.origin == IrDeclarationOrigin.FOR_LOOP_ITERATOR) {
                    "Expected FOR_LOOP_ITERATOR origin for iterator variable"
                }
                val newVar = oldVar.transform(this, null)

                val oldLoop = statements[1] as IrWhileLoop
                require(oldLoop.origin == IrStatementOrigin.FOR_LOOP_INNER_WHILE) {
                    "Expected FOR_LOOP_INNER_WHILE origin for while loop"
                }

                val newLoop = oldLoop.transform(this, null)

                if (newVar == oldVar && newLoop == oldLoop)
                    expression
                else if (newLoop is IrBlock) {
                    require(newLoop.statements.size == 3)
                    val before = newLoop.statements[0] as IrContainerExpressionBase
                    val loop = newLoop.statements[1] as IrWhileLoop
                    val after = newLoop.statements[2] as IrContainerExpressionBase

                    val result = mutableStatementContainer()
                    result.statements.addAll(listOf(
                        before,
                        irBlock(
                            type = expression.type,
                            origin = IrStatementOrigin.FOR_LOOP,
                            statements = listOf(
                                newVar,
                                loop
                            )
                        ),
                        after
                    ))
                    result
                } else {
                    error("Expected transformed loop to be an IrBlock")
                }
            }
            else -> super.visitBlock(expression)
        }
    }

    override fun visitCall(expression: IrCall): IrExpression {
        if (expression.isTransformedComposableCall() || expression.isSyntheticComposableCall()) {
            return visitComposableCall(expression)
        }
        if (expression.symbol.owner.isInline) {
            // if it is not a composable call but it is an inline function, then we allow
            // composable calls to happen inside of the inlined lambdas. This means that we have
            // some control flow analysis to handle there as well. We wrap the call in a
            // CallScope and coalescable group if the call has any composable invocations inside
            // of it..
            val captureScope = withScope(Scope.CaptureScope()) {
                expression.transformChildrenVoid()
            }
            return if (captureScope.hasCapturedComposableCall) {
                expression.asCoalescableGroup(captureScope)
            } else {
                expression
            }
        } else {
            return super.visitCall(expression)
        }
    }

    private fun visitComposableCall(expression: IrCall): IrExpression {
        encounteredComposableCall()
        if (expression.symbol.descriptor.fqNameSafe == ComposeFqNames.key) {
            return visitKeyCall(expression)
        }
        // it's important that we transform all of the parameters here since this will cause the
        // IrGetValue's of remapped default parameters to point to the right variable.
        expression.transformChildrenVoid()

        val ownerFn = expression.symbol.owner
        val numValueParams = ownerFn.valueParameters.size
        val numDefaults: Int
        val numChanged: Int
        val numRealValueParams: Int

        if (expression.isInvoke()) {
            // in the case of an invoke, all of the parameters are going to be type parameter
            // args which won't have special names. In this case, we know that the values cannot
            // be defaulted though, so we can calculate the number of real parameters based on
            // the total number of parameters
            numDefaults = 0
            numChanged = changedParamCountFromTotal(numValueParams + ownerFn.thisParamCount)
            numRealValueParams = numValueParams -
                    1 - // composer param
                    1 - // key param
                    numChanged
        } else {
            val hasDefaults = ownerFn.valueParameters.any {
                it.name == KtxNameConventions.DEFAULT_PARAMETER
            }
            numRealValueParams = ownerFn.valueParameters.indexOfLast {
                !it.name.asString().startsWith('$')
            } + 1
            numDefaults = if (hasDefaults) defaultParamCount(numRealValueParams) else 0
            numChanged = changedParamCount(numRealValueParams, ownerFn.thisParamCount)
        }

        require(
            numRealValueParams +
                    1 + // composer param
                    1 + // key param
                    numChanged +
                    numDefaults == numValueParams)

        val composerIndex = numRealValueParams
        val keyIndex = composerIndex + 1
        val changedArgIndex = keyIndex + 1
        val defaultArgIndex = changedArgIndex + numChanged
        val defaultArgs = (defaultArgIndex until numValueParams).map {
            expression.getValueArgument(it)
        }

        val defaultMasks = defaultArgs.map {
            when (it) {
                !is IrConst<*> -> error("Expected default mask to be a const")
                else -> it.value as? Int ?: error("Expected default mask to be an Int")
            }
        }.ifEmpty { listOf(0b0) }

        val paramMeta = mutableListOf<ParamMeta>()

        for (index in 0 until numRealValueParams) {
            val arg = expression.getValueArgument(index)
            if (arg == null) {
                val param = expression.symbol.owner.valueParameters[index]
                if (param.varargElementType == null) {
                    // ComposerParamTransformer should not allow for any null arguments on a composable
                    // invocation unless the parameter is vararg. If this is null here, we have
                    // missed something.
                    error("Unexpected null argument for composable call")
                } else {
                    paramMeta.add(ParamMeta(isVararg = true))
                    continue
                }
            }
            val bitIndex = defaultsBitIndex(index)
            val maskValue = defaultMasks[defaultsParamIndex(index)]
            val meta = paramMetaOf(arg, isProvided = maskValue and (0b1 shl bitIndex) == 0)

            paramMeta.add(meta)
        }

        val extensionMeta = expression.extensionReceiver?.let { paramMetaOf(it, isProvided = true) }
        val dispatchMeta = expression.dispatchReceiver?.let { paramMetaOf(it, isProvided = true) }

        val changedParams = buildChangedParamsForCall(
            paramMeta,
            extensionMeta,
            dispatchMeta
        )

        expression.putValueArgument(
            keyIndex,
            expression.irSourceKey()
        )

        changedParams.forEachIndexed { i, param ->
            expression.putValueArgument(changedArgIndex + i, param)
        }

        return expression
    }

    private fun visitKeyCall(expression: IrCall): IrExpression {
        val keyArgs = mutableListOf<IrExpression>()
        var blockArg: IrExpression? = null
        for (i in 0 until expression.valueArgumentsCount) {
            val param = expression.symbol.owner.valueParameters[i]
            val arg = expression.getValueArgument(i)
                ?: error("Unexpected null argument found on key call")
            if (param.name.asString().startsWith('$'))
                // we are done. synthetic args go at
                // the end
                break

            when {
                param.name.identifier == "block" -> {
                    blockArg = arg
                }
                arg is IrVararg -> {
                    keyArgs.addAll(arg.elements.mapNotNull { it as? IrExpression })
                }
                else -> {
                    keyArgs.add(arg)
                }
            }
        }
        val before = mutableStatementContainer()
        val after = mutableStatementContainer()

        if (blockArg !is IrFunctionExpression) error("Expected function expression")

        val (block, resultVar) = blockArg.function.body!!.asBodyAndResultVar()

        var transformed: IrExpression = block

        withScope(Scope.BranchScope()) {
            transformed = transformed.transform(this, null)
        }

        return irBlock(
            type = expression.type,
            statements = listOfNotNull(
                before,
                irStartMovableGroup(
                    expression,
                    irJoinKeyChain(keyArgs.map { it.transform(this, null) })
                ),
                block,
                irEndMovableGroup(),
                after,
                resultVar?.let { irGet(resultVar) }
            )
        )
    }

    private fun IrExpression.isStatic(): Boolean {
        return when (this) {
            // A constant by definition is static
            is IrConst<*> -> true
            // We want to consider all enum values as static
            is IrGetEnumValue -> true
            // Getting a companion object or top level object can be considered static if the
            // type of that object is Stable. (`Modifier` for instance is a common example)
            is IrGetObjectValue -> symbol.owner.superTypes.any { it.toKotlinType().isStable() }
            is IrConstructorCall -> {
                // special case constructors of inline classes as static if their underlying
                // value is static.
                if (
                    type.isInlined() &&
                    type.unboxInlineClass().toKotlinType().isStable() &&
                    getValueArgument(0)?.isStatic() == true
                ) {
                    return true
                }
                false
            }
            is IrCall -> when (origin) {
                is IrStatementOrigin.GET_PROPERTY -> {
                    // If we are in a GET_PROPERTY call, then this should usually resolve to
                    // non-null, but in case it doesn't, just return false
                    val prop = (symbol.owner as? IrSimpleFunction)
                        ?.correspondingPropertySymbol?.owner ?: return false

                    // if the property is a top level constant, then it is static.
                    if (prop.isConst) return true

                    val typeIsStable = type.toKotlinType().isStable()
                    val dispatchReceiverIsStatic = dispatchReceiver?.isStatic() != false
                    val extensionReceiverIsStatic = extensionReceiver?.isStatic() != false

                    // if we see that the property is read-only with a default getter and a
                    // stable return type , then reading the property can also be considered
                    // static if this is a top level property or the subject is also static.
                    if (!prop.isVar &&
                            prop.getter?.origin == IrDeclarationOrigin.DEFAULT_PROPERTY_ACCESSOR &&
                        typeIsStable &&
                        dispatchReceiverIsStatic && extensionReceiverIsStatic
                    ) {
                        return true
                    }

                    val getterIsStable = prop.hasStableAnnotation() ||
                            symbol.owner.hasStableAnnotation()

                    if (
                        getterIsStable &&
                        typeIsStable &&
                        dispatchReceiverIsStatic &&
                        extensionReceiverIsStatic
                    ) {
                        return true
                    }

                    false
                }
                is IrStatementOrigin.PLUS,
                is IrStatementOrigin.MUL,
                is IrStatementOrigin.MINUS,
                is IrStatementOrigin.ANDAND,
                is IrStatementOrigin.OROR,
                is IrStatementOrigin.DIV,
                is IrStatementOrigin.EQ,
                is IrStatementOrigin.EQEQ,
                is IrStatementOrigin.EQEQEQ,
                is IrStatementOrigin.GT,
                is IrStatementOrigin.GTEQ,
                is IrStatementOrigin.LT,
                is IrStatementOrigin.LTEQ -> {
                    // special case mathematical operators that are in the stdlib. These are
                    // immutable operations so the overall result is static if the operands are
                    // also static
                    val isStableOperator = symbol
                        .descriptor
                        .fqNameSafe
                        .topLevelName() == "kotlin" ||
                            symbol.owner.hasStableAnnotation()

                    val typeIsStable = type.toKotlinType().isStable()
                    if (!typeIsStable) return false

                    if (!isStableOperator) {
                        return false
                    }

                    getArguments().all { it.second.isStatic() }
                }
                null -> {
                    if (symbol.descriptor.fqNameSafe == ComposeFqNames.remember) {
                        // if it is a call to remember with 0 input arguments, then we can
                        // consider the value static if the result type of the lambda is stable
                        val syntheticRememberParams = 1 + // composer param
                                1 + // key param
                                1 // changed param
                        val expectedArgumentsCount = 1 + syntheticRememberParams // 1 for lambda
                        if (
                            valueArgumentsCount == expectedArgumentsCount &&
                            type.toKotlinType().isStable()
                        ) {
                            return true
                        }
                    }
                    if (symbol.descriptor.fqNameSafe == ComposeFqNames.restartableFunction) {
                        // calls to this function are generated by the compiler, and this
                        // function behaves similar to a remember call in that the result will
                        // _always_ be the same and the resulting type is _always_ stable, so
                        // thus it is static.
                        return true
                    }
                    // normal function call. If the function is marked as Stable and the result
                    // is Stable, then the static-ness of it is the static-ness of its arguments
                    val isStable = symbol.owner.hasStableAnnotation()
                    if (!isStable) return false

                    val typeIsStable = type.toKotlinType().isStable()
                    if (!typeIsStable) return false

                    // getArguments includes the receivers!
                    getArguments().all { it.second.isStatic() }
                }
                else -> false
            }
            is IrGetValue -> {
                val owner = symbol.owner
                when (owner) {
                    is IrVariable -> {
                        // If we have an immutable variable whose initializer is also static,
                        // then we can determine that the variable reference is also static.
                        !owner.isVar && owner.initializer?.isStatic() == true
                    }
                    else -> false
                }
            }
            else -> false
        }
    }

    private fun extractParamMetaFromScopes(meta: ParamMeta, param: IrValueDeclaration): Boolean {
        for (scope in scopeStack.asReversed()) {
            when (scope) {
                is Scope.FunctionScope -> {
                    if (scope.remappedParams.containsValue(param)) {
                        meta.isCertain = true
                        meta.maskParam = scope.dirty
                        meta.maskSlot = scope.paramsToSlots[param]!!
                        return true
                    }
                }
            }
        }
        return false
    }

    private fun buildChangedParamsForCall(
        valueParams: List<ParamMeta>,
        extensionParam: ParamMeta?,
        dispatchParam: ParamMeta?
    ): List<IrExpression> {
        val thisParams = listOfNotNull(extensionParam, dispatchParam)
        val allParams = valueParams + thisParams
        // passing in 0 for thisParams since they should be included in the params list
        val changedCount = changedParamCount(valueParams.size, thisParams.size)
        val result = mutableListOf<IrExpression>()
        for (i in 0 until changedCount) {
            val start = i * SLOTS_PER_INT
            val end = min(start + SLOTS_PER_INT, allParams.size)
            val slice = allParams.subList(start, end)
            result.add(buildChangedParamForCall(slice))
        }
        return result
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
            if (meta.isVararg) {
                bitMaskConstant = bitMaskConstant or ParamState.Uncertain.bitsForSlot(slot)
            } else if (!meta.isProvided) {
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
                orExprs.add(
                    irAnd(
                        irConst(bitsForSlot(0b11, slot)),
                        someMask.irShiftBits(parentSlot, slot)
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
        val declaration = expression.symbol.owner
        for (scope in scopeStack.asReversed()) {
            if (scope is Scope.FunctionScope) {
                if (scope.function.extensionReceiverParameter == declaration) {
                    scope.markGetExtensionReceiver()
                }
                if (scope.function.dispatchReceiverParameter == declaration) {
                    scope.markGetDispatchReceiver()
                }
                val remapped = scope.remappedParams[declaration]
                if (remapped != null) {
                    return irGet(remapped)
                }
            }
        }
        return expression
    }

    override fun visitReturn(expression: IrReturn): IrExpression {
        if (!isInComposableScope) return super.visitReturn(expression)
        expression.transformChildren()
        val endBlock = mutableStatementContainer()
        encounteredReturn(expression.returnTargetSymbol) { endBlock.statements.add(it) }
        return if (expression.value.type.isUnitOrNullableUnit()) {
            expression.wrap(listOf(endBlock))
        } else {
            val tempVar = irTemporary(expression.value, nameHint = "return")
            tempVar.wrap(
                expression.type,
                after = listOf(
                    endBlock,
                    IrReturnImpl(
                        expression.startOffset,
                        expression.endOffset,
                        expression.type,
                        expression.returnTargetSymbol,
                        irGet(tempVar)
                    )
                )
            )
        }
    }

    override fun visitBreakContinue(jump: IrBreakContinue): IrExpression {
        if (!isInComposableScope) return super.visitBreakContinue(jump)
        val endBlock = mutableStatementContainer()
        encounteredJump(jump) { endBlock.statements.add(it) }
        return jump.wrap(before = listOf(endBlock))
    }

    override fun visitDoWhileLoop(loop: IrDoWhileLoop): IrExpression {
        if (!isInComposableScope) return super.visitDoWhileLoop(loop)
        return handleLoop(loop as IrLoopBase)
    }

    override fun visitWhileLoop(loop: IrWhileLoop): IrExpression {
        if (!isInComposableScope) return super.visitWhileLoop(loop)
        return handleLoop(loop as IrLoopBase)
    }

    private fun handleLoop(loop: IrLoopBase): IrExpression {
        val loopScope = withScope(Scope.LoopScope(loop)) {
            loop.transformChildren()
        }
        return if (loopScope.hasComposableCalls) {
            loop.asCoalescableGroup(loopScope)
        } else {
            loop
        }
    }

    override fun visitWhen(expression: IrWhen): IrExpression {
        if (!isInComposableScope) return super.visitWhen(expression)

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
        var needsWrappingGroup = false
        var someResultsHaveCalls = false
        var hasElseBranch = false

        val transformed = IrWhenImpl(
            expression.startOffset,
            expression.endOffset,
            expression.type,
            expression.origin
        )
        val resultScopes = mutableListOf<Scope.BranchScope>()
        val condScopes = mutableListOf<Scope.BranchScope>()
        val whenScope = withScope(Scope.WhenScope()) {
            expression.branches.forEachIndexed { index, it ->
                if (it is IrElseBranch) {
                    hasElseBranch = true
                    val (resultScope, result) = it.result.transformWithScope(Scope.BranchScope())

                    condScopes.add(Scope.BranchScope())
                    resultScopes.add(resultScope)

                    someResultsHaveCalls = someResultsHaveCalls || resultScope.hasComposableCalls
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

                    // the first condition is always executed so if it has a composable call in it,
                    // it doesn't necessitate a group
                    needsWrappingGroup =
                        needsWrappingGroup || (index != 0 && condScope.hasComposableCalls)
                    someResultsHaveCalls = someResultsHaveCalls || resultScope.hasComposableCalls
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
        // one already. Note that we only need to do this if we aren't going to wrap the if
        // statement in a group entirely, which we will do if the conditions have calls in them.
        // NOTE: we might also be able to assume that the when is exhaustive if it has a non-unit
        // resulting type, since the type system should enforce that.
        if (!hasElseBranch && someResultsHaveCalls && !needsWrappingGroup) {
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
            // If the conditional block doesn't have a composable call in it, we don't need
            // to geneerate a group around it because we will be generating one around the entire
            // if statement
            if (needsWrappingGroup && condScope.hasComposableCalls) {
                it.condition = it.condition.asReplaceableGroup(condScope)
            }
            if (
                // if no wrapping group but some results have calls, we have to have every result
                // be a group so that we have a consistent number of groups during execution
                (someResultsHaveCalls && !needsWrappingGroup) ||
                // if we are wrapping the if with a group, then we only need to add a group when
                // the block has composable calls
                (needsWrappingGroup && resultScope.hasComposableCalls)
            ) {
                it.result = it.result.asReplaceableGroup(resultScope)
            }
        }

        return if (needsWrappingGroup) {
            transformed.asCoalescableGroup(whenScope)
        } else {
            transformed
        }
    }

    sealed class Scope(val name: String) {
        class FunctionScope(
            val function: IrFunction,
            transformer: ComposableFunctionBodyTransformer
        ) : BlockScope("fun ${function.name.asString()}") {
            val remappedParams = mutableMapOf<IrValueDeclaration, IrValueDeclaration>()
            val paramsToSlots = mutableMapOf<IrValueDeclaration, Int>()
            val isInlinedLambda = with(transformer) { function.isInlinedLambda() }

            private var lastTemporaryIndex: Int = 0

            private fun nextTemporaryIndex(): Int = lastTemporaryIndex++

            var composerParameter: IrValueParameter? = null
                private set

            var keyParameter: IrValueParameter? = null
                private set

            var defaultParameter: IrDefaultBitMaskValue? = null
                private set

            var changedParameter: IrChangedBitMaskValue? = null
                private set

            var realValueParamCount: Int = 0
                private set

            // slotCount will include the dispatchReceiver and extensionReceivers
            var slotCount: Int = 0
                private set

            var dirty: IrChangedBitMaskValue? = null

            var dispatchReceiverUsed: Boolean = false
                private set

            var extensionReceiverUsed: Boolean = false
                private set

            fun markGetDispatchReceiver() {
                dispatchReceiverUsed = true
            }

            fun markGetExtensionReceiver() {
                extensionReceiverUsed = true
            }

            init {
                val defaultParams = mutableListOf<IrValueParameter>()
                val changedParams = mutableListOf<IrValueParameter>()
                for (param in function.valueParameters) {
                    val paramName = param.name.asString()
                    when {
                        !paramName.startsWith('$') -> realValueParamCount++
                        paramName == KtxNameConventions.COMPOSER_PARAMETER.identifier ->
                            composerParameter = param
                        paramName == KtxNameConventions.KEY_PARAMETER.identifier ->
                            keyParameter = param
                        paramName.startsWith(KtxNameConventions.DEFAULT_PARAMETER.identifier) ->
                            defaultParams += param
                        paramName.startsWith(KtxNameConventions.CHANGED_PARAMETER.identifier) ->
                            changedParams += param
                        paramName.startsWith("\$anonymous\$parameter") -> Unit
                        paramName.startsWith("\$name\$for\$destructuring") -> Unit
                        else -> {
                            error("Unexpected parameter name: $paramName")
                        }
                    }
                }
                slotCount = realValueParamCount
                if (function.extensionReceiverParameter != null) slotCount++
                if (function.dispatchReceiverParameter != null) {
                    slotCount++
                } else if (function.origin == IrDeclarationOrigin.LOCAL_FUNCTION_FOR_LAMBDA) {
                    slotCount++
                }
                changedParameter = if (composerParameter != null)
                    transformer.IrChangedBitMaskValueImpl(
                        changedParams,
                        slotCount
                    )
                else
                    null
                defaultParameter = if (defaultParams.isNotEmpty())
                    transformer.IrDefaultBitMaskValueImpl(
                        defaultParams,
                        realValueParamCount
                    )
                else
                    null
            }

            val isComposable = composerParameter != null

            fun getNameForTemporary(nameHint: String?): String {
                val index = nextTemporaryIndex()
                return if (nameHint != null) "tmp${index}_$nameHint" else "tmp$index"
            }
        }

        abstract class BlockScope(name: String) : Scope(name) {
            private val extraEndLocations = mutableListOf<(IrExpression) -> Unit>()

            fun realizeGroup(makeEnd: () -> IrExpression) {
                realizeCoalescableGroup()
                realizeEndCalls(makeEnd)
            }

            fun markComposableCall() {
                hasComposableCalls = true
                if (coalescableChild != null) {
                    // if a call happens after the coalescable child group, then we should
                    // realize the group of the coalescable child
                    shouldRealizeCoalescableChild = true
                }
            }

            fun markReturn(extraEndLocation: (IrExpression) -> Unit) {
                hasReturn = true
                extraEndLocations.push(extraEndLocation)
            }

            fun markJump(extraEndLocation: (IrExpression) -> Unit) {
                hasJump = true
                extraEndLocations.push(extraEndLocation)
            }

            fun markCoalescableGroup(
                scope: BlockScope,
                realizeGroup: () -> Unit,
                makeEnd: () -> IrExpression
            ) {
                coalescableChild = scope
                realizeCoalescableChildGroup = {
                    realizeGroup()
                    scope.realizeGroup(makeEnd)
                    realizeCoalescableChildGroup = { error("Attempted to realize group twice") }
                }
            }

            private fun realizeCoalescableGroup() {
                if (shouldRealizeCoalescableChild) {
                    realizeCoalescableChildGroup()
                } else {
                    coalescableChild?.realizeCoalescableGroup()
                }
            }

            private fun realizeEndCalls(makeEnd: () -> IrExpression) {
                extraEndLocations.forEach {
                    it(makeEnd())
                }
            }

            var hasComposableCalls = false
                private set
            var hasReturn = false
                private set
            var hasJump = false
                private set
            private var realizeCoalescableChildGroup = {}
            private var shouldRealizeCoalescableChild = false
            private var coalescableChild: BlockScope? = null
        }
        class ClassScope(name: Name) : Scope("class ${name.asString()}")
        class PropertyScope(name: Name) : Scope("val ${name.asString()}")
        class FieldScope(name: Name) : Scope("field ${name.asString()}")
        class FileScope(name: FqName) : Scope("file $name")
        class LoopScope(val loop: IrLoop) : BlockScope("loop")
        class WhenScope : BlockScope("when")
        class BranchScope : BlockScope("branch")
        class CaptureScope : BlockScope("capture") {
            var hasCapturedComposableCall = false
                private set

            fun markCapturedComposableCall() {
                hasCapturedComposableCall = true
            }
        }
    }

    inner class IrDefaultBitMaskValueImpl(
        private val params: List<IrValueParameter>,
        private val count: Int
    ) : IrDefaultBitMaskValue {

        init {
            val actual = params.size
            val expected = defaultParamCount(count)
            require(actual == expected) {
                "Function with $count params had $actual default params but expected $expected"
            }
        }

        override fun irIsolateBitAtIndex(index: Int): IrExpression {
            require(index <= count)
            // (%default and 0b1)
            return irAnd(
                // a value of 1 in default means it was NOT provided
                irGet(params[defaultsParamIndex(index)]),
                irConst(0b1 shl defaultsBitIndex(index))
            )
        }

        override fun irHasAnyProvidedAndUnstable(unstable: BooleanArray): IrExpression {
            require(count == unstable.size)
            val expressions = params.mapIndexed { index, param ->
                val start = index * BITS_PER_INT
                val end = min(start + BITS_PER_INT, count)
                val unstableMask = bitMask(*unstable.sliceArray(start until end))
                irNotEqual(
                    // ~$default and unstableMask will be non-zero if any parameters were
                    // *provided* AND *unstable*
                    irAnd(
                        irInv(irGet(param)),
                        irConst(unstableMask)
                    ),
                    irConst(0)
                )
            }
            return if (expressions.size == 1)
                expressions.single()
            else
                expressions.reduce { lhs, rhs -> irOrOr(lhs, rhs) }
        }

        override fun putAsValueArgumentIn(fn: IrFunctionAccessExpression, startIndex: Int) {
            params.forEachIndexed { i, param ->
                fn.putValueArgument(
                    startIndex + i,
                    irGet(param)
                )
            }
        }
    }

    open inner class IrChangedBitMaskValueImpl(
        private val params: List<IrValueDeclaration>,
        private val count: Int
    ) : IrChangedBitMaskValue {
        protected fun paramIndexForSlot(slot: Int): Int = slot / SLOTS_PER_INT

        init {
            val actual = params.size
            val expected = changedParamCount(count, 0)
            require(actual == expected) {
                "Function with $count params had $actual changed params but expected $expected"
            }
        }

        override fun irLowBit(): IrExpression {
            return irAnd(
                irGet(params[0]),
                irConst(0b1)
            )
        }

        override fun irIsolateBitsAtSlot(slot: Int): IrExpression {
            // %changed and 0b11
            return irAnd(
                irGet(params[paramIndexForSlot(slot)]),
                irBitsForSlot(0b11, slot)
            )
        }

        override fun irHasDifferences(): IrExpression {
            if (count == 0) {
                // for 0 slots (no params), we can create a shortcut expression of just checking the
                // low-bit for non-zero. Since all of the higher bits will also be 0, we can just
                // simplify this to check if dirty is non-zero
                return irNotEqual(
                    irGet(params[0]),
                    irConst(0)
                )
            }

            val expressions = params.mapIndexed { index, param ->
                val start = index * SLOTS_PER_INT
                val end = min(start + SLOTS_PER_INT, count)

                // makes an int with each slot having 0b01 mask and the low bit being 0.
                // so for 3 slots, we would get 0b 01 01 01 0.
                // This pattern is useful because we can and + xor it with our $changed bitmask and it
                // will only be non-zero if any of the slots were DIFFERENT or UNCERTAIN.
                val bitPattern = (start until end).fold(0) { mask, slot ->
                    mask or bitsForSlot(0b01, slot)
                }

                // we use this pattern with the low bit set to 1 in the "and", and the low bit set to 0
                // for the "xor". This means that if the low bit was set, we will get 1 in the resulting
                // low bit. Since we use this calculation to determine if we need to run the body of the
                // function, this is exactly what we want.

                // $dirty and (0b 01 ... 01 1) xor (0b 01 ... 01 0)
                irNotEqual(
                    irXor(
                        irAnd(
                            irGet(param),
                            irConst(bitPattern or 0b1)
                        ),
                        irConst(bitPattern or 0b0)
                    ),
                    irConst(0) // anything non-zero means we have differences
                )
            }
            return if (expressions.size == 1)
                expressions.single()
            else
                expressions.reduce { lhs, rhs -> irOrOr(lhs, rhs) }
        }

        override fun irCopyToTemporary(
            nameHint: String?,
            isVar: Boolean,
            exactName: Boolean
        ): IrChangedBitMaskVariable {
            val temps = params.mapIndexed { index, param ->
                irTemporary(
                    irGet(param),
                    if (index == 0) nameHint else "$nameHint$index",
                    context.irBuiltIns.intType,
                    isVar,
                    exactName
                )
            }
            return IrChangedBitMaskVariableImpl(temps, count)
        }

        override fun putAsValueArgumentInWithLowBit(
            fn: IrFunctionAccessExpression,
            startIndex: Int,
            lowBit: Boolean
        ) {
            params.forEachIndexed { index, param ->
                fn.putValueArgument(
                    startIndex + index,
                    if (index == 0) {
                        irOr(irGet(param), irConst(if (lowBit) 0b1 else 0b0))
                    } else {
                        irGet(param)
                    }
                )
            }
        }

        override fun irShiftBits(fromSlot: Int, toSlot: Int): IrExpression {
            val fromSlotAdjusted = fromSlot.rem(SLOTS_PER_INT)
            val toSlotAdjusted = toSlot.rem(SLOTS_PER_INT)
            val bitsToShiftLeft = (toSlotAdjusted - fromSlotAdjusted) * 2
            val value = irGet(params[paramIndexForSlot(fromSlot)])

            if (bitsToShiftLeft == 0) return value
            val int = context.builtIns.intType
            val shiftLeft = context.symbols.getBinaryOperator(
                OperatorNames.SHL,
                int,
                int
            )
            val shiftRight = context.symbols.getBinaryOperator(
                OperatorNames.SHR,
                int,
                int
            )

            return irCall(
                if (bitsToShiftLeft > 0) shiftLeft else shiftRight,
                null,
                value,
                null,
                irConst(abs(bitsToShiftLeft))
            )
        }
    }

    inner class IrChangedBitMaskVariableImpl(
        private val temps: List<IrVariable>,
        count: Int
    ) : IrChangedBitMaskVariable, IrChangedBitMaskValueImpl(temps, count) {
        override fun asStatements(): List<IrStatement> {
            return temps
        }

        override fun irOrSetBitsAtSlot(slot: Int, value: IrExpression): IrExpression {
            val temp = temps[paramIndexForSlot(slot)]
            return irSet(
                temp,
                irOr(
                    irGet(temp),
                    value
                )
            )
        }
    }
}

inline fun <A, B, C> forEachWith(a: List<A>, b: List<B>, c: List<C>, fn: (A, B, C) -> Unit) {
    for (i in a.indices) {
        fn(a[i], b[i], c[i])
    }
}