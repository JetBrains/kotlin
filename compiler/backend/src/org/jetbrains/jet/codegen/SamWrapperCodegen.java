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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.asm4.MethodVisitor;
import org.jetbrains.asm4.Type;
import org.jetbrains.asm4.commons.InstructionAdapter;
import org.jetbrains.jet.codegen.binding.CodegenBinding;
import org.jetbrains.jet.codegen.context.CodegenContext;
import org.jetbrains.jet.codegen.state.GenerationState;
import org.jetbrains.jet.codegen.state.GenerationStateAware;
import org.jetbrains.jet.codegen.state.JetTypeMapperMode;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.psi.JetCallExpression;
import org.jetbrains.jet.lang.psi.JetExpression;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.calls.model.ResolvedCall;
import org.jetbrains.jet.lang.resolve.java.JvmClassName;
import org.jetbrains.jet.lang.resolve.java.sam.SingleAbstractMethodUtils;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;

import static org.jetbrains.asm4.Opcodes.*;
import static org.jetbrains.jet.codegen.AsmUtil.NO_FLAG_PACKAGE_PRIVATE;
import static org.jetbrains.jet.codegen.AsmUtil.genStubCode;
import static org.jetbrains.jet.lang.resolve.java.AsmTypeConstants.OBJECT_TYPE;

public class SamWrapperCodegen extends GenerationStateAware {
    private static final String FUNCTION_FIELD_NAME = "function";

    @NotNull private final ClassDescriptor samInterface;

    public SamWrapperCodegen(@NotNull GenerationState state, @NotNull ClassDescriptor samInterface) {
        super(state);
        this.samInterface = samInterface;
    }

    public JvmClassName genWrapper(JetCallExpression callExpression, JetExpression argumentExpression) {
        JvmClassName name = bindingContext.get(CodegenBinding.FQN_FOR_SAM_CONSTRUCTOR, callExpression);
        assert name != null : "internal class name not found for " + callExpression.getText();

        JetType functionType = bindingContext.get(BindingContext.EXPRESSION_TYPE, argumentExpression);
        assert functionType != null && KotlinBuiltIns.getInstance().isFunctionType(functionType) :
                "not a function type of " + argumentExpression.getText() + ": " + functionType;

        ResolvedCall<? extends CallableDescriptor> resolvedCall =
                bindingContext.get(BindingContext.RESOLVED_CALL, callExpression.getCalleeExpression());
        assert resolvedCall != null : "couldn't find resolved call for " + callExpression.getText();

        JetType resultType = resolvedCall.getResultingDescriptor().getReturnType();
        assert resultType != null && resultType.getConstructor() == samInterface.getTypeConstructor() :
                "unexpected result type: " + resultType;

        SimpleFunctionDescriptor interfaceFunction = SingleAbstractMethodUtils.getAbstractMethodOfSamType(resultType);

        ClassBuilder cv = state.getFactory().newVisitor(name.getInternalName(), callExpression.getContainingFile());
        cv.defineClass(callExpression,
                       V1_6,
                       ACC_FINAL,
                       name.getInternalName(),
                       null,
                       JvmClassName.byType(OBJECT_TYPE).getInternalName(),
                       new String[]{JvmClassName.byClassDescriptor(samInterface).getInternalName()}
        );
        cv.visitSource(callExpression.getContainingFile().getName(), null);

        Type functionAsmType = state.getTypeMapper().mapType(functionType, JetTypeMapperMode.VALUE);
        cv.newField(null,
                    ACC_SYNTHETIC | ACC_PRIVATE | ACC_FINAL,
                    FUNCTION_FIELD_NAME,
                    functionAsmType.getDescriptor(),
                    null,
                    null);

        generateConstructor(name.getAsmType(), functionAsmType, cv);
        generateMethod(name.getAsmType(), functionAsmType, cv, interfaceFunction, functionType);

        cv.done();

        return name;
    }

    private void generateConstructor(Type ownerType, Type functionType, ClassBuilder cv) {
        MethodVisitor mv = cv.newMethod(null, NO_FLAG_PACKAGE_PRIVATE, "<init>", Type.getMethodDescriptor(Type.VOID_TYPE, functionType), null, null);

        if (state.getClassBuilderMode() == ClassBuilderMode.STUBS) {
            genStubCode(mv);
        }
        else if (state.getClassBuilderMode() == ClassBuilderMode.FULL) {
            mv.visitCode();
            InstructionAdapter iv = new InstructionAdapter(mv);

            // super constructor
            iv.load(0, OBJECT_TYPE);
            iv.invokespecial(OBJECT_TYPE.getInternalName(), "<init>", "()V");

            // save parameter to field
            iv.load(0, OBJECT_TYPE);
            iv.load(1, functionType);
            iv.putfield(ownerType.getInternalName(), FUNCTION_FIELD_NAME, functionType.getDescriptor());

            iv.visitInsn(RETURN);
            FunctionCodegen.endVisit(iv, "constructor of SAM wrapper", null);
        }
    }

    private void generateMethod(
            Type ownerType,
            Type functionType,
            ClassBuilder cv,
            SimpleFunctionDescriptor interfaceFunction,
            JetType functionJetType
    ) {

        // using static context to avoid creating ClassDescriptor and everything else
        FunctionCodegen codegen = new FunctionCodegen(CodegenContext.STATIC, cv, state);

        FunctionDescriptor invokeFunction = functionJetType.getMemberScope()
                .getFunctions(Name.identifier("invoke")).iterator().next().getOriginal();
        StackValue functionField = StackValue.field(functionType, JvmClassName.byType(ownerType), FUNCTION_FIELD_NAME, false);
        codegen.genDelegate(interfaceFunction, invokeFunction, functionField);
    }
}
