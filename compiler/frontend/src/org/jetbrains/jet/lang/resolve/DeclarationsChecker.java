package org.jetbrains.jet.lang.resolve;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.intellij.lang.ASTNode;
import com.intellij.openapi.util.Pair;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.diagnostics.Errors;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.types.DeferredType;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lexer.JetKeywordToken;
import org.jetbrains.jet.lexer.JetToken;
import org.jetbrains.jet.lexer.JetTokens;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import static org.jetbrains.jet.lang.diagnostics.Errors.*;

/**
 * @author svtk
 */
public class DeclarationsChecker {
    private TopDownAnalysisContext context;

    public DeclarationsChecker(TopDownAnalysisContext context) {
        this.context = context;
    }

    public void process() {
        checkIfPrimaryConstructorIsNecessary();

        Map<JetClass, MutableClassDescriptor> classes = context.getClasses();
        for (Map.Entry<JetClass, MutableClassDescriptor> entry : classes.entrySet()) {
            JetClass aClass = entry.getKey();
            MutableClassDescriptor classDescriptor = entry.getValue();
            if (!context.completeAnalysisNeeded(aClass)) continue;

            checkClass(aClass, classDescriptor);
            checkModifiers(aClass.getModifierList());
        }

        Map<JetObjectDeclaration, MutableClassDescriptor> objects = context.getObjects();
        for (Map.Entry<JetObjectDeclaration, MutableClassDescriptor> entry : objects.entrySet()) {
            JetObjectDeclaration objectDeclaration = entry.getKey();
            MutableClassDescriptor objectDescriptor = entry.getValue();

            if (!context.completeAnalysisNeeded(objectDeclaration)) continue;
            checkObject(objectDeclaration, objectDescriptor);
        }

        Map<JetNamedFunction, FunctionDescriptorImpl> functions = context.getFunctions();
        for (Map.Entry<JetNamedFunction, FunctionDescriptorImpl> entry : functions.entrySet()) {
            JetNamedFunction function = entry.getKey();
            FunctionDescriptorImpl functionDescriptor = entry.getValue();
            
            if (!context.completeAnalysisNeeded(function)) continue;
            checkFunction(function, functionDescriptor);
            checkModifiers(function.getModifierList());
        }

        Map<JetProperty, PropertyDescriptor> properties = context.getProperties();
        for (Map.Entry<JetProperty, PropertyDescriptor> entry : properties.entrySet()) {
            JetProperty property = entry.getKey();
            PropertyDescriptor propertyDescriptor = entry.getValue();
            
            if (!context.completeAnalysisNeeded(property)) continue;
            checkProperty(property, propertyDescriptor);
            checkModifiers(property.getModifierList());
        }

    }

    private void checkIfPrimaryConstructorIsNecessary() {
        for (Map.Entry<JetClass, MutableClassDescriptor> entry : context.getClasses().entrySet()) {
            MutableClassDescriptor classDescriptor = entry.getValue();
            JetClass jetClass = entry.getKey();
            if (!context.completeAnalysisNeeded(jetClass)) return;
            if (classDescriptor.getUnsubstitutedPrimaryConstructor() == null && !(classDescriptor.getKind() == ClassKind.TRAIT)) {
                for (PropertyDescriptor propertyDescriptor : classDescriptor.getProperties()) {
                    if (context.getTrace().getBindingContext().get(BindingContext.BACKING_FIELD_REQUIRED, propertyDescriptor)) {
                        PsiElement nameIdentifier = jetClass.getNameIdentifier();
                        if (nameIdentifier != null) {
                            context.getTrace().report(PRIMARY_CONSTRUCTOR_MISSING_STATEFUL_PROPERTY.on(jetClass, nameIdentifier, propertyDescriptor));
                        }
                        break;
                    }
                }
            }
        }
    }

    private void checkClass(JetClass aClass, MutableClassDescriptor classDescriptor) {
        checkOpenMembers(aClass, classDescriptor);
        checkTraitModifiers(aClass);
    }

