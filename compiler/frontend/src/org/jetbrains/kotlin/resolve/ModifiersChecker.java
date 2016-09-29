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

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory1;
import org.jetbrains.kotlin.extensions.DeclarationAttributeAltererExtension;
import org.jetbrains.kotlin.lexer.KtKeywordToken;
import org.jetbrains.kotlin.lexer.KtModifierKeywordToken;
import org.jetbrains.kotlin.lexer.KtTokens;
import org.jetbrains.kotlin.psi.*;

import java.util.*;

import static org.jetbrains.kotlin.diagnostics.Errors.NESTED_CLASS_NOT_ALLOWED;
import static org.jetbrains.kotlin.lexer.KtTokens.*;
import static org.jetbrains.kotlin.psi.KtStubbedPsiUtil.getContainingDeclaration;

public class ModifiersChecker {
    private static final Set<KtModifierKeywordToken> MODIFIERS_ILLEGAL_ON_PARAMETERS;

    static {
        MODIFIERS_ILLEGAL_ON_PARAMETERS = Sets.newHashSet();
        MODIFIERS_ILLEGAL_ON_PARAMETERS.addAll(Arrays.asList(KtTokens.MODIFIER_KEYWORDS_ARRAY));
        MODIFIERS_ILLEGAL_ON_PARAMETERS.remove(KtTokens.VARARG_KEYWORD);
    }

    public static boolean isIllegalInner(@NotNull DeclarationDescriptor descriptor) {
        return checkIllegalInner(descriptor) != InnerModifierCheckResult.ALLOWED;
    }

    private enum InnerModifierCheckResult {
        ALLOWED,
        ILLEGAL_POSITION,
        IN_INTERFACE,
        IN_OBJECT,
    }


