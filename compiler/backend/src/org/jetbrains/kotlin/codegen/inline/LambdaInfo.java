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
import org.jetbrains.kotlin.codegen.AsmUtil;
import org.jetbrains.kotlin.codegen.PropertyReferenceCodegen;
import org.jetbrains.kotlin.codegen.StackValue;
import org.jetbrains.kotlin.codegen.binding.CalculatedClosure;
import org.jetbrains.kotlin.codegen.binding.CodegenBinding;
import org.jetbrains.kotlin.codegen.context.EnclosedValueDescriptor;
import org.jetbrains.kotlin.codegen.state.KotlinTypeMapper;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.psi.KtCallableReferenceExpression;
import org.jetbrains.kotlin.psi.KtExpression;
import org.jetbrains.kotlin.psi.KtLambdaExpression;
import org.jetbrains.kotlin.resolve.BindingContext;
import org.jetbrains.kotlin.resolve.calls.callUtil.CallUtilKt;
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall;
import org.jetbrains.kotlin.resolve.jvm.AsmTypes;
import org.jetbrains.org.objectweb.asm.Type;
import org.jetbrains.org.objectweb.asm.commons.Method;
import org.jetbrains.org.objectweb.asm.tree.FieldInsnNode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.jetbrains.kotlin.codegen.binding.CodegenBinding.*;

public class LambdaInfo implements LabelOwner {
    public final KtExpression expression;
    private final KotlinTypeMapper typeMapper;
    public final Set<String> labels;
    private final CalculatedClosure closure;
    public final boolean isCrossInline;
    private final FunctionDescriptor functionDescriptor;
    private final ClassDescriptor classDescriptor;
    private final Type closureClassType;

    private SMAPAndMethodNode node;
    private List<CapturedParamDesc> capturedVars;
    private final boolean isBoundCallableReference;
    private final PropertyReferenceInfo propertyReferenceInfo;

    public LambdaInfo(@NotNull KtExpression expression, @NotNull KotlinTypeMapper typeMapper, boolean isCrossInline, boolean isBoundCallableReference) {
        this.isCrossInline = isCrossInline;
        this.expression = expression instanceof KtLambdaExpression ?
                          ((KtLambdaExpression) expression).getFunctionLiteral() : expression;

        this.typeMapper = typeMapper;
        this.isBoundCallableReference = isBoundCallableReference;
        BindingContext bindingContext = typeMapper.getBindingContext();
        FunctionDescriptor function = bindingContext.get(BindingContext.FUNCTION, this.expression);
        if (function == null && expression instanceof KtCallableReferenceExpression) {
            VariableDescriptor variableDescriptor = bindingContext.get(BindingContext.VARIABLE, this.expression);
            assert variableDescriptor instanceof VariableDescriptorWithAccessors :
                    "Reference expression not resolved to variable descriptor with accessors: " + expression.getText();
            classDescriptor = CodegenBinding.anonymousClassForCallable(bindingContext, variableDescriptor);
            closureClassType = typeMapper.mapClass(classDescriptor);
            SimpleFunctionDescriptor getFunction = PropertyReferenceCodegen.findGetFunction(variableDescriptor);
            functionDescriptor = PropertyReferenceCodegen.createFakeOpenDescriptor(getFunction, classDescriptor);
            ResolvedCall<?> resolvedCall = CallUtilKt.getResolvedCallWithAssert(((KtCallableReferenceExpression) expression).getCallableReference(), bindingContext);
            propertyReferenceInfo = new PropertyReferenceInfo(
                    (VariableDescriptor) resolvedCall.getResultingDescriptor(), getFunction
            );
        }
        else {
            propertyReferenceInfo = null;
            functionDescriptor = function;
            assert functionDescriptor != null : "Function is not resolved to descriptor: " + expression.getText();
            classDescriptor = anonymousClassForCallable(bindingContext, functionDescriptor);
            closureClassType = asmTypeForAnonymousClass(bindingContext, functionDescriptor);
        }


        closure = bindingContext.get(CLOSURE, classDescriptor);
        assert closure != null : "Closure for lambda should be not null " + expression.getText();

        labels = InlineCodegen.getDeclarationLabels(expression, functionDescriptor);
    }

    @NotNull
    public SMAPAndMethodNode getNode() {
        return node;
    }

    public void setNode(@NotNull SMAPAndMethodNode node) {
        this.node = node;
    }

    @NotNull
    public FunctionDescriptor getFunctionDescriptor() {
        return functionDescriptor;
    }

    @NotNull
    public KtExpression getFunctionWithBodyOrCallableReference() {
        return expression;
    }

    @NotNull
    public ClassDescriptor getClassDescriptor() {
        return classDescriptor;
    }

    @NotNull
    public Type getLambdaClassType() {
        return closureClassType;
    }

    @NotNull
    public List<CapturedParamDesc> getCapturedVars() {
        //lazy initialization cause it would be calculated after object creation
        if (capturedVars == null) {
            capturedVars = new ArrayList<CapturedParamDesc>();

            if (closure.getCaptureThis() != null) {
                Type type = typeMapper.mapType(closure.getCaptureThis());
                EnclosedValueDescriptor descriptor =
                        new EnclosedValueDescriptor(
                                AsmUtil.CAPTURED_THIS_FIELD,
                                /* descriptor = */ null,
                                StackValue.field(type, closureClassType, AsmUtil.CAPTURED_THIS_FIELD, false, StackValue.LOCAL_0),
                                type
                        );
                capturedVars.add(getCapturedParamInfo(descriptor));
            }

            if (closure.getCaptureReceiverType() != null) {
                Type type = typeMapper.mapType(closure.getCaptureReceiverType());
                EnclosedValueDescriptor descriptor =
                        new EnclosedValueDescriptor(
                                AsmUtil.CAPTURED_RECEIVER_FIELD,
                                /* descriptor = */ null,
                                StackValue.field(type, closureClassType, AsmUtil.CAPTURED_RECEIVER_FIELD, false, StackValue.LOCAL_0),
                                type
                        );
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
        return new CapturedParamDesc(closureClassType, descriptor.getFieldName(), descriptor.getType());
    }

    @NotNull
    public List<Type> getInvokeParamsWithoutCaptured() {
        return Arrays.asList(typeMapper.mapAsmMethod(functionDescriptor).getArgumentTypes());
    }

    @NotNull
    public Parameters addAllParameters(@NotNull FieldRemapper remapper) {
        Method asmMethod = typeMapper.mapAsmMethod(getFunctionDescriptor());
        ParametersBuilder builder = ParametersBuilder.initializeBuilderFrom(AsmTypes.OBJECT_TYPE, asmMethod.getDescriptor(), this);

        for (CapturedParamDesc info : getCapturedVars()) {
            CapturedParamInfo field = remapper.findField(new FieldInsnNode(0, info.getContainingLambdaName(), info.getFieldName(), ""));
            assert field != null : "Captured field not found: " + info.getContainingLambdaName() + "." + info.getFieldName();
            builder.addCapturedParam(field, info.getFieldName());
        }

        return builder.buildParameters();
    }

    @Override
    public boolean isMyLabel(@NotNull String name) {
        return labels.contains(name);
    }

    public boolean isBoundCallableReference() {
        return isBoundCallableReference;
    }

    public boolean isPropertyReference() {
        return propertyReferenceInfo != null;
    }

    public PropertyReferenceInfo getPropertyReferenceInfo() {
        return propertyReferenceInfo;
    }
}
