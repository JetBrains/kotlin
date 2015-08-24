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
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory1;
import org.jetbrains.kotlin.lexer.JetKeywordToken;
import org.jetbrains.kotlin.lexer.JetModifierKeywordToken;
import org.jetbrains.kotlin.lexer.JetTokens;
import org.jetbrains.kotlin.psi.*;

import java.util.*;

import static org.jetbrains.kotlin.diagnostics.Errors.NESTED_CLASS_NOT_ALLOWED;
import static org.jetbrains.kotlin.lexer.JetTokens.*;
import static org.jetbrains.kotlin.psi.JetStubbedPsiUtil.getContainingDeclaration;

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

        public void checkModifiersForDeclaration(@NotNull JetDeclaration modifierListOwner, @NotNull MemberDescriptor descriptor) {
            checkNestedClassAllowed(modifierListOwner, descriptor);
            ModifierCheckerCore.INSTANCE$.check(modifierListOwner, trace, descriptor);
            checkTypeParametersModifiers(modifierListOwner);
            runDeclarationCheckers(modifierListOwner, descriptor);
            ClassDescriptor classDescriptor = descriptor instanceof ClassDescriptor ? (ClassDescriptor) descriptor : null;
            annotationChecker.check(modifierListOwner, trace, classDescriptor);
        }

        public void checkModifiersForLocalDeclaration(
                @NotNull JetDeclaration modifierListOwner,
                @NotNull DeclarationDescriptor descriptor
        ) {
            runDeclarationCheckers(modifierListOwner, descriptor);
            annotationChecker.check(modifierListOwner, trace,
                                              descriptor instanceof ClassDescriptor ? (ClassDescriptor) descriptor : null);
            ModifierCheckerCore.INSTANCE$.check(modifierListOwner, trace, descriptor);
        }

        private void checkNestedClassAllowed(@NotNull JetModifierListOwner modifierListOwner, @NotNull DeclarationDescriptor descriptor) {
            if (modifierListOwner.hasModifier(INNER_KEYWORD)) return;
            if (modifierListOwner instanceof JetClass && !(modifierListOwner instanceof JetEnumEntry)) {
                JetClass aClass = (JetClass) modifierListOwner;
                boolean localEnumError = aClass.isLocal() && aClass.isEnum();
                if (!localEnumError && isIllegalNestedClass(descriptor)) {
                    trace.report(NESTED_CLASS_NOT_ALLOWED.on(aClass));
                }
            }
        }

        @NotNull
        public Map<JetModifierKeywordToken, PsiElement> getTokensCorrespondingToModifiers(
                @NotNull JetModifierList modifierList,
                @NotNull Collection<JetModifierKeywordToken> possibleModifiers
        ) {
            Map<JetModifierKeywordToken, PsiElement> tokens = Maps.newHashMap();
            for (JetModifierKeywordToken modifier : possibleModifiers) {
                if (modifierList.hasModifier(modifier)) {
                    tokens.put(modifier, modifierList.getModifier(modifier));
                }
            }
            return tokens;
        }


        private void runDeclarationCheckers(@NotNull JetDeclaration declaration, @NotNull DeclarationDescriptor descriptor) {
            for (DeclarationChecker checker : declarationCheckers) {
                checker.check(declaration, descriptor, trace, trace.getBindingContext());
            }
        }

        public void checkTypeParametersModifiers(@NotNull JetModifierListOwner modifierListOwner) {
            if (!(modifierListOwner instanceof JetTypeParameterListOwner)) return;
            List<JetTypeParameter> typeParameters = ((JetTypeParameterListOwner) modifierListOwner).getTypeParameters();
            for (JetTypeParameter typeParameter : typeParameters) {
                ModifierCheckerCore.INSTANCE$.check(typeParameter, trace, null);
            }
        }
    }

    @NotNull
    private final AnnotationChecker annotationChecker;

    @NotNull
    private final Iterable<DeclarationChecker> declarationCheckers;

    public ModifiersChecker(@NotNull AnnotationChecker annotationChecker, @NotNull Iterable<DeclarationChecker> declarationCheckers) {
        this.annotationChecker = annotationChecker;
        this.declarationCheckers = declarationCheckers;
    }

    @NotNull
    public ModifiersCheckingProcedure withTrace(@NotNull BindingTrace trace) {
        return new ModifiersCheckingProcedure(trace);
    }
}
