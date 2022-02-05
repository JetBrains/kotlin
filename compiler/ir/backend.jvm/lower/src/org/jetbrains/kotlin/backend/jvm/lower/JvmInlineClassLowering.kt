/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.ir.copyParameterDeclarationsFrom
import org.jetbrains.kotlin.backend.common.ir.copyTo
import org.jetbrains.kotlin.backend.common.ir.passTypeArgumentsFrom
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.lower.irBlockBody
import org.jetbrains.kotlin.backend.common.lower.loops.forLoopsPhase
import org.jetbrains.kotlin.backend.common.phaser.makeIrFilePhase
import org.jetbrains.kotlin.backend.common.pop
import org.jetbrains.kotlin.backend.common.push
import org.jetbrains.kotlin.backend.jvm.*
import org.jetbrains.kotlin.backend.jvm.ir.erasedUpperBound
import org.jetbrains.kotlin.backend.jvm.ir.isInlineClassType
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.config.ApiVersion
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.addConstructor
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrFunctionImpl
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrValueSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrSimpleFunctionSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrValueParameterSymbolImpl
import org.jetbrains.kotlin.ir.transformStatement
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.JVM_INLINE_ANNOTATION_FQ_NAME
import org.jetbrains.kotlin.util.OperatorNameConventions
import org.jetbrains.kotlin.utils.addToStdlib.cast
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

val jvmInlineClassPhase = makeIrFilePhase(
    ::JvmInlineClassLowering,
    name = "Inline Classes",
    description = "Lower inline classes",
    // forLoopsPhase may produce UInt and ULong which are inline classes.
    // Standard library replacements are done on the unmangled names for UInt and ULong classes.
    // Collection stubs may require mangling by inline class rules.
    // SAM wrappers may require mangling for fun interfaces with inline class parameters
    prerequisite = setOf(forLoopsPhase, jvmBuiltInsPhase, collectionStubMethodLowering, singleAbstractMethodPhase),
)

/**
 * Adds new constructors, box, and unbox functions to inline classes as well as replacement
 * functions and bridges to avoid clashes between overloaded function. Changes calls with
 * known types to call the replacement functions.
 *
 * We do not unfold inline class types here. Instead, the type mapper will lower inline class
 * types to the types of their underlying field.
 */
private class JvmInlineClassLowering(private val context: JvmBackendContext) : FileLoweringPass, IrElementTransformerVoidWithContext() {
    private val valueMap = mutableMapOf<IrValueSymbol, IrValueDeclaration>()

    private fun addBindingsFor(original: IrFunction, replacement: IrFunction) {
        for ((param, newParam) in original.explicitParameters.zip(replacement.explicitParameters)) {
            valueMap[param.symbol] = newParam
        }
    }

    override fun lower(irFile: IrFile) {
        irFile.transformChildrenVoid()
    }

    override fun visitClassNew(declaration: IrClass): IrStatement {
        // The arguments to the primary constructor are in scope in the initializers of IrFields.
        declaration.primaryConstructor?.let {
            context.inlineClassReplacements.getReplacementFunction(it)?.let { replacement -> addBindingsFor(it, replacement) }
        }

        declaration.transformDeclarationsFlat { memberDeclaration ->
            if (memberDeclaration is IrFunction) {
                transformFunctionFlat(memberDeclaration)
            } else {
                memberDeclaration.accept(this, null)
                null
            }
        }

        if (declaration.isInline) {
            val irConstructor = declaration.primaryConstructor!!
            // The field getter is used by reflection and cannot be removed here unless it is internal.
            declaration.declarations.removeIf {
                (it == irConstructor && declaration.modality != Modality.SEALED) ||
                        (it is IrFunction && it.isInlineClassFieldGetter && !it.visibility.isPublicAPI)
            }

            if (declaration.isChildOfSealedInlineClass()) {
                updateGetterForSealedInlineClassChild(declaration)
            }

            if (!declaration.isChildOfSealedInlineClass()) {
                buildPrimaryInlineClassConstructor(declaration, irConstructor)
                buildBoxFunction(declaration)
                buildUnboxFunction(declaration)
            }
            buildSpecializedEqualsMethod(declaration)

            if (declaration.modality == Modality.SEALED) {
                patchReceiverParameterOfValueGetter(declaration)

                val inlineSubclasses = collectSubclasses(declaration) { it.owner.isInline }
                val inlineDirectSubclasses = declaration.sealedSubclasses.filter { it.owner.isInline }
                val noinlineSubclasses = collectSubclasses(declaration) { !it.owner.isInline }

                // TODO: Generate during Psi2Ir/Fir2Ir
                // TODO: Merge with rewriteOpenMethodsForSealed
                rewriteFunctionFromAnyForSealed(declaration, inlineSubclasses, noinlineSubclasses, "hashCode")
                rewriteFunctionFromAnyForSealed(declaration, inlineSubclasses, noinlineSubclasses, "toString")
                rewriteOpenMethodsForSealed(declaration, inlineDirectSubclasses, noinlineSubclasses)
            }
            addJvmInlineAnnotation(declaration)
        }

        return declaration
    }

    private fun patchReceiverParameterOfValueGetter(irClass: IrClass) {
        val getter = irClass.functions.single { it.origin == IrDeclarationOrigin.GETTER_OF_SEALED_INLINE_CLASS_FIELD }
        getter.dispatchReceiverParameter = irClass.thisReceiver?.copyTo(getter, type = irClass.defaultType)

        val field = irClass.fields.single { it.origin == IrDeclarationOrigin.FIELD_FOR_SEALED_INLINE_CLASS }

        with(context.createIrBuilder(getter.symbol)) {
            getter.body = irExprBody(
                irGetField(irGet(getter.dispatchReceiverParameter!!), field)
            )
        }
    }

