/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.ir.*
import org.jetbrains.kotlin.backend.common.lower.SamEqualsHashCodeMethodsGenerator
import org.jetbrains.kotlin.backend.common.lower.parents
import org.jetbrains.kotlin.backend.common.phaser.makeIrFilePhase
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.JvmLoweredDeclarationOrigin
import org.jetbrains.kotlin.backend.jvm.ir.*
import org.jetbrains.kotlin.backend.jvm.lower.inlineclasses.InlineClassAbi
import org.jetbrains.kotlin.builtins.functions.BuiltInFunctionArity
import org.jetbrains.kotlin.config.JvmClosureGenerationScheme
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.overrides.buildFakeOverrideMember
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrSimpleFunctionSymbolImpl
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames

internal val functionReferencePhase = makeIrFilePhase(
    ::FunctionReferenceLowering,
    name = "FunctionReference",
    description = "Construct instances of anonymous KFunction subclasses for function references"
)

internal class FunctionReferenceLowering(private val context: JvmBackendContext) : FileLoweringPass, IrElementTransformerVoidWithContext() {
    // This pass ignores function references used as inline arguments. `InlineCallableReferenceToLambdaPhase`
    // converts them into lambdas instead, so that after inlining there is only a direct call left, with no
    // function reference classes needed.
    private val ignoredFunctionReferences = mutableSetOf<IrCallableReference<*>>()

    private val inlineLambdaToValueParameter = HashMap<IrFunction, IrValueParameter>()

    private val IrFunctionReference.isIgnored: Boolean
        get() = (!type.isFunctionOrKFunction() && !isSuspendFunctionReference()) || ignoredFunctionReferences.contains(this)

    // `suspend` function references are the same as non-`suspend` ones, just with a `suspend` invoke;
    // however, suspending lambdas require different generation implemented in AddContinuationLowering
    // because they are also their own continuation classes.
    // TODO: Currently, origin of callable references explicitly written in source code is null. Do we need to create one?
    private fun IrFunctionReference.isSuspendFunctionReference(): Boolean = isSuspend &&
            (origin == null || origin == IrStatementOrigin.ADAPTED_FUNCTION_REFERENCE || origin == IrStatementOrigin.SUSPEND_CONVERSION)

    override fun lower(irFile: IrFile) {
        irFile.accept(
            object : IrInlineReferenceLocator(context) {
                override fun visitInlineLambda(
                    argument: IrFunctionReference,
                    callee: IrFunction,
                    parameter: IrValueParameter,
                    scope: IrDeclaration
                ) {
                    ignoredFunctionReferences.add(argument)
                    inlineLambdaToValueParameter[argument.symbol.owner] = parameter
                }
            },
            null
        )
        irFile.transformChildrenVoid(this)
    }

    private val shouldGenerateIndySamConversions =
        context.state.samConversionsScheme == JvmClosureGenerationScheme.INDY

    private val shouldGenerateIndyLambdas =
        context.state.lambdasScheme == JvmClosureGenerationScheme.INDY

    override fun visitBlock(expression: IrBlock): IrExpression {
        if (!expression.origin.isLambda)
            return super.visitBlock(expression)

        val reference = expression.statements.last() as IrFunctionReference
        if (reference.isIgnored)
            return super.visitBlock(expression)

        expression.statements.dropLast(1).forEach { it.transform(this, null) }
        reference.transformChildrenVoid(this)

        if (shouldGenerateIndyLambdas) {
            val lambdaMetafactoryArguments = getLambdaMetafactoryArgumentsOrNull(reference, reference.type, true)
            if (lambdaMetafactoryArguments != null) {
                return wrapLambdaReferenceWithIndySamConversion(expression, reference, lambdaMetafactoryArguments)
            }
        }

        return FunctionReferenceBuilder(reference).build()
    }

    private fun wrapLambdaReferenceWithIndySamConversion(
        expression: IrBlock,
        reference: IrFunctionReference,
        lambdaMetafactoryArguments: LambdaMetafactoryArguments
    ): IrBlock {
        val indySamConversion = wrapWithIndySamConversion(reference.type, lambdaMetafactoryArguments)
        expression.statements[expression.statements.size - 1] = indySamConversion
        expression.type = indySamConversion.type
        return expression
    }

    override fun visitFunctionReference(expression: IrFunctionReference): IrExpression {
        expression.transformChildrenVoid(this)
        return if (expression.isIgnored)
            expression
        else
            FunctionReferenceBuilder(expression).build()
    }

    // Handle SAM conversions which wrap a function reference:
    //     class sam$n(private val receiver: R) : Interface { override fun method(...) = receiver.target(...) }
    //
    // This avoids materializing an invokable KFunction representing, thus producing one less class.
    // This is actually very common, as `Interface { something }` is a local function + a SAM-conversion
    // of a reference to it into an implementation.
    override fun visitTypeOperator(expression: IrTypeOperatorCall): IrExpression {
        if (expression.operator != IrTypeOperator.SAM_CONVERSION) {
            return super.visitTypeOperator(expression)
        }

        val invokable = expression.argument
        val reference = if (invokable is IrFunctionReference) {
            invokable
        } else if (invokable is IrBlock && invokable.origin.isLambda && invokable.statements.last() is IrFunctionReference) {
            invokable.statements.dropLast(1).forEach { it.transform(this, null) }
            invokable.statements.last() as IrFunctionReference
        } else {
            return super.visitTypeOperator(expression)
        }
        reference.transformChildrenVoid()

        val samSuperType = expression.typeOperand

        if (shouldGenerateIndySamConversions) {
            val lambdaMetafactoryArguments = getLambdaMetafactoryArgumentsOrNull(reference, samSuperType, false)
            if (lambdaMetafactoryArguments != null) {
                return wrapSamConversionArgumentWithIndySamConversion(expression, lambdaMetafactoryArguments)
            }
        }

        return FunctionReferenceBuilder(reference, samSuperType).build()
    }

    private class LambdaMetafactoryArguments(
        val samMethod: IrSimpleFunction,
        val fakeInstanceMethod: IrSimpleFunction,
        val implMethodReference: IrFunctionReference,
        val extraOverriddenMethods: List<IrSimpleFunction>
    )

