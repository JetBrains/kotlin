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

package org.jetbrains.jet.lang.resolve;

import com.google.common.collect.Sets;
import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.descriptors.impl.MutableClassDescriptor;
import org.jetbrains.jet.lang.diagnostics.Errors;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.types.DeferredType;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lexer.JetKeywordToken;
import org.jetbrains.jet.lexer.JetTokens;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;

import static org.jetbrains.jet.lang.diagnostics.Errors.*;
import static org.jetbrains.jet.lang.resolve.BindingContext.TYPE;

public class DeclarationsChecker {
    @NotNull
    private BindingTrace trace;
    @NotNull
    private ModifiersChecker modifiersChecker;

    @Inject
    public void setTrace(@NotNull BindingTrace trace) {
        this.trace = trace;
        this.modifiersChecker = new ModifiersChecker(trace);
    }

    public void process(@NotNull BodiesResolveContext bodiesResolveContext) {
        Map<JetClass, MutableClassDescriptor> classes = bodiesResolveContext.getClasses();
        for (Map.Entry<JetClass, MutableClassDescriptor> entry : classes.entrySet()) {
            JetClass aClass = entry.getKey();
            MutableClassDescriptor classDescriptor = entry.getValue();
            if (!bodiesResolveContext.completeAnalysisNeeded(aClass)) continue;

            checkClass(aClass, classDescriptor);
            modifiersChecker.checkModifiersForDeclaration(aClass, classDescriptor);
        }

        Map<JetObjectDeclaration, MutableClassDescriptor> objects = bodiesResolveContext.getObjects();
        for (Map.Entry<JetObjectDeclaration, MutableClassDescriptor> entry : objects.entrySet()) {
            JetObjectDeclaration objectDeclaration = entry.getKey();
            MutableClassDescriptor objectDescriptor = entry.getValue();

            if (!bodiesResolveContext.completeAnalysisNeeded(objectDeclaration)) continue;
            checkObject(objectDeclaration);
            modifiersChecker.checkModifiersForDeclaration(objectDeclaration, objectDescriptor);
        }

        Map<JetNamedFunction, SimpleFunctionDescriptor> functions = bodiesResolveContext.getFunctions();
        for (Map.Entry<JetNamedFunction, SimpleFunctionDescriptor> entry : functions.entrySet()) {
            JetNamedFunction function = entry.getKey();
            SimpleFunctionDescriptor functionDescriptor = entry.getValue();

            if (!bodiesResolveContext.completeAnalysisNeeded(function)) continue;
            checkFunction(function, functionDescriptor);
            modifiersChecker.checkModifiersForDeclaration(function, functionDescriptor);
        }

        Map<JetProperty, PropertyDescriptor> properties = bodiesResolveContext.getProperties();
        for (Map.Entry<JetProperty, PropertyDescriptor> entry : properties.entrySet()) {
            JetProperty property = entry.getKey();
            PropertyDescriptor propertyDescriptor = entry.getValue();

            if (!bodiesResolveContext.completeAnalysisNeeded(property)) continue;
            checkProperty(property, propertyDescriptor);
            modifiersChecker.checkModifiersForDeclaration(property, propertyDescriptor);
        }

    }

    private void reportErrorIfHasEnumModifier(JetModifierListOwner declaration) {
        if (declaration.hasModifier(JetTokens.ENUM_KEYWORD)) {
            trace.report(ILLEGAL_ENUM_ANNOTATION.on(declaration));
        }
    }
    
    private void checkObject(JetObjectDeclaration declaration) {
        reportErrorIfHasEnumModifier(declaration);
    }

    private void checkClass(JetClass aClass, MutableClassDescriptor classDescriptor) {
        checkOpenMembers(classDescriptor);
        if (aClass.isTrait()) {
            checkTraitModifiers(aClass);
        }
        else if (classDescriptor.getKind() == ClassKind.ENUM_CLASS) {
            checkEnumModifiers(aClass);
        }
        else if (classDescriptor.getKind() == ClassKind.ENUM_ENTRY) {
            checkEnumEntry(aClass, classDescriptor);
        }
    }