    private void checkTraitModifiers(JetClass aClass) {
        if (!aClass.isTrait()) return;
        JetModifierList modifierList = aClass.getModifierList();
        if (modifierList == null) return;
        if (modifierList.hasModifier(JetTokens.FINAL_KEYWORD)) {
            context.getTrace().report(Errors.TRAIT_CAN_NOT_BE_FINAL.on(aClass, modifierList.getModifierNode(JetTokens.FINAL_KEYWORD)));
        }
        ArrayList<JetKeywordToken> redundantModifiers = Lists.newArrayList(JetTokens.OPEN_KEYWORD, JetTokens.ABSTRACT_KEYWORD);
        for (JetKeywordToken modifier : redundantModifiers) {
            if (modifierList.hasModifier(modifier)) {
                context.getTrace().report(Errors.REDUNDANT_MODIFIER_IN_TRAIT.on(modifierList, modifierList.getModifierNode(modifier), modifier));
            }
        }
    }

    
    private void checkObject(JetObjectDeclaration objectDeclaration, MutableClassDescriptor classDescriptor) {
        checkIllegalInThisContextModifiers(objectDeclaration.getModifierList(), Sets.newHashSet(JetTokens.ABSTRACT_KEYWORD, JetTokens.OPEN_KEYWORD, JetTokens.OVERRIDE_KEYWORD));
    }

    private void checkOpenMembers(JetClass aClass, MutableClassDescriptor classDescriptor) {
            for (CallableMemberDescriptor memberDescriptor : classDescriptor.getCallableMembers()) {
    
                JetNamedDeclaration member = (JetNamedDeclaration) context.getTrace().get(BindingContext.DESCRIPTOR_TO_DECLARATION, memberDescriptor);
                if (member != null && classDescriptor.getModality() == Modality.FINAL && member.hasModifier(JetTokens.OPEN_KEYWORD)) {
                    JetModifierList modifierList = member.getModifierList();
                    assert  modifierList != null;
                    ASTNode openModifierNode = modifierList.getModifierNode(JetTokens.OPEN_KEYWORD);
                    context.getTrace().report(NON_FINAL_MEMBER_IN_FINAL_CLASS.on(member, openModifierNode, aClass));
                }
            }
        }    

    private void checkProperty(JetProperty property, PropertyDescriptor propertyDescriptor) {
        DeclarationDescriptor containingDeclaration = propertyDescriptor.getContainingDeclaration();
        ClassDescriptor classDescriptor = (containingDeclaration instanceof ClassDescriptor)
                                          ? (ClassDescriptor) containingDeclaration
                                          : null;
        checkPropertyAbstractness(property, propertyDescriptor, classDescriptor);
        checkPropertyInitializer(property, propertyDescriptor, classDescriptor);
        checkAccessors(property, propertyDescriptor);
        checkDeclaredTypeInPublicMember(property, propertyDescriptor);
    }

    private void checkDeclaredTypeInPublicMember(JetNamedDeclaration member, CallableMemberDescriptor memberDescriptor) {
        PsiElement nameIdentifier = member.getNameIdentifier();
        boolean hasDeferredType;
        if (member instanceof JetProperty) {
            hasDeferredType = ((JetProperty) member).getPropertyTypeRef() == null && DescriptorResolver.hasBody((JetProperty) member);
        }
        else {
            assert member instanceof JetFunction;
            JetFunction function = (JetFunction) member;
            hasDeferredType = function.getReturnTypeRef() == null && function.getBodyExpression() != null && !function.hasBlockBody();
        }
        if ((memberDescriptor.getVisibility() == Visibility.PUBLIC || memberDescriptor.getVisibility() == Visibility.PROTECTED) &&
            hasDeferredType && nameIdentifier != null) {

            JetType returnType = memberDescriptor.getReturnType();
            if (returnType instanceof DeferredType) {
                returnType = ((DeferredType) returnType).getActualType();
            }
            context.getTrace().report(PUBLIC_MEMBER_SHOULD_SPECIFY_TYPE.on(member, nameIdentifier, returnType));
        }
    }

