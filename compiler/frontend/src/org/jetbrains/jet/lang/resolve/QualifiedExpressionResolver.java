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

package org.jetbrains.jet.lang.resolve;

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.diagnostics.Errors;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import static org.jetbrains.jet.lang.diagnostics.Errors.*;

public class QualifiedExpressionResolver {
    private static final Predicate<DeclarationDescriptor> CLASSIFIERS_AND_NAMESPACES = new Predicate<DeclarationDescriptor>() {
        @Override
        public boolean apply(@Nullable DeclarationDescriptor descriptor) {
            return descriptor instanceof ClassifierDescriptor || descriptor instanceof PackageViewDescriptor;
        }
    };

    public enum LookupMode {
        // Only classifier and packages are resolved
        ONLY_CLASSES,

        // Resolve all descriptors
        EVERYTHING
    }

    @NotNull
    public Collection<? extends DeclarationDescriptor> analyseImportReference(
            @NotNull JetImportDirective importDirective,
            @NotNull JetScope scope,
            @NotNull BindingTrace trace,
            @NotNull ModuleDescriptor module
    ) {
        return processImportReference(importDirective, scope, scope, Importer.DO_NOTHING, trace, module, LookupMode.EVERYTHING);
    }

    @NotNull
    public Collection<? extends DeclarationDescriptor> processImportReference(
            @NotNull JetImportDirective importDirective,
            @NotNull JetScope scope,
            @NotNull JetScope scopeToCheckVisibility,
            @NotNull Importer importer,
            @NotNull BindingTrace trace,
            @NotNull ModuleDescriptor module,
            @NotNull LookupMode lookupMode
    ) {
        if (importDirective.isAbsoluteInRootNamespace()) {
            trace.report(UNSUPPORTED.on(importDirective, "TypeHierarchyResolver")); // TODO
            return Collections.emptyList();
        }
        JetExpression importedReference = importDirective.getImportedReference();
        if (importedReference == null) {
            return Collections.emptyList();
        }

        Collection<? extends DeclarationDescriptor> descriptors;
        if (importedReference instanceof JetQualifiedExpression) {
            //store result only when we find all descriptors, not only classes on the second phase
            descriptors = lookupDescriptorsForQualifiedExpression(
                    (JetQualifiedExpression)importedReference, scope, scopeToCheckVisibility, trace,
                    lookupMode, lookupMode == LookupMode.EVERYTHING);
        }
        else {
            assert importedReference instanceof JetSimpleNameExpression;
            descriptors = lookupDescriptorsForSimpleNameReference(
                    (JetSimpleNameExpression)importedReference, scope, scopeToCheckVisibility, trace,
                    lookupMode, true, lookupMode == LookupMode.EVERYTHING);
        }

        JetSimpleNameExpression referenceExpression = JetPsiUtil.getLastReference(importedReference);
        if (importDirective.isAllUnder()) {
            if (referenceExpression == null || !canImportMembersFrom(descriptors, referenceExpression, trace, lookupMode)) {
                return Collections.emptyList();
            }

            for (DeclarationDescriptor descriptor : descriptors) {
                importer.addAllUnderImport(descriptor, module.getPlatformToKotlinClassMap());
            }
            return Collections.emptyList();
        }

        Name aliasName = JetPsiUtil.getAliasName(importDirective);
        if (aliasName == null) {
            return Collections.emptyList();
        }

        for (DeclarationDescriptor descriptor : descriptors) {
            importer.addAliasImport(descriptor, aliasName);
        }

        return descriptors;
    }

