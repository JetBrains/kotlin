/*
 * Copyright 2010-2012 JetBrains s.r.o.
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

import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.asm4.Label;
import org.jetbrains.asm4.MethodVisitor;
import org.jetbrains.asm4.Type;
import org.jetbrains.asm4.commons.InstructionAdapter;
import org.jetbrains.asm4.commons.Method;
import org.jetbrains.jet.codegen.context.CodegenContext;
import org.jetbrains.jet.codegen.context.MethodContext;
import org.jetbrains.jet.codegen.signature.JvmMethodParameterSignature;
import org.jetbrains.jet.codegen.signature.JvmMethodSignature;
import org.jetbrains.jet.codegen.signature.kotlin.JetMethodAnnotationWriter;
import org.jetbrains.jet.codegen.signature.kotlin.JetValueParameterAnnotationWriter;
import org.jetbrains.jet.codegen.state.GenerationState;
import org.jetbrains.jet.codegen.state.GenerationStateAware;
import org.jetbrains.jet.codegen.state.JetTypeMapperMode;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.psi.JetDeclarationWithBody;
import org.jetbrains.jet.lang.psi.JetFunctionLiteralExpression;
import org.jetbrains.jet.lang.psi.JetNamedFunction;
import org.jetbrains.jet.lang.psi.JetParameter;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.java.JvmAbi;
import org.jetbrains.jet.lang.resolve.java.JvmClassName;
import org.jetbrains.jet.lang.resolve.java.JvmStdlibNames;
import org.jetbrains.jet.lang.resolve.java.kt.DescriptorKindUtils;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverDescriptor;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;

import java.util.*;

import static org.jetbrains.asm4.Opcodes.*;
import static org.jetbrains.jet.codegen.AsmUtil.*;
import static org.jetbrains.jet.codegen.CodegenUtil.*;
import static org.jetbrains.jet.codegen.binding.CodegenBinding.isLocalFun;
import static org.jetbrains.jet.lang.resolve.BindingContextUtils.callableDescriptorToDeclaration;
import static org.jetbrains.jet.lang.resolve.BindingContextUtils.descriptorToDeclaration;
import static org.jetbrains.jet.lang.resolve.java.AsmTypeConstants.JAVA_ARRAY_GENERIC_TYPE;
import static org.jetbrains.jet.lang.resolve.java.AsmTypeConstants.OBJECT_TYPE;
import static org.jetbrains.jet.lang.resolve.java.AsmTypeConstants.getType;

/**
 * @author max
 * @author yole
 * @author alex.tkachman
 */
public class FunctionCodegen extends GenerationStateAware {
    private final CodegenContext owner;
    private final ClassBuilder v;

    public FunctionCodegen(CodegenContext owner, ClassBuilder v, GenerationState state) {
        super(state);
        this.owner = owner;
        this.v = v;
    }

    public void gen(JetNamedFunction f) {
        final SimpleFunctionDescriptor functionDescriptor = bindingContext.get(BindingContext.FUNCTION, f);
        assert functionDescriptor != null;
        JvmMethodSignature method =
                typeMapper.mapToCallableMethod(functionDescriptor, false, owner.getContextKind())
                        .getSignature();
        generateMethod(f, method, true, null, functionDescriptor);
    }

    public void generateMethod(
            JetDeclarationWithBody fun,
            JvmMethodSignature jvmSignature,
            boolean needJetAnnotations,
            @Nullable String propertyTypeSignature,
            FunctionDescriptor functionDescriptor
    ) {
        checkMustGenerateCode(functionDescriptor);

        OwnerKind kind = owner.getContextKind();
        if (!isStatic(kind) &&
                (kind instanceof OwnerKind.DelegateKind) != (functionDescriptor.getKind() == FunctionDescriptor.Kind.DELEGATION)) {
            throw new IllegalStateException("Mismatching kind in " + functionDescriptor + "; context kind: " + kind);
        }

        if (kind == OwnerKind.TRAIT_IMPL) {
            needJetAnnotations = false;
        }

        MethodContext context = owner.intoFunction(functionDescriptor);
        if (kind != OwnerKind.TRAIT_IMPL || fun.getBodyExpression() != null) {
            generateMethodHeaderAndBody(fun, jvmSignature, needJetAnnotations, propertyTypeSignature, functionDescriptor, context);

            if (state.getClassBuilderMode() == ClassBuilderMode.FULL && !isAbstract(functionDescriptor, kind)) {
                generateBridgeIfNeeded(owner, state, v, jvmSignature.getAsmMethod(), functionDescriptor, kind);
            }
        }

        generateDefaultIfNeeded(context, state, v, jvmSignature.getAsmMethod(), functionDescriptor, kind);
    }

