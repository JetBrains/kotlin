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
import org.jetbrains.jet.codegen.binding.CalculatedClosure;
import org.jetbrains.jet.codegen.context.CodegenContext;
import org.jetbrains.jet.codegen.context.LocalLookup;
import org.jetbrains.jet.codegen.signature.BothSignatureWriter;
import org.jetbrains.jet.codegen.state.GenerationState;
import org.jetbrains.jet.codegen.state.JetTypeMapper;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.java.JvmAbi;
import org.jetbrains.jet.lang.resolve.java.jvmSignature.JvmMethodSignature;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;
import org.jetbrains.org.objectweb.asm.MethodVisitor;
import org.jetbrains.org.objectweb.asm.Type;
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter;
import org.jetbrains.org.objectweb.asm.commons.Method;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.jetbrains.jet.codegen.AsmUtil.*;
import static org.jetbrains.jet.codegen.JvmCodegenUtil.isConst;
import static org.jetbrains.jet.codegen.binding.CodegenBinding.*;
import static org.jetbrains.jet.lang.resolve.java.JvmAnnotationNames.KotlinSyntheticClass;
import static org.jetbrains.jet.lang.resolve.java.diagnostics.DiagnosticsPackage.OtherOrigin;
import static org.jetbrains.org.objectweb.asm.Opcodes.*;

public class ClosureCodegen extends ParentCodegenAware {
    private final PsiElement fun;
    private final FunctionDescriptor funDescriptor;
    private final SamType samType;
    private final JetType superClassType;
    private final List<JetType> superInterfaceTypes;
    private final CodegenContext context;
    private final FunctionGenerationStrategy strategy;
    private final CalculatedClosure closure;
    private final Type asmType;
    private final int visibilityFlag;
    private final KotlinSyntheticClass.Kind syntheticClassKind;

    private Method constructor;

    public ClosureCodegen(
            @NotNull GenerationState state,
            @NotNull PsiElement fun,
            @NotNull FunctionDescriptor funDescriptor,
            @Nullable SamType samType,
            @NotNull CodegenContext parentContext,
            @NotNull KotlinSyntheticClass.Kind syntheticClassKind,
            @NotNull LocalLookup localLookup,
            @NotNull FunctionGenerationStrategy strategy,
            @Nullable MemberCodegen<?> parentCodegen
    ) {
        super(state, parentCodegen);

        this.fun = fun;
        this.funDescriptor = funDescriptor;
        this.samType = samType;
        this.context = parentContext.intoClosure(funDescriptor, localLookup, typeMapper);
        this.syntheticClassKind = syntheticClassKind;
        this.strategy = strategy;

        ClassDescriptor classDescriptor = anonymousClassForFunction(bindingContext, funDescriptor);

        if (samType == null) {
            this.superInterfaceTypes = new ArrayList<JetType>();

            JetType superClassType = null;
            for (JetType supertype : classDescriptor.getTypeConstructor().getSupertypes()) {
                ClassifierDescriptor classifier = supertype.getConstructor().getDeclarationDescriptor();
                if (DescriptorUtils.isTrait(classifier)) {
                    superInterfaceTypes.add(supertype);
                }
                else {
                    assert superClassType == null : "Closure class can't have more than one superclass: " + funDescriptor;
                    superClassType = supertype;
                }
            }
            assert superClassType != null : "Closure class should have a superclass: " + funDescriptor;

            this.superClassType = superClassType;
        }
        else {
            this.superInterfaceTypes = Collections.singletonList(samType.getType());
            this.superClassType = KotlinBuiltIns.getInstance().getAnyType();
        }

        this.closure = bindingContext.get(CLOSURE, classDescriptor);
        assert closure != null : "Closure must be calculated for class: " + classDescriptor;

        this.asmType = asmTypeForAnonymousClass(bindingContext, funDescriptor);

        visibilityFlag = AsmUtil.getVisibilityAccessFlagForAnonymous(classDescriptor);
    }