    private void checkTraitModifiers(JetClass aClass) {
        reportErrorIfHasEnumModifier(aClass);
        JetModifierList modifierList = aClass.getModifierList();
        if (modifierList == null) return;
        if (modifierList.hasModifier(JetTokens.FINAL_KEYWORD)) {
            trace.report(Errors.TRAIT_CAN_NOT_BE_FINAL.on(modifierList.getModifierNode(JetTokens.FINAL_KEYWORD).getPsi()));
        }
        if (modifierList.hasModifier(JetTokens.ABSTRACT_KEYWORD)) {
            trace.report(Errors.ABSTRACT_MODIFIER_IN_TRAIT.on(aClass));
        }
        if (modifierList.hasModifier(JetTokens.OPEN_KEYWORD)) {
            trace.report(Errors.OPEN_MODIFIER_IN_TRAIT.on(aClass));
        }
    }


    private void checkOpenMembers(MutableClassDescriptor classDescriptor) {
        for (CallableMemberDescriptor memberDescriptor : classDescriptor.getDeclaredCallableMembers()) {
            if (memberDescriptor.getKind() != CallableMemberDescriptor.Kind.DECLARATION) continue;
            JetNamedDeclaration member = (JetNamedDeclaration) BindingContextUtils.descriptorToDeclaration(trace.getBindingContext(), memberDescriptor);
            if (member != null && classDescriptor.getModality() == Modality.FINAL && member.hasModifier(JetTokens.OPEN_KEYWORD)) {
                trace.report(NON_FINAL_MEMBER_IN_FINAL_CLASS.on(member));
            }
        }
    }

    private void checkProperty(JetProperty property, PropertyDescriptor propertyDescriptor) {
        reportErrorIfHasEnumModifier(property);
        DeclarationDescriptor containingDeclaration = propertyDescriptor.getContainingDeclaration();
        if (containingDeclaration instanceof ClassDescriptor) {
            checkPropertyAbstractness(property, propertyDescriptor, (ClassDescriptor) containingDeclaration);
        }
        else {
            modifiersChecker.checkIllegalModalityModifiers(property);
        }
        checkPropertyInitializer(property, propertyDescriptor);
        checkAccessors(property, propertyDescriptor);
        checkDeclaredTypeInPublicMember(property, propertyDescriptor);
    }

    private void checkDeclaredTypeInPublicMember(JetNamedDeclaration member, CallableMemberDescriptor memberDescriptor) {
        boolean hasDeferredType;
        if (member instanceof JetProperty) {
            hasDeferredType = ((JetProperty) member).getTypeRef() == null && DescriptorResolver.hasBody((JetProperty) member);
        }
        else {
            assert member instanceof JetFunction;
            JetFunction function = (JetFunction) member;
            hasDeferredType = function.getReturnTypeRef() == null && function.getBodyExpression() != null && !function.hasBlockBody();
        }
        if ((memberDescriptor.getVisibility().isPublicAPI()) && memberDescriptor.getOverriddenDescriptors().size() == 0 && hasDeferredType) {
            trace.report(PUBLIC_MEMBER_SHOULD_SPECIFY_TYPE.on(member));
        }
    }

    private void checkPropertyAbstractness(
            @NotNull JetProperty property,
            @NotNull PropertyDescriptor propertyDescriptor,
            @NotNull ClassDescriptor classDescriptor
    ) {
        JetPropertyAccessor getter = property.getGetter();
        JetPropertyAccessor setter = property.getSetter();
        JetModifierList modifierList = property.getModifierList();
        ASTNode abstractNode = modifierList != null ? modifierList.getModifierNode(JetTokens.ABSTRACT_KEYWORD) : null;

        if (abstractNode != null) { //has abstract modifier
            if (!(classDescriptor.getModality() == Modality.ABSTRACT) && classDescriptor.getKind() != ClassKind.ENUM_CLASS) {
                String name = property.getName();
                trace.report(ABSTRACT_PROPERTY_IN_NON_ABSTRACT_CLASS.on(property, name != null ? name : "", classDescriptor));
                return;
            }
            if (classDescriptor.getKind() == ClassKind.TRAIT) {
                trace.report(ABSTRACT_MODIFIER_IN_TRAIT.on(property));
            }
        }

        if (propertyDescriptor.getModality() == Modality.ABSTRACT) {
            JetType returnType = propertyDescriptor.getReturnType();
            if (returnType instanceof DeferredType) {
                returnType = ((DeferredType) returnType).getActualType();
            }

            JetExpression initializer = property.getInitializer();
            if (initializer != null) {
                trace.report(ABSTRACT_PROPERTY_WITH_INITIALIZER.on(initializer));
            }
            if (getter != null && getter.getBodyExpression() != null) {
                trace.report(ABSTRACT_PROPERTY_WITH_GETTER.on(getter));
            }
            if (setter != null && setter.getBodyExpression() != null) {
                trace.report(ABSTRACT_PROPERTY_WITH_SETTER.on(setter));
            }
        }
    }