    private void generateMethodHeaderAndBody(
            @NotNull JetDeclarationWithBody fun,
            @NotNull JvmMethodSignature jvmSignature,
            boolean needJetAnnotations,
            @Nullable String propertyTypeSignature,
            @NotNull FunctionDescriptor functionDescriptor,
            @NotNull MethodContext context
    ) {
        OwnerKind kind = context.getContextKind();
        Method asmMethod = jvmSignature.getAsmMethod();

        MethodVisitor mv = v.newMethod(fun,
                                       getMethodAsmFlags(functionDescriptor, kind),
                                       asmMethod.getName(),
                                       asmMethod.getDescriptor(),
                                       jvmSignature.getGenericsSignature(),
                                       null);

        AnnotationCodegen.forMethod(mv, typeMapper).genAnnotations(functionDescriptor);
        if (state.getClassBuilderMode() == ClassBuilderMode.SIGNATURES) return;

        if (needJetAnnotations) {
            genJetAnnotations(state, functionDescriptor, jvmSignature, propertyTypeSignature, mv);
        }

        if (isAbstract(functionDescriptor, kind)) return;

        if (state.getClassBuilderMode() == ClassBuilderMode.STUBS) {
            genStubCode(mv);
            return;
        }

        LocalVariablesInfo localVariablesInfo = new LocalVariablesInfo();
        for (ValueParameterDescriptor parameter : functionDescriptor.getValueParameters()) {
            localVariablesInfo.names.add(parameter.getName().getName());
        }

        MethodBounds methodBounds = generateMethodBody(mv, fun, functionDescriptor, context, asmMethod, localVariablesInfo);

        Type thisType;
        ReceiverDescriptor expectedThisObject = functionDescriptor.getExpectedThisObject();
        if (expectedThisObject.exists()) {
            thisType = typeMapper.mapType(expectedThisObject.getType());
        }
        else if (fun instanceof JetFunctionLiteralExpression || isLocalFun(bindingContext, functionDescriptor)) {
            thisType = typeMapper.mapType(context.getThisDescriptor());
        }
        else {
            thisType = null;
        }

        generateLocalVariableTable(mv, functionDescriptor, thisType, localVariablesInfo, methodBounds);

        endVisit(mv, null, fun);
    }

    @NotNull
    private MethodBounds generateMethodBody(
            @NotNull MethodVisitor mv,
            @NotNull JetDeclarationWithBody fun,
            @NotNull FunctionDescriptor functionDescriptor,
            @NotNull MethodContext context,
            @NotNull Method asmMethod,
            @NotNull LocalVariablesInfo localVariablesInfo
    ) {
        mv.visitCode();

        Label methodBegin = new Label();
        mv.visitLabel(methodBegin);

        OwnerKind kind = context.getContextKind();
        if (kind instanceof OwnerKind.StaticDelegateKind) {
            generateStaticDelegateMethodBody(mv, asmMethod, (OwnerKind.StaticDelegateKind) kind);
        }
        else if (kind instanceof OwnerKind.DelegateKind) {
            generateDelegateMethodBody(mv, asmMethod, (OwnerKind.DelegateKind) kind);
        }
        else {
            FrameMap frameMap = context.prepareFrame(typeMapper);

            int add = 0;
            if (kind == OwnerKind.TRAIT_IMPL) {
                add++;
            }

            if (functionDescriptor.getReceiverParameter().exists()) {
                add++;
            }

            Type[] argTypes = asmMethod.getArgumentTypes();
            List<ValueParameterDescriptor> parameters = functionDescriptor.getValueParameters();
            for (int i = 0; i < parameters.size(); i++) {
                frameMap.enter(parameters.get(i), argTypes[i + add]);
            }

            createSharedVarsForParameters(mv, functionDescriptor, frameMap, localVariablesInfo);

            genNotNullAssertionsForParameters(new InstructionAdapter(mv), state, functionDescriptor, frameMap);

            ExpressionCodegen codegen = new ExpressionCodegen(mv, frameMap, asmMethod.getReturnType(), context, state);
            codegen.returnExpression(fun.getBodyExpression());

            localVariablesInfo.names.addAll(codegen.getLocalVariableNamesForExpression());
        }

        Label methodEnd = new Label();
        mv.visitLabel(methodEnd);

        return new MethodBounds(methodBegin, methodEnd);
    }

