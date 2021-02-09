/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.ir.allOverridden
import org.jetbrains.kotlin.backend.common.lower.parents
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.ir.erasedUpperBound
import org.jetbrains.kotlin.backend.jvm.ir.getSingleAbstractMethod
import org.jetbrains.kotlin.backend.jvm.ir.isCompiledToJvmDefault
import org.jetbrains.kotlin.builtins.functions.BuiltInFunctionArity
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.builders.declarations.buildClass
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrFunctionReference
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.overrides.buildFakeOverrideMember
import org.jetbrains.kotlin.ir.symbols.impl.IrSimpleFunctionSymbolImpl
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.name.Name

class LambdaMetafactoryArguments(
    val samMethod: IrSimpleFunction,
    val fakeInstanceMethod: IrSimpleFunction,
    val implMethodReference: IrFunctionReference,
    val extraOverriddenMethods: List<IrSimpleFunction>
)

class LambdaMetafactoryArgumentsBuilder(
    private val context: JvmBackendContext,
    private val crossinlineLambdas: Set<IrSimpleFunction>
) {
    /**
     * @see java.lang.invoke.LambdaMetafactory
     */
    fun getLambdaMetafactoryArgumentsOrNull(
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

    private fun IrDeclarationParent.isCrossinlineLambda(): Boolean =
        this is IrSimpleFunction && this in crossinlineLambdas

    private fun IrType.isJvmPrimitiveType() =
        isBoolean() || isChar() || isByte() || isShort() || isInt() || isLong() || isFloat() || isDouble()

    private fun collectValueParameters(irFun: IrFunction): List<IrValueParameter> {
        if (irFun.extensionReceiverParameter == null)
            return irFun.valueParameters
        return ArrayList<IrValueParameter>().apply {
            add(irFun.extensionReceiverParameter!!)
            addAll(irFun.valueParameters)
        }
    }
}