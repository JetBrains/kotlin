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

package org.jetbrains.jet.plugin.refactoring.safeDelete;

import com.intellij.ide.IdeBundle;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.*;
import com.intellij.psi.search.searches.OverridingMethodsSearch;
import com.intellij.psi.search.searches.ReferencesSearch;
import com.intellij.psi.util.MethodSignatureUtil;
import com.intellij.psi.util.PsiFormatUtil;
import com.intellij.psi.util.PsiFormatUtilBase;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.refactoring.safeDelete.JavaSafeDeleteProcessor;
import com.intellij.refactoring.safeDelete.NonCodeUsageSearchInfo;
import com.intellij.refactoring.safeDelete.usageInfo.SafeDeleteOverridingMethodUsageInfo;
import com.intellij.refactoring.safeDelete.usageInfo.SafeDeleteReferenceJavaDeleteUsageInfo;
import com.intellij.refactoring.safeDelete.usageInfo.SafeDeleteReferenceSimpleDeleteUsageInfo;
import com.intellij.usageView.UsageInfo;
import com.intellij.util.ArrayUtilRt;
import com.intellij.util.Function;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.Processor;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.asJava.LightClassUtil;
import org.jetbrains.jet.lang.descriptors.CallableMemberDescriptor;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.Modality;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.BindingContextUtils;
import org.jetbrains.jet.lang.resolve.java.JetClsMethod;
import org.jetbrains.jet.lexer.JetTokens;
import org.jetbrains.jet.plugin.JetBundle;
import org.jetbrains.jet.plugin.project.AnalyzerFacadeWithCache;
import org.jetbrains.jet.renderer.DescriptorRenderer;

import java.util.*;

public class KotlinSafeDeleteProcessor extends JavaSafeDeleteProcessor {
    public static boolean canDeleteElement(@NotNull PsiElement element) {
        return element instanceof JetClassOrObject
               || element instanceof JetObjectDeclarationName
               || element instanceof JetNamedFunction
               || element instanceof PsiMethod
               || element instanceof JetProperty
               || element instanceof JetTypeParameter
               || element instanceof JetParameter;
    }

    @Override
    public boolean handlesElement(@NotNull PsiElement element) {
        return canDeleteElement(element);
    }

    @NotNull
    protected static NonCodeUsageSearchInfo getSearchInfo(
            @NotNull PsiElement element, @NotNull final Collection<? extends PsiElement> ignoredElements
    ) {

        return new NonCodeUsageSearchInfo(
                new Condition<PsiElement>() {
                    @Override
                    public boolean value(PsiElement usage) {
                        if (usage instanceof JetFile) return false;
                        return isInside(usage, ignoredElements);
                    }
                },
                element
        );
    }

    @NotNull
    protected static NonCodeUsageSearchInfo getSearchInfo(@NotNull PsiElement element, @NotNull PsiElement[] ignoredElements) {
        return getSearchInfo(element, Arrays.asList(ignoredElements));
    }

    @Nullable
    @Override
    public NonCodeUsageSearchInfo findUsages(
            @NotNull PsiElement element, @NotNull PsiElement[] allElementsToDelete, @NotNull List<UsageInfo> result
    ) {
        if (element instanceof JetClassOrObject) {
            return findClassOrObjectUsages(element, (JetClassOrObject) element, allElementsToDelete, result);
        }
        if (element instanceof JetObjectDeclarationName) {
            PsiElement parent = getObjectDeclarationOrFail(element);
            return findClassOrObjectUsages(element, (JetObjectDeclaration) parent, allElementsToDelete, result);
        }
        if (element instanceof JetNamedFunction) {
            return findFunctionUsages((JetNamedFunction) element, allElementsToDelete, result);
        }
        if (element instanceof PsiMethod) {
            return findPsiMethodUsages((PsiMethod) element, allElementsToDelete, result);
        }
        if (element instanceof JetProperty) {
            JetProperty property = (JetProperty) element;

            if (property.isLocal()) {
                return findLocalVariableUsages(property, allElementsToDelete, result);
            }
            return findPropertyUsages(property, allElementsToDelete, result);
        }
        if (element instanceof JetTypeParameter) {
            return findTypeParameterUsages((JetTypeParameter) element, allElementsToDelete, result);
        }
        if (element instanceof JetParameter) {
            JetParameter jetParameter = (JetParameter) element;

            PsiParameter psiParameter = getPsiParameter(jetParameter);
            if (psiParameter != null) {
                super.findUsages(psiParameter, allElementsToDelete, result);
            }

            return findParameterUsages(jetParameter, allElementsToDelete, result);
        }

        return getSearchInfo(element, allElementsToDelete);
    }

    @NotNull
    private static PsiElement getObjectDeclarationOrFail(@NotNull PsiElement element) {
        PsiElement parent = element.getParent();
        assert parent instanceof JetObjectDeclaration;
        return parent;
    }

    @SuppressWarnings("MethodOverridesPrivateMethodOfSuperclass")
    protected static boolean isInside(@NotNull PsiElement place, @NotNull PsiElement[] ancestors) {
        return isInside(place, Arrays.asList(ancestors));
    }