    private static class MethodBounds {
        @NotNull private final Label begin;

        @NotNull private final Label end;

        private MethodBounds(@NotNull Label begin, @NotNull Label end) {
            this.begin = begin;
            this.end = end;
        }
    }

    private static class LocalVariablesInfo {
        @NotNull private final Collection<String> names = new HashSet<String>();

        @NotNull private final Map<Name, Label> labelsForSharedVars = new HashMap<Name, Label>();
    }

    private void generateLocalVariableTable(
            @NotNull MethodVisitor mv,
            @NotNull FunctionDescriptor functionDescriptor,
            @Nullable Type thisType,
            @NotNull LocalVariablesInfo localVariablesInfo,
            @NotNull MethodBounds methodBounds
    ) {
        // TODO: specify signatures

        Label methodBegin = methodBounds.begin;
        Label methodEnd = methodBounds.end;

        int k = 0;

        if (thisType != null) {
            mv.visitLocalVariable("this", thisType.getDescriptor(), null, methodBegin, methodEnd, k++);
        }

        if (functionDescriptor.getReceiverParameter().exists()) {
            Type type = typeMapper.mapType(functionDescriptor.getReceiverParameter().getType());
            mv.visitLocalVariable(JvmAbi.RECEIVER_PARAMETER, type.getDescriptor(), null, methodBegin, methodEnd, k);
            k += type.getSize();
        }

        for (ValueParameterDescriptor parameter : functionDescriptor.getValueParameters()) {
            Type type = typeMapper.mapType(parameter);

            Label divideLabel = localVariablesInfo.labelsForSharedVars.get(parameter.getName());
            String parameterName = parameter.getName().getName();
            if (divideLabel != null) {
                mv.visitLocalVariable(parameterName, type.getDescriptor(), null, methodBegin, divideLabel, k);

                String nameForSharedVar = createTmpVariableName(localVariablesInfo.names);
                localVariablesInfo.names.add(nameForSharedVar);

                Type sharedVarType = typeMapper.getSharedVarType(parameter);
                mv.visitLocalVariable(nameForSharedVar, sharedVarType.getDescriptor(), null, divideLabel, methodEnd, k);
                k += Math.max(type.getSize(), sharedVarType.getSize());
            }
            else {
                mv.visitLocalVariable(parameterName, type.getDescriptor(), null, methodBegin, methodEnd, k);
                k += type.getSize();
            }
        }
    }

    private void createSharedVarsForParameters(
            @NotNull MethodVisitor mv,
            @NotNull FunctionDescriptor functionDescriptor,
            @NotNull FrameMap frameMap,
            @NotNull LocalVariablesInfo localVariablesInfo
    ) {
        for (ValueParameterDescriptor parameter : functionDescriptor.getValueParameters()) {
            Type sharedVarType = typeMapper.getSharedVarType(parameter);
            if (sharedVarType == null) {
                continue;
            }

            Type localVarType = typeMapper.mapType(parameter);
            int index = frameMap.getIndex(parameter);
            mv.visitTypeInsn(NEW, sharedVarType.getInternalName());
            mv.visitInsn(DUP);
            mv.visitInsn(DUP);
            mv.visitMethodInsn(INVOKESPECIAL, sharedVarType.getInternalName(), "<init>", "()V");
            mv.visitVarInsn(localVarType.getOpcode(ILOAD), index);
            mv.visitFieldInsn(PUTFIELD, sharedVarType.getInternalName(), "ref", StackValue.refType(localVarType).getDescriptor());

            Label labelForSharedVar = new Label();
            mv.visitLabel(labelForSharedVar);
            localVariablesInfo.labelsForSharedVars.put(parameter.getName(), labelForSharedVar);

            mv.visitVarInsn(sharedVarType.getOpcode(ISTORE), index);
        }
    }

    private void generateDelegateMethodBody(
            @NotNull MethodVisitor mv,
            @NotNull Method asmMethod,
            @NotNull OwnerKind.DelegateKind dk
    ) {
        InstructionAdapter iv = new InstructionAdapter(mv);
        Type[] argTypes = asmMethod.getArgumentTypes();

        iv.load(0, OBJECT_TYPE);
        dk.getDelegate().put(OBJECT_TYPE, iv);
        for (int i = 0; i < argTypes.length; i++) {
            Type argType = argTypes[i];
            iv.load(i + 1, argType);
        }
        iv.invokeinterface(dk.getOwnerClass(), asmMethod.getName(), asmMethod.getDescriptor());
        iv.areturn(asmMethod.getReturnType());
    }

