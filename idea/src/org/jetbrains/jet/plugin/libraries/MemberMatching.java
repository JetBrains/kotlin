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

package org.jetbrains.jet.plugin.libraries;

import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.Sets;
import com.intellij.util.Function;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;
import org.jetbrains.jet.lexer.JetTokens;
import org.jetbrains.jet.renderer.DescriptorRenderer;
import org.jetbrains.jet.util.CommonSuppliers;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class MemberMatching {
    /* DECLARATIONS ROUGH MATCHING */
    @Nullable
    private static JetTypeReference getReceiverType(@NotNull JetNamedDeclaration propertyOrFunction) {
        if (propertyOrFunction instanceof JetNamedFunction) {
            return ((JetNamedFunction) propertyOrFunction).getReceiverTypeRef();
        }
        if (propertyOrFunction instanceof JetProperty) {
            return ((JetProperty) propertyOrFunction).getReceiverTypeRef();
        }
        throw new IllegalArgumentException("Neither function nor declaration: " + propertyOrFunction.getClass().getName());
    }

    @NotNull
    private static List<JetParameter> getValueParameters(@NotNull JetNamedDeclaration propertyOrFunction) {
        if (propertyOrFunction instanceof JetNamedFunction) {
            return ((JetNamedFunction) propertyOrFunction).getValueParameters();
        }
        if (propertyOrFunction instanceof JetProperty) {
            return Collections.emptyList();
        }
        throw new IllegalArgumentException("Neither function nor declaration: " + propertyOrFunction.getClass().getName());
    }

    private static String getTypeShortName(@NotNull JetTypeReference typeReference) {
        JetTypeElement typeElement = typeReference.getTypeElement();
        assert typeElement != null;
        return typeElement.accept(new JetVisitor<String, Void>() {
            @Override
            public String visitDeclaration(JetDeclaration declaration, Void data) {
                throw new IllegalStateException("This visitor shouldn't be invoked for " + declaration.getClass());
            }

            @Override
            public String visitUserType(JetUserType type, Void data) {
                JetSimpleNameExpression referenceExpression = type.getReferenceExpression();
                assert referenceExpression != null;
                return referenceExpression.getReferencedName();
            }

            @Override
            public String visitFunctionType(JetFunctionType type, Void data) {
                KotlinBuiltIns builtIns = KotlinBuiltIns.getInstance();
                int parameterCount = type.getParameters().size();

                if (type.getReceiverTypeRef() == null) {
                    return builtIns.getFunction(parameterCount).getName().getName();
                }
                else {
                    return builtIns.getExtensionFunction(parameterCount).getName().getName();
                }
            }

            @Override
            public String visitNullableType(JetNullableType nullableType, Void data) {
                return nullableType.getInnerType().accept(this, null);
            }
        }, null);
    }

    private static boolean typesHaveSameShortName(@NotNull JetTypeReference a, @NotNull JetTypeReference b) {
        return getTypeShortName(a).equals(getTypeShortName(b));
    }

    static <Decl extends JetNamedDeclaration> boolean sameReceiverPresenceAndParametersCount(
            @NotNull MemberNavigationStrategy<Decl, ?> navigationStrategy,
            @NotNull Decl a,
            @NotNull Decl b
    ) {
        boolean sameReceiverPresence = (getReceiverType(a) == null) == (getReceiverType(b) == null);
        boolean sameParametersCount = getValueParameters(a).size() == getValueParameters(b).size();
        return sameReceiverPresence && sameParametersCount;
    }

    static <Decl extends JetNamedDeclaration> boolean receiverAndParametersShortTypesMatch(
            @NotNull MemberNavigationStrategy<Decl, ?> navigationStrategy,
            @NotNull Decl a,
            @NotNull Decl b
    ) {
        JetTypeReference aReceiver = getReceiverType(a);
        JetTypeReference bReceiver = getReceiverType(b);
        if ((aReceiver == null) != (bReceiver == null)) {
            return false;
        }

        if (aReceiver != null && !typesHaveSameShortName(aReceiver, bReceiver)) {
            return false;
        }

        List<JetParameter> aParameters = getValueParameters(a);
        List<JetParameter> bParameters = getValueParameters(b);
        if (aParameters.size() != bParameters.size()) {
            return false;
        }
        for (int i = 0; i < aParameters.size(); i++) {
            JetTypeReference aType = aParameters.get(i).getTypeReference();
            JetTypeReference bType = bParameters.get(i).getTypeReference();

            assert aType != null;
            assert bType != null;

            if (!typesHaveSameShortName(aType, bType)) {
                return false;
            }
        }
        return true;
    }


    /* DECLARATION AND DESCRIPTOR STRICT MATCHING */
    static <Decl extends JetNamedDeclaration> boolean receiversMatch(
            @NotNull MemberNavigationStrategy<Decl, ?> navigationStrategy,
            @NotNull Decl declaration,
            @NotNull CallableDescriptor descriptor
    ) {
        JetTypeReference declarationReceiver = getReceiverType(declaration);
        ReceiverParameterDescriptor descriptorReceiver = descriptor.getReceiverParameter();
        if (declarationReceiver == null && descriptorReceiver == null) {
            return true;
        }
        if (declarationReceiver != null && descriptorReceiver != null) {
            return declarationReceiver.getText().equals(DescriptorRenderer.TEXT.renderType(descriptorReceiver.getType()));
        }
        return false;
    }

    static <Decl extends JetNamedDeclaration, Descr extends CallableDescriptor> boolean valueParametersTypesMatch(
            @NotNull MemberNavigationStrategy<Decl, Descr> navigationStrategy,
            @NotNull Decl declaration,
            @NotNull Descr descriptor
    ) {
        List<JetParameter> declarationParameters = getValueParameters(declaration);
        List<ValueParameterDescriptor> descriptorParameters = descriptor.getValueParameters();
        if (descriptorParameters.size() != declarationParameters.size()) {
            return false;
        }

        for (int i = 0; i < descriptorParameters.size(); i++) {
            ValueParameterDescriptor descriptorParameter = descriptorParameters.get(i);
            JetParameter declarationParameter = declarationParameters.get(i);
            JetTypeReference typeReference = declarationParameter.getTypeReference();
            if (typeReference == null) {
                return false;
            }
            JetModifierList modifierList = declarationParameter.getModifierList();
            boolean varargInDeclaration = modifierList != null && modifierList.hasModifier(JetTokens.VARARG_KEYWORD);
            boolean varargInDescriptor = descriptorParameter.getVarargElementType() != null;
            if (varargInDeclaration != varargInDescriptor) {
                return false;
            }
            String declarationTypeText = typeReference.getText();

            JetType typeToRender = varargInDeclaration ? descriptorParameter.getVarargElementType() : descriptorParameter.getType();
            assert typeToRender != null;
            String descriptorParameterText = DescriptorRenderer.TEXT.renderType(typeToRender);
            if (!declarationTypeText.equals(descriptorParameterText)) {
                return false;
            }
        }
        return true;
    }

    static boolean typeParametersMatch(
            @NotNull JetTypeParameterListOwner typeParameterListOwner,
            @NotNull List<TypeParameterDescriptor> typeParameterDescriptors
    ) {
        List<JetTypeParameter> decompiledParameters = typeParameterListOwner.getTypeParameters();
        if (decompiledParameters.size() != typeParameterDescriptors.size()) {
            return false;
        }

        Multimap<Name, String> decompiledParameterToBounds = Multimaps.newSetMultimap(
                Maps.<Name, Collection<String>>newHashMap(), CommonSuppliers.<String>getHashSetSupplier());
        for (JetTypeParameter parameter : decompiledParameters) {
            JetTypeReference extendsBound = parameter.getExtendsBound();
            if (extendsBound != null) {
                decompiledParameterToBounds.put(parameter.getNameAsName(), extendsBound.getText());
            }
        }

        for (JetTypeConstraint typeConstraint : typeParameterListOwner.getTypeConstraints()) {
            JetSimpleNameExpression typeParameterName = typeConstraint.getSubjectTypeParameterName();
            assert typeParameterName != null;

            JetTypeReference bound = typeConstraint.getBoundTypeReference();
            assert bound != null;

            decompiledParameterToBounds.put(typeParameterName.getReferencedNameAsName(), bound.getText());
        }

        for (int i = 0; i < decompiledParameters.size(); i++) {
            JetTypeParameter decompiledParameter = decompiledParameters.get(i);
            TypeParameterDescriptor descriptor = typeParameterDescriptors.get(i);

            Name name = decompiledParameter.getNameAsName();
            assert name != null;
            if (!name.equals(descriptor.getName())) {
                return false;
            }

            Set<String> descriptorUpperBounds = Sets.newHashSet(ContainerUtil.map(
                    descriptor.getUpperBounds(), new Function<JetType, String>() {
                @Override
                public String fun(JetType type) {
                    return DescriptorRenderer.TEXT.renderType(type);
                }
            }));

            Set<String> decompiledUpperBounds = decompiledParameterToBounds.get(descriptor.getName()).isEmpty()
                    ? Sets.newHashSet(DescriptorRenderer.TEXT.renderType(KotlinBuiltIns.getInstance().getDefaultBound()))
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