    private void checkPropertyAbstractness(JetProperty property, PropertyDescriptor propertyDescriptor, ClassDescriptor classDescriptor) {
        JetPropertyAccessor getter = property.getGetter();
        JetPropertyAccessor setter = property.getSetter();
        JetModifierList modifierList = property.getModifierList();
        ASTNode abstractNode = modifierList != null ? modifierList.getModifierNode(JetTokens.ABSTRACT_KEYWORD) : null;

        if (abstractNode != null) { //has abstract modifier
            if (classDescriptor == null) {
                context.getTrace().report(ABSTRACT_PROPERTY_NOT_IN_CLASS.on(property, abstractNode));
                return;
            }
            if (!(classDescriptor.getModality() == Modality.ABSTRACT) && classDescriptor.getKind() != ClassKind.ENUM_CLASS) {
                PsiElement classElement = context.getTrace().get(BindingContext.DESCRIPTOR_TO_DECLARATION, classDescriptor);
                assert classElement instanceof JetClass;
                String name = property.getName();
                context.getTrace().report(ABSTRACT_PROPERTY_IN_NON_ABSTRACT_CLASS.on(property, abstractNode, name != null ? name : "", classDescriptor, (JetClass) classElement));
                return;
            }
            if (classDescriptor.getKind() == ClassKind.TRAIT) {
                context.getTrace().report(REDUNDANT_MODIFIER_IN_TRAIT.on(modifierList, abstractNode, JetTokens.ABSTRACT_KEYWORD));
            }
        }

        if (propertyDescriptor.getModality() == Modality.ABSTRACT) {
            JetType returnType = propertyDescriptor.getReturnType();
            if (returnType instanceof DeferredType) {
                returnType = ((DeferredType) returnType).getActualType();
            }

            JetExpression initializer = property.getInitializer();
            if (initializer != null) {
                context.getTrace().report(ABSTRACT_PROPERTY_WITH_INITIALIZER.on(property, initializer, returnType));
            }
            if (getter != null && getter.getBodyExpression() != null) {
                context.getTrace().report(ABSTRACT_PROPERTY_WITH_GETTER.on(property, getter, returnType));
            }
            if (setter != null && setter.getBodyExpression() != null) {
                context.getTrace().report(ABSTRACT_PROPERTY_WITH_SETTER.on(property, setter, returnType));
            }
        }
    }

    private void checkPropertyInitializer(JetProperty property, PropertyDescriptor propertyDescriptor, ClassDescriptor classDescriptor) {
        JetPropertyAccessor getter = property.getGetter();
        JetPropertyAccessor setter = property.getSetter();
        boolean hasAccessorImplementation = (getter != null && getter.getBodyExpression() != null) ||
                                            (setter != null && setter.getBodyExpression() != null);
        if (propertyDescriptor.getModality() == Modality.ABSTRACT) return;

        boolean inTrait = classDescriptor != null && classDescriptor.getKind() == ClassKind.TRAIT;
        JetExpression initializer = property.getInitializer();
        boolean backingFieldRequired = context.getTrace().getBindingContext().get(BindingContext.BACKING_FIELD_REQUIRED, propertyDescriptor);

        PsiElement nameIdentifier = property.getNameIdentifier();
        ASTNode nameNode = nameIdentifier == null ? property.getNode() : nameIdentifier.getNode();

        if (inTrait && backingFieldRequired && hasAccessorImplementation) {
            context.getTrace().report(BACKING_FIELD_IN_TRAIT.on(nameNode));
        }
        if (initializer == null) {
            if (backingFieldRequired && !inTrait && !context.getTrace().getBindingContext().get(BindingContext.IS_INITIALIZED, propertyDescriptor)) {
                if (classDescriptor == null || hasAccessorImplementation) {
                    context.getTrace().report(MUST_BE_INITIALIZED.on(nameNode));
                }
                else {
                    context.getTrace().report(MUST_BE_INITIALIZED_OR_BE_ABSTRACT.on(property, nameNode));
                }
            }
            return;
        }
        if (inTrait) {
            JetType returnType = propertyDescriptor.getReturnType();
            if (returnType instanceof DeferredType) {
                returnType = ((DeferredType) returnType).getActualType();
            }
            context.getTrace().report(PROPERTY_INITIALIZER_IN_TRAIT.on(property, initializer, returnType));
        }
        else if (!backingFieldRequired) {
            context.getTrace().report(PROPERTY_INITIALIZER_NO_BACKING_FIELD.on(initializer));
        }
        else if (classDescriptor != null && classDescriptor.getUnsubstitutedPrimaryConstructor() == null) {
            PsiElement classElement = context.getTrace().get(BindingContext.DESCRIPTOR_TO_DECLARATION, classDescriptor);
            assert classElement instanceof JetClass;

            context.getTrace().report(PROPERTY_INITIALIZER_NO_PRIMARY_CONSTRUCTOR.on(property, initializer, (JetClass) classElement));
        }
    }