    private void generateStaticDelegateMethodBody(
            @NotNull MethodVisitor mv,
            @NotNull Method asmMethod,
            @NotNull OwnerKind.StaticDelegateKind dk
    ) {
        InstructionAdapter iv = new InstructionAdapter(mv);
        Type[] argTypes = asmMethod.getArgumentTypes();

        // The first line of some namespace file is written to the line number attribute of a static delegate to allow to 'step into' it
        // This is similar to what javac does with bridge methods
        Label label = new Label();
        iv.visitLabel(label);
        iv.visitLineNumber(1, label);

        int k = 0;
        for (Type argType : argTypes) {
            iv.load(k, argType);
            k += argType.getSize();
        }
        iv.invokestatic(dk.getOwnerClass(), asmMethod.getName(), asmMethod.getDescriptor());
        iv.areturn(asmMethod.getReturnType());
    }

    private static boolean isAbstract(FunctionDescriptor functionDescriptor, OwnerKind kind) {
        return (functionDescriptor.getModality() == Modality.ABSTRACT
                 || isInterface(functionDescriptor.getContainingDeclaration())
               )
               && !isStatic(kind)
               && kind != OwnerKind.TRAIT_IMPL;
    }

    public static int getMethodAsmFlags(FunctionDescriptor functionDescriptor, OwnerKind kind) {
        boolean isStatic = isStatic(kind);
        boolean isAbstract = isAbstract(functionDescriptor, kind);

        int flags = getVisibilityAccessFlag(functionDescriptor);

        if (!functionDescriptor.getValueParameters().isEmpty()
            && functionDescriptor.getValueParameters().get(functionDescriptor.getValueParameters().size() - 1)
                       .getVarargElementType() != null) {
            flags |= ACC_VARARGS;
        }

        if (functionDescriptor.getModality() == Modality.FINAL) {
            DeclarationDescriptor containingDeclaration = functionDescriptor.getContainingDeclaration();
            if (!(containingDeclaration instanceof ClassDescriptor) ||
                ((ClassDescriptor) containingDeclaration).getKind() != ClassKind.TRAIT) {
                flags |= ACC_FINAL;
            }
        }

        if (isStatic || kind == OwnerKind.TRAIT_IMPL) {
            flags |= ACC_STATIC;
        }

        if (isAbstract) flags |= ACC_ABSTRACT;

        if (KotlinBuiltIns.getInstance().isDeprecated(functionDescriptor)) {
            flags |= ACC_DEPRECATED;
        }
        return flags;
    }

    private static boolean isStatic(OwnerKind kind) {
        return kind == OwnerKind.NAMESPACE || kind instanceof OwnerKind.StaticDelegateKind;
    }