    @SuppressWarnings("MethodOverridesPrivateMethodOfSuperclass")
    protected static boolean isInside(@NotNull PsiElement place, @NotNull Collection<? extends PsiElement> ancestors) {
        for (PsiElement element : ancestors) {
            if (isInside(place, element)) return true;
        }
        return false;
    }

    @SuppressWarnings("MethodOverridesStaticMethodOfSuperclass")
    public static boolean isInside(@NotNull PsiElement place, @NotNull PsiElement ancestor) {
        if (ancestor instanceof JetClsMethod) {
            ancestor = ((JetClsMethod) ancestor).getOrigin();
        }
        return JavaSafeDeleteProcessor.isInside(place, ancestor);
    }

    @NotNull
    protected static NonCodeUsageSearchInfo findClassOrObjectUsages(
            @NotNull PsiElement referencedElement,
            @NotNull final JetClassOrObject classOrObject,
            @NotNull final PsiElement[] allElementsToDelete,
            @NotNull final List<UsageInfo> result
    ) {
        ReferencesSearch.search(referencedElement).forEach(new Processor<PsiReference>() {
            @Override
            public boolean process(PsiReference reference) {
                PsiElement element = reference.getElement();

                if (!isInside(element, allElementsToDelete)) {
                    JetImportDirective importDirective = PsiTreeUtil.getParentOfType(element, JetImportDirective.class, false);
                    if (importDirective != null) {
                        result.add(new SafeDeleteImportDirectiveUsageInfo(importDirective, classOrObject));
                        return true;
                    }

                    result.add(new SafeDeleteReferenceSimpleDeleteUsageInfo(element, classOrObject, false));
                }
                return true;
            }
        });

        return getSearchInfo(referencedElement, allElementsToDelete);
    }

    @Nullable
    protected NonCodeUsageSearchInfo findPsiMethodUsages(
            @NotNull PsiMethod method,
            @NotNull PsiElement[] allElementsToDelete,
            @NotNull List<UsageInfo> result
    ) {
        List<UsageInfo> javaUsages = new ArrayList<UsageInfo>();
        NonCodeUsageSearchInfo searchInfo = super.findUsages(method, allElementsToDelete, javaUsages);

        for (UsageInfo usageInfo : javaUsages) {
            if (usageInfo instanceof SafeDeleteOverridingMethodUsageInfo) {
                SafeDeleteOverridingMethodUsageInfo overrideUsageInfo = (SafeDeleteOverridingMethodUsageInfo) usageInfo;
                usageInfo = new KotlinSafeDeleteOverridingUsageInfo(
                        overrideUsageInfo.getSmartPointer().getElement(), overrideUsageInfo.getReferencedElement()
                );
            }
            result.add(usageInfo);
        }

        return searchInfo;
    }

    @NotNull
    private static <T, C extends T> List<C> difference(@NotNull Collection<C> from, @NotNull T[] a) {
        List<C> list = new ArrayList<C>(from);
        list.removeAll(Arrays.asList(a));
        return list;
    }

    private static void processDeclarationUsages(
            @NotNull JetDeclaration declaration,
            @NotNull PsiElement[] allElementsToDelete,
            @NotNull List<UsageInfo> result,
            @NotNull Collection<PsiReference> references,
            @NotNull List<? extends PsiElement> overridingDeclarations
    ) {
        for (PsiReference reference : references) {
            PsiElement element = reference.getElement();
            if (!isInside(element, allElementsToDelete) && !isInside(element, overridingDeclarations)) {
                JetImportDirective importDirective = PsiTreeUtil.getParentOfType(element, JetImportDirective.class, false);
                if (importDirective != null) {
                    result.add(new SafeDeleteImportDirectiveUsageInfo(importDirective, declaration));
                }
                else {
                    result.add(new SafeDeleteReferenceSimpleDeleteUsageInfo(element, declaration, false));
                }
            }
        }
    }

    @NotNull
    protected static NonCodeUsageSearchInfo findFunctionUsages(
            @NotNull JetNamedFunction function,
            @NotNull PsiElement[] allElementsToDelete,
            @NotNull List<UsageInfo> result
    ) {
        PsiMethod lightMethod = LightClassUtil.getLightClassMethod(function);
        if (lightMethod == null) {
            return getSearchInfo(function, allElementsToDelete);
        }

        Collection<PsiReference> references = ReferencesSearch.search(function).findAll();
        List<PsiMethod> overridingMethods = difference(OverridingMethodsSearch.search(lightMethod, true).findAll(), allElementsToDelete);

        processDeclarationUsages(function, allElementsToDelete, result, references, overridingMethods);

        Map<PsiMethod, Collection<PsiReference>> methodToReferences = getOverridingUsagesMap(overridingMethods);
        Set<PsiMethod> safeOverriding =
                filterSafeOverridingMethods(lightMethod, references, overridingMethods, methodToReferences, result, allElementsToDelete);

        List<PsiElement> ignoredElements = new ArrayList<PsiElement>(safeOverriding);
        ContainerUtil.addAll(ignoredElements, allElementsToDelete);
        return getSearchInfo(function, ignoredElements);
    }

