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

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.intellij.lang.ASTNode;
import com.intellij.openapi.util.Pair;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory1;
import org.jetbrains.kotlin.diagnostics.Errors;
import org.jetbrains.kotlin.lexer.JetKeywordToken;
import org.jetbrains.kotlin.lexer.JetModifierKeywordToken;
import org.jetbrains.kotlin.lexer.JetTokens;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.resolve.constants.ConstantValue;
import org.jetbrains.kotlin.resolve.constants.StringValue;

import java.util.*;

import static org.jetbrains.kotlin.diagnostics.Errors.*;
import static org.jetbrains.kotlin.lexer.JetTokens.*;
import static org.jetbrains.kotlin.psi.JetStubbedPsiUtil.getContainingDeclaration;
import static org.jetbrains.kotlin.resolve.DescriptorUtils.isCompanionObject;
import static org.jetbrains.kotlin.resolve.DescriptorUtils.isEnumEntry;

public class ModifiersChecker {
    private static final Collection<JetModifierKeywordToken> MODALITY_MODIFIERS =
            Lists.newArrayList(ABSTRACT_KEYWORD, OPEN_KEYWORD, FINAL_KEYWORD, OVERRIDE_KEYWORD, SEALED_KEYWORD);

    private static final Collection<JetModifierKeywordToken> VISIBILITY_MODIFIERS =
            Lists.newArrayList(PRIVATE_KEYWORD, PROTECTED_KEYWORD, PUBLIC_KEYWORD, INTERNAL_KEYWORD);

    private static final Set<JetModifierKeywordToken> MODIFIERS_ILLEGAL_ON_PARAMETERS;

    static {
        MODIFIERS_ILLEGAL_ON_PARAMETERS = Sets.newHashSet();
        MODIFIERS_ILLEGAL_ON_PARAMETERS.addAll(Arrays.asList(JetTokens.MODIFIER_KEYWORDS_ARRAY));
        MODIFIERS_ILLEGAL_ON_PARAMETERS.remove(JetTokens.VARARG_KEYWORD);
    }

    public static boolean isIllegalInner(@NotNull DeclarationDescriptor descriptor) {
        return checkIllegalInner(descriptor) != InnerModifierCheckResult.ALLOWED;
    }

    private enum InnerModifierCheckResult {
        ALLOWED,
        ILLEGAL_POSITION,
        IN_TRAIT,
        IN_OBJECT,
    }


    // NOTE: just checks if this is legal context for companion modifier (Companion object descriptor can be created)
    // COMPANION_OBJECT_NOT_ALLOWED can be reported later
    public static boolean isCompanionModifierAllowed(@NotNull JetDeclaration declaration) {
        if (declaration instanceof JetObjectDeclaration) {
            JetDeclaration containingDeclaration = getContainingDeclaration(declaration);
            if (containingDeclaration instanceof JetClassOrObject) {
                return true;
            }
        }
        return false;
    }

    @NotNull
    private static InnerModifierCheckResult checkIllegalInner(@NotNull DeclarationDescriptor descriptor) {
        if (!(descriptor instanceof ClassDescriptor)) return InnerModifierCheckResult.ILLEGAL_POSITION;
        ClassDescriptor classDescriptor = (ClassDescriptor) descriptor;

        if (classDescriptor.getKind() != ClassKind.CLASS) return InnerModifierCheckResult.ILLEGAL_POSITION;

        DeclarationDescriptor containingDeclaration = classDescriptor.getContainingDeclaration();
        if (!(containingDeclaration instanceof ClassDescriptor)) return InnerModifierCheckResult.ILLEGAL_POSITION;

        if (DescriptorUtils.isTrait(containingDeclaration)) {
            return InnerModifierCheckResult.IN_TRAIT;
        }
        else if (DescriptorUtils.isObject(containingDeclaration)) {
            return InnerModifierCheckResult.IN_OBJECT;
        }
        else {
            return InnerModifierCheckResult.ALLOWED;
        }
    }

    private static boolean isIllegalNestedClass(@NotNull DeclarationDescriptor descriptor) {
        if (!(descriptor instanceof ClassDescriptor)) return false;
        DeclarationDescriptor containingDeclaration = descriptor.getContainingDeclaration();
        if (!(containingDeclaration instanceof ClassDescriptor)) return false;
        ClassDescriptor containingClass = (ClassDescriptor) containingDeclaration;
        return containingClass.isInner() || containingClass.getContainingDeclaration() instanceof FunctionDescriptor;
    }

