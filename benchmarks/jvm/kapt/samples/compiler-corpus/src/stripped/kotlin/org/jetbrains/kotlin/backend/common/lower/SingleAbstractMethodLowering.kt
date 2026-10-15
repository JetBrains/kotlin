/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower

import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.ScopeWithIr
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.DescriptorVisibility
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.IrTypeOperator
import org.jetbrains.kotlin.ir.expressions.IrTypeOperatorCall
import org.jetbrains.kotlin.ir.expressions.impl.IrInstanceInitializerCallImpl
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classOrFail
import org.jetbrains.kotlin.ir.types.classifierOrFail
import org.jetbrains.kotlin.ir.types.makeNullable
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.util.OperatorNameConventions
import org.jetbrains.kotlin.utils.findIsInstanceAnd
import org.jetbrains.kotlin.utils.memoryOptimizedMap
import org.jetbrains.kotlin.utils.memoryOptimizedPlus

/**
 * Replaces SAM conversions with instances of interface-implementing classes.
 */
abstract class SingleAbstractMethodLowering(val context: CommonBackendContext) : FileLoweringPass, IrElementTransformerVoidWithContext() {
    // SAM wrappers are cached, either in the file class (if it exists), or in a top-level enclosing class.
    // In the latter case, the names of SAM wrappers depend on the order of classes in the file. For example:
    //
    //    class A {
    //      fun f(run: () -> Unit) = Runnable(run)
    //    }
    //
    //    class B {
    //      fun g(run: () -> Unit) = Runnable(run)
    //      fun h(p: (String) -> Boolean) = Predicate(p)
    //    }
    //
    // This code creates two SAM wrappers, `A$sam$java_lang_Runnable$0`, which is used in both
    // `A.f` and `B.g`, as well as `B$sam$java_util_function_Predicate$0`, which is used in `B.h`.
    //
    // Additionally, we need to cache SAM wrappers inside inline functions separately from those
    // outside of inline functions. Outside of inline functions we generate package private wrappers
    // with name prefix "sam$". In the scope of an inline function we generate public wrappers with
    // name prefix "sam$i$".
    //
    // Coming from the frontend, every SAM interface is associated with exactly one function type
    // (see SamType.getKotlinFunctionType). This is why we can cache implementations just based on
    // the superType.
    protected val cachedImplementations = mutableMapOf<IrType, IrClass>()
    protected val inlineCachedImplementations = mutableMapOf<IrType, IrClass>()
    protected var enclosingContainer: IrDeclarationContainer? = null

    abstract fun getWrapperVisibility(expression: IrTypeOperatorCall, scopes: List<ScopeWithIr>): DescriptorVisibility

    abstract fun getSuperTypeForWrapper(typeOperand: IrType): IrType

    protected open fun getWrappedFunctionType(klass: IrClass): IrType = TODO()

    protected open fun getSuspendFunctionWithoutContinuation(function: IrSimpleFunction): IrSimpleFunction = TODO()

    protected open fun IrFunctionBuilder.setConstructorSourceRange(createFor: IrElement) { TODO() }

    abstract val IrType.needEqualsHashCodeMethods: Boolean

    open val inInlineFunctionScope get() = allScopes.any { scope -> (scope.irElement as? IrFunction)?.isInline ?: false }

    protected open fun postprocessCreatedObjectProxy(klass: IrClass) { TODO() }

    override fun lower(irFile: IrFile) { TODO() }

    protected open fun currentScopeSymbol(): IrSymbol? { TODO() }

    override fun visitClassNew(declaration: IrClass): IrStatement { TODO() }

    override fun visitTypeOperator(expression: IrTypeOperatorCall): IrExpression { TODO() }

    private val SAM_WRAPPER_SUFFIX = "$0"
    private val FUNCTION_FIELD_NAME = "function"

    // Construct a class that wraps an invokable object into an implementation of an interface:
    //     class sam$n(private val invokable: F) : Interface { override fun method(...) = invokable(...) }
    private fun createObjectProxy(superType: IrType, wrapperVisibility: DescriptorVisibility, createFor: IrElement): IrClass { TODO() }

    private fun generateEqualsHashCode(klass: IrClass, superType: IrType, functionDelegateField: IrField) =
        SamEqualsHashCodeMethodsGenerator(context, klass, superType) { receiver ->
            irGetField(receiver, functionDelegateField)
        }.generate()

    private fun getAdditionalSupertypes(supertype: IrType) =
        if (supertype.needEqualsHashCodeMethods)
            listOf(context.symbols.functionAdapter.typeWith())
        else emptyList()
}

/**
 * Generates equals and hashCode for SAM and fun interface wrappers, as well as an implementation of getFunctionDelegate
 * (inherited from kotlin.jvm.internal.FunctionAdapter), needed to properly implement them.
 * This class is used in two places:
 * - FunctionReferenceLowering, which is the case of SAM conversion of a (maybe adapted) function reference, e.g. `FunInterface(foo::bar)`.
 *   Note that we don't generate equals/hashCode for SAM conversion of lambdas, e.g. `FunInterface {}`, even though lambdas are represented
 *   as a local function + reference to it. The reason for this is that all lambdas are unique, so after SAM conversion they are still
 *   never equal to each other. See [FunctionReferenceLowering.FunctionReferenceBuilder.needToGenerateSamEqualsHashCodeMethods].
 * - SingleAbstractMethodLowering, which is the case of SAM conversion of any value of a functional type,
 *   e.g. `val f = {}; FunInterface(f)`.
 */
class SamEqualsHashCodeMethodsGenerator(
    private val context: CommonBackendContext,
    private val klass: IrClass,
    private val samSuperType: IrType,
    private val obtainFunctionDelegate: IrBuilderWithScope.(receiver: IrExpression) -> IrExpression,
) {
    private val functionAdapterClass = context.symbols.functionAdapter.owner

    private val builtIns: IrBuiltIns get() = context.irBuiltIns
    private val getFunctionDelegate = functionAdapterClass.functions.single { it.name.asString() == "getFunctionDelegate" }

    fun generate() { TODO() }

    private fun generateGetFunctionDelegate() { TODO() }

    private fun generateEquals(anyGenerator: MethodsFromAnyGeneratorForLowerings) { TODO() }

    private fun generateHashCode(anyGenerator: MethodsFromAnyGeneratorForLowerings) { TODO() }
}
