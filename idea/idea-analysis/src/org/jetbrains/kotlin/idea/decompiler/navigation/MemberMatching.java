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

package org.jetbrains.kotlin.idea.decompiler.navigation;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.intellij.util.Function;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.builtins.KotlinBuiltIns;
import org.jetbrains.kotlin.descriptors.CallableDescriptor;
import org.jetbrains.kotlin.descriptors.ReceiverParameterDescriptor;
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor;
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor;
import org.jetbrains.kotlin.lexer.KtTokens;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.renderer.DescriptorRenderer;
import org.jetbrains.kotlin.resolve.descriptorUtil.DescriptorUtilsKt;
import org.jetbrains.kotlin.types.KotlinType;

import java.util.List;
import java.util.Set;

public class MemberMatching {
    /* DECLARATIONS ROUGH MATCHING */
    @Nullable
    private static KtTypeReference getReceiverType(@NotNull KtNamedDeclaration propertyOrFunction) {
        if (propertyOrFunction instanceof KtCallableDeclaration) {
            return ((KtCallableDeclaration) propertyOrFunction).getReceiverTypeReference();
        }
        throw new IllegalArgumentException("Not a callable declaration: " + propertyOrFunction.getClass().getName());
    }

    @NotNull
    private static List<KtParameter> getValueParameters(@NotNull KtNamedDeclaration propertyOrFunction) {
        if (propertyOrFunction instanceof KtCallableDeclaration) {
            return ((KtCallableDeclaration) propertyOrFunction).getValueParameters();
        }
        throw new IllegalArgumentException("Not a callable declaration: " + propertyOrFunction.getClass().getName());
    }

    private static String getTypeShortName(@NotNull KtTypeReference typeReference) {
        KtTypeElement typeElement = typeReference.getTypeElement();
        assert typeElement != null;
        return typeElement.accept(new KtVisitor<String, Void>() {
            @Override
            public String visitDeclaration(@NotNull KtDeclaration declaration, Void data) {
                throw new IllegalStateException("This visitor shouldn't be invoked for " + declaration.getClass());
            }

            @Override
            public String visitUserType(@NotNull KtUserType type, Void data) {
                KtSimpleNameExpression referenceExpression = type.getReferenceExpression();
                assert referenceExpression != null;
                return referenceExpression.getReferencedName();
            }

            @Override
            public String visitFunctionType(@NotNull KtFunctionType type, Void data) {
                return KotlinBuiltIns.getFunctionName(type.getParameters().size() + (type.getReceiverTypeReference() != null ? 1 : 0));
            }

            @Override
            public String visitNullableType(@NotNull KtNullableType nullableType, Void data) {
                KtTypeElement innerType = nullableType.getInnerType();
                assert innerType != null : "No inner type: " + nullableType;
                return innerType.accept(this, null);
            }

            @Override
            public String visitDynamicType(@NotNull KtDynamicType type, Void data) {
                return "dynamic";
            }
        }, null);
    }

    private static boolean typesHaveSameShortName(@NotNull KtTypeReference a, @NotNull KtTypeReference b) {
        return getTypeShortName(a).equals(getTypeShortName(b));
    }

    static boolean sameReceiverPresenceAndParametersCount(@NotNull KtNamedDeclaration a, @NotNull KtNamedDeclaration b) {
        boolean sameReceiverPresence = (getReceiverType(a) == null) == (getReceiverType(b) == null);
        boolean sameParametersCount = getValueParameters(a).size() == getValueParameters(b).size();
        return sameReceiverPresence && sameParametersCount;
    }

    static boolean receiverAndParametersShortTypesMatch(@NotNull KtNamedDeclaration a, @NotNull KtNamedDeclaration b) {
        KtTypeReference aReceiver = getReceiverType(a);
        KtTypeReference bReceiver = getReceiverType(b);
        if ((aReceiver == null) != (bReceiver == null)) {
            return false;
        }

        if (aReceiver != null && !typesHaveSameShortName(aReceiver, bReceiver)) {
            return false;
        }

        List<KtParameter> aParameters = getValueParameters(a);
        List<KtParameter> bParameters = getValueParameters(b);
        if (aParameters.size() != bParameters.size()) {
            return false;
        }
        for (int i = 0; i < aParameters.size(); i++) {
            KtTypeReference aType = aParameters.get(i).getTypeReference();
            KtTypeReference bType = bParameters.get(i).getTypeReference();

            assert aType != null;
            assert bType != null;

            if (!typesHaveSameShortName(aType, bType)) {
                return false;
            }
        }
        return true;
    }


