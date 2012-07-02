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

import com.intellij.openapi.project.Project;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Evgeny Gerashchenko
 * @since 6/5/12
 */
class AlternativeSignatureData {
    private JetNamedFunction altFunDeclaration;
    private PsiMethodWrapper method;

    private boolean none;
    private String error;

    private JavaDescriptorResolver.ValueParameterDescriptors altValueParameters;
    private JetType altReturnType;
    private List<TypeParameterDescriptor> altTypeParameters;

    private Map<TypeParameterDescriptor, TypeParameterDescriptorImpl> originalToAltTypeParameters =
            new HashMap<TypeParameterDescriptor, TypeParameterDescriptorImpl>();

    AlternativeSignatureData(
            @NotNull PsiMethodWrapper method,
            @NotNull JavaDescriptorResolver.ValueParameterDescriptors valueParameterDescriptors,
            @NotNull JetType returnType,
            @NotNull List<TypeParameterDescriptor> methodTypeParameters) {
        String signature = method.getSignatureAnnotation().signature();
        if (signature.isEmpty()) {
            none = true;
            return;
        }

        this.method = method;
        Project project = method.getPsiMethod().getProject();
        altFunDeclaration = JetPsiFactory.createFunction(project, signature);

        for (TypeParameterDescriptor tp : methodTypeParameters) {
            originalToAltTypeParameters.put(tp, TypeParameterDescriptorImpl
                    .createForFurtherModification(tp.getContainingDeclaration(), tp.getAnnotations(),
                                                  tp.isReified(), tp.getVariance(), tp.getName(), tp.getIndex()));
        }

        try {
            checkForSyntaxErrors();

            computeTypeParameters(methodTypeParameters);
            computeValueParameters(valueParameterDescriptors);
            computeReturnType(returnType);
        }
        catch (AlternativeSignatureMismatchException e) {
            error = e.getMessage();
        }
    }

    public boolean isNone() {
        return none;
    }

    @Nullable
    public String getError() {
        return error;
    }

    private void checkForErrors() {
        if (none || error != null) {
            throw new IllegalStateException("Trying to read result while there is none");
        }
    }

    @NotNull
    public JavaDescriptorResolver.ValueParameterDescriptors getValueParameters() {
        checkForErrors();
        return altValueParameters;
    }

    @NotNull
    public JetType getReturnType() {
        checkForErrors();
        return altReturnType;
    }

    @NotNull
    public List<TypeParameterDescriptor> getTypeParameters() {
        checkForErrors();
        return altTypeParameters;
    }

