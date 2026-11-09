/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower

import org.jetbrains.kotlin.backend.common.*
import org.jetbrains.kotlin.backend.common.descriptors.synthesizedString
import org.jetbrains.kotlin.backend.common.lower.ClosureAnnotator.ClosureBuilder
import org.jetbrains.kotlin.backend.common.phaser.PhasePrerequisites
import org.jetbrains.kotlin.config.AnalysisFlags
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.DescriptorVisibility
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.*
import org.jetbrains.kotlin.ir.builders.declarations.buildConstructor
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.builders.declarations.buildValueParameter
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.IrFieldSymbol
import org.jetbrains.kotlin.ir.symbols.IrValueParameterSymbol
import org.jetbrains.kotlin.ir.symbols.IrValueSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrFieldSymbolImpl
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.IrTypeTransformer
import org.jetbrains.kotlin.ir.visitors.IrVisitor
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.addToStdlib.assignFrom
import org.jetbrains.kotlin.utils.memoryOptimizedMap

interface VisibilityPolicy {
    fun forClass(declaration: IrClass, inPublicInlineScope: Boolean): DescriptorVisibility = TODO()
    fun forConstructor(declaration: IrConstructor, inInlineFunctionScope: Boolean): DescriptorVisibility = TODO()
    fun forCapturedField(value: IrValueSymbol): DescriptorVisibility = TODO()
    fun forSimpleFunction(declaration: IrSimpleFunction): DescriptorVisibility = TODO()

    companion object {
        val DEFAULT = object : VisibilityPolicy {}
    }
}

val BOUND_VALUE_PARAMETER by IrDeclarationOriginImpl.Synthetic

val BOUND_RECEIVER_PARAMETER by IrDeclarationOriginImpl.Synthetic


