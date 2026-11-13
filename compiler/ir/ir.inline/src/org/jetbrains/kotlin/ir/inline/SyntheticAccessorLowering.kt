/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.inline

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.LoweringContext
import org.jetbrains.kotlin.backend.common.lower.inline.KlibSyntheticAccessorGenerator
import org.jetbrains.kotlin.backend.common.phaser.PhasePrerequisites
import org.jetbrains.kotlin.config.AnalysisFlags
import org.jetbrains.kotlin.config.ExplicitApiMode
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities.isPrivate
import org.jetbrains.kotlin.descriptors.DescriptorVisibility
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.ir.IrDiagnosticRenderers
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.inline.diagnostics.IrInlinerErrors
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
 *
 * It also routes a read of a static field made outside the field's container (e.g. a lowered `::prop.isInitialized`
 * observed from a nested class) through a synthetic accessor in that container. A direct read bypasses the initialization
 * of the container that its functions do, while the accessor is a static function of the container, so it initializes
 * the container just like the property getter would. See KT-89375.
 */
@PhasePrerequisites(
    OuterThisInInlineFunctionsSpecialAccessorLowering::class,
    FunctionInlining::class, // only private inlining is required
)
class SyntheticAccessorLowering(private val context: LoweringContext, isExecutedOnFirstPhase: Boolean = false) : FileLoweringPass {
    /**
     * The name suffix of the accessors for static field reads outside the container generated on the second phase.
     *
     * Such reads in a KLIB produced with an older compiler may coexist with first-phase accessors of the same fields
     * (e.g. a read in the original body of a private inline function, which is kept in the KLIB, and an accessor for its copy
     * inlined into an internal inline function). The generator doesn't know about the deserialized accessors, so without the suffix
     * it would generate clashing duplicates. Accessors for inline functions don't get the suffix, since other modules that inline
     * these functions on the second phase refer to them by their names; they don't clash, since in a KLIB produced with
     * the inliner on the first phase non-private inline functions don't read fields directly.
     */
    private val staticFieldOutsideContainerAccessorNameSuffix = runIf(!isExecutedOnFirstPhase) { "s" }

    /**
     * Whether the visibility of a generated accessor should be narrowed from _public_ to _internal_ if an accessor is only used
     * in _internal_ inline functions and therefore is not a part of public ABI.
     * This "narrowing" is supposed to be used only during the first phase of compilation.
     */
    private val narrowAccessorVisibilities = isExecutedOnFirstPhase

    /**
     * Warnings about created synthetic accessors are only meaningful to library's author, to help maintain its ABI.
     * When accessors are created while consuming a library (the second stage), there is nothing that the user should worry about,
     * plus nothing they can do about them.
     */
    private val emitExplicitApiModeWarnings = isExecutedOnFirstPhase

    private val accessorGenerator = KlibSyntheticAccessorGenerator(context)

