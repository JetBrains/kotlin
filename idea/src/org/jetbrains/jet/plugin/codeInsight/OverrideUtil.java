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

package org.jetbrains.jet.plugin.codeInsight;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.psi.JetElement;
import org.jetbrains.jet.lang.psi.JetExpression;
import org.jetbrains.jet.lang.psi.JetNamedFunction;
import org.jetbrains.jet.lang.psi.JetPsiFactory;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;
import org.jetbrains.jet.renderer.DescriptorRenderer;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class OverrideUtil {

    public static JetElement createOverridedPropertyElementFromDescriptor(Project project, PropertyDescriptor descriptor) {
        StringBuilder bodyBuilder = new StringBuilder();
        bodyBuilder.append(displayableVisibility(descriptor)).append("override ");
        if (descriptor.isVar()) {
            bodyBuilder.append("var ");
        }
        else {
            bodyBuilder.append("val ");
        }

        addReceiverParameter(descriptor, bodyBuilder);

        bodyBuilder.append(descriptor.getName()).append(" : ").append(
                DescriptorRenderer.SHORT_NAMES_IN_TYPES.renderType(descriptor.getType()));
        String initializer = CodeInsightUtils.defaultInitializer(descriptor.getType());
        if (initializer != null) {
            bodyBuilder.append(" = ").append(initializer);
        }
        else {
            bodyBuilder.append(" = ?");
        }
        return JetPsiFactory.createProperty(project, bodyBuilder.toString());
    }

    private static String displayableVisibility(MemberDescriptor descriptor) {
        Visibility visibility = descriptor.getVisibility().normalize();
        return visibility != Visibilities.INTERNAL ? visibility.toString() + " ": "";
    }

    private static String renderType(JetType type, boolean shortNames) {
        if (shortNames) return DescriptorRenderer.SHORT_NAMES_IN_TYPES.renderType(type);
        else return DescriptorRenderer.TEXT.renderType(type);
    }

    private static void addReceiverParameter(CallableDescriptor descriptor, StringBuilder bodyBuilder) {
        ReceiverParameterDescriptor receiverParameter = descriptor.getReceiverParameter();
        if (receiverParameter != null) {
            bodyBuilder.append(receiverParameter.getType()).append(".");
        }
    }

    @NotNull
    public static String createOverridedFunctionSignatureStringFromDescriptor(
            @NotNull Project project,
            @NotNull SimpleFunctionDescriptor descriptor,
            boolean shortTypeNames
    ) {
        JetNamedFunction functionElement = createOverridedFunctionElementFromDescriptor(project, descriptor, shortTypeNames);
        JetExpression bodyExpression = functionElement.getBodyExpression();
        assert bodyExpression != null;
        bodyExpression.replace(JetPsiFactory.createWhiteSpace(project));
        return functionElement.getText().trim();
    }

    @NotNull
    public static JetNamedFunction createOverridedFunctionElementFromDescriptor(
            @NotNull Project project,
            @NotNull SimpleFunctionDescriptor descriptor,
            boolean shortNames
    ) {
        StringBuilder bodyBuilder = new StringBuilder();
        bodyBuilder.append(displayableVisibility(descriptor));
        bodyBuilder.append("override fun ");

        List<String> whereRestrictions = new ArrayList<String>();
        if (!descriptor.getTypeParameters().isEmpty()) {
            bodyBuilder.append("<");
            boolean first = true;
            for (TypeParameterDescriptor param : descriptor.getTypeParameters()) {
                if (!first) {
                    bodyBuilder.append(", ");
                }

                bodyBuilder.append(param.getName());
                Set<JetType> upperBounds = param.getUpperBounds();
                if (!upperBounds.isEmpty()) {
                    boolean firstUpperBound = true;
                    for (JetType upperBound : upperBounds) {
                        String upperBoundText = ": " + renderType(upperBound, shortNames);
                        if (!KotlinBuiltIns.getInstance().getDefaultBound().equals(upperBound)) {
                            if (firstUpperBound) {
                                bodyBuilder.append(upperBoundText);
                            }
                            else {
                                whereRestrictions.add(param.getName() + upperBoundText);
                            }
                        }
                        firstUpperBound = false;
                    }
                }

                first = false;
            }
            bodyBuilder.append("> ");
        }

        addReceiverParameter(descriptor, bodyBuilder);

        bodyBuilder.append(descriptor.getName()).append("(");
        boolean isAbstractFun = descriptor.getModality() == Modality.ABSTRACT;
        StringBuilder delegationBuilder = new StringBuilder();
        if (isAbstractFun) {
            delegationBuilder.append("throw UnsupportedOperationException()");
        }
        else {
            delegationBuilder.append("super<").append(descriptor.getContainingDeclaration().getName());
            delegationBuilder.append(">.").append(descriptor.getName()).append("(");
        }
        boolean first = true;
        for (ValueParameterDescriptor parameterDescriptor : descriptor.getValueParameters()) {
            if (!first) {
                bodyBuilder.append(", ");
                if (!isAbstractFun) {
                    delegationBuilder.append(", ");
                }
            }
            first = false;
            bodyBuilder.append(parameterDescriptor.getName());
            bodyBuilder.append(": ");
            bodyBuilder.append(renderType(parameterDescriptor.getType(), shortNames));

            if (!isAbstractFun) {
                delegationBuilder.append(parameterDescriptor.getName());
            }
        }
        bodyBuilder.append(")");
        if (!isAbstractFun) {
            delegationBuilder.append(")");
        }
        JetType returnType = descriptor.getReturnType();
        KotlinBuiltIns builtIns = KotlinBuiltIns.getInstance();

        boolean returnsNotUnit = returnType != null && !builtIns.getUnitType().equals(returnType);
        if (returnsNotUnit) {
            bodyBuilder.append(": ").append(renderType(returnType, shortNames));
        }
        if (!whereRestrictions.isEmpty()) {
            bodyBuilder.append("\n").append("where ").append(StringUtil.join(whereRestrictions, ", "));
        }
        bodyBuilder.append("{").append(returnsNotUnit && !isAbstractFun ? "return " : "").append(delegationBuilder.toString()).append("}");

        return JetPsiFactory.createFunction(project, bodyBuilder.toString());
    }
}
