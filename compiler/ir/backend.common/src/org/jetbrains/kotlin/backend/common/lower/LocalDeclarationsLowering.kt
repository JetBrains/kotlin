/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower

import org.jetbrains.kotlin.backend.common.BodyLoweringPass
import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.descriptors.synthesizedString
import org.jetbrains.kotlin.backend.common.lower.inline.isInlineParameter
import org.jetbrains.kotlin.backend.common.runOnFilePostfix
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.DescriptorVisibility
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
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
import org.jetbrains.kotlin.ir.transformStatement
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.Name

interface VisibilityPolicy {
    fun forClass(declaration: IrClass, inInlineFunctionScope: Boolean): DescriptorVisibility =
        declaration.visibility

    fun forConstructor(declaration: IrConstructor, inInlineFunctionScope: Boolean): DescriptorVisibility =
        DescriptorVisibilities.PRIVATE

    fun forCapturedField(value: IrValueSymbol): DescriptorVisibility =
        DescriptorVisibilities.PRIVATE

    companion object {
        val DEFAULT = object : VisibilityPolicy {}
    }
}

val IrDeclaration.parentsWithSelf: Sequence<IrDeclarationParent>
    get() = generateSequence(this as? IrDeclarationParent) { (it as? IrDeclaration)?.parent }

val IrDeclaration.parents: Sequence<IrDeclarationParent>
    get() = generateSequence(parent) { (it as? IrDeclaration)?.parent }

object BOUND_VALUE_PARAMETER : IrDeclarationOriginImpl("BOUND_VALUE_PARAMETER")

object BOUND_RECEIVER_PARAMETER : IrDeclarationOriginImpl("BOUND_RECEIVER_PARAMETER")