    JetType computeType(JetTypeElement alternativeTypeElement, final JetType autoType) {
        //noinspection NullableProblems
        return alternativeTypeElement.accept(new JetVisitor<JetType, Void>() {
            @Override
            public JetType visitNullableType(JetNullableType nullableType, Void data) {
                if (!autoType.isNullable()) {
                    fail("Auto type '%s' is not-null, while type in alternative signature is nullable: '%s'",
                         DescriptorRenderer.TEXT.renderType(autoType), nullableType.getText());
                }
                return TypeUtils.makeNullable(computeType(nullableType.getInnerType(), autoType));
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
                //noinspection ConstantConditions
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
                TypeConstructor originalTypeConstructor = autoType.getConstructor();
                ClassifierDescriptor declarationDescriptor = originalTypeConstructor.getDeclarationDescriptor();
                assert declarationDescriptor != null;
                String fqName = DescriptorUtils.getFQName(declarationDescriptor).toSafe().getFqName();
                if (!fqName.endsWith(expectedFqNamePostfix)) {
                    fail("Alternative signature type mismatch, expected: %s, actual: %s", expectedFqNamePostfix, fqName);
                }

                List<TypeProjection> arguments = autoType.getArguments();

                if (arguments.size() != type.getTypeArgumentsAsTypes().size()) {
                    fail("'%s' type in method signature has %d type arguments, while '%s' in alternative signature has %d of them",
                         DescriptorRenderer.TEXT.renderType(autoType), arguments.size(), type.getText(),
                         type.getTypeArgumentsAsTypes().size());
                }

                List<TypeProjection> altArguments = new ArrayList<TypeProjection>();
                for (int i = 0, size = arguments.size(); i < size; i++) {
                    JetTypeElement argumentAlternativeTypeElement = type.getTypeArgumentsAsTypes().get(i).getTypeElement();
                    TypeProjection argument = arguments.get(i);
                    JetType alternativeType =
                            computeType(argumentAlternativeTypeElement, argument.getType());
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
                                fail("Star projection is not available in alternative signatures");
                            default:
                        }
                        if (altVariance != variance) {
                            fail("Variance mismatch, actual: %s, in alternative signature: %s", variance, altVariance);
                        }
                    }
                    altArguments.add(new TypeProjection(variance, alternativeType));
                }

                TypeConstructor typeConstructor = originalTypeConstructor;
                if (typeConstructor.getDeclarationDescriptor() instanceof TypeParameterDescriptor
                        && originalToAltTypeParameters.containsKey(typeConstructor.getDeclarationDescriptor())) {
                    typeConstructor = originalToAltTypeParameters.get(typeConstructor.getDeclarationDescriptor()).getTypeConstructor();
                }
                return new JetTypeImpl(autoType.getAnnotations(), typeConstructor, false,
                                       altArguments, autoType.getMemberScope());
            }

            @Override
            public JetType visitSelfType(JetSelfType type, Void data) {
                throw new UnsupportedOperationException("Self-types are not supported yet");
            }
        }, null);
    }

    private void computeReturnType(@NotNull JetType autoType) {
        JetTypeReference altReturnTypeRef = altFunDeclaration.getReturnTypeRef();
        if (altReturnTypeRef == null) {
            if (JetStandardClasses.isUnit(autoType)) {
                altReturnType = autoType;
            }
            else {
                fail("Return type in alternative signature is missing, while in real signature it is '%s'",
                     DescriptorRenderer.TEXT.renderType(autoType));
            }
        }
        else {
            altReturnType = computeType(altReturnTypeRef.getTypeElement(),
                                        autoType);
        }
    }

    private void computeValueParameters(JavaDescriptorResolver.ValueParameterDescriptors valueParameterDescriptors) {
        List<ValueParameterDescriptor> parameterDescriptors = valueParameterDescriptors.descriptors;

        if (parameterDescriptors.size() != altFunDeclaration.getValueParameters().size()) {
            fail("Method signature has %d value parameters, but alternative signature has %d",
                 parameterDescriptors.size(), altFunDeclaration.getValueParameters().size());
        }

        List<ValueParameterDescriptor> altParamDescriptors = new ArrayList<ValueParameterDescriptor>();
        for (int i = 0, size = parameterDescriptors.size(); i < size; i++) {
            ValueParameterDescriptor pd = parameterDescriptors.get(i);
            JetParameter valueParameter = altFunDeclaration.getValueParameters().get(i);
            //noinspection ConstantConditions
            JetTypeElement alternativeTypeElement = valueParameter.getTypeReference().getTypeElement();
            JetType alternativeType;
            JetType alternativeVarargElementType;
            if (pd.getVarargElementType() == null) {
                if (valueParameter.isVarArg()) {
                    fail("Parameter in method signature is not vararg, but in alternative signature it is vararg");
                }
                alternativeType = computeType(alternativeTypeElement, pd.getType());
                alternativeVarargElementType = null;
            }
            else {
                if (!valueParameter.isVarArg()) {
                    fail("Parameter in method signature is vararg, but in alternative signature it is not");
                }
                alternativeVarargElementType = computeType(alternativeTypeElement, pd.getVarargElementType());
                alternativeType = JetStandardLibrary.getInstance().getArrayType(alternativeVarargElementType);
            }
            altParamDescriptors.add(new ValueParameterDescriptorImpl(pd.getContainingDeclaration(), pd.getIndex(), pd.getAnnotations(),
                                                                     pd.getName(), pd.isVar(), alternativeType, pd.declaresDefaultValue(),
                                                                     alternativeVarargElementType));
        }
        if (valueParameterDescriptors.receiverType != null) {
            throw new UnsupportedOperationException("Alternative annotations for extension functions are not supported yet");
        }
        altValueParameters = new JavaDescriptorResolver.ValueParameterDescriptors(null, altParamDescriptors);
    }

    private void computeTypeParameters(List<TypeParameterDescriptor> typeParameters) {

        if (typeParameters.size() != altFunDeclaration.getTypeParameters().size()) {
            fail("Method signature has %d type parameters, but alternative signature has %d",
                 typeParameters.size(), altFunDeclaration.getTypeParameters().size());
        }

        altTypeParameters = new ArrayList<TypeParameterDescriptor>();
        for (int i = 0, size = typeParameters.size(); i < size; i++) {
            TypeParameterDescriptor pd = typeParameters.get(i);
            TypeParameterDescriptorImpl altParamDescriptor = originalToAltTypeParameters.get(pd);
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
                    JetTypeConstraint constraint =
                            findTypeParameterConstraint(altFunDeclaration, pd.getName(), upperBoundIndex);
                    if (constraint == null) {
                        fail("Upper bound #%d for type parameter %s is missing", upperBoundIndex, pd.getName());
                    }
                    //noinspection ConstantConditions
                    altTypeElement = constraint.getBoundTypeReference().getTypeElement();
                }
                altParamDescriptor.addUpperBound(computeType(altTypeElement, upperBound));
                upperBoundIndex++;
            }
            if (findTypeParameterConstraint(altFunDeclaration, pd.getName(), upperBoundIndex) != null) {
                fail("Extra upper bound #%d for type parameter %s", upperBoundIndex, pd.getName());
            }

            altParamDescriptor.setInitialized();
            altTypeParameters.add(altParamDescriptor);
        }
    }

    @Nullable
    private static JetTypeConstraint findTypeParameterConstraint(@NotNull JetFunction function, @NotNull Name typeParameterName,
            int index) {
        if (index != 0) {
            int currentIndex = 0;
            for (JetTypeConstraint constraint : function.getTypeConstraints()) {
                JetSimpleNameExpression parameterName = constraint.getSubjectTypeParameterName();
                assert parameterName != null;
                if (typeParameterName.equals(parameterName.getReferencedNameAsName())) {
                    currentIndex++;
                }
                if (currentIndex == index) {
                    return constraint;
                }
            }
        }
        return null;
    }

    private void checkForSyntaxErrors() {
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

            if (syntaxErrors.size() == 1) {
                fail("Alternative signature for %s has syntax error at %d: %s",
                     textSignature, errorOffset, syntaxErrorDescription);
            }
            else {
                fail("Alternative signature for %s has %d syntax errors, first is at %d: %s",
                     textSignature, syntaxErrors.size(), errorOffset, syntaxErrorDescription);
            }
        }

        if (!ComparatorUtil.equalsNullable(method.getName(), altFunDeclaration.getName())) {
            fail("Function names mismatch, original: %s, alternative: %s", method.getName(), altFunDeclaration.getName());
        }
    }

    private static void fail(String format, Object... params) {
        throw new AlternativeSignatureMismatchException(String.format(format, params));
    }

    private static class AlternativeSignatureMismatchException extends RuntimeException {
        private AlternativeSignatureMismatchException(String message) {
            super(message);
        }
    }
}
