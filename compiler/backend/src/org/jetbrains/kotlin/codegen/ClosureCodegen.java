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

import com.google.common.collect.Lists;
import com.intellij.util.ArrayUtil;
import kotlin.Function1;
import kotlin.Unit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.builtins.KotlinBuiltIns;
import org.jetbrains.kotlin.codegen.binding.CalculatedClosure;
import org.jetbrains.kotlin.codegen.context.ClosureContext;
import org.jetbrains.kotlin.codegen.inline.InlineCodegenUtil;
import org.jetbrains.kotlin.codegen.signature.BothSignatureWriter;
import org.jetbrains.kotlin.codegen.state.GenerationState;
import org.jetbrains.kotlin.codegen.state.JetTypeMapper;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.descriptors.impl.SimpleFunctionDescriptorImpl;
import org.jetbrains.kotlin.load.java.JvmAbi;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.psi.JetElement;
import org.jetbrains.kotlin.resolve.BindingContext;
import org.jetbrains.kotlin.resolve.DescriptorUtils;
import org.jetbrains.kotlin.types.JetType;
import org.jetbrains.org.objectweb.asm.MethodVisitor;
import org.jetbrains.org.objectweb.asm.Type;
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter;
import org.jetbrains.org.objectweb.asm.commons.Method;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.jetbrains.kotlin.codegen.AsmUtil.*;
import static org.jetbrains.kotlin.codegen.JvmCodegenUtil.isConst;
import static org.jetbrains.kotlin.codegen.binding.CodegenBinding.*;
import static org.jetbrains.kotlin.load.java.JvmAnnotationNames.KotlinSyntheticClass;
import static org.jetbrains.kotlin.resolve.jvm.diagnostics.DiagnosticsPackage.OtherOrigin;
import static org.jetbrains.org.objectweb.asm.Opcodes.*;

public class ClosureCodegen extends MemberCodegen<JetElement> {
    private final FunctionDescriptor funDescriptor;
    private final ClassDescriptor classDescriptor;
    private final SamType samType;
    private final JetType superClassType;
    private final List<JetType> superInterfaceTypes;
    private final FunctionGenerationStrategy strategy;
    private final CalculatedClosure closure;
    private final Type asmType;
    private final int visibilityFlag;
    private final KotlinSyntheticClass.Kind syntheticClassKind;

    private Method constructor;
    private Type superClassAsmType;

    public ClosureCodegen(
            @NotNull GenerationState state,
            @NotNull JetElement element,
            @Nullable SamType samType,
            @NotNull ClosureContext context,
            @NotNull KotlinSyntheticClass.Kind syntheticClassKind,
            @NotNull FunctionGenerationStrategy strategy,
            @NotNull MemberCodegen<?> parentCodegen,
            @NotNull ClassBuilder classBuilder
    ) {
        super(state, parentCodegen, context, element, classBuilder);

        this.funDescriptor = context.getFunctionDescriptor();
        this.classDescriptor = context.getContextDescriptor();
        this.samType = samType;
        this.syntheticClassKind = syntheticClassKind;
        this.strategy = strategy;

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

        this.asmType = typeMapper.mapClass(classDescriptor);

        visibilityFlag = AsmUtil.getVisibilityAccessFlagForAnonymous(classDescriptor);
    }

    @Override
    protected void generateDeclaration() {
        BothSignatureWriter sw = new BothSignatureWriter(BothSignatureWriter.Mode.CLASS);
        if (samType != null) {
            typeMapper.writeFormalTypeParameters(samType.getType().getConstructor().getParameters(), sw);
        }
        sw.writeSuperclass();
        superClassAsmType = typeMapper.mapSupertype(superClassType, sw);
        sw.writeSuperclassEnd();
        String[] superInterfaceAsmTypes = new String[superInterfaceTypes.size()];
        for (int i = 0; i < superInterfaceTypes.size(); i++) {
            JetType superInterfaceType = superInterfaceTypes.get(i);
            sw.writeInterface();
            superInterfaceAsmTypes[i] = typeMapper.mapSupertype(superInterfaceType, sw).getInternalName();
            sw.writeInterfaceEnd();
        }

        v.defineClass(element,
                      V1_6,
                      ACC_FINAL | ACC_SUPER | visibilityFlag,
                      asmType.getInternalName(),
                      sw.makeJavaGenericSignature(),
                      superClassAsmType.getInternalName(),
                      superInterfaceAsmTypes
        );

        InlineCodegenUtil.initDefaultSourceMappingIfNeeded(context, this, state);

        v.visitSource(element.getContainingFile().getName(), null);
    }

    @Nullable
    @Override
    protected ClassDescriptor classForInnerClassRecord() {
        return JvmCodegenUtil.isLambdaWhichWillBeInlined(bindingContext, funDescriptor) ? null : classDescriptor;
    }