    public static void genJetAnnotations(
            @NotNull GenerationState state,
            @NotNull FunctionDescriptor functionDescriptor,
            @Nullable JvmMethodSignature jvmSignature,
            @Nullable String propertyTypeSignature,
            MethodVisitor mv
    ) {
        if (jvmSignature == null) {
            jvmSignature = state.getTypeMapper().mapToCallableMethod(functionDescriptor, false, OwnerKind.IMPLEMENTATION).getSignature();
        }

        List<ValueParameterDescriptor> paramDescrs = functionDescriptor.getValueParameters();
        Modality modality = functionDescriptor.getModality();
        ReceiverDescriptor receiverParameter = functionDescriptor.getReceiverParameter();

        int start = 0;
        if (functionDescriptor instanceof PropertyAccessorDescriptor) {
            assert propertyTypeSignature != null;
            PropertyCodegen.generateJetPropertyAnnotation(mv, propertyTypeSignature, jvmSignature.getKotlinTypeParameter(),
                                                          ((PropertyAccessorDescriptor) functionDescriptor)
                                                                  .getCorrespondingProperty(),
                                                          functionDescriptor.getVisibility());
        }
        else if (functionDescriptor instanceof SimpleFunctionDescriptor) {
            if (propertyTypeSignature != null) {
                throw new IllegalStateException();
            }
            JetMethodAnnotationWriter aw = JetMethodAnnotationWriter.visitAnnotation(mv);
            int kotlinFlags = getFlagsForVisibility(functionDescriptor.getVisibility());
            if (isInterface(functionDescriptor.getContainingDeclaration()) && modality != Modality.ABSTRACT) {
                kotlinFlags |= modality == Modality.FINAL
                                ? JvmStdlibNames.FLAG_FORCE_FINAL_BIT
                                : JvmStdlibNames.FLAG_FORCE_OPEN_BIT;
            }
            kotlinFlags |= DescriptorKindUtils.kindToFlags(functionDescriptor.getKind());
            //noinspection ConstantConditions
            aw.writeFlags(kotlinFlags);
            aw.writeTypeParameters(jvmSignature.getKotlinTypeParameter());
            aw.writeReturnType(jvmSignature.getKotlinReturnType());
            aw.visitEnd();
        }
        else {
            throw new IllegalStateException();
        }

        final List<JvmMethodParameterSignature> kotlinParameterTypes = jvmSignature.getKotlinParameterTypes();
        assert kotlinParameterTypes != null;

        if (receiverParameter.exists()) {
            JetValueParameterAnnotationWriter av = JetValueParameterAnnotationWriter.visitParameterAnnotation(mv, start++);
            av.writeName(JvmAbi.RECEIVER_PARAMETER);
            av.writeReceiver();
            if (kotlinParameterTypes.get(0) != null) {
                av.writeType(kotlinParameterTypes.get(0).getKotlinSignature());
            }
            av.visitEnd();
        }
        for (int i = 0; i != paramDescrs.size(); ++i) {
            ValueParameterDescriptor parameterDescriptor = paramDescrs.get(i);
            AnnotationCodegen.forParameter(i, mv, state.getTypeMapper()).genAnnotations(parameterDescriptor);
            JetValueParameterAnnotationWriter av = JetValueParameterAnnotationWriter.visitParameterAnnotation(mv, i + start);
            av.writeName(parameterDescriptor.getName().getName());
            av.writeHasDefaultValue(parameterDescriptor.declaresDefaultValue());
            if (kotlinParameterTypes.get(i) != null) {
                av.writeType(kotlinParameterTypes.get(i + start).getKotlinSignature());
            }
            av.visitEnd();
        }
    }

    public static void endVisit(MethodVisitor mv, @Nullable String description, @Nullable PsiElement method) {
        try {
            mv.visitMaxs(-1, -1);
        }
        catch (ProcessCanceledException e) {
            throw e;
        }
        catch (Throwable t) {
            throw new CompilationException(
                    "wrong code generated" +
                    (description != null ? " for " + description : "") +
                    t.getClass().getName() +
                    " " +
                    t.getMessage(),
                    t, method);
        }
        mv.visitEnd();
    }

    static void generateBridgeIfNeeded(
            CodegenContext owner,
            GenerationState state,
            ClassBuilder v,
            Method jvmSignature,
            FunctionDescriptor functionDescriptor,
            OwnerKind kind
    ) {
        if (kind == OwnerKind.TRAIT_IMPL) {
            return;
        }

        Method method =
                state.getTypeMapper().mapSignature(functionDescriptor.getName(), functionDescriptor).getAsmMethod();

        Queue<FunctionDescriptor> bfsQueue = new LinkedList<FunctionDescriptor>();
        Set<FunctionDescriptor> visited = new HashSet<FunctionDescriptor>();

        bfsQueue.offer(functionDescriptor.getOriginal());
        visited.add(functionDescriptor.getOriginal());
        for (FunctionDescriptor overriddenDescriptor : functionDescriptor.getOverriddenDescriptors()) {
            FunctionDescriptor orig = overriddenDescriptor.getOriginal();
            if (!visited.contains(orig)) {
                bfsQueue.offer(overriddenDescriptor);
                visited.add(overriddenDescriptor);
            }
        }

        Set<Method> bridgesToGenerate = new HashSet<Method>();
        while (!bfsQueue.isEmpty()) {
            FunctionDescriptor descriptor = bfsQueue.poll();
            if (descriptor.getKind() == CallableMemberDescriptor.Kind.DECLARATION) {
                Method overridden =
                        state.getTypeMapper().mapSignature(descriptor.getName(), descriptor.getOriginal()).getAsmMethod();
                if (differentMethods(method, overridden)) {
                    bridgesToGenerate.add(overridden);
                }
                continue;
            }

            for (FunctionDescriptor overriddenDescriptor : descriptor.getOverriddenDescriptors()) {
                FunctionDescriptor orig = overriddenDescriptor.getOriginal();
                if (!visited.contains(orig)) {
                    bfsQueue.offer(orig);
                    visited.add(orig);
                }
            }
        }

        for (Method overridden : bridgesToGenerate) {
            generateBridge(owner, state, v, jvmSignature, functionDescriptor, overridden);
        }
    }