    // NOTE: just checks if this is legal context for companion modifier (Companion object descriptor can be created)
    // COMPANION_OBJECT_NOT_ALLOWED can be reported later
    public static boolean isCompanionModifierAllowed(@NotNull KtDeclaration declaration) {
        if (declaration instanceof KtObjectDeclaration) {
            KtDeclaration containingDeclaration = getContainingDeclaration(declaration);
            if (containingDeclaration instanceof KtClassOrObject) {
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

        if (DescriptorUtils.isInterface(containingDeclaration)) {
            return InnerModifierCheckResult.IN_INTERFACE;
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
        return containingClass.isInner() || DescriptorUtils.isLocal(containingClass);
    }

    @NotNull
    public static Modality resolveMemberModalityFromModifiers(
            @Nullable KtModifierListOwner modifierListOwner,
            @NotNull Modality defaultModality,
            @NotNull BindingContext bindingContext,
            @Nullable DeclarationDescriptor containingDescriptor
    ) {
        return resolveModalityFromModifiers(modifierListOwner, defaultModality,
                                            bindingContext, containingDescriptor, /* allowSealed = */ false);
    }

    @NotNull
    public static Modality resolveModalityFromModifiers(
            @Nullable KtModifierListOwner modifierListOwner,
            @NotNull Modality defaultModality,
            @NotNull BindingContext bindingContext,
            @Nullable DeclarationDescriptor containingDescriptor,
            boolean allowSealed
    ) {
        KtModifierList modifierList = (modifierListOwner != null) ? modifierListOwner.getModifierList() : null;
        Modality modality = resolveModalityFromModifiers(modifierList, defaultModality, allowSealed);

        if (modifierListOwner != null) {
            Collection<DeclarationAttributeAltererExtension> extensions =
                    DeclarationAttributeAltererExtension.Companion.getInstances(modifierListOwner.getProject());

            DeclarationDescriptor descriptor = bindingContext.get(BindingContext.DECLARATION_TO_DESCRIPTOR, modifierListOwner);
            for (DeclarationAttributeAltererExtension extension : extensions) {
                Modality newModality = extension.refineDeclarationModality(
                        modifierListOwner, descriptor, containingDescriptor, modality, bindingContext);

                if (newModality != null) {
                    modality = newModality;
                    break;
                }
            }
        }

        return modality;
    }

    @NotNull
    private static Modality resolveModalityFromModifiers(
            @Nullable KtModifierList modifierList,
            @NotNull Modality defaultModality,
            boolean allowSealed
    ) {
        if (modifierList == null) return defaultModality;
        boolean hasAbstractModifier = modifierList.hasModifier(ABSTRACT_KEYWORD);
        boolean hasOverrideModifier = modifierList.hasModifier(OVERRIDE_KEYWORD);

        if (allowSealed && modifierList.hasModifier(SEALED_KEYWORD)) {
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
            @NotNull KtModifierListOwner modifierListOwner,
            @NotNull Visibility defaultVisibility
    ) {
        return resolveVisibilityFromModifiers(modifierListOwner.getModifierList(), defaultVisibility);
    }

    public static Visibility resolveVisibilityFromModifiers(@Nullable KtModifierList modifierList, @NotNull Visibility defaultVisibility) {
        if (modifierList == null) return defaultVisibility;
        if (modifierList.hasModifier(PRIVATE_KEYWORD)) return Visibilities.PRIVATE;
        if (modifierList.hasModifier(PUBLIC_KEYWORD)) return Visibilities.PUBLIC;
        if (modifierList.hasModifier(PROTECTED_KEYWORD)) return Visibilities.PROTECTED;
        if (modifierList.hasModifier(INTERNAL_KEYWORD)) return Visibilities.INTERNAL;
        return defaultVisibility;
    }

    public static boolean isInnerClass(@Nullable KtModifierList modifierList) {
        return modifierList != null && modifierList.hasModifier(INNER_KEYWORD);
    }

    public class ModifiersCheckingProcedure {

        @NotNull
        private final BindingTrace trace;

        private ModifiersCheckingProcedure(@NotNull BindingTrace trace) {
            this.trace = trace;
        }

        public void checkParameterHasNoValOrVar(
                @NotNull KtValVarKeywordOwner parameter,
                @NotNull DiagnosticFactory1<PsiElement, KtKeywordToken> diagnosticFactory
        ) {
            PsiElement valOrVar = parameter.getValOrVarKeyword();
            if (valOrVar != null) {
                trace.report(diagnosticFactory.on(valOrVar, ((KtKeywordToken) valOrVar.getNode().getElementType())));
            }
        }

        public void checkModifiersForDeclaration(@NotNull KtDeclaration modifierListOwner, @NotNull MemberDescriptor descriptor) {
            checkNestedClassAllowed(modifierListOwner, descriptor);
            checkTypeParametersModifiers(modifierListOwner);
            checkModifierListCommon(modifierListOwner, descriptor);
        }

        private void checkModifierListCommon(@NotNull KtDeclaration modifierListOwner, @NotNull DeclarationDescriptor descriptor) {
            AnnotationUseSiteTargetChecker.INSTANCE.check(modifierListOwner, descriptor, trace);
            runDeclarationCheckers(modifierListOwner, descriptor);
            annotationChecker.check(modifierListOwner, trace, descriptor);
            ModifierCheckerCore.INSTANCE.check(modifierListOwner, trace, descriptor);
        }

        public void checkModifiersForLocalDeclaration(
                @NotNull KtDeclaration modifierListOwner,
                @NotNull DeclarationDescriptor descriptor
        ) {
            checkModifierListCommon(modifierListOwner, descriptor);
        }

        public void checkModifiersForDestructuringDeclaration(@NotNull KtDestructuringDeclaration multiDeclaration) {
            annotationChecker.check(multiDeclaration, trace, null);
            ModifierCheckerCore.INSTANCE.check(multiDeclaration, trace, null);
            for (KtDestructuringDeclarationEntry multiEntry: multiDeclaration.getEntries()) {
                annotationChecker.check(multiEntry, trace, null);
                ModifierCheckerCore.INSTANCE.check(multiEntry, trace, null);
                UnderscoreChecker.INSTANCE.checkNamed(multiEntry, trace);
            }
        }

        private void checkNestedClassAllowed(@NotNull KtModifierListOwner modifierListOwner, @NotNull DeclarationDescriptor descriptor) {
            if (modifierListOwner.hasModifier(INNER_KEYWORD)) return;
            if (modifierListOwner instanceof KtClass && !(modifierListOwner instanceof KtEnumEntry)) {
                KtClass aClass = (KtClass) modifierListOwner;
                boolean localEnumError = aClass.isLocal() && aClass.isEnum();
                if (!localEnumError && isIllegalNestedClass(descriptor)) {
                    trace.report(NESTED_CLASS_NOT_ALLOWED.on(aClass));
                }
            }
        }

        @NotNull
        public Map<KtModifierKeywordToken, PsiElement> getTokensCorrespondingToModifiers(
                @NotNull KtModifierList modifierList,
                @NotNull Collection<KtModifierKeywordToken> possibleModifiers
        ) {
            Map<KtModifierKeywordToken, PsiElement> tokens = Maps.newHashMap();
            for (KtModifierKeywordToken modifier : possibleModifiers) {
                if (modifierList.hasModifier(modifier)) {
                    tokens.put(modifier, modifierList.getModifier(modifier));
                }
            }
            return tokens;
        }


        public void runDeclarationCheckers(
                @NotNull KtDeclaration declaration,
                @NotNull DeclarationDescriptor descriptor
        ) {
            for (DeclarationChecker checker : declarationCheckers) {
                checker.check(declaration, descriptor, trace, trace.getBindingContext());
            }
        }

        public void checkTypeParametersModifiers(@NotNull KtModifierListOwner modifierListOwner) {
            if (!(modifierListOwner instanceof KtTypeParameterListOwner)) return;
            List<KtTypeParameter> typeParameters = ((KtTypeParameterListOwner) modifierListOwner).getTypeParameters();
            for (KtTypeParameter typeParameter : typeParameters) {
                ModifierCheckerCore.INSTANCE.check(typeParameter, trace, null);
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
