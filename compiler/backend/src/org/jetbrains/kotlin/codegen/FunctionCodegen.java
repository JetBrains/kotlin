/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.codegen;

import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.psi.PsiElement;
import com.intellij.util.ArrayUtil;
import com.intellij.util.Function;
import com.intellij.util.containers.ContainerUtil;
import kotlin.Function1;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.backend.common.bridges.Bridge;
import org.jetbrains.kotlin.backend.common.bridges.BridgesPackage;
import org.jetbrains.kotlin.builtins.KotlinBuiltIns;
import org.jetbrains.kotlin.codegen.binding.CodegenBinding;
import org.jetbrains.kotlin.codegen.context.CodegenContext;
import org.jetbrains.kotlin.codegen.context.MethodContext;
import org.jetbrains.kotlin.codegen.context.PackageContext;
import org.jetbrains.kotlin.codegen.context.PackageFacadeContext;
import org.jetbrains.kotlin.codegen.optimization.OptimizationMethodVisitor;
import org.jetbrains.kotlin.codegen.state.GenerationState;
import org.jetbrains.kotlin.codegen.state.JetTypeMapper;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.kotlin.load.kotlin.nativeDeclarations.NativeDeclarationsPackage;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.psi.JetClass;
import org.jetbrains.kotlin.psi.JetClassOrObject;
import org.jetbrains.kotlin.psi.JetNamedFunction;
import org.jetbrains.kotlin.psi.JetSecondaryConstructor;
import org.jetbrains.kotlin.resolve.BindingContext;
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils;
import org.jetbrains.kotlin.resolve.DescriptorUtils;
import org.jetbrains.kotlin.resolve.annotations.AnnotationsPackage;
import org.jetbrains.kotlin.resolve.calls.CallResolverUtil;
import org.jetbrains.kotlin.resolve.constants.ArrayValue;
import org.jetbrains.kotlin.resolve.constants.CompileTimeConstant;
import org.jetbrains.kotlin.resolve.constants.JavaClassValue;
import org.jetbrains.kotlin.resolve.jvm.diagnostics.DiagnosticsPackage;
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOrigin;
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodParameterKind;
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodParameterSignature;
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodSignature;
import org.jetbrains.kotlin.types.Approximation;
import org.jetbrains.kotlin.types.TypesPackage;
import org.jetbrains.org.objectweb.asm.AnnotationVisitor;
import org.jetbrains.org.objectweb.asm.Label;
import org.jetbrains.org.objectweb.asm.MethodVisitor;
import org.jetbrains.org.objectweb.asm.Type;
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter;
import org.jetbrains.org.objectweb.asm.commons.Method;
import org.jetbrains.org.objectweb.asm.util.TraceMethodVisitor;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.*;

import static org.jetbrains.kotlin.codegen.AsmUtil.*;
import static org.jetbrains.kotlin.codegen.JvmSerializationBindings.*;
import static org.jetbrains.kotlin.descriptors.CallableMemberDescriptor.Kind.DECLARATION;
import static org.jetbrains.kotlin.load.java.JvmAnnotationNames.OLD_JET_VALUE_PARAMETER_ANNOTATION;
import static org.jetbrains.kotlin.resolve.DescriptorToSourceUtils.getSourceFromDescriptor;
import static org.jetbrains.kotlin.resolve.DescriptorUtils.*;
import static org.jetbrains.kotlin.resolve.jvm.AsmTypes.OBJECT_TYPE;
import static org.jetbrains.kotlin.resolve.jvm.diagnostics.DiagnosticsPackage.*;
import static org.jetbrains.org.objectweb.asm.Opcodes.*;

public class FunctionCodegen {
    public final GenerationState state;
    private final JetTypeMapper typeMapper;
    private final BindingContext bindingContext;
    private final CodegenContext owner;
    private final ClassBuilder v;
    private final MemberCodegen<?> memberCodegen;

    public FunctionCodegen(
            @NotNull CodegenContext owner,
            @NotNull ClassBuilder v,
            @NotNull GenerationState state,
            @NotNull MemberCodegen<?> memberCodegen
    ) {
        this.owner = owner;
        this.v = v;
        this.state = state;
        this.typeMapper = state.getTypeMapper();
        this.bindingContext = state.getBindingContext();
        this.memberCodegen = memberCodegen;
    }