    @NotNull
    protected static NonCodeUsageSearchInfo findPropertyUsages(
            @NotNull JetProperty property,
            @NotNull PsiElement[] allElementsToDelete,
            @NotNull List<UsageInfo> result
    ) {
        LightClassUtil.PropertyAccessorsPsiMethods propertyMethods = LightClassUtil.getLightClassPropertyMethods(property);
        PsiMethod getter = propertyMethods.getGetter();
        PsiMethod setter = propertyMethods.getSetter();

        Collection<PsiReference> references = ReferencesSearch.search(property).findAll();

        Collection<PsiMethod> getterOverriding =
                (getter != null) ? OverridingMethodsSearch.search(getter, true).findAll() : Collections.<PsiMethod>emptyList();
        Collection<PsiMethod> setterOverriding =
                (setter != null) ? OverridingMethodsSearch.search(setter, true).findAll() : Collections.<PsiMethod>emptyList();

        List<PsiMethod> overridingMethods = new ArrayList<PsiMethod>();
        overridingMethods.addAll(getterOverriding);
        overridingMethods.addAll(setterOverriding);
        overridingMethods = difference(overridingMethods, allElementsToDelete);

        processDeclarationUsages(property, allElementsToDelete, result, references, overridingMethods);

        Map<PsiMethod, Collection<PsiReference>> methodToReferences = getOverridingUsagesMap(overridingMethods);
        Set<PsiMethod> safeGetterOverriding =
                getter != null
                ? filterSafeOverridingMethods(getter, references, getterOverriding, methodToReferences, result, allElementsToDelete)
                : Collections.<PsiMethod>emptySet();
        Set<PsiMethod> safeSetterOverriding =
                setter != null
                ? filterSafeOverridingMethods(setter, references, setterOverriding, methodToReferences, result, allElementsToDelete)
                : Collections.<PsiMethod>emptySet();

        List<PsiElement> ignoredElements = new ArrayList<PsiElement>(safeGetterOverriding);
        ignoredElements.addAll(safeSetterOverriding);
        ContainerUtil.addAll(ignoredElements, allElementsToDelete);
        return getSearchInfo(property, ignoredElements);
    }

    @NotNull
    private static Map<PsiMethod, Collection<PsiReference>> getOverridingUsagesMap(@NotNull List<PsiMethod> overridingMethods) {
        Map<PsiMethod, Collection<PsiReference>> methodToReferences = new HashMap<PsiMethod, Collection<PsiReference>>();
        for (PsiMethod overridingMethod : overridingMethods) {
            Collection<PsiReference> overridingReferences =
                    ReferencesSearch.search(
                            overridingMethod instanceof JetClsMethod ? ((JetClsMethod) overridingMethod).getOrigin() : overridingMethod
                    ).findAll();
            methodToReferences.put(overridingMethod, overridingReferences);
        }
        return methodToReferences;
    }

    @NotNull
    protected static NonCodeUsageSearchInfo findLocalVariableUsages(
            @NotNull final JetProperty property,
            @NotNull final PsiElement[] allElementsToDelete,
            @NotNull final List<UsageInfo> result
    ) {
        ReferencesSearch.search(property, property.getUseScope()).forEach(new Processor<PsiReference>() {
            @Override
            public boolean process(PsiReference reference) {
                PsiElement element = reference.getElement();
                if (!isInside(element, allElementsToDelete)) {
                    result.add(new SafeDeleteReferenceSimpleDeleteUsageInfo(element, property, false));
                }
                return true;
            }
        });

        return getSearchInfo(property, allElementsToDelete);
    }

    @NotNull
    protected static NonCodeUsageSearchInfo findParameterUsages(
            @NotNull final JetParameter parameter,
            @NotNull final PsiElement[] allElementsToDelete,
            @NotNull final List<UsageInfo> result
    ) {
        NonCodeUsageSearchInfo searchInfo = getSearchInfo(parameter, allElementsToDelete);

        final JetNamedFunction function = PsiTreeUtil.getParentOfType(parameter, JetNamedFunction.class);
        if (function == null || parameter.getParent() != function.getValueParameterList()) return searchInfo;

        final int parameterIndex = function.getValueParameters().indexOf(parameter);

        ReferencesSearch.search(parameter, parameter.getUseScope()).forEach(new Processor<PsiReference>() {
            @Override
            public boolean process(PsiReference reference) {
                PsiElement element = reference.getElement();
                if (!isInside(element, allElementsToDelete)) {
                    result.add(new SafeDeleteReferenceSimpleDeleteUsageInfo(element, parameter, false));
                }
                return true;
            }
        });

        ReferencesSearch.search(function).forEach(
                new Processor<PsiReference>() {
                    @Override
                    public boolean process(PsiReference reference) {
                        processParameterUsageInCall(reference, function, parameterIndex, result, parameter);
                        return true;
                    }
                }
        );

        return searchInfo;
    }