    private fun IrClass.isChildOfSealedInlineClass(): Boolean = superTypes.any { it.isInlineClassType() }

    private fun updateGetterForSealedInlineClassChild(irClass: IrClass) {
        val fieldGetter = irClass.functions.find { it.isInlineClassFieldGetter } ?: error("${irClass.render()} has no getter")

        val parent = irClass.superTypes.single { it.isInlineClassType() }.asClass()

        val methodToOverride = parent.functions.single { it.name == InlineClassAbi.sealedInlineClassFieldName }

        require(
            methodToOverride.origin == IrDeclarationOrigin.FAKE_OVERRIDE ||
                    methodToOverride.origin == IrDeclarationOrigin.GETTER_OF_SEALED_INLINE_CLASS_FIELD
        )

        val fakeOverride = IrFunctionImpl(
            UNDEFINED_OFFSET, UNDEFINED_OFFSET,
            IrDeclarationOrigin.FAKE_OVERRIDE,
            IrSimpleFunctionSymbolImpl(),
            methodToOverride.name,
            methodToOverride.visibility,
            methodToOverride.modality,
            methodToOverride.returnType,
            methodToOverride.isInline,
            methodToOverride.isExternal,
            methodToOverride.isTailrec,
            methodToOverride.isSuspend,
            methodToOverride.isOperator,
            methodToOverride.isInfix,
            methodToOverride.isExpect
        )

        fakeOverride.overriddenSymbols += methodToOverride.symbol
        fakeOverride.dispatchReceiverParameter = irClass.thisReceiver?.copyTo(fakeOverride, type = irClass.defaultType)

        irClass.addMember(fakeOverride)

        with(context.createIrBuilder(fieldGetter.symbol)) {
            fieldGetter.body = irExprBody(
                irCall(fakeOverride.symbol).apply {
                    dispatchReceiver = irGet(fieldGetter.dispatchReceiverParameter!!)
                }
            )
        }
    }

    private fun collectSubclasses(irClass: IrClass, predicate: (IrClassSymbol) -> Boolean): List<SubclassInfo> {
        fun collectBottoms(irClass: IrClass): List<IrClassSymbol> =
            irClass.sealedSubclasses.flatMap {
                when {
                    it.owner.modality == Modality.SEALED -> collectBottoms(it.owner)
                    predicate(it) -> listOf(it)
                    else -> emptyList()
                }
            }

        fun collectSealedBetweenTopAndBottom(bottom: IrClassSymbol): List<IrClassSymbol> {
            fun superClass(symbol: IrClassSymbol): IrClassSymbol =
                symbol.owner.superTypes.single { it.asClass().kind == ClassKind.CLASS }.asClass().symbol

            val result = mutableListOf<IrClassSymbol>()
            var cursor = superClass(bottom)
            while (cursor != irClass.symbol) {
                result += cursor
                cursor = superClass(cursor)
            }
            return result
        }

        return collectBottoms(irClass).map { SubclassInfo(it.owner, collectSealedBetweenTopAndBottom(it)) }
    }

    private fun addJvmInlineAnnotation(declaration: IrClass) {
        if (declaration.hasAnnotation(JVM_INLINE_ANNOTATION_FQ_NAME)) return
        val constructor = context.ir.symbols.jvmInlineAnnotation.constructors.first()
        declaration.annotations = declaration.annotations + IrConstructorCallImpl.fromSymbolOwner(
            constructor.owner.returnType,
            constructor
        )
    }

    private fun transformFunctionFlat(function: IrFunction): List<IrDeclaration>? {
        if (function is IrConstructor && function.isPrimary && function.constructedClass.isInline)
            return null

        val replacement = context.inlineClassReplacements.getReplacementFunction(function)
        if (replacement == null) {
            function.transformChildrenVoid()
            return null
        }

        // If fun interface methods are already mangled, do not mangle them twice.
        if (function is IrSimpleFunction && function.overriddenSymbols.any { it.owner.parentAsClass.isFun } &&
            function.name.asString().substringAfterLast('-') == replacement.name.asString().substringAfterLast('-')
        ) return null

        addBindingsFor(function, replacement)
        return when (function) {
            is IrSimpleFunction -> transformSimpleFunctionFlat(function, replacement)
            is IrConstructor -> transformConstructorFlat(function, replacement)
            else -> throw IllegalStateException()
        }
    }

