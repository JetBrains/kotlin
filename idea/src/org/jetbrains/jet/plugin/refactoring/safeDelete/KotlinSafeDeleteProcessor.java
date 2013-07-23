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
import com.intellij.psi.search.searches.SuperMethodsSearch;
import com.intellij.psi.util.*;
import com.intellij.refactoring.safeDelete.JavaSafeDeleteProcessor;
import com.intellij.refactoring.safeDelete.NonCodeUsageSearchInfo;
import com.intellij.refactoring.safeDelete.SafeDeleteProcessor;
import com.intellij.refactoring.safeDelete.usageInfo.SafeDeleteOverridingMethodUsageInfo;
import com.intellij.refactoring.safeDelete.usageInfo.SafeDeleteReferenceSimpleDeleteUsageInfo;
import com.intellij.usageView.UsageInfo;
import com.intellij.util.*;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.asJava.LightClassUtil;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor;
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
    public static boolean canDeleteElement(PsiElement element) {
        return element instanceof JetClassOrObject
               || element instanceof JetObjectDeclarationName
               || element instanceof JetNamedFunction
               || element instanceof PsiMethod;
    }

    @Override
    public boolean handlesElement(PsiElement element) {
        return canDeleteElement(element);
    }

    protected static NonCodeUsageSearchInfo getDefaultNonCodeUsageSearchInfo(
            @NotNull PsiElement element, @NotNull PsiElement[] allElementsToDelete
    ) {
        return new NonCodeUsageSearchInfo(SafeDeleteProcessor.getDefaultInsideDeletedCondition(allElementsToDelete), element);
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
        return getDefaultNonCodeUsageSearchInfo(element, allElementsToDelete);
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

        return getDefaultNonCodeUsageSearchInfo(referencedElement, allElementsToDelete);
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
                usageInfo = new KotlinSafeDeleteOverridingMethodUsageInfo(
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

    protected static NonCodeUsageSearchInfo findFunctionUsages(
            JetNamedFunction function,
            final PsiElement[] allElementsToDelete,
            List<UsageInfo> result
    ) {
        PsiMethod lightMethod = LightClassUtil.getLightClassMethod(function);
        if (lightMethod == null) {
            return getDefaultNonCodeUsageSearchInfo(function, allElementsToDelete);
        }

        Collection<PsiReference> references = ReferencesSearch.search(function).findAll();
        List<PsiMethod> overridingMethods = difference(OverridingMethodsSearch.search(lightMethod, true).findAll(), allElementsToDelete);

        for (PsiReference reference : references) {
            PsiElement element = reference.getElement();
            if (!isInside(element, allElementsToDelete) && !isInside(element, overridingMethods)) {
                JetImportDirective importDirective = PsiTreeUtil.getParentOfType(element, JetImportDirective.class, false);
                if (importDirective != null) {
                    result.add(new SafeDeleteImportDirectiveUsageInfo(importDirective, function));
                }
                else {
                    result.add(new SafeDeleteReferenceSimpleDeleteUsageInfo(element, function, false));
                }
            }
        }

        HashMap<PsiMethod, Collection<PsiReference>> methodToReferences = new HashMap<PsiMethod, Collection<PsiReference>>();
        for (PsiMethod overridingMethod : overridingMethods) {
            Collection<PsiReference> overridingReferences =
                    ReferencesSearch.search(
                            overridingMethod instanceof JetClsMethod ? ((JetClsMethod) overridingMethod).getOrigin() : overridingMethod
                    ).findAll();
            methodToReferences.put(overridingMethod, overridingReferences);
        }
        final Set<PsiMethod> safeOverriding =
                filterSafeOverridingMethods(lightMethod, references, overridingMethods, methodToReferences, result, allElementsToDelete);

        return new NonCodeUsageSearchInfo(
                new Condition<PsiElement>() {
                    @Override
                    public boolean value(PsiElement usage) {
                        if (usage instanceof JetFile) return false;
                        return isInside(usage, allElementsToDelete) || isInside(usage, safeOverriding);
                    }
                },
                function
        );
    }

    /*
     * Mostly copied from JavaSafeDeleteProcessor.validateOverridingMethods
     * Revision: d4fc033
     * (simplified and implemented proper treatment of light methods)
     */
    private static Set<PsiMethod> filterSafeOverridingMethods(
            PsiMethod originalMethod, Collection<PsiReference> originalReferences,
            Collection<PsiMethod> overridingMethods, HashMap<PsiMethod, Collection<PsiReference>> methodToReferences,
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
                usages.add(new KotlinSafeDeleteOverridingMethodUsageInfo(method, originalMethod));
            }
        }

        return validOverriding;
    }

    @SuppressWarnings("MethodOverridesPrivateMethodOfSuperclass")
    private static boolean isMultipleInterfacesImplementation(PsiMethod method, PsiMethod originalMethod, PsiElement[] ignore) {
        PsiMethod[] methods = method.findSuperMethods();
        for (PsiMethod superMethod: methods) {
            PsiElement relevantElement = superMethod instanceof JetClsMethod ? ((JetClsMethod) superMethod).getOrigin() : superMethod;
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
            options |= PsiFormatUtilBase.SHOW_CONTAINING_CLASS;
        }

        String description = PsiFormatUtil.formatMethod(psiMethod, PsiSubstitutor.EMPTY, options, PsiFormatUtilBase.SHOW_TYPE);
        description = wrapOrSkip(description, inCode);

        return "[Java] " + description;
    }

    @Override
    public Collection<String> findConflicts(PsiElement element, PsiElement[] allElementsToDelete) {
        if (element instanceof JetNamedFunction) {
            JetClass jetClass = PsiTreeUtil.getParentOfType(element, JetClass.class);
            if (jetClass == null || jetClass.getBody() != element.getParent()) return null;

            JetModifierList modifierList = jetClass.getModifierList();
            if (modifierList != null && modifierList.hasModifier(JetTokens.ABSTRACT_KEYWORD)) return null;

            BindingContext bindingContext =
                    AnalyzerFacadeWithCache.analyzeFileWithCache((JetFile) element.getContainingFile()).getBindingContext();

            DeclarationDescriptor declarationDescriptor = bindingContext.get(BindingContext.DECLARATION_TO_DESCRIPTOR, element);
            if (!(declarationDescriptor instanceof FunctionDescriptor)) return null;

            List<String> messages = new ArrayList<String>();
            FunctionDescriptor functionDescriptor = (FunctionDescriptor) declarationDescriptor;
            for (FunctionDescriptor overridenDescriptor : functionDescriptor.getOverriddenDescriptors()) {
                if (overridenDescriptor.getModality() == Modality.ABSTRACT) {
                    String message = JetBundle.message(
                            "x.implements.y",
                            formatFunction(functionDescriptor, bindingContext, true),
                            formatClass(functionDescriptor.getContainingDeclaration(), bindingContext, true),
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
            if (usage instanceof KotlinSafeDeleteOverridingMethodUsageInfo) {
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
                KotlinOverridingMethodsDialog dialog = new KotlinOverridingMethodsDialog(project, overridingMethodUsages);
                dialog.show();
                if (!dialog.isOK()) return null;
                result.addAll(dialog.getSelected());
            }
        }

        return result.toArray(new UsageInfo[result.size()]);
    }

    private static void removeOverrideModifier(@NotNull PsiElement element) {
        if (element instanceof JetNamedFunction) {
            JetModifierList modifierList = ((JetNamedFunction) element).getModifierList();
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
    }

    private static void cleanUpOverrides(PsiMethod method) {
        Collection<MethodSignatureBackedByPsiMethod> superMethods =
                SuperMethodsSearch.search(method, null, true, false).findAll();
        Collection<PsiMethod> overridingMethods = OverridingMethodsSearch.search(method, false).findAll();
        overrideLoop: for (PsiMethod overridingMethod : overridingMethods) {
            PsiElement overridingElement = overridingMethod instanceof JetClsMethod
                                           ? ((JetClsMethod) overridingMethod).getOrigin()
                                           : overridingMethod;
            Collection<MethodSignatureBackedByPsiMethod> currentSuperMethods =
                    SuperMethodsSearch.search(overridingMethod, null, true, false).findAll();
            currentSuperMethods.addAll(superMethods);
            for (MethodSignatureBackedByPsiMethod superMethod: currentSuperMethods) {
                if (superMethod.getMethod() != method) continue overrideLoop;
            }

            removeOverrideModifier(overridingElement);
        }
    }

    @Nullable
    private static Collection<? extends PsiElement> checkSuperMethods(
            @NotNull JetNamedFunction function, @Nullable Collection<PsiElement> ignore
    ) {
        final BindingContext bindingContext =
                AnalyzerFacadeWithCache.analyzeFileWithCache((JetFile) function.getContainingFile()).getBindingContext();

        DeclarationDescriptor declarationDescriptor = bindingContext.get(BindingContext.DECLARATION_TO_DESCRIPTOR, function);
        if (!(declarationDescriptor instanceof FunctionDescriptor)) return null;

        FunctionDescriptor functionDescriptor = (FunctionDescriptor) declarationDescriptor;
        Set<? extends FunctionDescriptor> overridenDescriptors = functionDescriptor.getOverriddenDescriptors();

        Collection<? extends PsiElement> superMethods = ContainerUtil.map(
                overridenDescriptors,
                new Function<FunctionDescriptor, PsiElement>() {
                    @Override
                    public PsiElement fun(FunctionDescriptor descriptor) {
                        return BindingContextUtils.descriptorToDeclaration(bindingContext, descriptor);
                    }
                }
        );
        if (ignore != null) {
            superMethods.removeAll(ignore);
        }

        if (superMethods.isEmpty()) return Collections.singletonList(function);

        List<String> superClasses = ContainerUtil.map(
                superMethods,
                new Function<PsiElement, String>() {
                    @Override
                    public String fun(PsiElement element) {
                        String description;

                        if (element instanceof JetNamedFunction) {
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

        String superClassesStr = "\n" + StringUtil.join(superClasses, "");
        String message = JetBundle.message(
                "x.overrides.y.in.class.list",
                DescriptorRenderer.COMPACT.render(functionDescriptor),
                DescriptorRenderer.SOURCE_CODE_SHORT_NAMES_IN_TYPES.render(functionDescriptor.getContainingDeclaration()),
                superClassesStr
        );

        int exitCode = Messages.showYesNoCancelDialog(
                function.getProject(), message, IdeBundle.message("title.warning"), Messages.getQuestionIcon()
        );
        switch (exitCode) {
            case Messages.YES:
                return superMethods;
            case Messages.NO:
                return Collections.singletonList(function);
            default:
                return Collections.emptyList();
        }
    }

    @Nullable
    @Override
    public Collection<? extends PsiElement> getElementsToSearch(
            PsiElement element, @Nullable Module module, Collection<PsiElement> allElementsToDelete
    ) {
        if (ApplicationManager.getApplication().isUnitTestMode()) {
            return Collections.singletonList(element);
        }

        if (element instanceof JetNamedFunction) {
            return checkSuperMethods((JetNamedFunction) element, allElementsToDelete);
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
