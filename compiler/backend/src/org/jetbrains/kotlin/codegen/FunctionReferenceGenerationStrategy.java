/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen;

import kotlin.collections.CollectionsKt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.backend.common.CodegenUtil;
import org.jetbrains.kotlin.codegen.coroutines.CoroutineCodegenUtilKt;
import org.jetbrains.kotlin.codegen.state.GenerationState;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.resolve.BindingContext;
import org.jetbrains.kotlin.resolve.calls.components.ArgumentsUtilsKt;
import org.jetbrains.kotlin.resolve.calls.model.*;
import org.jetbrains.kotlin.resolve.calls.util.CallMaker;
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodSignature;
import org.jetbrains.kotlin.resolve.scopes.receivers.ExpressionReceiver;
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue;
import org.jetbrains.kotlin.types.KotlinType;
import org.jetbrains.org.objectweb.asm.Type;
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/*
 * Notice the difference between two function descriptors in this class.
 * - [referencedFunction] is the function declaration which is referenced by the "::" expression. This is a real function present in code.
 * - [functionDescriptor] is a synthetically created function which has the same signature as the "invoke" of the generated callable
 *   reference subclass. Its parameters include dispatch/extension receiver parameters of the referenced function, and those value
 *   parameters of the referenced function which are required by the expected function type where the callable reference is passed to.
 *   In simple cases, these value parameters are all of the referenced function's value parameters. But in cases when the referenced
 *   function has parameters with default values, or a vararg parameter, functionDescriptor can take fewer parameters than
 *   referencedFunction if the expected function type takes fewer parameters as well. For example:
 *
 * fun foo(a: A, b: B = ..., c: C = ..., vararg d: D) {}
 *
 * fun bar(f: (A, B) -> Unit) {}
 *
 * // referencedFunction: foo(A, B, C, vararg D)
 * // functionDescriptor: invoke(A, B)
 * bar(::foo)
 */
public class FunctionReferenceGenerationStrategy extends FunctionGenerationStrategy.CodegenBased {
    private final ResolvedCall<?> resolvedCall;
    private final FunctionDescriptor referencedFunction;
    private final FunctionDescriptor functionDescriptor;
    private final Type receiverType; // non-null for bound references
    private final KotlinType receiverKotlinType;
    private final StackValue receiverValue;
    private final boolean isInliningStrategy;

    public FunctionReferenceGenerationStrategy(
            @NotNull GenerationState state,
            @NotNull FunctionDescriptor functionDescriptor,
            @NotNull ResolvedCall<?> resolvedCall,
            @Nullable JvmKotlinType receiverJvmKotlinType,
            @Nullable StackValue receiverValue,
            boolean isInliningStrategy
    ) {
        super(state);
        this.resolvedCall = resolvedCall;
        FunctionDescriptor referencedFunction = (FunctionDescriptor) resolvedCall.getResultingDescriptor();
        if (referencedFunction.isSuspend()) {
            this.referencedFunction = CoroutineCodegenUtilKt.getOrCreateJvmSuspendFunctionView(referencedFunction, state);
            this.functionDescriptor = CoroutineCodegenUtilKt.getOrCreateJvmSuspendFunctionView(functionDescriptor, state);
        }
        else {
            this.referencedFunction = referencedFunction;
            this.functionDescriptor = functionDescriptor;
        }
        this.receiverType = receiverJvmKotlinType != null ? receiverJvmKotlinType.getType() : null;
        this.receiverKotlinType = receiverJvmKotlinType != null ? receiverJvmKotlinType.getKotlinType() : null;
        this.receiverValue = receiverValue;
        this.isInliningStrategy = isInliningStrategy;
        assert receiverType != null || receiverValue == null
                : "A receiver value is provided for unbound function reference. Either this is a bound reference and you forgot " +
                  "to pass receiverType, or you accidentally passed some receiverValue for a reference without receiver";
    }