    private void checkPropertyInitializer(
            @NotNull JetProperty property,
            @NotNull PropertyDescriptor propertyDescriptor
    ) {
        JetPropertyAccessor getter = property.getGetter();
        JetPropertyAccessor setter = property.getSetter();
        boolean hasAccessorImplementation = (getter != null && getter.getBodyExpression() != null) ||
                                            (setter != null && setter.getBodyExpression() != null);

        if (propertyDescriptor.getModality() == Modality.ABSTRACT) {
            if (property.getInitializer() == null && property.getTypeRef() == null) {
                trace.report(PROPERTY_WITH_NO_TYPE_NO_INITIALIZER.on(property));
            }
            return;
        }
        DeclarationDescriptor containingDeclaration = propertyDescriptor.getContainingDeclaration();
        boolean inTrait = containingDeclaration instanceof ClassDescriptor && ((ClassDescriptor)containingDeclaration).getKind() == ClassKind.TRAIT;
        JetExpression initializer = property.getInitializer();
        boolean backingFieldRequired = trace.getBindingContext().get(BindingContext.BACKING_FIELD_REQUIRED, propertyDescriptor);

        if (inTrait && backingFieldRequired && hasAccessorImplementation) {
            trace.report(BACKING_FIELD_IN_TRAIT.on(property));
        }
        if (initializer == null) {
            boolean error = false;
            if (backingFieldRequired && !inTrait && !trace.getBindingContext().get(BindingContext.IS_INITIALIZED, propertyDescriptor)) {
                if (!(containingDeclaration instanceof ClassDescriptor) || hasAccessorImplementation) {
                    error = true;
                    trace.report(MUST_BE_INITIALIZED.on(property));
                }
                else {
                    error = true;
                    trace.report(MUST_BE_INITIALIZED_OR_BE_ABSTRACT.on(property));
                }
            }
            if (!error && property.getTypeRef() == null) {
                trace.report(PROPERTY_WITH_NO_TYPE_NO_INITIALIZER.on(property));
            }
            if (inTrait && property.hasModifier(JetTokens.FINAL_KEYWORD) && backingFieldRequired) {
                trace.report(FINAL_PROPERTY_IN_TRAIT.on(property));
            }
            return;
        }
        if (inTrait) {
            trace.report(PROPERTY_INITIALIZER_IN_TRAIT.on(initializer));
        }
        else if (!backingFieldRequired) {
            trace.report(PROPERTY_INITIALIZER_NO_BACKING_FIELD.on(initializer));
        }
    }

