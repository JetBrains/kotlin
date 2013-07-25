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
import com.intellij.psi.util.*;
import com.intellij.refactoring.safeDelete.JavaSafeDeleteProcessor;
import com.intellij.refactoring.safeDelete.NonCodeUsageSearchInfo;
import com.intellij.refactoring.safeDelete.usageInfo.SafeDeleteOverridingMethodUsageInfo;
import com.intellij.refactoring.safeDelete.usageInfo.SafeDeleteReferenceSimpleDeleteUsageInfo;
import com.intellij.usageView.UsageInfo;
import com.intellij.util.*;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.asJava.LightClassUtil;
import org.jetbrains.jet.lang.descriptors.*;
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
    public static boolean canDeleteElement(PsiElement element) {
        return element instanceof JetClassOrObject
               || element instanceof JetObjectDeclarationName
               || element instanceof JetNamedFunction
               || element instanceof PsiMethod
               || element instanceof JetProperty;
    }

    @Override
    public boolean handlesElement(PsiElement element) {
        return canDeleteElement(element);
    }

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

    protected static NonCodeUsageSearchInfo getSearchInfo(@NotNull PsiElement element, @NotNull PsiElement[] ignoredElements) {
        return getSearchInfo(element, Arrays.asList(ignoredElements));
    }

    @Nullable
    @Override
    public NonCodeUsageSearchInfo findUsages(PsiElement element, PsiElement[] allElementsToDelete, List<UsageInfo> result) {
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
        return getSearchInfo(element, allElementsToDelete);
    }

    private static PsiElement getObjectDeclarationOrFail(PsiElement element) {
        PsiElement parent = element.getParent();
        assert parent instanceof JetObjectDeclaration;
        return parent;
    }

    @SuppressWarnings("MethodOverridesPrivateMethodOfSuperclass")
    protected static boolean isInside(PsiElement place, PsiElement[] ancestors) {
        return isInside(place, Arrays.asList(ancestors));
    }

    @SuppressWarnings("MethodOverridesPrivateMethodOfSuperclass")
    protected static boolean isInside(PsiElement place, Collection<? extends PsiElement> ancestors) {
        for (PsiElement element : ancestors) {
            if (isInside(place, element)) return true;
        }
        return false;
    }

    @SuppressWarnings("MethodOverridesStaticMethodOfSuperclass")
    public static boolean isInside(PsiElement place, PsiElement ancestor) {
        if (ancestor instanceof JetClsMethod) {
            ancestor = ((JetClsMethod) ancestor).getOrigin();
        }
        return JavaSafeDeleteProcessor.isInside(place, ancestor);
    }

    protected static NonCodeUsageSearchInfo findClassOrObjectUsages(
            PsiElement referencedElement,
            final JetClassOrObject classOrObject,
            final PsiElement[] allElementsToDelete,
            final List<UsageInfo> result
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

    protected NonCodeUsageSearchInfo findPsiMethodUsages(
            PsiMethod method,
            PsiElement[] allElementsToDelete,
            List<UsageInfo> result
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

    private static <T, C extends T> List<C> difference(Collection<C> from, T[] a) {
        List<C> list = new ArrayList<C>(from);
        list.removeAll(Arrays.asList(a));
        return list;
    }

    private static void processDeclarationUsages(
            JetDeclaration declaration,
            PsiElement[] allElementsToDelete,
            List<UsageInfo> result,
            Collection<PsiReference> references,
            List<? extends PsiElement> overridingDeclarations
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

    protected static NonCodeUsageSearchInfo findFunctionUsages(
            JetNamedFunction function,
            PsiElement[] allElementsToDelete,
            List<UsageInfo> result
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

    protected static NonCodeUsageSearchInfo findPropertyUsages(
            JetProperty property,
            PsiElement[] allElementsToDelete,
            List<UsageInfo> result
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
                filterSafeOverridingMethods(getter, references, getterOverriding, methodToReferences, result, allElementsToDelete);
        Set<PsiMethod> safeSetterOverriding =
                filterSafeOverridingMethods(setter, references, setterOverriding, methodToReferences, result, allElementsToDelete);

        List<PsiElement> ignoredElements = new ArrayList<PsiElement>(safeGetterOverriding);
        ignoredElements.addAll(safeSetterOverriding);
        ContainerUtil.addAll(ignoredElements, allElementsToDelete);
        return getSearchInfo(property, ignoredElements);
    }

    private static Map<PsiMethod, Collection<PsiReference>> getOverridingUsagesMap(List<PsiMethod> overridingMethods) {
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

    protected static NonCodeUsageSearchInfo findLocalVariableUsages(
            final JetProperty property,
            final PsiElement[] allElementsToDelete,
            final List<UsageInfo> result
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

    /*
     * Mostly copied from JavaSafeDeleteProcessor.validateOverridingMethods
     * Revision: d4fc033
     * (simplified and implemented proper treatment of light methods)
     */
    private static Set<PsiMethod> filterSafeOverridingMethods(
            PsiMethod originalMethod, Collection<PsiReference> originalReferences,
            Collection<PsiMethod> overridingMethods,
            Map<PsiMethod, Collection<PsiReference>> methodToReferences,
            List<UsageInfo> usages,
            PsiElement[] allElementsToDelete
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
    private static boolean isMultipleInterfacesImplementation(PsiMethod method, PsiMethod originalMethod, PsiElement[] ignore) {
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

    private static String wrapOrSkip(String s, boolean inCode) {
        return inCode ? "<code>" + s + "</code>" : s;
    }

    private static String formatClass(DeclarationDescriptor classDescriptor, BindingContext bindingContext, boolean inCode) {
        PsiElement element = BindingContextUtils.descriptorToDeclaration(bindingContext, classDescriptor);
        if (element instanceof PsiClass) {
            return formatPsiClass((PsiClass) element, false, inCode);
        }

        return wrapOrSkip(formatClassDescriptor(classDescriptor), inCode);
    }

    private static String formatFunction(DeclarationDescriptor functionDescriptor, BindingContext bindingContext, boolean inCode) {
        PsiElement element = BindingContextUtils.descriptorToDeclaration(bindingContext, functionDescriptor);
        if (element instanceof PsiMethod) {
            return formatPsiMethod((PsiMethod) element, false, inCode);
        }

        return wrapOrSkip(formatFunctionDescriptor(functionDescriptor), inCode);
    }

    private static String formatClassDescriptor(DeclarationDescriptor classDescriptor) {
        return DescriptorRenderer.SOURCE_CODE_SHORT_NAMES_IN_TYPES.render(classDescriptor);
    }

    private static String formatFunctionDescriptor(DeclarationDescriptor functionDescriptor) {
        return DescriptorRenderer.COMPACT.render(functionDescriptor);
    }

    public static String formatPsiClass(PsiClass psiClass, boolean markAsJava, boolean inCode) {
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

    public static String formatPsiMethod(PsiMethod psiMethod, boolean showContainingClass, boolean inCode) {
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
    public Collection<String> findConflicts(PsiElement element, PsiElement[] allElementsToDelete) {
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
    public UsageInfo[] preprocessUsages(Project project, UsageInfo[] usages) {
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

    private static void removeOverrideModifier(@NotNull PsiElement element) {
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

    @Override
    public void prepareForDeletion(PsiElement element) throws IncorrectOperationException {
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
    }

    private static boolean checkPsiMethodEquality(PsiMethod method1, PsiMethod method2) {
        if (method1 instanceof JetClsMethod && method2 instanceof JetClsMethod) {
            return ((JetClsMethod) method1).getOrigin().equals(((JetClsMethod) method2).getOrigin());
        }
        return method1.equals(method2);
    }

    private static void cleanUpOverrides(PsiMethod method) {
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

    private static Collection<? extends PsiElement> askUserForMethodsToSearch(
            JetDeclaration declaration,
            CallableMemberDescriptor callableDescriptor,
            Collection<? extends PsiElement> superMethods,
            List<String> superClasses
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

    private static List<String> getClassDescriptions(final BindingContext bindingContext, Collection<? extends PsiElement> superMethods) {
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
            PsiElement element, @Nullable Module module, Collection<PsiElement> allElementsToDelete
    ) {
        if (ApplicationManager.getApplication().isUnitTestMode()) {
            return Collections.singletonList(element);
        }

        if (element instanceof JetNamedFunction || element instanceof JetProperty) {
            return checkSuperMethods((JetDeclaration) element, allElementsToDelete);
        }
        return super.getElementsToSearch(element, module, allElementsToDelete);
    }

    @Override
    public Collection<PsiElement> getAdditionalElementsToDelete(
            PsiElement element, Collection<PsiElement> allElementsToDelete, boolean askUser
    ) {
        if (element instanceof JetObjectDeclarationName) {
            return Arrays.asList(getObjectDeclarationOrFail(element));
        }
        return super.getAdditionalElementsToDelete(element, allElementsToDelete, askUser);
    }
}