    @Override
    public void doGenerateBody(@NotNull ExpressionCodegen codegen, @NotNull JvmMethodSignature signature) {
        /*
         Here we need to put the arguments from our locals to the stack and invoke the referenced method. Since invocation
         of methods is highly dependent on expressions, we create a fake call expression. Then we create a new instance of
         ExpressionCodegen and, in order for it to generate code correctly, we save to its 'tempVariables' field every
         argument of our fake expression, pointing it to the corresponding index in our locals. This way generation of
         every argument boils down to calling LOAD with the corresponding index
         */

        int receivers = CallableReferenceUtilKt.computeExpectedNumberOfReceivers(referencedFunction, receiverType != null);
        int fakeArgCount = functionDescriptor.getValueParameters().size() - receivers;
        KtCallExpression fakeExpression = CodegenUtil.constructFakeFunctionCall(state.getProject(), fakeArgCount);
        List<? extends ValueArgument> fakeArguments = fakeExpression.getValueArguments();

        ReceiverValue dispatchReceiver = computeAndSaveReceiver(signature, codegen, referencedFunction.getDispatchReceiverParameter());
        ReceiverValue extensionReceiver = computeAndSaveReceiver(signature, codegen, referencedFunction.getExtensionReceiverParameter());
        computeAndSaveArguments(fakeArguments, codegen, receivers);

        ResolvedCall<CallableDescriptor> fakeResolvedCall = new DelegatingResolvedCall<CallableDescriptor>(resolvedCall) {

            private final Map<ValueParameterDescriptor, ResolvedValueArgument> argumentMap = new LinkedHashMap<>();
            {
                int i = 0;

                for (ValueParameterDescriptor parameter : referencedFunction.getValueParameters()) {
                    if (parameter.getVarargElementType() != null) {
                        // Two cases are possible for a function reference with a vararg parameter of type T: either several arguments
                        // of type T are bound to that parameter, or one argument of type Array<out T>. In the former case the argument
                        // is bound as a VarargValueArgument, in the latter it's an ExpressionValueArgument

                        if (i == fakeArgCount) {
                            // If we've exhausted the argument list of the reference and we still have one vararg parameter left,
                            // we should use its default value if present, or simply an empty vararg instead
                            argumentMap.put(
                                    parameter,
                                    ArgumentsUtilsKt.hasDefaultValue(parameter) ? DefaultValueArgument.DEFAULT : new VarargValueArgument()
                            );
                            continue;
                        }

                        if (functionDescriptor.getValueParameters().get(receivers + i).getType().equals(parameter.getVarargElementType())) {
                            argumentMap.put(parameter, new VarargValueArgument(fakeArguments.subList(i, fakeArgCount)));
                            i = fakeArgCount;
                            continue;
                        }
                    }

                    if (i < fakeArgCount) {
                        argumentMap.put(parameter, new ExpressionValueArgument(fakeArguments.get(i++)));
                    }
                    else {
                        assert ArgumentsUtilsKt.hasDefaultValue(parameter) :
                                "Parameter should be either vararg or expression or default: " + parameter +
                                " (reference in: " + functionDescriptor.getContainingDeclaration() + ")";
                        argumentMap.put(parameter, DefaultValueArgument.DEFAULT);
                    }
                }
            }

            @Nullable
            @Override
            public ReceiverValue getExtensionReceiver() {
                return extensionReceiver;
            }

            @Nullable
            @Override
            public ReceiverValue getDispatchReceiver() {
                return dispatchReceiver;
            }

            @NotNull
            @Override
            public List<ResolvedValueArgument> getValueArgumentsByIndex() {
                return new ArrayList<>(argumentMap.values());
            }

            @NotNull
            @Override
            public Map<ValueParameterDescriptor, ResolvedValueArgument> getValueArguments() {
                return argumentMap;
            }

            @NotNull
            @Override
            public CallableDescriptor getCandidateDescriptor() {
                return referencedFunction;
            }

            @NotNull
            @Override
            public CallableDescriptor getResultingDescriptor() {
                return referencedFunction;
            }
        };

        StackValue result;
        Type returnType = codegen.getReturnType();
        if (referencedFunction instanceof ConstructorDescriptor) {
            if (returnType.getSort() == Type.ARRAY) {
                //noinspection ConstantConditions
                result = codegen.generateNewArray(fakeExpression, referencedFunction.getReturnType(), fakeResolvedCall);
            }
            else {
                result = codegen.generateConstructorCall(fakeResolvedCall, returnType);
            }
        }
        else {
            Call call = CallMaker.makeCall(fakeExpression, null, null, fakeExpression, fakeArguments);
            result = codegen.invokeFunction(call, fakeResolvedCall, StackValue.none());
        }

        InstructionAdapter v = codegen.v;
        result.put(returnType, v);
        v.areturn(returnType);
    }

    private void computeAndSaveArguments(
            @NotNull List<? extends ValueArgument> fakeArguments, @NotNull ExpressionCodegen codegen, int receivers
    ) {
        List<ValueParameterDescriptor> valueParameters = CollectionsKt.drop(functionDescriptor.getValueParameters(), receivers);
        assert valueParameters.size() == fakeArguments.size()
                : functionDescriptor + ": " + valueParameters.size() + " != " + fakeArguments.size();
        for (int i = 0; i < valueParameters.size(); i++) {
            ValueParameterDescriptor parameter = valueParameters.get(i);
            ValueArgument fakeArgument = fakeArguments.get(i);

            Type type = state.getTypeMapper().mapType(parameter);
            int localIndex = codegen.myFrameMap.getIndex(parameter);
            if (localIndex > 0) {
                codegen.tempVariables.put(fakeArgument.getArgumentExpression(), StackValue.local(localIndex, type));
            }
            else {
                codegen.tempVariables.put(fakeArgument.getArgumentExpression(), StackValue.local(parameter.getIndex() + 1 + receivers, type));
            }
        }
    }

    @Nullable
    private ReceiverValue computeAndSaveReceiver(
            @NotNull JvmMethodSignature signature,
            @NotNull ExpressionCodegen codegen,
            @Nullable ReceiverParameterDescriptor receiver
    ) {
        if (receiver == null) return null;

        KtExpression receiverExpression = KtPsiFactoryKt.KtPsiFactory(state.getProject(), false).createExpression("callableReferenceFakeReceiver");
        codegen.tempVariables.put(receiverExpression, receiverParameterStackValue(signature, codegen));
        return ExpressionReceiver.Companion.create(receiverExpression, receiver.getType(), BindingContext.EMPTY);
    }

    @NotNull
    private StackValue receiverParameterStackValue(@NotNull JvmMethodSignature signature, @NotNull ExpressionCodegen codegen) {
        if (receiverValue != null) return receiverValue;

        if (receiverType != null) {
            ClassDescriptor classDescriptor = (ClassDescriptor) codegen.getContext().getParentContext().getContextDescriptor();
            Type asmType = codegen.getState().getTypeMapper().mapClass(classDescriptor);
            return CallableReferenceUtilKt.capturedBoundReferenceReceiver(asmType, receiverType, receiverKotlinType, isInliningStrategy);
        }

        // 0 is this (the callable reference class), 1 is the invoke() method's first parameter
        return StackValue.local(1, signature.getAsmMethod().getArgumentTypes()[0]);
    }
}