    static void processParameterUsageInCall(
            @NotNull PsiReference reference,
            @NotNull PsiElement originalDeclaration,
            int parameterIndex,
            @NotNull List<UsageInfo> result,
            @NotNull PsiElement parameter
    ) {
        PsiElement element = reference.getElement();

        JetCallExpression callExpression =
                PsiTreeUtil.getParentOfType(reference.getElement(), JetCallExpression.class, false);
        if (callExpression == null) return;

        JetExpression calleeExpression = callExpression.getCalleeExpression();
        if (!(calleeExpression instanceof JetReferenceExpression
              && PsiTreeUtil.isAncestor(calleeExpression, element, false))) return;

        BindingContext bindingContext =
                AnalyzerFacadeWithCache.analyzeFileWithCache((JetFile) element.getContainingFile()).getBindingContext();
        DeclarationDescriptor descriptor =
                bindingContext.get(BindingContext.REFERENCE_TARGET, (JetReferenceExpression) calleeExpression);
        if (descriptor == null) return;

        PsiElement declaration = BindingContextUtils.descriptorToDeclaration(bindingContext, descriptor);
        if (originalDeclaration.equals(declaration)) {
            List<? extends ValueArgument> args = callExpression.getValueArguments();
            int argCount = args.size();
            if (parameterIndex < argCount) {
                result.add(
                        new SafeDeleteValueArgumentListUsageInfo((JetValueArgument) args.get(parameterIndex), parameter)
                );
            }
            else {
                List<JetExpression> lambdaArgs = callExpression.getFunctionLiteralArguments();
                int lambdaIndex = parameterIndex - argCount;
                if (lambdaIndex < lambdaArgs.size()) {
                    result.add(new SafeDeleteReferenceSimpleDeleteUsageInfo(lambdaArgs.get(lambdaIndex), parameter, true));
                }
            }
        }
    }

    @NotNull
    protected static NonCodeUsageSearchInfo findTypeParameterUsages(
            @NotNull final JetTypeParameter parameter,
            @NotNull final PsiElement[] allElementsToDelete,
            @NotNull final List<UsageInfo> result
    ) {
        NonCodeUsageSearchInfo searchInfo = getSearchInfo(parameter, allElementsToDelete);

        ReferencesSearch.search(parameter).forEach(new Processor<PsiReference>() {
            @Override
            public boolean process(PsiReference reference) {
                PsiElement element = reference.getElement();

                if (!isInside(element, allElementsToDelete)) {
                    result.add(new SafeDeleteReferenceSimpleDeleteUsageInfo(element, parameter, false));
                }
                return true;
            }
        });

        JetTypeParameterListOwner owner = PsiTreeUtil.getParentOfType(parameter, JetTypeParameterListOwner.class);
        if (owner == null) return searchInfo;

        List<JetTypeParameter> parameterList = owner.getTypeParameters();
        final int parameterCount = parameterList.size();
        final int parameterIndex = parameterList.indexOf(parameter);

        ReferencesSearch.search(owner).forEach(
                new Processor<PsiReference>() {
                    @Override
                    public boolean process(PsiReference reference) {
                        if (reference instanceof PsiJavaCodeReferenceElement) {
                            processJavaTypeArgumentListCandidate(
                                    (PsiJavaCodeReferenceElement) reference, parameterIndex, parameterCount, result, parameter
                            );
                        }
                        else {
                            processKotlinTypeArgumentListCandidate(reference, parameterIndex, result, parameter);
                        }
                        return true;
                    }
                }
        );

        return searchInfo;
    }

    private static void processJavaTypeArgumentListCandidate(
            @NotNull PsiJavaCodeReferenceElement reference,
            int parameterIndex,
            int parameterCount,
            @NotNull List<UsageInfo> result,
            @NotNull PsiElement parameter
    ) {
        PsiReferenceParameterList parameterList = reference.getParameterList();
        if (parameterList != null) {
            PsiTypeElement[] typeArgs = parameterList.getTypeParameterElements();
            if (typeArgs.length > parameterIndex) {
                if (typeArgs.length == 1 && parameterCount > 1
                    && typeArgs[0].getType() instanceof PsiDiamondType) {
                    return;
                }
                result.add(new SafeDeleteReferenceJavaDeleteUsageInfo(typeArgs[parameterIndex], parameter, true));
            }
        }
    }

    private static void processKotlinTypeArgumentListCandidate(
            @NotNull PsiReference reference,
            int parameterIndex,
            @NotNull List<UsageInfo> result,
            @NotNull JetTypeParameter parameter
    ) {
        PsiElement referencedElement = reference.getElement();

        JetTypeArgumentList argList = null;

        JetUserType type = PsiTreeUtil.getParentOfType(referencedElement, JetUserType.class);
        if (type != null) {
            argList = type.getTypeArgumentList();
        }
        else {
            JetCallExpression callExpression = PsiTreeUtil.getParentOfType(referencedElement, JetCallExpression.class);
            if (callExpression != null) {
                argList = callExpression.getTypeArgumentList();
            }
        }

        if (argList != null) {
            List<JetTypeProjection> projections = argList.getArguments();
            if (parameterIndex < projections.size()) {
                result.add(new SafeDeleteTypeArgumentListUsageInfo(projections.get(parameterIndex), parameter));
            }
        }
    }

