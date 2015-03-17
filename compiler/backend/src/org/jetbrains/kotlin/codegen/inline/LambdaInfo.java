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

package org.jetbrains.kotlin.codegen.inline;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.codegen.AsmUtil;
import org.jetbrains.kotlin.codegen.StackValue;
import org.jetbrains.kotlin.codegen.binding.CalculatedClosure;
import org.jetbrains.kotlin.codegen.context.EnclosedValueDescriptor;
import org.jetbrains.kotlin.codegen.state.JetTypeMapper;
import org.jetbrains.kotlin.descriptors.ClassDescriptor;
import org.jetbrains.kotlin.descriptors.FunctionDescriptor;
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor;
import org.jetbrains.kotlin.psi.JetFunctionLiteral;
import org.jetbrains.kotlin.psi.JetFunctionLiteralExpression;
import org.jetbrains.kotlin.resolve.BindingContext;
import org.jetbrains.kotlin.resolve.jvm.AsmTypes;
import org.jetbrains.org.objectweb.asm.Type;
import org.jetbrains.org.objectweb.asm.tree.FieldInsnNode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.jetbrains.kotlin.codegen.binding.CodegenBinding.*;

public class LambdaInfo implements CapturedParamOwner, LabelOwner {

    public final JetFunctionLiteralExpression expression;

    private final JetTypeMapper typeMapper;

    @Nullable
    public final String labelName;

    private final CalculatedClosure closure;

    private SMAPAndMethodNode node;

    private List<CapturedParamDesc> capturedVars;

    private final FunctionDescriptor functionDescriptor;

    private final ClassDescriptor classDescriptor;

    private final Type closureClassType;

    LambdaInfo(@NotNull JetFunctionLiteralExpression expression, @NotNull JetTypeMapper typeMapper, @Nullable String labelName) {
        this.expression = expression;
        this.typeMapper = typeMapper;
        this.labelName = labelName;
        BindingContext bindingContext = typeMapper.getBindingContext();
        functionDescriptor = bindingContext.get(BindingContext.FUNCTION, expression.getFunctionLiteral());
        assert functionDescriptor != null : "Function is not resolved to descriptor: " + expression.getText();

        classDescriptor = anonymousClassForFunction(bindingContext, functionDescriptor);
        closureClassType = asmTypeForAnonymousClass(bindingContext, functionDescriptor);

        closure = bindingContext.get(CLOSURE, classDescriptor);
        assert closure != null : "Closure for lambda should be not null " + expression.getText();
    }

    public SMAPAndMethodNode getNode() {
        return node;
    }

    public void setNode(SMAPAndMethodNode node) {
        this.node = node;
    }

    public FunctionDescriptor getFunctionDescriptor() {
        return functionDescriptor;
    }

    public JetFunctionLiteral getFunctionLiteral() {
        return expression.getFunctionLiteral();
    }

    public ClassDescriptor getClassDescriptor() {
        return classDescriptor;
    }

    public Type getLambdaClassType() {
        return closureClassType;
    }

    public List<CapturedParamDesc> getCapturedVars() {
        //lazy initialization cause it would be calculated after object creation
        if (capturedVars == null) {
            capturedVars = new ArrayList<CapturedParamDesc>();

            if (closure.getCaptureThis() != null) {
                Type type = typeMapper.mapType(closure.getCaptureThis());
                EnclosedValueDescriptor descriptor =
                        new EnclosedValueDescriptor(AsmUtil.CAPTURED_THIS_FIELD,
                                                    null,
                                                    StackValue.field(type, closureClassType, AsmUtil.CAPTURED_THIS_FIELD, false,
                                                                     StackValue.LOCAL_0),
                                                    type);
                capturedVars.add(getCapturedParamInfo(descriptor));
            }

            if (closure.getCaptureReceiverType() != null) {
                Type type = typeMapper.mapType(closure.getCaptureReceiverType());
                EnclosedValueDescriptor descriptor =
                        new EnclosedValueDescriptor(
                                AsmUtil.CAPTURED_RECEIVER_FIELD,
                                null,
                                StackValue.field(type, closureClassType, AsmUtil.CAPTURED_RECEIVER_FIELD, false,
                                                 StackValue.LOCAL_0),
                                type);
                capturedVars.add(getCapturedParamInfo(descriptor));
            }

            for (EnclosedValueDescriptor descriptor : closure.getCaptureVariables().values()) {
                capturedVars.add(getCapturedParamInfo(descriptor));
            }
        }
        return capturedVars;
    }

    @NotNull
    private CapturedParamDesc getCapturedParamInfo(@NotNull EnclosedValueDescriptor descriptor) {
        return CapturedParamDesc.createDesc(this, descriptor.getFieldName(), descriptor.getType());
    }

    @NotNull
    public List<Type> getInvokeParamsWithoutCaptured() {
        Type[] types = typeMapper.mapSignature(functionDescriptor).getAsmMethod().getArgumentTypes();
        return Arrays.asList(types);
    }

    @NotNull
    public Parameters addAllParameters(FieldRemapper remapper) {
        ParametersBuilder builder = ParametersBuilder.newBuilder();
        //add skipped this cause inlined lambda doesn't have it
        builder.addThis(AsmTypes.OBJECT_TYPE, true).setLambda(this);

        List<ValueParameterDescriptor> valueParameters = getFunctionDescriptor().getValueParameters();
        for (ValueParameterDescriptor parameter : valueParameters) {
            Type type = typeMapper.mapType(parameter.getType());
            builder.addNextParameter(type, false, null);
        }

        for (CapturedParamDesc info : getCapturedVars()) {
            CapturedParamInfo field = remapper.findField(new FieldInsnNode(0, info.getContainingLambdaName(), info.getFieldName(), ""));
            builder.addCapturedParam(field, info.getFieldName());
        }

        return builder.buildParameters();
    }

    @Override
    public Type getType() {
        return closureClassType;
    }

    @Override
    public boolean isMyLabel(@NotNull String name) {
        return name.equals(labelName);
    }

}