    private boolean canImportMembersFrom(@NotNull Collection<? extends DeclarationDescriptor> descriptors,
            @NotNull JetSimpleNameExpression reference, @NotNull BindingTrace trace, @NotNull LookupMode lookupMode
    ) {

        if (lookupMode == LookupMode.ONLY_CLASSES) {
            return true;
        }

        if (descriptors.size() == 1) {
            return canImportMembersFrom(descriptors.iterator().next(), reference, trace, lookupMode);
        }
        TemporaryBindingTrace temporaryTrace = TemporaryBindingTrace.create(trace, "trace to find out if members can be imported from", reference);
        boolean canImport = false;
        for (DeclarationDescriptor descriptor : descriptors) {
            canImport |= canImportMembersFrom(descriptor, reference, temporaryTrace, lookupMode);
        }
        if (!canImport) {
            temporaryTrace.commit();
        }
        return canImport;
    }

    private boolean canImportMembersFrom(@NotNull DeclarationDescriptor descriptor,
            @NotNull JetSimpleNameExpression reference, @NotNull BindingTrace trace, @NotNull LookupMode lookupMode
    ) {

        assert lookupMode == LookupMode.EVERYTHING;
        if (descriptor instanceof PackageViewDescriptor) {
            return true;
        }
        if (descriptor instanceof ClassDescriptor && !((ClassDescriptor)descriptor).getKind().isSingleton()) {
            return true;
        }
        trace.report(CANNOT_IMPORT_FROM_ELEMENT.on(reference, descriptor));
        return false;
    }

    @NotNull
    public Collection<? extends DeclarationDescriptor> lookupDescriptorsForUserType(@NotNull JetUserType userType,
            @NotNull JetScope outerScope, @NotNull BindingTrace trace) {

        if (userType.isAbsoluteInRootNamespace()) {
            trace.report(Errors.UNSUPPORTED.on(userType, "package"));
            return Collections.emptyList();
        }
        JetSimpleNameExpression referenceExpression = userType.getReferenceExpression();
        if (referenceExpression == null) {
            return Collections.emptyList();
        }
        JetUserType qualifier = userType.getQualifier();
        if (qualifier == null) {
            return lookupDescriptorsForSimpleNameReference(referenceExpression, outerScope, outerScope, trace, LookupMode.ONLY_CLASSES, false, true);
        }
        Collection<? extends DeclarationDescriptor> declarationDescriptors = lookupDescriptorsForUserType(qualifier, outerScope, trace);
        return lookupSelectorDescriptors(referenceExpression, declarationDescriptors, trace, outerScope, LookupMode.ONLY_CLASSES, true);
    }

    @NotNull
    public Collection<? extends DeclarationDescriptor> lookupDescriptorsForQualifiedExpression(@NotNull JetQualifiedExpression importedReference,
            @NotNull JetScope outerScope, @NotNull JetScope scopeToCheckVisibility, @NotNull BindingTrace trace, @NotNull LookupMode lookupMode, boolean storeResult) {

        JetExpression receiverExpression = importedReference.getReceiverExpression();
        Collection<? extends DeclarationDescriptor> declarationDescriptors;
        if (receiverExpression instanceof JetQualifiedExpression) {
            declarationDescriptors = lookupDescriptorsForQualifiedExpression((JetQualifiedExpression)receiverExpression, outerScope, scopeToCheckVisibility, trace,
                                                                             lookupMode, storeResult);
        }
        else {
            assert receiverExpression instanceof JetSimpleNameExpression;
            declarationDescriptors = lookupDescriptorsForSimpleNameReference((JetSimpleNameExpression)receiverExpression, outerScope, scopeToCheckVisibility, trace,
                                                                             lookupMode, true, storeResult);
        }

        JetExpression selectorExpression = importedReference.getSelectorExpression();
        if (!(selectorExpression instanceof JetSimpleNameExpression)) {
            return Collections.emptyList();
        }

        JetSimpleNameExpression selector = (JetSimpleNameExpression)selectorExpression;
        JetSimpleNameExpression lastReference = JetPsiUtil.getLastReference(receiverExpression);
        if (lastReference == null || !canImportMembersFrom(declarationDescriptors, lastReference, trace, lookupMode)) {
            return Collections.emptyList();
        }

        return lookupSelectorDescriptors(selector, declarationDescriptors, trace, scopeToCheckVisibility, lookupMode, storeResult);
    }