    @NotNull
    public static Modality resolveModalityFromModifiers(
            @NotNull JetModifierListOwner modifierListOwner,
            @NotNull Modality defaultModality
    ) {
        return resolveModalityFromModifiers(modifierListOwner.getModifierList(), defaultModality);
    }

    @NotNull
    public static Modality resolveModalityFromModifiers(@Nullable JetModifierList modifierList, @NotNull Modality defaultModality) {
        if (modifierList == null) return defaultModality;
        boolean hasAbstractModifier = modifierList.hasModifier(ABSTRACT_KEYWORD);
        boolean hasOverrideModifier = modifierList.hasModifier(OVERRIDE_KEYWORD);

        if (modifierList.hasModifier(SEALED_KEYWORD)) {
            return Modality.SEALED;
        }
        if (modifierList.hasModifier(OPEN_KEYWORD)) {
            if (hasAbstractModifier || defaultModality == Modality.ABSTRACT) {
                return Modality.ABSTRACT;
            }
            return Modality.OPEN;
        }
        if (hasAbstractModifier) {
            return Modality.ABSTRACT;
        }
        boolean hasFinalModifier = modifierList.hasModifier(FINAL_KEYWORD);
        if (hasOverrideModifier && !hasFinalModifier && !(defaultModality == Modality.ABSTRACT)) {
            return Modality.OPEN;
        }
        if (hasFinalModifier) {
            return Modality.FINAL;
        }
        return defaultModality;
    }

    @NotNull
    public static Visibility resolveVisibilityFromModifiers(
            @NotNull JetModifierListOwner modifierListOwner,
            @NotNull Visibility defaultVisibility
    ) {
        return resolveVisibilityFromModifiers(modifierListOwner.getModifierList(), defaultVisibility);
    }

    public static Visibility resolveVisibilityFromModifiers(@Nullable JetModifierList modifierList, @NotNull Visibility defaultVisibility) {
        if (modifierList == null) return defaultVisibility;
        if (modifierList.hasModifier(PRIVATE_KEYWORD)) return Visibilities.PRIVATE;
        if (modifierList.hasModifier(PUBLIC_KEYWORD)) return Visibilities.PUBLIC;
        if (modifierList.hasModifier(PROTECTED_KEYWORD)) return Visibilities.PROTECTED;
        if (modifierList.hasModifier(INTERNAL_KEYWORD)) return Visibilities.INTERNAL;
        return defaultVisibility;
    }

    public static boolean isInnerClass(@Nullable JetModifierList modifierList) {
        return modifierList != null && modifierList.hasModifier(INNER_KEYWORD);
    }

    public class ModifiersCheckingProcedure {

        @NotNull
        private final BindingTrace trace;

        private ModifiersCheckingProcedure(@NotNull BindingTrace trace) {
            this.trace = trace;
        }

        public void checkParameterHasNoValOrVar(
                @NotNull JetParameter parameter,
                @NotNull DiagnosticFactory1<PsiElement, JetKeywordToken> diagnosticFactory
        ) {
            PsiElement valOrVar = parameter.getValOrVarKeyword();
            if (valOrVar != null) {
                trace.report(diagnosticFactory.on(valOrVar, ((JetKeywordToken) valOrVar.getNode().getElementType())));
            }
        }

        public void reportIllegalModifiers(
                @Nullable JetModifierList modifierList,
                @NotNull Collection<JetModifierKeywordToken> illegalModifiers
        ) {
            if (modifierList == null) return;

            for (JetModifierKeywordToken modifierToken : illegalModifiers) {
                if (modifierList.hasModifier(modifierToken)) {
                    PsiElement modifierPsi = modifierList.getModifier(modifierToken);
                    assert modifierPsi != null;
                    trace.report(ILLEGAL_MODIFIER.on(modifierPsi, modifierToken));
                }
            }
        }

        public void reportIllegalModifiers(
                @NotNull JetModifierListOwner modifierListOwner,
                @NotNull Collection<JetModifierKeywordToken> illegalModifiers
        ) {
            reportIllegalModifiers(modifierListOwner.getModifierList(), illegalModifiers);
        }

