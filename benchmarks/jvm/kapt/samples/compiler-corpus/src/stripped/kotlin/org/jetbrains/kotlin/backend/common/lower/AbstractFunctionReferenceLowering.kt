/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower

import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrInstanceInitializerCallImpl
import org.jetbrains.kotlin.ir.irAttribute
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrTransformer
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.addToStdlib.applyIf
import org.jetbrains.kotlin.utils.addToStdlib.shouldNotBeCalled
import org.jetbrains.kotlin.utils.memoryOptimizedPlus


/**
 * This attribute is used to store declarations as they were at this lowering.
 * This is needed to avoid creating fake overrides of declarations at later state.
 * @see org.jetbrains.kotlin.ir.overrides.IrFakeOverrideBuilder.buildFakeOverridesForClassUsingOverriddenSymbols for more details.
 */
var IrClass.declarationsAtFunctionReferenceLowering: List<IrDeclaration>? by irAttribute(copyByDefault = true)

/**
 * This lowering transforms [IrRichFunctionReference] nodes to an anonymous class.
 *
 * The class would have:
 *   * Constructor capturing all values from [IrRichFunctionReference.boundValues], and storing them to fields
 *   * A method overriding [IrRichFunctionReference.overriddenFunctionSymbol], with body moved from [IrRichFunctionReference.invokeFunction]
 *   * [IrRichFunctionReference.type] as a super-interface type (typically `[K][Suspend]FunctionN`,
 *     or fun interface the reference was sam converted to)
 *
 * Platforms can customize:
 *   * Super-class with platform-specific reference implementation details by overriding [getSuperClassType] method
 *   * Equality/hashCode/toString and other reflection methods implementation by adding methods in overridden [generateExtraMethods] method
 *   * exact names/origins of generated classes/methods by overriding corresponding methods
 *
 * For example, the following code:
 * ```kotlin
 * fun foo1(l: () -> String): String {
 *   return l()
 * }
 * fun <FooTP> foo2(v: FooTP, l: (FooTP) -> String): String {
 *   return l(v)
 * }
 *
 * private fun <T> bar(t: T): String { /* ... */ }
 *
 * fun <BarTP> bar(v: BarTP): String {
 *   return foo1(v::bar/*<T=BarTP>*/) + foo2(v) { bar/*<T=BarTP>*/(it) }
 * }
 * ```
 *
 * is lowered into:
 * ```kotlin
 * fun <BarTP> bar(v: BarTP): String {
 *   /*local*/ class <local-platform-specific-name-1>(p$0: BarTP) : KFunction0<String>, PlatformSpecificSuperType() {
 *     private val f$0: BarTP = p$0
 *     override fun invoke() = bar<BarTP>(f$0)
 *     // some platform specific reflection information
 *   }
 *   /*local*/ class <local-platform-specific-name-2> : Function1<BarTP, String>, PlatformSpecificSuperTypeProbablyAny() {
 *     override fun invoke(p0: BarTP) = bar<BarTP>(p0)
 *   }
 *   return foo1(<local-platform-specific-name-1>(v)) + foo2(v, <local-platform-specific-name-2>())
 * }
 * ```
 *
 * Note that as all these classes are defined as local ones, they don't need to explicitly capture local variables or any type parameters.
 * But it can happen, that [LocalDeclarationsLowering] would later capture something additional into the classes.
 */
abstract class AbstractFunctionReferenceLowering<C : CommonBackendContext>(val context: C) : FileLoweringPass {
    override fun lower(irFile: IrFile) { TODO() }

    private fun buildClass(functionReference: IrRichFunctionReference, parent: IrDeclarationParent): IrClass { TODO() }

    /**
     * This function is very similar to [org.jetbrains.kotlin.backend.jvm.lower.FunctionReferenceLowering.FunctionReferenceBuilder.createInvokeMethod].
     * If you make any changes, don't forget to also change the other one.
     */
    private fun buildInvokeMethod(
        functionReference: IrRichFunctionReference,
        functionReferenceClass: IrClass,
        superInterfaceType: IrType,
        boundFields: List<IrField>
    ): IrSimpleFunction { TODO() }

    protected open fun postprocessClass(functionReferenceClass: IrClass, functionReference: IrRichFunctionReference) { TODO() }
    protected open fun postprocessInvoke(invokeFunction: IrSimpleFunction, functionReference: IrRichFunctionReference) { TODO() }
    protected open fun generateExtraMethods(functionReferenceClass: IrClass, reference: IrRichFunctionReference) { TODO() }

    protected open fun getExtraConstructorParameters(
        constructor: IrConstructor,
        reference: IrRichFunctionReference,
    ): List<IrValueParameter> = TODO()

    protected open fun IrBuilderWithScope.getExtraConstructorArgument(
        parameter: IrValueParameter,
        reference: IrRichFunctionReference,
    ): IrExpression? = TODO()

    protected abstract fun IrBuilderWithScope.generateSuperClassConstructorCall(
        constructor: IrConstructor,
        superClassType: IrType,
        functionReference: IrRichFunctionReference,
    ): IrDelegatingConstructorCall

    protected abstract fun getReferenceClassName(reference: IrRichFunctionReference): Name
    protected abstract fun getSuperClassType(reference: IrRichFunctionReference): IrType
    protected open fun getAdditionalInterfaces(reference: IrRichFunctionReference): List<IrType> = TODO()
    protected abstract fun getClassOrigin(reference: IrRichFunctionReference): IrDeclarationOrigin
    protected abstract fun getConstructorOrigin(reference: IrRichFunctionReference): IrDeclarationOrigin
    protected abstract fun getInvokeMethodOrigin(reference: IrRichFunctionReference): IrDeclarationOrigin
    protected abstract fun getConstructorCallOrigin(reference: IrRichFunctionReference): IrStatementOrigin?
}

fun CommonBackendContext.addBoundValueAtOverride(
    functionReferenceClass: IrClass,
    fields: List<IrField>,
    overrideOrigin: IrDeclarationOrigin = IrDeclarationOrigin.DEFINED,
) { TODO() }

private fun IrBuilderWithScope.irBoundValueAt(function: IrFunction, fields: List<IrField>, resultType: IrType): IrExpression { TODO() }