    @NotNull
    private Collection<? extends DeclarationDescriptor> lookupSelectorDescriptors(@NotNull JetSimpleNameExpression selector,
            @NotNull Collection<? extends DeclarationDescriptor> declarationDescriptors, @NotNull BindingTrace trace,
            @NotNull JetScope scopeToCheckVisibility, @NotNull LookupMode lookupMode, boolean storeResult) {

        Set<SuccessfulLookupResult> results = Sets.newHashSet();
        for (DeclarationDescriptor declarationDescriptor : declarationDescriptors) {
            if (declarationDescriptor instanceof PackageViewDescriptor) {
                addResult(results, lookupSimpleNameReference(selector, ((PackageViewDescriptor)declarationDescriptor).getMemberScope(),
                                                             lookupMode, true));
            }
            if (declarationDescriptor instanceof ClassDescriptor) {
                addResult(results, lookupSimpleNameReference(selector, getAppropriateScope((ClassDescriptor)declarationDescriptor,
                                                                                           lookupMode), lookupMode, false));
                ClassDescriptor classObjectDescriptor = ((ClassDescriptor)declarationDescriptor).getClassObjectDescriptor();
                if (classObjectDescriptor != null) {
                    addResult(results, lookupSimpleNameReference(selector, getAppropriateScope(classObjectDescriptor, lookupMode),
                                                                 lookupMode, false));
                }
            }
        }
        return filterAndStoreResolutionResult(results, selector, trace, scopeToCheckVisibility, lookupMode, storeResult);
    }

    @NotNull
    private JetScope getAppropriateScope(@NotNull ClassDescriptor classDescriptor, @NotNull LookupMode lookupMode) {
        return lookupMode == LookupMode.ONLY_CLASSES ? classDescriptor.getUnsubstitutedInnerClassesScope() : classDescriptor.getDefaultType().getMemberScope();
    }

    private void addResult(@NotNull Set<SuccessfulLookupResult> results, @NotNull LookupResult result) {
        if (result == LookupResult.EMPTY) return;
        results.add((SuccessfulLookupResult)result);
    }


    @NotNull
    public Collection<? extends DeclarationDescriptor> lookupDescriptorsForSimpleNameReference(@NotNull JetSimpleNameExpression referenceExpression,
            @NotNull JetScope outerScope, @NotNull JetScope scopeToCheckVisibility, @NotNull BindingTrace trace, @NotNull LookupMode lookupMode, boolean namespaceLevel, boolean storeResult) {

        LookupResult lookupResult = lookupSimpleNameReference(referenceExpression, outerScope, lookupMode, namespaceLevel);
        if (lookupResult == LookupResult.EMPTY) return Collections.emptyList();
        return filterAndStoreResolutionResult(Collections.singletonList((SuccessfulLookupResult)lookupResult), referenceExpression, trace, scopeToCheckVisibility,
                                              lookupMode, storeResult);
    }

    @NotNull
    private LookupResult lookupSimpleNameReference(@NotNull JetSimpleNameExpression referenceExpression,
            @NotNull JetScope outerScope, @NotNull LookupMode lookupMode, boolean namespaceLevel) {

        Name referencedName = referenceExpression.getReferencedNameAsName();

        Set<DeclarationDescriptor> descriptors = Sets.newHashSet();
        PackageViewDescriptor packageDescriptor = outerScope.getPackage(referencedName);
        if (packageDescriptor != null) {
            descriptors.add(packageDescriptor);
        }

        ClassifierDescriptor classifierDescriptor = outerScope.getClassifier(referencedName);
        if (classifierDescriptor != null) {
            descriptors.add(classifierDescriptor);
        }

        if (lookupMode == LookupMode.EVERYTHING) {
            descriptors.addAll(outerScope.getFunctions(referencedName));
            descriptors.addAll(outerScope.getProperties(referencedName));

            VariableDescriptor localVariable = outerScope.getLocalVariable(referencedName);
            if (localVariable != null) {
                descriptors.add(localVariable);
            }
        }
        return new SuccessfulLookupResult(descriptors, outerScope, namespaceLevel);
    }