    private fun transformSimpleFunctionFlat(function: IrSimpleFunction, replacement: IrSimpleFunction): List<IrDeclaration> {
        replacement.valueParameters.forEach { it.transformChildrenVoid() }
        allScopes.push(createScope(function))
        replacement.body = function.body?.transform(this, null)?.patchDeclarationParents(replacement)
        allScopes.pop()
        replacement.copyAttributes(function)

        // Don't create a wrapper for functions which are only used in an unboxed context
        if (function.overriddenSymbols.isEmpty() || replacement.dispatchReceiverParameter != null)
            return listOf(replacement)

        val bridgeFunction = createBridgeDeclaration(
            function,
            when {
                // If the original function has signature which need mangling we still need to replace it with a mangled version.
                (!function.isFakeOverride || function.findInterfaceImplementation(context.state.jvmDefaultMode) != null) &&
                        function.signatureRequiresMangling() ->
                    replacement.name
                // Since we remove the corresponding property symbol from the bridge we need to resolve getter/setter
                // names at this point.
                replacement.isGetter ->
                    Name.identifier(JvmAbi.getterName(replacement.correspondingPropertySymbol!!.owner.name.asString()))
                replacement.isSetter ->
                    Name.identifier(JvmAbi.setterName(replacement.correspondingPropertySymbol!!.owner.name.asString()))
                else ->
                    function.name
            }
        )

        // Update the overridden symbols to point to their inline class replacements
        bridgeFunction.overriddenSymbols = replacement.overriddenSymbols

        val overriddenFromSealedInline = replacement.overriddenSymbols.find { overridden ->
            overridden.owner.parent.let { it is IrClass && it.isInline }
        }

        // Replace the function body with a wrapper
        if (!bridgeFunction.isFakeOverride || !bridgeFunction.parentAsClass.isInline) {
            createBridgeBody(bridgeFunction, replacement)
        } else if (overriddenFromSealedInline != null) {
            // If fake override overrides function from sealed inline class, call the overridden function
            createBridgeBody(replacement, overriddenFromSealedInline.owner)
            // However, if the fake override is overridden, generate when in it, calling its children.
            if (replacement.parentAsClass.modality == Modality.SEALED) {
                @Suppress("NAME_SHADOWING")
                val function = replacement.attributeOwnerId as IrSimpleFunction
                val irClass = function.parentAsClass
                val inlineSubclasses = irClass.sealedSubclasses.filter { it.owner.isInline }
                val noinlineSubclasses = collectSubclasses(irClass) { !it.owner.isInline }
                val overridesInNoinline = mapOverriddenToOverrides(noinlineSubclasses, listOf(function.symbol))
                val overridesInInline =
                    mapOverriddenToOverrides(inlineSubclasses.map { SubclassInfo(it.owner, emptyList()) }, listOf(function.symbol))
                rewriteSingleMethod(
                    replacement,
                    overridesInInline.getOrElse(function.symbol) { emptySet() }
                        .filterNot { it.owner.isFakeOverride && it.owner.parentAsClass.modality != Modality.SEALED },
                    overridesInNoinline.getOrElse(function.symbol) { emptySet() }
                        .filterNot { it.owner.isFakeOverride || it.owner.parentAsClass.isInline }
                )
            }
        } else {
            // Fake overrides redirect from the replacement to the original function, which is in turn replaced during interfacePhase.
            createBridgeBody(replacement, bridgeFunction)
        }

        return listOf(replacement, bridgeFunction)
    }

    private fun IrSimpleFunction.signatureRequiresMangling() =
        fullValueParameterList.any { it.type.requiresMangling } ||
                context.state.functionsWithInlineClassReturnTypesMangled && returnType.requiresMangling

    // We may need to add a bridge method for inline class methods with static replacements. Ideally, we'd do this in BridgeLowering,
    // but unfortunately this is a special case in the old backend. The bridge method is not marked as such and does not follow the normal
    // visibility rules for bridge methods.
    private fun createBridgeDeclaration(source: IrSimpleFunction, mangledName: Name) =
        context.irFactory.buildFun {
            updateFrom(source)
            name = mangledName
            returnType = source.returnType
        }.apply {
            copyParameterDeclarationsFrom(source)
            annotations = source.annotations
            parent = source.parent
            // We need to ensure that this bridge has the same attribute owner as its static inline class replacement, since this
            // is used in [CoroutineCodegen.isStaticInlineClassReplacementDelegatingCall] to identify the bridge and avoid generating
            // a continuation class.
            copyAttributes(source)
        }

    private fun createBridgeBody(source: IrSimpleFunction, target: IrSimpleFunction) {
        source.body = context.createIrBuilder(source.symbol, source.startOffset, source.endOffset).run {
            irExprBody(irCall(target).apply {
                passTypeArgumentsFrom(source)
                for ((parameter, newParameter) in source.explicitParameters.zip(target.explicitParameters)) {
                    putArgument(newParameter, irGet(parameter))
                }
            })
        }
    }

    // Secondary constructors for boxed types get translated to static functions returning
    // unboxed arguments. We remove the original constructor.
    private fun transformConstructorFlat(constructor: IrConstructor, replacement: IrSimpleFunction): List<IrDeclaration> {
        replacement.valueParameters.forEach { it.transformChildrenVoid() }
        replacement.body = context.createIrBuilder(replacement.symbol, replacement.startOffset, replacement.endOffset).irBlockBody(
            replacement
        ) {
            val thisVar = irTemporary(irType = replacement.returnType, nameHint = "\$this")
            valueMap[constructor.constructedClass.thisReceiver!!.symbol] = thisVar

            constructor.body?.statements?.forEach { statement ->
                +statement
                    .transformStatement(object : IrElementTransformerVoid() {
                        // Don't recurse under nested class declarations
                        override fun visitClass(declaration: IrClass): IrStatement {
                            return declaration
                        }

                        // Capture the result of a delegating constructor call in a temporary variable "thisVar".
                        //
                        // Within the constructor we replace references to "this" with references to "thisVar".
                        // This is safe, since the delegating constructor call precedes all references to "this".
                        override fun visitDelegatingConstructorCall(expression: IrDelegatingConstructorCall): IrExpression {
                            expression.transformChildrenVoid()
                            return irSet(thisVar.symbol, expression)
                        }

                        // A constructor body has type unit and may contain explicit return statements.
                        // These early returns may have side-effects however, so we still have to evaluate
                        // the return expression. Afterwards we return "thisVar".
                        // For example, the following is a valid inline class declaration.
                        //
                        //     inline class Foo(val x: String) {
                        //       constructor(y: Int) : this(y.toString()) {
                        //         if (y == 0) return throw java.lang.IllegalArgumentException()
                        //         if (y == 1) return
                        //         return Unit
                        //       }
                        //     }
                        override fun visitReturn(expression: IrReturn): IrExpression {
                            expression.transformChildrenVoid()
                            if (expression.returnTargetSymbol != constructor.symbol)
                                return expression

                            return irReturn(irBlock(expression.startOffset, expression.endOffset) {
                                +expression.value
                                +irGet(thisVar)
                            })
                        }
                    })
                    .transformStatement(this@JvmInlineClassLowering)
                    .patchDeclarationParents(replacement)
            }

            +irReturn(irGet(thisVar))
        }

        return listOf(replacement)
    }