    /**
     * @see java.lang.invoke.LambdaMetafactory
     */
    private fun getLambdaMetafactoryArgumentsOrNull(
        reference: IrFunctionReference,
        samType: IrType,
        plainLambda: Boolean
    ): LambdaMetafactoryArguments? {
        // Can't use JDK LambdaMetafactory for function references by default (because of 'equals').
        // TODO special mode that would generate indy everywhere?
        if (reference.origin != IrStatementOrigin.LAMBDA)
            return null

        val samClass = samType.getClass()
            ?: throw AssertionError("SAM type is not a class: ${samType.render()}")
        val samMethod = samClass.getSingleAbstractMethod()
            ?: throw AssertionError("SAM class has no single abstract method: ${samClass.render()}")

        // Can't use JDK LambdaMetafactory for fun interface with suspend fun.
        if (samMethod.isSuspend)
            return null

        // Can't use JDK LambdaMetafactory for fun interfaces that require delegation to $DefaultImpls.
        if (samClass.requiresDelegationToDefaultImpls())
            return null

        val target = reference.symbol.owner as? IrSimpleFunction
            ?: throw AssertionError("Simple function expected: ${reference.symbol.owner.render()}")

        // Can't use JDK LambdaMetafactory for annotated lambdas.
        // JDK LambdaMetafactory doesn't copy annotations from implementation method to an instance method in a
        // corresponding synthetic class, which doesn't look like a binary compatible change.
        // TODO relaxed mode?
        if (target.annotations.isNotEmpty())
            return null

        // Don't use JDK LambdaMetafactory for big arity lambdas.
        if (plainLambda) {
            var parametersCount = target.valueParameters.size
            if (target.extensionReceiverParameter != null) ++parametersCount
            if (parametersCount >= BuiltInFunctionArity.BIG_ARITY)
                return null
        }

        // Can't use indy-based SAM conversion inside inline fun (Ok in inline lambda).
        if (target.parents.any { it.isInlineFunction() || it.isCrossinlineLambda() })
            return null

        // Do the hard work of matching Kotlin functional interface hierarchy against LambdaMetafactory constraints.
        // Briefly: sometimes we have to force boxing on the primitive and inline class values, sometimes we have to keep them unboxed.
        // If this results in conflicting requirements, we can't use INVOKEDYNAMIC with LambdaMetafactory for creating a closure.
        return getLambdaMetafactoryArgsOrNullInner(reference, samMethod, samType, target)
    }

    private fun IrClass.requiresDelegationToDefaultImpls(): Boolean {
        for (irMemberFun in functions) {
            if (irMemberFun.modality == Modality.ABSTRACT)
                continue
            val irImplFun =
                if (irMemberFun.isFakeOverride)
                    irMemberFun.findInterfaceImplementation(context.state.jvmDefaultMode)
                        ?: continue
                else
                    irMemberFun
            if (irImplFun.origin == IrDeclarationOrigin.IR_EXTERNAL_JAVA_DECLARATION_STUB)
                continue
            if (!irImplFun.isCompiledToJvmDefault(context.state.jvmDefaultMode))
                return true
        }
        return false
    }

    private fun getLambdaMetafactoryArgsOrNullInner(
        reference: IrFunctionReference,
        samMethod: IrSimpleFunction,
        samType: IrType,
        implLambda: IrSimpleFunction
    ): LambdaMetafactoryArguments? {
        val nonFakeOverriddenFuns = samMethod.allOverridden().filterNot { it.isFakeOverride }
        val relevantOverriddenFuns = if (samMethod.isFakeOverride) nonFakeOverriddenFuns else nonFakeOverriddenFuns + samMethod

        // Create a fake instance method as if it was defined in a class implementing SAM interface
        // (such class would be eventually created by LambdaMetafactory at run-time).
        val fakeClass = context.irFactory.buildClass { name = Name.special("<fake>") }
        fakeClass.parent = context.ir.symbols.kotlinJvmInternalInvokeDynamicPackage
        val fakeInstanceMethod = buildFakeOverrideMember(samType, samMethod, fakeClass) as IrSimpleFunction
        (fakeInstanceMethod as IrFakeOverrideFunction).acquireSymbol(IrSimpleFunctionSymbolImpl())
        fakeInstanceMethod.overriddenSymbols = listOf(samMethod.symbol)

        // Compute signature adaptation constraints for a fake instance method signature against all relevant overrides.
        // If at any step we encounter a conflict (e.g., one override requires boxing a parameter, and another requires
        // to keep it unboxed), we can't adapt this signature and can't use LambdaMetafactory to create a closure.
        //
        // Note that those constraints are not checked precisely in JDK 1.8 (jdk1.8.0_231), but are checked more strictly
        // in later JDK versions and in D8 (so if you see an exception from D8 in codegen test failures, corresponding code
        // with INVOKEDYNAMIC would quite likely fail on JDK 9 and beyond).
        //
        // Example 1 (requires boxing):
        //      fun interface IFoo<T> {
        //          fun foo(x: T)
        //      }
        //      val t = IFoo<Int> { println(it + 1) }
        // Here IFoo<T>::foo requires 'x' to be reference type (even though corresponding lambda accepts a primitive int).
        // this
        //
        // Example 2 (no explicit override, boxing-unboxing conflict):
        //      fun interface IFooT<T> {
        //          fun foo(x: T)
        //      }
        //      fun interface IFooInt {
        //          fun foo(x: Int)
        //      }
        //      fun interface IFooMix : IFooT<Int>, IFooInt
        //      val t = IFooMix { println(it + 1) }
        // Here IFooT<T>::foo requires 'x' to be of a reference type, and IFooInt::foo requires 'x' to be of a primitive type.
        // LambdaMetafactory can't handle such case.
        //
        // Example 3 (explicit override, boxing-unboxing conflict):
        //      fun interface IFooT<T> {
        //          fun foo(x: T)
        //      }
        //      fun interface IFooInt {
        //          fun foo(x: Int)
        //      }
        //      fun interface IFooMix : IFooT<Int>, IFooInt {
        //          override fun foo(x: Int)
        //      }
        //      val t = IFooMix { println(it + 1) }
        // Here, even though we have an explicit 'override fun foo(x: Int)' in IFooMix, we don't generate a bridge for 'foo' in IFooMix.
        // Thus, class for a lambda created by LambdaMetafactory should provide a bridge for 'foo'.
        // Thus, 'x' should be of a reference type.
        // On the other hand, it should also override IFooInt#foo, where 'x' should be a primitive type.
        // LambdaMetafactory can't handle such case.
        //
        // TODO accept Example 3 if IFooMix is compiled with default interface methods
        // Note that this is a conservative check; if we reject LambdaMetafactory-based closure generation scheme, compiler would still
        // generate proper (although somewhat sub-optimal) code with explicit class for a corresponding SAM-converted lambda.
        val signatureAdaptationConstraints = run {
            var result = SignatureAdaptationConstraints(emptyMap(), null)
            for (overriddenFun in relevantOverriddenFuns) {
                val constraintsFromOverridden = computeSignatureAdaptationConstraints(fakeInstanceMethod, overriddenFun)
                    ?: return null
                result = joinSignatureAdaptationConstraints(result, constraintsFromOverridden)
                    ?: return null
            }
            result
        }

        // We should have bailed out before if we encountered any kind of type adaptation conflict.
        // Still, check that we are fine - just in case.
        if (signatureAdaptationConstraints.returnType == TypeAdaptationConstraint.CONFLICT ||
            signatureAdaptationConstraints.valueParameters.values.any { it == TypeAdaptationConstraint.CONFLICT }
        )
            return null

        adaptFakeInstanceMethodSignature(fakeInstanceMethod, signatureAdaptationConstraints)

        adaptLambdaSignature(implLambda, fakeInstanceMethod, signatureAdaptationConstraints)

        if (samMethod.isFakeOverride && nonFakeOverriddenFuns.size == 1) {
            return LambdaMetafactoryArguments(nonFakeOverriddenFuns.single(), fakeInstanceMethod, reference, listOf())
        }
        return LambdaMetafactoryArguments(samMethod, fakeInstanceMethod, reference, nonFakeOverriddenFuns)
    }