    static void generateDefaultIfNeeded(
            MethodContext owner,
            GenerationState state,
            ClassBuilder v,
            Method jvmSignature,
            @NotNull FunctionDescriptor functionDescriptor,
            OwnerKind kind
    ) {
        DeclarationDescriptor contextClass = owner.getContextDescriptor().getContainingDeclaration();

        if (kind != OwnerKind.TRAIT_IMPL &&
            contextClass instanceof ClassDescriptor &&
            ((ClassDescriptor) contextClass).getKind() == ClassKind.TRAIT) {
            return;
        }

        if (!isDefaultNeeded(functionDescriptor)) {
            return;
        }

        ReceiverDescriptor receiverParameter = functionDescriptor.getReceiverParameter();
        boolean hasReceiver = receiverParameter.exists();
        boolean isStatic = isStatic(kind);

        if (kind == OwnerKind.TRAIT_IMPL) {
            String correctedDescr = "(" + jvmSignature.getDescriptor().substring(jvmSignature.getDescriptor().indexOf(";") + 1);
            jvmSignature = new Method(jvmSignature.getName(), correctedDescr);
        }

        int flags = ACC_PUBLIC | ACC_SYNTHETIC; // TODO.

        JvmClassName ownerInternalName;
        if (contextClass instanceof NamespaceDescriptor) {
            ownerInternalName = NamespaceCodegen.getJVMClassNameForKotlinNs(DescriptorUtils.getFQName(contextClass).toSafe());
        }
        else {
            ownerInternalName = JvmClassName.byType(state.getTypeMapper()
                                                            .mapType(((ClassDescriptor) contextClass).getDefaultType(),
                                                                     JetTypeMapperMode.IMPL));
        }

        String descriptor = jvmSignature.getDescriptor().replace(")", "I)");
        boolean isConstructor = "<init>".equals(jvmSignature.getName());
        if (!isStatic && !isConstructor) {
            descriptor = descriptor.replace("(", "(" + ownerInternalName.getDescriptor());
        }
        final MethodVisitor mv = v.newMethod(null, flags | (isConstructor ? 0 : ACC_STATIC),
                                             isConstructor ? "<init>" : jvmSignature.getName() + JvmAbi.DEFAULT_PARAMS_IMPL_SUFFIX,
                                             descriptor, null, null);
        InstructionAdapter iv = new InstructionAdapter(mv);
        if (state.getClassBuilderMode() == ClassBuilderMode.STUBS) {
            genStubCode(mv);
        }
        else if (state.getClassBuilderMode() == ClassBuilderMode.FULL) {
            generateDefaultImpl(owner, state, jvmSignature, functionDescriptor, kind, receiverParameter, hasReceiver, isStatic,
                                ownerInternalName,
                                isConstructor, mv, iv);
        }
    }