    /* DECLARATION AND DESCRIPTOR STRICT MATCHING */
    static boolean receiversMatch(@NotNull KtNamedDeclaration declaration, @NotNull CallableDescriptor descriptor) {
        KtTypeReference declarationReceiver = getReceiverType(declaration);
        ReceiverParameterDescriptor descriptorReceiver = descriptor.getExtensionReceiverParameter();
        if (declarationReceiver == null && descriptorReceiver == null) {
            return true;
        }
        if (declarationReceiver != null && descriptorReceiver != null) {
            return declarationReceiver.getText().equals(DescriptorRenderer.FQ_NAMES_IN_TYPES.renderType(descriptorReceiver.getType()));
        }
        return false;
    }

    static boolean valueParametersTypesMatch(@NotNull KtNamedDeclaration declaration, @NotNull CallableDescriptor descriptor) {
        List<KtParameter> declarationParameters = getValueParameters(declaration);
        List<ValueParameterDescriptor> descriptorParameters = descriptor.getValueParameters();
        if (descriptorParameters.size() != declarationParameters.size()) {
            return false;
        }

        for (int i = 0; i < descriptorParameters.size(); i++) {
            ValueParameterDescriptor descriptorParameter = descriptorParameters.get(i);
            KtParameter declarationParameter = declarationParameters.get(i);
            KtTypeReference typeReference = declarationParameter.getTypeReference();
            if (typeReference == null) {
                return false;
            }
            KtModifierList modifierList = declarationParameter.getModifierList();
            boolean varargInDeclaration = modifierList != null && modifierList.hasModifier(KtTokens.VARARG_KEYWORD);
            boolean varargInDescriptor = descriptorParameter.getVarargElementType() != null;
            if (varargInDeclaration != varargInDescriptor) {
                return false;
            }
            String declarationTypeText = typeReference.getText();

            KotlinType typeToRender = varargInDeclaration ? descriptorParameter.getVarargElementType() : descriptorParameter.getType();
            assert typeToRender != null;
            String descriptorParameterText = DescriptorRenderer.FQ_NAMES_IN_TYPES.renderType(typeToRender);
            if (!declarationTypeText.equals(descriptorParameterText)) {
                return false;
            }
        }
        return true;
    }

    static boolean typeParametersMatch(
            @NotNull KtTypeParameterListOwner typeParameterListOwner,
            @NotNull List<TypeParameterDescriptor> typeParameterDescriptors
    ) {
        List<KtTypeParameter> decompiledParameters = typeParameterListOwner.getTypeParameters();
        if (decompiledParameters.size() != typeParameterDescriptors.size()) {
            return false;
        }

        Multimap<Name, String> decompiledParameterToBounds = HashMultimap.create();
        for (KtTypeParameter parameter : decompiledParameters) {
            KtTypeReference extendsBound = parameter.getExtendsBound();
            if (extendsBound != null) {
                decompiledParameterToBounds.put(parameter.getNameAsName(), extendsBound.getText());
            }
        }

        for (KtTypeConstraint typeConstraint : typeParameterListOwner.getTypeConstraints()) {
            KtSimpleNameExpression typeParameterName = typeConstraint.getSubjectTypeParameterName();
            assert typeParameterName != null;

            KtTypeReference bound = typeConstraint.getBoundTypeReference();
            assert bound != null;

            decompiledParameterToBounds.put(typeParameterName.getReferencedNameAsName(), bound.getText());
        }

        for (int i = 0; i < decompiledParameters.size(); i++) {
            KtTypeParameter decompiledParameter = decompiledParameters.get(i);
            TypeParameterDescriptor descriptor = typeParameterDescriptors.get(i);

            Name name = decompiledParameter.getNameAsName();
            assert name != null;
            if (!name.equals(descriptor.getName())) {
                return false;
            }

            Set<String> descriptorUpperBounds = Sets.newHashSet(ContainerUtil.map(
                    descriptor.getUpperBounds(), new Function<KotlinType, String>() {
                @Override
                public String fun(KotlinType type) {
                    return DescriptorRenderer.FQ_NAMES_IN_TYPES.renderType(type);
                }
            }));

            KotlinBuiltIns builtIns = DescriptorUtilsKt.getBuiltIns(descriptor);
            Set<String> decompiledUpperBounds = decompiledParameterToBounds.get(descriptor.getName()).isEmpty()
                    ? Sets.newHashSet(DescriptorRenderer.FQ_NAMES_IN_TYPES.renderType(builtIns.getDefaultBound()))
                    : Sets.newHashSet(decompiledParameterToBounds.get(descriptor.getName()));
            if (!descriptorUpperBounds.equals(decompiledUpperBounds)) {
                return false;
            }
        }
        return true;
    }

    private MemberMatching() {
    }
}
