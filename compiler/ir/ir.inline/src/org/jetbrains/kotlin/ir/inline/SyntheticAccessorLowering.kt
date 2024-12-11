/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.inline

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.LoweringContext
import org.jetbrains.kotlin.backend.common.lower.inline.KlibSyntheticAccessorGenerator
import org.jetbrains.kotlin.backend.common.phaser.PhaseDescription
import org.jetbrains.kotlin.backend.common.reportWarning
import org.jetbrains.kotlin.config.KlibConfigurationKeys
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities.isPrivate
import org.jetbrains.kotlin.descriptors.DescriptorVisibility
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.irAttribute
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrTransformer
import org.jetbrains.kotlin.utils.addToStdlib.getOrSetIfNull
import org.jetbrains.kotlin.utils.addToStdlib.runIf

/**
 * Generates synthetic accessor functions for private declarations that are referenced from non-private inline functions,
 * so that after those functions are inlined, there'll be no visibility violations.
 *
 * There are a few important assumptions that this lowering relies on:
 * - It's executed either at the first phase of compilation (source -> KLIB) before serializing the compiled metadata
 *   and IR to a KLIB, or at the second phase (KLIB -> binaries) in a KLIB-based backend.
 * - It's not designed to work with the JVM backend because the visibility rules on JVM are stricter.
 * - By the point it's executed, all _private_ inline functions have already been inlined.
 */
@PhaseDescription("SyntheticAccessorLowering")
class SyntheticAccessorLowering(private val context: LoweringContext) : FileLoweringPass {
    /**
     * Whether the visibility of a generated accessor should be narrowed from _public_ to _internal_ if an accessor is only used
     * in _internal_ inline functions and therefore is not a part of public ABI.
     * This "narrowing" is supposed to be used only during the first phase of compilation.
     */
    private val narrowAccessorVisibilities =
        context.configuration.getBoolean(KlibConfigurationKeys.SYNTHETIC_ACCESSORS_WITH_NARROWED_VISIBILITY)

    private val accessorGenerator = KlibSyntheticAccessorGenerator(context)

