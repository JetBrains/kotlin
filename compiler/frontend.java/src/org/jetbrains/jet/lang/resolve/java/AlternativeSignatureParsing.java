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

import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiErrorElement;
import com.intellij.psi.PsiSubstitutor;
import com.intellij.psi.PsiType;
import com.intellij.util.Function;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.AnalyzingUtils;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.JetTypeImpl;
import org.jetbrains.jet.lang.types.TypeProjection;
import org.jetbrains.jet.lang.types.TypeUtils;
import org.jetbrains.jet.lang.types.lang.JetStandardLibrary;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Evgeny Gerashchenko
 * @since 6/5/12
 */
class AlternativeSignatureParsing {
    static JetType computeAlternativeTypeFromAnnotation(JetTypeElement alternativeTypeElement, final JetType autoType)
            throws AlternativeSignatureMismatchException {
        final Ref<AlternativeSignatureMismatchException> exception = new Ref<AlternativeSignatureMismatchException>();
        JetType result = alternativeTypeElement.accept(new JetVisitor<JetType, Void>() {
            @Override
            public JetType visitNullableType(JetNullableType nullableType, Void data) {
                try {
                    return TypeUtils.makeNullable(computeAlternativeTypeFromAnnotation(nullableType.getInnerType(), autoType));
                }
                catch (AlternativeSignatureMismatchException e) {
                    exception.set(e);
                    return null;
                }
            }

            @Override
            public JetType visitFunctionType(JetFunctionType type, Void data) {
                return visitCommonType(type);
            }

            @Override
            public JetType visitTupleType(JetTupleType type, Void data) {
                return visitCommonType(type);
            }

            @Override
            public JetType visitUserType(JetUserType type, Void data) {
                return visitCommonType(type);
            }

            private JetType visitCommonType(JetTypeElement type) {
                try {
                    List<TypeProjection> arguments = autoType.getArguments();
                    List<TypeProjection> altArguments = new ArrayList<TypeProjection>();
                    for (int i = 0, size = arguments.size(); i < size; i++) {
                        JetTypeElement argumentAlternativeTypeElement = type.getTypeArgumentsAsTypes().get(i).getTypeElement();
                        TypeProjection argument = arguments.get(i);
                        JetType alternativeType =
                                computeAlternativeTypeFromAnnotation(argumentAlternativeTypeElement, argument.getType());
                        altArguments.add(new TypeProjection(argument.getProjectionKind(), alternativeType));
                    }
                    return new JetTypeImpl(autoType.getAnnotations(), autoType.getConstructor(), false,
                                           altArguments, autoType.getMemberScope());
                }
                catch (AlternativeSignatureMismatchException e) {
                    exception.set(e);
                    return null;
                }
            }

            @Override
            public JetType visitSelfType(JetSelfType type, Void data) {
                throw new UnsupportedOperationException("Self-types are not supported yet");
            }
        }, null);
        //noinspection ThrowableResultOfMethodCallIgnored
        if (exception.get() != null) {
            throw exception.get();
        }
        return result;
    }

    static JavaDescriptorResolver.ValueParameterDescriptors computeAlternativeValueParameters(
            JavaDescriptorResolver.ValueParameterDescriptors valueParameterDescriptors,
            JetNamedFunction altFunDeclaration) throws AlternativeSignatureMismatchException {
        List<ValueParameterDescriptor> parameterDescriptors = valueParameterDescriptors.descriptors;

        if (parameterDescriptors.size() != altFunDeclaration.getValueParameters().size()) {
            throw new AlternativeSignatureMismatchException(
                    String.format("Method signature has %d value parameters, but alternative signature has %d",
                                  parameterDescriptors.size(), altFunDeclaration.getValueParameters().size()));
        }

        List<ValueParameterDescriptor> altParamDescriptors = new ArrayList<ValueParameterDescriptor>();
        for (int i = 0, size = parameterDescriptors.size(); i < size; i++) {
            ValueParameterDescriptor pd = parameterDescriptors.get(i);
            JetTypeElement alternativeTypeElement = altFunDeclaration.getValueParameters().get(i).getTypeReference().getTypeElement();
            JetType alternativeType;
            JetType alternativeVarargElementType;
            // TODO check that alternative PSI has "vararg" modifier
            if (pd.getVarargElementType() == null) {
                alternativeType = computeAlternativeTypeFromAnnotation(alternativeTypeElement, pd.getType());
                alternativeVarargElementType = null;
            }
            else {
                alternativeVarargElementType = computeAlternativeTypeFromAnnotation(alternativeTypeElement, pd.getVarargElementType());
                alternativeType = JetStandardLibrary.getInstance().getArrayType(alternativeVarargElementType);
            }
            altParamDescriptors.add(new ValueParameterDescriptorImpl(pd.getContainingDeclaration(), pd.getIndex(), pd.getAnnotations(),
                                                                     pd.getName(), pd.isVar(), alternativeType, pd.declaresDefaultValue(),
                                                                     alternativeVarargElementType));
        }
        JetType altReceiverType = null;
        if (valueParameterDescriptors.receiverType != null) {
            altReceiverType = computeAlternativeTypeFromAnnotation(altFunDeclaration.getReceiverTypeRef().getTypeElement(),
                                                                   valueParameterDescriptors.receiverType);
        }
        return new JavaDescriptorResolver.ValueParameterDescriptors(altReceiverType, altParamDescriptors);
    }