    private fun adaptLambdaSignature(
        lambda: IrSimpleFunction,
        fakeInstanceMethod: IrSimpleFunction,
        constraints: SignatureAdaptationConstraints
    ) {
        val lambdaParameters = collectValueParameters(lambda)
        val methodParameters = collectValueParameters(fakeInstanceMethod)
        if (lambdaParameters.size != methodParameters.size)
            throw AssertionError(
                "Mismatching lambda and instance method parameters:\n" +
                        "lambda: ${lambda.render()}\n" +
                        "  (${lambdaParameters.size} parameters)\n" +
                        "instance method: ${fakeInstanceMethod.render()}\n" +
                        "  (${methodParameters.size} parameters)"
            )
        for ((lambdaParameter, methodParameter) in lambdaParameters.zip(methodParameters)) {
            // TODO box inline class parameters only?
            val parameterConstraint = constraints.valueParameters[methodParameter]
            if (parameterConstraint == TypeAdaptationConstraint.FORCE_BOXING) {
                lambdaParameter.type = lambdaParameter.type.makeNullable()
            }
        }
        if (constraints.returnType == TypeAdaptationConstraint.FORCE_BOXING) {
            lambda.returnType = lambda.returnType.makeNullable()
        }
    }

    private fun adaptFakeInstanceMethodSignature(fakeInstanceMethod: IrSimpleFunction, constraints: SignatureAdaptationConstraints) {
        for ((valueParameter, constraint) in constraints.valueParameters) {
            if (valueParameter.parent != fakeInstanceMethod)
                throw AssertionError(
                    "Unexpected value parameter: ${valueParameter.render()}; fakeInstanceMethod:\n" +
                            fakeInstanceMethod.dump()
                )
            if (constraint == TypeAdaptationConstraint.FORCE_BOXING) {
                valueParameter.type = valueParameter.type.makeNullable()
            }
        }
        if (constraints.returnType == TypeAdaptationConstraint.FORCE_BOXING) {
            fakeInstanceMethod.returnType = fakeInstanceMethod.returnType.makeNullable()
        }
    }

    private enum class TypeAdaptationConstraint {
        FORCE_BOXING,
        KEEP_UNBOXED,
        CONFLICT
    }

    private class SignatureAdaptationConstraints(
        val valueParameters: Map<IrValueParameter, TypeAdaptationConstraint>,
        val returnType: TypeAdaptationConstraint?
    )

    private fun computeSignatureAdaptationConstraints(
        adapteeFun: IrSimpleFunction,
        expectedFun: IrSimpleFunction
    ): SignatureAdaptationConstraints? {
        val returnTypeConstraint = computeReturnTypeAdaptationConstraint(adapteeFun, expectedFun)
        if (returnTypeConstraint == TypeAdaptationConstraint.CONFLICT)
            return null

        val valueParameterConstraints = HashMap<IrValueParameter, TypeAdaptationConstraint>()
        val adapteeParameters = collectValueParameters(adapteeFun)
        val expectedParameters = collectValueParameters(expectedFun)
        if (adapteeParameters.size != expectedParameters.size)
            throw AssertionError(
                "Mismatching value parameters:\n" +
                        "adaptee: ${adapteeFun.render()}\n" +
                        "  ${adapteeParameters.size} value parameters;\n" +
                        "expected: ${expectedFun.render()}\n" +
                        "  ${expectedParameters.size} value parameters."
            )
        for ((adapteeParameter, expectedParameter) in adapteeParameters.zip(expectedParameters)) {
            val parameterConstraint = computeParameterTypeAdaptationConstraint(adapteeParameter.type, expectedParameter.type)
                ?: continue
            if (parameterConstraint == TypeAdaptationConstraint.CONFLICT)
                return null
            valueParameterConstraints[adapteeParameter] = parameterConstraint
        }

        return SignatureAdaptationConstraints(
            if (valueParameterConstraints.isEmpty()) emptyMap() else valueParameterConstraints,
            returnTypeConstraint
        )
    }