    protected void checkFunction(JetNamedFunction function, FunctionDescriptor functionDescriptor) {
        DeclarationDescriptor containingDescriptor = functionDescriptor.getContainingDeclaration();
        PsiElement nameIdentifier = function.getNameIdentifier();
        JetModifierList modifierList = function.getModifierList();
        ASTNode abstractNode = modifierList != null ? modifierList.getModifierNode(JetTokens.ABSTRACT_KEYWORD) : null;
        boolean hasAbstractModifier = abstractNode != null;
        checkDeclaredTypeInPublicMember(function, functionDescriptor);
        if (containingDescriptor instanceof ClassDescriptor) {
            ClassDescriptor classDescriptor = (ClassDescriptor) containingDescriptor;
            boolean inTrait = classDescriptor.getKind() == ClassKind.TRAIT;
            boolean inEnum = classDescriptor.getKind() == ClassKind.ENUM_CLASS;
            boolean inAbstractClass = classDescriptor.getModality() == Modality.ABSTRACT;
            if (hasAbstractModifier && !inAbstractClass && !inTrait && !inEnum) {
                PsiElement classElement = context.getTrace().get(BindingContext.DESCRIPTOR_TO_DECLARATION, classDescriptor);
                assert classElement instanceof JetClass;
                context.getTrace().report(ABSTRACT_FUNCTION_IN_NON_ABSTRACT_CLASS.on(function, abstractNode, functionDescriptor.getName(), classDescriptor, (JetClass) classElement));
            }
            if (hasAbstractModifier && inTrait) {
                context.getTrace().report(REDUNDANT_MODIFIER_IN_TRAIT.on(modifierList, abstractNode, JetTokens.ABSTRACT_KEYWORD));
            }
            if (function.getBodyExpression() != null && hasAbstractModifier) {
                context.getTrace().report(ABSTRACT_FUNCTION_WITH_BODY.on(function, abstractNode, functionDescriptor));
            }
            if (function.getBodyExpression() == null && !hasAbstractModifier && !inTrait && nameIdentifier != null) {
                context.getTrace().report(NON_ABSTRACT_FUNCTION_WITH_NO_BODY.on(function, nameIdentifier, functionDescriptor));
            }
            return;
        }
        if (hasAbstractModifier) {
                context.getTrace().report(NON_MEMBER_ABSTRACT_FUNCTION.on(function, abstractNode, functionDescriptor));
        }
        if (function.getBodyExpression() == null && !hasAbstractModifier && nameIdentifier != null) {
            context.getTrace().report(NON_MEMBER_FUNCTION_NO_BODY.on(function, nameIdentifier, functionDescriptor));
        }
    }

    private void checkAccessors(JetProperty property, PropertyDescriptor propertyDescriptor) {
        for (JetPropertyAccessor accessor : property.getAccessors()) {
            checkIllegalInThisContextModifiers(accessor.getModifierList(), Sets.newHashSet(JetTokens.ABSTRACT_KEYWORD, JetTokens.OPEN_KEYWORD, JetTokens.FINAL_KEYWORD, JetTokens.OVERRIDE_KEYWORD));
        }
        JetPropertyAccessor getter = property.getGetter();
        PropertyGetterDescriptor getterDescriptor = propertyDescriptor.getGetter();
        JetModifierList getterModifierList = getter != null ? getter.getModifierList() : null;
        if (getterModifierList != null && getterDescriptor != null) {
            Map<JetKeywordToken, ASTNode> nodes = getNodesCorrespondingToModifiers(getterModifierList, Sets.newHashSet(JetTokens.PUBLIC_KEYWORD, JetTokens.PROTECTED_KEYWORD, JetTokens.PRIVATE_KEYWORD, JetTokens.INTERNAL_KEYWORD));
            if (getterDescriptor.getVisibility() != propertyDescriptor.getVisibility()) {
                for (Map.Entry<JetKeywordToken, ASTNode> entry : nodes.entrySet()) {
                    context.getTrace().report(Errors.GETTER_VISIBILITY_DIFFERS_FROM_PROPERTY_VISIBILITY.on(getterModifierList, entry.getValue(), entry.getKey()));
                }
            }
            else {
                for (Map.Entry<JetKeywordToken, ASTNode> entry : nodes.entrySet()) {
                    context.getTrace().report(Errors.REDUNDANT_MODIFIER_IN_GETTER.on(getterModifierList, entry.getValue(), entry.getKey()));
                }
            }
        }
    }