    @Override
    protected void generateBody() {
        FunctionDescriptor erasedInterfaceFunction;
        if (samType == null) {
            erasedInterfaceFunction = getErasedInvokeFunction(funDescriptor);
        }
        else {
            erasedInterfaceFunction = samType.getAbstractMethod().getOriginal();
        }

        generateBridge(
                typeMapper.mapSignature(erasedInterfaceFunction).getAsmMethod(),
                typeMapper.mapSignature(funDescriptor).getAsmMethod()
        );

        functionCodegen.generateMethod(OtherOrigin(element, funDescriptor), funDescriptor, strategy);

        //TODO: rewrite cause ugly hack
        if (samType != null) {
            SimpleFunctionDescriptorImpl descriptorForBridges = SimpleFunctionDescriptorImpl
                    .create(funDescriptor.getContainingDeclaration(), funDescriptor.getAnnotations(),
                            erasedInterfaceFunction.getName(),
                            CallableMemberDescriptor.Kind.DECLARATION, funDescriptor.getSource());

            descriptorForBridges
                    .initialize(null, erasedInterfaceFunction.getDispatchReceiverParameter(), erasedInterfaceFunction.getTypeParameters(),
                                erasedInterfaceFunction.getValueParameters(), erasedInterfaceFunction.getReturnType(), Modality.OPEN,
                                erasedInterfaceFunction.getVisibility());

            descriptorForBridges.addOverriddenDescriptor(erasedInterfaceFunction);
            functionCodegen.generateBridges(descriptorForBridges);
        }

        this.constructor = generateConstructor(superClassAsmType);

        if (isConst(closure)) {
            generateConstInstance();
        }

        genClosureFields(closure, v, typeMapper);

        functionCodegen.generateDefaultIfNeeded(
                context.intoFunction(funDescriptor), funDescriptor, context.getContextKind(), DefaultParameterValueLoader.DEFAULT, null
        );
    }

    @Override
    protected void generateKotlinAnnotation() {
        writeKotlinSyntheticClassAnnotation(v, syntheticClassKind);
    }

    @Override
    protected void done() {
        writeOuterClassAndEnclosingMethod();
        super.done();
    }

    @NotNull
    public StackValue putInstanceOnStack(@NotNull final ExpressionCodegen codegen) {
        return StackValue.operation(asmType, new Function1<InstructionAdapter, Unit>() {
            @Override
            public Unit invoke(InstructionAdapter v) {
                if (isConst(closure)) {
                    v.getstatic(asmType.getInternalName(), JvmAbi.INSTANCE_FIELD, asmType.getDescriptor());
                }
                else {
                    v.anew(asmType);
                    v.dup();

                    codegen.pushClosureOnStack(classDescriptor, true, codegen.defaultCallGenerator);
                    v.invokespecial(asmType.getInternalName(), "<init>", constructor.getDescriptor(), false);
                }
                return Unit.INSTANCE$;
            }
        });
    }


    private void generateConstInstance() {
        MethodVisitor mv = v.newMethod(OtherOrigin(element, funDescriptor), ACC_STATIC | ACC_SYNTHETIC, "<clinit>", "()V", null, ArrayUtil.EMPTY_STRING_ARRAY);
        InstructionAdapter iv = new InstructionAdapter(mv);

        v.newField(OtherOrigin(element, funDescriptor), ACC_STATIC | ACC_FINAL | ACC_PUBLIC, JvmAbi.INSTANCE_FIELD, asmType.getDescriptor(), null, null);

        if (state.getClassBuilderMode() == ClassBuilderMode.FULL) {
            mv.visitCode();
            iv.anew(asmType);
            iv.dup();
            iv.invokespecial(asmType.getInternalName(), "<init>", "()V", false);
            iv.putstatic(asmType.getInternalName(), JvmAbi.INSTANCE_FIELD, asmType.getDescriptor());
            mv.visitInsn(RETURN);
            FunctionCodegen.endVisit(mv, "<clinit>", element);
        }
    }

    private void generateBridge(@NotNull Method bridge, @NotNull Method delegate) {
        if (bridge.equals(delegate)) return;

        MethodVisitor mv =
                v.newMethod(OtherOrigin(element, funDescriptor), ACC_PUBLIC | ACC_BRIDGE, bridge.getName(), bridge.getDescriptor(), null, ArrayUtil.EMPTY_STRING_ARRAY);

        if (state.getClassBuilderMode() != ClassBuilderMode.FULL) return;

        mv.visitCode();

        InstructionAdapter iv = new InstructionAdapter(mv);
        ImplementationBodyCodegen.markLineNumberForSyntheticFunction(DescriptorUtils.getParentOfType(funDescriptor, ClassDescriptor.class), iv);

        iv.load(0, asmType);

        ReceiverParameterDescriptor receiver = funDescriptor.getExtensionReceiverParameter();
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

        FunctionCodegen.endVisit(mv, "bridge", element);
    }

    @NotNull
    private Method generateConstructor(@NotNull Type superClassAsmType) {
        List<FieldInfo> args = calculateConstructorParameters(typeMapper, closure, asmType);

        Type[] argTypes = fieldListToTypeArray(args);

        Method constructor = new Method("<init>", Type.VOID_TYPE, argTypes);
        MethodVisitor mv = v.newMethod(OtherOrigin(element, funDescriptor), visibilityFlag, "<init>", constructor.getDescriptor(), null,
                                        ArrayUtil.EMPTY_STRING_ARRAY);
        if (state.getClassBuilderMode() == ClassBuilderMode.FULL) {
            mv.visitCode();
            InstructionAdapter iv = new InstructionAdapter(mv);

            int k = 1;
            for (FieldInfo fieldInfo : args) {
                k = genAssignInstanceFieldFromParam(fieldInfo, k, iv);
            }

            iv.load(0, superClassAsmType);
            iv.invokespecial(superClassAsmType.getInternalName(), "<init>", "()V", false);

            iv.visitInsn(RETURN);

            FunctionCodegen.endVisit(iv, "constructor", element);
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
            else if (DescriptorUtils.isLocalFunction(descriptor)) {
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
    public static FunctionDescriptor getErasedInvokeFunction(@NotNull FunctionDescriptor elementDescriptor) {
        int arity = elementDescriptor.getValueParameters().size();
        ClassDescriptor elementClass = elementDescriptor.getExtensionReceiverParameter() == null
                                   ? KotlinBuiltIns.getInstance().getFunction(arity)
                                   : KotlinBuiltIns.getInstance().getExtensionFunction(arity);
        return elementClass.getDefaultType().getMemberScope().getFunctions(Name.identifier("invoke")).iterator().next();
    }
}