    private fun computeParameterTypeAdaptationConstraint(adapteeType: IrType, expectedType: IrType): TypeAdaptationConstraint? {
        if (adapteeType !is IrSimpleType)
            throw AssertionError("Simple type expected: ${adapteeType.render()}")
        if (expectedType !is IrSimpleType)
            throw AssertionError("Simple type expected: ${expectedType.render()}")

        // TODO what if adapteeType and/or expectedType are type parameters with JVM primitive type upper bounds?

        if (adapteeType.isNothing() || adapteeType.isNullableNothing())
            return TypeAdaptationConstraint.CONFLICT

        // ** JVM primitives **
        // All Kotlin types mapped to JVM primitive are final,
        // and their supertypes are trivially mapped reference types.
        if (adapteeType.isJvmPrimitiveType()) {
            return if (expectedType.isJvmPrimitiveType())
                TypeAdaptationConstraint.KEEP_UNBOXED
            else
                TypeAdaptationConstraint.FORCE_BOXING
        }

        // ** Inline classes **
        // All Kotlin inline classes are final,
        // and their supertypes are trivially mapped to reference types.
        val erasedAdapteeClass = getErasedClassForSignatureAdaptation(adapteeType)
        if (erasedAdapteeClass.isInline) {
            // Inline classes mapped to non-null reference types are a special case because they can't be boxed trivially.
            // TODO consider adding a special type annotation to force boxing on an inline class type regardless of its underlying type.
            val underlyingAdapteeType = getInlineClassUnderlyingType(erasedAdapteeClass) as? IrSimpleType
                ?: throw AssertionError("Underlying type for inline class should be a simple type: ${erasedAdapteeClass.render()}")
            if (!underlyingAdapteeType.hasQuestionMark && !underlyingAdapteeType.isJvmPrimitiveType()) {
                return TypeAdaptationConstraint.CONFLICT
            }

            val erasedExpectedClass = getErasedClassForSignatureAdaptation(expectedType)
            return if (erasedExpectedClass.isInline) {
                // LambdaMetafactory doesn't know about method mangling.
                TypeAdaptationConstraint.CONFLICT
            } else {
                // Trying to pass inline class value as non-inline class value (Any or other supertype)
                // => box it
                TypeAdaptationConstraint.FORCE_BOXING
            }
        }

        // Other cases don't enforce type adaptation
        return null
    }

    private fun getErasedClassForSignatureAdaptation(irType: IrSimpleType): IrClass =
        when (val classifier = irType.classifier.owner) {
            is IrTypeParameter -> classifier.erasedUpperBound
            is IrClass -> classifier
            else ->
                throw AssertionError("Unexpected classifier: ${classifier.render()}")
        }

    private fun computeReturnTypeAdaptationConstraint(
        adapteeFun: IrSimpleFunction,
        expectedFun: IrSimpleFunction
    ): TypeAdaptationConstraint? {
        val adapteeReturnType = adapteeFun.returnType
        if (adapteeReturnType.isUnit()) {
            // Can't mix '()V' and '()Lkotlin.Unit;' or '()Ljava.lang.Object;' in supertype method signatures.
            return if (expectedFun.returnType.isUnit())
                TypeAdaptationConstraint.KEEP_UNBOXED
            else {
                TypeAdaptationConstraint.FORCE_BOXING
            }
        }

        val expectedReturnType = expectedFun.returnType
        return computeParameterTypeAdaptationConstraint(adapteeReturnType, expectedReturnType)
    }

    private fun joinSignatureAdaptationConstraints(
        sig1: SignatureAdaptationConstraints,
        sig2: SignatureAdaptationConstraints
    ): SignatureAdaptationConstraints? {
        val newReturnTypeConstraint = composeTypeAdaptationConstraints(sig1.returnType, sig2.returnType)
        if (newReturnTypeConstraint == TypeAdaptationConstraint.CONFLICT)
            return null

        val newValueParameterConstraints =
            when {
                sig1.valueParameters.isEmpty() -> sig2.valueParameters
                sig2.valueParameters.isEmpty() -> sig1.valueParameters
                else -> {
                    val joined = HashMap<IrValueParameter, TypeAdaptationConstraint>()
                    joined.putAll(sig1.valueParameters)
                    for ((vp2, t2) in sig2.valueParameters.entries) {
                        val tx = composeTypeAdaptationConstraints(joined[vp2], t2) ?: continue
                        if (tx == TypeAdaptationConstraint.CONFLICT)
                            return null
                        joined[vp2] = tx
                    }
                    joined
                }
            }

        return SignatureAdaptationConstraints(newValueParameterConstraints, newReturnTypeConstraint)
    }

    private fun composeTypeAdaptationConstraints(t1: TypeAdaptationConstraint?, t2: TypeAdaptationConstraint?): TypeAdaptationConstraint? =
        when {
            t1 == null -> t2
            t2 == null -> t1
            t1 == t2 -> t1
            else ->
                TypeAdaptationConstraint.CONFLICT
        }


    private fun IrDeclarationParent.isInlineFunction() =
        this is IrSimpleFunction && isInline && origin != IrDeclarationOrigin.LOCAL_FUNCTION_FOR_LAMBDA

    private fun IrDeclarationParent.isCrossinlineLambda(): Boolean {
        val irFun = this as? IrSimpleFunction ?: return false
        return origin == IrDeclarationOrigin.LOCAL_FUNCTION_FOR_LAMBDA &&
                inlineLambdaToValueParameter[irFun]?.isCrossinline == true
    }

    private fun IrType.isJvmPrimitiveType() =
        isBoolean() || isChar() || isByte() || isShort() || isInt() || isLong() || isFloat() || isDouble()

    private fun wrapSamConversionArgumentWithIndySamConversion(
        expression: IrTypeOperatorCall,
        lambdaMetafactoryArguments: LambdaMetafactoryArguments
    ): IrExpression {
        val samType = expression.typeOperand
        return when (val argument = expression.argument) {
            is IrFunctionReference -> {
                wrapWithIndySamConversion(samType, lambdaMetafactoryArguments)
            }
            is IrBlock -> {
                val indySamConversion = wrapWithIndySamConversion(samType, lambdaMetafactoryArguments)
                argument.statements[argument.statements.size - 1] = indySamConversion
                argument.type = indySamConversion.type
                return argument
            }
            else -> throw AssertionError("Block or function reference expected: ${expression.render()}")
        }
    }

    private val jvmIndyLambdaMetafactoryIntrinsic = context.ir.symbols.indyLambdaMetafactoryIntrinsic

    private val specialNullabilityAnnotationsFqNames =
        setOf(
            context.ir.symbols.flexibleNullabilityAnnotationFqName,
            context.ir.symbols.enhancedNullabilityAnnotationFqName
        )

