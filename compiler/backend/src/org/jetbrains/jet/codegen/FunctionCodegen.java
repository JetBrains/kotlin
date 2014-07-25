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
import com.intellij.util.ArrayUtil;
import com.intellij.util.Function;
import com.intellij.util.containers.ContainerUtil;
import kotlin.Function1;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.codegen.binding.CodegenBinding;
import org.jetbrains.jet.codegen.bridges.Bridge;
import org.jetbrains.jet.codegen.bridges.BridgesPackage;
import org.jetbrains.jet.codegen.context.CodegenContext;
import org.jetbrains.jet.codegen.context.MethodContext;
import org.jetbrains.jet.codegen.context.PackageFacadeContext;
import org.jetbrains.jet.codegen.optimization.OptimizationMethodVisitor;
import org.jetbrains.jet.codegen.state.GenerationState;
import org.jetbrains.jet.codegen.state.JetTypeMapper;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.psi.JetNamedFunction;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.calls.CallResolverUtil;
import org.jetbrains.jet.lang.resolve.constants.ArrayValue;
import org.jetbrains.jet.lang.resolve.constants.CompileTimeConstant;
import org.jetbrains.jet.lang.resolve.constants.JavaClassValue;
import org.jetbrains.jet.lang.resolve.java.diagnostics.JvmDeclarationOrigin;
import org.jetbrains.jet.lang.resolve.java.jvmSignature.JvmMethodParameterKind;
import org.jetbrains.jet.lang.resolve.java.jvmSignature.JvmMethodParameterSignature;
import org.jetbrains.jet.lang.resolve.java.jvmSignature.JvmMethodSignature;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;
import org.jetbrains.org.objectweb.asm.AnnotationVisitor;
import org.jetbrains.org.objectweb.asm.Label;
import org.jetbrains.org.objectweb.asm.MethodVisitor;
import org.jetbrains.org.objectweb.asm.Type;
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter;
import org.jetbrains.org.objectweb.asm.commons.Method;
import org.jetbrains.org.objectweb.asm.util.TraceMethodVisitor;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import static org.jetbrains.jet.codegen.AsmUtil.*;
import static org.jetbrains.jet.codegen.JvmSerializationBindings.*;
import static org.jetbrains.jet.codegen.binding.CodegenBinding.isLocalNamedFun;
import static org.jetbrains.jet.lang.descriptors.CallableMemberDescriptor.Kind.DECLARATION;
import static org.jetbrains.jet.lang.resolve.DescriptorToSourceUtils.callableDescriptorToDeclaration;
import static org.jetbrains.jet.lang.resolve.DescriptorUtils.isFunctionLiteral;
import static org.jetbrains.jet.lang.resolve.DescriptorUtils.isTrait;
import static org.jetbrains.jet.lang.resolve.java.AsmTypeConstants.OBJECT_TYPE;
import static org.jetbrains.jet.lang.resolve.java.JvmAnnotationNames.OLD_JET_VALUE_PARAMETER_ANNOTATION;
import static org.jetbrains.jet.lang.resolve.java.diagnostics.DiagnosticsPackage.OtherOrigin;
import static org.jetbrains.org.objectweb.asm.Opcodes.*;

public class FunctionCodegen extends ParentCodegenAware {
    private final CodegenContext owner;

    private final ClassBuilder v;

    public FunctionCodegen(
            @NotNull CodegenContext owner,
            @NotNull ClassBuilder v,
            @NotNull GenerationState state,
            MemberCodegen<?> parentCodegen
    ) {
        super(state, parentCodegen);
        this.owner = owner;
        this.v = v;
    }

    public void gen(@NotNull JetNamedFunction function) {
        SimpleFunctionDescriptor functionDescriptor = bindingContext.get(BindingContext.FUNCTION, function);
        assert functionDescriptor != null : "No descriptor for function " + function.getText() + "\n" +
                                            "in " + function.getContainingFile().getVirtualFile();

        OwnerKind kind = owner.getContextKind();
        JvmMethodSignature method = typeMapper.mapSignature(functionDescriptor, kind);

        if (kind != OwnerKind.TRAIT_IMPL || function.hasBody()) {
            generateMethod(OtherOrigin(function, functionDescriptor),
                           method, functionDescriptor,
                           new FunctionGenerationStrategy.FunctionDefault(state, functionDescriptor, function));
        }

        generateDefaultIfNeeded(owner.intoFunction(functionDescriptor), method, functionDescriptor, kind,
                                DefaultParameterValueLoader.DEFAULT, function);
    }