    private fun typedArgumentList(function: IrFunction, expression: IrMemberAccessExpression<*>) =
        listOfNotNull(
            function.dispatchReceiverParameter?.let { it to expression.dispatchReceiver },
            function.extensionReceiverParameter?.let { it to expression.extensionReceiver }
        ) + function.valueParameters.map { it to expression.getValueArgument(it.index) }

    private fun IrMemberAccessExpression<*>.buildReplacement(
        originalFunction: IrFunction,
        original: IrMemberAccessExpression<*>,
        replacement: IrSimpleFunction
    ) {
        copyTypeArgumentsFrom(original)
        val valueParameterMap = originalFunction.explicitParameters.zip(replacement.explicitParameters).toMap()
        for ((parameter, argument) in typedArgumentList(originalFunction, original)) {
            if (argument == null) continue
            val newParameter = valueParameterMap.getValue(parameter)
            putArgument(replacement, newParameter, argument.transform(this@JvmInlineClassLowering, null))
        }
    }

    override fun visitFunctionReference(expression: IrFunctionReference): IrExpression {
        if (expression.origin == InlineClassAbi.UNMANGLED_FUNCTION_REFERENCE)
            return super.visitFunctionReference(expression)

        val function = expression.symbol.owner
        val replacement = context.inlineClassReplacements.getReplacementFunction(function)
            ?: return super.visitFunctionReference(expression)

        return IrFunctionReferenceImpl(
            expression.startOffset, expression.endOffset, expression.type,
            replacement.symbol, function.typeParameters.size,
            replacement.valueParameters.size, expression.reflectionTarget, expression.origin
        ).apply {
            buildReplacement(function, expression, replacement)
        }.copyAttributes(expression)
    }

    override fun visitFunctionAccess(expression: IrFunctionAccessExpression): IrExpression {
        val function = expression.symbol.owner

        val delegatingCallOrSealedInlineClassConstructorInsideNoinlineClass =
            expression is IrDelegatingConstructorCall && function.parentAsClass.isInline &&
                    (currentClass?.irElement as? IrClass)?.isInline == false

        val replacement = context.inlineClassReplacements.getReplacementFunction(function)
            .takeIf { !delegatingCallOrSealedInlineClassConstructorInsideNoinlineClass }
            ?: return super.visitFunctionAccess(expression)

        return IrCallImpl(
            expression.startOffset, expression.endOffset, function.returnType.substitute(expression.typeSubstitutionMap),
            replacement.symbol, replacement.typeParameters.size, replacement.valueParameters.size,
            expression.origin, (expression as? IrCall)?.superQualifierSymbol
        ).apply {
            buildReplacement(function, expression, replacement)
        }
    }

    private fun coerceInlineClasses(argument: IrExpression, from: IrType, to: IrType, skipCast: Boolean = false): IrExpression {
        return IrCallImpl.fromSymbolOwner(UNDEFINED_OFFSET, UNDEFINED_OFFSET, to, context.ir.symbols.unsafeCoerceIntrinsic).apply {
            val underlyingType = from.erasedUpperBound.inlineClassRepresentation?.underlyingType
            if (underlyingType?.isTypeParameter() == true && !skipCast) {
                putTypeArgument(0, from)
                putTypeArgument(1, underlyingType)
                putValueArgument(
                    0, IrTypeOperatorCallImpl(
                        UNDEFINED_OFFSET, UNDEFINED_OFFSET, to, IrTypeOperator.IMPLICIT_CAST, underlyingType, argument
                    )
                )
            } else {
                putTypeArgument(0, from)
                putTypeArgument(1, to)
                putValueArgument(0, argument)
            }
        }
    }

    private fun IrExpression.coerceToUnboxed() =
        coerceInlineClasses(this, this.type, this.type.unboxInlineClass())

    // Precondition: left has an inline class type, but may not be unboxed
    private fun IrBuilderWithScope.specializeEqualsCall(left: IrExpression, right: IrExpression): IrExpression? {
        // There's already special handling for null-comparisons in the Equals intrinsic.
        if (left.isNullConst() || right.isNullConst())
            return null

        // We don't specialize calls when both arguments are boxed.
        val leftIsUnboxed = left.type.unboxInlineClass() != left.type
        val rightIsUnboxed = right.type.unboxInlineClass() != right.type
        if (!leftIsUnboxed && !rightIsUnboxed)
            return null

        // Precondition: left is an unboxed inline class type
        fun equals(left: IrExpression, right: IrExpression): IrExpression {
            // Unsigned types use primitive comparisons
            if (left.type.isUnsigned() && right.type.isUnsigned() && rightIsUnboxed)
                return irEquals(left.coerceToUnboxed(), right.coerceToUnboxed())

            val klass = left.type.classOrNull!!.owner
            val equalsMethod = if (rightIsUnboxed) {
                this@JvmInlineClassLowering.context.inlineClassReplacements.getSpecializedEqualsMethod(klass, context.irBuiltIns)
            } else {
                val equals = klass.functions.single { it.name.asString() == "equals" && it.overriddenSymbols.isNotEmpty() }
                this@JvmInlineClassLowering.context.inlineClassReplacements.getReplacementFunction(equals)!!
            }

            return irCall(equalsMethod).apply {
                putValueArgument(0, left)
                putValueArgument(1, right)
            }
        }

        val leftNullCheck = left.type.isNullable()
        val rightNullCheck = rightIsUnboxed && right.type.isNullable() // equals-impl has a nullable second argument
        return if (leftNullCheck || rightNullCheck) {
            irBlock {
                val leftVal = if (left is IrGetValue) left.symbol.owner else irTemporary(left)
                val rightVal = if (right is IrGetValue) right.symbol.owner else irTemporary(right)

                val equalsCall = equals(
                    if (leftNullCheck) irImplicitCast(irGet(leftVal), left.type.makeNotNull()) else irGet(leftVal),
                    if (rightNullCheck) irImplicitCast(irGet(rightVal), right.type.makeNotNull()) else irGet(rightVal)
                )

                val equalsRight = if (rightNullCheck) {
                    irIfNull(context.irBuiltIns.booleanType, irGet(rightVal), irFalse(), equalsCall)
                } else {
                    equalsCall
                }

                if (leftNullCheck) {
                    +irIfNull(context.irBuiltIns.booleanType, irGet(leftVal), irEqualsNull(irGet(rightVal)), equalsRight)
                } else {
                    +equalsRight
                }
            }
        } else {
            equals(left, right)
        }
    }