    /*
     * Mostly copied from JavaSafeDeleteProcessor.validateOverridingMethods
     * Revision: d4fc033
     * (simplified and implemented proper treatment of light methods)
     */
    private static Set<PsiMethod> filterSafeOverridingMethods(
            @NotNull PsiMethod originalMethod,
            @NotNull Collection<PsiReference> originalReferences,
            @NotNull Collection<PsiMethod> overridingMethods,
            @NotNull Map<PsiMethod, Collection<PsiReference>> methodToReferences,
            @NotNull List<UsageInfo> usages,
            @NotNull PsiElement[] allElementsToDelete
    ) {
        Set<PsiMethod> validOverriding = new LinkedHashSet<PsiMethod>(overridingMethods);
        boolean anyNewBadRefs;
        do {
            anyNewBadRefs = false;
            for (PsiMethod overridingMethod : overridingMethods) {
                if (validOverriding.contains(overridingMethod)) {
                    Collection<PsiReference> overridingReferences = methodToReferences.get(overridingMethod);
                    boolean anyOverridingRefs = false;
                    for (PsiReference overridingReference : overridingReferences) {
                        PsiElement element = overridingReference.getElement();
                        if (!isInside(element, allElementsToDelete) && !isInside(element, validOverriding)) {
                            anyOverridingRefs = true;
                            break;
                        }
                    }

                    if (!anyOverridingRefs && isMultipleInterfacesImplementation(overridingMethod, originalMethod, allElementsToDelete)) {
                        anyOverridingRefs = true;
                    }

                    if (anyOverridingRefs) {
                        validOverriding.remove(overridingMethod);
                        anyNewBadRefs = true;

                        for (PsiReference reference : originalReferences) {
                            PsiElement element = reference.getElement();
                            if (!isInside(element, allElementsToDelete) && !isInside(element, overridingMethods)) {
                                validOverriding.clear();
                            }
                        }
                    }
                }
            }
        }
        while (anyNewBadRefs && !validOverriding.isEmpty());

        for (PsiMethod method : validOverriding) {
            if (method != originalMethod) {
                usages.add(new KotlinSafeDeleteOverridingUsageInfo(method, originalMethod));
            }
        }

        return validOverriding;
    }

    @SuppressWarnings("MethodOverridesPrivateMethodOfSuperclass")
    private static boolean isMultipleInterfacesImplementation(
            @NotNull PsiMethod method, @NotNull PsiMethod originalMethod, @NotNull PsiElement[] ignore
    ) {
        PsiMethod[] methods = method.findSuperMethods();
        for (PsiMethod superMethod : methods) {
            PsiElement relevantElement = superMethod instanceof JetClsMethod ? ((JetClsMethod) superMethod).getOrigin() : superMethod;
            relevantElement = JetPsiUtil.ascendIfPropertyAccessor(relevantElement);
            if (ArrayUtilRt.find(ignore, relevantElement) < 0 && !MethodSignatureUtil.isSuperMethod(originalMethod, superMethod)) {
                return true;
            }
        }
        return false;
    }

    @NotNull
    private static String wrapOrSkip(@NotNull String s, boolean inCode) {
        return inCode ? "<code>" + s + "</code>" : s;
    }

    @NotNull
    private static String formatClass(
            @NotNull DeclarationDescriptor classDescriptor,
            @NotNull BindingContext bindingContext,
            boolean inCode
    ) {
        PsiElement element = BindingContextUtils.descriptorToDeclaration(bindingContext, classDescriptor);
        if (element instanceof PsiClass) {
            return formatPsiClass((PsiClass) element, false, inCode);
        }

        return wrapOrSkip(formatClassDescriptor(classDescriptor), inCode);
    }

    @NotNull
    private static String formatFunction(
            @NotNull DeclarationDescriptor functionDescriptor,
            @NotNull BindingContext bindingContext,
            boolean inCode
    ) {
        PsiElement element = BindingContextUtils.descriptorToDeclaration(bindingContext, functionDescriptor);
        if (element instanceof PsiMethod) {
            return formatPsiMethod((PsiMethod) element, false, inCode);
        }

        return wrapOrSkip(formatFunctionDescriptor(functionDescriptor), inCode);
    }

    @NotNull
    private static String formatClassDescriptor(@NotNull DeclarationDescriptor classDescriptor) {
        return DescriptorRenderer.SOURCE_CODE_SHORT_NAMES_IN_TYPES.render(classDescriptor);
    }

    @NotNull
    private static String formatFunctionDescriptor(@NotNull DeclarationDescriptor functionDescriptor) {
        return DescriptorRenderer.COMPACT.render(functionDescriptor);
    }

    @NotNull
    public static String formatPsiClass(
            @NotNull PsiClass psiClass,
            boolean markAsJava,
            boolean inCode
    ) {
        String description;

        String kind = psiClass.isInterface() ? "interface " : "class ";
        description = kind + PsiFormatUtil.formatClass(
                psiClass,
                PsiFormatUtilBase.SHOW_CONTAINING_CLASS
                | PsiFormatUtilBase.SHOW_NAME
                | PsiFormatUtilBase.SHOW_PARAMETERS
                | PsiFormatUtilBase.SHOW_TYPE
        );
        description = wrapOrSkip(description, inCode);

        return markAsJava ? "[Java] " + description : description;
    }

    @NotNull
    public static String formatPsiMethod(
            @NotNull PsiMethod psiMethod,
            boolean showContainingClass,
            boolean inCode) {
        int options = PsiFormatUtilBase.SHOW_NAME | PsiFormatUtilBase.SHOW_PARAMETERS | PsiFormatUtilBase.SHOW_TYPE;
        if (showContainingClass) {
            //noinspection ConstantConditions
            options |= PsiFormatUtilBase.SHOW_CONTAINING_CLASS;
        }

        String description = PsiFormatUtil.formatMethod(psiMethod, PsiSubstitutor.EMPTY, options, PsiFormatUtilBase.SHOW_TYPE);
        description = wrapOrSkip(description, inCode);

        return "[Java] " + description;
    }