    public void generateMethod(
            @NotNull JvmDeclarationOrigin origin,
            @NotNull JvmMethodSignature jvmSignature,
            @NotNull FunctionDescriptor functionDescriptor,
            @NotNull FunctionGenerationStrategy strategy
    ) {
        generateMethod(origin, jvmSignature, functionDescriptor, owner.intoFunction(functionDescriptor), strategy);
    }

    public void generateMethod(
            @NotNull JvmDeclarationOrigin origin,
            @NotNull JvmMethodSignature jvmSignature,
            @NotNull FunctionDescriptor functionDescriptor,
            @NotNull MethodContext methodContext,
            @NotNull FunctionGenerationStrategy strategy
    ) {
        OwnerKind methodContextKind = methodContext.getContextKind();
        Method asmMethod = jvmSignature.getAsmMethod();

        MethodVisitor mv = v.newMethod(origin,
                                       getMethodAsmFlags(functionDescriptor, methodContextKind),
                                       asmMethod.getName(),
                                       asmMethod.getDescriptor(),
                                       jvmSignature.getGenericsSignature(),
                                       getThrownExceptions(functionDescriptor, typeMapper));

        if (owner instanceof PackageFacadeContext) {
            Type ownerType = ((PackageFacadeContext) owner).getDelegateToClassType();
            v.getSerializationBindings().put(IMPL_CLASS_NAME_FOR_CALLABLE, functionDescriptor, shortNameByAsmType(ownerType));
        }
        else {
            v.getSerializationBindings().put(METHOD_FOR_FUNCTION, functionDescriptor, asmMethod);
        }

        AnnotationCodegen.forMethod(mv, typeMapper).genAnnotations(functionDescriptor, asmMethod.getReturnType());

        generateParameterAnnotations(functionDescriptor, mv, jvmSignature);

        generateJetValueParameterAnnotations(mv, functionDescriptor, jvmSignature);

        generateBridges(functionDescriptor);

        if (state.getClassBuilderMode() == ClassBuilderMode.LIGHT_CLASSES || isAbstractMethod(functionDescriptor, methodContextKind)) {
            generateLocalVariableTable(
                    mv,
                    jvmSignature,
                    functionDescriptor,
                    getThisTypeForFunction(functionDescriptor, methodContext, typeMapper),
                    new Label(),
                    new Label(),
                    methodContextKind
            );

            mv.visitEnd();
            return;
        }

        generateMethodBody(mv, functionDescriptor, methodContext, jvmSignature, strategy, getParentCodegen());

        endVisit(mv, null, origin.getElement());

        methodContext.recordSyntheticAccessorIfNeeded(functionDescriptor, bindingContext);
    }

    private void generateParameterAnnotations(
            @NotNull FunctionDescriptor functionDescriptor,
            @NotNull MethodVisitor mv,
            @NotNull JvmMethodSignature jvmSignature
    ) {
        Iterator<ValueParameterDescriptor> iterator = functionDescriptor.getValueParameters().iterator();
        List<JvmMethodParameterSignature> kotlinParameterTypes = jvmSignature.getValueParameters();

        for (int i = 0; i < kotlinParameterTypes.size(); i++) {
            JvmMethodParameterSignature parameterSignature = kotlinParameterTypes.get(i);
            JvmMethodParameterKind kind = parameterSignature.getKind();
            if (kind.isSkippedInGenericSignature()) {
                markEnumOrInnerConstructorParameterAsSynthetic(mv, i);
                continue;
            }

            if (kind == JvmMethodParameterKind.VALUE) {
                ValueParameterDescriptor parameter = iterator.next();
                v.getSerializationBindings().put(INDEX_FOR_VALUE_PARAMETER, parameter, i);
                AnnotationCodegen.forParameter(i, mv, typeMapper).genAnnotations(parameter, parameterSignature.getAsmType());
            }
        }
    }