    override fun visitCall(expression: IrCall): IrExpression =
        when {
            // Getting the underlying field of an inline class merely changes the IR type,
            // since the underlying representations are the same.
            expression.symbol.owner.isInlineClassFieldGetter -> {
                val arg = expression.dispatchReceiver!!.transform(this, null)
                coerceInlineClasses(arg, expression.symbol.owner.dispatchReceiverParameter!!.type, expression.type)
            }
            // Specialize calls to equals when the left argument is a value of inline class type.
            expression.isSpecializedInlineClassEqEq -> {
                expression.transformChildrenVoid()
                context.createIrBuilder(currentScope!!.scope.scopeOwnerSymbol, expression.startOffset, expression.endOffset)
                    .specializeEqualsCall(expression.getValueArgument(0)!!, expression.getValueArgument(1)!!)
                    ?: expression
            }
            else ->
                super.visitCall(expression)
        }

    private val IrCall.isSpecializedInlineClassEqEq: Boolean
        get() {
            // Note that reference equality (x === y) is not allowed on values of inline class type,
            // so it is enough to check for eqeq.
            if (symbol != context.irBuiltIns.eqeqSymbol)
                return false

            val leftClass = getValueArgument(0)?.type?.classOrNull?.owner?.takeIf { it.isInline }
                ?: return false

            // Before version 1.4, we cannot rely on the Result.equals-impl0 method
            return (leftClass.fqNameWhenAvailable != StandardNames.RESULT_FQ_NAME) ||
                    context.state.languageVersionSettings.apiVersion >= ApiVersion.KOTLIN_1_4
        }

    override fun visitGetField(expression: IrGetField): IrExpression {
        val field = expression.symbol.owner
        val parent = field.parent
        if (field.origin == IrDeclarationOrigin.PROPERTY_BACKING_FIELD &&
            parent is IrClass &&
            parent.isInline &&
            field.name == parent.inlineClassFieldName
        ) {
            val receiver = expression.receiver!!.transform(this, null)
            return coerceInlineClasses(receiver, receiver.type, field.type)
        }
        return super.visitGetField(expression)
    }

    override fun visitReturn(expression: IrReturn): IrExpression {
        expression.returnTargetSymbol.owner.safeAs<IrFunction>()?.let { target ->
            context.inlineClassReplacements.getReplacementFunction(target)?.let {
                return context.createIrBuilder(it.symbol, expression.startOffset, expression.endOffset).irReturn(
                    expression.value.transform(this, null)
                )
            }
        }
        return super.visitReturn(expression)
    }

    private fun visitStatementContainer(container: IrStatementContainer) {
        container.statements.transformFlat { statement ->
            if (statement is IrFunction)
                transformFunctionFlat(statement)
            else
                listOf(statement.transformStatement(this))
        }
    }

    override fun visitContainerExpression(expression: IrContainerExpression): IrExpression {
        visitStatementContainer(expression)
        return expression
    }

    override fun visitBlockBody(body: IrBlockBody): IrBody {
        visitStatementContainer(body)
        return body
    }

    override fun visitGetValue(expression: IrGetValue): IrExpression {
        valueMap[expression.symbol]?.let {
            return IrGetValueImpl(
                expression.startOffset, expression.endOffset,
                it.type, it.symbol, expression.origin
            )
        }
        return super.visitGetValue(expression)
    }

    override fun visitSetValue(expression: IrSetValue): IrExpression {
        valueMap[expression.symbol]?.let {
            return IrSetValueImpl(
                expression.startOffset, expression.endOffset,
                it.type, it.symbol,
                expression.value.transform(this@JvmInlineClassLowering, null),
                expression.origin
            )
        }
        return super.visitSetValue(expression)
    }

    // Anonymous initializers in inline classes are processed when building the primary constructor.
    override fun visitAnonymousInitializerNew(declaration: IrAnonymousInitializer): IrStatement {
        if (declaration.parent.safeAs<IrClass>()?.isInline == true)
            return declaration
        return super.visitAnonymousInitializerNew(declaration)
    }

