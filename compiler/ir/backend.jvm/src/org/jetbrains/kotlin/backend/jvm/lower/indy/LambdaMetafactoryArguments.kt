/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower.indy

import org.jetbrains.kotlin.backend.common.ir.allOverridden
import org.jetbrains.kotlin.backend.common.ir.isFromJava
import org.jetbrains.kotlin.backend.common.lower.VariableRemapper
import org.jetbrains.kotlin.backend.common.lower.parents
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.ir.erasedUpperBound
import org.jetbrains.kotlin.backend.jvm.ir.getSingleAbstractMethod
import org.jetbrains.kotlin.backend.jvm.ir.isCompiledToJvmDefault
import org.jetbrains.kotlin.backend.jvm.lower.findInterfaceImplementation
import org.jetbrains.kotlin.backend.jvm.lower.isPrivate
import org.jetbrains.kotlin.builtins.functions.BuiltInFunctionArity
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.builders.declarations.buildClass
import org.jetbrains.kotlin.ir.builders.declarations.buildValueParameter
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrFunctionReference
import org.jetbrains.kotlin.ir.expressions.impl.IrFunctionReferenceImpl
import org.jetbrains.kotlin.ir.overrides.buildFakeOverrideMember
import org.jetbrains.kotlin.ir.symbols.impl.IrSimpleFunctionSymbolImpl
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.load.java.JvmAnnotationNames
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.utils.addIfNotNull

internal class LambdaMetafactoryArguments(
    val samMethod: IrSimpleFunction,
    val fakeInstanceMethod: IrSimpleFunction,
    val implMethodReference: IrFunctionReference,
    val extraOverriddenMethods: List<IrSimpleFunction>
)

