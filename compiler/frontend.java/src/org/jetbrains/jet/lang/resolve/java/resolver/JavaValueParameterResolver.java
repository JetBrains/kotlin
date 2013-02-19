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

package org.jetbrains.jet.lang.resolve.java.resolver;

import com.intellij.psi.PsiEllipsisType;
import com.intellij.psi.PsiType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.ValueParameterDescriptor;
import org.jetbrains.jet.lang.descriptors.impl.ValueParameterDescriptorImpl;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.resolve.java.*;
import org.jetbrains.jet.lang.resolve.java.wrapper.PsiParameterWrapper;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.TypeUtils;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class JavaValueParameterResolver {

    private JavaTypeTransformer typeTransformer;

    public JavaValueParameterResolver() {
    }

    @NotNull
    private JvmMethodParameterMeaning resolveParameterDescriptor(
            DeclarationDescriptor containingDeclaration, int i,
            PsiParameterWrapper parameter, TypeVariableResolver typeVariableResolver
    ) {

        if (parameter.getJetTypeParameter().isDefined()) {
            return JvmMethodParameterMeaning.typeInfo();
        }

        PsiType psiType = parameter.getPsiParameter().getType();

        // TODO: must be very slow, make it lazy?
        Name name = Name.identifier(getParameterName(i, parameter));

        if (parameter.getJetValueParameter().name().length() > 0) {
            name = Name.identifier(parameter.getJetValueParameter().name());
        }

        String typeFromAnnotation = parameter.getJetValueParameter().type();
        boolean receiver = parameter.getJetValueParameter().receiver();
        boolean hasDefaultValue = parameter.getJetValueParameter().hasDefaultValue();

        JetType outType;
        if (typeFromAnnotation.length() > 0) {
            outType = getTypeTransformer().transformToType(typeFromAnnotation, typeVariableResolver);
        }
        else {
            outType = getTypeTransformer().transformToType(psiType, TypeUsage.MEMBER_SIGNATURE_CONTRAVARIANT,
                                                           typeVariableResolver);
        }

        JetType varargElementType;
        if (psiType instanceof PsiEllipsisType || parameter.getJetValueParameter().vararg()) {
            varargElementType = KotlinBuiltIns.getInstance().getArrayElementType(TypeUtils.makeNotNullable(outType));
            outType = TypeUtils.makeNotNullable(outType);
        }
        else {
            varargElementType = null;
        }

        if (receiver) {
            return JvmMethodParameterMeaning.receiver(outType);
        }
        else {

            JetType transformedType;
            if (JavaAnnotationResolver.findAnnotationWithExternal(parameter.getPsiParameter(), JvmAbi.JETBRAINS_NOT_NULL_ANNOTATION.getFqName().getFqName()) !=
                null) {
                transformedType = TypeUtils.makeNullableAsSpecified(outType, false);
            }
            else {
                transformedType = outType;
            }
            return JvmMethodParameterMeaning.regular(new ValueParameterDescriptorImpl(
                    containingDeclaration,
                    i,
                    Collections.<AnnotationDescriptor>emptyList(), // TODO
                    name,
                    false,
                    transformedType,
                    hasDefaultValue,
                    varargElementType
            ));
        }
    }

    @NotNull
    private JavaTypeTransformer getTypeTransformer() {
        return typeTransformer;
    }

    @Inject
    public void setTypeTransformer(JavaTypeTransformer typeTransformer) {
        this.typeTransformer = typeTransformer;
    }

    @NotNull
    private static String getParameterName(int number, @NotNull PsiParameterWrapper parameter) {
        String psiParameterName = parameter.getPsiParameter().getName();
        return psiParameterName != null ? psiParameterName : "p" + number;
    }

    public JavaDescriptorResolver.ValueParameterDescriptors resolveParameterDescriptors(
            DeclarationDescriptor containingDeclaration,
            List<PsiParameterWrapper> parameters, TypeVariableResolver typeVariableResolver
    ) {
        List<ValueParameterDescriptor> result = new ArrayList<ValueParameterDescriptor>();
        JetType receiverType = null;
        int indexDelta = 0;
        for (int i = 0, parametersLength = parameters.size(); i < parametersLength; i++) {
            PsiParameterWrapper parameter = parameters.get(i);
            JvmMethodParameterMeaning meaning =
                    resolveParameterDescriptor(containingDeclaration, i + indexDelta, parameter, typeVariableResolver);
            if (meaning.kind == JvmMethodParameterKind.TYPE_INFO) {
                // TODO
                --indexDelta;
            }
            else if (meaning.kind == JvmMethodParameterKind.REGULAR) {
                result.add(meaning.valueParameterDescriptor);
            }
            else if (meaning.kind == JvmMethodParameterKind.RECEIVER) {
                if (receiverType != null) {
                    throw new IllegalStateException("more than one receiver");
                }
                --indexDelta;
                receiverType = meaning.receiverType;
            }
        }
        return new JavaDescriptorResolver.ValueParameterDescriptors(receiverType, result);
    }

    public enum JvmMethodParameterKind {
        REGULAR,
        RECEIVER,
        TYPE_INFO,
    }

    public static class JvmMethodParameterMeaning {
        public final JvmMethodParameterKind kind;
        private final JetType receiverType;
        private final ValueParameterDescriptor valueParameterDescriptor;

        private JvmMethodParameterMeaning(
                JvmMethodParameterKind kind,
                @Nullable JetType receiverType,
                @Nullable ValueParameterDescriptor valueParameterDescriptor
        ) {
            this.kind = kind;
            this.receiverType = receiverType;
            this.valueParameterDescriptor = valueParameterDescriptor;
        }

        public static JvmMethodParameterMeaning receiver(@NotNull JetType receiverType) {
            return new JvmMethodParameterMeaning(JvmMethodParameterKind.RECEIVER, receiverType, null);
        }

        public static JvmMethodParameterMeaning regular(@NotNull ValueParameterDescriptor valueParameterDescriptor) {
            return new JvmMethodParameterMeaning(JvmMethodParameterKind.REGULAR, null, valueParameterDescriptor);
        }

        public static JvmMethodParameterMeaning typeInfo() {
            return new JvmMethodParameterMeaning(JvmMethodParameterKind.TYPE_INFO, null, null);
        }
    }
}