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

import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.asm4.Label;
import org.jetbrains.asm4.MethodVisitor;
import org.jetbrains.asm4.Type;
import org.jetbrains.asm4.commons.InstructionAdapter;
import org.jetbrains.asm4.commons.Method;
import org.jetbrains.jet.codegen.binding.CodegenBinding;
import org.jetbrains.jet.codegen.context.CodegenContext;
import org.jetbrains.jet.codegen.context.MethodContext;
import org.jetbrains.jet.codegen.signature.JvmMethodParameterSignature;
import org.jetbrains.jet.codegen.signature.JvmMethodSignature;
import org.jetbrains.jet.codegen.signature.kotlin.JetMethodAnnotationWriter;
import org.jetbrains.jet.codegen.signature.kotlin.JetValueParameterAnnotationWriter;
import org.jetbrains.jet.codegen.state.GenerationState;
import org.jetbrains.jet.codegen.state.GenerationStateAware;
import org.jetbrains.jet.codegen.state.JetTypeMapper;
import org.jetbrains.jet.codegen.state.JetTypeMapperMode;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.java.JvmAbi;
import org.jetbrains.jet.lang.resolve.java.JvmClassName;
import org.jetbrains.jet.lang.resolve.java.JvmStdlibNames;
import org.jetbrains.jet.lang.resolve.java.kt.DescriptorKindUtils;
import org.jetbrains.jet.lang.resolve.name.Name;

import java.util.*;

import static org.jetbrains.asm4.Opcodes.*;
import static org.jetbrains.jet.codegen.AsmUtil.*;
import static org.jetbrains.jet.codegen.CodegenUtil.*;
import static org.jetbrains.jet.codegen.binding.CodegenBinding.isLocalNamedFun;
import static org.jetbrains.jet.lang.resolve.BindingContextUtils.callableDescriptorToDeclaration;
import static org.jetbrains.jet.lang.resolve.BindingContextUtils.descriptorToDeclaration;
import static org.jetbrains.jet.lang.resolve.java.AsmTypeConstants.OBJECT_TYPE;

public class FunctionCodegen extends GenerationStateAware {
    private final CodegenContext owner;
    private final ClassBuilder v;

    public FunctionCodegen(CodegenContext owner, ClassBuilder v, GenerationState state) {
        super(state);
        this.owner = owner;
        this.v = v;
    }

    public void gen(JetNamedFunction f) {
        SimpleFunctionDescriptor functionDescriptor = bindingContext.get(BindingContext.FUNCTION, f);
        assert functionDescriptor != null;
        JvmMethodSignature method =
                typeMapper.mapToCallableMethod(
                        functionDescriptor,
                        false,
                        isCallInsideSameClassAsDeclared(functionDescriptor, owner),
                        isCallInsideSameModuleAsDeclared(functionDescriptor, owner),
                        owner.getContextKind()).getSignature();
        generateMethod(f, method, true, null, functionDescriptor);
    }


    public void generateMethod(
            @NotNull JetDeclaration declaration,
            @NotNull JvmMethodSignature jvmSignature,
            boolean needJetAnnotations,
            @Nullable String propertyTypeSignature,
            @NotNull FunctionDescriptor functionDescriptor
    ) {
        assert declaration instanceof JetDeclarationWithBody || declaration instanceof JetProperty || declaration instanceof JetParameter;

        checkMustGenerateCode(functionDescriptor);

        OwnerKind kind = owner.getContextKind();

        if (kind == OwnerKind.TRAIT_IMPL) {
            needJetAnnotations = false;
        }

        boolean hasBodyExpression = hasBodyExpression(declaration);

        MethodContext context = owner.intoFunction(functionDescriptor);
        if (kind != OwnerKind.TRAIT_IMPL || hasBodyExpression) {
            generateMethodHeaderAndBody(declaration, jvmSignature, needJetAnnotations, propertyTypeSignature, functionDescriptor, context);

            if (state.getClassBuilderMode() == ClassBuilderMode.FULL && !isAbstract(functionDescriptor, kind)) {
                generateBridgeIfNeeded(owner, state, v, jvmSignature.getAsmMethod(), functionDescriptor);
            }
        }

        generateDefaultIfNeeded(context, state, v, jvmSignature.getAsmMethod(), functionDescriptor, kind, DefaultParameterValueLoader.DEFAULT);
    }

