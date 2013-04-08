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
import com.intellij.util.ArrayUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.asm4.MethodVisitor;
import org.jetbrains.asm4.Type;
import org.jetbrains.asm4.commons.InstructionAdapter;
import org.jetbrains.asm4.commons.Method;
import org.jetbrains.asm4.signature.SignatureWriter;
import org.jetbrains.jet.codegen.binding.CalculatedClosure;
import org.jetbrains.jet.codegen.context.CodegenContext;
import org.jetbrains.jet.codegen.context.LocalLookup;
import org.jetbrains.jet.codegen.signature.JvmMethodSignature;
import org.jetbrains.jet.codegen.state.GenerationState;
import org.jetbrains.jet.codegen.state.GenerationStateAware;
import org.jetbrains.jet.codegen.state.JetTypeMapper;
import org.jetbrains.jet.codegen.state.JetTypeMapperMode;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.java.JvmAbi;
import org.jetbrains.jet.lang.resolve.java.JvmClassName;
import org.jetbrains.jet.lang.resolve.java.sam.SingleAbstractMethodUtils;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;

import java.util.List;

import static org.jetbrains.asm4.Opcodes.*;
import static org.jetbrains.jet.codegen.AsmUtil.*;
import static org.jetbrains.jet.codegen.CodegenUtil.isConst;
import static org.jetbrains.jet.codegen.FunctionTypesUtil.getFunctionImplClassName;
import static org.jetbrains.jet.codegen.binding.CodegenBinding.*;

public class ClosureCodegen extends GenerationStateAware {
    private final PsiElement fun;
    private final FunctionDescriptor funDescriptor;
    private final ClassDescriptor samInterface;
    private final JvmClassName superClass;
    private final CodegenContext context;
    private final FunctionGenerationStrategy strategy;
    private final CalculatedClosure closure;
    private final JvmClassName name;

    private Method constructor;

    public ClosureCodegen(
            @NotNull GenerationState state,
            @NotNull PsiElement fun,
            @NotNull FunctionDescriptor funDescriptor,
            @Nullable ClassDescriptor samInterface,
            @NotNull JvmClassName closureSuperClass,
            @NotNull CodegenContext context,
            @NotNull LocalLookup localLookup,
            @NotNull FunctionGenerationStrategy strategy
    ) {
        super(state);

        this.fun = fun;
        this.funDescriptor = funDescriptor;
        this.samInterface = samInterface;
        this.superClass = closureSuperClass;
        this.context = context.intoClosure(funDescriptor, localLookup, typeMapper);
        this.strategy = strategy;

        ClassDescriptor classDescriptor = anonymousClassForFunction(bindingContext, funDescriptor);
        this.closure = bindingContext.get(CLOSURE, classDescriptor);
        assert closure != null : "Closure must be calculated for class: " + classDescriptor;

        this.name = classNameForAnonymousClass(bindingContext, funDescriptor);
    }


    public void gen() {
        ClassBuilder cv = state.getFactory().newVisitor(name.getInternalName(), fun.getContainingFile());

        FunctionDescriptor interfaceFunction;
        String[] superInterfaces;

        if (samInterface == null) {
            interfaceFunction = getInvokeFunction(funDescriptor);
            superInterfaces = ArrayUtil.EMPTY_STRING_ARRAY;
        }
        else {
            interfaceFunction = SingleAbstractMethodUtils.getAbstractMethodOfSamInterface(samInterface);
            superInterfaces = new String[] {JvmClassName.byClassDescriptor(samInterface).getInternalName()};
        }

        cv.defineClass(fun,
                       V1_6,
                       ACC_FINAL | ACC_SUPER,
                       name.getInternalName(),
                       getGenericSignature(),
                       superClass.getInternalName(),
                       superInterfaces
        );
        cv.visitSource(fun.getContainingFile().getName(), null);


        generateBridge(interfaceFunction, cv);

        JvmMethodSignature jvmMethodSignature = typeMapper.mapSignature(interfaceFunction.getName(), funDescriptor);

        FunctionCodegen fc = new FunctionCodegen(context, cv, state);
        fc.generateMethod(fun, jvmMethodSignature, false, null, funDescriptor, strategy);

        this.constructor = generateConstructor(cv);

        if (isConst(closure)) {
            generateConstInstance(cv);
        }

        genClosureFields(closure, cv, typeMapper);

        cv.done();
    }

