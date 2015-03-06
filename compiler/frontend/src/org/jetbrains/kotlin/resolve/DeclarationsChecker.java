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

package org.jetbrains.kotlin.resolve;

import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.diagnostics.Errors;
import org.jetbrains.kotlin.lexer.JetModifierKeywordToken;
import org.jetbrains.kotlin.lexer.JetTokens;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.types.JetType;
import org.jetbrains.kotlin.types.SubstitutionUtils;
import org.jetbrains.kotlin.types.TypeConstructor;
import org.jetbrains.kotlin.types.TypeProjection;
import org.jetbrains.kotlin.types.checker.JetTypeChecker;

import javax.inject.Inject;
import java.util.*;

import static org.jetbrains.kotlin.diagnostics.Errors.*;
import static org.jetbrains.kotlin.resolve.BindingContext.TYPE;
import static org.jetbrains.kotlin.resolve.BindingContext.TYPE_PARAMETER;
import static org.jetbrains.kotlin.resolve.DescriptorUtils.classCanHaveAbstractMembers;
import static org.jetbrains.kotlin.resolve.DescriptorUtils.classCanHaveOpenMembers;

public class DeclarationsChecker {
    private BindingTrace trace;
    private ModifiersChecker modifiersChecker;
    private DescriptorResolver descriptorResolver;

    @Inject
    public void setTrace(@NotNull BindingTrace trace) {
        this.trace = trace;
    }

    @Inject
    public void setDescriptorResolver(@NotNull DescriptorResolver descriptorResolver) {
        this.descriptorResolver = descriptorResolver;
    }

    @Inject
    public void setModifiersChecker(@NotNull ModifiersChecker modifiersChecker) {
        this.modifiersChecker = modifiersChecker;
    }