    @Override
    @Nullable
    public Collection<String> findConflicts(@NotNull PsiElement element, @NotNull PsiElement[] allElementsToDelete) {
        if (element instanceof JetNamedFunction || element instanceof JetProperty) {
            JetClass jetClass = PsiTreeUtil.getParentOfType(element, JetClass.class);
            if (jetClass == null || jetClass.getBody() != element.getParent()) return null;

            JetModifierList modifierList = jetClass.getModifierList();
            if (modifierList != null && modifierList.hasModifier(JetTokens.ABSTRACT_KEYWORD)) return null;

            BindingContext bindingContext =
                    AnalyzerFacadeWithCache.analyzeFileWithCache((JetFile) element.getContainingFile()).getBindingContext();

            DeclarationDescriptor declarationDescriptor = bindingContext.get(BindingContext.DECLARATION_TO_DESCRIPTOR, element);
            if (!(declarationDescriptor instanceof CallableMemberDescriptor)) return null;

            List<String> messages = new ArrayList<String>();
            CallableMemberDescriptor callableDescriptor = (CallableMemberDescriptor) declarationDescriptor;
            for (CallableMemberDescriptor overridenDescriptor : callableDescriptor.getOverriddenDescriptors()) {
                if (overridenDescriptor.getModality() == Modality.ABSTRACT) {
                    String message = JetBundle.message(
                            "x.implements.y",
                            formatFunction(callableDescriptor, bindingContext, true),
                            formatClass(callableDescriptor.getContainingDeclaration(), bindingContext, true),
                            formatFunction(overridenDescriptor, bindingContext, true),
                            formatClass(overridenDescriptor.getContainingDeclaration(), bindingContext, true)
                    );
                    messages.add(message);
                }
            }

            if (!messages.isEmpty()) return messages;
        }
        return super.findConflicts(element, allElementsToDelete);
    }

    /*
     * Mostly copied from JavaSafeDeleteProcessor.preprocessUsages
     * Revision: d4fc033
     * (replaced original dialog)
     */
    @Nullable
    @Override
    public UsageInfo[] preprocessUsages(@NotNull Project project, @NotNull UsageInfo[] usages) {
        ArrayList<UsageInfo> result = new ArrayList<UsageInfo>();
        ArrayList<UsageInfo> overridingMethodUsages = new ArrayList<UsageInfo>();

        for (UsageInfo usage : usages) {
            if (usage instanceof KotlinSafeDeleteOverridingUsageInfo) {
                overridingMethodUsages.add(usage);
            }
            else {
                result.add(usage);
            }
        }

        if (!overridingMethodUsages.isEmpty()) {
            if (ApplicationManager.getApplication().isUnitTestMode()) {
                result.addAll(overridingMethodUsages);
            }
            else {
                KotlinOverridingDialog dialog = new KotlinOverridingDialog(project, overridingMethodUsages);
                dialog.show();
                if (!dialog.isOK()) return null;
                result.addAll(dialog.getSelected());
            }
        }

        return result.toArray(new UsageInfo[result.size()]);
    }

    static void removeOverrideModifier(@NotNull PsiElement element) {
        if (element instanceof JetNamedFunction || element instanceof JetProperty) {
            JetModifierList modifierList = ((JetModifierListOwner) element).getModifierList();
            if (modifierList == null) return;

            PsiElement overrideModifier = modifierList.getModifier(JetTokens.OVERRIDE_KEYWORD);
            if (overrideModifier != null) {
                overrideModifier.delete();
            }
        }
        else if (element instanceof PsiMethod) {
            PsiMethod method = (PsiMethod) element;

            PsiAnnotation overrideAnnotation = null;
            for (PsiAnnotation annotation : method.getModifierList().getAnnotations()) {
                if ("java.lang.Override".equals(annotation.getQualifiedName())) {
                    overrideAnnotation = annotation;
                    break;
                }
            }

            if (overrideAnnotation != null) {
                overrideAnnotation.delete();
            }
        }
    }

    @Nullable
    private static PsiParameter getPsiParameter(@NotNull JetParameter parameter) {
        JetNamedFunction function = PsiTreeUtil.getParentOfType(parameter, JetNamedFunction.class);
        if (function == null || parameter.getParent() != function.getValueParameterList()) return null;

        PsiMethod lightMethod = LightClassUtil.getLightClassMethod(function);
        if (lightMethod == null) return null;

        int parameterIndex = function.getValueParameters().indexOf(parameter);
        return lightMethod.getParameterList().getParameters()[parameterIndex];
    }

