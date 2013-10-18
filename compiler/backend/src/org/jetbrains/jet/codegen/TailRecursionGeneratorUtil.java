/*
 * Copyright 2010-2013 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.jet.codegen;

import com.google.common.collect.Lists;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.asm4.Type;
import org.jetbrains.asm4.commons.InstructionAdapter;
import org.jetbrains.jet.codegen.context.MethodContext;
import org.jetbrains.jet.codegen.state.GenerationState;
import org.jetbrains.jet.lang.descriptors.CallableDescriptor;
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor;
import org.jetbrains.jet.lang.descriptors.MemberDescriptor;
import org.jetbrains.jet.lang.descriptors.ValueParameterDescriptor;
import org.jetbrains.jet.lang.psi.JetCallExpression;
import org.jetbrains.jet.lang.psi.JetExpression;
import org.jetbrains.jet.lang.psi.JetSimpleNameExpression;
import org.jetbrains.jet.lang.psi.ValueArgument;
import org.jetbrains.jet.lang.resolve.calls.model.*;
import org.jetbrains.jet.lang.types.JetType;

import java.util.ArrayList;
import java.util.List;

import static org.jetbrains.asm4.Opcodes.ACC_PRIVATE;
import static org.jetbrains.jet.codegen.AsmUtil.pushDefaultValueOnStack;
import static org.jetbrains.jet.codegen.CodegenUtil.isNullableType;
import static org.jetbrains.jet.lang.resolve.BindingContext.TAIL_RECURSION_CALL;

public class TailRecursionGeneratorUtil {

    @NotNull
    private final MethodContext context;
    @NotNull
    private final ExpressionCodegen codegen;
    @NotNull
    private final InstructionAdapter v;
    @NotNull
    private final GenerationState state;

    public TailRecursionGeneratorUtil(
            @NotNull MethodContext context,
            @NotNull ExpressionCodegen codegen,
            @NotNull InstructionAdapter v,
            @NotNull GenerationState state
    ) {
        this.context = context;
        this.codegen = codegen;
        this.v = v;
        this.state = state;
    }

    public boolean isTailRecursion(@NotNull JetCallExpression expression) {
        return state.getBindingContext().get(TAIL_RECURSION_CALL, expression) == Boolean.TRUE;
    }

    public StackValue generateTailRecursion(ResolvedCall<? extends CallableDescriptor> resolvedCall, JetCallExpression callExpression) {
        CallableDescriptor fd = resolvedCall.getResultingDescriptor();
        assert fd instanceof FunctionDescriptor;
        CallableMethod callable = (CallableMethod) codegen.resolveToCallable((FunctionDescriptor) fd, false);
        List<Type> types = callable.getValueParameterTypes();
        List<ValueParameterDescriptor> parametersStored = prepareParameterValuesOnStack(fd, types, resolvedCall.getValueArgumentsByIndex());

        boolean generateNullChecks = AsmUtil.getVisibilityAccessFlag((MemberDescriptor) fd) != ACC_PRIVATE;
        // we can't store values to the variables in the loop above because it will affect expressions evaluation
        for (ValueParameterDescriptor parameterDescriptor : Lists.reverse(parametersStored)) {
            JetType type = parameterDescriptor.getReturnType();
            Type asmType = types.get(parameterDescriptor.getIndex());
            int index = getParameterVariableIndex(parameterDescriptor, callExpression);

            if (generateNullChecks) {
                generateNullCheckIfNeeded(parameterDescriptor, type, asmType);
            }

            v.store(index, asmType);
        }

        v.goTo(context.getMethodStartLabel());

        return StackValue.none();
    }

    private List<ValueParameterDescriptor> prepareParameterValuesOnStack(
            CallableDescriptor fd,
            List<Type> types,
            List<ResolvedValueArgument> valueArguments
    ) {
        List<ValueParameterDescriptor> descriptorsStored = new ArrayList<ValueParameterDescriptor>(valueArguments.size());
        for (ValueParameterDescriptor parameterDescriptor : fd.getValueParameters()) {
            ResolvedValueArgument arg = valueArguments.get(parameterDescriptor.getIndex());
            Type type = types.get(parameterDescriptor.getIndex());

            if (arg instanceof ExpressionValueArgument) {
                ExpressionValueArgument ev = (ExpressionValueArgument) arg;
                ValueArgument argument = ev.getValueArgument();
                JetExpression argumentExpression = argument == null ? null : argument.getArgumentExpression();

                if (argumentExpression instanceof JetSimpleNameExpression) {
                    JetSimpleNameExpression nameExpression = (JetSimpleNameExpression) argumentExpression;
                    if (nameExpression.getReferencedNameAsName().equals(parameterDescriptor.getName())) {
                        // do nothing: we shouldn't store argument to itself again
                        continue;
                    }
                }

                codegen.gen(argumentExpression, type);
            }
            else if (arg instanceof DefaultValueArgument) { // what case is it?
                pushDefaultValueOnStack(type, v);
            }
            else if (arg instanceof VarargValueArgument) {
                VarargValueArgument valueArgument = (VarargValueArgument) arg;
                codegen.genVarargs(parameterDescriptor, valueArgument);
            }
            else {
                throw new UnsupportedOperationException();
            }

            descriptorsStored.add(parameterDescriptor);
        }
        return descriptorsStored;
    }

    private void generateNullCheckIfNeeded(ValueParameterDescriptor parameterDescriptor, JetType type, Type asmType) {
        if (type != null && !isNullableType(type)) { // we probably may try to analyze the expression has been stored to drop few more null checks
            if (asmType.getSort() == Type.OBJECT || asmType.getSort() == Type.ARRAY) {
                v.dup();
                v.visitLdcInsn(parameterDescriptor.getName().asString());
                v.invokestatic("jet/runtime/Intrinsics", "checkParameterIsNotNull", "(Ljava/lang/Object;Ljava/lang/String;)V");
            }
        }
    }

    private int getParameterVariableIndex(ValueParameterDescriptor parameterDescriptor, PsiElement node) {
        int index = codegen.lookupLocalIndex(parameterDescriptor);
        if (index == -1) {
            index = codegen.lookupLocalIndex(parameterDescriptor.getOriginal());
        }

        if (index == -1) {
            throw new CompilationException("Failed to obtain parameter index: " + parameterDescriptor.getName(), null, node);
        }

        return index;
    }
}