    private void generateMethodHeaderAndBody(
            @NotNull JetDeclaration declaration,
            @NotNull JvmMethodSignature jvmSignature,
            boolean needJetAnnotations,
            @Nullable String propertyTypeSignature,
            @NotNull FunctionDescriptor functionDescriptor,
            @NotNull MethodContext context
    ) {
        OwnerKind kind = context.getContextKind();
        Method asmMethod = jvmSignature.getAsmMethod();

        MethodVisitor mv = v.newMethod(declaration,
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

        LocalVariablesInfo localVariablesInfo = generateLocalVariablesInfo(functionDescriptor);

        MethodBounds methodBounds = generateMethodBody(mv, declaration, functionDescriptor, context, asmMethod, localVariablesInfo);

        Type thisType;
        ReceiverParameterDescriptor expectedThisObject = functionDescriptor.getExpectedThisObject();
        if (expectedThisObject != null) {
            thisType = typeMapper.mapType(expectedThisObject.getType());
        }
        else if (declaration instanceof JetFunctionLiteral || isLocalNamedFun(functionDescriptor)) {
            thisType = typeMapper.mapType(context.getThisDescriptor());
        }
        else {
            thisType = null;
        }

        generateLocalVariableTable(typeMapper, mv, functionDescriptor, thisType, localVariablesInfo, methodBounds);

        endVisit(mv, null, declaration);
    }

    @NotNull
    private MethodBounds generateMethodBody(
            @NotNull MethodVisitor mv,
            @NotNull JetDeclaration funOrProperty,
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
        else {
            FrameMap frameMap = context.prepareFrame(typeMapper);

            int add = 0;
            if (kind == OwnerKind.TRAIT_IMPL) {
                add++;
            }

            if (functionDescriptor.getReceiverParameter() != null) {
                add++;
            }

            Type[] argTypes = asmMethod.getArgumentTypes();
            List<ValueParameterDescriptor> parameters = functionDescriptor.getValueParameters();
            for (int i = 0; i < parameters.size(); i++) {
                frameMap.enter(parameters.get(i), argTypes[i + add]);
            }

            createSharedVarsForParameters(mv, functionDescriptor, frameMap, localVariablesInfo);

            genNotNullAssertionsForParameters(new InstructionAdapter(mv), state, functionDescriptor, frameMap);

            boolean hasBodyExpression = hasBodyExpression(funOrProperty);
            if (hasBodyExpression) {
                JetDeclarationWithBody fun = (JetDeclarationWithBody) funOrProperty;
                ExpressionCodegen codegen = new ExpressionCodegen(mv, frameMap, asmMethod.getReturnType(), context, state);
                codegen.returnExpression(fun.getBodyExpression());

                localVariablesInfo.names.addAll(codegen.getLocalVariableNamesForExpression());
            } else {
                ///generate default accessor
                assert functionDescriptor instanceof PropertyAccessorDescriptor;
                PropertyCodegen.generateDefaultAccessor(
                        (PropertyAccessorDescriptor) functionDescriptor,
                        new InstructionAdapter(mv),
                        kind,
                        typeMapper,
                        context);
            }
        }

        Label methodEnd = new Label();
        mv.visitLabel(methodEnd);

        return new MethodBounds(methodBegin, methodEnd);
    }

    private static boolean hasBodyExpression(JetDeclaration funOrProperty) {
        return (funOrProperty instanceof JetDeclarationWithBody
                && ((JetDeclarationWithBody) funOrProperty).getBodyExpression() != null);
    }


    public static class MethodBounds {
        @NotNull private final Label begin;

        @NotNull private final Label end;

        public MethodBounds(@NotNull Label begin, @NotNull Label end) {
            this.begin = begin;
            this.end = end;
        }
    }

    public static class LocalVariablesInfo {
        @NotNull public final Collection<String> names = new HashSet<String>();

        @NotNull public final Map<Name, Label> labelsForSharedVars = new HashMap<Name, Label>();
    }

    public static LocalVariablesInfo generateLocalVariablesInfo(FunctionDescriptor functionDescriptor) {
        LocalVariablesInfo localVariablesInfo = new LocalVariablesInfo();
        for (ValueParameterDescriptor parameter : functionDescriptor.getValueParameters()) {
            localVariablesInfo.names.add(parameter.getName().getName());
        }
        return localVariablesInfo;
    }

    public static void generateLocalVariableTable(
            @NotNull JetTypeMapper typeMapper,
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

        ReceiverParameterDescriptor receiverParameter = functionDescriptor.getReceiverParameter();
        if (receiverParameter != null) {
            Type type = typeMapper.mapType(receiverParameter.getType());
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

    public static void generateStaticDelegateMethodBody(
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

    public static void genJetAnnotations(
            @NotNull GenerationState state,
            @NotNull FunctionDescriptor functionDescriptor,
            @Nullable JvmMethodSignature jvmSignature,
            @Nullable String propertyTypeSignature,
            MethodVisitor mv
    ) {
        if (jvmSignature == null) {
            jvmSignature = state.getTypeMapper().mapToCallableMethod(functionDescriptor, false, false, false, OwnerKind.IMPLEMENTATION).getSignature();
        }

        List<ValueParameterDescriptor> paramDescrs = functionDescriptor.getValueParameters();
        Modality modality = functionDescriptor.getModality();
        ReceiverParameterDescriptor receiverParameter = functionDescriptor.getReceiverParameter();

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

        List<JvmMethodParameterSignature> kotlinParameterTypes = jvmSignature.getKotlinParameterTypes();
        assert kotlinParameterTypes != null;

        if (receiverParameter != null) {
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
            av.writeVararg(parameterDescriptor.getVarargElementType() != null);
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
            FunctionDescriptor functionDescriptor
    ) {
        if (owner.getContextKind() == OwnerKind.TRAIT_IMPL) {
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

    static void generateConstructorWithoutParametersIfNeeded(
            @NotNull GenerationState state,
            @NotNull CallableMethod method,
            @NotNull ConstructorDescriptor constructorDescriptor,
            @NotNull ClassBuilder classBuilder
    ) {
        if (!isDefaultConstructorNeeded(state.getBindingContext(), constructorDescriptor)) {
            return;
        }
        int flags = getVisibilityAccessFlag(constructorDescriptor);
        MethodVisitor mv = classBuilder.newMethod(null, flags, "<init>", "()V", null, null);

        if (state.getClassBuilderMode() == ClassBuilderMode.SIGNATURES) {
            return;
        }
        else if (state.getClassBuilderMode() == ClassBuilderMode.STUBS) {
            genStubCode(mv);
        }
        else if (state.getClassBuilderMode() == ClassBuilderMode.FULL) {
            InstructionAdapter v = new InstructionAdapter(mv);
            mv.visitCode();

            JvmClassName ownerInternalName = method.getOwner();
            Method jvmSignature = method.getSignature().getAsmMethod();
            v.load(0, ownerInternalName.getAsmType()); // Load this on stack

            int mask = 0;
            for (ValueParameterDescriptor parameterDescriptor : constructorDescriptor.getValueParameters()) {
                Type paramType = state.getTypeMapper().mapType(parameterDescriptor.getType());
                pushDefaultValueOnStack(paramType, v);
                mask |= (1 << parameterDescriptor.getIndex());
            }
            v.iconst(mask);
            String desc = jvmSignature.getDescriptor().replace(")", "I)");
            v.invokespecial(ownerInternalName.getInternalName(), "<init>", desc);
            v.areturn(Type.VOID_TYPE);
            endVisit(mv, "default constructor for " + ownerInternalName.getInternalName(), null);
        }
    }

    static void generateDefaultIfNeeded(
            MethodContext owner,
            GenerationState state,
            ClassBuilder v,
            Method jvmSignature,
            @NotNull FunctionDescriptor functionDescriptor,
            OwnerKind kind,
            DefaultParameterValueLoader loadStrategy
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

        ReceiverParameterDescriptor receiverParameter = functionDescriptor.getReceiverParameter();
        boolean hasReceiver = receiverParameter != null;

        // Has outer in local variables (constructor for inner class)
        boolean hasOuter = functionDescriptor instanceof ConstructorDescriptor &&
                           CodegenBinding.canHaveOuter(state.getBindingContext(), ((ConstructorDescriptor) functionDescriptor).getContainingDeclaration());
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
        MethodVisitor mv = v.newMethod(null, flags | (isConstructor ? 0 : ACC_STATIC),
                                             isConstructor ? "<init>" : jvmSignature.getName() + JvmAbi.DEFAULT_PARAMS_IMPL_SUFFIX,
                                             descriptor, null, null);
        InstructionAdapter iv = new InstructionAdapter(mv);
        if (state.getClassBuilderMode() == ClassBuilderMode.STUBS) {
            genStubCode(mv);
        }
        else if (state.getClassBuilderMode() == ClassBuilderMode.FULL) {
            generateDefaultImpl(owner, state, jvmSignature, functionDescriptor, kind, receiverParameter, hasReceiver, hasOuter, isStatic,
                                ownerInternalName,
                                isConstructor, mv, iv, loadStrategy);
        }
    }

    private static void generateDefaultImpl(
            MethodContext owner,
            GenerationState state,
            Method jvmSignature,
            FunctionDescriptor functionDescriptor,
            OwnerKind kind,
            ReceiverParameterDescriptor receiverParameter,
            boolean hasReceiver, boolean hasOuter,
            boolean aStatic,
            JvmClassName ownerInternalName,
            boolean constructor,
            MethodVisitor mv,
            InstructionAdapter iv,
            DefaultParameterValueLoader loadStrategy
    ) {
        mv.visitCode();

        boolean isEnumConstructor = functionDescriptor instanceof ConstructorDescriptor &&
                                    DescriptorUtils.isEnumClass(functionDescriptor.getContainingDeclaration());

        FrameMap frameMap = owner.prepareFrame(state.getTypeMapper());

        if (kind instanceof OwnerKind.StaticDelegateKind) {
            frameMap.leaveTemp(OBJECT_TYPE);
        }

        if (hasOuter) {
            frameMap.enterTemp(OBJECT_TYPE);
        }

        if (isEnumConstructor) {
            frameMap.enterTemp(OBJECT_TYPE);
            frameMap.enterTemp(Type.INT_TYPE);
        }

        ExpressionCodegen codegen = new ExpressionCodegen(mv, frameMap, jvmSignature.getReturnType(), owner, state);

        Type receiverType = null;
        if (hasReceiver) {
            receiverType = state.getTypeMapper().mapType(receiverParameter.getType());
        }

        int extraInLocalVariablesTable = getSizeOfExplicitArgumentsInLocalVariablesTable(aStatic, hasOuter, isEnumConstructor, receiverType);
        int countOfExtraVarsInMethodArgs = getCountOfExplicitArgumentsInMethodArguments(hasOuter, hasReceiver, isEnumConstructor);

        Type[] argTypes = jvmSignature.getArgumentTypes();
        List<ValueParameterDescriptor> paramDescrs = functionDescriptor.getValueParameters();
        int paramSizeInLocalVariablesTable = 0;
        for (int i = 0; i < paramDescrs.size(); i++) {
            Type argType = argTypes[i + countOfExtraVarsInMethodArgs];
            int size = argType.getSize();
            frameMap.enter(paramDescrs.get(i), argType);
            paramSizeInLocalVariablesTable += size;
        }

        int maskIndex = extraInLocalVariablesTable + paramSizeInLocalVariablesTable;

        loadExplicitArgumentsOnStack(iv, OBJECT_TYPE, receiverType, ownerInternalName.getAsmType(), aStatic, hasOuter, isEnumConstructor);

        int indexInLocalVariablesTable = extraInLocalVariablesTable;
        for (int index = 0; index < paramDescrs.size(); index++) {
            ValueParameterDescriptor parameterDescriptor = paramDescrs.get(index);

            Type t = argTypes[countOfExtraVarsInMethodArgs + index];

            if (frameMap.getIndex(parameterDescriptor) < 0) {
                frameMap.enter(parameterDescriptor, t);
            }

            if (parameterDescriptor.declaresDefaultValue()) {
                iv.load(maskIndex, Type.INT_TYPE);
                iv.iconst(1 << index);
                iv.and(Type.INT_TYPE);
                Label loadArg = new Label();
                iv.ifeq(loadArg);

                loadStrategy.putValueOnStack(parameterDescriptor, codegen);

                int ind = frameMap.getIndex(parameterDescriptor);
                iv.store(ind, t);

                iv.mark(loadArg);
            }

            iv.load(indexInLocalVariablesTable, t);
            indexInLocalVariablesTable += t.getSize();
        }

        String internalName = ownerInternalName.getInternalName();
        String jvmSignatureName = jvmSignature.getName();
        String jvmSignatureDescriptor = jvmSignature.getDescriptor();
        if (!aStatic) {
            if (kind == OwnerKind.TRAIT_IMPL) {
                iv.invokeinterface(internalName, jvmSignatureName, jvmSignatureDescriptor);
            }
            else {
                if (!constructor && functionDescriptor.getVisibility() != Visibilities.PRIVATE) {
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

    private static int getSizeOfExplicitArgumentsInLocalVariablesTable(
            boolean isStatic,
            boolean hasOuter,
            boolean isEnumConstructor,
            @Nullable Type receiverType
    ) {
        int result = 0;
        if (!isStatic) result++;
        if (receiverType != null) result += receiverType.getSize();
        if (hasOuter) result++;
        if (isEnumConstructor) result += 2;
        return result;
    }

    private static int getCountOfExplicitArgumentsInMethodArguments(
            boolean hasOuter,
            boolean hasReceiver,
            boolean isEnumConstructor
    ) {
        int result = 0;
        if (hasReceiver) result++;
        if (hasOuter) result++;
        if (isEnumConstructor) result += 2;
        return result;
    }

    private static void loadExplicitArgumentsOnStack(@NotNull InstructionAdapter iv,
            @NotNull Type ownerType, @Nullable Type receiverType, @NotNull Type outerType,
            boolean isStatic, boolean hasOuter, boolean isEnumConstructor) {
        int var = 0;
        if (!isStatic) {
            iv.load(var++, ownerType);
        }

        if (hasOuter) {
            iv.load(var++, outerType);
        }

        if (isEnumConstructor) {
            iv.load(var++, OBJECT_TYPE);
            iv.load(var++, Type.INT_TYPE);
        }

        if (receiverType != null) {
            iv.load(var, receiverType);
        }
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

    private static boolean isDefaultConstructorNeeded(@NotNull BindingContext context, @NotNull ConstructorDescriptor constructorDescriptor) {
        ClassDescriptor classDescriptor = constructorDescriptor.getContainingDeclaration();

        if (CodegenBinding.canHaveOuter(context, classDescriptor)) return false;

        if (classDescriptor.getVisibility() == Visibilities.PRIVATE ||
            constructorDescriptor.getVisibility() == Visibilities.PRIVATE) return false;

        if (constructorDescriptor.getValueParameters().isEmpty()) return false;

        for (ValueParameterDescriptor parameterDescriptor : constructorDescriptor.getValueParameters()) {
            if (!parameterDescriptor.declaresDefaultValue()) {
                return false;
            }
        }
        return true;
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
        int flags = ACC_PUBLIC | ACC_BRIDGE | ACC_SYNTHETIC; // TODO.

        MethodVisitor mv = v.newMethod(null, flags, jvmSignature.getName(), overridden.getDescriptor(), null, null);
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
                StackValue.local(reg, argTypes[i]).put(originalArgTypes[i], iv);
                //noinspection AssignmentToForLoopParameter
                reg += argTypes[i].getSize();
            }

            iv.invokevirtual(state.getTypeMapper().mapType(
                    (ClassDescriptor) owner.getContextDescriptor()).getInternalName(),
                             jvmSignature.getName(), jvmSignature.getDescriptor());

            StackValue.onStack(jvmSignature.getReturnType()).put(overridden.getReturnType(), iv);

            iv.areturn(overridden.getReturnType());
            endVisit(mv, "bridge method", callableDescriptorToDeclaration(state.getBindingContext(), functionDescriptor));
        }
    }

    public void genDelegate(FunctionDescriptor functionDescriptor, FunctionDescriptor overriddenDescriptor, StackValue field) {
        genDelegate(functionDescriptor, (ClassDescriptor) overriddenDescriptor.getContainingDeclaration(), field,
                    typeMapper.mapSignature(functionDescriptor.getName(), functionDescriptor),
                    typeMapper.mapSignature(overriddenDescriptor.getName(), overriddenDescriptor.getOriginal())
        );
    }

    public void genDelegate(
            FunctionDescriptor functionDescriptor,
            ClassDescriptor toClass,
            StackValue field,
            JvmMethodSignature jvmDelegateMethodSignature,
            JvmMethodSignature jvmOverriddenMethodSignature
    ) {
        Method overriddenMethod = jvmOverriddenMethodSignature.getAsmMethod();
        Method delegateMethod = jvmDelegateMethodSignature.getAsmMethod();

        int flags = ACC_PUBLIC | ACC_SYNTHETIC; // TODO.

        MethodVisitor mv = v.newMethod(null, flags, delegateMethod.getName(), delegateMethod.getDescriptor(), null, null);
        if (state.getClassBuilderMode() == ClassBuilderMode.STUBS) {
            genStubCode(mv);
        }
        else if (state.getClassBuilderMode() == ClassBuilderMode.FULL) {
            mv.visitCode();

            Type[] argTypes = delegateMethod.getArgumentTypes();
            Type[] originalArgTypes = overriddenMethod.getArgumentTypes();

            InstructionAdapter iv = new InstructionAdapter(mv);
            iv.load(0, OBJECT_TYPE);
            field.put(field.type, iv);
            for (int i = 0, reg = 1; i < argTypes.length; i++) {
                StackValue.local(reg, argTypes[i]).put(originalArgTypes[i], iv);
                //noinspection AssignmentToForLoopParameter
                reg += argTypes[i].getSize();
            }

            String internalName = typeMapper.mapType(toClass).getInternalName();
            if (toClass.getKind() == ClassKind.TRAIT) {
                iv.invokeinterface(internalName, overriddenMethod.getName(), overriddenMethod.getDescriptor());
            }
            else {
                iv.invokevirtual(internalName, overriddenMethod.getName(), overriddenMethod.getDescriptor());
            }

            StackValue.onStack(overriddenMethod.getReturnType()).put(delegateMethod.getReturnType(), iv);

            iv.areturn(delegateMethod.getReturnType());
            endVisit(mv, "delegate method", descriptorToDeclaration(bindingContext, functionDescriptor));

            generateBridgeIfNeeded(owner, state, v, jvmDelegateMethodSignature.getAsmMethod(), functionDescriptor);
        }
    }
}