        public void checkIncompatibleModifiers(
                @Nullable JetModifierList modifierList,
                @NotNull Collection<JetModifierKeywordToken> availableModifiers,
                @NotNull Collection<JetModifierKeywordToken>... availableCombinations
        ) {
            if (modifierList == null) return;
            Collection<JetModifierKeywordToken> presentModifiers = Sets.newLinkedHashSet();
            for (JetModifierKeywordToken modifier : availableModifiers) {
                if (modifierList.hasModifier(modifier)) {
                    presentModifiers.add(modifier);
                }
            }
            checkRepeatedModifiers(modifierList, availableModifiers);

            if (presentModifiers.size() == 1) {
                return;
            }
            for (Collection<JetModifierKeywordToken> combination : availableCombinations) {
                if (presentModifiers.containsAll(combination) && combination.containsAll(presentModifiers)) {
                    return;
                }
            }
            for (JetModifierKeywordToken token : presentModifiers) {
                trace.report(Errors.INCOMPATIBLE_MODIFIERS.on(modifierList.getModifierNode(token).getPsi(), presentModifiers));
            }
        }

        private void checkRepeatedModifiers(
                @NotNull JetModifierList modifierList,
                @NotNull Collection<JetModifierKeywordToken> availableModifiers
        ) {
            for (JetModifierKeywordToken token : availableModifiers) {
                if (!modifierList.hasModifier(token)) continue;

                List<ASTNode> nodesOfRepeatedTokens = Lists.newArrayList();
                ASTNode node = modifierList.getNode().getFirstChildNode();
                while (node != null) {
                    if (node.getElementType() == token) {
                        nodesOfRepeatedTokens.add(node);
                    }
                    node = node.getTreeNext();
                }
                if (nodesOfRepeatedTokens.size() > 1) {
                    for (ASTNode repeatedToken : nodesOfRepeatedTokens) {
                        trace.report(REPEATED_MODIFIER.on(repeatedToken.getPsi(), token));
                    }
                }
            }
        }

        public void checkIncompatibleVarianceModifiers(@Nullable JetModifierList modifierList) {
            checkIncompatibleModifiers(modifierList, Arrays.asList(JetTokens.IN_KEYWORD, JetTokens.OUT_KEYWORD));
        }


        public void checkModifiersForDeclaration(@NotNull JetDeclaration modifierListOwner, @NotNull MemberDescriptor descriptor) {
            if (modifierListOwner instanceof JetEnumEntry) {
                reportIllegalModifiers(modifierListOwner.getModifierList(), Arrays.asList(MODIFIER_KEYWORDS_ARRAY));
            }
            else {
                checkInnerModifier(modifierListOwner, descriptor);
                checkCompanionModifier(modifierListOwner);
                checkModalityModifiers(modifierListOwner);
                checkVisibilityModifiers(modifierListOwner, descriptor);
                checkVarianceModifiers(modifierListOwner);
                checkVarargsModifiers(modifierListOwner, descriptor);
            }
            checkPlatformNameApplicability(descriptor);
            runDeclarationCheckers(modifierListOwner, descriptor);
            AnnotationChecker.INSTANCE$.check(modifierListOwner, trace,
                                                    descriptor instanceof ClassDescriptor ? (ClassDescriptor) descriptor : null);
        }

        private void checkVarargsModifiers(@NotNull JetDeclaration owner, @NotNull MemberDescriptor descriptor) {
            if (!(owner instanceof JetParameter)) {
                reportIllegalModifiers(owner.getModifierList(), Collections.singleton(VARARG_KEYWORD));
            }
        }

        public void checkModifiersForLocalDeclaration(
                @NotNull JetDeclaration modifierListOwner,
                @NotNull DeclarationDescriptor descriptor
        ) {
            reportIllegalModalityModifiers(modifierListOwner);
            reportIllegalVisibilityModifiers(modifierListOwner);
            checkPlatformNameApplicability(descriptor);
            runDeclarationCheckers(modifierListOwner, descriptor);
            AnnotationChecker.INSTANCE$.check(modifierListOwner, trace,
                                                    descriptor instanceof ClassDescriptor ? (ClassDescriptor) descriptor : null);
        }

        public void reportIllegalModalityModifiers(@NotNull JetModifierListOwner modifierListOwner) {
            reportIllegalModifiers(modifierListOwner, MODALITY_MODIFIERS);
        }

