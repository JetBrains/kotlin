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
import org.jetbrains.annotations.Nullable;
import org.jetbrains.asm4.Type;
import org.jetbrains.asm4.commons.InstructionAdapter;
import org.jetbrains.jet.codegen.context.MethodContext;
import org.jetbrains.jet.codegen.state.GenerationState;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.calls.model.*;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;

import java.util.*;

import static org.jetbrains.asm4.Opcodes.ACC_PRIVATE;
import static org.jetbrains.jet.codegen.AsmUtil.pushDefaultValueOnStack;
import static org.jetbrains.jet.codegen.CodegenUtil.isNullableType;
import static org.jetbrains.jet.lang.resolve.BindingContext.RESOLVED_CALL;
import static org.jetbrains.jet.lang.resolve.BindingContext.TAIL_RECURSION_CALL;

public class TailRecursionGeneratorUtil {

    private static final boolean IGNORE_ANNOTATION_ABSENCE = false;

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
        return isRecursion(expression) && isTailRecursiveCall(expression);
    }

    public static boolean hasTailRecursiveAnnotation(DeclarationDescriptor descriptor) {
        if (IGNORE_ANNOTATION_ABSENCE) {
            return true;
        }

        ClassDescriptor tailRecursive = KotlinBuiltIns.getInstance().getBuiltInClassByName(FqName.fromSegments(Arrays.asList("jet", "TailRecursive")).shortName());
        for (AnnotationDescriptor annotation : descriptor.getOriginal().getAnnotations()) {
            ClassifierDescriptor declarationDescriptor = annotation.getType().getConstructor().getDeclarationDescriptor();
            if (declarationDescriptor != null && declarationDescriptor.equals(tailRecursive)) {
                return true;
            }
        }

        return false;
    }

    @NotNull
    public static List<JetCallExpression> findRecursiveCalls(@NotNull DeclarationDescriptor descriptor, @NotNull GenerationState state) {
        List<JetCallExpression> tailRecursionsFound = new ArrayList<JetCallExpression>();
        Collection<JetCallExpression> calls = state.getBindingTrace().getKeys(BindingContext.TAIL_RECURSION_CALL);
        for (JetCallExpression callExpression : calls) {
            ResolvedCall<? extends CallableDescriptor> resolvedCall = resolve(callExpression, state.getBindingContext());
            if (resolvedCall != null) {
                if (resolvedCall.getCandidateDescriptor().equals(descriptor)) {
                    tailRecursionsFound.add(callExpression);
                }
            }
        }

        return tailRecursionsFound;
    }

    @Nullable
    private static ResolvedCall<? extends CallableDescriptor> resolve(@NotNull JetCallExpression callExpression, BindingContext context) {
        JetExpression callee = callExpression.getCalleeExpression();
        return callee == null ? null : context.get(RESOLVED_CALL, callee);
    }

    private boolean isRecursion(@NotNull JetCallExpression callExpression) {
        ResolvedCall<? extends CallableDescriptor> resolvedCall = resolve(callExpression, state.getBindingContext());
        if (resolvedCall != null && context.getContextDescriptor().equals(resolvedCall.getCandidateDescriptor())) {
            return true;
        }

        return false;
    }

    private static boolean isTailRecursiveCall(@NotNull JetCallExpression expression) {
        return traceToRoot(expression, new TailRecursionDetectorVisitor(), true);
    }

    private static boolean isFunctionElement(PsiElement element) {
        return element instanceof JetFunction;
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


        state.getBindingTrace().record(TAIL_RECURSION_CALL, callExpression);

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
            } else if (arg instanceof DefaultValueArgument) { // what case is it?
                pushDefaultValueOnStack(type, v);
            } else if (arg instanceof VarargValueArgument) {
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


    public static class TraceStatus<T> {
        @NotNull
        private final T data;
        private final boolean abortTrace;

        public TraceStatus(@NotNull T data, boolean abortTrace) {
            this.data = data;
            this.abortTrace = abortTrace;
        }

        @NotNull
        public T getData() {
            return data;
        }

        public boolean isAbortTrace() {
            return abortTrace;
        }
    }

    @NotNull
    private static <T> T traceToRoot(@NotNull PsiElement element, @NotNull JetVisitor<TraceStatus<T>, List<? extends PsiElement>> visitor, T def) {
        ArrayList<PsiElement> track = new ArrayList<PsiElement>();
        List<PsiElement> view = Collections.unmodifiableList(track);
        @NotNull
        TraceStatus<T> lastStatus = new TraceStatus<T>(def, true);

        do {
            track.add(element);
            PsiElement parent = element.getParent();
            if (parent instanceof JetElement) {
                JetElement jet = (JetElement) parent;
                TraceStatus<T> status = jet.accept(visitor, view);
                if (status == null) {
                    throw new IllegalStateException("visitor has returned null status");
                }
                if (status.isAbortTrace()) {
                    return status.getData();
                }
                lastStatus = status;
            }

            element = parent;
        } while (element != null && !isFunctionElement(element));

        return lastStatus.getData();
    }
}