    private fun wrapWithIndySamConversion(
        samType: IrType,
        lambdaMetafactoryArguments: LambdaMetafactoryArguments
    ): IrCall {
        val notNullSamType = samType.makeNotNull()
            .removeAnnotations { it.type.classFqName in specialNullabilityAnnotationsFqNames }
        return context.createJvmIrBuilder(currentScope!!.scope.scopeOwnerSymbol).run {
            // See [org.jetbrains.kotlin.backend.jvm.JvmSymbols::indyLambdaMetafactoryIntrinsic].
            irCall(jvmIndyLambdaMetafactoryIntrinsic, notNullSamType).apply {
                putTypeArgument(0, notNullSamType)
                putValueArgument(0, irRawFunctionRef(lambdaMetafactoryArguments.samMethod))
                putValueArgument(1, lambdaMetafactoryArguments.implMethodReference)
                putValueArgument(2, irRawFunctionRef(lambdaMetafactoryArguments.fakeInstanceMethod))
                putValueArgument(3, irVarargOfRawFunctionRefs(lambdaMetafactoryArguments.extraOverriddenMethods))
            }
        }
    }

    private fun IrBuilderWithScope.irRawFunctionRef(irFun: IrFunction) =
        irRawFunctionReferefence(context.irBuiltIns.anyType, irFun.symbol)

    private fun IrBuilderWithScope.irVarargOfRawFunctionRefs(irFuns: List<IrFunction>) =
        irVararg(context.irBuiltIns.anyType, irFuns.map { irRawFunctionRef(it) })

    private fun collectValueParameters(irFun: IrFunction): List<IrValueParameter> {
        if (irFun.extensionReceiverParameter == null)
            return irFun.valueParameters
        return ArrayList<IrValueParameter>().apply {
            add(irFun.extensionReceiverParameter!!)
            addAll(irFun.valueParameters)
        }
    }

    private fun IrType.isUnboxedInlineClassType() =
        this is IrSimpleType && isInlined() && !hasQuestionMark

