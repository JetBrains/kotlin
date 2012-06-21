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
import com.intellij.util.containers.ComparatorUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.AnalyzingUtils;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.types.*;
import org.jetbrains.jet.lang.types.lang.JetStandardClasses;
import org.jetbrains.jet.lang.types.lang.JetStandardLibrary;
import org.jetbrains.jet.resolve.DescriptorRenderer;

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
                    if (!autoType.isNullable()) {
                        throw new AlternativeSignatureMismatchException(String.format(
                                "Auto type '%s' is not-null, while type in alternative signature is nullable: '%s'",
                                DescriptorRenderer.TEXT.renderType(autoType), nullableType.getText()));
                    }
                    return TypeUtils.makeNullable(computeAlternativeTypeFromAnnotation(nullableType.getInnerType(), autoType));
                }
                catch (AlternativeSignatureMismatchException e) {
                    exception.set(e);
                    return null;
                }
            }

            @Override
            public JetType visitFunctionType(JetFunctionType type, Void data) {
                return visitCommonType(type.getReceiverTypeRef() == null
                        ? JetStandardClasses.getFunction(type.getParameters().size())
                        : JetStandardClasses.getReceiverFunction(type.getParameters().size()), type);
            }

            @Override
            public JetType visitTupleType(JetTupleType type, Void data) {
                return visitCommonType(JetStandardClasses.getTuple(type.getComponentTypeRefs().size()), type);
            }

            @Override
            public JetType visitUserType(JetUserType type, Void data) {
                JetUserType qualifier = type.getQualifier();
                String shortName = type.getReferenceExpression().getReferencedName();
                String longName = (qualifier == null ? "" : qualifier.getText() + ".") + shortName;
                if (JetStandardClasses.UNIT_ALIAS.getName().equals(longName)) {
                    return visitCommonType(JetStandardClasses.getTuple(0), type);
                }
                return visitCommonType(longName, type);
            }

            private JetType visitCommonType(@NotNull ClassDescriptor classDescriptor, @NotNull JetTypeElement type) {
                return visitCommonType(DescriptorUtils.getFQName(classDescriptor).toSafe().getFqName(), type);
            }

            private JetType visitCommonType(@NotNull String expectedFqNamePostfix, @NotNull JetTypeElement type) {
                try {
                    String fqName = DescriptorUtils.getFQName(autoType.getConstructor().getDeclarationDescriptor()).toSafe().getFqName();
                    if (!fqName.endsWith(expectedFqNamePostfix)) {
                        throw new AlternativeSignatureMismatchException(String.format(
                                "Alternative signature type mismatch, expected: %s, actual: %s", expectedFqNamePostfix, fqName));
                    }

                    List<TypeProjection> arguments = autoType.getArguments();

                    if (arguments.size() != type.getTypeArgumentsAsTypes().size()) {
                        throw new AlternativeSignatureMismatchException(String.format(
                                "'%s' type in method signature has %d type arguments, while '%s' in alternative signature has %d of them",
                                DescriptorRenderer.TEXT.renderType(autoType), arguments.size(),
                                type.getText(), type.getTypeArgumentsAsTypes().size()));
                    }

                    List<TypeProjection> altArguments = new ArrayList<TypeProjection>();
                    for (int i = 0, size = arguments.size(); i < size; i++) {
                        JetTypeElement argumentAlternativeTypeElement = type.getTypeArgumentsAsTypes().get(i).getTypeElement();
                        TypeProjection argument = arguments.get(i);
                        JetType alternativeType =
                                computeAlternativeTypeFromAnnotation(argumentAlternativeTypeElement, argument.getType());
                        Variance variance = argument.getProjectionKind();
                        if (type instanceof JetUserType) {
                            JetTypeProjection typeProjection = ((JetUserType) type).getTypeArguments().get(i);
                            Variance altVariance = Variance.INVARIANT;
                            switch (typeProjection.getProjectionKind()) {
                                case IN:
                                    altVariance = Variance.IN_VARIANCE;
                                    break;
                                case OUT:
                                    altVariance = Variance.OUT_VARIANCE;
                                    break;
                                case STAR:
                                    throw new AlternativeSignatureMismatchException(
                                            "Star projection is not available in alternative signatures");
                                default:
                            }
                            if (altVariance != variance) {
                                throw new AlternativeSignatureMismatchException(String.format(
                                        "Variance mismatch, actual: %s, in alternative signature: %s", variance, altVariance));
                            }
                        }
                        altArguments.add(new TypeProjection(variance, alternativeType));
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

    static JetType computeAlternativeReturnType(@NotNull JetType autoType, @Nullable JetTypeReference altReturnTypeRef)
            throws AlternativeSignatureMismatchException {
        JetType altReturnType;
        if (altReturnTypeRef == null) {
            if (JetStandardClasses.isUnit(autoType)) {
                altReturnType = autoType;
            }
            else {
                throw new AlternativeSignatureMismatchException(String.format(
                        "Return type in alternative signature is missing, while in real signature it is '%s'",
                        DescriptorRenderer.TEXT.renderType(autoType)));
            }
        }
        else {
            altReturnType = computeAlternativeTypeFromAnnotation(altReturnTypeRef.getTypeElement(),
                                                                 autoType);
        }
        return altReturnType;
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
            JetParameter valueParameter = altFunDeclaration.getValueParameters().get(i);
            JetTypeElement alternativeTypeElement = valueParameter.getTypeReference().getTypeElement();
            JetType alternativeType;
            JetType alternativeVarargElementType;
            if (pd.getVarargElementType() == null) {
                if (valueParameter.isVarArg()) {
                    throw new AlternativeSignatureMismatchException(
                            "Parameter in method signature is not vararg, but in alternative signature it is vararg");
                }
                alternativeType = computeAlternativeTypeFromAnnotation(alternativeTypeElement, pd.getType());
                alternativeVarargElementType = null;
            }
            else {
                if (!valueParameter.isVarArg()) {
                    throw new AlternativeSignatureMismatchException(
                            "Parameter in method signature is vararg, but in alternative signature it is not");
                }
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
        if (typeParameterDescriptors.size() != altFunDeclaration.getTypeParameters().size()) {
            throw new AlternativeSignatureMismatchException(
                    String.format("Method signature has %d type parameters, but alternative signature has %d",
                                  typeParameterDescriptors.size(), altFunDeclaration.getTypeParameters().size()));
        }

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

        if (!ComparatorUtil.equalsNullable(method.getName(), altFunDeclaration.getName())) {
            throw new AlternativeSignatureMismatchException(String.format(
                    "Function names mismatch, original: %s, alternative: %s",
                    method.getName(), altFunDeclaration.getName()));
        }
    }
}