    @Override
    public void prepareForDeletion(@NotNull PsiElement element) throws IncorrectOperationException {
        if (element instanceof PsiMethod) {
            cleanUpOverrides((PsiMethod) element);
        }
        else if (element instanceof JetNamedFunction) {
            PsiMethod lightMethod = LightClassUtil.getLightClassMethod((JetNamedFunction) element);
            if (lightMethod == null) {
                return;
            }

            cleanUpOverrides(lightMethod);
        }
        else if (element instanceof JetProperty) {
            LightClassUtil.PropertyAccessorsPsiMethods propertyMethods =
                    LightClassUtil.getLightClassPropertyMethods((JetProperty) element);
            PsiMethod getter = propertyMethods.getGetter();
            PsiMethod setter = propertyMethods.getSetter();

            if (getter != null) {
                cleanUpOverrides(getter);
            }
            if (setter != null) {
                cleanUpOverrides(setter);
            }
        }
        else if (element instanceof JetTypeParameter) {
            deleteElementAndCleanParent(element);
        }
        else if (element instanceof JetParameter) {
            JetPsiUtil.deleteElementWithDelimiters(element);
        }
    }

    public static void deleteElementAndCleanParent(@NotNull PsiElement element) {
        PsiElement parent = element.getParent();
        JetPsiUtil.deleteElementWithDelimiters(element);
        JetPsiUtil.deleteChildlessElement(parent, element.getClass());
    }

    private static boolean checkPsiMethodEquality(@NotNull PsiMethod method1, @NotNull PsiMethod method2) {
        if (method1 instanceof JetClsMethod && method2 instanceof JetClsMethod) {
            return ((JetClsMethod) method1).getOrigin().equals(((JetClsMethod) method2).getOrigin());
        }
        return method1.equals(method2);
    }

    public static void cleanUpOverrides(@NotNull PsiMethod method) {
        Collection<PsiMethod> superMethods = Arrays.asList(method.findSuperMethods(true));
        Collection<PsiMethod> overridingMethods = OverridingMethodsSearch.search(method, true).findAll();
        overrideLoop:
        for (PsiMethod overridingMethod : overridingMethods) {
            PsiElement overridingElement = overridingMethod instanceof JetClsMethod
                                           ? ((JetClsMethod) overridingMethod).getOrigin()
                                           : overridingMethod;

            Collection<PsiMethod> currentSuperMethods = new ArrayList<PsiMethod>();
            ContainerUtil.addAll(currentSuperMethods, overridingMethod.findSuperMethods(true));
            currentSuperMethods.addAll(superMethods);
            for (PsiMethod superMethod : currentSuperMethods) {
                if (!checkPsiMethodEquality(superMethod, method)) continue overrideLoop;
            }

            removeOverrideModifier(overridingElement);
        }
    }

    @Nullable
    private static Collection<? extends PsiElement> checkSuperMethods(
            @NotNull JetDeclaration declaration, @Nullable Collection<PsiElement> ignore
    ) {
        final BindingContext bindingContext =
                AnalyzerFacadeWithCache.analyzeFileWithCache((JetFile) declaration.getContainingFile()).getBindingContext();

        DeclarationDescriptor declarationDescriptor = bindingContext.get(BindingContext.DECLARATION_TO_DESCRIPTOR, declaration);
        if (!(declarationDescriptor instanceof CallableMemberDescriptor)) return null;

        CallableMemberDescriptor callableDescriptor = (CallableMemberDescriptor) declarationDescriptor;
        Set<? extends CallableMemberDescriptor> overridenDescriptors = callableDescriptor.getOverriddenDescriptors();

        Collection<? extends PsiElement> superMethods = ContainerUtil.map(
                overridenDescriptors,
                new Function<CallableMemberDescriptor, PsiElement>() {
                    @Override
                    public PsiElement fun(CallableMemberDescriptor descriptor) {
                        return BindingContextUtils.descriptorToDeclaration(bindingContext, descriptor);
                    }
                }
        );
        if (ignore != null) {
            superMethods.removeAll(ignore);
        }

        if (superMethods.isEmpty()) return Collections.singletonList(declaration);

        List<String> superClasses = getClassDescriptions(bindingContext, superMethods);
        return askUserForMethodsToSearch(declaration, callableDescriptor, superMethods, superClasses);
    }

    @NotNull
    private static Collection<? extends PsiElement> askUserForMethodsToSearch(
            @NotNull JetDeclaration declaration,
            @NotNull CallableMemberDescriptor callableDescriptor,
            @NotNull Collection<? extends PsiElement> superMethods,
            @NotNull List<String> superClasses
    ) {
        String superClassesStr = "\n" + StringUtil.join(superClasses, "");
        String message = JetBundle.message(
                "x.overrides.y.in.class.list",
                DescriptorRenderer.COMPACT.render(callableDescriptor),
                DescriptorRenderer.SOURCE_CODE_SHORT_NAMES_IN_TYPES.render(callableDescriptor.getContainingDeclaration()),
                superClassesStr
        );

        int exitCode = Messages.showYesNoCancelDialog(
                declaration.getProject(), message, IdeBundle.message("title.warning"), Messages.getQuestionIcon()
        );
        switch (exitCode) {
            case Messages.YES:
                return superMethods;
            case Messages.NO:
                return Collections.singletonList(declaration);
            default:
                return Collections.emptyList();
        }
    }