    private inner class FunctionReferenceBuilder(val irFunctionReference: IrFunctionReference, val samSuperType: IrType? = null) {
        private val isLambda = irFunctionReference.origin.isLambda

        private val callee = irFunctionReference.symbol.owner

        // Only function references can bind a receiver and even then we can only bind either an extension or a dispatch receiver.
        // However, when we bind a value of an inline class type as a receiver, the receiver will turn into an argument of
        // the function in question. Yet we still need to record it as the "receiver" in CallableReference in order for reflection
        // to work correctly.
        private val boundReceiver: Pair<IrValueParameter, IrExpression>? =
            if (callee.isJvmStaticInObject()) createFakeBoundReceiverForJvmStaticInObject()
            else irFunctionReference.getArgumentsWithIr().singleOrNull()

        // The type of the reference is KFunction<in A1, ..., in An, out R>
        private val parameterTypes = (irFunctionReference.type as IrSimpleType).arguments.map { (it as IrTypeProjection).type }
        private val argumentTypes = parameterTypes.dropLast(1)

        private val typeArgumentsMap = irFunctionReference.typeSubstitutionMap

        private val functionSuperClass =
            samSuperType?.classOrNull
                ?: if (irFunctionReference.isSuspend)
                    context.ir.symbols.getJvmSuspendFunctionClass(argumentTypes.size)
                else
                    context.ir.symbols.getJvmFunctionClass(argumentTypes.size)
        private val superMethod =
            functionSuperClass.owner.getSingleAbstractMethod()
                ?: throw AssertionError("Not a SAM class: ${functionSuperClass.owner.render()}")

        private val useOptimizedSuperClass =
            context.state.generateOptimizedCallableReferenceSuperClasses

        private val adapteeCall: IrFunctionAccessExpression? =
            if (callee.origin == IrDeclarationOrigin.ADAPTER_FOR_CALLABLE_REFERENCE) {
                // The body of a callable reference adapter contains either only a call, or an IMPLICIT_COERCION_TO_UNIT type operator
                // applied to a call. That call's target is the original function which we need to get owner/name/signature.
                val call = when (val statement = callee.body!!.statements.single()) {
                    is IrTypeOperatorCall -> {
                        assert(statement.operator == IrTypeOperator.IMPLICIT_COERCION_TO_UNIT) {
                            "Unexpected type operator in ADAPTER_FOR_CALLABLE_REFERENCE: ${callee.render()}"
                        }
                        statement.argument
                    }
                    is IrReturn -> statement.value
                    else -> statement
                }
                if (call !is IrFunctionAccessExpression) {
                    throw UnsupportedOperationException("Unknown structure of ADAPTER_FOR_CALLABLE_REFERENCE: ${callee.render()}")
                }
                call
            } else {
                null
            }

        private val adaptedReferenceOriginalTarget: IrFunction? = adapteeCall?.symbol?.owner
        private val isAdaptedReference = adaptedReferenceOriginalTarget != null

        private val samInterface = samSuperType?.getClass()
        private val isKotlinFunInterface = samInterface != null && !samInterface.isFromJava()

        private val needToGenerateSamEqualsHashCodeMethods =
            isKotlinFunInterface && (isAdaptedReference || !isLambda)

        private val superType =
            samSuperType ?: when {
                isLambda -> context.ir.symbols.lambdaClass
                useOptimizedSuperClass -> when {
                    isAdaptedReference -> context.ir.symbols.adaptedFunctionReference
                    else -> context.ir.symbols.functionReferenceImpl
                }
                else -> context.ir.symbols.functionReference
            }.defaultType

        private val functionReferenceClass = context.irFactory.buildClass {
            setSourceRange(irFunctionReference)
            visibility = DescriptorVisibilities.LOCAL
            // A callable reference results in a synthetic class, while a lambda is not synthetic.
            // We don't produce GENERATED_SAM_IMPLEMENTATION, which is always synthetic.
            origin = if (isLambda) JvmLoweredDeclarationOrigin.LAMBDA_IMPL else JvmLoweredDeclarationOrigin.FUNCTION_REFERENCE_IMPL
            name = SpecialNames.NO_NAME_PROVIDED
        }.apply {
            parent = currentDeclarationParent ?: error("No current declaration parent at ${irFunctionReference.dump()}")
            superTypes = listOfNotNull(
                superType,
                if (samSuperType == null)
                    functionSuperClass.typeWith(parameterTypes)
                else null,
                if (irFunctionReference.isSuspend)
                    context.ir.symbols.suspendFunctionInterface.defaultType
                else null,
                if (needToGenerateSamEqualsHashCodeMethods)
                    context.ir.symbols.functionAdapter.defaultType
                else null,
            )
            if (samInterface != null && origin == JvmLoweredDeclarationOrigin.LAMBDA_IMPL) {
                // Old back-end generates formal type parameters as in SAM supertype.
                // Here we create formal type parameters with same names and equivalent upper bounds.
                // We don't really perform any type substitutions within class body
                // (it's all fine as soon as we have required generic signatures and don't fail anywhere).
                // NB this would no longer matter if we generate SAM wrapper classes as synthetic.
                typeParameters = createFakeFormalTypeParameters(samInterface.typeParameters, this)
            }
            createImplicitParameterDeclarationWithWrappedDescriptor()
            copyAttributes(irFunctionReference)
            if (isLambda) {
                metadata = irFunctionReference.symbol.owner.metadata
            }
        }

        private fun createFakeFormalTypeParameters(sourceTypeParameters: List<IrTypeParameter>, irClass: IrClass): List<IrTypeParameter> {
            if (sourceTypeParameters.isEmpty()) return emptyList()

            val fakeTypeParameters = sourceTypeParameters.map {
                buildTypeParameter(irClass) {
                    updateFrom(it)
                    name = it.name
                }
            }
            val typeRemapper = IrTypeParameterRemapper(sourceTypeParameters.associateWith { fakeTypeParameters[it.index] })
            for (fakeTypeParameter in fakeTypeParameters) {
                val sourceTypeParameter = sourceTypeParameters[fakeTypeParameter.index]
                fakeTypeParameter.superTypes = sourceTypeParameter.superTypes.map { typeRemapper.remapType(it) }
            }

            return fakeTypeParameters
        }

        private val receiverField = context.ir.symbols.functionReferenceReceiverField.owner

        fun build(): IrExpression = context.createJvmIrBuilder(currentScope!!.scope.scopeOwnerSymbol).run {
            irBlock(irFunctionReference.startOffset, irFunctionReference.endOffset) {
                val constructor = createConstructor()
                val boundReceiverVar =
                    if (samSuperType != null && boundReceiver != null) {
                        irTemporary(boundReceiver.second)
                    } else null
                createInvokeMethod(boundReceiverVar)

                if (!isLambda && samSuperType == null && !useOptimizedSuperClass) {
                    createLegacyMethodOverride(irSymbols.functionReferenceGetSignature.owner) {
                        generateSignature(callee.symbol)
                    }
                    createLegacyMethodOverride(irSymbols.functionReferenceGetName.owner) {
                        irString(callee.originalName.asString())
                    }
                    createLegacyMethodOverride(irSymbols.functionReferenceGetOwner.owner) {
                        calculateOwner(callee.parent, backendContext)
                    }
                }

                if (needToGenerateSamEqualsHashCodeMethods) {
                    generateSamEqualsHashCodeMethods(boundReceiverVar)
                }
                if (isKotlinFunInterface) {
                    functionReferenceClass.addFakeOverrides(context.irBuiltIns)
                }

                +functionReferenceClass
                +irCall(constructor.symbol).apply {
                    if (valueArgumentsCount > 0) putValueArgument(0, boundReceiver!!.second)
                }
            }
        }

        private fun JvmIrBuilder.generateSamEqualsHashCodeMethods(boundReceiverVar: IrVariable?) {
            checkNotNull(samSuperType) { "equals/hashCode can only be generated for fun interface wrappers: ${callee.render()}" }

            if (!useOptimizedSuperClass) {
                // This is the case of a fun interface wrapper over a (maybe adapted) function reference,
                // with `-Xno-optimized-callable-references` enabled. We can't use constructors of FunctionReferenceImpl,
                // so we'd need to basically generate a full class for a reference inheriting from FunctionReference,
                // effectively disabling the optimization of fun interface wrappers over references.
                // This scenario is probably not very popular because it involves using equals/hashCode on function references
                // and enabling the mentioned internal compiler argument.
                // Right now we generate them as abstract so that any call would result in AbstractMethodError.
                // TODO: generate getFunctionDelegate, equals and hashCode properly in this case
                functionReferenceClass.addFunction("equals", backendContext.irBuiltIns.booleanType, Modality.ABSTRACT).apply {
                    addValueParameter("other", backendContext.irBuiltIns.anyNType)
                    overriddenSymbols = listOf(functionSuperClass.functions.single { isEqualsFromAny(it.owner) })
                }
                functionReferenceClass.addFunction("hashCode", backendContext.irBuiltIns.intType, Modality.ABSTRACT).apply {
                    overriddenSymbols = listOf(functionSuperClass.functions.single { isHashCodeFromAny(it.owner) })
                }
                return
            }

            SamEqualsHashCodeMethodsGenerator(backendContext, functionReferenceClass, samSuperType) {
                val internalClass = when {
                    isAdaptedReference -> backendContext.ir.symbols.adaptedFunctionReference
                    else -> backendContext.ir.symbols.functionReferenceImpl
                }
                val constructor = internalClass.owner.constructors.single {
                    // arity, [receiver], owner, name, signature, flags
                    it.valueParameters.size == 1 + (if (boundReceiver != null) 1 else 0) + 4
                }
                irCallConstructor(constructor.symbol, emptyList()).apply {
                    generateConstructorCallArguments(this) { irGet(boundReceiverVar!!) }
                }
            }.generate()
        }

        private fun isEqualsFromAny(f: IrSimpleFunction): Boolean =
            f.name.asString() == "equals" && f.extensionReceiverParameter == null &&
                    f.valueParameters.singleOrNull()?.type?.isNullableAny() == true

        private fun isHashCodeFromAny(f: IrSimpleFunction): Boolean =
            f.name.asString() == "hashCode" && f.extensionReceiverParameter == null && f.valueParameters.isEmpty()

        private fun createConstructor(): IrConstructor =
            functionReferenceClass.addConstructor {
                origin = JvmLoweredDeclarationOrigin.GENERATED_MEMBER_IN_CALLABLE_REFERENCE
                returnType = functionReferenceClass.defaultType
                isPrimary = true
            }.apply {
                // Add receiver parameter for bound function references
                if (samSuperType == null) {
                    boundReceiver?.let { (param, arg) ->
                        valueParameters += param.copyTo(irFunction = this, index = 0, type = arg.type)
                    }
                }

                // Super constructor:
                // - For SAM references, the super class is Any
                // - For lambdas, accepts arity
                // - For optimized function references (1.4+), accepts:
                //       arity, [receiver], owner, name, signature, flags
                // - For unoptimized function references, accepts:
                //       arity, [receiver]
                val constructor = if (samSuperType != null) {
                    context.irBuiltIns.anyClass.owner.constructors.single()
                } else {
                    val expectedArity =
                        if (isLambda && !isAdaptedReference) 1
                        else 1 + (if (boundReceiver != null) 1 else 0) + (if (useOptimizedSuperClass) 4 else 0)
                    superType.getClass()!!.constructors.single {
                        it.valueParameters.size == expectedArity
                    }
                }

                body = context.createJvmIrBuilder(symbol).run {
                    irBlockBody(startOffset, endOffset) {
                        +irDelegatingConstructorCall(constructor).also { call ->
                            if (samSuperType == null) {
                                generateConstructorCallArguments(call) { irGet(valueParameters.first()) }
                            }
                        }
                        +IrInstanceInitializerCallImpl(startOffset, endOffset, functionReferenceClass.symbol, context.irBuiltIns.unitType)
                    }
                }
            }

        private fun JvmIrBuilder.generateConstructorCallArguments(
            call: IrFunctionAccessExpression,
            generateBoundReceiver: IrBuilder.() -> IrExpression
        ) {
            var index = 0
            call.putValueArgument(index++, irInt(argumentTypes.size + if (irFunctionReference.isSuspend) 1 else 0))
            if (boundReceiver != null) {
                call.putValueArgument(index++, generateBoundReceiver())
            }
            if (!isLambda && useOptimizedSuperClass) {
                val callableReferenceTarget = adaptedReferenceOriginalTarget ?: callee
                val owner = calculateOwnerKClass(callableReferenceTarget.parent, backendContext)
                call.putValueArgument(index++, kClassToJavaClass(owner, backendContext))
                call.putValueArgument(index++, irString(callableReferenceTarget.originalName.asString()))
                call.putValueArgument(index++, generateSignature(callableReferenceTarget.symbol))
                call.putValueArgument(index, irInt(getFunctionReferenceFlags(callableReferenceTarget)))
            }
        }

        private fun getFunctionReferenceFlags(callableReferenceTarget: IrFunction): Int {
            val isTopLevelBit = getCallableReferenceTopLevelFlag(callableReferenceTarget)
            val adaptedCallableReferenceFlags = getAdaptedCallableReferenceFlags()
            return isTopLevelBit + (adaptedCallableReferenceFlags shl 1)
        }

        private fun getAdaptedCallableReferenceFlags(): Int {
            if (adaptedReferenceOriginalTarget == null) return 0

            val isVarargMappedToElementBit = if (hasVarargMappedToElement()) 1 else 0
            val isSuspendConvertedBit = if (!adaptedReferenceOriginalTarget.isSuspend && callee.isSuspend) 1 else 0
            val isCoercedToUnitBit = if (!adaptedReferenceOriginalTarget.returnType.isUnit() && callee.returnType.isUnit()) 1 else 0

            return isVarargMappedToElementBit +
                    (isSuspendConvertedBit shl 1) +
                    (isCoercedToUnitBit shl 2)
        }

        private fun hasVarargMappedToElement(): Boolean {
            if (adapteeCall == null) return false
            for (i in 0 until adapteeCall.valueArgumentsCount) {
                val arg = adapteeCall.getValueArgument(i) ?: continue
                if (arg !is IrVararg) continue
                for (varargElement in arg.elements) {
                    if (varargElement is IrGetValue) return true
                }
            }
            return false
        }

        private fun createInvokeMethod(receiverVar: IrValueDeclaration?): IrSimpleFunction =
            functionReferenceClass.addFunction {
                setSourceRange(if (isLambda) callee else irFunctionReference)
                name =
                    if (samSuperType == null && callee.returnType.erasedUpperBound.isInline && context.state.functionsWithInlineClassReturnTypesMangled) {
                        // For functions with inline class return type we need to mangle the invoke method.
                        // Otherwise, bridge lowering may fail to generate bridges for inline class types erasing to Any.
                        val suffix = InlineClassAbi.hashReturnSuffix(callee)
                        Name.identifier("${superMethod.name.asString()}-${suffix}")
                    } else superMethod.name
                returnType = callee.returnType
                isSuspend = callee.isSuspend
            }.apply {
                overriddenSymbols += superMethod.symbol
                dispatchReceiverParameter = buildReceiverParameter(
                    this,
                    IrDeclarationOrigin.INSTANCE_RECEIVER,
                    functionReferenceClass.symbol.defaultType
                )
                if (isLambda) createLambdaInvokeMethod() else createFunctionReferenceInvokeMethod(receiverVar)
            }

        // Inline the body of an anonymous function into the generated lambda subclass.
        private fun IrSimpleFunction.createLambdaInvokeMethod() {
            annotations += callee.annotations
            val valueParameterMap = callee.explicitParameters.withIndex().associate { (index, param) ->
                param to param.copyTo(this, index = index)
            }
            valueParameters += valueParameterMap.values
            body = callee.moveBodyTo(this, valueParameterMap)
        }

        private fun IrSimpleFunction.createFunctionReferenceInvokeMethod(receiver: IrValueDeclaration?) {
            for ((index, argumentType) in argumentTypes.withIndex()) {
                addValueParameter {
                    name = Name.identifier("p$index")
                    type = argumentType
                }
            }

            body = context.createJvmIrBuilder(symbol, startOffset, endOffset).run {
                var unboundIndex = 0
                irExprBody(irCall(callee).apply {
                    for (typeParameter in irFunctionReference.symbol.owner.allTypeParameters) {
                        putTypeArgument(typeParameter.index, typeArgumentsMap[typeParameter.symbol])
                    }

                    for (parameter in callee.explicitParameters) {
                        when {
                            boundReceiver?.first == parameter ->
                                // Bound receiver parameter. For function references, this is stored in a field of the superclass.
                                // For sam references, we just capture the value in a local variable and LocalDeclarationsLowering
                                // will put it into a field.
                                if (samSuperType == null)
                                    irImplicitCast(
                                        irGetField(
                                            irGet(dispatchReceiverParameter!!),
                                            this@FunctionReferenceBuilder.receiverField
                                        ),
                                        boundReceiver.second.type
                                    )
                                else
                                    irGet(receiver!!)

                            // If a vararg parameter corresponds to exactly one KFunction argument, which is an array, that array
                            // is forwarded as is.
                            //
                            //     fun f(x: (Int, Array<String>) -> String) = x(0, arrayOf("OK", "FAIL"))
                            //     fun h(i: Int, vararg xs: String) = xs[i]
                            //     f(::h)
                            //
                            parameter.isVararg && unboundIndex < argumentTypes.size && parameter.type == valueParameters[unboundIndex].type ->
                                irGet(valueParameters[unboundIndex++])
                            // In all other cases, excess arguments are packed into a new array.
                            //
                            //     fun g(x: (Int, String, String) -> String) = x(0, "OK", "FAIL")
                            //     f(::h) == g(::h)
                            //
                            parameter.isVararg && (unboundIndex < argumentTypes.size || !parameter.hasDefaultValue()) ->
                                irArray(parameter.type) {
                                    (unboundIndex until argumentTypes.size).forEach { +irGet(valueParameters[unboundIndex++]) }
                                }

                            unboundIndex >= argumentTypes.size ->
                                // Default value argument (this pass doesn't handle suspend functions, otherwise
                                // it could also be the continuation argument)
                                null

                            else ->
                                irGet(valueParameters[unboundIndex++])
                        }?.let { putArgument(callee, parameter, it) }
                    }
                })
            }
        }

        private fun buildOverride(superFunction: IrSimpleFunction, newReturnType: IrType = superFunction.returnType): IrSimpleFunction =
            functionReferenceClass.addFunction {
                setSourceRange(irFunctionReference)
                origin = JvmLoweredDeclarationOrigin.GENERATED_MEMBER_IN_CALLABLE_REFERENCE
                name = superFunction.name
                returnType = newReturnType
                visibility = superFunction.visibility
                isSuspend = superFunction.isSuspend
            }.apply {
                overriddenSymbols += superFunction.symbol
                dispatchReceiverParameter = functionReferenceClass.thisReceiver?.copyTo(this)
            }

        private val IrFunction.originalName: Name
            get() = metadata?.name ?: name

        private fun JvmIrBuilder.generateSignature(target: IrFunctionSymbol): IrExpression =
            irCall(backendContext.ir.symbols.signatureStringIntrinsic).apply {
                putValueArgument(
                    0,
                    //don't pass receivers otherwise LocalDeclarationLowering will create additional captured parameters
                    IrFunctionReferenceImpl(
                        UNDEFINED_OFFSET, UNDEFINED_OFFSET, irFunctionReference.type, target,
                        irFunctionReference.typeArgumentsCount, target.owner.valueParameters.size,
                        irFunctionReference.reflectionTarget, null
                    ).apply {
                        copyTypeArgumentsFrom(irFunctionReference)
                    }
                )
            }

        private fun createFakeBoundReceiverForJvmStaticInObject(): Pair<IrValueParameter, IrGetObjectValueImpl> {
            // JvmStatic functions in objects are special in that they are generated as static methods in the bytecode, and JVM IR lowers
            // both declarations and call sites early on in jvmStaticInObjectPhase because it's easier that way in subsequent lowerings.
            // However from the point of view of Kotlin language (and thus reflection), these functions still take the dispatch receiver
            // parameter of the object type. So we pretend here that a JvmStatic function in object has an additional dispatch receiver
            // parameter, so that the correct function reference object will be created and reflective calls will work at runtime.
            val objectClass = callee.parentAsClass
            return buildValueParameter(callee) {
                name = Name.identifier("\$this")
                type = objectClass.typeWith()
            } to IrGetObjectValueImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, objectClass.typeWith(), objectClass.symbol)
        }