    @SuppressWarnings("deprecation")
    private static void generateJetValueParameterAnnotations(
            @NotNull MethodVisitor mv,
            @NotNull FunctionDescriptor functionDescriptor,
            @NotNull JvmMethodSignature jvmSignature
    ) {
        Iterator<ValueParameterDescriptor> descriptors = functionDescriptor.getValueParameters().iterator();
        List<JvmMethodParameterSignature> kotlinParameterTypes = jvmSignature.getValueParameters();

        for (int i = 0; i < kotlinParameterTypes.size(); i++) {
            JvmMethodParameterKind kind = kotlinParameterTypes.get(i).getKind();
            if (kind.isSkippedInGenericSignature()) continue;

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
                    mv.visitParameterAnnotation(i, asmDescByFqNameWithoutInnerClasses(OLD_JET_VALUE_PARAMETER_ANNOTATION), true);
            if (av != null) {
                av.visit("name", name);
                if (nullableType) {
                    av.visit("type", "?");
                }
                av.visitEnd();
            }
        }
    }

    private void markEnumOrInnerConstructorParameterAsSynthetic(MethodVisitor mv, int i) {
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
    private static Type getThisTypeForFunction(@NotNull FunctionDescriptor functionDescriptor, @NotNull MethodContext context, @NotNull JetTypeMapper typeMapper) {
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

    public static void generateMethodBody(
            @NotNull MethodVisitor mv,
            @NotNull FunctionDescriptor functionDescriptor,
            @NotNull MethodContext context,
            @NotNull JvmMethodSignature signature,
            @NotNull FunctionGenerationStrategy strategy,
            @NotNull MemberCodegen<?> parentCodegen
    ) {
        mv.visitCode();

        Label methodBegin = new Label();
        mv.visitLabel(methodBegin);

        JetTypeMapper typeMapper = parentCodegen.typeMapper;

        if (context.getParentContext() instanceof PackageFacadeContext) {
            generateStaticDelegateMethodBody(mv, signature.getAsmMethod(), (PackageFacadeContext) context.getParentContext());
        }
        else {
            FrameMap frameMap = createFrameMap(parentCodegen.state, functionDescriptor, signature, isStatic(context.getContextKind()));

            Label methodEntry = new Label();
            mv.visitLabel(methodEntry);
            context.setMethodStartLabel(methodEntry);

            if (!JetTypeMapper.isAccessor(functionDescriptor)) {
                genNotNullAssertionsForParameters(new InstructionAdapter(mv), parentCodegen.state, functionDescriptor, frameMap);
            }

            strategy.generateBody(mv, frameMap, signature, context, parentCodegen);
        }

        Label methodEnd = new Label();
        mv.visitLabel(methodEnd);

        Type thisType = getThisTypeForFunction(functionDescriptor, context, typeMapper);
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
        List<JvmMethodParameterSignature> params = jvmMethodSignature.getValueParameters();
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

        // The first line of some package file is written to the line number attribute of a static delegate to allow to 'step into' it
        // This is similar to what javac does with bridge methods
        Label label = new Label();
        iv.visitLabel(label);
        iv.visitLineNumber(1, label);

        int k = 0;
        for (Type argType : argTypes) {
            iv.load(k, argType);
            k += argType.getSize();
        }
        iv.invokestatic(context.getDelegateToClassType().getInternalName(), asmMethod.getName(), asmMethod.getDescriptor(), false);
        iv.areturn(asmMethod.getReturnType());
    }

    private static boolean needIndexForVar(JvmMethodParameterKind kind) {
        return kind == JvmMethodParameterKind.CAPTURED_LOCAL_VARIABLE ||
               kind == JvmMethodParameterKind.ENUM_NAME_OR_ORDINAL ||
               kind == JvmMethodParameterKind.SUPER_CALL_PARAM;
    }

    public static void endVisit(MethodVisitor mv, @Nullable String description, @Nullable PsiElement method) {
        try {
            mv.visitMaxs(-1, -1);
            mv.visitEnd();
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
    }

    private static String renderByteCodeIfAvailable(MethodVisitor mv) {
        String bytecode = null;

        if (mv instanceof OptimizationMethodVisitor) {
            mv = ((OptimizationMethodVisitor) mv).getTraceMethodVisitorIfPossible();
        }

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

    public void generateBridges(@NotNull FunctionDescriptor descriptor) {
        if (descriptor instanceof ConstructorDescriptor) return;
        if (owner.getContextKind() == OwnerKind.TRAIT_IMPL) return;
        if (isTrait(descriptor.getContainingDeclaration())) return;

        // equals(Any?), hashCode(), toString() never need bridges
        if (isMethodOfAny(descriptor)) return;

        // If the function doesn't have a physical declaration among super-functions, it's a SAM adapter or alike and doesn't need bridges
        if (CallResolverUtil.isOrOverridesSynthesized(descriptor)) return;

        Set<Bridge<Method>> bridgesToGenerate = BridgesPackage.generateBridgesForFunctionDescriptor(
                descriptor,
                new Function1<FunctionDescriptor, Method>() {
                    @Override
                    public Method invoke(FunctionDescriptor descriptor) {
                        return typeMapper.mapSignature(descriptor).getAsmMethod();
                    }
                }
        );

        if (!bridgesToGenerate.isEmpty()) {
            PsiElement origin = descriptor.getKind() == DECLARATION ? callableDescriptorToDeclaration(descriptor) : null;
            for (Bridge<Method> bridge : bridgesToGenerate) {
                generateBridge(origin, descriptor, bridge.getFrom(), bridge.getTo());
            }
        }
    }

    private static boolean isMethodOfAny(@NotNull FunctionDescriptor descriptor) {
        String name = descriptor.getName().asString();
        List<ValueParameterDescriptor> parameters = descriptor.getValueParameters();
        if (parameters.isEmpty()) {
            return name.equals("hashCode") || name.equals("toString");
        }
        else if (parameters.size() == 1 && name.equals("equals")) {
            ValueParameterDescriptor parameter = parameters.get(0);
            return parameter.getType().equals(KotlinBuiltIns.getInstance().getNullableAnyType());
        }
        return false;
    }

    @NotNull
    private static String[] getThrownExceptions(@NotNull FunctionDescriptor function, @NotNull final JetTypeMapper mapper) {
        AnnotationDescriptor annotation = function.getAnnotations().findAnnotation(new FqName("kotlin.throws"));
        if (annotation == null) return ArrayUtil.EMPTY_STRING_ARRAY;

        Collection<CompileTimeConstant<?>> values = annotation.getAllValueArguments().values();
        if (values.isEmpty()) return ArrayUtil.EMPTY_STRING_ARRAY;

        Object value = values.iterator().next();
        if (!(value instanceof ArrayValue)) return ArrayUtil.EMPTY_STRING_ARRAY;
        ArrayValue arrayValue = (ArrayValue) value;

        List<String> strings = ContainerUtil.mapNotNull(
                arrayValue.getValue(),
                new Function<CompileTimeConstant<?>, String>() {
                    @Override
                    public String fun(CompileTimeConstant<?> constant) {
                        if (constant instanceof JavaClassValue) {
                            JavaClassValue classValue = (JavaClassValue) constant;
                            ClassDescriptor classDescriptor = DescriptorUtils.getClassDescriptorForType(classValue.getValue());
                            return mapper.mapClass(classDescriptor).getInternalName();
                        }
                        return null;
                    }
                }
        );
        return ArrayUtil.toStringArray(strings);
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
        MethodVisitor mv = classBuilder.newMethod(OtherOrigin(constructorDescriptor), flags, "<init>", "()V", null,
                                                  getThrownExceptions(constructorDescriptor, state.getTypeMapper()));

        if (state.getClassBuilderMode() == ClassBuilderMode.LIGHT_CLASSES) return;

        InstructionAdapter v = new InstructionAdapter(mv);
        mv.visitCode();

        Type methodOwner = method.getOwner();
        v.load(0, methodOwner); // Load this on stack

        int mask = 0;
        for (ValueParameterDescriptor parameterDescriptor : constructorDescriptor.getValueParameters()) {
            Type paramType = state.getTypeMapper().mapType(parameterDescriptor.getType());
            pushDefaultValueOnStack(paramType, v);
            mask |= (1 << parameterDescriptor.getIndex());
        }
        v.iconst(mask);
        String desc = method.getAsmMethod().getDescriptor().replace(")", "I)");
        v.invokespecial(methodOwner.getInternalName(), "<init>", desc, false);
        v.areturn(Type.VOID_TYPE);
        endVisit(mv, "default constructor for " + methodOwner.getInternalName(), null);
    }

    void generateDefaultIfNeeded(
            @NotNull MethodContext owner,
            @NotNull JvmMethodSignature signature,
            @NotNull FunctionDescriptor functionDescriptor,
            @NotNull OwnerKind kind,
            @NotNull DefaultParameterValueLoader loadStrategy,
            @Nullable JetNamedFunction function
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

        Method jvmSignature = signature.getAsmMethod();

        int flags = getVisibilityAccessFlag(functionDescriptor) | getDeprecatedAccessFlag(functionDescriptor);

        boolean isConstructor = "<init>".equals(jvmSignature.getName());

        Method defaultMethod = typeMapper.mapDefaultMethod(functionDescriptor, kind, owner);

        MethodVisitor mv = v.newMethod(OtherOrigin(functionDescriptor), flags | (isConstructor ? 0 : ACC_STATIC),
                                       defaultMethod.getName(),
                                       defaultMethod.getDescriptor(), null,
                                       getThrownExceptions(functionDescriptor, typeMapper));

        if (state.getClassBuilderMode() == ClassBuilderMode.FULL) {
            if (this.owner instanceof PackageFacadeContext) {
                mv.visitCode();
                generateStaticDelegateMethodBody(mv, defaultMethod, (PackageFacadeContext) this.owner);
                endVisit(mv, "default method delegation", callableDescriptorToDeclaration(functionDescriptor));
            }
            else {
                generateDefaultImpl(owner, signature, functionDescriptor, isStatic(kind), mv, loadStrategy, function);
            }
        }
    }

    private void generateDefaultImpl(
            @NotNull MethodContext methodContext,
            @NotNull JvmMethodSignature signature,
            @NotNull FunctionDescriptor functionDescriptor,
            boolean isStatic,
            @NotNull MethodVisitor mv,
            @NotNull DefaultParameterValueLoader loadStrategy,
            @Nullable JetNamedFunction function
    ) {
        mv.visitCode();
        generateDefaultImplBody(methodContext, signature, functionDescriptor, isStatic, mv, loadStrategy, function, getParentCodegen(), state);
        endVisit(mv, "default method", callableDescriptorToDeclaration(functionDescriptor));
    }

    public static void generateDefaultImplBody(
            @NotNull MethodContext methodContext,
            @NotNull JvmMethodSignature signature,
            @NotNull FunctionDescriptor functionDescriptor,
            boolean isStatic,
            @NotNull MethodVisitor mv,
            @NotNull DefaultParameterValueLoader loadStrategy,
            @Nullable JetNamedFunction function,
            @NotNull MemberCodegen<?> parentCodegen,
            @NotNull GenerationState state
    ) {
        FrameMap frameMap = createFrameMap(state, functionDescriptor, signature, isStatic);

        ExpressionCodegen codegen = new ExpressionCodegen(mv, frameMap, signature.getReturnType(), methodContext, state, parentCodegen);

        int maskIndex = frameMap.enterTemp(Type.INT_TYPE);

        CallGenerator generator = codegen.getOrCreateCallGenerator(functionDescriptor, function);

        InstructionAdapter iv = new InstructionAdapter(mv);
        loadExplicitArgumentsOnStack(iv, OBJECT_TYPE, isStatic, signature);
        generator.putHiddenParams();

        List<JvmMethodParameterSignature> mappedParameters = signature.getValueParameters();
        int capturedArgumentsCount = 0;
        while (capturedArgumentsCount < mappedParameters.size() &&
               mappedParameters.get(capturedArgumentsCount).getKind() != JvmMethodParameterKind.VALUE) {
            capturedArgumentsCount++;
        }

        List<ValueParameterDescriptor> valueParameters = functionDescriptor.getValueParameters();
        for (int index = 0; index < valueParameters.size(); index++) {
            ValueParameterDescriptor parameterDescriptor = valueParameters.get(index);
            Type type = mappedParameters.get(capturedArgumentsCount + index).getAsmType();

            int parameterIndex = frameMap.getIndex(parameterDescriptor);
            if (parameterDescriptor.declaresDefaultValue()) {
                iv.load(maskIndex, Type.INT_TYPE);
                iv.iconst(1 << index);
                iv.and(Type.INT_TYPE);
                Label loadArg = new Label();
                iv.ifeq(loadArg);

                loadStrategy.putValueOnStack(parameterDescriptor, codegen);

                iv.store(parameterIndex, type);

                iv.mark(loadArg);
            }

            generator.putValueIfNeeded(parameterDescriptor, type, StackValue.local(parameterIndex, type));
        }

        CallableMethod method;
        if (functionDescriptor instanceof ConstructorDescriptor) {
            method = state.getTypeMapper().mapToCallableMethod((ConstructorDescriptor) functionDescriptor);
        }
        else {
            method = state.getTypeMapper().mapToCallableMethod(functionDescriptor, false, methodContext);
        }

        generator.genCallWithoutAssertions(method, codegen);

        iv.areturn(signature.getReturnType());
    }

    @NotNull
    private static FrameMap createFrameMap(
            @NotNull GenerationState state,
            @NotNull FunctionDescriptor function,
            @NotNull JvmMethodSignature signature,
            boolean isStatic
    ) {
        FrameMap frameMap = new FrameMap();
        if (!isStatic) {
            frameMap.enterTemp(OBJECT_TYPE);
        }

        for (JvmMethodParameterSignature parameter : signature.getValueParameters()) {
            if (parameter.getKind() != JvmMethodParameterKind.VALUE) {
                frameMap.enterTemp(parameter.getAsmType());
            }
        }

        for (ValueParameterDescriptor parameter : function.getValueParameters()) {
            frameMap.enter(parameter, state.getTypeMapper().mapType(parameter));
        }

        return frameMap;
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

        for (JvmMethodParameterSignature parameterSignature : signature.getValueParameters()) {
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

    private void generateBridge(
            @Nullable PsiElement origin,
            @NotNull FunctionDescriptor descriptor,
            @NotNull Method bridge,
            @NotNull Method delegateTo
    ) {
        int flags = ACC_PUBLIC | ACC_BRIDGE | ACC_SYNTHETIC; // TODO.

        MethodVisitor mv = v.newMethod(OtherOrigin(descriptor), flags, delegateTo.getName(), bridge.getDescriptor(), null, null);
        if (state.getClassBuilderMode() != ClassBuilderMode.FULL) return;

        mv.visitCode();

        Type[] argTypes = bridge.getArgumentTypes();
        Type[] originalArgTypes = delegateTo.getArgumentTypes();

        InstructionAdapter iv = new InstructionAdapter(mv);
        iv.load(0, OBJECT_TYPE);
        for (int i = 0, reg = 1; i < argTypes.length; i++) {
            StackValue.local(reg, argTypes[i]).put(originalArgTypes[i], iv);
            //noinspection AssignmentToForLoopParameter
            reg += argTypes[i].getSize();
        }

        iv.invokevirtual(v.getThisName(), delegateTo.getName(), delegateTo.getDescriptor());

        StackValue.coerce(delegateTo.getReturnType(), bridge.getReturnType(), iv);
        iv.areturn(bridge.getReturnType());

        endVisit(mv, "bridge method", origin);
    }

    public void genDelegate(FunctionDescriptor functionDescriptor, FunctionDescriptor overriddenDescriptor, StackValue field) {
        genDelegate(functionDescriptor, (ClassDescriptor) overriddenDescriptor.getContainingDeclaration(), field,
                    typeMapper.mapSignature(functionDescriptor),
                    typeMapper.mapSignature(overriddenDescriptor.getOriginal())
        );
    }

    public void genDelegate(
            FunctionDescriptor functionDescriptor,
            final ClassDescriptor toClass,
            final StackValue field,
            final JvmMethodSignature jvmDelegateMethodSignature,
            final JvmMethodSignature jvmOverriddenMethodSignature
    ) {
        generateMethod(
                OtherOrigin(functionDescriptor), jvmDelegateMethodSignature, functionDescriptor,
                new FunctionGenerationStrategy() {
                    @Override
                    public void generateBody(
                            @NotNull MethodVisitor mv,
                            @NotNull FrameMap frameMap,
                            @NotNull JvmMethodSignature signature,
                            @NotNull MethodContext context,
                            @NotNull MemberCodegen<?> parentCodegen
                    ) {
                        Method overriddenMethod = jvmOverriddenMethodSignature.getAsmMethod();
                        Method delegateMethod = jvmDelegateMethodSignature.getAsmMethod();

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
                    }
                }
        );
    }
}