    @NotNull
    private Collection<? extends DeclarationDescriptor> filterAndStoreResolutionResult(@NotNull Collection<SuccessfulLookupResult> lookupResults,
            @NotNull JetSimpleNameExpression referenceExpression, @NotNull final BindingTrace trace, @NotNull JetScope scopeToCheckVisibility,
            @NotNull LookupMode lookupMode, boolean storeResult) {

        if (lookupResults.isEmpty()) {
            return Collections.emptyList();
        }
        Collection<DeclarationDescriptor> descriptors = Sets.newLinkedHashSet();
        for (SuccessfulLookupResult lookupResult : lookupResults) {
            descriptors.addAll(lookupResult.descriptors);
        }

        Collection<JetScope> possibleResolutionScopes = Lists.newArrayList();
        for (SuccessfulLookupResult lookupResult : lookupResults) {
            if (!lookupResult.descriptors.isEmpty()) {
                possibleResolutionScopes.add(lookupResult.resolutionScope);
            }
        }
        if (possibleResolutionScopes.isEmpty()) {
            for (SuccessfulLookupResult lookupResult : lookupResults) {
                possibleResolutionScopes.add(lookupResult.resolutionScope);
            }
        }

        Collection<DeclarationDescriptor> filteredDescriptors;
        if (lookupMode == LookupMode.ONLY_CLASSES) {
            filteredDescriptors = Collections2.filter(descriptors, CLASSIFIERS_AND_NAMESPACES);
        }
        else {
            filteredDescriptors = Sets.newLinkedHashSet();
            //functions and properties can be imported if lookupResult.namespaceLevel == true
            for (SuccessfulLookupResult lookupResult : lookupResults) {
                if (lookupResult.namespaceLevel) {
                    filteredDescriptors.addAll(lookupResult.descriptors);
                    continue;
                }
                filteredDescriptors.addAll(Collections2.filter(lookupResult.descriptors, CLASSIFIERS_AND_NAMESPACES));
            }
        }
        if (storeResult) {
            storeResolutionResult(descriptors, filteredDescriptors, referenceExpression, possibleResolutionScopes, trace,
                                  scopeToCheckVisibility);
        }
        return filteredDescriptors;
    }

    private void storeResolutionResult(@NotNull Collection<? extends DeclarationDescriptor> descriptors,
            @NotNull Collection<? extends DeclarationDescriptor> canBeImportedDescriptors,
            @NotNull JetSimpleNameExpression referenceExpression,
            @NotNull Collection<JetScope> possibleResolutionScopes,
            @NotNull BindingTrace trace,
            @NotNull JetScope scopeToCheckVisibility) {

        assert canBeImportedDescriptors.size() <= descriptors.size();
        assert !possibleResolutionScopes.isEmpty();
        //todo completion here needs all possible resolution scopes, if there are many
        JetScope resolutionScope = possibleResolutionScopes.iterator().next();

        // A special case - will fill all trace information
        if (resolveClassNamespaceAmbiguity(canBeImportedDescriptors, referenceExpression, resolutionScope, trace, scopeToCheckVisibility)) {
            return;
        }

        // Simple case of no descriptors
        if (descriptors.isEmpty()) {
            trace.record(BindingContext.RESOLUTION_SCOPE, referenceExpression, resolutionScope);
            trace.report(UNRESOLVED_REFERENCE.on(referenceExpression, referenceExpression));
            return;
        }

        // Decide if expression has resolved reference
        DeclarationDescriptor descriptor = null;
        if (descriptors.size() == 1) {
            descriptor = descriptors.iterator().next();
            assert canBeImportedDescriptors.size() <= 1;
        }
        else if (canBeImportedDescriptors.size() == 1) {
            descriptor = canBeImportedDescriptors.iterator().next();
        }
        if (descriptor != null) {
            trace.record(BindingContext.REFERENCE_TARGET, referenceExpression, descriptors.iterator().next());
            trace.record(BindingContext.RESOLUTION_SCOPE, referenceExpression, resolutionScope);

            if (descriptor instanceof DeclarationDescriptorWithVisibility) {
                checkVisibility((DeclarationDescriptorWithVisibility)descriptor, trace, referenceExpression, scopeToCheckVisibility);
            }
        }

        // Check for more information and additional errors
        if (canBeImportedDescriptors.isEmpty()) {
            assert descriptors.size() >= 1;
            trace.report(CANNOT_BE_IMPORTED.on(referenceExpression, descriptors.iterator().next()));
            return;
        }
        if (canBeImportedDescriptors.size() > 1) {
            trace.record(BindingContext.AMBIGUOUS_REFERENCE_TARGET, referenceExpression, descriptors);
        }
    }