internal class LambdaMetafactoryArgumentsBuilder(
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
        val samClass = samType.getClass()
            ?: throw AssertionError("SAM type is not a class: ${samType.render()}")

        // Can't use JDK LambdaMetafactory for function references by default (because of 'equals').
        // TODO special mode that would generate indy everywhere?
        if (!reference.origin.isLambda && !samClass.isFromJava())
            return null

        val samMethod = samClass.getSingleAbstractMethod()
            ?: throw AssertionError("SAM class has no single abstract method: ${samClass.render()}")

        // Can't use JDK LambdaMetafactory for fun interface with suspend fun.
        if (samMethod.isSuspend)
            return null

        // Can't use JDK LambdaMetafactory for fun interfaces that require delegation to $DefaultImpls.
        if (samClass.requiresDelegationToDefaultImpls())
            return null

        val implFun = reference.symbol.owner

        // Don't generate references to intrinsic functions as invokedynamic (no such method exists at run-time).
        if (context.irIntrinsics.getIntrinsic(implFun.symbol) != null)
            return null

        if (implFun is IrConstructor && implFun.visibility.isPrivate) {
            // Kotlin generates constructor accessors differently from Java.
            // TODO more precise accessibility check (see SyntheticAccessorLowering::isAccessible)
            // TODO wrap inaccessible constructor with a local function
            return null
        }

        val implFunParent = implFun.parent
        if (implFunParent is IrClass && implFunParent.origin == IrDeclarationOrigin.JVM_MULTIFILE_CLASS) {
            // LambdaMetafactory treats multifile class part members as non-accessible,
            // even if the member is referenced via facade,
            // because corresponding part class is non-accessible
            return null
        }

        // Can't use JDK LambdaMetafactory for annotated lambdas.
        // JDK LambdaMetafactory doesn't copy annotations from implementation method to an instance method in a
        // corresponding synthetic class, which doesn't look like a binary compatible change.
        // TODO relaxed mode?
        if (reference.origin.isLambda && implFun.annotations.isNotEmpty())
            return null

        // Don't use JDK LambdaMetafactory for big arity lambdas.
        if (plainLambda) {
            var parametersCount = implFun.valueParameters.size
            if (implFun.extensionReceiverParameter != null) ++parametersCount
            if (parametersCount >= BuiltInFunctionArity.BIG_ARITY)
                return null
        }

        // Can't use indy-based SAM conversion inside inline fun (Ok in inline lambda).
        if (implFun.parents.any { it.isInlineFunction() || it.isCrossinlineLambda() })
            return null

        // Don't try to use indy on SAM types with non-invariant projections because buildFakeOverrideMember doesn't support such supertypes
        // (and rightly so: supertypes in Kotlin can't have projections in immediate type arguments). This can happen for example in case
        // the SAM type is instantiated with an intersection type in arguments, which is approximated to an out-projection in psi2ir.
        if (samType is IrSimpleType && samType.arguments.any { it is IrTypeProjection && it.variance != Variance.INVARIANT })
            return null

        // Do the hard work of matching Kotlin functional interface hierarchy against LambdaMetafactory constraints.
        // Briefly: sometimes we have to force boxing on the primitive and inline class values, sometimes we have to keep them unboxed.
        // If this results in conflicting requirements, we can't use INVOKEDYNAMIC with LambdaMetafactory for creating a closure.
        return getLambdaMetafactoryArgsOrNullInner(reference, samMethod, samType, implFun)
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
        implFun: IrFunction
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
        if (signatureAdaptationConstraints.hasConflicts())
            return null

        adaptFakeInstanceMethodSignature(fakeInstanceMethod, signatureAdaptationConstraints)
        if (implFun.origin == IrDeclarationOrigin.LOCAL_FUNCTION_FOR_LAMBDA) {
            adaptLambdaSignature(implFun as IrSimpleFunction, fakeInstanceMethod, signatureAdaptationConstraints)
        } else if (
            !checkMethodSignatureCompliance(implFun, fakeInstanceMethod, signatureAdaptationConstraints, reference)
        ) {
            return null
        }

        val newReference =
            if (implFun.origin == IrDeclarationOrigin.LOCAL_FUNCTION_FOR_LAMBDA)
                remapExtensionLambda(implFun as IrSimpleFunction, reference)
            else
                reference

        if (samMethod.isFakeOverride && nonFakeOverriddenFuns.size == 1) {
            return LambdaMetafactoryArguments(nonFakeOverriddenFuns.single(), fakeInstanceMethod, newReference, listOf())
        }
        return LambdaMetafactoryArguments(samMethod, fakeInstanceMethod, newReference, nonFakeOverriddenFuns)
    }

    private fun checkMethodSignatureCompliance(
        implFun: IrFunction,
        fakeInstanceMethod: IrSimpleFunction,
        constraints: SignatureAdaptationConstraints,
        reference: IrFunctionReference
    ): Boolean {
        val implParameters = collectValueParameters(
            implFun,
            withDispatchReceiver = reference.dispatchReceiver == null,
            withExtensionReceiver = reference.extensionReceiver == null
        )
        val methodParameters = collectValueParameters(fakeInstanceMethod)
        if (implParameters.size != methodParameters.size)
            throw AssertionError(
                "Mismatching lambda and instance method parameters:\n" +
                        "implFun: ${implFun.render()}\n" +
                        "  (${implParameters.size} parameters)\n" +
                        "instance method: ${fakeInstanceMethod.render()}\n" +
                        "  (${methodParameters.size} parameters)"
            )
        for ((implParameter, methodParameter) in implParameters.zip(methodParameters)) {
            val constraint = constraints.valueParameters[methodParameter]
            if (!checkTypeCompliesWithConstraint(implParameter.type, constraint))
                return false
        }
        if (!checkTypeCompliesWithConstraint(implFun.returnType, constraints.returnType))
            return false
        if (implFun.returnType.isUnit() && !fakeInstanceMethod.returnType.isUnit())
            return false
        return true
    }

    private fun checkTypeCompliesWithConstraint(irType: IrType, constraint: TypeAdaptationConstraint?): Boolean =
        when (constraint) {
            null -> true
            TypeAdaptationConstraint.FORCE_BOXING -> irType.isNullable()
            TypeAdaptationConstraint.KEEP_UNBOXED -> !irType.isNullable()
            TypeAdaptationConstraint.BOX_PRIMITIVE -> irType.getPrimitiveType() != null
            TypeAdaptationConstraint.CONFLICT -> false
        }

    private fun adaptLambdaSignature(
        implFun: IrSimpleFunction,
        fakeInstanceMethod: IrSimpleFunction,
        constraints: SignatureAdaptationConstraints
    ) {
        if (implFun.origin != IrDeclarationOrigin.LOCAL_FUNCTION_FOR_LAMBDA) {
            throw AssertionError("Can't adapt non-lambda function signature: ${implFun.render()}")
        }

        val implParameters = collectValueParameters(implFun)
        val methodParameters = collectValueParameters(fakeInstanceMethod)
        if (implParameters.size != methodParameters.size)
            throw AssertionError(
                "Mismatching lambda and instance method parameters:\n" +
                        "implFun: ${implFun.render()}\n" +
                        "  (${implParameters.size} parameters)\n" +
                        "instance method: ${fakeInstanceMethod.render()}\n" +
                        "  (${methodParameters.size} parameters)"
            )
        for ((implParameter, methodParameter) in implParameters.zip(methodParameters)) {
            val parameterConstraint = constraints.valueParameters[methodParameter]
            if (parameterConstraint.requiresImplLambdaBoxing()) {
                implParameter.type = implParameter.type.makeNullable()
            }
        }
        if (constraints.returnType.requiresImplLambdaBoxing() ||
            implFun.returnType.isUnit() && !fakeInstanceMethod.returnType.isUnit()
        ) {
            implFun.returnType = implFun.returnType.makeNullable()
        }
    }

    private fun remapExtensionLambda(lambda: IrSimpleFunction, reference: IrFunctionReference): IrFunctionReference {
        val oldExtensionReceiver = lambda.extensionReceiverParameter
            ?: return reference

        val newValueParameters = ArrayList<IrValueParameter>()
        val oldToNew = HashMap<IrValueParameter, IrValueParameter>()
        var newParameterIndex = 0

        newValueParameters.add(
            oldExtensionReceiver.copy(lambda, newParameterIndex++, Name.identifier("\$receiver")).also {
                oldToNew[oldExtensionReceiver] = it
            }
        )

        lambda.valueParameters.mapTo(newValueParameters) { oldParameter ->
            oldParameter.copy(lambda, newParameterIndex++).also {
                oldToNew[oldParameter] = it
            }
        }

        lambda.body?.transformChildrenVoid(VariableRemapper(oldToNew))

        lambda.extensionReceiverParameter = null
        lambda.valueParameters = newValueParameters

        return IrFunctionReferenceImpl(
            reference.startOffset, reference.endOffset, reference.type,
            lambda.symbol,
            typeArgumentsCount = 0,
            valueArgumentsCount = newValueParameters.size,
            reflectionTarget = null,
            origin = reference.origin
        )
    }


    private fun IrValueParameter.copy(parent: IrSimpleFunction, newIndex: Int, newName: Name = this.name): IrValueParameter =
        buildValueParameter(parent) {
            updateFrom(this@copy)
            index = newIndex
            name = newName
        }

    private fun adaptFakeInstanceMethodSignature(fakeInstanceMethod: IrSimpleFunction, constraints: SignatureAdaptationConstraints) {
        for ((valueParameter, constraint) in constraints.valueParameters) {
            if (valueParameter.parent != fakeInstanceMethod)
                throw AssertionError(
                    "Unexpected value parameter: ${valueParameter.render()}; fakeInstanceMethod:\n" +
                            fakeInstanceMethod.dump()
                )
            if (constraint.requiresInstanceMethodBoxing()) {
                valueParameter.type = valueParameter.type.makeNullable()
            }
        }
        if (constraints.returnType.requiresInstanceMethodBoxing()) {
            fakeInstanceMethod.returnType = fakeInstanceMethod.returnType.makeNullable()
        }
    }

    private enum class TypeAdaptationConstraint {
        FORCE_BOXING,
        BOX_PRIMITIVE,
        KEEP_UNBOXED,
        CONFLICT
    }

    private fun TypeAdaptationConstraint?.requiresInstanceMethodBoxing() =
        this == TypeAdaptationConstraint.FORCE_BOXING || this == TypeAdaptationConstraint.BOX_PRIMITIVE

    private fun TypeAdaptationConstraint?.requiresImplLambdaBoxing() =
        this == TypeAdaptationConstraint.FORCE_BOXING

    private class SignatureAdaptationConstraints(
        val valueParameters: Map<IrValueParameter, TypeAdaptationConstraint>,
        val returnType: TypeAdaptationConstraint?
    ) {
        fun hasConflicts() =
            returnType == TypeAdaptationConstraint.CONFLICT ||
                    TypeAdaptationConstraint.CONFLICT in valueParameters.values
    }

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
        if (adapteeType.isPrimitiveType()) {
            return if (
                expectedType.isPrimitiveType() &&
                !expectedType.hasAnnotation(JvmAnnotationNames.ENHANCED_NULLABILITY_ANNOTATION)
            )
                TypeAdaptationConstraint.KEEP_UNBOXED
            else
                TypeAdaptationConstraint.BOX_PRIMITIVE
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
            if (!underlyingAdapteeType.hasQuestionMark && !underlyingAdapteeType.isPrimitiveType()) {
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

    fun collectValueParameters(
        irFun: IrFunction,
        withDispatchReceiver: Boolean = false,
        withExtensionReceiver: Boolean = true
    ): List<IrValueParameter> {
        if ((!withDispatchReceiver || irFun.dispatchReceiverParameter == null) &&
            (!withExtensionReceiver || irFun.extensionReceiverParameter == null)
        )
            return irFun.valueParameters
        return ArrayList<IrValueParameter>().apply {
            if (withDispatchReceiver) {
                addIfNotNull(irFun.dispatchReceiverParameter)
            }
            if (withExtensionReceiver) {
                addIfNotNull(irFun.extensionReceiverParameter)
            }
            addAll(irFun.valueParameters)
        }
    }
}