/**
 * Prepares local declarations like classes and functions for being lifted into the nearest declaration container, adding explicit
 * type and value parameters if the local declaration captures a type parameter or a value from the outer scope.
 *
 * (Note: in the JVM backend, we don't remap captured type parameters inside local declarations for reasons described
 * in [this commit](https://github.com/JetBrains/kotlin/commit/9ce6fd9a15618b856b64efd449799f2b5c379eb5).)
 *
 * Lifting is done in a separate lowering [LocalDeclarationPopupLowering].
 *
 * For example, transforms this
 * ```kotlin
 * fun <T> foo(t: T, i: Int) {
 *   class Local<S> {
 *     private val t: T
 *     private val s: S
 *     constructor(t: T, s: S) {
 *       this.t = t
 *       this.s = s
 *     }
 *     fun getI(): Int {
 *       return i
 *     }
 *   }
 *
 *   fun bar<S>(s: S): String {
 *     return s.toString() + t.toString() + i.toString()
 *   }
 *
 *   println(Local<String>(t, "hello").getI())
 *   println(bar<String>("hello"))
 * }
 * ```
 * into this:
 * ```kotlin
 * fun <T> foo(t: T, i: Int) {
 *   class Local<S, T1> {
 *     private val t: T1
 *     private val s: S
 *     private /*field*/ val i$0: Int
 *     constructor(t: T1, s: S, i$0: Int) {
 *       this.t = t
 *       this.s = s
 *       this.i$0 = i$0
 *     }
 *     fun getI(): Int {
 *       return this.i$0
 *     }
 *   }
 *   println(Local<String, T>(t, "hello", i).getI())
 *   println(bar<T, String>("hello"))
 * }
 *
 * fun bar<T1, S>(s: S, t$0: T1, i$0: Int): String {
 *   return s.toString() + t$0.toString() + i$0.toString()
 * }
 * ```
 */
@PhasePrerequisites(SharedVariablesLowering::class, LocalDelegatedPropertiesLowering::class)
open class LocalDeclarationsLowering(
    open val context: LoweringContext,
    val visibilityPolicy: VisibilityPolicy = VisibilityPolicy.DEFAULT,
    val forceFieldsForInlineCaptures: Boolean = false, // See `LocalClassContext`
    val remapCapturedTypesInExtractedLocalDeclarations: Boolean = true,
    val closureBuilders: MutableMap<IrDeclaration, ClosureBuilder> = mutableMapOf(),
    val transformedDeclarations: MutableMap<IrSymbolOwner, IrDeclaration> = mutableMapOf(),
    val newParameterToCaptured: MutableMap<IrValueParameter, IrValueSymbol> = mutableMapOf(),
    val newParameterToOld: MutableMap<IrValueParameter, IrValueParameter> = mutableMapOf(),
    val oldParameterToNew: MutableMap<IrValueParameter, IrValueParameter> = mutableMapOf(),
    val considerRichFunctionReferenceInvokeFunctionsAsLocal: Boolean = false,
) : BodyLoweringPass {

    override fun lower(irFile: IrFile) { TODO() }

    companion object {
        val DECLARATION_ORIGIN_FIELD_FOR_CAPTURED_VALUE =
            IrDeclarationOriginImpl("FIELD_FOR_CAPTURED_VALUE", isSynthetic = true)

        val DECLARATION_ORIGIN_FIELD_FOR_CROSSINLINE_CAPTURED_VALUE =
            IrDeclarationOriginImpl("FIELD_FOR_CROSSINLINE_CAPTURED_VALUE", isSynthetic = true)
    }

    override fun lower(irBody: IrBody, container: IrDeclaration) { TODO() }

    fun lower(irBlock: IrBlock, container: IrDeclaration, closestParent: IrDeclarationParent) { TODO() }

    open fun getReplacementSymbolForCaptured(container: IrDeclaration, symbol: IrValueSymbol): IrValueSymbol = TODO()

    protected open fun IrClass.getConstructorsThatCouldCaptureParamsWithoutFieldCreating(): Iterable<IrConstructor> = TODO()

    abstract class LocalContext {
        val capturedTypeParameterToTypeParameter: MutableMap<IrTypeParameter, IrTypeParameter> = mutableMapOf()

        // By the time typeRemapper is used, the map will be already filled
        val typeRemapper = IrTypeParameterRemapper(capturedTypeParameterToTypeParameter)

        val ownTypeParameterMap: MutableMap<IrTypeParameter, IrTypeParameter> = mutableMapOf()

        // By the time ownTypeParameterRemapper is used, the map will be already filled
        val ownTypeParameterRemapper = IrTypeParameterRemapper(ownTypeParameterMap)

        abstract val sourceFileWhenInlined: IrFileEntry?

        /**
         * @return the expression to get the value for given declaration, or `null` if [IrGetValue] should be used.
         */
        abstract fun irGet(startOffset: Int, endOffset: Int, valueDeclaration: IrValueDeclaration): IrExpression?
    }

    abstract class LocalContextWithClosureAsParameters(
        override val sourceFileWhenInlined: IrFileEntry?,
    ) : LocalContext() {

        abstract val declaration: IrFunction
        abstract val transformedDeclaration: IrFunction

        val capturedValueToParameter: MutableMap<IrValueDeclaration, IrValueParameter> = mutableMapOf()

        override fun irGet(startOffset: Int, endOffset: Int, valueDeclaration: IrValueDeclaration): IrExpression? { TODO() }
    }

    class LocalFunctionContext(
        override val declaration: IrSimpleFunction,
        sourceFileWhenInlined: IrFileEntry?,
    ) :
        LocalContextWithClosureAsParameters(sourceFileWhenInlined) {
        lateinit var closure: Closure

        override lateinit var transformedDeclaration: IrSimpleFunction
    }

    private class LocalClassConstructorContext(
        override val declaration: IrConstructor,
        val inInlineFunctionScope: Boolean,
        sourceFileWhenInlined: IrFileEntry?,
    ) :
        LocalContextWithClosureAsParameters(sourceFileWhenInlined) {
        override lateinit var transformedDeclaration: IrConstructor
    }

    private class PotentiallyUnusedField {
        var symbolIfUsed: IrFieldSymbol? = null
            private set

        val symbol: IrFieldSymbol
            get() = symbolIfUsed ?: IrFieldSymbolImpl().also { symbolIfUsed = it }
    }

    private inner class LocalClassContext(
        val declaration: IrClass,
        val inPublicInlineScope: Boolean,
        val constructorContext: LocalContext?,
        override val sourceFileWhenInlined: IrFileEntry?,
    ) : LocalContext() {
        val numberOfOwnTypeParameters = declaration.typeParameters.size
        lateinit var closure: Closure

        // NOTE: This map is iterated over in `rewriteClassMembers` and we're relying on
        // the deterministic iteration order that `mutableMapOf` provides.
        val capturedValueToField: MutableMap<IrValueDeclaration, PotentiallyUnusedField> = mutableMapOf()

        override fun irGet(startOffset: Int, endOffset: Int, valueDeclaration: IrValueDeclaration): IrExpression? { TODO() }

        private fun IrValueDeclaration.isInlineDeclaration() =
            this is IrValueParameter && parent.let { it is IrFunction && it.isInline } && isInlineParameter()
    }

    private class LocalClassMemberContext(val member: IrDeclaration, val classContext: LocalClassContext) : LocalContext() {
        override val sourceFileWhenInlined
            get() = classContext.sourceFileWhenInlined

        override fun irGet(startOffset: Int, endOffset: Int, valueDeclaration: IrValueDeclaration): IrExpression? { TODO() }
    }

    private fun LocalContext.remapType(type: IrType): IrType { TODO() }

    private fun LocalContext.remapAllTypes(body: IrBody) { TODO() }

    private fun LocalContext.remapOnlyOwnTypeParameters(body: IrBody) { TODO() }

    private inner class LocalDeclarationsTransformer(
        val irElement: IrElement, val container: IrDeclaration,
        val transformedDeclarations: MutableMap<IrSymbolOwner, IrDeclaration>,
        val newParameterToCaptured: MutableMap<IrValueParameter, IrValueSymbol>,
        val newParameterToOld: MutableMap<IrValueParameter, IrValueParameter>,
        val oldParameterToNew: MutableMap<IrValueParameter, IrValueParameter>,
        val closestParent: IrDeclarationParent? = null,
        val dontCaptureOutsideOfContainer: Boolean = false,
    ) {
        val localFunctions: MutableMap<IrFunction, LocalFunctionContext> = LinkedHashMap()
        val localClasses: MutableMap<IrClass, LocalClassContext> = LinkedHashMap()
        val localClassConstructors: MutableMap<IrConstructor, LocalClassConstructorContext> = LinkedHashMap()

        val IrFunction.transformed: IrFunction?
            get() = transformedDeclarations[this] as IrFunction?

        fun lowerLocalDeclarations() { TODO() }

        private inner class FunctionBodiesRewriter(val localContext: LocalContext?) : IrElementTransformerVoid() {
            override fun visitClass(declaration: IrClass) =
                localClasses[declaration]?.declaration
                    ?: visitMember(declaration)
                    ?: super.visitClass(declaration)

            override fun visitFunction(declaration: IrFunction): IrStatement = TODO()

            override fun visitAnonymousInitializer(declaration: IrAnonymousInitializer): IrStatement = TODO()

            override fun visitField(declaration: IrField): IrStatement = TODO()

            private fun visitMember(declaration: IrDeclaration): IrStatement? = TODO()

            private fun visitWithTheSingleConstructorContext(declaration: IrDeclaration): IrStatement? { TODO() }

            override fun visitConstructor(declaration: IrConstructor): IrStatement { TODO() }

            override fun visitGetValue(expression: IrGetValue): IrExpression { TODO() }

            override fun visitSetValue(expression: IrSetValue): IrExpression { TODO() }

            override fun visitCall(expression: IrCall): IrExpression { TODO() }

            override fun visitConstructorCall(expression: IrConstructorCall): IrExpression { TODO() }

            override fun visitDelegatingConstructorCall(expression: IrDelegatingConstructorCall): IrExpression { TODO() }

            private fun <T : IrMemberAccessExpression<*>> T.fillArguments2(
                oldExpression: IrMemberAccessExpression<*>,
                newTarget: IrFunction,
            ): T { TODO() }

            private fun replacedCapturedValue(startOffset: Int, endOffset: Int, capturedValueSymbol: IrValueSymbol): IrExpression { TODO() }

            override fun visitFunctionReference(expression: IrFunctionReference): IrExpression { TODO() }

            // note: we don't need to upgrade property references as properties are not moved by the lowering
            override fun visitRichFunctionReference(expression: IrRichFunctionReference): IrExpression { TODO() }

            override fun visitReturn(expression: IrReturn): IrExpression { TODO() }

            override fun visitRawFunctionReference(expression: IrRawFunctionReference): IrExpression { TODO() }

            override fun visitDeclarationReference(expression: IrDeclarationReference): IrExpression { TODO() }

            override fun visitDeclaration(declaration: IrDeclarationBase): IrStatement { TODO() }
        }

        private inner class LocalClassTypeParameterRemapper(currentLocalClass: LocalClassContext?) : IrTypeParameterRemapper(
            currentLocalClass?.capturedTypeParameterToTypeParameter ?: emptyMap()
        ) {
            override fun remapTypeOrNull(type: IrType): IrType? { TODO() }
        }

        private fun rewriteFunctionBody(irDeclaration: IrElement, localContext: LocalContext?) { TODO() }

        private fun rewriteClassMembers(irClass: IrClass, localClassContext: LocalClassContext) { TODO() }

        private fun rewriteDeclarations() { TODO() }

        private fun createNewCall(oldCall: IrCall, newCallee: IrSimpleFunction) =
            IrCallImpl(
                oldCall.startOffset, oldCall.endOffset,
                oldCall.type,
                newCallee.symbol,
                typeArgumentsCount = newCallee.typeParameters.size,
                origin = oldCall.origin,
                superQualifierSymbol = oldCall.superQualifierSymbol
            ).also {
                it.setLocalTypeArguments(oldCall.symbol.owner)
                it.copyTypeArgumentsFrom(oldCall, shift = newCallee.typeParameters.size - oldCall.typeArguments.size)
            }

        private fun IrMemberAccessExpression<*>.setLocalTypeArguments(callee: IrFunction) { TODO() }

        private fun transformDeclarations() { TODO() }

        /**
         * This function removes [IrFunction]s created by `Fir2IrDeclarationStorage.fillUnboundSymbols()` to handle the code fragment
         * in the middle of code compiler of `KaCompilerFacility`. This function assumes two things about such [IrFunction]
         *  1. It is added to [IrClass] with [IrDeclarationOrigin.FILE_CLASS], [IrDeclarationOrigin.JVM_MULTIFILE_CLASS],
         *   or [IrDeclarationOrigin.SYNTHETIC_FILE_CLASS], so [IrDeclarationParent.isFacadeClass] is `true`.
         *  2. It has [IrDeclarationOrigin.FILLED_FOR_UNBOUND_SYMBOL] for its origin.
         */
        private fun cleanUpLocalFunctionsForUnboundSymbols(irFile: IrFile) { TODO() }

        private fun createTransformedSimpleFunction(localFunctionContext: LocalFunctionContext) { TODO() }

        private fun createTransformedValueParameters(
            capturedValues: List<IrValueSymbol>,
            localFunctionContext: LocalContext,
            oldDeclaration: IrFunction,
            newDeclaration: IrFunction,
            isExplicitLocalFunction: Boolean = false,
        ): List<IrValueParameter> { TODO() }

        private fun IrFunction.recordTransformedValueParameters(localContext: LocalContextWithClosureAsParameters) { TODO() }

        private fun createTransformedConstructorDeclaration(constructorContext: LocalClassConstructorContext) { TODO() }

        private fun createFieldsForCapturedValues(localClassContext: LocalClassContext): List<IrField> { TODO() }

        private fun <K, V> MutableMap<K, V>.putAbsentOrSame(key: K, value: V) { TODO() }

        private fun suggestNameForCapturedValue(
            declaration: IrValueDeclaration,
            usedNames: MutableSet<String>,
            isExplicitLocalFunction: Boolean = false,
        ): Name { TODO() }

        private inline fun findFirstUnusedName(initialName: String, usedNames: MutableSet<String>, nextName: (Int) -> String): Name {
            var chosen = initialName
            var suffix = 0
            while (!usedNames.add(chosen))
                chosen = nextName(++suffix)
            return Name.identifier(chosen)
        }

        private val CAPTURED_RECEIVER_PREFIX = "\$this\$"

        private fun IrValueParameter.isCapturedReceiver(): Boolean = TODO()

        private val IrValueParameter.parentNameSuffixForExtensionReceiver: String
            get() {
                val parentFun = parent as? IrSimpleFunction
                    ?: throw AssertionError("Extension receiver parent is not a simple function: ${parent.render()}")
                val correspondingProperty = parentFun.correspondingPropertySymbol?.owner
                return when {
                    correspondingProperty != null ->
                        correspondingProperty.name.asStringStripSpecialMarkers()
                    else ->
                        parentFun.name.asStringStripSpecialMarkers()
                }
            }

        private inline fun setClosures(block: (IrDeclaration) -> Closure) {
            localFunctions.forEach { [declaration, context] ->
                context.closure = block(declaration)
            }

            localClasses.forEach { [declaration, context] ->
                context.closure = block(declaration)
            }
        }

        private fun collectClosureForLocalDeclarations() { TODO() }

        private fun collectLocalDeclarations() { TODO() }
    }

    @JvmInline
    value class DeclarationKeyForScope private constructor(private val name: String) {
        companion object {
            fun createDeclarationKeyForScopeBySanitizedName(name: Name, nameSanitizer: (String) -> String): DeclarationKeyForScope = TODO()
        }
    }
}

// Local inner classes capture anything through outer
internal fun IrClass.isLocalNotInner(): Boolean = TODO()