    public void process(@NotNull BodiesResolveContext bodiesResolveContext) {
        for (JetFile file : bodiesResolveContext.getFiles()) {
            checkModifiersAndAnnotationsInPackageDirective(file);
        }

        Map<JetClassOrObject, ClassDescriptorWithResolutionScopes> classes = bodiesResolveContext.getDeclaredClasses();
        for (Map.Entry<JetClassOrObject, ClassDescriptorWithResolutionScopes> entry : classes.entrySet()) {
            JetClassOrObject classOrObject = entry.getKey();
            ClassDescriptorWithResolutionScopes classDescriptor = entry.getValue();
            if (!bodiesResolveContext.completeAnalysisNeeded(classOrObject)) continue;

            checkSupertypesForConsistency(classDescriptor);
            checkTypesInClassHeader(classOrObject);

            if (classOrObject instanceof JetClass) {
                JetClass jetClass = (JetClass) classOrObject;
                checkClass(bodiesResolveContext, jetClass, classDescriptor);
                descriptorResolver.checkNamesInConstraints(
                        jetClass, classDescriptor, classDescriptor.getScopeForClassHeaderResolution(), trace);
            }
            else if (classOrObject instanceof JetObjectDeclaration) {
                checkObject((JetObjectDeclaration) classOrObject, classDescriptor);
            }

            modifiersChecker.checkModifiersForDeclaration(classOrObject, classDescriptor);
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

    private void reportErrorIfHasIllegalModifier(JetModifierListOwner declaration) {
        if (declaration.hasModifier(JetTokens.ENUM_KEYWORD)) {
            trace.report(ILLEGAL_ENUM_ANNOTATION.on(declaration));
        }
        if (declaration.hasModifier(JetTokens.ANNOTATION_KEYWORD)) {
            trace.report(ILLEGAL_ANNOTATION_KEYWORD.on(declaration));
        }
    }

    private void checkModifiersAndAnnotationsInPackageDirective(JetFile file) {
        JetPackageDirective packageDirective = file.getPackageDirective();
        if (packageDirective == null) return;

        JetModifierList modifierList = packageDirective.getModifierList();
        if (modifierList == null) return;

        for (JetAnnotationEntry annotationEntry : modifierList.getAnnotationEntries()) {
            JetConstructorCalleeExpression calleeExpression = annotationEntry.getCalleeExpression();
            if (calleeExpression != null) {
                JetReferenceExpression reference = calleeExpression.getConstructorReferenceExpression();
                if (reference != null) {
                    trace.report(UNRESOLVED_REFERENCE.on(reference, reference));
                }
            }
        }

        ModifiersChecker.reportIllegalModifiers(modifierList, Arrays.asList(JetTokens.MODIFIER_KEYWORDS_ARRAY), trace);
    }

    private void checkTypesInClassHeader(@NotNull JetClassOrObject classOrObject) {
        for (JetDelegationSpecifier delegationSpecifier : classOrObject.getDelegationSpecifiers()) {
            checkBoundsForTypeInClassHeader(delegationSpecifier.getTypeReference());
        }

        if (!(classOrObject instanceof JetClass)) return;
        JetClass jetClass = (JetClass) classOrObject;

        for (JetTypeParameter jetTypeParameter : jetClass.getTypeParameters()) {
            checkBoundsForTypeInClassHeader(jetTypeParameter.getExtendsBound());
            checkFinalUpperBounds(jetTypeParameter.getExtendsBound(), false);
        }

        for (JetTypeConstraint constraint : jetClass.getTypeConstraints()) {
            checkBoundsForTypeInClassHeader(constraint.getBoundTypeReference());
            checkFinalUpperBounds(constraint.getBoundTypeReference(), constraint.isDefaultObjectConstraint());
        }
    }

    private void checkBoundsForTypeInClassHeader(@Nullable JetTypeReference typeReference) {
        if (typeReference != null) {
            JetType type = trace.getBindingContext().get(TYPE, typeReference);
            if (type != null) {
                DescriptorResolver.checkBounds(typeReference, type, trace);
            }
        }
    }

    private void checkFinalUpperBounds(@Nullable JetTypeReference typeReference, boolean isDefaultObjectConstraint) {
        if (typeReference != null) {
            JetType type = trace.getBindingContext().get(TYPE, typeReference);
            if (type != null) {
                DescriptorResolver.checkUpperBoundType(typeReference, type, isDefaultObjectConstraint, trace);
            }
        }
    }

    private void checkSupertypesForConsistency(@NotNull ClassDescriptor classDescriptor) {
        Multimap<TypeConstructor, TypeProjection> multimap = SubstitutionUtils
                .buildDeepSubstitutionMultimap(classDescriptor.getDefaultType());
        for (Map.Entry<TypeConstructor, Collection<TypeProjection>> entry : multimap.asMap().entrySet()) {
            Collection<TypeProjection> projections = entry.getValue();
            if (projections.size() > 1) {
                TypeConstructor typeConstructor = entry.getKey();
                DeclarationDescriptor declarationDescriptor = typeConstructor.getDeclarationDescriptor();
                assert declarationDescriptor instanceof TypeParameterDescriptor : declarationDescriptor;
                TypeParameterDescriptor typeParameterDescriptor = (TypeParameterDescriptor) declarationDescriptor;

                // Immediate arguments of supertypes cannot be projected
                Set<JetType> conflictingTypes = Sets.newLinkedHashSet();
                for (TypeProjection projection : projections) {
                    conflictingTypes.add(projection.getType());
                }
                switch (typeParameterDescriptor.getVariance()) {
                    case INVARIANT:
                        // Leave conflicting types as is
                        Filter.REMOVE_IF_EQUAL_TYPE_IN_THE_SET.proceed(conflictingTypes);
                        break;
                    case IN_VARIANCE:
                        // Filter out those who have supertypes in this set (common supertype)
                        Filter.REMOVE_IF_SUPERTYPE_IN_THE_SET.proceed(conflictingTypes);
                        break;
                    case OUT_VARIANCE:
                        // Filter out those who have subtypes in this set (common subtype)
                        Filter.REMOVE_IF_SUBTYPE_IN_THE_SET.proceed(conflictingTypes);
                        break;
                }

                if (conflictingTypes.size() > 1) {
                    DeclarationDescriptor containingDeclaration = typeParameterDescriptor.getContainingDeclaration();
                    assert containingDeclaration instanceof ClassDescriptor : containingDeclaration;
                    JetClassOrObject psiElement = (JetClassOrObject) DescriptorToSourceUtils.getSourceFromDescriptor(classDescriptor);
                    JetDelegationSpecifierList delegationSpecifierList = psiElement.getDelegationSpecifierList();
                    assert delegationSpecifierList != null;
                    //                        trace.getErrorHandler().genericError(delegationSpecifierList.getNode(), "Type parameter " + typeParameterDescriptor.getName() + " of " + containingDeclaration.getName() + " has inconsistent values: " + conflictingTypes);
                    trace.report(INCONSISTENT_TYPE_PARAMETER_VALUES
                                         .on(delegationSpecifierList, typeParameterDescriptor, (ClassDescriptor) containingDeclaration,
                                             conflictingTypes));
                }
            }
        }
    }

    private enum Filter {
        REMOVE_IF_SUBTYPE_IN_THE_SET {
            @Override
            public boolean removeNeeded(JetType subject, JetType other) {
                return JetTypeChecker.DEFAULT.isSubtypeOf(other, subject);
            }
        },
        REMOVE_IF_SUPERTYPE_IN_THE_SET {
            @Override
            public boolean removeNeeded(JetType subject, JetType other) {
                return JetTypeChecker.DEFAULT.isSubtypeOf(subject, other);
            }
        },
        REMOVE_IF_EQUAL_TYPE_IN_THE_SET {
            @Override
            public boolean removeNeeded(JetType subject, JetType other) {
                return JetTypeChecker.DEFAULT.equalTypes(subject, other);
            }
        };

        private void proceed(Set<JetType> conflictingTypes) {
            for (Iterator<JetType> iterator = conflictingTypes.iterator(); iterator.hasNext(); ) {
                JetType type = iterator.next();
                for (JetType otherType : conflictingTypes) {
                    boolean subtypeOf = removeNeeded(type, otherType);
                    if (type != otherType && subtypeOf) {
                        iterator.remove();
                        break;
                    }
                }
            }
        }

        public abstract boolean removeNeeded(JetType subject, JetType other);
    }

    private void checkObject(JetObjectDeclaration declaration, ClassDescriptor classDescriptor) {
        reportErrorIfHasIllegalModifier(declaration);
        if  (declaration.isLocal() && !declaration.isDefault() && !declaration.isObjectLiteral()) {
            trace.report(LOCAL_OBJECT_NOT_ALLOWED.on(declaration, classDescriptor));
        }
    }

    private void checkClass(BodiesResolveContext c, JetClass aClass, ClassDescriptorWithResolutionScopes classDescriptor) {
        checkOpenMembers(classDescriptor);
        checkConstructorParameters(aClass);
        if (c.getTopDownAnalysisParameters().isLazy()) {
            checkTypeParameters(aClass);
        }
        if (aClass.isTrait()) {
            checkTraitModifiers(aClass);
            checkConstructorInTrait(aClass);
        }
        else if (aClass.isAnnotation()) {
            checkAnnotationClassWithBody(aClass);
            checkValOnAnnotationParameter(aClass);
        }
        else if (aClass.isEnum()) {
            checkEnumModifiers(aClass);
            if (aClass.isLocal()) {
                trace.report(LOCAL_ENUM_NOT_ALLOWED.on(aClass, classDescriptor));
            }
        }
        else if (aClass instanceof JetEnumEntry) {
            checkEnumEntry((JetEnumEntry) aClass, classDescriptor);
        }
    }

    private void checkConstructorParameters(JetClass aClass) {
        for (JetParameter parameter : aClass.getPrimaryConstructorParameters()) {
            PropertyDescriptor propertyDescriptor = trace.get(BindingContext.PRIMARY_CONSTRUCTOR_PARAMETER, parameter);
            if (propertyDescriptor != null) {
                modifiersChecker.checkModifiersForDeclaration(parameter, propertyDescriptor);
            }
        }
    }

    private void checkTypeParameters(JetTypeParameterListOwner typeParameterListOwner) {
        // TODO: Support annotation for type parameters
        for (JetTypeParameter jetTypeParameter : typeParameterListOwner.getTypeParameters()) {
            AnnotationResolver.reportUnsupportedAnnotationForTypeParameter(jetTypeParameter, trace);

            TypeParameterDescriptor typeParameter = trace.get(TYPE_PARAMETER, jetTypeParameter);
            if (typeParameter != null) {
                DescriptorResolver.checkConflictingUpperBounds(trace, typeParameter, jetTypeParameter);
            }
        }
    }

    private void checkConstructorInTrait(JetClass klass) {
        JetParameterList primaryConstructorParameterList = klass.getPrimaryConstructorParameterList();
        if (primaryConstructorParameterList != null) {
            trace.report(CONSTRUCTOR_IN_TRAIT.on(primaryConstructorParameterList));
        }
    }

    private void checkTraitModifiers(JetClass aClass) {
        reportErrorIfHasIllegalModifier(aClass);
        JetModifierList modifierList = aClass.getModifierList();
        if (modifierList == null) return;
        if (modifierList.hasModifier(JetTokens.FINAL_KEYWORD)) {
            trace.report(Errors.TRAIT_CAN_NOT_BE_FINAL.on(aClass));
        }
        if (modifierList.hasModifier(JetTokens.ABSTRACT_KEYWORD)) {
            trace.report(Errors.ABSTRACT_MODIFIER_IN_TRAIT.on(aClass));
        }
        if (modifierList.hasModifier(JetTokens.OPEN_KEYWORD)) {
            trace.report(Errors.OPEN_MODIFIER_IN_TRAIT.on(aClass));
        }
    }

    private void checkAnnotationClassWithBody(JetClassOrObject classOrObject) {
        if (classOrObject.getBody() != null) {
            trace.report(ANNOTATION_CLASS_WITH_BODY.on(classOrObject.getBody()));
        }
    }

    private void checkValOnAnnotationParameter(JetClass aClass) {
        for (JetParameter parameter : aClass.getPrimaryConstructorParameters()) {
            if (!parameter.hasValOrVarNode()) {
                trace.report(MISSING_VAL_ON_ANNOTATION_PARAMETER.on(parameter));
            }
        }
    }

    private void checkOpenMembers(ClassDescriptorWithResolutionScopes classDescriptor) {
        if (classCanHaveOpenMembers(classDescriptor)) return;

        for (CallableMemberDescriptor memberDescriptor : classDescriptor.getDeclaredCallableMembers()) {
            if (memberDescriptor.getKind() != CallableMemberDescriptor.Kind.DECLARATION) continue;
            JetNamedDeclaration member = (JetNamedDeclaration) DescriptorToSourceUtils.descriptorToDeclaration(memberDescriptor);
            if (member != null && member.hasModifier(JetTokens.OPEN_KEYWORD)) {
                trace.report(NON_FINAL_MEMBER_IN_FINAL_CLASS.on(member));
            }
        }
    }

    private void checkProperty(JetProperty property, PropertyDescriptor propertyDescriptor) {
        reportErrorIfHasIllegalModifier(property);
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
            hasDeferredType = ((JetProperty) member).getTypeReference() == null && DescriptorResolver.hasBody((JetProperty) member);
        }
        else {
            assert member instanceof JetFunction;
            JetFunction function = (JetFunction) member;
            hasDeferredType = function.getTypeReference() == null && function.hasBody() && !function.hasBlockBody();
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
            if (!classCanHaveAbstractMembers(classDescriptor)) {
                String name = property.getName();
                trace.report(ABSTRACT_PROPERTY_IN_NON_ABSTRACT_CLASS.on(property, name != null ? name : "", classDescriptor));
                return;
            }
            if (classDescriptor.getKind() == ClassKind.TRAIT) {
                trace.report(ABSTRACT_MODIFIER_IN_TRAIT.on(property));
            }
        }

        if (propertyDescriptor.getModality() == Modality.ABSTRACT) {
            JetExpression initializer = property.getInitializer();
            if (initializer != null) {
                trace.report(ABSTRACT_PROPERTY_WITH_INITIALIZER.on(initializer));
            }
            JetPropertyDelegate delegate = property.getDelegate();
            if (delegate != null) {
                trace.report(ABSTRACT_DELEGATED_PROPERTY.on(delegate));
            }
            if (getter != null && getter.hasBody()) {
                trace.report(ABSTRACT_PROPERTY_WITH_GETTER.on(getter));
            }
            if (setter != null && setter.hasBody()) {
                trace.report(ABSTRACT_PROPERTY_WITH_SETTER.on(setter));
            }
        }
    }

    private void checkPropertyInitializer(@NotNull JetProperty property, @NotNull PropertyDescriptor propertyDescriptor) {
        JetPropertyAccessor getter = property.getGetter();
        JetPropertyAccessor setter = property.getSetter();
        boolean hasAccessorImplementation = (getter != null && getter.hasBody()) ||
                                            (setter != null && setter.hasBody());

        if (propertyDescriptor.getModality() == Modality.ABSTRACT) {
            if (!property.hasDelegateExpressionOrInitializer() && property.getTypeReference() == null) {
                trace.report(PROPERTY_WITH_NO_TYPE_NO_INITIALIZER.on(property));
            }
            return;
        }
        DeclarationDescriptor containingDeclaration = propertyDescriptor.getContainingDeclaration();
        boolean inTrait = containingDeclaration instanceof ClassDescriptor && ((ClassDescriptor)containingDeclaration).getKind() == ClassKind.TRAIT;
        JetExpression initializer = property.getInitializer();
        JetPropertyDelegate delegate = property.getDelegate();
        boolean backingFieldRequired =
                Boolean.TRUE.equals(trace.getBindingContext().get(BindingContext.BACKING_FIELD_REQUIRED, propertyDescriptor));

        if (inTrait && backingFieldRequired && hasAccessorImplementation) {
            trace.report(BACKING_FIELD_IN_TRAIT.on(property));
        }

        if (initializer == null && delegate == null) {
            boolean error = false;
            if (backingFieldRequired && !inTrait &&
                Boolean.FALSE.equals(trace.getBindingContext().get(BindingContext.IS_INITIALIZED, propertyDescriptor))) {
                if (!(containingDeclaration instanceof ClassDescriptor) || hasAccessorImplementation) {
                    error = true;
                    trace.report(MUST_BE_INITIALIZED.on(property));
                }
                else {
                    error = true;
                    trace.report(MUST_BE_INITIALIZED_OR_BE_ABSTRACT.on(property));
                }
            }
            if (!error && property.getTypeReference() == null) {
                trace.report(PROPERTY_WITH_NO_TYPE_NO_INITIALIZER.on(property));
            }
            if (inTrait && property.hasModifier(JetTokens.FINAL_KEYWORD) && backingFieldRequired) {
                trace.report(FINAL_PROPERTY_IN_TRAIT.on(property));
            }
            return;
        }

        if (inTrait) {
            if (delegate != null) {
                trace.report(DELEGATED_PROPERTY_IN_TRAIT.on(delegate));
            }
            else {
                trace.report(PROPERTY_INITIALIZER_IN_TRAIT.on(initializer));
            }
        }
        else if (delegate == null) {
            if (!backingFieldRequired) {
                trace.report(PROPERTY_INITIALIZER_NO_BACKING_FIELD.on(initializer));
            }
            else if (property.getReceiverTypeReference() != null) {
                trace.report(EXTENSION_PROPERTY_WITH_BACKING_FIELD.on(initializer));
            }
        }
    }

    protected void checkFunction(JetNamedFunction function, SimpleFunctionDescriptor functionDescriptor) {
        reportErrorIfHasIllegalModifier(function);
        DeclarationDescriptor containingDescriptor = functionDescriptor.getContainingDeclaration();
        boolean hasAbstractModifier = function.hasModifier(JetTokens.ABSTRACT_KEYWORD);
        checkDeclaredTypeInPublicMember(function, functionDescriptor);
        if (containingDescriptor instanceof ClassDescriptor) {
            ClassDescriptor classDescriptor = (ClassDescriptor) containingDescriptor;
            boolean inTrait = classDescriptor.getKind() == ClassKind.TRAIT;
            if (hasAbstractModifier && !classCanHaveAbstractMembers(classDescriptor)) {
                trace.report(ABSTRACT_FUNCTION_IN_NON_ABSTRACT_CLASS.on(function, functionDescriptor.getName().asString(), classDescriptor));
            }
            if (hasAbstractModifier && inTrait) {
                trace.report(ABSTRACT_MODIFIER_IN_TRAIT.on(function));
            }
            boolean hasBody = function.hasBody();
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
        if (!function.hasBody() && !hasAbstractModifier) {
            trace.report(NON_MEMBER_FUNCTION_NO_BODY.on(function, functionDescriptor));
        }
    }

    private void checkAccessors(@NotNull JetProperty property, @NotNull PropertyDescriptor propertyDescriptor) {
        for (JetPropertyAccessor accessor : property.getAccessors()) {
            PropertyAccessorDescriptor propertyAccessorDescriptor = accessor.isGetter() ? propertyDescriptor.getGetter() : propertyDescriptor.getSetter();
            assert propertyAccessorDescriptor != null : "No property accessor descriptor for " + property.getText();
            modifiersChecker.checkModifiersForDeclaration(accessor, propertyAccessorDescriptor);
            modifiersChecker.checkIllegalModalityModifiers(accessor);
        }
        JetPropertyAccessor getter = property.getGetter();
        PropertyGetterDescriptor getterDescriptor = propertyDescriptor.getGetter();
        JetModifierList getterModifierList = getter != null ? getter.getModifierList() : null;
        if (getterModifierList != null && getterDescriptor != null) {
            Map<JetModifierKeywordToken, ASTNode> nodes = ModifiersChecker.getNodesCorrespondingToModifiers(getterModifierList, Sets
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
        if (aClass.hasModifier(JetTokens.ABSTRACT_KEYWORD)) {
            trace.report(ABSTRACT_MODIFIER_IN_ENUM.on(aClass));
        }
    }

    private void checkEnumEntry(@NotNull JetEnumEntry enumEntry, @NotNull ClassDescriptor classDescriptor) {
        DeclarationDescriptor declaration = classDescriptor.getContainingDeclaration();
        assert DescriptorUtils.isEnumClass(declaration) : "Enum entry should be declared in enum class: " + classDescriptor;
        ClassDescriptor enumClass = (ClassDescriptor) declaration;

        List<JetDelegationSpecifier> delegationSpecifiers = enumEntry.getDelegationSpecifiers();
        ConstructorDescriptor constructor = enumClass.getUnsubstitutedPrimaryConstructor();
        assert constructor != null;
        if (!constructor.getValueParameters().isEmpty() && delegationSpecifiers.isEmpty()) {
            trace.report(ENUM_ENTRY_SHOULD_BE_INITIALIZED.on(enumEntry, enumClass));
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
