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
import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.diagnostics.Errors;
import org.jetbrains.kotlin.lexer.JetModifierKeywordToken;
import org.jetbrains.kotlin.lexer.JetTokens;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.psi.psiUtil.PsiUtilPackage;
import org.jetbrains.kotlin.types.JetType;
import org.jetbrains.kotlin.types.SubstitutionUtils;
import org.jetbrains.kotlin.types.TypeConstructor;
import org.jetbrains.kotlin.types.TypeProjection;
import org.jetbrains.kotlin.types.checker.JetTypeChecker;

import java.util.*;

import static org.jetbrains.kotlin.diagnostics.Errors.*;
import static org.jetbrains.kotlin.resolve.BindingContext.TYPE;
import static org.jetbrains.kotlin.resolve.BindingContext.TYPE_PARAMETER;
import static org.jetbrains.kotlin.resolve.DescriptorUtils.classCanHaveAbstractMembers;
import static org.jetbrains.kotlin.resolve.DescriptorUtils.classCanHaveOpenMembers;

public class DeclarationsChecker {
    @NotNull private final BindingTrace trace;
    @NotNull private final ModifiersChecker.ModifiersCheckingProcedure modifiersChecker;
    @NotNull private final DescriptorResolver descriptorResolver;

    public DeclarationsChecker(
            @NotNull DescriptorResolver descriptorResolver,
            @NotNull ModifiersChecker modifiersChecker,
            @NotNull BindingTrace trace
    ) {
        this.descriptorResolver = descriptorResolver;
        this.modifiersChecker = modifiersChecker.withTrace(trace);
        this.trace = trace;
    }

    public void process(@NotNull BodiesResolveContext bodiesResolveContext) {
        for (JetFile file : bodiesResolveContext.getFiles()) {
            checkModifiersAndAnnotationsInPackageDirective(file);
            AnnotationChecker.INSTANCE$.check(file, trace, null);
        }

        Map<JetClassOrObject, ClassDescriptorWithResolutionScopes> classes = bodiesResolveContext.getDeclaredClasses();
        for (Map.Entry<JetClassOrObject, ClassDescriptorWithResolutionScopes> entry : classes.entrySet()) {
            JetClassOrObject classOrObject = entry.getKey();
            ClassDescriptorWithResolutionScopes classDescriptor = entry.getValue();

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

            checkPrimaryConstructor(classOrObject, classDescriptor);

            modifiersChecker.checkModifiersForDeclaration(classOrObject, classDescriptor);
        }

        Map<JetNamedFunction, SimpleFunctionDescriptor> functions = bodiesResolveContext.getFunctions();
        for (Map.Entry<JetNamedFunction, SimpleFunctionDescriptor> entry : functions.entrySet()) {
            JetNamedFunction function = entry.getKey();
            SimpleFunctionDescriptor functionDescriptor = entry.getValue();

            checkFunction(function, functionDescriptor);
            modifiersChecker.checkModifiersForDeclaration(function, functionDescriptor);
        }

        Map<JetProperty, PropertyDescriptor> properties = bodiesResolveContext.getProperties();
        for (Map.Entry<JetProperty, PropertyDescriptor> entry : properties.entrySet()) {
            JetProperty property = entry.getKey();
            PropertyDescriptor propertyDescriptor = entry.getValue();

            checkProperty(property, propertyDescriptor);
            modifiersChecker.checkModifiersForDeclaration(property, propertyDescriptor);
        }

        for (Map.Entry<JetSecondaryConstructor, ConstructorDescriptor> entry : bodiesResolveContext.getSecondaryConstructors().entrySet()) {
            ConstructorDescriptor constructorDescriptor = entry.getValue();
            JetSecondaryConstructor declaration = entry.getKey();
            checkConstructorDeclaration(constructorDescriptor, declaration);
        }

    }

    private void checkConstructorDeclaration(ConstructorDescriptor constructorDescriptor, JetDeclaration declaration) {
        modifiersChecker.checkModifiersForDeclaration(declaration, constructorDescriptor);
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
        AnnotationChecker.INSTANCE$.check(packageDirective, trace, null);
        ModifierCheckerCore.INSTANCE$.check(packageDirective, trace, null);
    }

