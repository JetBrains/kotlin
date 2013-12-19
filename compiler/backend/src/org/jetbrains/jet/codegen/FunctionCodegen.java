/*
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
import jet.runtime.typeinfo.JetValueParameter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.asm4.AnnotationVisitor;
import org.jetbrains.asm4.Label;
import org.jetbrains.asm4.MethodVisitor;
import org.jetbrains.asm4.Type;
import org.jetbrains.asm4.commons.InstructionAdapter;
import org.jetbrains.asm4.commons.Method;
import org.jetbrains.asm4.util.TraceMethodVisitor;
import org.jetbrains.jet.codegen.binding.CodegenBinding;
import org.jetbrains.jet.codegen.context.CodegenContext;
import org.jetbrains.jet.codegen.context.MethodContext;
import org.jetbrains.jet.codegen.context.PackageFacadeContext;
import org.jetbrains.jet.codegen.signature.JvmMethodParameterKind;
import org.jetbrains.jet.codegen.signature.JvmMethodParameterSignature;
import org.jetbrains.jet.codegen.signature.JvmMethodSignature;
import org.jetbrains.jet.codegen.state.GenerationState;
import org.jetbrains.jet.codegen.state.JetTypeMapper;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.psi.JetNamedFunction;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.java.JvmAbi;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.*;

import static org.jetbrains.asm4.Opcodes.*;
import static org.jetbrains.jet.codegen.AsmUtil.*;
import static org.jetbrains.jet.codegen.CodegenUtil.isCallInsideSameClassAsDeclared;
import static org.jetbrains.jet.codegen.CodegenUtil.isCallInsideSameModuleAsDeclared;
import static org.jetbrains.jet.codegen.JvmSerializationBindings.*;
import static org.jetbrains.jet.codegen.binding.CodegenBinding.asmTypeForAnonymousClass;
import static org.jetbrains.jet.codegen.binding.CodegenBinding.isLocalNamedFun;
import static org.jetbrains.jet.lang.resolve.BindingContextUtils.callableDescriptorToDeclaration;
import static org.jetbrains.jet.lang.resolve.BindingContextUtils.descriptorToDeclaration;
import static org.jetbrains.jet.lang.resolve.DescriptorUtils.isFunctionLiteral;
import static org.jetbrains.jet.lang.resolve.java.AsmTypeConstants.OBJECT_TYPE;
import static org.jetbrains.jet.lang.resolve.java.resolver.DescriptorResolverUtils.fqNameByClass;

public class FunctionCodegen extends ParentCodegenAwareImpl {
    private final CodegenContext owner;

    private final ClassBuilder v;

    public FunctionCodegen(@NotNull CodegenContext owner, @NotNull ClassBuilder v, @NotNull GenerationState state, MemberCodegen parentCodegen) {
        super(state, parentCodegen);
        this.owner = owner;
        this.v = v;
    }

    public void gen(@NotNull JetNamedFunction function) {
        SimpleFunctionDescriptor functionDescriptor = bindingContext.get(BindingContext.FUNCTION, function);
        assert functionDescriptor != null : "No descriptor for function " + function.getText() + "\n" +
                                            "in " + function.getContainingFile().getVirtualFile();

        OwnerKind kind = owner.getContextKind();
        JvmMethodSignature method = typeMapper.mapSignature(functionDescriptor, true, kind);

        if (kind != OwnerKind.TRAIT_IMPL || function.getBodyExpression() != null) {
            generateMethod(function, method, functionDescriptor,
                           new FunctionGenerationStrategy.FunctionDefault(state, functionDescriptor, function));
        }

        generateDefaultIfNeeded(owner.intoFunction(functionDescriptor), method, functionDescriptor, kind,
                                DefaultParameterValueLoader.DEFAULT);
    }

    public void generateMethod(
            @Nullable PsiElement origin,
            @NotNull JvmMethodSignature jvmSignature,
            @NotNull FunctionDescriptor functionDescriptor,
            @NotNull FunctionGenerationStrategy strategy
    ) {
        generateMethod(origin, jvmSignature, functionDescriptor, owner.intoFunction(functionDescriptor), strategy);
    }

    public void generateMethod(
            @Nullable PsiElement origin,
            @NotNull JvmMethodSignature jvmSignature,
            @NotNull FunctionDescriptor functionDescriptor,
            @NotNull MethodContext methodContext,
            @NotNull FunctionGenerationStrategy strategy
    ) {
        Method asmMethod = jvmSignature.getAsmMethod();

        MethodVisitor mv = v.newMethod(origin,
                                       getMethodAsmFlags(functionDescriptor, methodContext.getContextKind()),
                                       asmMethod.getName(),
                                       asmMethod.getDescriptor(),
                                       jvmSignature.getGenericsSignature(),
                                       null);

        if (owner instanceof PackageFacadeContext) {
            Type ownerType = ((PackageFacadeContext) owner).getDelegateToClassType();
            v.getSerializationBindings().put(IMPL_CLASS_NAME_FOR_CALLABLE, functionDescriptor, shortNameByAsmType(ownerType));
        }
        else {
            v.getSerializationBindings().put(METHOD_FOR_FUNCTION, functionDescriptor, asmMethod);
        }

        AnnotationCodegen.forMethod(mv, typeMapper).genAnnotations(functionDescriptor);

        generateParameterAnnotations(functionDescriptor, mv, jvmSignature);

        generateJetValueParameterAnnotations(mv, functionDescriptor, jvmSignature);

        if (state.getClassBuilderMode() == ClassBuilderMode.LIGHT_CLASSES ||
            isAbstractMethod(functionDescriptor, methodContext.getContextKind())) {
            generateLocalVariableTable(
                    mv,
                    jvmSignature,
                    functionDescriptor,
                    getThisTypeForFunction(functionDescriptor, methodContext),
                    new Label(),
                    new Label(),
                    methodContext.getContextKind()
            );
            return;
        }

        generateMethodBody(mv, functionDescriptor, methodContext, jvmSignature, strategy);

        endVisit(mv, null, origin);

        generateBridgeIfNeeded(owner, state, v, jvmSignature.getAsmMethod(), functionDescriptor);

        methodContext.recordSyntheticAccessorIfNeeded(functionDescriptor, typeMapper);
    }

    private void generateParameterAnnotations(
            @NotNull FunctionDescriptor functionDescriptor,
            @NotNull MethodVisitor mv,
            @NotNull JvmMethodSignature jvmSignature
    ) {
        Iterator<ValueParameterDescriptor> iterator = functionDescriptor.getValueParameters().iterator();
        List<JvmMethodParameterSignature> kotlinParameterTypes = jvmSignature.getKotlinParameterTypes();

        for (int i = 0; i < kotlinParameterTypes.size(); i++) {
            JvmMethodParameterKind kind = kotlinParameterTypes.get(i).getKind();
            if (kind == JvmMethodParameterKind.ENUM_NAME || kind == JvmMethodParameterKind.ENUM_ORDINAL) {
                markEnumConstructorParameterAsSynthetic(mv, i);
                continue;
            }

            if (kind == JvmMethodParameterKind.VALUE) {
                ValueParameterDescriptor parameter = iterator.next();
                v.getSerializationBindings().put(INDEX_FOR_VALUE_PARAMETER, parameter, i);
                AnnotationCodegen.forParameter(i, mv, typeMapper).genAnnotations(parameter);
            }
        }
    }

    @SuppressWarnings("deprecation")
    private void generateJetValueParameterAnnotations(
            @NotNull MethodVisitor mv,
            @NotNull FunctionDescriptor functionDescriptor,
            @NotNull JvmMethodSignature jvmSignature
    ) {
        Iterator<ValueParameterDescriptor> descriptors = functionDescriptor.getValueParameters().iterator();
        List<JvmMethodParameterSignature> kotlinParameterTypes = jvmSignature.getKotlinParameterTypes();

        for (int i = 0; i < kotlinParameterTypes.size(); i++) {
            JvmMethodParameterKind kind = kotlinParameterTypes.get(i).getKind();
            if (kind == JvmMethodParameterKind.ENUM_NAME || kind == JvmMethodParameterKind.ENUM_ORDINAL) {
                markEnumConstructorParameterAsSynthetic(mv, i);
                continue;
            }

            String name;
            boolean nullableType;
            if (kind == JvmMethodParameterKind.VALUE) {
                ValueParameterDescriptor descriptor = descriptors.next();
                name = descriptor.getName().asString();
                nullableType = descriptor.getType().isNullable();
            }
            else {
                String lowercaseKind = kind.name().toLowerCase();
                if (needIndexForVar(kind)) {
                    name = "$" + lowercaseKind + "$" + i;
                }
                else {
                    name = "$" + lowercaseKind;
                }

                if (kind == JvmMethodParameterKind.RECEIVER) {
                    ReceiverParameterDescriptor receiver = functionDescriptor.getReceiverParameter();
                    nullableType = receiver == null || receiver.getType().isNullable();
                }
                else {
                    nullableType = true;
                }
            }

            AnnotationVisitor av =
                    mv.visitParameterAnnotation(i, asmDescByFqNameWithoutInnerClasses(fqNameByClass(JetValueParameter.class)), true);
            if (av != null) {
                av.visit("name", name);
                if (nullableType) {
                    av.visit("type", "?");
                }
                av.visitEnd();
            }
        }
    }

    private void markEnumConstructorParameterAsSynthetic(MethodVisitor mv, int i) {
        // IDEA's ClsPsi builder fails to annotate synthetic parameters
        if (state.getClassBuilderMode() == ClassBuilderMode.LIGHT_CLASSES) return;

        // This is needed to avoid RuntimeInvisibleParameterAnnotations error in javac:
        // see MethodWriter.visitParameterAnnotation()

        AnnotationVisitor av = mv.visitParameterAnnotation(i, "Ljava/lang/Synthetic;", true);
        if (av != null) {
            av.visitEnd();
        }
    }

    @Nullable
    private Type getThisTypeForFunction(@NotNull FunctionDescriptor functionDescriptor, @NotNull MethodContext context) {
        ReceiverParameterDescriptor expectedThisObject = functionDescriptor.getExpectedThisObject();
        if (functionDescriptor instanceof ConstructorDescriptor) {
            return typeMapper.mapType(functionDescriptor);
        }
        else if (expectedThisObject != null) {
            return typeMapper.mapType(expectedThisObject.getType());
        }
        else if (isFunctionLiteral(functionDescriptor) || isLocalNamedFun(functionDescriptor)) {
            return typeMapper.mapType(context.getThisDescriptor());
        }
        else {
            return null;
        }
    }

    private void generateMethodBody(
            @NotNull MethodVisitor mv,
            @NotNull FunctionDescriptor functionDescriptor,
            @NotNull MethodContext context,
            @NotNull JvmMethodSignature signature,
            @NotNull FunctionGenerationStrategy strategy
    ) {
        mv.visitCode();

        Label methodBegin = new Label();
        mv.visitLabel(methodBegin);

        if (context.getParentContext() instanceof PackageFacadeContext) {
            generateStaticDelegateMethodBody(mv, signature.getAsmMethod(), (PackageFacadeContext) context.getParentContext());
        }
        else {
            FrameMap frameMap = strategy.getFrameMap(typeMapper, context);

            for (ValueParameterDescriptor parameter : functionDescriptor.getValueParameters()) {
                frameMap.enter(parameter, typeMapper.mapType(parameter));
            }

            Label methodEntry = new Label();
            mv.visitLabel(methodEntry);
            context.setMethodStartLabel(methodEntry);

            if (!JetTypeMapper.isAccessor(functionDescriptor)) {
                genNotNullAssertionsForParameters(new InstructionAdapter(mv), state, functionDescriptor, frameMap);
            }

            strategy.generateBody(mv, signature, context, getParentCodegen());
        }

        Label methodEnd = new Label();
        mv.visitLabel(methodEnd);


        Type thisType = getThisTypeForFunction(functionDescriptor, context);
        generateLocalVariableTable(mv, signature, functionDescriptor, thisType, methodBegin, methodEnd, context.getContextKind());
    }

    private static void generateLocalVariableTable(
            @NotNull MethodVisitor mv,
            @NotNull JvmMethodSignature jvmMethodSignature,
            @NotNull FunctionDescriptor functionDescriptor,
            @Nullable Type thisType,
            @NotNull Label methodBegin,
            @NotNull Label methodEnd,
            @NotNull OwnerKind ownerKind
    ) {
        Iterator<ValueParameterDescriptor> valueParameters = functionDescriptor.getValueParameters().iterator();
        List<JvmMethodParameterSignature> params = jvmMethodSignature.getKotlinParameterTypes();
        int shift = 0;

        boolean isStatic = AsmUtil.isStaticMethod(ownerKind, functionDescriptor);
        if (!isStatic) {
            //add this
            if (thisType != null) {
                mv.visitLocalVariable("this", thisType.getDescriptor(), null, methodBegin, methodEnd, shift);
            } else {
                //TODO: provide thisType for callable reference
            }
            shift++;
        }

        for (int i = 0; i < params.size(); i++) {
            JvmMethodParameterSignature param =  params.get(i);
            JvmMethodParameterKind kind = param.getKind();
            String parameterName;

            if (kind == JvmMethodParameterKind.VALUE) {
                ValueParameterDescriptor parameter = valueParameters.next();
                parameterName = parameter.getName().asString();
            }
            else {
                String lowercaseKind = kind.name().toLowerCase();
                parameterName = needIndexForVar(kind)
                                ? "$" + lowercaseKind + "$" + i
                                : "$" + lowercaseKind;
            }

            Type type = param.getAsmType();
            mv.visitLocalVariable(parameterName, type.getDescriptor(), null, methodBegin, methodEnd, shift);
            shift += type.getSize();
        }
    }

    private static void generateStaticDelegateMethodBody(
            @NotNull MethodVisitor mv,
            @NotNull Method asmMethod,
            @NotNull PackageFacadeContext context
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
        iv.invokestatic(context.getDelegateToClassType().getInternalName(), asmMethod.getName(), asmMethod.getDescriptor());
        iv.areturn(asmMethod.getReturnType());
    }

    private static boolean needIndexForVar(JvmMethodParameterKind kind) {
        return kind == JvmMethodParameterKind.SHARED_VAR || kind == JvmMethodParameterKind.SUPER_CALL_PARAM;
    }

    public static void endVisit(MethodVisitor mv, @Nullable String description, @Nullable PsiElement method) {
        try {
            mv.visitMaxs(-1, -1);
        }
        catch (ProcessCanceledException e) {
            throw e;
        }
        catch (Throwable t) {
            String bytecode = renderByteCodeIfAvailable(mv);
            throw new CompilationException(
                    "wrong code generated" +
                    (description != null ? " for " + description : "") +
                    t.getClass().getName() +
                    " " +
                    t.getMessage() +
                    (bytecode != null ? "\nbytecode:\n" + bytecode : ""),
                    t, method);
        }
        mv.visitEnd();
    }

    private static String renderByteCodeIfAvailable(MethodVisitor mv) {
        String bytecode = null;
        if (mv instanceof TraceMethodVisitor) {
            TraceMethodVisitor traceMethodVisitor = (TraceMethodVisitor) mv;
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            traceMethodVisitor.p.print(pw);
            pw.close();
            bytecode = sw.toString();
        }
        return bytecode;
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
                state.getTypeMapper().mapSignature(functionDescriptor).getAsmMethod();

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
                        state.getTypeMapper().mapSignature(descriptor.getOriginal()).getAsmMethod();
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
            generateBridge(state, v, jvmSignature, functionDescriptor, overridden);
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

        if (state.getClassBuilderMode() == ClassBuilderMode.LIGHT_CLASSES) {
            return;
        }
        else if (state.getClassBuilderMode() == ClassBuilderMode.FULL) {
            InstructionAdapter v = new InstructionAdapter(mv);
            mv.visitCode();

            Type methodOwner = method.getOwner();
            Method jvmSignature = method.getSignature().getAsmMethod();
            v.load(0, methodOwner); // Load this on stack

            int mask = 0;
            for (ValueParameterDescriptor parameterDescriptor : constructorDescriptor.getValueParameters()) {
                Type paramType = state.getTypeMapper().mapType(parameterDescriptor.getType());
                pushDefaultValueOnStack(paramType, v);
                mask |= (1 << parameterDescriptor.getIndex());
            }
            v.iconst(mask);
            String desc = jvmSignature.getDescriptor().replace(")", "I)");
            v.invokespecial(methodOwner.getInternalName(), "<init>", desc);
            v.areturn(Type.VOID_TYPE);
            endVisit(mv, "default constructor for " + methodOwner.getInternalName(), null);
        }
    }

    void generateDefaultIfNeeded(
            @NotNull MethodContext owner,
            @NotNull JvmMethodSignature signature,
            @NotNull FunctionDescriptor functionDescriptor,
            @NotNull OwnerKind kind,
            @NotNull DefaultParameterValueLoader loadStrategy
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

        boolean isStatic = isStatic(kind);

        Method jvmSignature = signature.getAsmMethod();

        int flags = ACC_PUBLIC | ACC_SYNTHETIC; // TODO.

        Type ownerType;
        if (contextClass instanceof PackageFragmentDescriptor) {
            ownerType = state.getTypeMapper().getOwner(functionDescriptor, kind, true);
        }
        else if (contextClass instanceof ClassDescriptor) {
            ownerType = state.getTypeMapper().mapClass((ClassDescriptor) contextClass);
        }
        else if (isLocalNamedFun(functionDescriptor)) {
            ownerType = asmTypeForAnonymousClass(state.getBindingContext(), functionDescriptor);
        }
        else {
            throw new IllegalStateException("Couldn't obtain owner name for " + functionDescriptor);
        }

        String descriptor = jvmSignature.getDescriptor().replace(")", "I)");
        boolean isConstructor = "<init>".equals(jvmSignature.getName());
        if (!isStatic && !isConstructor) {
            descriptor = descriptor.replace("(", "(" + ownerType.getDescriptor());
        }
        MethodVisitor mv = v.newMethod(null, flags | (isConstructor ? 0 : ACC_STATIC),
                                       isConstructor ? "<init>" : jvmSignature.getName() + JvmAbi.DEFAULT_PARAMS_IMPL_SUFFIX,
                                       descriptor, null, null);

        if (state.getClassBuilderMode() == ClassBuilderMode.FULL) {
            generateDefaultImpl(owner, signature, functionDescriptor, isStatic, mv, loadStrategy);

        }
    }

    private void generateDefaultImpl(
            @NotNull MethodContext methodContext,
            @NotNull JvmMethodSignature signature,
            @NotNull FunctionDescriptor functionDescriptor,
            boolean aStatic,
            @NotNull MethodVisitor mv,
            @NotNull DefaultParameterValueLoader loadStrategy
    ) {
        mv.visitCode();

        FrameMap frameMap = new FrameMap();

        if (!aStatic) {
            frameMap.enterTemp(OBJECT_TYPE);
        }

        Method jvmSignature = signature.getAsmMethod();
        ExpressionCodegen codegen = new ExpressionCodegen(mv, frameMap, jvmSignature.getReturnType(), methodContext, state, getParentCodegen());

        Type[] argTypes = jvmSignature.getArgumentTypes();
        List<ValueParameterDescriptor> paramDescrs = functionDescriptor.getValueParameters();
        Iterator<ValueParameterDescriptor> iterator = paramDescrs.iterator();

        int countOfExtraVarsInMethodArgs = 0;

        for (JvmMethodParameterSignature parameterSignature : signature.getKotlinParameterTypes()) {
            if (parameterSignature.getKind() != JvmMethodParameterKind.VALUE) {
                countOfExtraVarsInMethodArgs++;
                frameMap.enterTemp(parameterSignature.getAsmType());
            }
            else {
                frameMap.enter(iterator.next(), parameterSignature.getAsmType());
            }
        }

        int maskIndex = frameMap.enterTemp(Type.INT_TYPE);

        InstructionAdapter iv = new InstructionAdapter(mv);
        loadExplicitArgumentsOnStack(iv, OBJECT_TYPE, aStatic, signature);

        for (int index = 0; index < paramDescrs.size(); index++) {
            ValueParameterDescriptor parameterDescriptor = paramDescrs.get(index);

            Type t = argTypes[countOfExtraVarsInMethodArgs + index];

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

            iv.load(frameMap.getIndex(parameterDescriptor), t);
        }

        CallableMethod method;
        if (functionDescriptor instanceof ConstructorDescriptor) {
            method = state.getTypeMapper().mapToCallableMethod((ConstructorDescriptor) functionDescriptor);
        } else {
            method = state.getTypeMapper()
                    .mapToCallableMethod(functionDescriptor, false, isCallInsideSameClassAsDeclared(functionDescriptor, methodContext),
                                         isCallInsideSameModuleAsDeclared(functionDescriptor, methodContext), OwnerKind.IMPLEMENTATION);
        }

        iv.visitMethodInsn(method.getInvokeOpcode(), method.getOwner().getInternalName(), method.getSignature().getAsmMethod().getName(),
                           method.getSignature().getAsmMethod().getDescriptor());

        iv.areturn(jvmSignature.getReturnType());

        endVisit(mv, "default method", callableDescriptorToDeclaration(state.getBindingContext(), functionDescriptor));
    }


    private static void loadExplicitArgumentsOnStack(
            @NotNull InstructionAdapter iv,
            @NotNull Type ownerType,
            boolean isStatic,
            @NotNull JvmMethodSignature signature
    ) {
        int var = 0;
        if (!isStatic) {
            iv.load(var, ownerType);
            var += ownerType.getSize();
        }

        for (JvmMethodParameterSignature parameterSignature : signature.getKotlinParameterTypes()) {
            if (parameterSignature.getKind() != JvmMethodParameterKind.VALUE) {
                Type type = parameterSignature.getAsmType();
                iv.load(var, type);
                var += type.getSize();
            }
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
            GenerationState state,
            ClassBuilder v,
            Method jvmSignature,
            FunctionDescriptor functionDescriptor,
            Method overridden
    ) {
        int flags = ACC_PUBLIC | ACC_BRIDGE | ACC_SYNTHETIC; // TODO.

        MethodVisitor mv = v.newMethod(null, flags, jvmSignature.getName(), overridden.getDescriptor(), null, null);
        if (state.getClassBuilderMode() == ClassBuilderMode.FULL) {
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

            iv.invokevirtual(v.getThisName(), jvmSignature.getName(), jvmSignature.getDescriptor());

            StackValue.onStack(jvmSignature.getReturnType()).put(overridden.getReturnType(), iv);

            iv.areturn(overridden.getReturnType());
            endVisit(mv, "bridge method", callableDescriptorToDeclaration(state.getBindingContext(), functionDescriptor));
        }
    }

    public void genDelegate(FunctionDescriptor functionDescriptor, FunctionDescriptor overriddenDescriptor, StackValue field) {
        genDelegate(functionDescriptor, (ClassDescriptor) overriddenDescriptor.getContainingDeclaration(), field,
                    typeMapper.mapSignature(functionDescriptor),
                    typeMapper.mapSignature(overriddenDescriptor.getOriginal())
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
        if (state.getClassBuilderMode() == ClassBuilderMode.FULL) {
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
            endVisit(mv, "Delegate method " + functionDescriptor + " to " + jvmOverriddenMethodSignature,
                     descriptorToDeclaration(bindingContext, functionDescriptor.getContainingDeclaration()));

            generateBridgeIfNeeded(owner, state, v, jvmDelegateMethodSignature.getAsmMethod(), functionDescriptor);
        }
    }
}