    private fun buildPrimaryInlineClassConstructor(irClass: IrClass, irConstructor: IrConstructor) {
        // Add the default primary constructor
        irClass.addConstructor {
            updateFrom(irConstructor)
            visibility = DescriptorVisibilities.PRIVATE
            origin = JvmLoweredDeclarationOrigin.SYNTHETIC_INLINE_CLASS_MEMBER
            returnType = irConstructor.returnType
        }.apply {
            if (irClass.modality == Modality.SEALED) {
                valueParameters = listOf(
                    context.irFactory.createValueParameter(
                        UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                        IrDeclarationOrigin.PRIMARY_CONSTRUCTOR_PARAMETER_FOR_SEALED_INLINE_CLASS,
                        IrValueParameterSymbolImpl(),
                        InlineClassAbi.sealedInlineClassFieldName,
                        index = 0,
                        type = context.irBuiltIns.anyNType,
                        varargElementType = null,
                        isCrossinline = false,
                        isNoinline = false,
                        isHidden = false,
                        isAssignable = false
                    )
                )
            }
            // Don't create a default argument stub for the primary constructor
            irConstructor.valueParameters.forEach { it.defaultValue = null }
            copyParameterDeclarationsFrom(irConstructor)
            annotations = irConstructor.annotations
            body = context.createIrBuilder(this.symbol).irBlockBody(this) {
                val superClass = irClass.superTypes.singleOrNull { it.asClass().kind == ClassKind.CLASS }?.asClass()
                    ?: context.irBuiltIns.anyClass.owner
                +irDelegatingConstructorCall(superClass.primaryConstructor!!)
                +irSetField(
                    irGet(irClass.thisReceiver!!),
                    getInlineClassBackingField(irClass),
                    irGet(this@apply.valueParameters[0])
                )
            }
        }

        // Add a static bridge method to the primary constructor. This contains
        // null-checks, default arguments, and anonymous initializers.
        val function = context.inlineClassReplacements.getReplacementFunction(irConstructor)!!

        val initBlocks = irClass.declarations.filterIsInstance<IrAnonymousInitializer>()

        function.valueParameters.forEach { it.transformChildrenVoid() }
        function.body = context.createIrBuilder(function.symbol).irBlockBody {
            val argument = function.valueParameters[0]
            val thisValue = irTemporary(coerceInlineClasses(irGet(argument), argument.type, function.returnType, skipCast = true))
            valueMap[irClass.thisReceiver!!.symbol] = thisValue
            for (initBlock in initBlocks) {
                for (stmt in initBlock.body.statements) {
                    +stmt.transformStatement(this@JvmInlineClassLowering).patchDeclarationParents(function)
                }
            }
            +irReturn(irGet(thisValue))
        }

        irClass.declarations.removeAll(initBlocks)
        irClass.declarations += function
    }

    private fun IrType.asClass(): IrClass = classifierOrFail.owner.cast()

    private fun buildBoxFunction(irClass: IrClass) {
        val function = context.inlineClassReplacements.getBoxFunction(irClass)

        val primaryConstructor =
            if (irClass.modality == Modality.SEALED) irClass.declarations.single {
                it is IrConstructor && it.origin == JvmLoweredDeclarationOrigin.SYNTHETIC_INLINE_CLASS_MEMBER
            } as IrConstructor
            else irClass.primaryConstructor!!

        with(context.createIrBuilder(function.symbol)) {
            function.body = irExprBody(
                irCall(primaryConstructor.symbol).apply {
                    passTypeArgumentsFrom(function)
                    putValueArgument(0, irGet(function.valueParameters[0]))
                }
            )
        }
        irClass.declarations += function
    }

    private fun buildBoxFunctionForSealed(
        irClass: IrClass,
        inlineSubclasses: List<IrClassSymbol>,
        noinlineSubclasses: List<IrClassSymbol>
    ) {
        val function = context.inlineClassReplacements.getBoxFunction(irClass)
        with(context.createIrBuilder(function.symbol)) {
            val branches = noinlineSubclasses.map {
                irBranch(
                    irIs(irGet(function.valueParameters[0]), it.owner.defaultType),
                    irReturn(irGet(function.valueParameters[0]))
                )
            } + inlineSubclasses.map {
                val type = getInlineClassUnderlyingType(it.owner)
                irBranch(
                    irIs(irGet(function.valueParameters[0]), type),
                    irReturn(irCall(this@JvmInlineClassLowering.context.inlineClassReplacements.getBoxFunction(it.owner)).apply {
                        putValueArgument(0, irImplicitCast(irGet(function.valueParameters[0]), type))
                    })
                )
            } + irBranch(
                irTrue(), irReturn(
                    irImplicitCast(irGet(function.valueParameters[0]), irClass.defaultType)
                )
            )
            function.body = irBlockBody {
                +irReturn(irWhen(irClass.defaultType, branches))
            }
        }
        irClass.declarations += function
    }

    private fun rewriteFunctionFromAnyForSealed(
        irClass: IrClass,
        inlineSubclasses: List<SubclassInfo>,
        noinlineSubclasses: List<SubclassInfo>,
        name: String
    ) {
        val replacements = context.inlineClassReplacements
        val function = irClass.declarations.single { it is IrSimpleFunction && it.name.asString() == "$name-impl" } as IrFunction

        val oldBody = function.body

        fun IrClass.isFunctionDeclared(): Boolean =
            declarations.any { it is IrSimpleFunction && it.name.asString() == name && !it.isFakeOverride }

        with(context.createIrBuilder(function.symbol)) {
            function.body = irBlockBody {
                val tmp = irTemporary(
                    coerceInlineClasses(
                        irGet(function.valueParameters[0]),
                        function.valueParameters[0].type,
                        context.irBuiltIns.anyType
                    )
                )

                val funFromObject = context.irBuiltIns.anyClass.owner.declarations
                    .single { it is IrSimpleFunction && it.name.asString() == name } as IrFunction
                val branches = noinlineSubclasses
                    .filter { info ->
                        info.bottom.isFunctionDeclared() || info.sealedParents.any { it.owner.isFunctionDeclared() }
                    }.map {
                        irBranch(
                            irIs(irGet(tmp), it.bottom.defaultType),
                            irReturn(irCall(funFromObject.symbol).apply {
                                dispatchReceiver = irGet(tmp)
                            })
                        )
                    } + inlineSubclasses.map {
                    val delegate = replacements.getReplacementFunction(
                        it.bottom.declarations
                            .single { f -> f is IrSimpleFunction && f.name.asString() == name } as IrFunction
                    )!!
                    val underlyingType = getInlineClassUnderlyingType(it.bottom)

                    irBranch(
                        irIs(irGet(tmp), underlyingType),
                        irReturn(irCall(delegate.symbol).apply {
                            putValueArgument(
                                0, coerceInlineClasses(
                                    irImplicitCast(irGet(tmp), underlyingType),
                                    underlyingType,
                                    it.bottom.defaultType
                                )
                            )
                        })
                    )
                } + irBranch(irTrue(), when (oldBody) {
                    is IrExpressionBody -> oldBody.expression
                    is IrBlockBody -> irComposite {
                        for (stmt in oldBody.statements) {
                            +stmt
                        }
                    }
                    else -> if (name == "hashCode") irInt(0) else irNull()
                })
                +irReturn(irWhen(function.returnType, branches))
            }
        }
    }