    private void checkTypesInClassHeader(@NotNull JetClassOrObject classOrObject) {
        for (JetDelegationSpecifier delegationSpecifier : classOrObject.getDelegationSpecifiers()) {
            checkBoundsForTypeInClassHeader(delegationSpecifier.getTypeReference());
        }

        if (!(classOrObject instanceof JetClass)) return;
        JetClass jetClass = (JetClass) classOrObject;

        for (JetTypeParameter jetTypeParameter : jetClass.getTypeParameters()) {
            checkBoundsForTypeInClassHeader(jetTypeParameter.getExtendsBound());
            checkFinalUpperBounds(jetTypeParameter.getExtendsBound());
        }

        for (JetTypeConstraint constraint : jetClass.getTypeConstraints()) {
            checkBoundsForTypeInClassHeader(constraint.getBoundTypeReference());
            checkFinalUpperBounds(constraint.getBoundTypeReference());
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

    private void checkFinalUpperBounds(@Nullable JetTypeReference typeReference) {
        if (typeReference != null) {
            JetType type = trace.getBindingContext().get(TYPE, typeReference);
            if (type != null) {
                DescriptorResolver.checkUpperBoundType(typeReference, type, trace);
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
                removeDuplicateTypes(conflictingTypes);
                if (conflictingTypes.size() > 1) {
                    DeclarationDescriptor containingDeclaration = typeParameterDescriptor.getContainingDeclaration();
                    assert containingDeclaration instanceof ClassDescriptor : containingDeclaration;
                    JetClassOrObject psiElement = (JetClassOrObject) DescriptorToSourceUtils.getSourceFromDescriptor(classDescriptor);
                    assert psiElement != null;
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

    private static void removeDuplicateTypes(Set<JetType> conflictingTypes) {
        for (Iterator<JetType> iterator = conflictingTypes.iterator(); iterator.hasNext(); ) {
            JetType type = iterator.next();
            for (JetType otherType : conflictingTypes) {
                boolean subtypeOf = JetTypeChecker.DEFAULT.equalTypes(type, otherType);
                if (type != otherType && subtypeOf) {
                    iterator.remove();
                    break;
                }
            }
        }
    }

    private void checkObject(JetObjectDeclaration declaration, ClassDescriptor classDescriptor) {
        if  (declaration.isLocal() && !declaration.isCompanion() && !declaration.isObjectLiteral()) {
            trace.report(LOCAL_OBJECT_NOT_ALLOWED.on(declaration, classDescriptor));
        }
    }

    private void checkClass(BodiesResolveContext c, JetClass aClass, ClassDescriptorWithResolutionScopes classDescriptor) {
        checkOpenMembers(classDescriptor);
        checkTypeParameters(aClass);

        if (aClass.isInterface()) {
            ASTNode traitKeyword = aClass.getNode().findChildByType(JetTokens.TRAIT_KEYWORD);
            if (traitKeyword != null) {
                trace.report(Errors.DEPRECATED_TRAIT_KEYWORD.on(traitKeyword.getPsi()));
            }

            checkConstructorInTrait(aClass);
        }
        else if (classDescriptor.getKind() == ClassKind.ANNOTATION_CLASS) {
            checkAnnotationClassWithBody(aClass);
            checkValOnAnnotationParameter(aClass);
        }
        else if (aClass instanceof JetEnumEntry) {
            checkEnumEntry((JetEnumEntry) aClass, classDescriptor);
        }
    }

    private void checkPrimaryConstructor(JetClassOrObject classOrObject, ClassDescriptor classDescriptor) {
        ConstructorDescriptor primaryConstructor = classDescriptor.getUnsubstitutedPrimaryConstructor();
        JetPrimaryConstructor declaration = classOrObject.getPrimaryConstructor();
        if (primaryConstructor == null || declaration == null) return;

        for (JetParameter parameter : declaration.getValueParameters()) {
            PropertyDescriptor propertyDescriptor = trace.get(BindingContext.PRIMARY_CONSTRUCTOR_PARAMETER, parameter);
            if (propertyDescriptor != null) {
                modifiersChecker.checkModifiersForDeclaration(parameter, propertyDescriptor);
            }
        }

        if (declaration.getModifierList() != null && !declaration.hasConstructorKeyword()) {
            trace.report(MISSING_CONSTRUCTOR_KEYWORD.on(declaration.getModifierList()));
        }

        if (!(classOrObject instanceof JetClass)) {
            trace.report(CONSTRUCTOR_IN_OBJECT.on(declaration));
        }

        checkConstructorDeclaration(primaryConstructor, declaration);
    }

    private void checkTypeParameters(JetTypeParameterListOwner typeParameterListOwner) {
        // TODO: Support annotation for type parameters
        for (JetTypeParameter jetTypeParameter : typeParameterListOwner.getTypeParameters()) {
            AnnotationResolver.reportUnsupportedAnnotationForTypeParameter(jetTypeParameter, trace);

            TypeParameterDescriptor typeParameter = trace.get(TYPE_PARAMETER, jetTypeParameter);
            if (typeParameter != null) {
                DescriptorResolver.checkConflictingUpperBounds(trace, typeParameter, jetTypeParameter);
            }
            AnnotationChecker.INSTANCE$.check(jetTypeParameter, trace, null);
        }
    }

    private void checkConstructorInTrait(JetClass klass) {
        JetPrimaryConstructor primaryConstructor = klass.getPrimaryConstructor();
        if (primaryConstructor != null) {
            trace.report(CONSTRUCTOR_IN_TRAIT.on(primaryConstructor));
        }
    }

    private void checkAnnotationClassWithBody(JetClassOrObject classOrObject) {
        if (classOrObject.getBody() != null) {
            trace.report(ANNOTATION_CLASS_WITH_BODY.on(classOrObject.getBody()));
        }
    }

    private void checkValOnAnnotationParameter(JetClass aClass) {
        for (JetParameter parameter : aClass.getPrimaryConstructorParameters()) {
            if (!parameter.hasValOrVar()) {
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
        DeclarationDescriptor containingDeclaration = propertyDescriptor.getContainingDeclaration();
        if (containingDeclaration instanceof ClassDescriptor) {
            checkPropertyAbstractness(property, propertyDescriptor, (ClassDescriptor) containingDeclaration);
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
            if (classDescriptor.getKind() == ClassKind.INTERFACE) {
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
        boolean inTrait = containingDeclaration instanceof ClassDescriptor && ((ClassDescriptor)containingDeclaration).getKind() == ClassKind.INTERFACE;
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
                Boolean.TRUE.equals(trace.getBindingContext().get(BindingContext.IS_UNINITIALIZED, propertyDescriptor))) {
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
        DeclarationDescriptor containingDescriptor = functionDescriptor.getContainingDeclaration();
        boolean hasAbstractModifier = function.hasModifier(JetTokens.ABSTRACT_KEYWORD);
        checkDeclaredTypeInPublicMember(function, functionDescriptor);
        if (containingDescriptor instanceof ClassDescriptor) {
            ClassDescriptor classDescriptor = (ClassDescriptor) containingDescriptor;
            boolean inTrait = classDescriptor.getKind() == ClassKind.INTERFACE;
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
        if (!function.hasBody() && !hasAbstractModifier) {
            trace.report(NON_MEMBER_FUNCTION_NO_BODY.on(function, functionDescriptor));
        }
    }

    private void checkAccessors(@NotNull JetProperty property, @NotNull PropertyDescriptor propertyDescriptor) {
        for (JetPropertyAccessor accessor : property.getAccessors()) {
            PropertyAccessorDescriptor propertyAccessorDescriptor = accessor.isGetter() ? propertyDescriptor.getGetter() : propertyDescriptor.getSetter();
            assert propertyAccessorDescriptor != null : "No property accessor descriptor for " + property.getText();
            modifiersChecker.checkModifiersForDeclaration(accessor, propertyAccessorDescriptor);
        }
        JetPropertyAccessor getter = property.getGetter();
        PropertyGetterDescriptor getterDescriptor = propertyDescriptor.getGetter();
        JetModifierList getterModifierList = getter != null ? getter.getModifierList() : null;
        if (getterModifierList != null && getterDescriptor != null) {
            Map<JetModifierKeywordToken, ASTNode> nodes = modifiersChecker.getNodesCorrespondingToModifiers(getterModifierList, Sets
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

    // Temporary
    // Returns true if deprecated constructor is in use, like
    // ENTRY: Enum(arguments) instead of
    // ENTRY(arguments)
    public static boolean enumEntryUsesDeprecatedSuperConstructor(@NotNull JetEnumEntry enumEntry) {
        JetInitializerList initializerList = enumEntry.getInitializerList();
        if (initializerList == null || initializerList.getInitializers().isEmpty()) return false;
        JetTypeReference typeReference = initializerList.getInitializers().get(0).getTypeReference();
        if (typeReference == null) return false;
        JetUserType userType = (JetUserType) typeReference.getTypeElement();
        if (userType == null || userType.getReferenceExpression() instanceof JetEnumEntrySuperclassReferenceExpression) return false;
        return true;
    }

    // Temporary
    // Returns comma if it's an enum entry without following comma (entry is not last in enum),
    // or semicolon if it's an enum entry without following semicolon, may be after comma (entry is last in enum),
    // or empty string if an enum entry has the necessary following delimiter
    @NotNull
    private static String enumEntryExpectedDelimiter(@NotNull JetEnumEntry enumEntry) {
        PsiElement next = enumEntry.getNextSibling();
        while (next != null) {
            if (next instanceof JetDeclaration) break;
            next = next.getNextSibling();
        }
        JetDeclaration nextDeclaration = (JetDeclaration) next;
        next = PsiUtilPackage.getNextSiblingIgnoringWhitespaceAndComments(enumEntry);
        IElementType nextType = next != null ? next.getNode().getElementType() : null;
        if (nextDeclaration instanceof JetEnumEntry) {
            // Not last
            return nextType != JetTokens.COMMA ? "," : "";
        }
        else {
            // Last: after it we can have semicolon, just closing brace, or comma followed by semicolon / closing brace
            if (nextType == JetTokens.COMMA) {
                next = PsiUtilPackage.getNextSiblingIgnoringWhitespaceAndComments(next);
                nextType = next != null ? next.getNode().getElementType() : null;
            }
            return nextType != JetTokens.SEMICOLON && nextType != JetTokens.RBRACE ? ";" : "";
        }
    }

    public static boolean enumEntryUsesDeprecatedOrNoDelimiter(@NotNull JetEnumEntry enumEntry) {
        return !enumEntryExpectedDelimiter(enumEntry).isEmpty();
    }

    static public boolean enumEntryAfterEnumMember(@NotNull JetEnumEntry enumEntry) {
        PsiElement previous = enumEntry.getPrevSibling();
        while (previous != null) {
            if (previous instanceof JetEnumEntry) return false;
            if (previous instanceof JetDeclaration) return true;
            previous = previous.getPrevSibling();
        }
        return false;
    }

    private void checkEnumEntry(@NotNull JetEnumEntry enumEntry, @NotNull ClassDescriptor classDescriptor) {
        if (enumEntryUsesDeprecatedSuperConstructor(enumEntry)) {
            trace.report(Errors.ENUM_ENTRY_USES_DEPRECATED_SUPER_CONSTRUCTOR.on(enumEntry, classDescriptor));
        }
        String neededDelimiter = enumEntryExpectedDelimiter(enumEntry);
        if (!neededDelimiter.isEmpty()) {
            trace.report(Errors.ENUM_ENTRY_USES_DEPRECATED_OR_NO_DELIMITER.on(enumEntry, classDescriptor, neededDelimiter));
        }
        if (enumEntryAfterEnumMember(enumEntry)) {
            trace.report(Errors.ENUM_ENTRY_AFTER_ENUM_MEMBER.on(enumEntry, classDescriptor));
        }

        DeclarationDescriptor declaration = classDescriptor.getContainingDeclaration();
        if (DescriptorUtils.isEnumClass(declaration)) {
            ClassDescriptor enumClass = (ClassDescriptor) declaration;

            List<JetDelegationSpecifier> delegationSpecifiers = enumEntry.getDelegationSpecifiers();
            ConstructorDescriptor constructor = enumClass.getUnsubstitutedPrimaryConstructor();
            if ((constructor == null || !constructor.getValueParameters().isEmpty()) && delegationSpecifiers.isEmpty()) {
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
        else {
            assert DescriptorUtils.isTrait(declaration) : "Enum entry should be declared in enum class: " +
                                                          classDescriptor + " " +
                                                          classDescriptor.getKind();
        }
    }

}
