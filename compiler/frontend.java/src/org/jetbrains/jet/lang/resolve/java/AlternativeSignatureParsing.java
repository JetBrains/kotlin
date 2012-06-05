/*
 * Copyright 2010-2012 JetBrains s.r.o.
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

package org.jetbrains.jet.lang.resolve.java;

import org.jetbrains.jet.lang.descriptors.ValueParameterDescriptor;
import org.jetbrains.jet.lang.descriptors.ValueParameterDescriptorImpl;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.JetTypeImpl;
import org.jetbrains.jet.lang.types.TypeProjection;
import org.jetbrains.jet.lang.types.TypeUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Evgeny Gerashchenko
 * @since 6/5/12
 */
class AlternativeSignatureParsing {
    static JetType computeAlternativeTypeFromAnnotation(JetTypeElement alternativeTypeElement, final JetType autoType) {
        return alternativeTypeElement.accept(new JetVisitor<JetType, Void>() {
            @Override
            public JetType visitNullableType(JetNullableType nullableType, Void data) {
                return TypeUtils.makeNullable(computeAlternativeTypeFromAnnotation(nullableType.getInnerType(), autoType));
            }

            @Override
            public JetType visitFunctionType(JetFunctionType type, Void data) {
                return autoType;    //TODO
            }

            @Override
            public JetType visitTupleType(JetTupleType type, Void data) {
                return autoType;    //TODO
            }

            @Override
            public JetType visitUserType(JetUserType type, Void data) {
                List<TypeProjection> arguments = autoType.getArguments();
                List<TypeProjection> altArguments = new ArrayList<TypeProjection>();
                for (int i = 0, size = arguments.size(); i < size; i++) {
                    JetTypeElement argumentAlternativeTypeElement = type.getTypeArgumentsAsTypes().get(i).getTypeElement();
                    TypeProjection argument = arguments.get(i);
                    JetType alternativeType = computeAlternativeTypeFromAnnotation(argumentAlternativeTypeElement, argument.getType());
                    altArguments.add(new TypeProjection(argument.getProjectionKind(), alternativeType));
                }
                return new JetTypeImpl(autoType.getAnnotations(), autoType.getConstructor(), false,
                                       altArguments, autoType.getMemberScope());
            }

            @Override
            public JetType visitSelfType(JetSelfType type, Void data) {
                throw new UnsupportedOperationException("Self-types are not supported yet");
            }
        }, null);
    }

    static JavaDescriptorResolver.ValueParameterDescriptors computeAlternativeValueParameters(JavaDescriptorResolver.ValueParameterDescriptors valueParameterDescriptors,
            JetNamedFunction altFunDeclaration) {
        List<ValueParameterDescriptor> parameterDescriptors = valueParameterDescriptors.descriptors;
        List<ValueParameterDescriptor> altParamDescriptors = new ArrayList<ValueParameterDescriptor>();
        for (int i = 0, size = parameterDescriptors.size(); i < size; i++) {
            ValueParameterDescriptor pd = parameterDescriptors.get(i);
            JetTypeElement alternativeTypeElement = altFunDeclaration.getValueParameters().get(i).getTypeReference().getTypeElement();
            JetType alternativeType = computeAlternativeTypeFromAnnotation(alternativeTypeElement, pd.getType());
            // TODO vararg
            altParamDescriptors.add(new ValueParameterDescriptorImpl(pd.getContainingDeclaration(), pd.getIndex(), pd.getAnnotations(),
                                                                     pd.getName(), pd.isVar(), alternativeType, pd.declaresDefaultValue(),
                                                                     pd.getVarargElementType()));
        }
        JetType altReceiverType = null;
        if (valueParameterDescriptors.receiverType != null) {
            altReceiverType = computeAlternativeTypeFromAnnotation(altFunDeclaration.getReceiverTypeRef().getTypeElement(), valueParameterDescriptors.receiverType);
        }
        return new JavaDescriptorResolver.ValueParameterDescriptors(altReceiverType, altParamDescriptors);
    }
}