    private static void generateDefaultImpl(
            MethodContext owner,
            GenerationState state,
            Method jvmSignature,
            FunctionDescriptor functionDescriptor,
            OwnerKind kind,
            ReceiverDescriptor receiverParameter,
            boolean hasReceiver,
            boolean aStatic,
            JvmClassName ownerInternalName,
            boolean constructor,
            MethodVisitor mv,
            InstructionAdapter iv
    ) {
        mv.visitCode();

        FrameMap frameMap = owner.prepareFrame(state.getTypeMapper());

        if (kind instanceof OwnerKind.StaticDelegateKind) {
            frameMap.leaveTemp(OBJECT_TYPE);
        }

        ExpressionCodegen codegen = new ExpressionCodegen(mv, frameMap, jvmSignature.getReturnType(), owner, state);

        int var = 0;
        if (!aStatic) {
            var++;
        }

        Type receiverType;
        if (hasReceiver) {
            receiverType = state.getTypeMapper().mapType(receiverParameter.getType());
            var += receiverType.getSize();
        }
        else {
            receiverType = Type.DOUBLE_TYPE;
        }

        Type[] argTypes = jvmSignature.getArgumentTypes();
        List<ValueParameterDescriptor> paramDescrs = functionDescriptor.getValueParameters();
        for (int i = 0; i < paramDescrs.size(); i++) {
            Type argType = argTypes[i + (hasReceiver ? 1 : 0)];
            int size = argType.getSize();
            frameMap.enter(paramDescrs.get(i), argType);
            var += size;
        }

        int maskIndex = var;

        var = 0;
        if (!aStatic) {
            mv.visitVarInsn(ALOAD, var++);
        }

        if (hasReceiver) {
            iv.load(var, receiverType);
            var += receiverType.getSize();
        }

        int extra = hasReceiver ? 1 : 0;

        for (int index = 0; index < paramDescrs.size(); index++) {
            ValueParameterDescriptor parameterDescriptor = paramDescrs.get(index);

            Type t = argTypes[extra + index];

            if (frameMap.getIndex(parameterDescriptor) < 0) {
                frameMap.enter(parameterDescriptor, t);
            }

            if (parameterDescriptor.declaresDefaultValue()) {
                iv.load(maskIndex, Type.INT_TYPE);
                iv.iconst(1 << index);
                iv.and(Type.INT_TYPE);
                Label loadArg = new Label();
                iv.ifeq(loadArg);

                JetParameter jetParameter = (JetParameter) descriptorToDeclaration(state.getBindingContext(), parameterDescriptor);
                assert jetParameter != null;
                codegen.gen(jetParameter.getDefaultValue(), t);

                int ind = frameMap.getIndex(parameterDescriptor);
                iv.store(ind, t);

                iv.mark(loadArg);
            }

            iv.load(var, t);
            var += t.getSize();
        }

        final String internalName = ownerInternalName.getInternalName();
        final String jvmSignatureName = jvmSignature.getName();
        final String jvmSignatureDescriptor = jvmSignature.getDescriptor();
        if (!aStatic) {
            if (kind == OwnerKind.TRAIT_IMPL) {
                iv.invokeinterface(internalName, jvmSignatureName, jvmSignatureDescriptor);
            }
            else {
                if (!constructor) {
                    iv.invokevirtual(internalName, jvmSignatureName, jvmSignatureDescriptor);
                }
                else {
                    iv.invokespecial(internalName, jvmSignatureName, jvmSignatureDescriptor);
                }
            }
        }
        else {
            iv.invokestatic(internalName, jvmSignatureName, jvmSignatureDescriptor);
        }

        iv.areturn(jvmSignature.getReturnType());

        endVisit(mv, "default method", callableDescriptorToDeclaration(state.getBindingContext(), functionDescriptor));
        mv.visitEnd();
    }

    private static boolean isDefaultNeeded(FunctionDescriptor functionDescriptor) {
        boolean needed = false;
        if (functionDescriptor != null) {
            for (ValueParameterDescriptor parameterDescriptor : functionDescriptor.getValueParameters()) {
                if (parameterDescriptor.declaresDefaultValue()) {
                    needed = true;
                    break;
                }
            }
        }
        return needed;
    }

    private static boolean differentMethods(Method method, Method overridden) {
        if (!method.getReturnType().equals(overridden.getReturnType())) {
            return true;
        }
        Type[] methodArgumentTypes = method.getArgumentTypes();
        Type[] overriddenArgumentTypes = overridden.getArgumentTypes();
        if (methodArgumentTypes.length != overriddenArgumentTypes.length) {
            return true;
        }
        for (int i = 0; i != methodArgumentTypes.length; ++i) {
            if (!methodArgumentTypes[i].equals(overriddenArgumentTypes[i])) {
                return true;
            }
        }
        return false;
    }