    public void gen() {
        ClassBuilder cv = state.getFactory().newVisitor(OtherOrigin(fun, funDescriptor), asmType, fun.getContainingFile());

        FunctionDescriptor erasedInterfaceFunction;
        if (samType == null) {
            erasedInterfaceFunction = getErasedInvokeFunction(funDescriptor);
        }
        else {
            erasedInterfaceFunction = samType.getAbstractMethod().getOriginal();
        }

        BothSignatureWriter sw = new BothSignatureWriter(BothSignatureWriter.Mode.CLASS);
        if (samType != null) {
            typeMapper.writeFormalTypeParameters(samType.getType().getConstructor().getParameters(), sw);
        }
        sw.writeSuperclass();
        Type superClassAsmType = typeMapper.mapSupertype(superClassType, sw);
        sw.writeSuperclassEnd();
        String[] superInterfaceAsmTypes = new String[superInterfaceTypes.size()];
        for (int i = 0; i < superInterfaceTypes.size(); i++) {
            JetType superInterfaceType = superInterfaceTypes.get(i);
            sw.writeInterface();
            superInterfaceAsmTypes[i] = typeMapper.mapSupertype(superInterfaceType, sw).getInternalName();
            sw.writeInterfaceEnd();
        }

        cv.defineClass(fun,
                       V1_6,
                       ACC_FINAL | ACC_SUPER | visibilityFlag,
                       asmType.getInternalName(),
                       sw.makeJavaGenericSignature(),
                       superClassAsmType.getInternalName(),
                       superInterfaceAsmTypes
        );
        cv.visitSource(fun.getContainingFile().getName(), null);

        writeKotlinSyntheticClassAnnotation(cv, syntheticClassKind);

        JvmMethodSignature jvmMethodSignature =
                typeMapper.mapSignature(funDescriptor).replaceName(erasedInterfaceFunction.getName().toString());
        generateBridge(cv, typeMapper.mapSignature(erasedInterfaceFunction).getAsmMethod(), jvmMethodSignature.getAsmMethod());

        FunctionCodegen fc = new FunctionCodegen(context, cv, state, getParentCodegen());
        fc.generateMethod(OtherOrigin(fun, funDescriptor), jvmMethodSignature, funDescriptor, strategy);

        this.constructor = generateConstructor(cv, superClassAsmType);

        if (isConst(closure)) {
            generateConstInstance(cv);
        }

        genClosureFields(closure, cv, typeMapper);

        fc.generateDefaultIfNeeded(context.intoFunction(funDescriptor),
                                   typeMapper.mapSignature(funDescriptor),
                                   funDescriptor,
                                   context.getContextKind(),
                                   DefaultParameterValueLoader.DEFAULT,
                                   null);


        AsmUtil.writeOuterClassAndEnclosingMethod(anonymousClassForFunction(bindingContext, funDescriptor), funDescriptor, typeMapper, cv);
        cv.done();
    }

    @NotNull
    public StackValue putInstanceOnStack(@NotNull InstructionAdapter v, @NotNull ExpressionCodegen codegen) {
        if (isConst(closure)) {
            v.getstatic(asmType.getInternalName(), JvmAbi.INSTANCE_FIELD, asmType.getDescriptor());
        }
        else {
            v.anew(asmType);
            v.dup();

            codegen.pushClosureOnStack(closure, false, codegen.defaultCallGenerator);
            v.invokespecial(asmType.getInternalName(), "<init>", constructor.getDescriptor(), false);
        }
        return StackValue.onStack(asmType);
    }


    private void generateConstInstance(@NotNull ClassBuilder cv) {
        MethodVisitor mv = cv.newMethod(OtherOrigin(fun, funDescriptor), ACC_STATIC | ACC_SYNTHETIC, "<clinit>", "()V", null, ArrayUtil.EMPTY_STRING_ARRAY);
        InstructionAdapter iv = new InstructionAdapter(mv);

        cv.newField(OtherOrigin(fun, funDescriptor), ACC_STATIC | ACC_FINAL, JvmAbi.INSTANCE_FIELD, asmType.getDescriptor(), null, null);

        if (state.getClassBuilderMode() == ClassBuilderMode.FULL) {
            mv.visitCode();
            iv.anew(asmType);
            iv.dup();
            iv.invokespecial(asmType.getInternalName(), "<init>", "()V", false);
            iv.putstatic(asmType.getInternalName(), JvmAbi.INSTANCE_FIELD, asmType.getDescriptor());
            mv.visitInsn(RETURN);
            FunctionCodegen.endVisit(mv, "<clinit>", fun);
        }
    }