    private void checkModifiers(@Nullable JetModifierList modifierList) {
        checkModalityModifiers(modifierList);
        checkVisibilityModifiers(modifierList);
    }

    private void checkModalityModifiers(@Nullable JetModifierList modifierList) {
        if (modifierList == null) return;
        checkRedundantModifier(modifierList, Pair.create(JetTokens.OPEN_KEYWORD, JetTokens.ABSTRACT_KEYWORD), Pair.create(JetTokens.OPEN_KEYWORD, JetTokens.OVERRIDE_KEYWORD));

        checkCompatibility(modifierList, Lists.newArrayList(JetTokens.ABSTRACT_KEYWORD, JetTokens.OPEN_KEYWORD, JetTokens.FINAL_KEYWORD),
                           Lists.<JetToken>newArrayList(JetTokens.ABSTRACT_KEYWORD, JetTokens.OPEN_KEYWORD));
    }

    private void checkVisibilityModifiers(@Nullable JetModifierList modifierList) {
        if (modifierList == null) return;

        checkCompatibility(modifierList, Lists.newArrayList(JetTokens.PRIVATE_KEYWORD, JetTokens.PROTECTED_KEYWORD, JetTokens.PUBLIC_KEYWORD, JetTokens.INTERNAL_KEYWORD),
                           Lists.<JetToken>newArrayList(JetTokens.PROTECTED_KEYWORD, JetTokens.INTERNAL_KEYWORD));
    }

    private void checkCompatibility(@Nullable JetModifierList modifierList, Collection<JetKeywordToken> availableModifiers, Collection<JetToken>... availableCombinations) {
        if (modifierList == null) return;
        Collection<JetKeywordToken> presentModifiers = Sets.newLinkedHashSet();
        for (JetKeywordToken modifier : availableModifiers) {
            if (modifierList.hasModifier(modifier)) {
                presentModifiers.add(modifier);
            }
        }
        if (presentModifiers.size() == 1) {
            return;
        }
        for (Collection<JetToken> combination : availableCombinations) {
            if (presentModifiers.containsAll(combination) && combination.containsAll(presentModifiers)) {
                return;
            }
        }
        for (JetKeywordToken token : presentModifiers) {
            context.getTrace().report(Errors.INCOMPATIBLE_MODIFIERS.on(modifierList.getModifierNode(token), presentModifiers));
        }
    }

    private void checkRedundantModifier(@NotNull JetModifierList modifierList, Pair<JetKeywordToken, JetKeywordToken>... redundantBundles) {
        for (Pair<JetKeywordToken, JetKeywordToken> tokenPair : redundantBundles) {
            JetKeywordToken redundantModifier = tokenPair.getFirst();
            JetKeywordToken sufficientModifier = tokenPair.getSecond();
            if (modifierList.hasModifier(redundantModifier) && modifierList.hasModifier(sufficientModifier)) {
                context.getTrace().report(Errors.REDUNDANT_MODIFIER.on(modifierList, modifierList.getModifierNode(redundantModifier), redundantModifier, sufficientModifier));
            }
        }
    }

    private void checkIllegalInThisContextModifiers(@Nullable JetModifierList modifierList, Collection<JetKeywordToken> illegalModifiers) {
        if (modifierList == null) return;
        for (JetKeywordToken modifier : illegalModifiers) {
            if (modifierList.hasModifier(modifier)) {
                context.getTrace().report(Errors.ILLEGAL_MODIFIER.on(modifierList, modifierList.getModifierNode(modifier), modifier));
            }
        }
    }
    
    @NotNull
    public static Map<JetKeywordToken, ASTNode> getNodesCorrespondingToModifiers(@NotNull JetModifierList modifierList, Collection<JetKeywordToken> possibleModifiers) {
        Map<JetKeywordToken, ASTNode> nodes = Maps.newHashMap();
        for (JetKeywordToken modifier : possibleModifiers) {
            if (modifierList.hasModifier(modifier)) {
                nodes.put(modifier, modifierList.getModifierNode(modifier));
            }
        }
        return nodes;
    }
}