        private fun createLegacyMethodOverride(
            superFunction: IrSimpleFunction,
            generator: JvmIrBuilder.() -> IrExpression
        ): IrSimpleFunction =
            buildOverride(superFunction).apply {
                body = context.createJvmIrBuilder(symbol, startOffset, endOffset).run {
                    irExprBody(generator())
                }
            }
    }

    companion object {
        private fun IrBuilderWithScope.kClassReference(classType: IrType) =
            IrClassReferenceImpl(
                startOffset, endOffset, context.irBuiltIns.kClassClass.starProjectedType, context.irBuiltIns.kClassClass, classType
            )

        internal fun IrBuilderWithScope.kClassToJavaClass(kClassReference: IrExpression, context: JvmBackendContext) =
            irGet(context.ir.symbols.javaLangClass.starProjectedType, null, context.ir.symbols.kClassJava.owner.getter!!.symbol).apply {
                extensionReceiver = kClassReference
            }

        internal fun IrBuilderWithScope.javaClassReference(classType: IrType, context: JvmBackendContext) =
            kClassToJavaClass(kClassReference(classType), context)

        internal fun IrBuilderWithScope.calculateOwner(irContainer: IrDeclarationParent, context: JvmBackendContext): IrExpression {
            val kClass = calculateOwnerKClass(irContainer, context)

            if ((irContainer as? IrClass)?.isFileClass != true && irContainer !is IrPackageFragment)
                return kClass

            return irCall(context.ir.symbols.getOrCreateKotlinPackage).apply {
                putValueArgument(0, kClassToJavaClass(kClass, context))
                // Note that this name is not used in reflection. There should be the name of the referenced declaration's
                // module instead, but there's no nice API to obtain that name here yet
                // TODO: write the referenced declaration's module name and use it in reflection
                putValueArgument(1, irString(context.state.moduleName))
            }
        }

        internal fun IrBuilderWithScope.calculateOwnerKClass(irContainer: IrDeclarationParent, context: JvmBackendContext): IrExpression =
            kClassReference(getOwnerKClassType(irContainer, context))

        internal fun getOwnerKClassType(irContainer: IrDeclarationParent, context: JvmBackendContext): IrType =
            if (irContainer is IrClass) irContainer.defaultType
            else {
                // For built-in members (i.e. top level `toString`) we generate reference to an internal class for an owner.
                // This allows kotlin-reflect to understand that this is a built-in intrinsic which has no real declaration,
                // and construct a special KCallable object.
                context.ir.symbols.intrinsicsKotlinClass.defaultType
            }

        internal fun getCallableReferenceTopLevelFlag(declaration: IrDeclaration): Int =
            if (isCallableReferenceTopLevel(declaration)) 1 else 0

        internal fun isCallableReferenceTopLevel(declaration: IrDeclaration): Boolean =
            declaration.parent.let { it is IrClass && it.isFileClass }
    }
}