    static List<TypeParameterDescriptor> computeAlternativeTypeParameters(List<TypeParameterDescriptor> typeParameterDescriptors,
            JetNamedFunction altFunDeclaration) throws AlternativeSignatureMismatchException {
        List<TypeParameterDescriptor> altParamDescriptors = new ArrayList<TypeParameterDescriptor>();
        for (int i = 0, size = typeParameterDescriptors.size(); i < size; i++) {
            TypeParameterDescriptor pd = typeParameterDescriptors.get(i);
            DeclarationDescriptor containingDeclaration = pd.getContainingDeclaration();
            assert containingDeclaration != null;
            TypeParameterDescriptorImpl altParamDescriptor = TypeParameterDescriptorImpl
                    .createForFurtherModification(containingDeclaration, pd.getAnnotations(),
                                                  pd.isReified(), pd.getVariance(), pd.getName(), pd.getIndex());
            int upperBoundIndex = 0;
            for (JetType upperBound : pd.getUpperBounds()) {
                JetTypeElement altTypeElement;
                JetTypeParameter parameter = altFunDeclaration.getTypeParameters().get(i);
                if (upperBoundIndex == 0) {
                    JetTypeReference extendsBound = parameter.getExtendsBound();
                    if (extendsBound == null) { // default upper bound
                        assert pd.getUpperBounds().size() == 1;
                        altParamDescriptor.addDefaultUpperBound();
                        break;
                    }
                    else {
                        altTypeElement = extendsBound.getTypeElement();
                    }
                }
                else {
                    altTypeElement = findTypeParameterConstraint(altFunDeclaration, parameter.getNameAsName(), upperBoundIndex).getBoundTypeReference().getTypeElement();
                }
                altParamDescriptor.addUpperBound(computeAlternativeTypeFromAnnotation(altTypeElement, upperBound));
                upperBoundIndex++;
            }

            altParamDescriptor.setInitialized();
            altParamDescriptors.add(altParamDescriptor);
        }
        return altParamDescriptors;
    }

    @Nullable
    private static JetTypeConstraint findTypeParameterConstraint(@NotNull JetFunction function, @NotNull Name typeParameterName, int index) {
        if (index != 0) {
            int currentIndex = 0;
            for (JetTypeConstraint constraint : function.getTypeConstraints()) {
                if (typeParameterName.equals(constraint.getSubjectTypeParameterName().getReferencedNameAsName())) {
                    currentIndex++;
                }
                if (currentIndex == index) {
                    return constraint;
                }
            }
        }
        return null;
    }

    static void checkForSyntaxErrors(PsiMethodWrapper method, JetNamedFunction altFunDeclaration)
            throws AlternativeSignatureMismatchException {
        List<PsiErrorElement> syntaxErrors = AnalyzingUtils.getSyntaxErrorRanges(altFunDeclaration);
        if (!syntaxErrors.isEmpty()) {
            String textSignature = String.format("%s(%s)", method.getName(),
                    StringUtil.join(method.getPsiMethod().getSignature(PsiSubstitutor.EMPTY).getParameterTypes(),
                                    new Function<PsiType, String>() {
                                        @Override
                                        public String fun(PsiType psiType) {
                                            return psiType.getPresentableText();
                                        }
                                    }, ", "));
            int errorOffset = syntaxErrors.get(0).getTextOffset();
            String syntaxErrorDescription = syntaxErrors.get(0).getErrorDescription();

            String errorText = syntaxErrors.size() == 1
                    ? String.format("Alternative signature for %s has syntax error at %d: %s", textSignature,
                                    errorOffset, syntaxErrorDescription)
                    : String.format("Alternative signature for %s has %d syntax errors, first is at %d: %s", textSignature,
                                    syntaxErrors.size(), errorOffset, syntaxErrorDescription);
            throw new AlternativeSignatureMismatchException(errorText);
        }
    }
}