    private fun rewriteOpenMethodsForSealed(
        irClass: IrClass,
        inlineSubclasses: List<IrClassSymbol>,
        noinlineSubclasses: List<SubclassInfo>
    ) {
        val openMethods = irClass.functions.filter {
            it.origin == JvmLoweredDeclarationOrigin.STATIC_INLINE_CLASS_REPLACEMENT && it.modality == Modality.OPEN
        }.map { (it.attributeOwnerId as IrSimpleFunction).symbol }.toList()

        if (openMethods.isEmpty()) return

        val overridesInNoinline = mapOverriddenToOverrides(noinlineSubclasses, openMethods)
        val overridesInInline = mapOverriddenToOverrides(inlineSubclasses.map { SubclassInfo(it.owner, emptyList()) }, openMethods)

        for (function in openMethods) {
            // TODO: Remove
            if (function.owner.name.asString().let { it == "toString" || it == "hashCode" }) continue
            if (function !in overridesInInline && function !in overridesInNoinline) continue
            if (function.owner.isFakeOverride) continue

            rewriteSingleMethod(
                context.inlineClassReplacements.getReplacementFunction(function.owner)!!,
                overridesInInline.getOrElse(function) { emptySet() }
                    .filterNot { it.owner.isFakeOverride && it.owner.parentAsClass.modality != Modality.SEALED },
                overridesInNoinline.getOrElse(function) { emptySet() }
                    .filterNot { it.owner.isFakeOverride || it.owner.parentAsClass.isInline }
            )
        }
    }

    private fun rewriteSingleMethod(
        function: IrSimpleFunction,
        overridesInInline: Collection<IrSimpleFunctionSymbol>,
        overridesInNoinline: Collection<IrSimpleFunctionSymbol>
    ) {
        val oldBody = function.body
        val replacements = context.inlineClassReplacements

        with(context.createIrBuilder(function.symbol)) {
            function.body = irBlockBody {
                val receiver = irTemporary(
                    coerceInlineClasses(
                        irGet(function.valueParameters[0]),
                        function.valueParameters[0].type,
                        context.irBuiltIns.anyType
                    )
                )

                val branches = overridesInNoinline.map {
                    irBranch(
                        irIs(irGet(receiver), it.owner.parentAsClass.defaultType),
                        irReturn(irCall(it).apply {
                            dispatchReceiver = irImplicitCast(irGet(receiver), it.owner.parentAsClass.defaultType)
                            for ((target, source) in it.owner.explicitParameters.zip(function.explicitParameters).drop(1)) {
                                putArgument(target, irGet(source))
                            }
                        })
                    )
                } + overridesInInline.map {
                    val underlyingType = it.owner.parentAsClass.inlineClassRepresentation!!.underlyingType
                    val toCall = replacements.getReplacementFunction(it.owner)!!
                    irBranch(
                        irIs(irGet(receiver), underlyingType),
                        irReturn(irCall(toCall.symbol).apply {
                            putValueArgument(
                                0, coerceInlineClasses(
                                    irImplicitCast(irGet(receiver), underlyingType),
                                    underlyingType,
                                    it.owner.parentAsClass.defaultType
                                )
                            )
                            for ((target, source) in toCall.explicitParameters.zip(function.explicitParameters).drop(1)) {
                                putArgument(target, irGet(source))
                            }
                        })
                    )
                } + irBranch(
                    irTrue(),
                    when {
                        function.attributeOwnerId.let { it is IrSimpleFunction && it.isFakeOverride } -> {
                            val overridden = function.overriddenSymbols.find { !it.owner.isFakeOverride }!!
                            irCall(overridden).apply {
                                if (overridden.owner.parentAsClass.isInline) {
                                    putValueArgument(0, irGet(receiver))
                                } else {
                                    dispatchReceiver = irImplicitCast(irGet(receiver), overridden.owner.parentAsClass.defaultType)
                                }
                                for ((target, source) in overridden.owner.explicitParameters.zip(function.explicitParameters).drop(1)) {
                                    putArgument(target, irGet(source))
                                }
                            }
                        }
                        oldBody is IrExpressionBody -> oldBody.expression
                        oldBody is IrBlockBody -> irComposite {
                            for (stmt in oldBody.statements) {
                                +stmt
                            }
                        }
                        oldBody == null -> irCall(this@JvmInlineClassLowering.context.ir.symbols.throwKotlinNothingValueException)
                        else -> error("Expected either expression or block body")
                    }
                )

                +irReturn(irWhen(function.returnType, branches))
            }
        }
    }

    private fun IrSimpleFunctionSymbol.overrides(openMethods: List<IrSimpleFunctionSymbol>): IrSimpleFunctionSymbol? {
        for (symbol in owner.overriddenSymbols) {
            if (symbol.owner.isFakeOverride) {
                val overridden = symbol.overrides(openMethods)
                if (overridden != null) return overridden
            }
            if (symbol in openMethods) return symbol
        }
        return null
    }