    private void generateBridge(@NotNull ClassBuilder cv, @NotNull Method bridge, @NotNull Method delegate) {
        if (bridge.equals(delegate)) return;

        MethodVisitor mv =
                cv.newMethod(OtherOrigin(fun, funDescriptor), ACC_PUBLIC | ACC_BRIDGE, bridge.getName(), bridge.getDescriptor(), null, ArrayUtil.EMPTY_STRING_ARRAY);

        if (state.getClassBuilderMode() != ClassBuilderMode.FULL) return;

        mv.visitCode();

        InstructionAdapter iv = new InstructionAdapter(mv);
        iv.load(0, asmType);

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

        iv.invokevirtual(asmType.getInternalName(), delegate.getName(), delegate.getDescriptor(), false);
        StackValue.onStack(delegate.getReturnType()).put(bridge.getReturnType(), iv);

        iv.areturn(bridge.getReturnType());

        FunctionCodegen.endVisit(mv, "bridge", fun);
    }

    @NotNull
    private Method generateConstructor(@NotNull ClassBuilder cv, @NotNull Type superClassAsmType) {
        List<FieldInfo> args = calculateConstructorParameters(typeMapper, closure, asmType);

        Type[] argTypes = fieldListToTypeArray(args);

        Method constructor = new Method("<init>", Type.VOID_TYPE, argTypes);
        MethodVisitor mv = cv.newMethod(OtherOrigin(fun, funDescriptor), visibilityFlag, "<init>", constructor.getDescriptor(), null,
                                        ArrayUtil.EMPTY_STRING_ARRAY);
        if (state.getClassBuilderMode() == ClassBuilderMode.FULL) {
            mv.visitCode();
            InstructionAdapter iv = new InstructionAdapter(mv);

            int k = 1;
            for (FieldInfo fieldInfo : args) {
                k = AsmUtil.genAssignInstanceFieldFromParam(fieldInfo, k, iv);
            }

            iv.load(0, superClassAsmType);
            iv.invokespecial(superClassAsmType.getInternalName(), "<init>", "()V", false);

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
        JetType captureReceiverType = closure.getCaptureReceiverType();
        if (captureReceiverType != null) {
            args.add(FieldInfo.createForHiddenField(ownerType, typeMapper.mapType(captureReceiverType), CAPTURED_RECEIVER_FIELD));
        }

        for (DeclarationDescriptor descriptor : closure.getCaptureVariables().keySet()) {
            if (descriptor instanceof VariableDescriptor && !(descriptor instanceof PropertyDescriptor)) {
                Type sharedVarType = typeMapper.getSharedVarType(descriptor);

                Type type = sharedVarType != null
                                  ? sharedVarType
                                  : typeMapper.mapType((VariableDescriptor) descriptor);
                args.add(FieldInfo.createForHiddenField(ownerType, type, "$" + descriptor.getName().asString()));
            }
            else if (isLocalNamedFun(descriptor)) {
                Type classType = asmTypeForAnonymousClass(bindingContext, (FunctionDescriptor) descriptor);
                args.add(FieldInfo.createForHiddenField(ownerType, classType, "$" + descriptor.getName().asString()));
            }
            else if (descriptor instanceof FunctionDescriptor) {
                assert captureReceiverType != null;
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
    public static FunctionDescriptor getErasedInvokeFunction(@NotNull FunctionDescriptor funDescriptor) {
        int arity = funDescriptor.getValueParameters().size();
        ClassDescriptor funClass = funDescriptor.getReceiverParameter() == null
                                   ? KotlinBuiltIns.getInstance().getFunction(arity)
                                   : KotlinBuiltIns.getInstance().getExtensionFunction(arity);
        return funClass.getDefaultType().getMemberScope().getFunctions(Name.identifier("invoke")).iterator().next();
    }
}