    private static void generateBridge(
            CodegenContext owner,
            GenerationState state,
            ClassBuilder v,
            Method jvmSignature,
            FunctionDescriptor functionDescriptor,
            Method overridden
    ) {
        int flags = ACC_PUBLIC | ACC_BRIDGE; // TODO.

        final MethodVisitor mv = v.newMethod(null, flags, jvmSignature.getName(), overridden.getDescriptor(), null, null);
        if (state.getClassBuilderMode() == ClassBuilderMode.STUBS) {
            genStubCode(mv);
        }
        else if (state.getClassBuilderMode() == ClassBuilderMode.FULL) {
            mv.visitCode();

            Type[] argTypes = overridden.getArgumentTypes();
            Type[] originalArgTypes = jvmSignature.getArgumentTypes();
            InstructionAdapter iv = new InstructionAdapter(mv);
            iv.load(0, OBJECT_TYPE);
            for (int i = 0, reg = 1; i < argTypes.length; i++) {
                Type argType = argTypes[i];
                iv.load(reg, argType);
                if (argType.getSort() == Type.OBJECT) {
                    StackValue.onStack(OBJECT_TYPE).put(originalArgTypes[i], iv);
                }
                else if (argType.getSort() == Type.ARRAY) {
                    StackValue.onStack(JAVA_ARRAY_GENERIC_TYPE).put(originalArgTypes[i], iv);
                }

                //noinspection AssignmentToForLoopParameter
                reg += argType.getSize();
            }

            iv.invokevirtual(state.getTypeMapper().mapType(
                    (ClassDescriptor) owner.getContextDescriptor()).getInternalName(),
                             jvmSignature.getName(), jvmSignature.getDescriptor());
            if (isPrimitive(jvmSignature.getReturnType()) && !isPrimitive(overridden.getReturnType())) {
                StackValue.valueOf(iv, jvmSignature.getReturnType());
            }
            if (jvmSignature.getReturnType() == Type.VOID_TYPE) {
                iv.aconst(null);
            }
            iv.areturn(overridden.getReturnType());
            endVisit(mv, "bridge method", callableDescriptorToDeclaration(state.getBindingContext(), functionDescriptor));
        }
    }

    public void genDelegate(FunctionDescriptor functionDescriptor, CallableMemberDescriptor overriddenDescriptor, StackValue field) {
        genDelegate(functionDescriptor, overriddenDescriptor, field,
                    state.getTypeMapper().mapSignature(functionDescriptor.getName(), functionDescriptor),
                    state.getTypeMapper().mapSignature(overriddenDescriptor.getName(), (FunctionDescriptor)overriddenDescriptor.getOriginal())
        );
    }

    public void genDelegate(
            CallableMemberDescriptor functionDescriptor,
            CallableMemberDescriptor overriddenDescriptor,
            StackValue field,
            JvmMethodSignature jvmDelegateMethodSignature,
            JvmMethodSignature jvmMethodSignature
    ) {
        Method method = jvmMethodSignature.getAsmMethod();
        Method functionMethod = jvmDelegateMethodSignature.getAsmMethod();

        int flags = ACC_PUBLIC | ACC_SYNTHETIC; // TODO.

        final MethodVisitor mv = v.newMethod(null, flags, functionMethod.getName(), functionMethod.getDescriptor(), null, null);
        if (state.getClassBuilderMode() == ClassBuilderMode.STUBS) {
            genStubCode(mv);
        }
        else if (state.getClassBuilderMode() == ClassBuilderMode.FULL) {
            mv.visitCode();

            Type[] argTypes = method.getArgumentTypes();
            InstructionAdapter iv = new InstructionAdapter(mv);
            iv.load(0, OBJECT_TYPE);
            field.put(field.type, iv);
            for (int i = 0, reg = 1; i < argTypes.length; i++) {
                Type argType = argTypes[i];
                iv.load(reg, argType);
                if (argType.getSort() == Type.OBJECT) {
                    StackValue.onStack(OBJECT_TYPE).put(method.getArgumentTypes()[i], iv);
                }
                else if (argType.getSort() == Type.ARRAY) {
                    StackValue.onStack(JAVA_ARRAY_GENERIC_TYPE).put(method.getArgumentTypes()[i], iv);
                }

                //noinspection AssignmentToForLoopParameter
                reg += argType.getSize();
            }

            ClassDescriptor classDescriptor = (ClassDescriptor) overriddenDescriptor.getContainingDeclaration();
            String internalName =
                    state.getTypeMapper().mapType(classDescriptor).getInternalName();
            if (classDescriptor.getKind() == ClassKind.TRAIT) {
                iv.invokeinterface(internalName, method.getName(), method.getDescriptor());
            }
            else {
                iv.invokevirtual(internalName, method.getName(), method.getDescriptor());
            }

            if (!functionMethod.getReturnType().equals(method.getReturnType()) && !Type.VOID_TYPE.equals(method.getReturnType())) {
                iv.checkcast(functionMethod.getReturnType());
            }

            iv.areturn(functionMethod.getReturnType());
            endVisit(mv, "delegate method", descriptorToDeclaration(bindingContext, functionDescriptor));
        }
    }
}