    public void gen(@NotNull JetNamedFunction function) {
        SimpleFunctionDescriptor functionDescriptor = bindingContext.get(BindingContext.FUNCTION, function);
        assert functionDescriptor != null : "No descriptor for function " + function.getText() + "\n" +
                                            "in " + function.getContainingFile().getVirtualFile();

        if (owner.getContextKind() != OwnerKind.TRAIT_IMPL || function.hasBody()) {
            generateMethod(OtherOrigin(function, functionDescriptor), functionDescriptor,
                           new FunctionGenerationStrategy.FunctionDefault(state, functionDescriptor, function));
        }

        generateDefaultIfNeeded(owner.intoFunction(functionDescriptor), functionDescriptor, owner.getContextKind(),
                                DefaultParameterValueLoader.DEFAULT, function);
    }

    public void generateMethod(
            @NotNull JvmDeclarationOrigin origin,
            @NotNull FunctionDescriptor descriptor,
            @NotNull FunctionGenerationStrategy strategy
    ) {
        generateMethod(origin, descriptor, owner.intoFunction(descriptor), strategy);
    }

    public void generateMethod(
            @NotNull JvmDeclarationOrigin origin,
            @NotNull FunctionDescriptor functionDescriptor,
            @NotNull MethodContext methodContext,
            @NotNull FunctionGenerationStrategy strategy
    ) {
        OwnerKind contextKind = methodContext.getContextKind();
        JvmMethodSignature jvmSignature = typeMapper.mapSignature(functionDescriptor, contextKind);
        Method asmMethod = jvmSignature.getAsmMethod();

        int flags = getMethodAsmFlags(functionDescriptor, contextKind);
        boolean isNative = NativeDeclarationsPackage.hasNativeAnnotation(functionDescriptor);

        if (isNative && owner instanceof PackageContext && !(owner instanceof PackageFacadeContext)) {
            // Native methods are only defined in package facades and do not need package part implementations
            return;
        }
        MethodVisitor mv = v.newMethod(origin,
                                       flags,
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

        if (state.getClassBuilderMode() != ClassBuilderMode.LIGHT_CLASSES) {
            generateJetValueParameterAnnotations(mv, functionDescriptor, jvmSignature);
        }

        generateBridges(functionDescriptor);

        boolean staticInDefaultObject = AnnotationsPackage.isPlatformStaticInDefaultObject(functionDescriptor);
        if (staticInDefaultObject) {
            ImplementationBodyCodegen parentBodyCodegen = (ImplementationBodyCodegen) memberCodegen.getParentCodegen();
            parentBodyCodegen.addAdditionalTask(new PlatformStaticGenerator(functionDescriptor, origin, state));
        }

        if (state.getClassBuilderMode() == ClassBuilderMode.LIGHT_CLASSES || isAbstractMethod(functionDescriptor, contextKind)) {
            generateLocalVariableTable(
                    mv,
                    jvmSignature,
                    functionDescriptor,
                    getThisTypeForFunction(functionDescriptor, methodContext, typeMapper),
                    new Label(),
                    new Label(),
                    contextKind
            );

            mv.visitEnd();
            return;
        }

        if (!isNative) {
            generateMethodBody(mv, functionDescriptor, methodContext, jvmSignature, strategy, memberCodegen);
        }
        else if (staticInDefaultObject) {
            // native platformStatic foo() in default object should delegate to the static native function moved to the outer class
            mv.visitCode();
            FunctionDescriptor staticFunctionDescriptor = PlatformStaticGenerator.createStaticFunctionDescriptor(functionDescriptor);
            JvmMethodSignature jvmMethodSignature =
                    typeMapper.mapSignature(memberCodegen.getContext().accessibleFunctionDescriptor(staticFunctionDescriptor));
            Type owningType = typeMapper.mapClass((ClassifierDescriptor) staticFunctionDescriptor.getContainingDeclaration());
            generateDelegateToMethodBody(false, mv, jvmMethodSignature.getAsmMethod(), owningType.getInternalName());
        }

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
                nullableType = descriptor.getType().isMarkedNullable();
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
                    ReceiverParameterDescriptor receiver = functionDescriptor.getExtensionReceiverParameter();
                    nullableType = receiver == null || receiver.getType().isMarkedNullable();
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
        ReceiverParameterDescriptor dispatchReceiver = functionDescriptor.getDispatchReceiverParameter();
        if (functionDescriptor instanceof ConstructorDescriptor) {
            return typeMapper.mapType(functionDescriptor);
        }
        else if (dispatchReceiver != null) {
            return typeMapper.mapType(dispatchReceiver.getType());
        }
        else if (isFunctionLiteral(functionDescriptor) ||
                 isLocalFunction(functionDescriptor) ||
                 isFunctionExpression(functionDescriptor)
                ) {
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

        Label methodEnd;
        if (context.getParentContext() instanceof PackageFacadeContext) {
            generatePackageDelegateMethodBody(mv, signature.getAsmMethod(), (PackageFacadeContext) context.getParentContext());
            methodEnd = new Label();
        }
        else {
            FrameMap frameMap = createFrameMap(parentCodegen.state, functionDescriptor, signature, isStaticMethod(context.getContextKind(),
                                                                                                                  functionDescriptor));

            Label methodEntry = new Label();
            mv.visitLabel(methodEntry);
            context.setMethodStartLabel(methodEntry);

            if (!JetTypeMapper.isAccessor(functionDescriptor)) {
                genNotNullAssertionsForParameters(new InstructionAdapter(mv), parentCodegen.state, functionDescriptor, frameMap);
            }
            methodEnd = new Label();
            context.setMethodEndLabel(methodEnd);
            strategy.generateBody(mv, frameMap, signature, context, parentCodegen);
        }

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

    private static void generatePackageDelegateMethodBody(
            @NotNull MethodVisitor mv,
            @NotNull Method asmMethod,
            @NotNull PackageFacadeContext context
    ) {
        generateDelegateToMethodBody(true, mv, asmMethod, context.getDelegateToClassType().getInternalName());
    }

    private static void generateDelegateToMethodBody(
            boolean isStatic,
            @NotNull MethodVisitor mv,
            @NotNull Method asmMethod,
            @NotNull String classToDelegateTo
    ) {
        InstructionAdapter iv = new InstructionAdapter(mv);
        Type[] argTypes = asmMethod.getArgumentTypes();

        // The first line of some package file is written to the line number attribute of a static delegate to allow to 'step into' it
        // This is similar to what javac does with bridge methods
        Label label = new Label();
        iv.visitLabel(label);
        iv.visitLineNumber(1, label);

        int k = isStatic ? 0 : 1;
        for (Type argType : argTypes) {
            iv.load(k, argType);
            k += argType.getSize();
        }
        iv.invokestatic(classToDelegateTo, asmMethod.getName(), asmMethod.getDescriptor(), false);
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
            PsiElement origin = descriptor.getKind() == DECLARATION ? getSourceFromDescriptor(descriptor) : null;
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
    public static String[] getThrownExceptions(@NotNull FunctionDescriptor function, @NotNull final JetTypeMapper mapper) {
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
            @NotNull ClassBuilder classBuilder,
            @NotNull JetClassOrObject classOrObject
    ) {
        if (!isEmptyConstructorNeeded(state.getBindingContext(), constructorDescriptor, classOrObject)) {
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
        List<Integer> masks = new ArrayList<Integer>(1);
        for (ValueParameterDescriptor parameterDescriptor : constructorDescriptor.getValueParameters()) {
            Type paramType = state.getTypeMapper().mapType(parameterDescriptor.getType());
            pushDefaultValueOnStack(paramType, v);
            int i = parameterDescriptor.getIndex();
            if (i != 0 && i % Integer.SIZE == 0) {
                masks.add(mask);
                mask = 0;
            }
            mask |= (1 << (i % Integer.SIZE));
        }
        masks.add(mask);
        for (int m : masks) {
            v.iconst(m);
        }

        // constructors with default arguments has last synthetic argument of specific type
        v.aconst(null);

        String desc = JetTypeMapper.getDefaultDescriptor(method.getAsmMethod(), false);
        v.invokespecial(methodOwner.getInternalName(), "<init>", desc, false);
        v.areturn(Type.VOID_TYPE);
        endVisit(mv, "default constructor for " + methodOwner.getInternalName(), classOrObject);
    }

    void generateDefaultIfNeeded(
            @NotNull MethodContext owner,
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

        int flags = getVisibilityAccessFlag(functionDescriptor) |
                    getDeprecatedAccessFlag(functionDescriptor) |
                    (functionDescriptor instanceof ConstructorDescriptor ? 0 : ACC_STATIC);

        Method defaultMethod = typeMapper.mapDefaultMethod(functionDescriptor, kind, owner);

        MethodVisitor mv = v.newMethod(
                Synthetic(function, functionDescriptor),
                flags,
                defaultMethod.getName(),
                defaultMethod.getDescriptor(), null,
                getThrownExceptions(functionDescriptor, typeMapper)
        );

        if (state.getClassBuilderMode() == ClassBuilderMode.FULL) {
            if (this.owner instanceof PackageFacadeContext) {
                mv.visitCode();
                generatePackageDelegateMethodBody(mv, defaultMethod, (PackageFacadeContext) this.owner);
                endVisit(mv, "default method delegation", getSourceFromDescriptor(functionDescriptor));
            }
            else {
                mv.visitCode();
                generateDefaultImplBody(owner, functionDescriptor, mv, loadStrategy, function, memberCodegen);
                endVisit(mv, "default method", getSourceFromDescriptor(functionDescriptor));
            }
        }
    }

    public static void generateDefaultImplBody(
            @NotNull MethodContext methodContext,
            @NotNull FunctionDescriptor functionDescriptor,
            @NotNull MethodVisitor mv,
            @NotNull DefaultParameterValueLoader loadStrategy,
            @Nullable JetNamedFunction function,
            @NotNull MemberCodegen<?> parentCodegen
    ) {
        GenerationState state = parentCodegen.state;
        JvmMethodSignature signature = state.getTypeMapper().mapSignature(functionDescriptor, methodContext.getContextKind());

        boolean isStatic = isStaticMethod(methodContext.getContextKind(), functionDescriptor);
        FrameMap frameMap = createFrameMap(state, functionDescriptor, signature, isStatic);

        ExpressionCodegen codegen = new ExpressionCodegen(mv, frameMap, signature.getReturnType(), methodContext, state, parentCodegen);

        CallGenerator generator = codegen.getOrCreateCallGenerator(functionDescriptor, function);

        loadExplicitArgumentsOnStack(OBJECT_TYPE, isStatic, signature, generator);

        List<JvmMethodParameterSignature> mappedParameters = signature.getValueParameters();
        int capturedArgumentsCount = 0;
        while (capturedArgumentsCount < mappedParameters.size() &&
               mappedParameters.get(capturedArgumentsCount).getKind() != JvmMethodParameterKind.VALUE) {
            capturedArgumentsCount++;
        }

        InstructionAdapter iv = new InstructionAdapter(mv);

        int maskIndex = 0;
        List<ValueParameterDescriptor> valueParameters = functionDescriptor.getValueParameters();
        for (int index = 0; index < valueParameters.size(); index++) {
            if (index % Integer.SIZE == 0) {
                maskIndex = frameMap.enterTemp(Type.INT_TYPE);
            }
            ValueParameterDescriptor parameterDescriptor = valueParameters.get(index);
            Type type = mappedParameters.get(capturedArgumentsCount + index).getAsmType();

            int parameterIndex = frameMap.getIndex(parameterDescriptor);
            if (parameterDescriptor.declaresDefaultValue()) {
                iv.load(maskIndex, Type.INT_TYPE);
                iv.iconst(1 << (index % Integer.SIZE));
                iv.and(Type.INT_TYPE);
                Label loadArg = new Label();
                iv.ifeq(loadArg);

                StackValue.local(parameterIndex, type).store(loadStrategy.genValue(parameterDescriptor, codegen), iv);

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
            if (parameter.getKind() == JvmMethodParameterKind.RECEIVER) {
                ReceiverParameterDescriptor receiverParameter = function.getExtensionReceiverParameter();
                if (receiverParameter != null) {
                    frameMap.enter(receiverParameter, state.getTypeMapper().mapType(receiverParameter));
                }
            }
            else if (parameter.getKind() != JvmMethodParameterKind.VALUE) {
                frameMap.enterTemp(parameter.getAsmType());
            }
        }

        for (ValueParameterDescriptor parameter : function.getValueParameters()) {
            frameMap.enter(parameter, state.getTypeMapper().mapType(parameter));
        }

        return frameMap;
    }

    private static void loadExplicitArgumentsOnStack(
            @NotNull Type ownerType,
            boolean isStatic,
            @NotNull JvmMethodSignature signature,
            @NotNull CallGenerator callGenerator
    ) {
        int var = 0;
        if (!isStatic) {
            callGenerator.putValueIfNeeded(null, ownerType, StackValue.local(var, ownerType));
            var += ownerType.getSize();
        }

        for (JvmMethodParameterSignature parameterSignature : signature.getValueParameters()) {
            if (parameterSignature.getKind() != JvmMethodParameterKind.VALUE) {
                Type type = parameterSignature.getAsmType();
                callGenerator.putValueIfNeeded(null, type, StackValue.local(var, type));
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

    private static boolean isEmptyConstructorNeeded(
            @NotNull BindingContext context,
            @NotNull ConstructorDescriptor constructorDescriptor,
            @NotNull JetClassOrObject classOrObject
    ) {
        ClassDescriptor classDescriptor = constructorDescriptor.getContainingDeclaration();

        if (classOrObject.isLocal()) return false;

        if (CodegenBinding.canHaveOuter(context, classDescriptor)) return false;

        if (Visibilities.isPrivate(classDescriptor.getVisibility()) ||
            Visibilities.isPrivate(constructorDescriptor.getVisibility())) return false;

        if (constructorDescriptor.getValueParameters().isEmpty()) return false;
        if (classOrObject instanceof JetClass && hasSecondaryConstructorsWithNoParameters((JetClass) classOrObject)) return false;

        for (ValueParameterDescriptor parameterDescriptor : constructorDescriptor.getValueParameters()) {
            if (!parameterDescriptor.declaresDefaultValue()) {
                return false;
            }
        }
        return true;
    }

    private static boolean hasSecondaryConstructorsWithNoParameters(@NotNull JetClass klass) {
        for (JetSecondaryConstructor constructor : klass.getSecondaryConstructors()) {
            if (constructor.getValueParameters().isEmpty()) return true;
        }
        return false;
    }

    private void generateBridge(
            @Nullable PsiElement origin,
            @NotNull FunctionDescriptor descriptor,
            @NotNull Method bridge,
            @NotNull Method delegateTo
    ) {
        int flags = ACC_PUBLIC | ACC_BRIDGE | ACC_SYNTHETIC; // TODO.

        MethodVisitor mv = v.newMethod(DiagnosticsPackage.Bridge(descriptor, origin), flags, delegateTo.getName(), bridge.getDescriptor(), null, null);
        if (state.getClassBuilderMode() != ClassBuilderMode.FULL) return;

        mv.visitCode();

        Type[] argTypes = bridge.getArgumentTypes();
        Type[] originalArgTypes = delegateTo.getArgumentTypes();

        InstructionAdapter iv = new InstructionAdapter(mv);
        ImplementationBodyCodegen.markLineNumberForSyntheticFunction(owner.getThisDescriptor(), iv);

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

    public void genDelegate(@NotNull FunctionDescriptor functionDescriptor, FunctionDescriptor overriddenDescriptor, StackValue field) {
        genDelegate(functionDescriptor, overriddenDescriptor.getOriginal(), (ClassDescriptor) overriddenDescriptor.getContainingDeclaration(), field);
    }

    public void genDelegate(
            @NotNull final FunctionDescriptor delegateFunction,
            final FunctionDescriptor delegatedTo,
            final ClassDescriptor toClass,
            final StackValue field
    ) {
        generateMethod(
                Delegation(DescriptorToSourceUtils.descriptorToDeclaration(delegatedTo), delegateFunction), delegateFunction,
                new FunctionGenerationStrategy() {
                    @Override
                    public void generateBody(
                            @NotNull MethodVisitor mv,
                            @NotNull FrameMap frameMap,
                            @NotNull JvmMethodSignature signature,
                            @NotNull MethodContext context,
                            @NotNull MemberCodegen<?> parentCodegen
                    ) {
                        Method delegateToMethod = typeMapper.mapSignature(delegatedTo).getAsmMethod();
                        Method delegateMethod = typeMapper.mapSignature(delegateFunction).getAsmMethod();

                        Type[] argTypes = delegateMethod.getArgumentTypes();
                        Type[] originalArgTypes = delegateToMethod.getArgumentTypes();

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
                            iv.invokeinterface(internalName, delegateToMethod.getName(), delegateToMethod.getDescriptor());
                        }
                        else {
                            iv.invokevirtual(internalName, delegateToMethod.getName(), delegateToMethod.getDescriptor());
                        }

                        StackValue stackValue = AsmUtil.genNotNullAssertions(
                                state,
                                StackValue.onStack(delegateToMethod.getReturnType()),
                                TypesPackage.getApproximationTo(
                                        delegatedTo.getReturnType(),
                                        delegateFunction.getReturnType(),
                                        new Approximation.DataFlowExtras.OnlyMessage(delegatedTo.getName() + "(...)")
                                )
                        );

                        stackValue.put(delegateMethod.getReturnType(), iv);

                        iv.areturn(delegateMethod.getReturnType());
                    }
                }
        );
    }
}