        public void reportIllegalVisibilityModifiers(@NotNull JetModifierListOwner modifierListOwner) {
            reportIllegalModifiers(modifierListOwner, VISIBILITY_MODIFIERS);
        }

        private void checkModalityModifiers(@NotNull JetModifierListOwner modifierListOwner) {
            JetModifierList modifierList = modifierListOwner.getModifierList();
            if (modifierList == null) return;

            checkRedundantModifier(modifierList, Pair.create(OPEN_KEYWORD, ABSTRACT_KEYWORD), Pair.create(OPEN_KEYWORD, OVERRIDE_KEYWORD));

            checkCompatibility(modifierList, Arrays.asList(ABSTRACT_KEYWORD, OPEN_KEYWORD, FINAL_KEYWORD),
                               Arrays.asList(ABSTRACT_KEYWORD, OPEN_KEYWORD));

            if (modifierListOwner instanceof JetObjectDeclaration) {
                reportIllegalModalityModifiers(modifierListOwner);
            }
            else if (modifierListOwner instanceof JetClassOrObject) {
                reportIllegalModifiers(modifierListOwner, Collections.singletonList(OVERRIDE_KEYWORD));
            }
        }

        private void checkVisibilityModifiers(@NotNull JetModifierListOwner modifierListOwner, @NotNull DeclarationDescriptor descriptor) {
            JetModifierList modifierList = modifierListOwner.getModifierList();
            if (modifierList == null) return;

            DeclarationDescriptor containingDeclaration = descriptor.getContainingDeclaration();
            if (containingDeclaration instanceof PackageFragmentDescriptor) {
                if (modifierList.hasModifier(PROTECTED_KEYWORD)) {
                    trace.report(Errors.PACKAGE_MEMBER_CANNOT_BE_PROTECTED.on(modifierListOwner));
                }
            }

            checkCompatibility(modifierList, VISIBILITY_MODIFIERS);
        }

        private void checkInnerModifier(@NotNull JetModifierListOwner modifierListOwner, @NotNull DeclarationDescriptor descriptor) {
            if (modifierListOwner.hasModifier(INNER_KEYWORD)) {
                switch (checkIllegalInner(descriptor)) {
                    case ALLOWED:
                        break;
                    case ILLEGAL_POSITION:
                        reportIllegalModifiers(modifierListOwner, Collections.singletonList(INNER_KEYWORD));
                        break;
                    case IN_TRAIT:
                        trace.report(INNER_CLASS_IN_TRAIT.on(modifierListOwner));
                        break;
                    case IN_OBJECT:
                        trace.report(INNER_CLASS_IN_OBJECT.on(modifierListOwner));
                        break;
                }
                return;
            }
            if (modifierListOwner instanceof JetClass && !(modifierListOwner instanceof JetEnumEntry)) {
                JetClass aClass = (JetClass) modifierListOwner;
                boolean localEnumError = aClass.isLocal() && aClass.isEnum();
                if (!localEnumError && isIllegalNestedClass(descriptor)) {
                    trace.report(NESTED_CLASS_NOT_ALLOWED.on(aClass));
                }
            }
        }

        private void checkCompanionModifier(@NotNull JetDeclaration declaration) {
            if (declaration.hasModifier(COMPANION_KEYWORD) && !isCompanionModifierAllowed(declaration)) {
                reportIllegalModifiers(declaration, Collections.singletonList(COMPANION_KEYWORD));
            }
        }

        private void checkPlatformNameApplicability(@NotNull DeclarationDescriptor descriptor) {
            AnnotationDescriptor annotation = descriptor.getAnnotations().findAnnotation(new FqName("kotlin.platform.platformName"));
            if (annotation == null) return;

            JetAnnotationEntry annotationEntry = trace.get(BindingContext.ANNOTATION_DESCRIPTOR_TO_PSI_ELEMENT, annotation);
            if (annotationEntry == null) return;

            if (descriptor instanceof FunctionDescriptor && !isRenamableFunction((FunctionDescriptor) descriptor)) {
                trace.report(INAPPLICABLE_PLATFORM_NAME.on(annotationEntry));
            }

            String value = null;
            Collection<ConstantValue<?>> values = annotation.getAllValueArguments().values();
            if (!values.isEmpty()) {
                ConstantValue<?> name = values.iterator().next();
                if (name instanceof StringValue) {
                    value = ((StringValue) name).getValue();
                }
            }
            if (value == null || !Name.isValidIdentifier(value)) {
                trace.report(ILLEGAL_PLATFORM_NAME.on(annotationEntry, String.valueOf(value)));
            }

            if (descriptor instanceof CallableMemberDescriptor) {
                CallableMemberDescriptor callableMemberDescriptor = (CallableMemberDescriptor) descriptor;
                if (DescriptorUtils.isOverride(callableMemberDescriptor) || callableMemberDescriptor.getModality().isOverridable()) {
                    trace.report(INAPPLICABLE_PLATFORM_NAME.on(annotationEntry));
                }
            }
        }


