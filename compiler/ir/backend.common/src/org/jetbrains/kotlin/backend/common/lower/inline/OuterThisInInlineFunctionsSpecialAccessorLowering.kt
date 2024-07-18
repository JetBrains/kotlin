/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower.inline

import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities.*
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.expressions.impl.fromSymbolOwner
import org.jetbrains.kotlin.ir.irAttribute
import org.jetbrains.kotlin.ir.util.file
import org.jetbrains.kotlin.ir.util.parents
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.ir.visitors.*

class OuterThisInInlineFunctionsSpecialAccessorLowering(
    context: CommonBackendContext,

    /**
     * This key is a temporary workaround for static caches in Kotlin/Native:
     * - The "outer this" accessors are supposed to be _private_ and non-static.
     * - It is supposed that another layer of _public_ static accessors should be generated
     *   to wrap "outer this" accessor calls. This should be done in [SyntheticAccessorLowering].
     * - However, at the moment [SyntheticAccessorLowering] is only enabled with double-inlining.
     * - After stabilization, [SyntheticAccessorLowering] should be enabled by default.
     * - But until then we can have a situation when _private_ accessor is generated and _public_ one
     *   is not, so that _private_ accessor leaks to another IR file or even module. This could lead
     *   to unexpected crashes in Kotlin/Native backend while building static caches.
     * - To work around this we shall generate public "outer this" accessors if [SyntheticAccessorLowering]
     *   is not enabled.
     */
    private val generatePublicAccessors: Boolean = false
) : FileLoweringPass {

    private class GeneratedOuterThisAccessors {
        private val accessors = HashSet<IrSimpleFunction>()
        private var frozen = false

        fun memoize(accessor: IrSimpleFunction) {
            check(!frozen) { "An attempt to generate an accessor after all accessors have been already added to their containers" }
            accessors += accessor
        }

        fun freezeAndGetAccessors(): Collection<IrSimpleFunction> {
            check(!frozen) { "An attempt to add the generated accessors to their containers once again" }
            frozen = true
            return accessors
        }
    }

    private val accessorGenerator = KlibSyntheticAccessorGenerator(context)

    override fun lower(irFile: IrFile) {
        val transformer = Transformer(irFile)
        irFile.transformChildren(transformer, null)

        val accessors = transformer.generatedOuterThisAccessors.freezeAndGetAccessors()
        if (accessors.isNotEmpty()) {
            addAccessorsToParents(accessors)
        }
    }

    /**
     * Lower a single inline [IrFunction] from an [IrFile] that is not being lowered right now.
     *
     * Note: This function is supposed to be used in [InlineFunctionResolver] implementations that allow
     * deserializing bodiless inline functions and lowering them on demand. Example: [NativeInlineFunctionResolver].
     *
     * All the generated accessors are cached. This helps to avoid accidentally generating their duplicates on the next invocation
     * of [lowerWithoutAddingAccessorsToParents] for another inline function that may also exist in the same [IrFile].
     *
     * Note that the generated accessors are not added to their [IrClass]s. This is only possible when lowering the
     * whole [IrFile] through `lower(IrFile)`.
     * - It helps to keep the invariant that only the file that is being lowered is modified.
     * - Only during file-wise lowering it's possible to compute the proper order in which accessors should be placed.
     */
    fun lowerWithoutAddingAccessorsToParents(irFunction: IrFunction) {
        irFunction.accept(Transformer(irFunction.file), null)
    }

    private fun addAccessorsToParents(accessors: Collection<IrSimpleFunction>) {
        for ((innerClass, accessorsInInnerClass) in accessors.groupBy { it.parent as IrClass }) {
            if (accessorsInInnerClass.size == 1) {
                innerClass.declarations += accessorsInInnerClass[0]
            } else {
                // Sort accessors to always have a stable order.
                innerClass.declarations += accessorsInInnerClass.sortedBy(IrSimpleFunction::name)
            }
        }
    }

    private class TransformerData(private val currentInlineFunction: IrFunction) {

        /**
         * This function serves two goals:
         * 1. Find the innermost inner class of [currentInlineFunction].
         * 2. Ensure that the outer class represented by [maybeOuterClass] is indeed the outer class of the
         *    current inline function and the innermost inner class.
         */
        fun getInnermostInnerClassIfApplicable(maybeOuterClass: IrClass): IrClass? {
            var maybeInnerClass: IrClass? = null
            currentInlineFunction.parents.filterIsInstance<IrClass>().forEach { clazz ->
                if (clazz == maybeOuterClass) {
                    return maybeInnerClass
                } else if (maybeInnerClass == null) {
                    maybeInnerClass = clazz.takeIf { it.isInner }
                }
            }

            return null
        }

        /**
         * Get the innermost dispatch receiver parameter to be used in accessor call.
         *
         * We need this because sometimes there are local inline functions without a dispatch receiver.
         * This can happen, for example, if an inline function is generated by a compiler plugin such as AtomicFU.
         */
        val innermostDispatchReceiverParameter: IrValueParameter
            get() = currentInlineFunction.getEffectiveDispatchReceiverParameter()
                ?: error("Cannot get dispatch receiver parameter for inline function ${currentInlineFunction.render()}")

        companion object {
            private tailrec fun IrFunction.getEffectiveDispatchReceiverParameter(): IrValueParameter? {
                dispatchReceiverParameter?.let { return it }
                val outerFunction = (parent as? IrFunction) ?: return null
                return outerFunction.getEffectiveDispatchReceiverParameter()
            }
        }
    }

    private inner class Transformer(irFile: IrFile) : IrElementTransformer<TransformerData?> {
        val generatedOuterThisAccessors = irFile.generatedOuterThisAccessors
            ?: GeneratedOuterThisAccessors().also { irFile.generatedOuterThisAccessors = it }

        override fun visitFunction(declaration: IrFunction, data: TransformerData?): IrStatement {
            val newData = if (declaration.isInline) TransformerData(declaration) else data

            // Wrap it to the stage controller to avoid JS BE failing with not found lowered declaration signature
            // in `IrDeclaration.signatureForJsIC` cache.
            return declaration.factory.stageController.restrictTo(declaration) {
                super.visitFunction(declaration, newData)
            }
        }

        override fun visitGetValue(expression: IrGetValue, data: TransformerData?): IrExpression {
            if (data == null) {
                // Don't inspect value parameters inside non-inline functions.
                return expression
            }

            val maybeOuterThisValueParameter = expression.symbol.owner as? IrValueParameter
                ?: return expression

            val maybeOuterClass = maybeOuterThisValueParameter.parent as? IrClass
            if (maybeOuterClass != maybeOuterThisValueParameter.parent)
                return expression

            // Note: We need to make sure that `maybeOuterClass` is indeed an outer class as compared to the current inline function,
            // and get the innermost inner class of the inline function.
            val innerClass = data.getInnermostInnerClassIfApplicable(maybeOuterClass)
                ?: return expression

            val accessor = accessorGenerator.getSyntheticOuterThisParameterAccessor(
                expression,
                outerThisValueParameter = maybeOuterThisValueParameter,
                innerClass = innerClass
            )

            if (generatePublicAccessors)
                accessor.visibility = PUBLIC

            generatedOuterThisAccessors.memoize(accessor)

            return IrCallImpl.fromSymbolOwner(expression.startOffset, expression.endOffset, accessor.symbol).apply {
                dispatchReceiver = IrGetValueImpl(
                    startOffset,
                    endOffset,
                    data.innermostDispatchReceiverParameter.symbol,
                    origin
                )
            }
        }
    }

    companion object {
        private var IrFile.generatedOuterThisAccessors: GeneratedOuterThisAccessors? by irAttribute(followAttributeOwner = false)
    }
}
