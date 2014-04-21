/*
 * Copyright 2010-2014 JetBrains s.r.o.
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
import org.jetbrains.jet.lang.psi.JetClass;
import org.jetbrains.jet.lang.psi.JetEnumEntry;
import org.jetbrains.jet.lang.psi.JetModifierList;
import org.jetbrains.jet.lang.psi.JetModifierListOwner;
import org.jetbrains.jet.lexer.JetModifierKeywordToken;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import static org.jetbrains.jet.lang.diagnostics.Errors.ILLEGAL_MODIFIER;
import static org.jetbrains.jet.lexer.JetTokens.*;

public class ModifiersChecker {
    private static final Collection<JetModifierKeywordToken> MODALITY_MODIFIERS =
            Lists.newArrayList(ABSTRACT_KEYWORD, OPEN_KEYWORD, FINAL_KEYWORD, OVERRIDE_KEYWORD);

    private static final Collection<JetModifierKeywordToken> VISIBILITY_MODIFIERS =
            Lists.newArrayList(PRIVATE_KEYWORD, PROTECTED_KEYWORD, PUBLIC_KEYWORD, INTERNAL_KEYWORD);

    public static void reportIllegalModifiers(
            @Nullable JetModifierList modifierList,
            @NotNull Collection<JetModifierKeywordToken> illegalModifiers,
            @NotNull BindingTrace trace
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

    @NotNull
    private final BindingTrace trace;

    public ModifiersChecker(@NotNull BindingTrace trace) {
        this.trace = trace;
    }

    public static ModifiersChecker create(@NotNull BindingTrace trace) {
        return new ModifiersChecker(trace);
    }

    public void checkModifiersForDeclaration(@NotNull JetModifierListOwner modifierListOwner, @NotNull DeclarationDescriptor descriptor) {
        JetModifierList modifierList = modifierListOwner.getModifierList();
        checkModalityModifiers(modifierList);
        checkVisibilityModifiers(modifierList, descriptor);
        checkInnerModifier(modifierListOwner, descriptor);
    }

    public void checkModifiersForLocalDeclaration(@NotNull JetModifierListOwner modifierListOwner) {
        checkIllegalModalityModifiers(modifierListOwner);
        checkIllegalVisibilityModifiers(modifierListOwner);
    }

    public void checkIllegalModalityModifiers(@NotNull JetModifierListOwner modifierListOwner) {
        checkIllegalInThisContextModifiers(modifierListOwner.getModifierList(), MODALITY_MODIFIERS);
    }

    public void checkIllegalVisibilityModifiers(@NotNull JetModifierListOwner modifierListOwner) {
        checkIllegalInThisContextModifiers(modifierListOwner.getModifierList(), VISIBILITY_MODIFIERS);
    }

    private void checkModalityModifiers(@Nullable JetModifierList modifierList) {
        if (modifierList == null) return;
        checkRedundantModifier(modifierList, Pair.create(OPEN_KEYWORD, ABSTRACT_KEYWORD), Pair.create(OPEN_KEYWORD, OVERRIDE_KEYWORD));

        checkCompatibility(modifierList, Lists.newArrayList(ABSTRACT_KEYWORD, OPEN_KEYWORD, FINAL_KEYWORD),
                           Lists.newArrayList(ABSTRACT_KEYWORD, OPEN_KEYWORD));
    }

    private void checkVisibilityModifiers(@Nullable JetModifierList modifierList, @NotNull DeclarationDescriptor descriptor) {
        if (modifierList == null) return;

        DeclarationDescriptor containingDeclaration = descriptor.getContainingDeclaration();
        if (containingDeclaration instanceof PackageFragmentDescriptor) {
            if (modifierList.hasModifier(PROTECTED_KEYWORD)) {
                trace.report(Errors.PACKAGE_MEMBER_CANNOT_BE_PROTECTED.on(modifierList.getModifierNode(PROTECTED_KEYWORD).getPsi()));
            }
        }

        checkCompatibility(modifierList, VISIBILITY_MODIFIERS);
    }

    private void checkInnerModifier(@NotNull JetModifierListOwner modifierListOwner, @NotNull DeclarationDescriptor descriptor) {
        JetModifierList modifierList = modifierListOwner.getModifierList();

        if (modifierList != null && modifierList.hasModifier(INNER_KEYWORD)) {
            if (isIllegalInner(descriptor)) {
                checkIllegalInThisContextModifiers(modifierList, Collections.singletonList(INNER_KEYWORD));
            }
        }
        else {
            if (modifierListOwner instanceof JetClass && !(modifierListOwner instanceof JetEnumEntry) && isIllegalNestedClass(descriptor)) {
                PsiElement name = ((JetClass) modifierListOwner).getNameIdentifier();
                if (name != null) {
                    trace.report(Errors.NESTED_CLASS_NOT_ALLOWED.on(name));
                }
            }
        }
    }

    private static boolean isIllegalInner(@NotNull DeclarationDescriptor descriptor) {
        if (!(descriptor instanceof ClassDescriptor)) return true;
        ClassDescriptor classDescriptor = (ClassDescriptor) descriptor;
        if (classDescriptor.getKind() != ClassKind.CLASS) return true;
        DeclarationDescriptor containingDeclaration = classDescriptor.getContainingDeclaration();
        if (!(containingDeclaration instanceof ClassDescriptor)) return true;
        return ((ClassDescriptor) containingDeclaration).getKind() == ClassKind.TRAIT;
    }

    private static boolean isIllegalNestedClass(@NotNull DeclarationDescriptor descriptor) {
        if (!(descriptor instanceof ClassDescriptor)) return false;
        DeclarationDescriptor containingDeclaration = descriptor.getContainingDeclaration();
        if (!(containingDeclaration instanceof ClassDescriptor)) return false;
        ClassDescriptor containingClass = (ClassDescriptor) containingDeclaration;
        return containingClass.isInner() || containingClass.getContainingDeclaration() instanceof FunctionDescriptor;
    }

    private void checkCompatibility(@Nullable JetModifierList modifierList, Collection<JetModifierKeywordToken> availableModifiers, Collection<JetModifierKeywordToken>... availableCombinations) {
        if (modifierList == null) return;
        Collection<JetModifierKeywordToken> presentModifiers = Sets.newLinkedHashSet();
        for (JetModifierKeywordToken modifier : availableModifiers) {
            if (modifierList.hasModifier(modifier)) {
                presentModifiers.add(modifier);
            }
        }
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

    private void checkRedundantModifier(@NotNull JetModifierList modifierList, Pair<JetModifierKeywordToken, JetModifierKeywordToken>... redundantBundles) {
        for (Pair<JetModifierKeywordToken, JetModifierKeywordToken> tokenPair : redundantBundles) {
            JetModifierKeywordToken redundantModifier = tokenPair.getFirst();
            JetModifierKeywordToken sufficientModifier = tokenPair.getSecond();
            if (modifierList.hasModifier(redundantModifier) && modifierList.hasModifier(sufficientModifier)) {
                trace.report(Errors.REDUNDANT_MODIFIER.on(modifierList.getModifierNode(redundantModifier).getPsi(), redundantModifier, sufficientModifier));
            }
        }
    }

    public void checkIllegalInThisContextModifiers(
            @Nullable JetModifierList modifierList,
            @NotNull Collection<JetModifierKeywordToken> illegalModifiers
    ) {
        reportIllegalModifiers(modifierList, illegalModifiers, trace);
    }

    @NotNull
    public static Map<JetModifierKeywordToken, ASTNode> getNodesCorrespondingToModifiers(@NotNull JetModifierList modifierList, @NotNull Collection<JetModifierKeywordToken> possibleModifiers) {
        Map<JetModifierKeywordToken, ASTNode> nodes = Maps.newHashMap();
        for (JetModifierKeywordToken modifier : possibleModifiers) {
            if (modifierList.hasModifier(modifier)) {
                nodes.put(modifier, modifierList.getModifierNode(modifier));
            }
        }
        return nodes;
    }

    @NotNull
    public static Modality resolveModalityFromModifiers(@NotNull JetModifierListOwner modifierListOwner, @NotNull Modality defaultModality) {
        return resolveModalityFromModifiers(modifierListOwner.getModifierList(), defaultModality);
    }

    public static Modality resolveModalityFromModifiers(@Nullable JetModifierList modifierList, @NotNull Modality defaultModality) {
        if (modifierList == null) return defaultModality;
        boolean hasAbstractModifier = modifierList.hasModifier(ABSTRACT_KEYWORD);
        boolean hasOverrideModifier = modifierList.hasModifier(OVERRIDE_KEYWORD);

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
    public static Visibility resolveVisibilityFromModifiers(@NotNull JetModifierListOwner modifierListOwner, @NotNull Visibility defaultVisibility) {
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

    @NotNull
    public static Visibility getDefaultClassVisibility(@NotNull ClassDescriptor descriptor) {
        ClassKind kind = descriptor.getKind();
        if (kind == ClassKind.ENUM_ENTRY) {
            return Visibilities.PUBLIC;
        }
        if (kind == ClassKind.CLASS_OBJECT) {
            return ((ClassDescriptor) descriptor.getContainingDeclaration()).getVisibility();
        }
        return Visibilities.INTERNAL;
    }
}
