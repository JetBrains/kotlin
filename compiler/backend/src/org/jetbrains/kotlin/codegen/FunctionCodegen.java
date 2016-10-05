/*
 * Copyright 2010-2016 JetBrains s.r.o.
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
import kotlin.collections.CollectionsKt;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import kotlin.text.StringsKt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.backend.common.bridges.Bridge;
import org.jetbrains.kotlin.backend.common.bridges.ImplKt;
import org.jetbrains.kotlin.codegen.annotation.AnnotatedWithOnlyTargetedAnnotations;
import org.jetbrains.kotlin.codegen.context.*;
import org.jetbrains.kotlin.codegen.state.GenerationState;
import org.jetbrains.kotlin.codegen.state.KotlinTypeMapper;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.descriptors.annotations.Annotated;
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget;
import org.jetbrains.kotlin.descriptors.impl.ValueParameterDescriptorImpl;
import org.jetbrains.kotlin.load.java.BuiltinMethodsWithSpecialGenericSignature;
import org.jetbrains.kotlin.load.java.JvmAbi;
import org.jetbrains.kotlin.load.java.SpecialBuiltinMembers;
import org.jetbrains.kotlin.load.java.descriptors.JavaClassDescriptor;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.psi.KtElement;
import org.jetbrains.kotlin.psi.KtFunction;
import org.jetbrains.kotlin.psi.KtNamedFunction;
import org.jetbrains.kotlin.resolve.BindingContext;
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils;
import org.jetbrains.kotlin.resolve.DescriptorUtils;
import org.jetbrains.kotlin.resolve.annotations.AnnotationUtilKt;
import org.jetbrains.kotlin.resolve.constants.ArrayValue;
import org.jetbrains.kotlin.resolve.constants.ConstantValue;
import org.jetbrains.kotlin.resolve.constants.KClassValue;
import org.jetbrains.kotlin.resolve.inline.InlineUtil;
import org.jetbrains.kotlin.resolve.jvm.AsmTypes;
import org.jetbrains.kotlin.resolve.jvm.RuntimeAssertionInfo;
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOrigin;
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOriginKind;
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOriginKt;
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodGenericSignature;
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodParameterKind;
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodParameterSignature;
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodSignature;
import org.jetbrains.kotlin.types.KotlinType;
import org.jetbrains.kotlin.types.TypeUtils;
import org.jetbrains.org.objectweb.asm.*;
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter;
import org.jetbrains.org.objectweb.asm.commons.Method;
import org.jetbrains.org.objectweb.asm.util.TraceMethodVisitor;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import static org.jetbrains.kotlin.builtins.KotlinBuiltIns.isNullableAny;
import static org.jetbrains.kotlin.codegen.AsmUtil.*;
import static org.jetbrains.kotlin.codegen.JvmCodegenUtil.isAnnotationOrJvm6Interface;
import static org.jetbrains.kotlin.codegen.JvmCodegenUtil.isJvm8Interface;
import static org.jetbrains.kotlin.codegen.JvmCodegenUtil.isJvm8InterfaceMember;
import static org.jetbrains.kotlin.codegen.serialization.JvmSerializationBindings.METHOD_FOR_FUNCTION;
import static org.jetbrains.kotlin.descriptors.CallableMemberDescriptor.Kind.DECLARATION;
import static org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget.*;
import static org.jetbrains.kotlin.resolve.DescriptorToSourceUtils.getSourceFromDescriptor;
import static org.jetbrains.kotlin.resolve.DescriptorUtils.*;
import static org.jetbrains.kotlin.resolve.jvm.AsmTypes.OBJECT_TYPE;
import static org.jetbrains.kotlin.types.expressions.ExpressionTypingUtils.*;
import static org.jetbrains.org.objectweb.asm.Opcodes.*;

public class FunctionCodegen {
    public final GenerationState state;
    private final KotlinTypeMapper typeMapper;
    private final BindingContext bindingContext;
    private final CodegenContext owner;
    private final ClassBuilder v;
    private final MemberCodegen<?> memberCodegen;

    private final Function1<DeclarationDescriptor, Boolean> IS_PURE_INTERFACE_CHECKER = new Function1<DeclarationDescriptor, Boolean>() {
        @Override
        public Boolean invoke(DeclarationDescriptor descriptor) {
            return JvmCodegenUtil.isAnnotationOrJvm6Interface(descriptor, state);
        }
    };

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

    public void gen(@NotNull KtNamedFunction function) {
        SimpleFunctionDescriptor functionDescriptor = bindingContext.get(BindingContext.FUNCTION, function);
        if (functionDescriptor == null) {
            throw ExceptionLogger.logDescriptorNotFound("No descriptor for function " + function.getName(), function);
        }

        if (owner.getContextKind() != OwnerKind.DEFAULT_IMPLS || function.hasBody()) {
            generateMethod(JvmDeclarationOriginKt.OtherOrigin(function, functionDescriptor), functionDescriptor,
                           new FunctionGenerationStrategy.FunctionDefault(state, function));
        }

        generateDefaultIfNeeded(owner.intoFunction(functionDescriptor), functionDescriptor, owner.getContextKind(),
                                DefaultParameterValueLoader.DEFAULT, function);

        generateOverloadsWithDefaultValues(function, functionDescriptor, functionDescriptor);
    }

    public void generateOverloadsWithDefaultValues(
            @Nullable KtNamedFunction function,
            @NotNull FunctionDescriptor functionDescriptor,
            @NotNull FunctionDescriptor delegateFunctionDescriptor
    ) {
        new DefaultParameterValueSubstitutor(state).generateOverloadsIfNeeded(
                function, functionDescriptor, delegateFunctionDescriptor, owner.getContextKind(), v, memberCodegen
        );
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
        if (isInterface(functionDescriptor.getContainingDeclaration()) &&
            functionDescriptor.getVisibility() == Visibilities.PRIVATE &&
            !processInterfaceMember(functionDescriptor, contextKind, state)) {
            return;
        }

        JvmMethodGenericSignature jvmSignature = typeMapper.mapSignatureWithGeneric(functionDescriptor, contextKind);
        Method asmMethod = jvmSignature.getAsmMethod();

        int flags = getMethodAsmFlags(functionDescriptor, contextKind, state);

        if (origin.getOriginKind() == JvmDeclarationOriginKind.SAM_DELEGATION) {
            flags |= ACC_SYNTHETIC;
        }

        if (functionDescriptor.isExternal() && owner instanceof MultifileClassFacadeContext) {
            // Native methods are only defined in facades and do not need package part implementations
            return;
        }
        MethodVisitor mv = v.newMethod(origin,
                                       flags,
                                       asmMethod.getName(),
                                       asmMethod.getDescriptor(),
                                       jvmSignature.getGenericsSignature(),
                                       getThrownExceptions(functionDescriptor, typeMapper));

        if (CodegenContextUtil.isImplClassOwner(owner)) {
            v.getSerializationBindings().put(METHOD_FOR_FUNCTION, functionDescriptor, asmMethod);
        }

        generateMethodAnnotations(functionDescriptor, asmMethod, mv);

        generateParameterAnnotations(functionDescriptor, mv, typeMapper.mapSignatureSkipGeneric(functionDescriptor));

        generateBridges(functionDescriptor);

        if (isJvm8InterfaceMember(functionDescriptor, state) && contextKind != OwnerKind.DEFAULT_IMPLS && state.getGenerateDefaultImplsForJvm8()) {
            generateDelegateForDefaultImpl(functionDescriptor, origin.getElement());
        }

        boolean staticInCompanionObject = AnnotationUtilKt.isPlatformStaticInCompanionObject(functionDescriptor);
        if (staticInCompanionObject) {
            ImplementationBodyCodegen parentBodyCodegen = (ImplementationBodyCodegen) memberCodegen.getParentCodegen();
            parentBodyCodegen.addAdditionalTask(new JvmStaticGenerator(functionDescriptor, origin, state, parentBodyCodegen));
        }

        if (!state.getClassBuilderMode().generateBodies || isAbstractMethod(functionDescriptor, contextKind, state)) {
            generateLocalVariableTable(
                    mv,
                    jvmSignature,
                    functionDescriptor,
                    getThisTypeForFunction(functionDescriptor, methodContext, typeMapper),
                    new Label(),
                    new Label(),
                    contextKind,
                    typeMapper
            );

            mv.visitEnd();
            return;
        }

        if (!functionDescriptor.isExternal()) {
            generateMethodBody(mv, functionDescriptor, methodContext, jvmSignature, strategy, memberCodegen);
        }
        else if (staticInCompanionObject) {
            // native @JvmStatic foo() in companion object should delegate to the static native function moved to the outer class
            mv.visitCode();
            FunctionDescriptor staticFunctionDescriptor = JvmStaticGenerator.createStaticFunctionDescriptor(functionDescriptor);
            Method accessorMethod =
                    typeMapper.mapAsmMethod(memberCodegen.getContext().accessibleDescriptor(staticFunctionDescriptor, null));
            Type owningType = typeMapper.mapClass((ClassifierDescriptor) staticFunctionDescriptor.getContainingDeclaration());
            generateDelegateToStaticMethodBody(false, mv, accessorMethod, owningType.getInternalName());
        }

        endVisit(mv, null, origin.getElement());
    }

    private void generateDelegateForDefaultImpl(
            @NotNull final FunctionDescriptor functionDescriptor,
            @Nullable PsiElement element
    ) {
        Method defaultImplMethod = typeMapper.mapAsmMethod(functionDescriptor, OwnerKind.DEFAULT_IMPLS);

        CodegenUtilKt.generateMethod(
                v, "Default Impl delegate in interface", Opcodes.ACC_SYNTHETIC | Opcodes.ACC_STATIC | Opcodes.ACC_PUBLIC,
                new Method(defaultImplMethod.getName() + JvmAbi.DEFAULT_IMPLS_DELEGATE_SUFFIX, defaultImplMethod.getDescriptor()),
                element, JvmDeclarationOrigin.NO_ORIGIN,
                state, new Function1<InstructionAdapter, Unit>() {
                    @Override
                    public Unit invoke(InstructionAdapter adapter) {
                        Method interfaceMethod = typeMapper.mapAsmMethod(functionDescriptor, OwnerKind.IMPLEMENTATION);
                        Type type = typeMapper.mapOwner(functionDescriptor);
                        generateDelegateToMethodBody(
                                -1, adapter,
                                interfaceMethod,
                                type.getInternalName(),
                                Opcodes.INVOKESPECIAL,
                                true
                        );
                        return null;
                    }
                }
        );
    }

    private void generateMethodAnnotations(
            @NotNull FunctionDescriptor functionDescriptor,
            Method asmMethod,
            MethodVisitor mv
    ) {
        AnnotationCodegen annotationCodegen = AnnotationCodegen.forMethod(mv, memberCodegen, typeMapper);

        if (functionDescriptor instanceof PropertyAccessorDescriptor) {
            AnnotationUseSiteTarget target = functionDescriptor instanceof PropertySetterDescriptor ? PROPERTY_SETTER : PROPERTY_GETTER;
            annotationCodegen.genAnnotations(functionDescriptor, asmMethod.getReturnType(), target);
        }
        else {
            annotationCodegen.genAnnotations(functionDescriptor, asmMethod.getReturnType());
        }
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
                AnnotationCodegen annotationCodegen = AnnotationCodegen.forParameter(i, mv, memberCodegen, typeMapper);

                if (functionDescriptor instanceof PropertySetterDescriptor) {
                    PropertyDescriptor propertyDescriptor = ((PropertySetterDescriptor) functionDescriptor).getCorrespondingProperty();
                    Annotated targetedAnnotations = new AnnotatedWithOnlyTargetedAnnotations(propertyDescriptor);
                    annotationCodegen.genAnnotations(targetedAnnotations, parameterSignature.getAsmType(), SETTER_PARAMETER);
                }

                if (functionDescriptor instanceof ConstructorDescriptor) {
                    annotationCodegen.genAnnotations(parameter, parameterSignature.getAsmType(), CONSTRUCTOR_PARAMETER);
                }
                else {
                    annotationCodegen.genAnnotations(parameter, parameterSignature.getAsmType());
                }
            }
            else if (kind == JvmMethodParameterKind.RECEIVER) {
                ReceiverParameterDescriptor receiver = JvmCodegenUtil.getDirectMember(functionDescriptor).getExtensionReceiverParameter();

                if (receiver != null) {
                    AnnotationCodegen annotationCodegen = AnnotationCodegen.forParameter(i, mv, memberCodegen, typeMapper);
                    Annotated targetedAnnotations = new AnnotatedWithOnlyTargetedAnnotations(receiver.getType());
                    annotationCodegen.genAnnotations(targetedAnnotations, parameterSignature.getAsmType(), RECEIVER);

                    annotationCodegen.genAnnotations(receiver, parameterSignature.getAsmType());
                }
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
    private static Type getThisTypeForFunction(
            @NotNull FunctionDescriptor functionDescriptor,
            @NotNull MethodContext context,
            @NotNull KotlinTypeMapper typeMapper
    ) {
        ReceiverParameterDescriptor dispatchReceiver = functionDescriptor.getDispatchReceiverParameter();
        if (functionDescriptor instanceof ConstructorDescriptor) {
            return typeMapper.mapType(functionDescriptor);
        }
        else if (dispatchReceiver != null) {
            return typeMapper.mapType(dispatchReceiver.getType());
        }
        else if (isFunctionLiteral(functionDescriptor) ||
                 isLocalFunction(functionDescriptor) ||
                 isFunctionExpression(functionDescriptor)) {
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

        KotlinTypeMapper typeMapper = parentCodegen.typeMapper;
        if (BuiltinSpecialBridgesUtil.shouldHaveTypeSafeBarrier(functionDescriptor, getSignatureMapper(typeMapper))) {
            generateTypeCheckBarrierIfNeeded(
                    new InstructionAdapter(mv), functionDescriptor, signature.getReturnType(), /* delegateParameterTypes = */null);
        }

        Label methodEnd;

        int functionFakeIndex = -1;
        int lambdaFakeIndex = -1;

        if (context.getParentContext() instanceof MultifileClassFacadeContext) {
            generateFacadeDelegateMethodBody(mv, signature.getAsmMethod(), (MultifileClassFacadeContext) context.getParentContext());
            methodEnd = new Label();
        }
        else if (OwnerKind.DEFAULT_IMPLS == context.getContextKind() && isJvm8InterfaceMember(functionDescriptor, parentCodegen.state)) {
            int flags = AsmUtil.getMethodAsmFlags(functionDescriptor, OwnerKind.DEFAULT_IMPLS, context.getState());
            assert (flags & Opcodes.ACC_ABSTRACT) == 0 : "Interface method with body should be non-abstract" + functionDescriptor;
            Type type = typeMapper.mapOwner(functionDescriptor);
            Method asmMethod = typeMapper.mapAsmMethod(functionDescriptor, OwnerKind.DEFAULT_IMPLS);
            generateDelegateToStaticMethodBody(
                    true, mv,
                    new Method(asmMethod.getName() + JvmAbi.DEFAULT_IMPLS_DELEGATE_SUFFIX, asmMethod.getDescriptor()),
                    type.getInternalName()
            );
            methodEnd = new Label();
        }
        else {
            FrameMap frameMap = createFrameMap(parentCodegen.state, functionDescriptor, signature, isStaticMethod(context.getContextKind(),
                                                                                                                  functionDescriptor));
            if (context.isInlineMethodContext()) {
                functionFakeIndex = frameMap.enterTemp(Type.INT_TYPE);
            }

            if (context instanceof InlineLambdaContext) {
                lambdaFakeIndex = frameMap.enterTemp(Type.INT_TYPE);
            }

            Label methodEntry = new Label();
            mv.visitLabel(methodEntry);
            context.setMethodStartLabel(methodEntry);

            if (!KotlinTypeMapper.isAccessor(functionDescriptor)) {
                genNotNullAssertionsForParameters(new InstructionAdapter(mv), parentCodegen.state, functionDescriptor, frameMap);
            }

            parentCodegen.beforeMethodBody(mv);

            methodEnd = new Label();
            context.setMethodEndLabel(methodEnd);
            strategy.generateBody(mv, frameMap, signature, context, parentCodegen);
        }

        mv.visitLabel(methodEnd);

        Type thisType = getThisTypeForFunction(functionDescriptor, context, typeMapper);
        generateLocalVariableTable(
                mv, signature, functionDescriptor, thisType, methodBegin, methodEnd, context.getContextKind(), typeMapper);

        if (context.isInlineMethodContext() && functionFakeIndex != -1) {
            mv.visitLocalVariable(
                    JvmAbi.LOCAL_VARIABLE_NAME_PREFIX_INLINE_FUNCTION + functionDescriptor.getName().asString(),
                    Type.INT_TYPE.getDescriptor(), null,
                    methodBegin, methodEnd,
                    functionFakeIndex);
        }

        if (context instanceof InlineLambdaContext && thisType != null && lambdaFakeIndex != -1) {
            String name = thisType.getClassName();
            int indexOfLambdaOrdinal = name.lastIndexOf("$");
            if (indexOfLambdaOrdinal > 0) {
                int lambdaOrdinal = Integer.parseInt(name.substring(indexOfLambdaOrdinal + 1));

                KtElement functionArgument = parentCodegen.element;
                String functionName = "unknown";
                if (functionArgument instanceof KtFunction) {
                    ValueParameterDescriptor inlineArgumentDescriptor =
                            InlineUtil.getInlineArgumentDescriptor((KtFunction) functionArgument, parentCodegen.bindingContext);
                    if (inlineArgumentDescriptor != null) {
                        functionName = inlineArgumentDescriptor.getContainingDeclaration().getName().asString();
                    }
                }
                mv.visitLocalVariable(
                        JvmAbi.LOCAL_VARIABLE_NAME_PREFIX_INLINE_ARGUMENT + lambdaOrdinal +  "$" + functionName,
                        Type.INT_TYPE.getDescriptor(), null,
                        methodBegin, methodEnd,
                        lambdaFakeIndex);
            }
        }
    }

    private static void generateLocalVariableTable(
            @NotNull MethodVisitor mv,
            @NotNull JvmMethodSignature jvmMethodSignature,
            @NotNull FunctionDescriptor functionDescriptor,
            @Nullable Type thisType,
            @NotNull Label methodBegin,
            @NotNull Label methodEnd,
            @NotNull OwnerKind ownerKind,
            @NotNull KotlinTypeMapper typeMapper
    ) {
        generateLocalVariablesForParameters(mv, jvmMethodSignature, thisType, methodBegin, methodEnd,
                                            functionDescriptor.getValueParameters(),
                                            AsmUtil.isStaticMethod(ownerKind, functionDescriptor), typeMapper);
    }

    public static void generateLocalVariablesForParameters(
            @NotNull MethodVisitor mv,
            @NotNull JvmMethodSignature jvmMethodSignature,
            @Nullable Type thisType,
            @NotNull Label methodBegin,
            @NotNull Label methodEnd,
            Collection<ValueParameterDescriptor> valueParameters,
            boolean isStatic,
            KotlinTypeMapper typeMapper
    ) {
        Iterator<ValueParameterDescriptor> valueParameterIterator = valueParameters.iterator();
        List<JvmMethodParameterSignature> params = jvmMethodSignature.getValueParameters();
        int shift = 0;

        if (!isStatic) {
            //add this
            if (thisType != null) {
                mv.visitLocalVariable("this", thisType.getDescriptor(), null, methodBegin, methodEnd, shift);
            }
            else {
                //TODO: provide thisType for callable reference
            }
            shift++;
        }

        for (int i = 0; i < params.size(); i++) {
            JvmMethodParameterSignature param = params.get(i);
            JvmMethodParameterKind kind = param.getKind();
            String parameterName;

            if (kind == JvmMethodParameterKind.VALUE) {
                ValueParameterDescriptor parameter = valueParameterIterator.next();
                List<VariableDescriptor> destructuringVariables = ValueParameterDescriptorImpl.getDestructuringVariablesOrNull(parameter);

                parameterName =
                        destructuringVariables == null
                        ? parameter.getName().asString()
                        : "$" + joinParameterNames(destructuringVariables);
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

        for (ValueParameterDescriptor parameter : valueParameters) {
            List<VariableDescriptor> destructuringVariables = ValueParameterDescriptorImpl.getDestructuringVariablesOrNull(parameter);
            if (destructuringVariables == null) continue;

            for (VariableDescriptor entry : destructuringVariables) {
                Type type = typeMapper.mapType(entry.getType());
                mv.visitLocalVariable(entry.getName().asString(), type.getDescriptor(), null, methodBegin, methodEnd, shift);
                shift += type.getSize();
            }
        }
    }

    private static String joinParameterNames(@NotNull List<VariableDescriptor> variables) {
        return org.jetbrains.kotlin.utils.StringsKt.join(CollectionsKt.map(variables, new Function1<VariableDescriptor, String>() {
            @Override
            public String invoke(VariableDescriptor descriptor) {
                return descriptor.getName().asString();
            }
        }), "_");
    }

    private static void generateFacadeDelegateMethodBody(
            @NotNull MethodVisitor mv,
            @NotNull Method asmMethod,
            @NotNull MultifileClassFacadeContext context
    ) {
        generateDelegateToStaticMethodBody(true, mv, asmMethod, context.getFilePartType().getInternalName());
    }

    private static void generateDelegateToMethodBody(
            // -1 means to add additional this parameter on stack
            int firstParamIndex,
            @NotNull MethodVisitor mv,
            @NotNull Method asmMethod,
            @NotNull String classToDelegateTo,
            int opcode,
            boolean isInterface
    ) {
        InstructionAdapter iv = new InstructionAdapter(mv);
        Type[] argTypes = asmMethod.getArgumentTypes();

        // The first line of some package file is written to the line number attribute of a static delegate to allow to 'step into' it
        // This is similar to what javac does with bridge methods
        Label label = new Label();
        iv.visitLabel(label);
        iv.visitLineNumber(1, label);

        int paramIndex = firstParamIndex;
        if (paramIndex == -1) {
            iv.load(0, AsmTypes.OBJECT_TYPE);
            paramIndex = 1;
        }

        for (Type argType : argTypes) {
            iv.load(paramIndex, argType);
            paramIndex += argType.getSize();
        }
        iv.visitMethodInsn(opcode, classToDelegateTo, asmMethod.getName(), asmMethod.getDescriptor(), isInterface);
        iv.areturn(asmMethod.getReturnType());
    }

    private static void generateDelegateToStaticMethodBody(
            boolean isStatic,
            @NotNull MethodVisitor mv,
            @NotNull Method asmMethod,
            @NotNull String classToDelegateTo
    ) {
        generateDelegateToMethodBody(isStatic ? 0 : 1, mv, asmMethod, classToDelegateTo, Opcodes.INVOKESTATIC, false);
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
                    "wrong code generated\n" +
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

        if (mv instanceof TransformationMethodVisitor) {
            mv = ((TransformationMethodVisitor) mv).getTraceMethodVisitorIfPossible();
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
        if (owner.getContextKind() == OwnerKind.DEFAULT_IMPLS) return;
        if (isAnnotationOrJvm6Interface(descriptor.getContainingDeclaration(), state)) return;

        // equals(Any?), hashCode(), toString() never need bridges
        if (isMethodOfAny(descriptor)) return;

        boolean isSpecial = SpecialBuiltinMembers.getOverriddenBuiltinReflectingJvmDescriptor(descriptor) != null;

        Set<Bridge<Method>> bridgesToGenerate;
        if (!isSpecial) {
            bridgesToGenerate = ImplKt.generateBridgesForFunctionDescriptor(
                    descriptor,
                    getSignatureMapper(typeMapper),
                    IS_PURE_INTERFACE_CHECKER
            );
            if (!bridgesToGenerate.isEmpty()) {
                PsiElement origin = descriptor.getKind() == DECLARATION ? getSourceFromDescriptor(descriptor) : null;
                boolean isSpecialBridge =
                        BuiltinMethodsWithSpecialGenericSignature.getOverriddenBuiltinFunctionWithErasedValueParametersInJava(descriptor) != null;

                for (Bridge<Method> bridge : bridgesToGenerate) {
                    generateBridge(origin, descriptor, bridge.getFrom(), bridge.getTo(), isSpecialBridge, false);
                }
            }
        }
        else {
            Set<BridgeForBuiltinSpecial<Method>> specials = BuiltinSpecialBridgesUtil.generateBridgesForBuiltinSpecial(
                    descriptor,
                    getSignatureMapper(typeMapper),
                    IS_PURE_INTERFACE_CHECKER
            );

            if (!specials.isEmpty()) {
                PsiElement origin = descriptor.getKind() == DECLARATION ? getSourceFromDescriptor(descriptor) : null;
                for (BridgeForBuiltinSpecial<Method> bridge : specials) {
                    generateBridge(
                            origin, descriptor, bridge.getFrom(), bridge.getTo(),
                            bridge.isSpecial(), bridge.isDelegateToSuper());
                }
            }

            if (!descriptor.getKind().isReal() && isAbstractMethod(descriptor, OwnerKind.IMPLEMENTATION, state)) {
                CallableDescriptor overridden = SpecialBuiltinMembers.getOverriddenBuiltinReflectingJvmDescriptor(descriptor);
                assert overridden != null;

                if (!isThereOverriddenInKotlinClass(descriptor)) {
                    Method method = typeMapper.mapAsmMethod(descriptor);
                    int flags = ACC_ABSTRACT | getVisibilityAccessFlag(descriptor);
                    v.newMethod(JvmDeclarationOriginKt.OtherOrigin(overridden), flags, method.getName(), method.getDescriptor(), null, null);
                }
            }
        }
    }

    private static boolean isThereOverriddenInKotlinClass(@NotNull CallableMemberDescriptor descriptor) {
        return CollectionsKt.any(getAllOverriddenDescriptors(descriptor), new Function1<CallableMemberDescriptor, Boolean>() {
            @Override
            public Boolean invoke(CallableMemberDescriptor descriptor) {
                return !(descriptor.getContainingDeclaration() instanceof JavaClassDescriptor) &&
                            isClass(descriptor.getContainingDeclaration());
            }
        });
    }

    @NotNull
    private static Function1<FunctionDescriptor, Method> getSignatureMapper(final @NotNull KotlinTypeMapper typeMapper) {
        return new Function1<FunctionDescriptor, Method>() {
            @Override
            public Method invoke(FunctionDescriptor descriptor) {
                return typeMapper.mapAsmMethod(descriptor);
            }
        };
    }

    private static boolean isMethodOfAny(@NotNull FunctionDescriptor descriptor) {
        String name = descriptor.getName().asString();
        List<ValueParameterDescriptor> parameters = descriptor.getValueParameters();
        if (parameters.isEmpty()) {
            return name.equals("hashCode") || name.equals("toString");
        }
        else if (parameters.size() == 1 && name.equals("equals")) {
            return isNullableAny(parameters.get(0).getType());
        }
        return false;
    }

    @NotNull
    public static String[] getThrownExceptions(@NotNull FunctionDescriptor function, @NotNull final KotlinTypeMapper mapper) {
        AnnotationDescriptor annotation = function.getAnnotations().findAnnotation(new FqName("kotlin.throws"));
        if (annotation == null) {
            annotation = function.getAnnotations().findAnnotation(new FqName("kotlin.jvm.Throws"));
        }

        if (annotation == null) return ArrayUtil.EMPTY_STRING_ARRAY;

        Collection<ConstantValue<?>> values = annotation.getAllValueArguments().values();
        if (values.isEmpty()) return ArrayUtil.EMPTY_STRING_ARRAY;

        Object value = values.iterator().next();
        if (!(value instanceof ArrayValue)) return ArrayUtil.EMPTY_STRING_ARRAY;
        ArrayValue arrayValue = (ArrayValue) value;

        List<String> strings = ContainerUtil.mapNotNull(
                arrayValue.getValue(),
                new Function<ConstantValue<?>, String>() {
                    @Override
                    public String fun(ConstantValue<?> constant) {
                        if (constant instanceof KClassValue) {
                            KClassValue classValue = (KClassValue) constant;
                            ClassDescriptor classDescriptor = DescriptorUtils.getClassDescriptorForType(classValue.getValue());
                            return mapper.mapClass(classDescriptor).getInternalName();
                        }
                        return null;
                    }
                }
        );
        return ArrayUtil.toStringArray(strings);
    }

    void generateDefaultIfNeeded(
            @NotNull MethodContext owner,
            @NotNull FunctionDescriptor functionDescriptor,
            @NotNull OwnerKind kind,
            @NotNull DefaultParameterValueLoader loadStrategy,
            @Nullable KtNamedFunction function
    ) {
        DeclarationDescriptor contextClass = owner.getContextDescriptor().getContainingDeclaration();

        if (isInterface(contextClass) && !processInterface(contextClass, kind, state)) {
            return;
        }

        if (!isDefaultNeeded(functionDescriptor)) {
            return;
        }

        int flags = getVisibilityAccessFlag(functionDescriptor) |
                    getDeprecatedAccessFlag(functionDescriptor) |
                    ACC_SYNTHETIC;
        if (!(functionDescriptor instanceof ConstructorDescriptor)) {
            flags |= ACC_STATIC | ACC_BRIDGE;
        }
        // $default methods are never private to be accessible from other class files (e.g. inner) without the need of synthetic accessors
        flags &= ~ACC_PRIVATE;

        Method defaultMethod = typeMapper.mapDefaultMethod(functionDescriptor, kind);

        MethodVisitor mv = v.newMethod(
                JvmDeclarationOriginKt.Synthetic(function, functionDescriptor),
                flags,
                defaultMethod.getName(),
                defaultMethod.getDescriptor(), null,
                getThrownExceptions(functionDescriptor, typeMapper)
        );

        // Only method annotations are copied to the $default method. Parameter annotations are not copied until there are valid use cases;
        // enum constructors have two additional synthetic parameters which somewhat complicate this task
        AnnotationCodegen.forMethod(mv, memberCodegen, typeMapper).genAnnotations(functionDescriptor, defaultMethod.getReturnType());

        if (state.getClassBuilderMode().generateBodies) {
            if (this.owner instanceof MultifileClassFacadeContext) {
                mv.visitCode();
                generateFacadeDelegateMethodBody(mv, defaultMethod, (MultifileClassFacadeContext) this.owner);
                endVisit(mv, "default method delegation", getSourceFromDescriptor(functionDescriptor));
            }
            else {
                mv.visitCode();
                generateDefaultImplBody(owner, functionDescriptor, mv, loadStrategy, function, memberCodegen, defaultMethod);
                endVisit(mv, "default method", getSourceFromDescriptor(functionDescriptor));
            }
        }
    }

    public static void generateDefaultImplBody(
            @NotNull MethodContext methodContext,
            @NotNull FunctionDescriptor functionDescriptor,
            @NotNull MethodVisitor mv,
            @NotNull DefaultParameterValueLoader loadStrategy,
            @Nullable KtNamedFunction function,
            @NotNull MemberCodegen<?> parentCodegen,
            @NotNull Method defaultMethod
    ) {
        GenerationState state = parentCodegen.state;
        JvmMethodSignature signature = state.getTypeMapper().mapSignatureWithGeneric(functionDescriptor, methodContext.getContextKind());

        boolean isStatic = isStaticMethod(methodContext.getContextKind(), functionDescriptor);
        FrameMap frameMap = createFrameMap(state, functionDescriptor, signature, isStatic);

        ExpressionCodegen codegen = new ExpressionCodegen(mv, frameMap, signature.getReturnType(), methodContext, state, parentCodegen);

        CallGenerator generator = codegen.getOrCreateCallGeneratorForDefaultImplBody(functionDescriptor, function);

        InstructionAdapter iv = new InstructionAdapter(mv);
        genDefaultSuperCallCheckIfNeeded(iv, defaultMethod);

        loadExplicitArgumentsOnStack(OBJECT_TYPE, isStatic, signature, generator);

        List<JvmMethodParameterSignature> mappedParameters = signature.getValueParameters();
        int capturedArgumentsCount = 0;
        while (capturedArgumentsCount < mappedParameters.size() &&
               mappedParameters.get(capturedArgumentsCount).getKind() != JvmMethodParameterKind.VALUE) {
            capturedArgumentsCount++;
        }

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

            generator.putValueIfNeeded(type, StackValue.local(parameterIndex, type));
        }

        CallableMethod method = state.getTypeMapper().mapToCallableMethod(functionDescriptor, false);

        generator.genCall(method, null, false, codegen);

        iv.areturn(signature.getReturnType());
    }

    private static void genDefaultSuperCallCheckIfNeeded(@NotNull InstructionAdapter iv, @NotNull Method defaultMethod) {
        String defaultMethodName = defaultMethod.getName();
        if ("<init>".equals(defaultMethodName)) {
            return;
        }
        Label end = new Label();
        int handleIndex = (Type.getArgumentsAndReturnSizes(defaultMethod.getDescriptor()) >> 2) - 2; /*-1 for this, and -1 for handle*/
        iv.load(handleIndex, OBJECT_TYPE);
        iv.ifnull(end);
        AsmUtil.genThrow(iv,
                         "java/lang/UnsupportedOperationException",
                         "Super calls with default arguments not supported in this target, function: " +
                         StringsKt.substringBeforeLast(defaultMethodName, JvmAbi.DEFAULT_PARAMS_IMPL_SUFFIX, defaultMethodName));
        iv.visitLabel(end);
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
                else {
                    frameMap.enterTemp(parameter.getAsmType());
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
            callGenerator.putValueIfNeeded(ownerType, StackValue.local(var, ownerType));
            var += ownerType.getSize();
        }

        for (JvmMethodParameterSignature parameterSignature : signature.getValueParameters()) {
            if (parameterSignature.getKind() != JvmMethodParameterKind.VALUE) {
                Type type = parameterSignature.getAsmType();
                callGenerator.putValueIfNeeded(type, StackValue.local(var, type));
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

    private void generateBridge(
            @Nullable PsiElement origin,
            @NotNull FunctionDescriptor descriptor,
            @NotNull Method bridge,
            @NotNull Method delegateTo,
            boolean isSpecialBridge,
            boolean isStubDeclarationWithDelegationToSuper
    ) {
        boolean isSpecialOrDelegationToSuper = isSpecialBridge || isStubDeclarationWithDelegationToSuper;
        int flags = ACC_PUBLIC | ACC_BRIDGE | (!isSpecialOrDelegationToSuper ? ACC_SYNTHETIC : 0) | (isSpecialBridge ? ACC_FINAL : 0); // TODO.

        MethodVisitor mv =
                v.newMethod(JvmDeclarationOriginKt.Bridge(descriptor, origin), flags, bridge.getName(), bridge.getDescriptor(), null, null);
        if (!state.getClassBuilderMode().generateBodies) return;

        mv.visitCode();

        Type[] argTypes = bridge.getArgumentTypes();
        Type[] originalArgTypes = delegateTo.getArgumentTypes();

        InstructionAdapter iv = new InstructionAdapter(mv);
        MemberCodegen.markLineNumberForDescriptor(owner.getThisDescriptor(), iv);

        if (delegateTo.getArgumentTypes().length > 0 && isSpecialBridge) {
            generateTypeCheckBarrierIfNeeded(iv, descriptor, bridge.getReturnType(), delegateTo.getArgumentTypes());
        }

        iv.load(0, OBJECT_TYPE);
        for (int i = 0, reg = 1; i < argTypes.length; i++) {
            StackValue.local(reg, argTypes[i]).put(originalArgTypes[i], iv);
            //noinspection AssignmentToForLoopParameter
            reg += argTypes[i].getSize();
        }

        if (isStubDeclarationWithDelegationToSuper) {
            ClassDescriptor parentClass = getSuperClassDescriptor((ClassDescriptor) descriptor.getContainingDeclaration());
            assert parentClass != null;
            String parentInternalName = typeMapper.mapClass(parentClass).getInternalName();
            iv.invokespecial(parentInternalName, delegateTo.getName(), delegateTo.getDescriptor());
        }
        else {
            if (isJvm8InterfaceMember(descriptor, state)) {
                iv.invokeinterface(v.getThisName(), delegateTo.getName(), delegateTo.getDescriptor());
            }
            else {
                iv.invokevirtual(v.getThisName(), delegateTo.getName(), delegateTo.getDescriptor());
            }
        }

        StackValue.coerce(delegateTo.getReturnType(), bridge.getReturnType(), iv);
        iv.areturn(bridge.getReturnType());

        endVisit(mv, "bridge method", origin);
    }

    private static void generateTypeCheckBarrierIfNeeded(
            @NotNull InstructionAdapter iv,
            @NotNull FunctionDescriptor descriptor,
            @NotNull Type returnType,
            @Nullable Type[] delegateParameterTypes
    ) {
        BuiltinMethodsWithSpecialGenericSignature.TypeSafeBarrierDescription typeSafeBarrierDescription =
                BuiltinMethodsWithSpecialGenericSignature.getDefaultValueForOverriddenBuiltinFunction(descriptor);
        if (typeSafeBarrierDescription == null) return;

        FunctionDescriptor overriddenBuiltin =
                BuiltinMethodsWithSpecialGenericSignature.getOverriddenBuiltinFunctionWithErasedValueParametersInJava(descriptor);

        assert overriddenBuiltin != null : "Overridden built-in method should not be null for " + descriptor;

        Label defaultBranch = new Label();

        for (int i = 0; i < descriptor.getValueParameters().size(); i++) {
            if (!typeSafeBarrierDescription.checkParameter(i)) continue;
            boolean isCheckForAny = delegateParameterTypes == null || OBJECT_TYPE.equals(delegateParameterTypes[i]);

            KotlinType kotlinType = descriptor.getValueParameters().get(i).getType();

            if (isCheckForAny && TypeUtils.isNullableType(kotlinType)) continue;

            iv.load(1 + i, OBJECT_TYPE);

            if (isCheckForAny) {
                assert !TypeUtils.isNullableType(kotlinType) : "Only bridges for not-nullable types are necessary";
                iv.ifnull(defaultBranch);
            }
            else {
                CodegenUtilKt.generateIsCheck(iv, kotlinType, boxType(delegateParameterTypes[i]));
                iv.ifeq(defaultBranch);
            }
        }

        Label afterDefaultBranch = new Label();

        iv.goTo(afterDefaultBranch);

        iv.visitLabel(defaultBranch);

        if (typeSafeBarrierDescription.equals(BuiltinMethodsWithSpecialGenericSignature.TypeSafeBarrierDescription.MAP_GET_OR_DEFAULT)) {
            iv.load(2, returnType);
        }
        else {
            StackValue.constant(typeSafeBarrierDescription.getDefaultValue(), returnType).put(returnType, iv);
        }
        iv.areturn(returnType);

        iv.visitLabel(afterDefaultBranch);
    }

    public void genSamDelegate(@NotNull FunctionDescriptor functionDescriptor, FunctionDescriptor overriddenDescriptor, StackValue field) {
        FunctionDescriptor delegatedTo = overriddenDescriptor.getOriginal();
        JvmDeclarationOrigin declarationOrigin =
                JvmDeclarationOriginKt.SamDelegation(functionDescriptor);
        genDelegate(
                functionDescriptor, delegatedTo,
                declarationOrigin,
                (ClassDescriptor) overriddenDescriptor.getContainingDeclaration(),
                field);
    }

    public void genDelegate(@NotNull FunctionDescriptor functionDescriptor, FunctionDescriptor overriddenDescriptor, StackValue field) {
        genDelegate(functionDescriptor, overriddenDescriptor.getOriginal(),
                    (ClassDescriptor) overriddenDescriptor.getContainingDeclaration(), field);
    }

    public void genDelegate(
            @NotNull FunctionDescriptor delegateFunction,
            FunctionDescriptor delegatedTo,
            ClassDescriptor toClass,
            StackValue field
    ) {
        JvmDeclarationOrigin declarationOrigin =
                JvmDeclarationOriginKt.Delegation(DescriptorToSourceUtils.descriptorToDeclaration(delegatedTo), delegateFunction);
        genDelegate(delegateFunction, delegatedTo, declarationOrigin, toClass, field);
    }

    private void genDelegate(
            @NotNull final FunctionDescriptor delegateFunction,
            final FunctionDescriptor delegatedTo,
            @NotNull JvmDeclarationOrigin declarationOrigin,
            final ClassDescriptor toClass,
            final StackValue field
    ) {
        generateMethod(
                declarationOrigin, delegateFunction,
                new FunctionGenerationStrategy() {
                    @Override
                    public void generateBody(
                            @NotNull MethodVisitor mv,
                            @NotNull FrameMap frameMap,
                            @NotNull JvmMethodSignature signature,
                            @NotNull MethodContext context,
                            @NotNull MemberCodegen<?> parentCodegen
                    ) {
                        Method delegateToMethod = typeMapper.mapToCallableMethod(delegatedTo, /* superCall = */ false).getAsmMethod();
                        Method delegateMethod = typeMapper.mapAsmMethod(delegateFunction);

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
                        if (toClass.getKind() == ClassKind.INTERFACE) {
                            iv.invokeinterface(internalName, delegateToMethod.getName(), delegateToMethod.getDescriptor());
                        }
                        else {
                            iv.invokevirtual(internalName, delegateToMethod.getName(), delegateToMethod.getDescriptor());
                        }

                        StackValue stackValue = AsmUtil.genNotNullAssertions(
                                state,
                                StackValue.onStack(delegateToMethod.getReturnType()),
                                RuntimeAssertionInfo.create(
                                        delegateFunction.getReturnType(),
                                        delegatedTo.getReturnType(),
                                        new RuntimeAssertionInfo.DataFlowExtras.OnlyMessage(delegatedTo.getName() + "(...)")
                                )
                        );

                        stackValue.put(delegateMethod.getReturnType(), iv);

                        iv.areturn(delegateMethod.getReturnType());
                    }
                }
        );
    }

    public static boolean processInterfaceMember(
            @NotNull CallableMemberDescriptor function,
            @NotNull OwnerKind kind,
            @NotNull GenerationState state
    ) {
        return processInterface(function.getContainingDeclaration(), kind, state);
    }

    public static boolean processInterface(
            @NotNull DeclarationDescriptor contextClass,
            @NotNull OwnerKind kind,
            @NotNull GenerationState state
    ) {
        assert isInterface(contextClass) : "'processInterface' method should be called only for interfaces, but: " + contextClass;
        return isJvm8Interface(contextClass, state) ? kind != OwnerKind.DEFAULT_IMPLS : kind == OwnerKind.DEFAULT_IMPLS;
    }
}