    override fun lower(irFile: IrFile) {
        val transformer = Transformer(irFile)
        irFile.transformChildren(transformer, null)

        val accessors = transformer.generatedAccessors.freezeAndGetAccessors()
        runIf(accessors.isNotEmpty()) {
            runIf(narrowAccessorVisibilities) { narrowAccessorVisibilities(accessors) }
            emitWarningForPublicAccessorsInExplicitAPIMode(accessors, irFile)
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
        for ([parent, accessorsInParent] in accessors.groupBy { it.accessorFunction.parent }) {
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

        /** The innermost declaration container of the current element. */
        private var currentContainer: IrDeclarationContainer = currentFile

        override fun visitClass(declaration: IrClass, data: TransformerData?): IrStatement {
            val outerContainer = currentContainer
            currentContainer = declaration
            try {
                return super.visitClass(declaration, data)
            } finally {
                currentContainer = outerContainer
            }
        }

        override fun visitFunction(declaration: IrFunction, data: TransformerData?): IrStatement {
            val newData = data ?: runIf(declaration.isInline && !declaration.symbol.isConsideredAsPrivateForInlining()) {
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
            if (data == null) {
                if (!expression.isStaticFieldReadOutsideContainer())
                    return super.visitGetField(expression, data = null)

                val accessor = getOrCreateGetterAccessor(expression, nameSuffix = staticFieldOutsideContainerAccessorNameSuffix)
                generatedAccessors.memoize(accessor, expression.symbol)
                return super.visitExpression(accessorGenerator.modifyGetterExpression(expression, accessor.symbol), data = null)
            }

            val targetField = expression.symbol.owner
            if (!targetField.isNonLocalBackingField() || expression.checkIncorrectCrossFileDeclarationAccess())
                return super.visitGetField(expression, data)

            val accessor = getOrCreateGetterAccessor(expression)
            generatedAccessors.memoize(accessor, expression.symbol, data.currentInlineFunction)
            return super.visitExpression(accessorGenerator.modifyGetterExpression(expression, accessor.symbol), data)
        }

        /**
         * Generates and memoizes the accessor. The visibility can be narrowed later.
         */
        private fun getOrCreateGetterAccessor(
            expression: IrGetField,
            nameSuffix: String? = null,
        ): IrSimpleFunction {
            val targetField = expression.symbol.owner
            val accessor = targetField.factory.stageController.restrictTo(targetField) {
                accessorGenerator.getSyntheticGetter(expression, null, nameSuffix)
            }
            return accessor
        }

        private fun IrGetField.isStaticFieldReadOutsideContainer(): Boolean {
            val field = symbol.owner
            if (!field.isStatic || !field.isNonLocalBackingField()) return false
            if (field.correspondingPropertySymbol?.owner?.isConst == true) return false // nothing to initialize
            // Static functions of the innermost container initialize it, instance ones run after a constructor.
            if (field.parent == currentContainer) return false
            // Only the file being lowered can get an accessor. A read of a field from another file is possible only in invalid IR,
            // see `checkIncorrectCrossFileDeclarationAccess`.
            return field.fileOrNull == currentFile
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
                context.diagnosticReporter.at(this, currentFile)
                    .report(
                        IrInlinerErrors.IR_SYNTHETIC_ACCESSOR_LOWERING_WARNING,
                        "Accessing a private declaration from another file is not permitted. " +
                                "This is likely caused by invalid IR generated by a compiler plugin. " +
                                "Please file a bug on https://kotl.in/issue. " +
                                "Offending access: ${render()}"
                    )
            }
            return isIncorrect
        }
    }

    private fun emitWarningForPublicAccessorsInExplicitAPIMode(accessors: Collection<GeneratedAccessor>, irFile: IrFile) {
        if (!emitExplicitApiModeWarnings) return
        val explicitApiMode = context.configuration.languageVersionSettings.getFlag(AnalysisFlags.explicitApiMode)
        if (explicitApiMode == ExplicitApiMode.DISABLED) return

        for (accessor in accessors) {
            if (accessor.accessorFunction.visibility == DescriptorVisibilities.PUBLIC) {
                val message = buildString {
                    append("Public synthetic accessor '${accessor.accessorFunction.name}' was generated in explicit API mode. ")
                    append("This accessor is a part of the library ABI now.")
                    (accessor.targetSymbol.owner as? IrDeclarationWithName)?.let { target ->
                        val kind = IrDiagnosticRenderers.DECLARATION_KIND.render(target)
                        append(" It became possible because $kind '${target.name}', which is not a part of the library ABI, ")
                        append("is exposed through an inline function so it can be used in other libraries. ")
                        append("Please, modify the source code to avoid exposure of $kind '${target.name}' and generation of the accessor.")
                    }
                }
                context.diagnosticReporter.at(accessor.accessorFunction, irFile)
                    .report(IrInlinerErrors.IR_SYNTHETIC_ACCESSOR_LOWERING_WARNING, message)
            }
        }
    }

    companion object {
        private fun IrFunction.isNonLocalPrivateFunction(): Boolean =
            isPrivate(visibility) && !isLocal

        private fun IrField.isNonLocalBackingField(): Boolean =
            correspondingPropertySymbol?.owner?.isLocal == false
    }
}

private var IrFile.generatedAccessors: GeneratedAccessors? by irAttribute(copyByDefault = false)

private class GeneratedAccessors {
    private val accessors = HashMap<IrFunction, GeneratedAccessor>()
    private var frozen = false


    fun memoize(accessor: IrFunction, targetSymbol: IrSymbol, inlineFunction: IrFunction) {
        memoize(accessor, targetSymbol).inlineFunctions += inlineFunction
    }

    /**
     * Memoizes an accessor that is not used in an inline function (e.g. an accessor for a static field read outside its container).
     */
    fun memoize(accessor: IrFunction, targetSymbol: IrSymbol): GeneratedAccessor {
        check(!frozen) { "An attempt to generate an accessor after all accessors have been already added to their containers" }
        return accessors.getOrPut(accessor) { GeneratedAccessor(accessor, targetSymbol) }
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
            val inlineFunctionIsEffectivelyPublicAbi = inlineFunction.parentsWithSelf
                .filterIsInstance<IrDeclarationWithVisibility>()
                .all { declaration ->
                    val visibility = declaration.visibility.delegate
                    visibility.isPublicAPI || (visibility is Visibilities.Internal && declaration.isPublishedApi())
                }

            if (inlineFunctionIsEffectivelyPublicAbi)
                return DescriptorVisibilities.PUBLIC
        }

        return DescriptorVisibilities.INTERNAL
    }
}