    /**
     * This method tries to resolve descriptors ambiguity between class descriptor and namespace descriptor for the same class.
     * It's ok choose class for expression reference resolution.
     *
     * @return <code>true</code> if method has successfully resolved ambiguity
     */
    private boolean resolveClassNamespaceAmbiguity(@NotNull Collection<? extends DeclarationDescriptor> filteredDescriptors,
        @NotNull JetSimpleNameExpression referenceExpression, @NotNull JetScope resolutionScope, @NotNull BindingTrace trace,
        @NotNull JetScope scopeToCheckVisibility) {

        if (filteredDescriptors.size() == 2) {
            PackageViewDescriptor packageView = null;
            ClassDescriptor classDescriptor = null;

            for (DeclarationDescriptor filteredDescriptor : filteredDescriptors) {
                if (filteredDescriptor instanceof PackageViewDescriptor) {
                    packageView = (PackageViewDescriptor)filteredDescriptor;
                }
                else if (filteredDescriptor instanceof ClassDescriptor) {
                    classDescriptor = (ClassDescriptor)filteredDescriptor;
                }
            }

            if (packageView != null && classDescriptor != null) {
                if (packageView.getFqName().equalsTo(DescriptorUtils.getFqName(classDescriptor))) {
                    trace.record(BindingContext.REFERENCE_TARGET, referenceExpression, classDescriptor);
                    trace.record(BindingContext.RESOLUTION_SCOPE, referenceExpression, resolutionScope);
                    checkVisibility(classDescriptor, trace, referenceExpression, scopeToCheckVisibility);
                    return true;
                }
            }
        }

        return false;
    }

    private void checkVisibility(@NotNull DeclarationDescriptorWithVisibility descriptor, @NotNull BindingTrace trace,
            @NotNull JetSimpleNameExpression referenceExpression, @NotNull JetScope scopeToCheckVisibility) {

        if (!Visibilities.isVisible(descriptor, scopeToCheckVisibility.getContainingDeclaration())) {
            trace.report(INVISIBLE_REFERENCE.on(referenceExpression, descriptor, descriptor.getVisibility(), descriptor.getContainingDeclaration()));
        }
    }

    private interface LookupResult {
        LookupResult EMPTY = new LookupResult() {
        };
    }

    private static class SuccessfulLookupResult implements LookupResult {
        final Collection<? extends DeclarationDescriptor> descriptors;
        final JetScope resolutionScope;
        final boolean namespaceLevel;

        private SuccessfulLookupResult(Collection<? extends DeclarationDescriptor> descriptors,
                                       JetScope resolutionScope,
                                       boolean namespaceLevel) {
            this.descriptors = descriptors;
            this.resolutionScope = resolutionScope;
            this.namespaceLevel = namespaceLevel;
        }
    }
}