    protected void checkFunction(JetNamedFunction function, SimpleFunctionDescriptor functionDescriptor) {
        reportErrorIfHasEnumModifier(function);
        DeclarationDescriptor containingDescriptor = functionDescriptor.getContainingDeclaration();
        boolean hasAbstractModifier = function.hasModifier(JetTokens.ABSTRACT_KEYWORD);
        checkDeclaredTypeInPublicMember(function, functionDescriptor);
        if (containingDescriptor instanceof ClassDescriptor) {
            ClassDescriptor classDescriptor = (ClassDescriptor) containingDescriptor;
            boolean inTrait = classDescriptor.getKind() == ClassKind.TRAIT;
            boolean inEnum = classDescriptor.getKind() == ClassKind.ENUM_CLASS;
            boolean inAbstractClass = classDescriptor.getModality() == Modality.ABSTRACT;
            if (hasAbstractModifier && !inAbstractClass && !inEnum) {
                trace.report(ABSTRACT_FUNCTION_IN_NON_ABSTRACT_CLASS.on(function, functionDescriptor.getName().getName(), classDescriptor));
            }
            if (hasAbstractModifier && inTrait) {
                trace.report(ABSTRACT_MODIFIER_IN_TRAIT.on(function));
            }
            boolean hasBody = function.getBodyExpression() != null;
            if (hasBody && hasAbstractModifier) {
                trace.report(ABSTRACT_FUNCTION_WITH_BODY.on(function, functionDescriptor));
            }
            if (!hasBody && function.hasModifier(JetTokens.FINAL_KEYWORD) && inTrait) {
                trace.report(FINAL_FUNCTION_WITH_NO_BODY.on(function, functionDescriptor));
            }
            if (!hasBody && !hasAbstractModifier && !inTrait) {
                trace.report(NON_ABSTRACT_FUNCTION_WITH_NO_BODY.on(function, functionDescriptor));
            }
            return;
        }
        modifiersChecker.checkIllegalModalityModifiers(function);
        if (function.getBodyExpression() == null && !hasAbstractModifier) {
            trace.report(NON_MEMBER_FUNCTION_NO_BODY.on(function, functionDescriptor));
        }
    }

    private void checkAccessors(@NotNull JetProperty property, @NotNull PropertyDescriptor propertyDescriptor) {
        for (JetPropertyAccessor accessor : property.getAccessors()) {
            modifiersChecker.checkIllegalModalityModifiers(accessor);
        }
        JetPropertyAccessor getter = property.getGetter();
        PropertyGetterDescriptor getterDescriptor = propertyDescriptor.getGetter();
        JetModifierList getterModifierList = getter != null ? getter.getModifierList() : null;
        if (getterModifierList != null && getterDescriptor != null) {
            Map<JetKeywordToken, ASTNode> nodes = ModifiersChecker.getNodesCorrespondingToModifiers(getterModifierList, Sets
                    .newHashSet(JetTokens.PUBLIC_KEYWORD, JetTokens.PROTECTED_KEYWORD, JetTokens.PRIVATE_KEYWORD,
                                JetTokens.INTERNAL_KEYWORD));
            if (getterDescriptor.getVisibility() != propertyDescriptor.getVisibility()) {
                for (ASTNode node : nodes.values()) {
                    trace.report(Errors.GETTER_VISIBILITY_DIFFERS_FROM_PROPERTY_VISIBILITY.on(node.getPsi()));
                }
            }
            else {
                for (ASTNode node : nodes.values()) {
                    trace.report(Errors.REDUNDANT_MODIFIER_IN_GETTER.on(node.getPsi()));
                }
            }
        }
    }

    private void checkEnumModifiers(JetClass aClass) {
        if (aClass.hasModifier(JetTokens.OPEN_KEYWORD)) {
            trace.report(OPEN_MODIFIER_IN_ENUM.on(aClass));
        }
    }

    private void checkEnumEntry(JetClass aClass, ClassDescriptor classDescriptor) {
        DeclarationDescriptor declaration = classDescriptor.getContainingDeclaration().getContainingDeclaration();
        assert declaration instanceof ClassDescriptor;
        ClassDescriptor enumClass = (ClassDescriptor) declaration;
        assert enumClass.getKind() == ClassKind.ENUM_CLASS;

        List<JetDelegationSpecifier> delegationSpecifiers = aClass.getDelegationSpecifiers();
        ConstructorDescriptor constructor = enumClass.getUnsubstitutedPrimaryConstructor();
        assert constructor != null;
        if (!constructor.getValueParameters().isEmpty() && delegationSpecifiers.isEmpty()) {
            trace.report(ENUM_ENTRY_SHOULD_BE_INITIALIZED.on(aClass, enumClass));
        }

        for (JetDelegationSpecifier delegationSpecifier : delegationSpecifiers) {
            JetTypeReference typeReference = delegationSpecifier.getTypeReference();
            if (typeReference != null) {
                JetType type = trace.getBindingContext().get(TYPE, typeReference);
                if (type != null) {
                    JetType enumType = enumClass.getDefaultType();
                    if (!type.getConstructor().equals(enumType.getConstructor())) {
                        trace.report(ENUM_ENTRY_ILLEGAL_TYPE.on(typeReference, enumClass));
                    }
                }
            }
        }
    }
}