    @NotNull
    public StackValue putInstanceOnStack(@NotNull InstructionAdapter v, @NotNull ExpressionCodegen codegen) {
        Type asmType = name.getAsmType();
        if (isConst(closure)) {
            v.getstatic(name.getInternalName(), JvmAbi.INSTANCE_FIELD, name.getDescriptor());
        }
        else {
            v.anew(asmType);
            v.dup();

            codegen.pushClosureOnStack(closure, false);
            v.invokespecial(name.getInternalName(), "<init>", constructor.getDescriptor());
        }
        return StackValue.onStack(asmType);
    }


    private void generateConstInstance(@NotNull ClassBuilder cv) {
        MethodVisitor mv = cv.newMethod(fun, ACC_STATIC | ACC_SYNTHETIC, "<clinit>", "()V", null, ArrayUtil.EMPTY_STRING_ARRAY);
        InstructionAdapter iv = new InstructionAdapter(mv);

        cv.newField(fun, ACC_STATIC | ACC_FINAL, JvmAbi.INSTANCE_FIELD, name.getDescriptor(), null, null);

        if (state.getClassBuilderMode() == ClassBuilderMode.STUBS) {
            genStubCode(mv);
        }
        else if (state.getClassBuilderMode() == ClassBuilderMode.FULL) {
            mv.visitCode();
            genInitSingletonField(name.getAsmType(), iv);
            mv.visitInsn(RETURN);
            FunctionCodegen.endVisit(mv, "<clinit>", fun);
        }
    }

    private void generateBridge(@NotNull FunctionDescriptor interfaceFunction, @NotNull ClassBuilder cv) {
        Method bridge = typeMapper.mapSignature(interfaceFunction).getAsmMethod();

        Method delegate = typeMapper.mapSignature(interfaceFunction.getName(), funDescriptor).getAsmMethod();

        if (bridge.getDescriptor().equals(delegate.getDescriptor())) {
            return;
        }

        MethodVisitor mv = cv.newMethod(fun, ACC_PUBLIC | ACC_BRIDGE, interfaceFunction.getName().getName(),
                                        bridge.getDescriptor(), null, ArrayUtil.EMPTY_STRING_ARRAY);
        if (state.getClassBuilderMode() == ClassBuilderMode.STUBS) {
            genStubCode(mv);
        }
        if (state.getClassBuilderMode() == ClassBuilderMode.FULL) {
            mv.visitCode();

            InstructionAdapter iv = new InstructionAdapter(mv);

            iv.load(0, name.getAsmType());

            ReceiverParameterDescriptor receiver = funDescriptor.getReceiverParameter();
            int count = 1;
            if (receiver != null) {
                StackValue.local(count, bridge.getArgumentTypes()[count - 1]).put(typeMapper.mapType(receiver.getType()), iv);
                count++;
            }

            List<ValueParameterDescriptor> params = funDescriptor.getValueParameters();
            for (ValueParameterDescriptor param : params) {
                StackValue.local(count, bridge.getArgumentTypes()[count - 1]).put(typeMapper.mapType(param.getType()), iv);
                count++;
            }

            iv.invokevirtual(name.getInternalName(), interfaceFunction.getName().getName(), delegate.getDescriptor());
            StackValue.onStack(delegate.getReturnType()).put(bridge.getReturnType(), iv);

            iv.areturn(bridge.getReturnType());

            FunctionCodegen.endVisit(mv, "bridge", fun);
        }
    }

    @NotNull
    private Method generateConstructor(@NotNull ClassBuilder cv) {
        List<FieldInfo> args = calculateConstructorParameters(typeMapper, closure, name.getAsmType());

        Type[] argTypes = fieldListToTypeArray(args);

        Method constructor = new Method("<init>", Type.VOID_TYPE, argTypes);
        MethodVisitor mv = cv.newMethod(fun, NO_FLAG_PACKAGE_PRIVATE, "<init>", constructor.getDescriptor(), null,
                                        ArrayUtil.EMPTY_STRING_ARRAY);
        if (state.getClassBuilderMode() == ClassBuilderMode.STUBS) {
            genStubCode(mv);
        }
        else if (state.getClassBuilderMode() == ClassBuilderMode.FULL) {
            mv.visitCode();
            InstructionAdapter iv = new InstructionAdapter(mv);

            iv.load(0, superClass.getAsmType());
            iv.invokespecial(superClass.getInternalName(), "<init>", "()V");

            int k = 1;
            for (FieldInfo fieldInfo : args) {
                k = AsmUtil.genAssignInstanceFieldFromParam(fieldInfo, k, iv);
            }

            iv.visitInsn(RETURN);

            FunctionCodegen.endVisit(iv, "constructor", fun);
        }
        return constructor;
    }