        private boolean isRenamableFunction(@NotNull FunctionDescriptor descriptor) {
            DeclarationDescriptor containingDescriptor = descriptor.getContainingDeclaration();

            return containingDescriptor instanceof PackageFragmentDescriptor || containingDescriptor instanceof ClassDescriptor;
        }

        private void checkCompatibility(
                @Nullable JetModifierList modifierList,
                Collection<JetModifierKeywordToken> availableModifiers,
                Collection<JetModifierKeywordToken>... availableCombinations
        ) {
            checkIncompatibleModifiers(modifierList, availableModifiers, availableCombinations);
        }

        private void checkRedundantModifier(
                @NotNull JetModifierList modifierList,
                Pair<JetModifierKeywordToken, JetModifierKeywordToken>... redundantBundles
        ) {
            for (Pair<JetModifierKeywordToken, JetModifierKeywordToken> tokenPair : redundantBundles) {
                JetModifierKeywordToken redundantModifier = tokenPair.getFirst();
                JetModifierKeywordToken sufficientModifier = tokenPair.getSecond();
                if (modifierList.hasModifier(redundantModifier) && modifierList.hasModifier(sufficientModifier)) {
                    trace.report(Errors.REDUNDANT_MODIFIER.on(modifierList.getModifierNode(redundantModifier).getPsi(), redundantModifier,
                                                              sufficientModifier));
                }
            }
        }

        @NotNull
        public Map<JetModifierKeywordToken, ASTNode> getNodesCorrespondingToModifiers(
                @NotNull JetModifierList modifierList,
                @NotNull Collection<JetModifierKeywordToken> possibleModifiers
        ) {
            Map<JetModifierKeywordToken, ASTNode> nodes = Maps.newHashMap();
            for (JetModifierKeywordToken modifier : possibleModifiers) {
                if (modifierList.hasModifier(modifier)) {
                    nodes.put(modifier, modifierList.getModifierNode(modifier));
                }
            }
            return nodes;
        }


        private void runDeclarationCheckers(@NotNull JetDeclaration declaration, @NotNull DeclarationDescriptor descriptor) {
            for (DeclarationChecker checker : declarationCheckers) {
                checker.check(declaration, descriptor, trace, trace.getBindingContext());
            }
        }

        public void checkVarianceModifiers(@NotNull JetModifierListOwner modifierListOwner) {
            reportIllegalModifiers(modifierListOwner.getModifierList(), Arrays.asList(IN_KEYWORD, OUT_KEYWORD, REIFIED_KEYWORD));
            if (!(modifierListOwner instanceof JetTypeParameterListOwner)) return;
            List<JetTypeParameter> typeParameters = ((JetTypeParameterListOwner) modifierListOwner).getTypeParameters();
            for (JetTypeParameter typeParameter : typeParameters) {
                JetModifierList modifierList = typeParameter.getModifierList();
                checkIncompatibleVarianceModifiers(modifierList);
            }
        }

        public void checkParameterHasNoModifier(
                @NotNull JetParameter parameter
        ) {
            reportIllegalModifiers(parameter.getModifierList(), MODIFIERS_ILLEGAL_ON_PARAMETERS);
        }
    }

    @NotNull
    private final Iterable<? extends DeclarationChecker> declarationCheckers;

    public ModifiersChecker(@NotNull Iterable<? extends DeclarationChecker> declarationCheckers) {
        this.declarationCheckers = declarationCheckers;
    }

    @NotNull
    public ModifiersCheckingProcedure withTrace(@NotNull BindingTrace trace) {
        return new ModifiersCheckingProcedure(trace);
    }
}