    override fun lower(irFile: IrFile) {
        val transformer = Transformer(irFile)
        irFile.transformChildren(transformer, null)

        val accessors = transformer.generatedAccessors.freezeAndGetAccessors()
        runIf(accessors.isNotEmpty()) {
            runIf(narrowAccessorVisibilities) { narrowAccessorVisibilities(accessors) }
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
     * Note that the generated accessors are not added to their [IrDeclarationContainer]s. This is only possible when lowering the
     * whole [IrFile] through `lower(IrFile)`.
     * - It helps to keep the invariant that only the file that is being lowered is modified.
     * - Only during file-wise lowering it's possible to compute the proper order in which accessors should be placed.
     */
    fun lowerWithoutAddingAccessorsToParents(irFunction: IrFunction) {
        irFunction.accept(Transformer(irFunction.file), null)
    }

    private fun narrowAccessorVisibilities(accessors: Collection<GeneratedAccessor>) {
        for (accessor in accessors) {
            accessor.accessorFunction.visibility = accessor.computeNarrowedVisibility()
        }
    }

    /**
     * Add accessors to their containers repeating the order of the corresponding target declarations.
     * Example:
     * ```kotlin
     * class MyClass {
     *     // declarations that are used inside inline function:
     *     private val foo
     *         private get()
     *         private set(value)
     *     public val bar
     *         public get()
     *         private set(value)
     *     public fun baz()
     *     private fun qux()
     *     // their accessors in the "proper" order:
     *     fun access$<get-foo>()
     *     fun access$<set-foo>()
     *     fun access$<set-bar>()
     *     fun access$qux()
     * }
     * ```
     */
    private fun addAccessorsToParents(accessors: Collection<GeneratedAccessor>) {
        for ((parent, accessorsInParent) in accessors.groupBy { it.accessorFunction.parent }) {
            addAccessorsToParent(parent as IrDeclarationContainer, accessorsInParent)
        }
    }

    private fun addAccessorsToParent(parent: IrDeclarationContainer, accessors: List<GeneratedAccessor>) {
        if (accessors.size == 1) {
            parent.declarations += accessors[0].accessorFunction
            return
        }

        /**
         * Note: The mapping between a single target declaration and a single accessor works well at this place.
         * We don't have any cases when multiple accessors can be generated for a single target.
         * Though, that's possible with field-getters (generating now) and field-setters (unsupported).
         */
        val targetToAccessorFunction: Map<IrDeclaration, IrFunction> = accessors.associate { accessor ->
            accessor.targetSymbol.owner as IrDeclaration to accessor.accessorFunction
        }

        val remainingAccessorFunctions: MutableSet<IrFunction> = accessors.mapTo(HashSet(), GeneratedAccessor::accessorFunction)

        fun addAccessorFunctionToParent(maybeTarget: IrDeclaration?) {
            if (maybeTarget == null) return
            val accessorFunction = targetToAccessorFunction[maybeTarget] ?: return
            parent.declarations += accessorFunction
            remainingAccessorFunctions -= accessorFunction
        }

        for (maybeTarget in parent.declarations.toList()) {
            addAccessorFunctionToParent(maybeTarget)
            if (maybeTarget is IrProperty) {
                addAccessorFunctionToParent(maybeTarget.getter)
                addAccessorFunctionToParent(maybeTarget.setter)
                addAccessorFunctionToParent(maybeTarget.backingField)
            }
        }

        if (remainingAccessorFunctions.isNotEmpty()) {
            irError(
                "There are ${remainingAccessorFunctions.size} synthetic accessors in file ${remainingAccessorFunctions.first().file.fileEntry.name}" +
                        " that have been generated but it's not possible to compute the proper order for them"
            ) {
                remainingAccessorFunctions.forEachIndexed { index, accessor -> withIrEntry("accessor$index", accessor) }
            }
        }
    }

    private class TransformerData(val currentInlineFunction: IrFunction)

    private inner class Transformer(private val currentFile: IrFile) : IrTransformer<TransformerData?>() {
        val generatedAccessors = currentFile::generatedAccessors.getOrSetIfNull(::GeneratedAccessors)

        override fun visitFunction(declaration: IrFunction, data: TransformerData?): IrStatement {
            val newData = data ?: runIf(declaration.isInline && !declaration.isConsideredAsPrivateForInlining()) {
                // By the time this lowering is executed, there must be no private inline functions; however,
                // there are exceptions, for example, `suspendCoroutineUninterceptedOrReturn` which are somewhat magical.
                // If we encounter one, ignore it.
                TransformerData(declaration)
            }

            return super.visitFunction(declaration, newData)
        }

        override fun visitFunctionAccess(expression: IrFunctionAccessExpression, data: TransformerData?): IrElement {
            if (data == null)
                return super.visitFunctionAccess(expression, data = null)

            val targetFunction = expression.symbol.owner
            if (!targetFunction.isNonLocalPrivateFunction() || expression.checkIncorrectCrossFileDeclarationAccess())
                return super.visitFunctionAccess(expression, data)

            // Generate and memoize the accessor. The visibility can be narrowed later.
            val accessor = targetFunction.factory.stageController.restrictTo(targetFunction) {
                accessorGenerator.getSyntheticFunctionAccessor(expression, null)
            }
            generatedAccessors.memoize(
                accessor,
                targetSymbol = expression.symbol,
                inlineFunction = data.currentInlineFunction
            )

            return super.visitExpression(accessorGenerator.modifyFunctionAccessExpression(expression, accessor.symbol), data)
        }

        /**
         * Note: Only field-getter accessors are generated. Field-setter accessors are not supported, as we don't know
         * real cases when it's necessary.
         */
        override fun visitGetField(expression: IrGetField, data: TransformerData?): IrExpression {
            if (data == null)
                return super.visitGetField(expression, data = null)

            val targetField = expression.symbol.owner
            if (!targetField.isNonLocalBackingField() || expression.checkIncorrectCrossFileDeclarationAccess())
                return super.visitGetField(expression, data)

            val accessor = targetField.factory.stageController.restrictTo(targetField) {
                accessorGenerator.getSyntheticGetter(expression, null)
            }
            generatedAccessors.memoize(
                accessor,
                targetSymbol = expression.symbol,
                inlineFunction = data.currentInlineFunction
            )

            return super.visitExpression(accessorGenerator.modifyGetterExpression(expression, accessor.symbol), data)
        }

        /**
         * Cross-file accesses should be forbidden entirely. This should be an assertion rather than a warning,
         * but currently we have to be able to lower older KLIBs that may still have them (and it somehow doesn't explode),
         * so we just keep things as is. Adding an accessor into another file doesn't make sense anyway.
         * See [KT-72521](https://youtrack.jetbrains.com/issue/KT-72521) and [KT-72623](https://youtrack.jetbrains.com/issue/KT-72623).
         */
        private fun IrDeclarationReference.checkIncorrectCrossFileDeclarationAccess(): Boolean {
            val callee = symbol.owner as? IrDeclaration ?: return false
            val isIncorrect = callee.fileOrNull != currentFile

            // We have a bunch of older KLIBs, which have access violations due to the way the Compose plugin used to generate IR.
            // In newer versions of Compose this has been fixed, but users still can use KLIBs compiled with older versions.
            // Don't show them the warning, because it's not actionable for them AND cannot be fixed retroactively by us.
            // Yes, this is very ugly, but it does the job.
            // See KT-73482
            val isComposeStableField =
                callee is IrField &&
                        callee.correspondingPropertySymbol?.owner?.isConst == true &&
                        callee.name.asString().endsWith("\$stable")

            if (isIncorrect && !isComposeStableField) {
                context.reportWarning(
                    "Accessing a private declaration from another file is not permitted. " +
                            "This is likely caused by invalid IR generated by a compiler plugin. " +
                            "Please file a bug on https://kotl.in/issue. " +
                            "Offending access: ${render()}",
                    currentFile,
                    this,
                )
            }
            return isIncorrect
        }
    }

    companion object {
        private fun IrFunction.isNonLocalPrivateFunction(): Boolean =
            isPrivate(visibility) && !isLocal

        private fun IrField.isNonLocalBackingField(): Boolean =
            correspondingPropertySymbol?.owner?.isLocal == false
    }
}

private var IrFile.generatedAccessors: GeneratedAccessors? by irAttribute(followAttributeOwner = false)

private class GeneratedAccessors {
    private val accessors = HashMap<IrFunction, GeneratedAccessor>()
    private var frozen = false


    fun memoize(accessor: IrFunction, targetSymbol: IrSymbol, inlineFunction: IrFunction) {
        check(!frozen) { "An attempt to generate an accessor after all accessors have been already added to their containers" }
        accessors.getOrPut(accessor) { GeneratedAccessor(accessor, targetSymbol) }.inlineFunctions += inlineFunction
    }

    fun freezeAndGetAccessors(): Collection<GeneratedAccessor> {
        check(!frozen) { "An attempt to add the generated accessors to their containers once again" }
        frozen = true
        return accessors.values
    }
}

/**
 * @property accessorFunction The generated synthetic accessor.
 * @property targetSymbol The symbol of a private declaration that this accessor wraps.
 * @property inlineFunctions All inline functions where the accessor is used.
 */
private class GeneratedAccessor(
    val accessorFunction: IrFunction,
    val targetSymbol: IrSymbol,
) {
    val inlineFunctions: MutableSet<IrFunction> = hashSetOf()

    fun computeNarrowedVisibility(): DescriptorVisibility {
        for (inlineFunction in inlineFunctions) {
            when (val visibility = inlineFunction.visibility) {
                DescriptorVisibilities.PUBLIC, DescriptorVisibilities.PROTECTED -> return DescriptorVisibilities.PUBLIC
                DescriptorVisibilities.INTERNAL -> if (inlineFunction.isPublishedApi()) return DescriptorVisibilities.PUBLIC
                else -> irError("Unexpected visibility of inline function: $visibility") {
                    withIrEntry("inlineFunction", inlineFunction)
                }
            }
        }

        return DescriptorVisibilities.INTERNAL
    }
}