    @NotNull
    private static List<String> getClassDescriptions(
            @NotNull final BindingContext bindingContext, @NotNull Collection<? extends PsiElement> superMethods
    ) {
        return ContainerUtil.map(
                superMethods,
                new Function<PsiElement, String>() {
                    @Override
                    public String fun(PsiElement element) {
                        String description;

                        if (element instanceof JetNamedFunction || element instanceof JetProperty) {
                            DeclarationDescriptor descriptor =
                                    bindingContext.get(BindingContext.DECLARATION_TO_DESCRIPTOR, element);
                            assert descriptor != null;

                            DeclarationDescriptor containingDescriptor = descriptor.getContainingDeclaration();
                            assert containingDescriptor != null;

                            description = formatClassDescriptor(containingDescriptor);
                        }
                        else {
                            assert element instanceof PsiMethod;

                            PsiClass psiClass = ((PsiMethod) element).getContainingClass();
                            assert psiClass != null;

                            description = formatPsiClass(psiClass, true, false);
                        }

                        return "    " + description + "\n";
                    }
                }
        );
    }

    @Nullable
    @Override
    public Collection<? extends PsiElement> getElementsToSearch(
            @NotNull PsiElement element, @Nullable Module module, @NotNull Collection<PsiElement> allElementsToDelete
    ) {
        if (element instanceof JetParameter) {
            PsiParameter psiParameter = getPsiParameter((JetParameter) element);
            if (psiParameter != null) return checkParametersInMethodHierarchy(psiParameter);
        }

        if (element instanceof PsiParameter) {
            return checkParametersInMethodHierarchy((PsiParameter) element);
        }

        if (ApplicationManager.getApplication().isUnitTestMode()) {
            return Collections.singletonList(element);
        }

        if (element instanceof JetNamedFunction || element instanceof JetProperty) {
            return checkSuperMethods((JetDeclaration) element, allElementsToDelete);
        }

        return super.getElementsToSearch(element, module, allElementsToDelete);
    }

    @Nullable
    private static Collection<? extends PsiElement> checkParametersInMethodHierarchy(@NotNull PsiParameter parameter) {
        PsiMethod method = (PsiMethod)parameter.getDeclarationScope();
        int parameterIndex = method.getParameterList().getParameterIndex(parameter);

        Set<PsiElement> parametersToDelete = collectParametersToDelete(method, parameterIndex);
        if (parametersToDelete.size() > 1) {
            if (ApplicationManager.getApplication().isUnitTestMode()) {
                return parametersToDelete;
            }

            String message =
                    JetBundle.message("delete.param.in.method.hierarchy", formatJavaOrLightMethod(method));
            int exitCode = Messages.showOkCancelDialog(
                    parameter.getProject(), message, IdeBundle.message("title.warning"), Messages.getQuestionIcon()
            );
            if (exitCode == Messages.OK) {
                return parametersToDelete;
            }
            else {
                return null;
            }
        }

        return parametersToDelete;
    }

    // TODO: generalize breadth-first search
    @NotNull
    private static Set<PsiElement> collectParametersToDelete(@NotNull PsiMethod method, int parameterIndex) {
        Deque<PsiMethod> queue = new ArrayDeque<PsiMethod>();
        Set<PsiMethod> visited = new HashSet<PsiMethod>();
        Set<PsiElement> parametersToDelete = new HashSet<PsiElement>();

        queue.add(method);
        while (!queue.isEmpty()) {
            PsiMethod currentMethod = queue.poll();

            visited.add(currentMethod);
            addParameter(currentMethod, parametersToDelete, parameterIndex);

            for (PsiMethod superMethod : currentMethod.findSuperMethods(true)) {
                if (!visited.contains(superMethod)) {
                    queue.offer(superMethod);
                }
            }
            for (PsiMethod overrider : OverridingMethodsSearch.search(currentMethod)) {
                if (!visited.contains(overrider)) {
                    queue.offer(overrider);
                }
            }
        }
        return parametersToDelete;
    }

    @NotNull
    private static String formatJavaOrLightMethod(@NotNull PsiMethod method) {
        if (method instanceof JetClsMethod) {
            JetDeclaration declaration = ((JetClsMethod) method).getOrigin();
            BindingContext bindingContext =
                    AnalyzerFacadeWithCache.analyzeFileWithCache((JetFile) declaration.getContainingFile()).getBindingContext();
            DeclarationDescriptor descriptor =
                    bindingContext.get(BindingContext.DECLARATION_TO_DESCRIPTOR, declaration);
            if (descriptor != null) return formatFunctionDescriptor(descriptor);
        }
        return formatPsiMethod(method, false, false);
    }

    private static void addParameter(@NotNull PsiMethod method, @NotNull Set<PsiElement> result, int parameterIndex) {
        if (method instanceof JetClsMethod) {
            JetDeclaration declaration = ((JetClsMethod) method).getOrigin();
            if (declaration instanceof JetNamedFunction) {
                result.add(((JetNamedFunction) declaration).getValueParameters().get(parameterIndex));
            }
        }
        else {
            result.add(method.getParameterList().getParameters()[parameterIndex]);
        }
    }

    @Nullable
    @Override
    public Collection<PsiElement> getAdditionalElementsToDelete(
            @NotNull PsiElement element, @NotNull Collection<PsiElement> allElementsToDelete, boolean askUser
    ) {
        if (element instanceof JetObjectDeclarationName) {
            return Arrays.asList(getObjectDeclarationOrFail(element));
        }
        return super.getAdditionalElementsToDelete(element, allElementsToDelete, askUser);
    }
}