/*
  Local functions raised in LocalDeclarationLowering continue to refer to
  type parameters no longer visible to them.
  We add new type parameters to their declarations, which
  makes JVM accept those declarations. The generated IR is still
  semantically incorrect (TODO: needs further fix), but code generation seems
  to proceed nevertheless.
*/
class LocalDeclarationsLowering(
    val context: CommonBackendContext,
    val localNameSanitizer: (String) -> String = { it },
    val visibilityPolicy: VisibilityPolicy = VisibilityPolicy.DEFAULT,
    val suggestUniqueNames: Boolean = true, // When `true` appends a `$#index` suffix to lifted declaration names
    val compatibilityModeForInlinedLocalDelegatedPropertyAccessors: Boolean = false, // Keep old names because of KT-49030
    val forceFieldsForInlineCaptures: Boolean = false, // See `LocalClassContext`
    private val postLocalDeclarationLoweringCallback: ((IntermediateDatastructures) -> Unit)? = null
) :
    BodyLoweringPass {

    override fun lower(irFile: IrFile) {
        runOnFilePostfix(irFile)
    }

    object DECLARATION_ORIGIN_FIELD_FOR_CAPTURED_VALUE :
        IrDeclarationOriginImpl("FIELD_FOR_CAPTURED_VALUE", isSynthetic = true)

    object DECLARATION_ORIGIN_FIELD_FOR_CROSSINLINE_CAPTURED_VALUE :
        IrDeclarationOriginImpl("FIELD_FOR_CROSSINLINE_CAPTURED_VALUE", isSynthetic = true)

    override fun lower(irBody: IrBody, container: IrDeclaration) {
        LocalDeclarationsTransformer(irBody, container).lowerLocalDeclarations()
    }

    fun lower(irElement: IrElement, container: IrDeclaration, classesToLower: Set<IrClass>) {
        LocalDeclarationsTransformer(irElement, container, null, classesToLower).lowerLocalDeclarations()
    }

    fun lower(
        irBlock: IrBlock, container: IrDeclaration, closestParent: IrDeclarationParent,
        classesToLower: Set<IrClass>, functionsToSkip: Set<IrSimpleFunction>
    ) {
        LocalDeclarationsTransformer(irBlock, container, closestParent, classesToLower, functionsToSkip).lowerLocalDeclarations()
    }

    internal class ScopeWithCounter(val irElement: IrElement) {
        // Continuous numbering across all declarations in the container.
        var counter: Int = 0
        val usedLocalFunctionNames: MutableSet<Name> = hashSetOf()
    }

    internal class LocalScopeWithCounterMap {
        val scopeMap: MutableMap<IrSymbolOwner, ScopeWithCounter> = hashMapOf()
    }

    // Need to keep LocalFunctionContext.index
    private val IrSymbolOwner.scopeWithCounter: ScopeWithCounter
        get() = context.ir.localScopeWithCounterMap.scopeMap.getOrPut(this) {
            ScopeWithCounter(this)
        }

    abstract class LocalContext {
        val capturedTypeParameterToTypeParameter: MutableMap<IrTypeParameter, IrTypeParameter> = mutableMapOf()

        // By the time typeRemapper is used, the map will be already filled
        val typeRemapper = IrTypeParameterRemapper(capturedTypeParameterToTypeParameter)

        /**
         * @return the expression to get the value for given declaration, or `null` if [IrGetValue] should be used.
         */
        abstract fun irGet(startOffset: Int, endOffset: Int, valueDeclaration: IrValueDeclaration): IrExpression?
    }

    abstract class LocalContextWithClosureAsParameters : LocalContext() {

        abstract val declaration: IrFunction
        abstract val transformedDeclaration: IrFunction

        val capturedValueToParameter: MutableMap<IrValueDeclaration, IrValueParameter> = mutableMapOf()

        override fun irGet(startOffset: Int, endOffset: Int, valueDeclaration: IrValueDeclaration): IrExpression? {
            val parameter = capturedValueToParameter[valueDeclaration] ?: return null

            return IrGetValueImpl(startOffset, endOffset, parameter.type, parameter.symbol)
        }
    }

    sealed class OwnerForLoweredDeclaration(val isLocal: Boolean) {
        abstract fun addChild(declaration: IrDeclaration)
        abstract fun closestDeclarationParent(): IrDeclarationParent

        // Usually, just move local functions to the nearest class or file.
        class DeclarationContainer(private val irDeclarationContainer: IrDeclarationContainer) : OwnerForLoweredDeclaration(false) {
            override fun addChild(declaration: IrDeclaration) = irDeclarationContainer.addChild(declaration)

            override fun closestDeclarationParent() = irDeclarationContainer
        }

        // But, local functions defined in an inline lambda need to be popped up to the root inline call.
        class Block(private val irBlock: IrBlock, private val irDeclarationParent: IrDeclarationParent) : OwnerForLoweredDeclaration(true) {
            private val initialStatementsCount = irBlock.statements.size
            override fun addChild(declaration: IrDeclaration) {
                // Place all children at the block's start but in order they are being added.
                irBlock.statements.add(irBlock.statements.size - initialStatementsCount, declaration)
            }

            override fun closestDeclarationParent() = irDeclarationParent
        }
    }

    class LocalFunctionContext(
        override val declaration: IrSimpleFunction,
        val index: Int,
        val ownerForLoweredDeclaration: OwnerForLoweredDeclaration
    ) :
        LocalContextWithClosureAsParameters() {
        lateinit var closure: Closure

        override lateinit var transformedDeclaration: IrSimpleFunction
    }

    private class LocalClassConstructorContext(override val declaration: IrConstructor, val inInlineFunctionScope: Boolean) :
        LocalContextWithClosureAsParameters() {
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
        val inInlineFunctionScope: Boolean,
        val constructorContext: LocalContext?
    ) : LocalContext() {
        lateinit var closure: Closure

        // NOTE: This map is iterated over in `rewriteClassMembers` and we're relying on
        // the deterministic iteration order that `mutableMapOf` provides.
        val capturedValueToField: MutableMap<IrValueDeclaration, PotentiallyUnusedField> = mutableMapOf()

        override fun irGet(startOffset: Int, endOffset: Int, valueDeclaration: IrValueDeclaration): IrExpression? {
            // TODO: this used to be a hack for the JVM bytecode inliner (which misbehaved when inline lambdas had no fields),
            //  but it's no longer necessary. It is only here for backwards compatibility with old kotlinc versions
            //  and can be removed, probably in 1.9.
            if (!forceFieldsForInlineCaptures || !valueDeclaration.isInlineDeclaration()) {
                // We're in the initializer scope, which will be moved to a primary constructor later.
                // Thus we can directly use that constructor's context and read from a parameter instead of a field.
                constructorContext?.irGet(startOffset, endOffset, valueDeclaration)?.let { return it }
            }

            val field = capturedValueToField[valueDeclaration] ?: return null
            val receiver = declaration.thisReceiver!!
            return IrGetFieldImpl(
                startOffset, endOffset, field.symbol, valueDeclaration.type,
                receiver = IrGetValueImpl(startOffset, endOffset, receiver.type, receiver.symbol)
            )
        }

        private fun IrValueDeclaration.isInlineDeclaration() =
            this is IrValueParameter && parent.let { it is IrFunction && it.isInline } && isInlineParameter()
    }

    private class LocalClassMemberContext(val member: IrDeclaration, val classContext: LocalClassContext) : LocalContext() {
        override fun irGet(startOffset: Int, endOffset: Int, valueDeclaration: IrValueDeclaration): IrExpression? {
            val field = classContext.capturedValueToField[valueDeclaration] ?: return null
            // This lowering does not process accesses to outer `this`.
            val receiver = (if (member is IrFunction) member.dispatchReceiverParameter else classContext.declaration.thisReceiver)
                ?: error("No dispatch receiver parameter for ${member.render()}")
            return IrGetFieldImpl(
                startOffset, endOffset, field.symbol, valueDeclaration.type,
                receiver = IrGetValueImpl(startOffset, endOffset, receiver.type, receiver.symbol)
            )
        }
    }

    private fun LocalContext.remapType(type: IrType): IrType {
        if (capturedTypeParameterToTypeParameter.isEmpty()) return type
        return typeRemapper.remapType(type)
    }

    private fun LocalContext.remapTypes(body: IrBody) {
        if (capturedTypeParameterToTypeParameter.isEmpty()) return
        body.remapTypes(typeRemapper)
    }

    private inner class LocalDeclarationsTransformer(
        val irElement: IrElement, val container: IrDeclaration, val closestParent: IrDeclarationParent? = null,
        val classesToLower: Set<IrClass>? = null, val functionsToSkip: Set<IrSimpleFunction>? = null
    ) {
        val localFunctions: MutableMap<IrFunction, LocalFunctionContext> = LinkedHashMap()
        val localClasses: MutableMap<IrClass, LocalClassContext> = LinkedHashMap()
        val localClassConstructors: MutableMap<IrConstructor, LocalClassConstructorContext> = LinkedHashMap()

        val transformedDeclarations = mutableMapOf<IrSymbolOwner, IrDeclaration>()

        val IrFunction.transformed: IrFunction?
            get() = transformedDeclarations[this] as IrFunction?

        val newParameterToOld: MutableMap<IrValueParameter, IrValueParameter> = mutableMapOf()
        val oldParameterToNew: MutableMap<IrValueParameter, IrValueParameter> = mutableMapOf()
        val newParameterToCaptured: MutableMap<IrValueParameter, IrValueSymbol> = mutableMapOf()

        fun lowerLocalDeclarations() {
            collectLocalDeclarations()
            if (localFunctions.isEmpty() && localClasses.isEmpty()) return

            collectClosureForLocalDeclarations()

            transformDeclarations()

            rewriteDeclarations()

            insertLoweredDeclarationForLocalFunctions()

            postLocalDeclarationLoweringCallback?.invoke(
                IntermediateDatastructures(localFunctions, newParameterToOld, newParameterToCaptured)
            )
        }

        private fun insertLoweredDeclarationForLocalFunctions() {
            localFunctions.values.forEach { localContext ->
                localContext.transformedDeclaration.apply {
                    val original = localContext.declaration

                    this.body = original.body
                    this.body?.let { localContext.remapTypes(it) }

                    original.valueParameters.filter { v -> v.defaultValue != null }.forEach { argument ->
                        val body = argument.defaultValue!!
                        localContext.remapTypes(body)
                        oldParameterToNew[argument]!!.defaultValue = body
                    }
                    acceptChildren(SetDeclarationsParentVisitor, this)
                }
                localContext.ownerForLoweredDeclaration.addChild(localContext.transformedDeclaration)
            }
        }


        private inner class FunctionBodiesRewriter(val localContext: LocalContext?) : IrElementTransformerVoid() {
            override fun visitLocalDelegatedProperty(declaration: IrLocalDelegatedProperty): IrStatement =
                // Both accessors extracted as closures.
                declaration.delegate.transformStatement(this)

            override fun visitClass(declaration: IrClass) =
                localClasses[declaration]?.declaration
                    ?: visitMember(declaration)
                    ?: super.visitClass(declaration)

            override fun visitFunction(declaration: IrFunction): IrStatement =
                if (declaration in localFunctions) {
                    // Replace local function definition with an empty composite.
                    IrCompositeImpl(declaration.startOffset, declaration.endOffset, context.irBuiltIns.unitType)
                } else {
                    visitMember(declaration) ?: super.visitFunction(declaration)
                }

            private fun visitMember(declaration: IrDeclaration): IrStatement? =
                if (localContext is LocalClassContext && declaration.parent == localContext.declaration) {
                    val classMemberLocalContext = LocalClassMemberContext(declaration, localContext)
                    declaration.apply { transformChildrenVoid(FunctionBodiesRewriter(classMemberLocalContext)) }
                } else {
                    null
                }

            override fun visitConstructor(declaration: IrConstructor): IrStatement {
                // Body is transformed separately. See loop over constructors in rewriteDeclarations().

                val constructorContext = localClassConstructors[declaration] ?: return super.visitConstructor(declaration)
                return constructorContext.transformedDeclaration.apply {
                    this.body = declaration.body!!

                    declaration.valueParameters.filter { it.defaultValue != null }.forEach { argument ->
                        oldParameterToNew[argument]!!.defaultValue = argument.defaultValue
                    }
                    acceptChildren(SetDeclarationsParentVisitor, this)
                }
            }

            override fun visitGetValue(expression: IrGetValue): IrExpression {
                val declaration = expression.symbol.owner

                localContext?.irGet(expression.startOffset, expression.endOffset, declaration)?.let {
                    return it
                }

                oldParameterToNew[declaration]?.let {
                    return IrGetValueImpl(expression.startOffset, expression.endOffset, it.type, it.symbol)
                }

                return expression
            }

            override fun visitSetValue(expression: IrSetValue): IrExpression {
                expression.transformChildrenVoid(this)

                val declaration = expression.symbol.owner
                oldParameterToNew[declaration]?.let {
                    return IrSetValueImpl(
                        expression.startOffset,
                        expression.endOffset,
                        it.type,
                        it.symbol,
                        expression.value,
                        expression.origin
                    )
                }
                return expression
            }

            override fun visitCall(expression: IrCall): IrExpression {
                expression.transformChildrenVoid(this)

                val oldCallee = expression.symbol.owner
                val newCallee = (oldCallee.transformed ?: return expression) as IrSimpleFunction

                return createNewCall(expression, newCallee).fillArguments2(expression, newCallee)
            }

            override fun visitConstructorCall(expression: IrConstructorCall): IrExpression {
                expression.transformChildrenVoid(this)

                val oldCallee = expression.symbol.owner
                val newCallee = (oldCallee.transformed ?: return expression) as IrConstructor

                return createNewCall(expression, newCallee).fillArguments2(expression, newCallee)
            }

            override fun visitDelegatingConstructorCall(expression: IrDelegatingConstructorCall): IrExpression {
                expression.transformChildrenVoid(this)

                val oldCallee = expression.symbol.owner
                val newCallee = transformedDeclarations[oldCallee] as IrConstructor? ?: return expression

                return IrDelegatingConstructorCallImpl(
                    expression.startOffset, expression.endOffset,
                    context.irBuiltIns.unitType,
                    newCallee.symbol,
                    typeArgumentsCount = expression.typeArgumentsCount,
                    valueArgumentsCount = newCallee.valueParameters.size
                ).also {
                    it.fillArguments2(expression, newCallee)
                    it.copyTypeArgumentsFrom(expression)
                }
            }

            inline fun <T : IrMemberAccessExpression<*>> T.mapValueParameters(
                newTarget: IrFunction,
                transform: (IrValueParameter) -> IrExpression?
            ): T =
                apply {
                    for (p in newTarget.valueParameters) {
                        putValueArgument(p.index, transform(p))
                    }
                }

            private fun <T : IrMemberAccessExpression<*>> T.fillArguments2(
                oldExpression: IrMemberAccessExpression<*>,
                newTarget: IrFunction
            ): T {
                mapValueParameters(newTarget) { newValueParameterDeclaration ->
                    val oldParameter = newParameterToOld[newValueParameterDeclaration]

                    if (oldParameter != null) {
                        oldExpression.getValueArgument(oldParameter.index)
                    } else {
                        // The callee expects captured value as argument.
                        val capturedValueSymbol =
                            newParameterToCaptured[newValueParameterDeclaration]
                                ?: throw AssertionError("Non-mapped parameter $newValueParameterDeclaration")

                        val capturedValue = capturedValueSymbol.owner

                        localContext?.irGet(oldExpression.startOffset, oldExpression.endOffset, capturedValue) ?: run {
                            // Captured value is directly available for the caller.
                            val value = oldParameterToNew[capturedValue] ?: capturedValue
                            IrGetValueImpl(oldExpression.startOffset, oldExpression.endOffset, value.symbol)
                        }
                    }

                }

                dispatchReceiver = oldExpression.dispatchReceiver
                extensionReceiver = oldExpression.extensionReceiver

                return this
            }

            override fun visitFunctionReference(expression: IrFunctionReference): IrExpression {
                expression.transformChildrenVoid(this)

                val oldCallee = expression.symbol.owner
                val newCallee = oldCallee.transformed ?: return expression
                val newReflectionTarget = expression.reflectionTarget?.run { owner.transformed ?: owner }

                val typeParameters = if (newCallee is IrConstructor)
                    newCallee.parentAsClass.typeParameters
                else
                    newCallee.typeParameters
                return IrFunctionReferenceImpl(
                    expression.startOffset, expression.endOffset,
                    expression.type, // TODO functional type for transformed descriptor
                    newCallee.symbol,
                    typeArgumentsCount = typeParameters.size,
                    valueArgumentsCount = newCallee.valueParameters.size,
                    reflectionTarget = newReflectionTarget?.symbol,
                    origin = expression.origin
                ).also {
                    it.fillArguments2(expression, newCallee)
                    it.setLocalTypeArguments(oldCallee)
                    it.copyTypeArgumentsFrom(expression, shift = typeParameters.size - expression.typeArgumentsCount)
                    it.copyAttributes(expression)
                }
            }

            override fun visitReturn(expression: IrReturn): IrExpression {
                expression.transformChildrenVoid(this)

                val oldReturnTarget = expression.returnTargetSymbol.owner as? IrFunction ?: return expression
                val newReturnTarget = oldReturnTarget.transformed ?: return expression

                return IrReturnImpl(
                    expression.startOffset, expression.endOffset,
                    context.irBuiltIns.nothingType,
                    newReturnTarget.symbol, expression.value
                )
            }

            override fun visitDeclarationReference(expression: IrDeclarationReference): IrExpression {
                if (expression.symbol.owner in transformedDeclarations) {
                    TODO()
                }
                return super.visitDeclarationReference(expression)
            }

            override fun visitDeclaration(declaration: IrDeclarationBase): IrStatement {
                if (declaration in transformedDeclarations) {
                    TODO()
                }
                return super.visitDeclaration(declaration)
            }
        }

        private fun rewriteFunctionBody(irDeclaration: IrElement, localContext: LocalContext?) {
            irDeclaration.transformChildrenVoid(FunctionBodiesRewriter(localContext))
        }

        private fun rewriteClassMembers(irClass: IrClass, localClassContext: LocalClassContext) {
            val constructors = irClass.declarations.filterIsInstance<IrConstructor>()

            irClass.transformChildrenVoid(FunctionBodiesRewriter(localClassContext))

            // NOTE: if running before InitializersLowering, we can instead look for constructors that have
            //   IrInstanceInitializerCall. However, Native runs these two lowerings in opposite order.
            val constructorsByDelegationKinds: Map<ConstructorDelegationKind, List<LocalClassConstructorContext>> = constructors
                .asSequence()
                .map { localClassConstructors[it]!! }
                .groupBy { it.declaration.delegationKind(context.irBuiltIns) }

            val constructorsCallingSuper = constructorsByDelegationKinds[ConstructorDelegationKind.CALLS_SUPER].orEmpty()

            assert(constructorsCallingSuper.isNotEmpty() || constructorsByDelegationKinds[ConstructorDelegationKind.PARTIAL_LINKAGE_ERROR] != null) {
                "Expected at least one constructor calling super; class: $irClass"
            }

            val usedCaptureFields = createFieldsForCapturedValues(localClassContext)
            irClass.declarations += usedCaptureFields

            context.mapping.capturedFields[irClass] =
                (context.mapping.capturedFields[irClass] ?: emptyList()) + usedCaptureFields

            for (constructorContext in constructorsCallingSuper) {
                val blockBody = constructorContext.declaration.body as? IrBlockBody
                    ?: throw AssertionError("Unexpected constructor body: ${constructorContext.declaration.body}")

                // NOTE: It's important to set the fields for captured values in the same order as the arguments,
                // since `AnonymousObjectTransformer` relies on this ordering.
                blockBody.statements.addAll(
                    0,
                    localClassContext.capturedValueToField.mapNotNull { (capturedValue, field) ->
                        val symbol = field.symbolIfUsed ?: return@mapNotNull null
                        IrSetFieldImpl(
                            UNDEFINED_OFFSET, UNDEFINED_OFFSET, symbol,
                            IrGetValueImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, irClass.thisReceiver!!.symbol),
                            constructorContext.irGet(UNDEFINED_OFFSET, UNDEFINED_OFFSET, capturedValue)!!,
                            context.irBuiltIns.unitType,
                            LoweredStatementOrigins.STATEMENT_ORIGIN_INITIALIZER_OF_FIELD_FOR_CAPTURED_VALUE
                        )
                    }
                )
            }
        }

        private fun rewriteDeclarations() {
            localFunctions.values.forEach {
                rewriteFunctionBody(it.declaration, it)
            }

            localClassConstructors.values.forEach {
                rewriteFunctionBody(it.declaration, it)
            }

            localClasses.values.forEach {
                rewriteClassMembers(it.declaration, it)
            }

            rewriteFunctionBody(irElement, null)
        }

        private fun createNewCall(oldCall: IrCall, newCallee: IrSimpleFunction) =
            IrCallImpl(
                oldCall.startOffset, oldCall.endOffset,
                oldCall.type,
                newCallee.symbol,
                typeArgumentsCount = newCallee.typeParameters.size,
                valueArgumentsCount = newCallee.valueParameters.size,
                origin = oldCall.origin,
                superQualifierSymbol = oldCall.superQualifierSymbol
            ).also {
                it.setLocalTypeArguments(oldCall.symbol.owner)
                it.copyTypeArgumentsFrom(oldCall, shift = newCallee.typeParameters.size - oldCall.typeArgumentsCount)
            }

        private fun createNewCall(oldCall: IrConstructorCall, newCallee: IrConstructor) =
            IrConstructorCallImpl.fromSymbolOwner(
                oldCall.startOffset, oldCall.endOffset,
                oldCall.type,
                newCallee.symbol,
                newCallee.parentAsClass.typeParameters.size,
                oldCall.origin
            ).also {
                it.copyTypeArgumentsFrom(oldCall)
            }

        private fun IrMemberAccessExpression<*>.setLocalTypeArguments(callee: IrFunction) {
            val context = localFunctions[callee] ?: return
            for ((outerTypeParameter, innerTypeParameter) in context.capturedTypeParameterToTypeParameter) {
                putTypeArgument(innerTypeParameter.index, outerTypeParameter.defaultType) // TODO: remap default type!
            }
        }

        private fun transformDeclarations() {
            localFunctions.values.forEach {
                createLiftedDeclaration(it)
            }

            localClasses.values.forEach {
                it.declaration.visibility = visibilityPolicy.forClass(it.declaration, it.inInlineFunctionScope)
                it.closure.capturedValues.associateTo(it.capturedValueToField) { capturedValue ->
                    capturedValue.owner to PotentiallyUnusedField()
                }
            }

            localClassConstructors.values.forEach {
                createTransformedConstructorDeclaration(it)
            }
        }

        private fun suggestLocalName(declaration: IrDeclarationWithName): String {
            val declarationName = localNameSanitizer(declaration.name.asString())
            localFunctions[declaration]?.let {
                val baseName = if (declaration.name.isSpecial) "lambda" else declarationName
                if (it.index >= 0) {
                    if (!suggestUniqueNames) return baseName

                    val separator = if (
                        compatibilityModeForInlinedLocalDelegatedPropertyAccessors &&
                        declaration.origin == IrDeclarationOrigin.DELEGATED_PROPERTY_ACCESSOR &&
                        container is IrFunction && container.isInline
                    ) "-" else "$"
                    return "$baseName$separator${it.index}"
                }
            }

            return declarationName
        }

        private fun generateNameForLiftedDeclaration(
            declaration: IrDeclaration,
            newOwner: IrDeclarationParent
        ): Name {
            val parents = declaration.parentsWithSelf.takeWhile { it != newOwner }.toList().reversed()
            val nameFromParents = parents.joinToString(separator = "$") { suggestLocalName(it as IrDeclarationWithName) }
            // Local functions declared in anonymous initializers have classes as their parents.
            // Such anonymous initializers, however, are inlined into the constructors delegating to super class constructor.
            // There can be local functions declared in local function in init blocks (and further),
            // but such functions would have proper "safe" names (outerLocalFun1$outerLocalFun2$...$localFun).
            return if (parents.size == 1 && declaration.parent is IrClass)
                Name.identifier("_init_\$$nameFromParents")
            else
                Name.identifier(nameFromParents)
        }

        private fun createLiftedDeclaration(localFunctionContext: LocalFunctionContext) {
            val oldDeclaration = localFunctionContext.declaration
            if (oldDeclaration.dispatchReceiverParameter != null) {
                throw AssertionError("local functions must not have dispatch receiver")
            }

            val owner = localFunctionContext.ownerForLoweredDeclaration
            val ownerParent = owner.closestDeclarationParent()
            val newName = generateNameForLiftedDeclaration(oldDeclaration, ownerParent)

            // TODO: consider using fields to access the closure of enclosing class.
            val (capturedValues, capturedTypeParameters) = localFunctionContext.closure

            val newDeclaration = context.irFactory.buildFun {
                updateFrom(oldDeclaration)
                name = newName
                visibility = if (owner.isLocal) DescriptorVisibilities.LOCAL else DescriptorVisibilities.PRIVATE
                modality = Modality.FINAL
            }

            localFunctionContext.transformedDeclaration = newDeclaration

            val newTypeParameters = newDeclaration.copyTypeParameters(capturedTypeParameters)
            localFunctionContext.capturedTypeParameterToTypeParameter.putAll(
                capturedTypeParameters.zip(newTypeParameters)
            )
            newDeclaration.copyTypeParametersFrom(oldDeclaration, parameterMap = localFunctionContext.capturedTypeParameterToTypeParameter)
            localFunctionContext.capturedTypeParameterToTypeParameter.putAll(
                oldDeclaration.typeParameters.zip(newDeclaration.typeParameters.drop(newTypeParameters.size))
            )
            // Type parameters of oldDeclaration may depend on captured type parameters, so deal with that after copying.
            newDeclaration.typeParameters.drop(newTypeParameters.size).forEach { tp ->
                tp.superTypes = tp.superTypes.map { localFunctionContext.remapType(it) }
            }

            newDeclaration.parent = ownerParent
            newDeclaration.returnType = localFunctionContext.remapType(oldDeclaration.returnType)
            newDeclaration.dispatchReceiverParameter = null
            newDeclaration.extensionReceiverParameter = oldDeclaration.extensionReceiverParameter?.run {
                copyTo(newDeclaration, type = localFunctionContext.remapType(this.type)).also {
                    newParameterToOld.putAbsentOrSame(it, this)
                }
            }
            newDeclaration.copyAttributes(oldDeclaration)

            newDeclaration.valueParameters += createTransformedValueParameters(
                capturedValues, localFunctionContext, oldDeclaration, newDeclaration,
                isExplicitLocalFunction = oldDeclaration.origin == IrDeclarationOrigin.LOCAL_FUNCTION
            )
            newDeclaration.recordTransformedValueParameters(localFunctionContext)
            val parametersMapping = buildMap {
                oldDeclaration.extensionReceiverParameter?.let { put(it, newDeclaration.extensionReceiverParameter!!) }
                putAll(oldDeclaration.valueParameters zip newDeclaration.valueParameters.takeLast(oldDeclaration.valueParameters.size))
            }
            context.remapMultiFieldValueClassStructure(oldDeclaration, newDeclaration, parametersMapping)

            newDeclaration.annotations = oldDeclaration.annotations

            transformedDeclarations[oldDeclaration] = newDeclaration
        }

        private fun createTransformedValueParameters(
            capturedValues: List<IrValueSymbol>,
            localFunctionContext: LocalContext,
            oldDeclaration: IrFunction,
            newDeclaration: IrFunction,
            isExplicitLocalFunction: Boolean = false
        ) = ArrayList<IrValueParameter>(capturedValues.size + oldDeclaration.valueParameters.size).apply {
            val generatedNames = mutableSetOf<String>()
            capturedValues.mapIndexedTo(this) { i, capturedValue ->
                val p = capturedValue.owner
                buildValueParameter(newDeclaration) {
                    startOffset = p.startOffset
                    endOffset = p.endOffset
                    origin =
                        if (p is IrValueParameter && p.index < 0 && newDeclaration is IrConstructor) BOUND_RECEIVER_PARAMETER
                        else BOUND_VALUE_PARAMETER
                    name = suggestNameForCapturedValue(p, generatedNames, isExplicitLocalFunction = isExplicitLocalFunction)
                    index = i
                    type = localFunctionContext.remapType(p.type)
                    isCrossInline = (capturedValue as? IrValueParameterSymbol)?.owner?.isCrossinline == true
                    isNoinline = (capturedValue as? IrValueParameterSymbol)?.owner?.isNoinline == true
                }.also {
                    newParameterToCaptured[it] = capturedValue
                }
            }

            oldDeclaration.valueParameters.mapTo(this) { v ->
                v.copyTo(
                    newDeclaration,
                    index = v.index + capturedValues.size,
                    type = localFunctionContext.remapType(v.type),
                    varargElementType = v.varargElementType?.let { localFunctionContext.remapType(it) }
                ).also {
                    newParameterToOld.putAbsentOrSame(it, v)
                }
            }
        }

        private fun IrFunction.recordTransformedValueParameters(localContext: LocalContextWithClosureAsParameters) {

            valueParameters.forEach {
                val capturedValue = newParameterToCaptured[it]
                if (capturedValue != null) {
                    localContext.capturedValueToParameter[capturedValue.owner] = it
                }
            }

            (listOfNotNull(dispatchReceiverParameter, extensionReceiverParameter) + valueParameters).forEach {
                val oldParameter = newParameterToOld[it]
                if (oldParameter != null) {
                    oldParameterToNew.putAbsentOrSame(oldParameter, it)
                }
            }

        }

        private fun createTransformedConstructorDeclaration(constructorContext: LocalClassConstructorContext) {
            val oldDeclaration = constructorContext.declaration

            val localClassContext = localClasses[oldDeclaration.parent]!!
            val capturedValues = localClassContext.closure.capturedValues

            val newDeclaration = context.irFactory.buildConstructor {
                updateFrom(oldDeclaration)
                visibility = visibilityPolicy.forConstructor(oldDeclaration, constructorContext.inInlineFunctionScope)
                returnType = oldDeclaration.returnType
            }

            constructorContext.transformedDeclaration = newDeclaration

            newDeclaration.parent = localClassContext.declaration
            newDeclaration.copyTypeParametersFrom(oldDeclaration)

            oldDeclaration.dispatchReceiverParameter?.run {
                throw AssertionError("Local class constructor can't have dispatch receiver: ${ir2string(oldDeclaration)}")
            }
            oldDeclaration.extensionReceiverParameter?.run {
                throw AssertionError("Local class constructor can't have extension receiver: ${ir2string(oldDeclaration)}")
            }

            newDeclaration.valueParameters += createTransformedValueParameters(
                capturedValues, localClassContext, oldDeclaration, newDeclaration
            )
            newDeclaration.recordTransformedValueParameters(constructorContext)

            newDeclaration.annotations = oldDeclaration.annotations

            newDeclaration.metadata = oldDeclaration.metadata

            transformedDeclarations[oldDeclaration] = newDeclaration
            context.mapping.capturedConstructors[oldDeclaration] = newDeclaration
        }

        private fun createFieldsForCapturedValues(localClassContext: LocalClassContext): List<IrField> {
            val classDeclaration = localClassContext.declaration
            val generatedNames = mutableSetOf<String>()
            return localClassContext.capturedValueToField.mapNotNull { (capturedValue, field) ->
                val symbol = field.symbolIfUsed ?: return@mapNotNull null
                val origin = if (capturedValue is IrValueParameter && capturedValue.isCrossinline)
                    DECLARATION_ORIGIN_FIELD_FOR_CROSSINLINE_CAPTURED_VALUE
                else
                    DECLARATION_ORIGIN_FIELD_FOR_CAPTURED_VALUE
                context.irFactory.createField(
                    classDeclaration.startOffset,
                    classDeclaration.endOffset,
                    origin,
                    symbol,
                    suggestNameForCapturedValue(capturedValue, generatedNames),
                    capturedValue.type,
                    visibilityPolicy.forCapturedField(capturedValue.symbol),
                    isFinal = true, isExternal = false, isStatic = false,
                ).also {
                    it.parent = classDeclaration
                }
            }
        }

        private fun <K, V> MutableMap<K, V>.putAbsentOrSame(key: K, value: V) {
            val current = this.getOrPut(key) { value }

            if (current != value) {
                error("$current != $value")
            }
        }

        private fun suggestNameForCapturedValue(declaration: IrValueDeclaration, usedNames: MutableSet<String>, isExplicitLocalFunction: Boolean = false): Name {
            if (declaration is IrValueParameter) {
                if (declaration.name.asString() == "<this>" && declaration.isDispatchReceiver()) {
                    return findFirstUnusedName("this\$0", usedNames) {
                        "this\$$it"
                    }
                } else if (declaration.name.asString() == "<this>" && declaration.isExtensionReceiver()) {
                    val parentNameSuffix = declaration.parentNameSuffixForExtensionReceiver
                    return findFirstUnusedName("\$this_$parentNameSuffix", usedNames) {
                        "\$this_$parentNameSuffix\$$it"
                    }
                } else if (declaration.isCapturedReceiver()) {
                    val baseName = declaration.name.asString().removePrefix(CAPTURED_RECEIVER_PREFIX)
                    return findFirstUnusedName("\$this_$baseName", usedNames) {
                        "\$this_$baseName\$$it"
                    }
                }
            }

            val base = if (declaration.name.isSpecial) {
                declaration.name.asStringStripSpecialMarkers()
            } else {
                declaration.name.asString()
            }

            return if (isExplicitLocalFunction && declaration is IrVariable) {
                findFirstUnusedName(base, usedNames) {
                    "$base$$it"
                }
            } else {
                findFirstUnusedName(base.synthesizedString, usedNames) {
                    "$base$$it".synthesizedString
                }
            }
        }

        private inline fun findFirstUnusedName(initialName: String, usedNames: MutableSet<String>, nextName: (Int) -> String): Name {
            var chosen = initialName
            var suffix = 0
            while (!usedNames.add(chosen))
                chosen = nextName(++suffix)
            return Name.identifier(chosen)
        }

        private fun IrValueParameter.isDispatchReceiver(): Boolean =
            when (val parent = this.parent) {
                is IrFunction ->
                    parent.dispatchReceiverParameter == this
                is IrClass ->
                    parent.thisReceiver == this
                else ->
                    false
            }

        private fun IrValueParameter.isExtensionReceiver(): Boolean {
            val parentFun = parent as? IrFunction ?: return false
            return parentFun.extensionReceiverParameter == this
        }

        private val CAPTURED_RECEIVER_PREFIX = "\$this\$"

        private fun IrValueParameter.isCapturedReceiver(): Boolean =
            name.asString().startsWith(CAPTURED_RECEIVER_PREFIX)

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

        private fun collectClosureForLocalDeclarations() {
            //TODO: maybe use for granular declarations
            val annotator = ClosureAnnotator(irElement, container)

            localFunctions.forEach { (declaration, context) ->
                context.closure = annotator.getFunctionClosure(declaration)
            }

            localClasses.forEach { (declaration, context) ->
                context.closure = annotator.getClassClosure(declaration)
            }
        }

        private fun collectLocalDeclarations() {
            val enclosingPackageFragment = container.getPackageFragment()
            val enclosingClass = run {
                var currentParent = container as? IrClass ?: container.parent
                while (currentParent is IrDeclaration && currentParent !is IrClass) {
                    currentParent = currentParent.parent
                }

                currentParent as? IrClass
            }

            class Data(val currentClass: ScopeWithCounter?, val isInInlineFunction: Boolean) {
                fun withCurrentClass(currentClass: IrClass): Data =
                    // Don't cache local declarations
                    Data(ScopeWithCounter(currentClass), isInInlineFunction)

                fun withInline(isInline: Boolean): Data =
                    if (isInline && !isInInlineFunction) Data(currentClass, true) else this
            }

            irElement.accept(object : IrElementVisitor<Unit, Data> {
                override fun visitElement(element: IrElement, data: Data) {
                    element.acceptChildren(this, data)
                }

                override fun visitFunctionExpression(expression: IrFunctionExpression, data: Data) {
                    // TODO: For now IrFunctionExpression can only be encountered here if this was called from the inliner,
                    // then all IrFunctionExpression will be replaced by IrFunctionReferenceExpression.
                    // Don't forget to fix this when that replacement has been dropped.
                    // Also, a note: even if a lambda is not an inline one, there still cannot be a reference to it
                    // from an outside declaration, so it is safe to skip them here and correctly handle later, after the above conversion.
                    expression.function.acceptChildren(this, data)
                }

                override fun visitSimpleFunction(declaration: IrSimpleFunction, data: Data) {
                    if (functionsToSkip?.contains(declaration) == true) return
                    super.visitSimpleFunction(declaration, data.withInline(declaration.isInline))

                    if (declaration.visibility == DescriptorVisibilities.LOCAL) {
                        val enclosingScope = data.currentClass
                            ?: enclosingClass?.scopeWithCounter
                            // File is required for K/N because file declarations are not split by classes.
                            ?: enclosingPackageFragment.scopeWithCounter
                        val index =
                            if (declaration.name.isSpecial || declaration.name in enclosingScope.usedLocalFunctionNames)
                                enclosingScope.counter++
                            else -1
                        val ownerForLoweredDeclaration =
                            data.currentClass?.let { OwnerForLoweredDeclaration.DeclarationContainer(it.irElement as IrDeclarationContainer) }
                                ?: (irElement as? IrBlock)?.let { OwnerForLoweredDeclaration.Block(irElement, closestParent!!) }
                                ?: OwnerForLoweredDeclaration.DeclarationContainer(enclosingScope.irElement as IrDeclarationContainer)
                        localFunctions[declaration] = LocalFunctionContext(declaration, index, ownerForLoweredDeclaration)

                        enclosingScope.usedLocalFunctionNames.add(declaration.name)
                    }
                }

                override fun visitConstructor(declaration: IrConstructor, data: Data) {
                    super.visitConstructor(declaration, data)

                    if (!declaration.constructedClass.isLocalNotInner()) return

                    localClassConstructors[declaration] = LocalClassConstructorContext(declaration, data.inInlineFunctionScope)
                }

                override fun visitClass(declaration: IrClass, data: Data) {
                    if (classesToLower?.contains(declaration) == false) return
                    super.visitClass(declaration, data.withCurrentClass(declaration))

                    if (!declaration.isLocalNotInner()) return

                    // If there are many non-delegating constructors, each copy of the initializer requires different remapping:
                    //   class C {
                    //     constructor() {}
                    //     constructor(x: Int) {}
                    //     val x = y // which constructor's parameter?
                    //   }
                    // TODO: this should ideally run after initializers are added to constructors, but that'd place
                    //   other restrictions on IR (e.g. after the initializers are moved you can no longer create fields
                    //   with initializers) which makes that hard to implement.
                    val constructorContext = declaration.constructors.mapNotNull { localClassConstructors[it] }
                        .singleOrNull { it.declaration.delegationKind(context.irBuiltIns) == ConstructorDelegationKind.CALLS_SUPER }
                    localClasses[declaration] = LocalClassContext(declaration, data.inInlineFunctionScope, constructorContext)
                }

                private val Data.inInlineFunctionScope: Boolean
                    get() = isInInlineFunction ||
                            generateSequence(container) { it.parent as? IrDeclaration }.any { it is IrFunction && it.isInline }
            }, Data(null, false))
        }
    }

    data class IntermediateDatastructures(
        val localFunctions: Map<IrFunction, LocalFunctionContext>,
        val newParameterToOld: Map<IrValueParameter, IrValueParameter>,
        val newParameterToCaptured: Map<IrValueParameter, IrValueSymbol>
    )
}

// Local inner classes capture anything through outer
internal fun IrClass.isLocalNotInner(): Boolean = visibility == DescriptorVisibilities.LOCAL && !isInner