    private fun MutableMap<IrSimpleFunctionSymbol, MutableSet<IrSimpleFunctionSymbol>>.putOverriddenToOverridesFromSingleClass(
        irClass: IrClass,
        openMethods: List<IrSimpleFunctionSymbol>
    ) {
        val overridesToOverridden =
            irClass.functions.mapNotNull { child -> child.symbol.overrides(openMethods)?.let { child.symbol to it } }.toList()

        for ((override, overridden) in overridesToOverridden) {
            getOrPut(overridden) { mutableSetOf() } += override
        }
    }

    // Go from bottom to top and collect classes, in which a function is overridden, preserving order,
    // since we need to check for INSTANCEOF, return map from overridden function to all overrides
    private fun mapOverriddenToOverrides(
        subclasses: List<SubclassInfo>,
        openMethods: List<IrSimpleFunctionSymbol>
    ): Map<IrSimpleFunctionSymbol, Set<IrSimpleFunctionSymbol>> {
        val result = mutableMapOf<IrSimpleFunctionSymbol, MutableSet<IrSimpleFunctionSymbol>>()
        for (info in subclasses) {
            result.putOverriddenToOverridesFromSingleClass(info.bottom, openMethods)
            for (sealedParent in info.sealedParents) {
                result.putOverriddenToOverridesFromSingleClass(sealedParent.owner, openMethods)
            }
        }
        return result
    }

    private fun buildUnboxFunction(irClass: IrClass) {
        val function = context.inlineClassReplacements.getUnboxFunction(irClass)
        val field = getInlineClassBackingField(irClass)

        function.body = context.createIrBuilder(function.symbol).irBlockBody {
            val thisVal = irGet(function.dispatchReceiverParameter!!)
            +irReturn(irGetField(thisVal, field))
        }

        irClass.declarations += function
    }

    // unbox-impl is virtual, so, if the child is inline, its unbox-impl is called
    // otherwise, just return this.
    private fun buildUnboxFunctionForSealed(irClass: IrClass) {
        val function = context.inlineClassReplacements.getUnboxFunction(irClass)

        with(context.createIrBuilder(function.symbol)) {
            function.body = irExprBody(irGet(function.dispatchReceiverParameter!!))
        }

        irClass.declarations += function
    }

    private fun buildSpecializedEqualsMethod(irClass: IrClass) {
        val function = context.inlineClassReplacements.getSpecializedEqualsMethod(irClass, context.irBuiltIns)
        val left = function.valueParameters[0]
        val right = function.valueParameters[1]
        val type = left.type.unboxInlineClass()

        function.body = context.createIrBuilder(irClass.symbol).run {
            irExprBody(
                irEquals(
                    coerceInlineClasses(irGet(left), left.type, type),
                    coerceInlineClasses(irGet(right), right.type, type)
                )
            )
        }

        irClass.declarations += function
    }

    private fun buildSpecializedEqualsMethodForSealed(
        irClass: IrClass,
        inlineSubclasses: List<IrClassSymbol>,
        noinlineSubclasses: List<IrClassSymbol>
    ) {
        val boolAnd = context.ir.symbols.getBinaryOperator(
            OperatorNameConventions.AND, context.irBuiltIns.booleanType, context.irBuiltIns.booleanType
        )
        val equals = context.ir.symbols.getBinaryOperator(
            OperatorNameConventions.EQUALS, context.irBuiltIns.anyNType, context.irBuiltIns.anyNType
        )

        val function = context.inlineClassReplacements.getSpecializedEqualsMethod(irClass, context.irBuiltIns)

        with(context.createIrBuilder(function.symbol)) {
            function.body = irBlockBody {
                val left = irTemporary(
                    coerceInlineClasses(
                        irGet(function.valueParameters[0]),
                        function.valueParameters[0].type,
                        context.irBuiltIns.anyType
                    )
                )
                val right = irTemporary(
                    coerceInlineClasses(
                        irGet(function.valueParameters[1]),
                        function.valueParameters[1].type,
                        context.irBuiltIns.anyType
                    )
                )
                val branches = noinlineSubclasses.map {
                    irBranch(
                        irCallOp(
                            boolAnd, context.irBuiltIns.booleanType,
                            irIs(irGet(left), it.owner.defaultType),
                            irIs(irGet(right), it.owner.defaultType),
                        ),
                        irReturn(irCallOp(equals, context.irBuiltIns.booleanType, irGet(left), irGet(right)))
                    )
                } + inlineSubclasses.map {
                    val eq = this@JvmInlineClassLowering.context.inlineClassReplacements
                        .getSpecializedEqualsMethod(it.owner, context.irBuiltIns)
                    val underlyingType = getInlineClassUnderlyingType(it.owner)
                    irBranch(
                        irCallOp(
                            boolAnd, context.irBuiltIns.booleanType,
                            irIs(irGet(left), underlyingType),
                            irIs(irGet(right), underlyingType),
                        ),
                        irReturn(
                            irCall(eq).apply {
                                putValueArgument(
                                    0, coerceInlineClasses(
                                        irImplicitCast(irGet(left), underlyingType),
                                        underlyingType,
                                        it.owner.defaultType
                                    )
                                )
                                putValueArgument(
                                    1, coerceInlineClasses(
                                        irImplicitCast(irGet(right), underlyingType),
                                        underlyingType,
                                        it.owner.defaultType
                                    )
                                )
                            }
                        )
                    )
                } + irBranch(irTrue(), irReturn(irFalse()))
                +irReturn(irWhen(context.irBuiltIns.booleanType, branches))
            }
        }

        irClass.declarations += function
    }
}

private class SubclassInfo(
    // The bottom class
    val bottom: IrClass,
    // Sealed subclasses from top to the bottom
    val sealedParents: List<IrClassSymbol>
)