    @NotNull
    public static List<FieldInfo> calculateConstructorParameters(
            @NotNull JetTypeMapper typeMapper,
            @NotNull CalculatedClosure closure,
            @NotNull Type ownerType
    ) {
        BindingContext bindingContext = typeMapper.getBindingContext();
        List<FieldInfo> args = Lists.newArrayList();
        ClassDescriptor captureThis = closure.getCaptureThis();
        if (captureThis != null) {
            Type type = typeMapper.mapType(captureThis);
            args.add(FieldInfo.createForHiddenField(ownerType, type, CAPTURED_THIS_FIELD));
        }
        ClassifierDescriptor captureReceiver = closure.getCaptureReceiver();
        if (captureReceiver != null) {
            args.add(FieldInfo.createForHiddenField(ownerType, typeMapper.mapType(captureReceiver), CAPTURED_RECEIVER_FIELD));
        }

        for (DeclarationDescriptor descriptor : closure.getCaptureVariables().keySet()) {
            if (descriptor instanceof VariableDescriptor && !(descriptor instanceof PropertyDescriptor)) {
                Type sharedVarType = typeMapper.getSharedVarType(descriptor);

                Type type = sharedVarType != null
                                  ? sharedVarType
                                  : typeMapper.mapType((VariableDescriptor) descriptor);
                args.add(FieldInfo.createForHiddenField(ownerType, type, "$" + descriptor.getName().getName()));
            }
            else if (isLocalNamedFun(descriptor)) {
                JvmClassName className = classNameForAnonymousClass(bindingContext, (FunctionDescriptor) descriptor);
                args.add(FieldInfo.createForHiddenField(ownerType, className.getAsmType(), "$" + descriptor.getName().getName()));
            }
            else if (descriptor instanceof FunctionDescriptor) {
                assert captureReceiver != null;
            }
        }
        return args;
    }

    private static Type[] fieldListToTypeArray(List<FieldInfo> args) {
        Type[] argTypes = new Type[args.size()];
        for (int i = 0; i != argTypes.length; ++i) {
            argTypes[i] = args.get(i).getFieldType();
        }
        return argTypes;
    }

    @NotNull
    private String getGenericSignature() {
        // TODO: obtain the correct funDescriptor and use BothSignatureWriter instead of this code
        SignatureWriter signatureWriter = new SignatureWriter();
        JvmClassName funClass = getFunctionImplClassName(funDescriptor);
        signatureWriter.visitClassType(funClass.getInternalName());
        ReceiverParameterDescriptor receiverParameter = funDescriptor.getReceiverParameter();
        if (receiverParameter != null) {
            appendType(signatureWriter, receiverParameter.getType(), '=');
        }
        for (ValueParameterDescriptor parameter : funDescriptor.getValueParameters()) {
            appendType(signatureWriter, parameter.getType(), '=');
        }

        appendType(signatureWriter, funDescriptor.getReturnType(), '=');
        signatureWriter.visitEnd();

        return signatureWriter.toString();
    }

    private void appendType(SignatureWriter signatureWriter, JetType type, char variance) {
        signatureWriter.visitTypeArgument(variance);

        Type rawRetType = typeMapper.mapType(type, JetTypeMapperMode.TYPE_PARAMETER);
        while (rawRetType.getSort() == Type.ARRAY) {
            signatureWriter.visitArrayType();
            rawRetType = rawRetType.getElementType();
        }

        if (rawRetType.getSort() == Type.OBJECT) {
            signatureWriter.visitClassType(rawRetType.getInternalName());
            signatureWriter.visitEnd();
        }
        else {
            assert isPrimitive(rawRetType);
            signatureWriter.visitBaseType(rawRetType.getDescriptor().charAt(0));
        }
    }

    private static FunctionDescriptor getInvokeFunction(FunctionDescriptor funDescriptor) {
        int paramCount = funDescriptor.getValueParameters().size();
        KotlinBuiltIns builtIns = KotlinBuiltIns.getInstance();
        ClassDescriptor funClass = funDescriptor.getReceiverParameter() == null
                                   ? builtIns.getFunction(paramCount)
                                   : builtIns.getExtensionFunction(paramCount);
        return funClass.getDefaultType().getMemberScope().getFunctions(Name.identifier("invoke")).iterator().next();
    }
}
