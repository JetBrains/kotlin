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

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.Mutable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.diagnostics.Errors;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.resolve.scopes.AbstractScopeAdapter;
import org.jetbrains.kotlin.resolve.scopes.JetScope;
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import static org.jetbrains.kotlin.diagnostics.Errors.*;

public class QualifiedExpressionResolver {
    private static final Predicate<DeclarationDescriptor> CLASSIFIERS_AND_PACKAGE_VIEWS = new Predicate<DeclarationDescriptor>() {
        @Override
        public boolean apply(@Nullable DeclarationDescriptor descriptor) {
            return descriptor instanceof ClassifierDescriptor || descriptor instanceof PackageViewDescriptor;
        }
    };

    public enum LookupMode {
        // Only classifier and packages are resolved
        ONLY_CLASSES_AND_PACKAGES,

        // Resolve all descriptors
        EVERYTHING
    }

    public static boolean canAllUnderImportFrom(@NotNull Collection<DeclarationDescriptor> descriptors) {
        if (descriptors.isEmpty()) {
            return true;
        }
        for (DeclarationDescriptor descriptor : descriptors) {
            if (!(descriptor instanceof ClassDescriptor)) {
                return true;
            }
            if (canAllUnderImportFromClass((ClassDescriptor) descriptor)) {
                return true;
            }
        }
        return false;
    }

    public static boolean canAllUnderImportFromClass(@NotNull ClassDescriptor descriptor) {
        return !descriptor.getKind().isSingleton();
    }

    @NotNull
    public Collection<DeclarationDescriptor> processImportReference(
            @NotNull JetImportDirective importDirective,
            @NotNull JetScope scope,
            @NotNull JetScope scopeToCheckVisibility,
            @Nullable Importer importer,
            @NotNull BindingTrace trace,
            @NotNull LookupMode lookupMode
    ) {
        if (importDirective.isAbsoluteInRootPackage()) {
            trace.report(UNSUPPORTED.on(importDirective, "TypeHierarchyResolver")); // TODO
            return Collections.emptyList();
        }
        JetExpression importedReference = importDirective.getImportedReference();
        if (importedReference == null) {
            return Collections.emptyList();
        }

        Collection<DeclarationDescriptor> descriptors;
        if (importedReference instanceof JetQualifiedExpression) {
            //store result only when we find all descriptors, not only classes on the second phase
            descriptors = lookupDescriptorsForQualifiedExpression(
                    (JetQualifiedExpression) importedReference, scope, scopeToCheckVisibility, trace,
                    lookupMode, lookupMode == LookupMode.EVERYTHING);
        }
        else {
            assert importedReference instanceof JetSimpleNameExpression;
            descriptors = lookupDescriptorsForSimpleNameReference(
                    (JetSimpleNameExpression) importedReference, scope, scopeToCheckVisibility, trace,
                    lookupMode, true, lookupMode == LookupMode.EVERYTHING);
        }

        JetSimpleNameExpression referenceExpression = JetPsiUtil.getLastReference(importedReference);
        if (importDirective.isAllUnder()) {
            if (!canAllUnderImportFrom(descriptors) && referenceExpression != null) {
                trace.report(CANNOT_IMPORT_ON_DEMAND_FROM_SINGLETON.on(referenceExpression, (ClassDescriptor) descriptors.iterator().next()));
            }

            if (referenceExpression == null || !canImportMembersFrom(descriptors, referenceExpression, trace, lookupMode)) {
                return Collections.emptyList();
            }

            if (importer != null) {
                for (DeclarationDescriptor descriptor : descriptors) {
                    importer.addAllUnderImport(descriptor);
                }
            }
            return Collections.emptyList();
        }

        Name aliasName = JetPsiUtil.getAliasName(importDirective);
        if (aliasName == null) {
            return Collections.emptyList();
        }

        if (importer != null) {
            for (DeclarationDescriptor descriptor : descriptors) {
                importer.addAliasImport(descriptor, aliasName);
            }
        }

        return descriptors;
    }

    private static boolean canImportMembersFrom(
            @NotNull Collection<DeclarationDescriptor> descriptors,
            @NotNull JetSimpleNameExpression reference,
            @NotNull BindingTrace trace,
            @NotNull LookupMode lookupMode
    ) {
        if (lookupMode == LookupMode.ONLY_CLASSES_AND_PACKAGES) {
            return true;
        }

        if (descriptors.size() == 1) {
            return canImportMembersFrom(descriptors.iterator().next(), reference, trace, lookupMode);
        }

        TemporaryBindingTrace temporaryTrace =
                TemporaryBindingTrace.create(trace, "trace to find out if members can be imported from", reference);
        boolean canImport = false;
        for (DeclarationDescriptor descriptor : descriptors) {
            canImport |= canImportMembersFrom(descriptor, reference, temporaryTrace, lookupMode);
        }
        if (!canImport) {
            temporaryTrace.commit();
        }
        return canImport;
    }

    private static boolean canImportMembersFrom(
            @NotNull DeclarationDescriptor descriptor,
            @NotNull JetSimpleNameExpression reference,
            @NotNull BindingTrace trace,
            @NotNull LookupMode lookupMode
    ) {
        assert lookupMode == LookupMode.EVERYTHING;
        if (descriptor instanceof PackageViewDescriptor) {
            return true;
        }
        if (descriptor instanceof ClassDescriptor) {
            return true;
        }
        trace.report(CANNOT_IMPORT_FROM_ELEMENT.on(reference, descriptor));
        return false;
    }

    @NotNull
    public Collection<DeclarationDescriptor> lookupDescriptorsForUserType(
            @NotNull JetUserType userType,
            @NotNull JetScope outerScope,
            @NotNull BindingTrace trace,
            boolean onlyClassifiers
    ) {

        if (userType.isAbsoluteInRootPackage()) {
            trace.report(Errors.UNSUPPORTED.on(userType, "package"));
            return Collections.emptyList();
        }

        JetSimpleNameExpression referenceExpression = userType.getReferenceExpression();
        if (referenceExpression == null) {
            return Collections.emptyList();
        }
        JetUserType qualifier = userType.getQualifier();

        // We do not want to resolve the last segment of a user type to a package
        JetScope filteredScope = filterOutPackagesIfNeeded(outerScope, onlyClassifiers);

        if (qualifier == null) {
            return lookupDescriptorsForSimpleNameReference(referenceExpression, filteredScope, outerScope, trace, LookupMode.ONLY_CLASSES_AND_PACKAGES,
                                                           false, true);
        }
        Collection<DeclarationDescriptor> declarationDescriptors = lookupDescriptorsForUserType(qualifier, outerScope, trace, false);
        return lookupSelectorDescriptors(referenceExpression, declarationDescriptors, trace, filteredScope, LookupMode.ONLY_CLASSES_AND_PACKAGES, true);
    }

    private static JetScope filterOutPackagesIfNeeded(final JetScope outerScope, boolean noPackages) {
        return !noPackages ? outerScope : new AbstractScopeAdapter() {

                    @NotNull
                    @Override
                    protected JetScope getWorkerScope() {
                        return outerScope;
                    }

                    @Nullable
                    @Override
                    public PackageViewDescriptor getPackage(@NotNull Name name) {
                        return null;
                    }
        };
    }

    @NotNull
    public Collection<DeclarationDescriptor> lookupDescriptorsForQualifiedExpression(
            @NotNull JetQualifiedExpression importedReference,
            @NotNull JetScope outerScope,
            @NotNull JetScope scopeToCheckVisibility,
            @NotNull BindingTrace trace,
            @NotNull LookupMode lookupMode,
            boolean storeResult
    ) {
        JetExpression receiverExpression = importedReference.getReceiverExpression();
        Collection<DeclarationDescriptor> declarationDescriptors;
        if (receiverExpression instanceof JetQualifiedExpression) {
            declarationDescriptors =
                    lookupDescriptorsForQualifiedExpression((JetQualifiedExpression) receiverExpression, outerScope, scopeToCheckVisibility,
                                                            trace, lookupMode, storeResult);
        }
        else {
            assert receiverExpression instanceof JetSimpleNameExpression;
            declarationDescriptors =
                    lookupDescriptorsForSimpleNameReference((JetSimpleNameExpression) receiverExpression, outerScope,
                                                            scopeToCheckVisibility, trace, lookupMode, true, storeResult);
        }

        JetExpression selectorExpression = importedReference.getSelectorExpression();
        if (!(selectorExpression instanceof JetSimpleNameExpression)) {
            return Collections.emptyList();
        }

        JetSimpleNameExpression selector = (JetSimpleNameExpression) selectorExpression;
        JetSimpleNameExpression lastReference = JetPsiUtil.getLastReference(receiverExpression);
        if (lastReference == null || !canImportMembersFrom(declarationDescriptors, lastReference, trace, lookupMode)) {
            return Collections.emptyList();
        }

        return lookupSelectorDescriptors(selector, declarationDescriptors, trace, scopeToCheckVisibility, lookupMode, storeResult);
    }

    @NotNull
    private static Collection<DeclarationDescriptor> lookupSelectorDescriptors(
            @NotNull JetSimpleNameExpression selector,
            @NotNull Collection<DeclarationDescriptor> declarationDescriptors,
            @NotNull BindingTrace trace,
            @NotNull JetScope scopeToCheckVisibility,
            @NotNull LookupMode lookupMode,
            boolean storeResult
    ) {
        Set<LookupResult> results = Sets.newLinkedHashSet();
        for (DeclarationDescriptor declarationDescriptor : declarationDescriptors) {
            if (declarationDescriptor instanceof PackageViewDescriptor) {
                results.add(lookupSimpleNameReference(selector, ((PackageViewDescriptor) declarationDescriptor).getMemberScope(),
                                                      lookupMode, true));
            }
            if (declarationDescriptor instanceof ClassDescriptor) {
                addResultsForClass(results, selector, lookupMode, (ClassDescriptor) declarationDescriptor);
            }
        }
        return filterAndStoreResolutionResult(results, selector, trace, scopeToCheckVisibility, lookupMode, storeResult);
    }

    private static void addResultsForClass(
            @NotNull @Mutable Set<LookupResult> results,
            @NotNull JetSimpleNameExpression selector,
            @NotNull LookupMode lookupMode,
            @NotNull ClassDescriptor descriptor
    ) {
        JetScope scope = lookupMode == LookupMode.ONLY_CLASSES_AND_PACKAGES
                         ? descriptor.getUnsubstitutedInnerClassesScope()
                         : descriptor.getDefaultType().getMemberScope();
        results.add(lookupSimpleNameReference(selector, scope, lookupMode, false));

        results.add(lookupSimpleNameReference(selector, descriptor.getStaticScope(), lookupMode, true));

        ClassDescriptor classObject = descriptor.getDefaultObjectDescriptor();
        if (classObject != null) {
            addResultsForClass(results, selector, lookupMode, classObject);
        }
    }


    @NotNull
    @SuppressWarnings("MethodMayBeStatic")
    public Collection<DeclarationDescriptor> lookupDescriptorsForSimpleNameReference(
            @NotNull JetSimpleNameExpression referenceExpression,
            @NotNull JetScope outerScope,
            @NotNull JetScope scopeToCheckVisibility,
            @NotNull BindingTrace trace,
            @NotNull LookupMode lookupMode,
            boolean packageLevel,
            boolean storeResult
    ) {
        LookupResult lookupResult = lookupSimpleNameReference(referenceExpression, outerScope, lookupMode, packageLevel);
        return filterAndStoreResolutionResult(Collections.singletonList(lookupResult), referenceExpression, trace, scopeToCheckVisibility,
                                              lookupMode, storeResult);
    }

    @NotNull
    private static LookupResult lookupSimpleNameReference(
            @NotNull JetSimpleNameExpression referenceExpression,
            @NotNull JetScope outerScope,
            @NotNull LookupMode lookupMode,
            boolean packageLevel
    ) {
        Name referencedName = referenceExpression.getReferencedNameAsName();

        Collection<DeclarationDescriptor> descriptors = Sets.newLinkedHashSet();
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

        return new LookupResult(descriptors, outerScope, packageLevel);
    }

    @NotNull
    private static Collection<DeclarationDescriptor> filterAndStoreResolutionResult(
            @NotNull Collection<LookupResult> lookupResults,
            @NotNull JetSimpleNameExpression referenceExpression,
            @NotNull BindingTrace trace,
            @NotNull JetScope scopeToCheckVisibility,
            @NotNull LookupMode lookupMode,
            boolean storeResult
    ) {
        if (lookupResults.isEmpty()) {
            return Collections.emptyList();
        }

        Collection<DeclarationDescriptor> descriptors = Sets.newLinkedHashSet();
        for (LookupResult lookupResult : lookupResults) {
            descriptors.addAll(lookupResult.descriptors);
        }

        Collection<JetScope> possibleResolutionScopes = Lists.newArrayList();
        for (LookupResult lookupResult : lookupResults) {
            if (!lookupResult.descriptors.isEmpty()) {
                possibleResolutionScopes.add(lookupResult.resolutionScope);
            }
        }
        if (possibleResolutionScopes.isEmpty()) {
            for (LookupResult lookupResult : lookupResults) {
                possibleResolutionScopes.add(lookupResult.resolutionScope);
            }
        }

        Collection<DeclarationDescriptor> filteredDescriptors;
        if (lookupMode == LookupMode.ONLY_CLASSES_AND_PACKAGES) {
            filteredDescriptors = Collections2.filter(descriptors, CLASSIFIERS_AND_PACKAGE_VIEWS);
        }
        else {
            filteredDescriptors = Sets.newLinkedHashSet();
            //functions and properties can be imported if lookupResult.packageLevel == true
            for (LookupResult lookupResult : lookupResults) {
                if (lookupResult.packageLevel) {
                    filteredDescriptors.addAll(lookupResult.descriptors);
                }
                else {
                    filteredDescriptors.addAll(Collections2.filter(lookupResult.descriptors, CLASSIFIERS_AND_PACKAGE_VIEWS));
                }
            }
        }

        if (storeResult) {
            storeResolutionResult(descriptors, filteredDescriptors, referenceExpression, possibleResolutionScopes, trace,
                                  scopeToCheckVisibility);
        }

        return filteredDescriptors;
    }

    private static void storeResolutionResult(
            @NotNull Collection<DeclarationDescriptor> descriptors,
            @NotNull Collection<DeclarationDescriptor> canBeImportedDescriptors,
            @NotNull JetSimpleNameExpression referenceExpression,
            @NotNull Collection<JetScope> possibleResolutionScopes,
            @NotNull BindingTrace trace,
            @NotNull JetScope scopeToCheckVisibility
    ) {
        assert canBeImportedDescriptors.size() <= descriptors.size();
        assert !possibleResolutionScopes.isEmpty();
        //todo completion here needs all possible resolution scopes, if there are many
        JetScope resolutionScope = possibleResolutionScopes.iterator().next();

        // A special case - will fill all trace information
        if (resolveClassPackageAmbiguity(canBeImportedDescriptors, referenceExpression, resolutionScope, trace, scopeToCheckVisibility)) {
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
                checkVisibility((DeclarationDescriptorWithVisibility) descriptor, trace, referenceExpression, scopeToCheckVisibility);
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
     * This method tries to resolve descriptors ambiguity between class descriptor and package descriptor for the same class.
     * It's ok choose class for expression reference resolution.
     *
     * @return <code>true</code> if method has successfully resolved ambiguity
     */
    private static boolean resolveClassPackageAmbiguity(
            @NotNull Collection<DeclarationDescriptor> filteredDescriptors,
            @NotNull JetSimpleNameExpression referenceExpression,
            @NotNull JetScope resolutionScope,
            @NotNull BindingTrace trace,
            @NotNull JetScope scopeToCheckVisibility
    ) {
        if (filteredDescriptors.size() == 2) {
            PackageViewDescriptor packageView = null;
            ClassDescriptor classDescriptor = null;

            for (DeclarationDescriptor filteredDescriptor : filteredDescriptors) {
                if (filteredDescriptor instanceof PackageViewDescriptor) {
                    packageView = (PackageViewDescriptor) filteredDescriptor;
                }
                else if (filteredDescriptor instanceof ClassDescriptor) {
                    classDescriptor = (ClassDescriptor) filteredDescriptor;
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

    private static void checkVisibility(
            @NotNull DeclarationDescriptorWithVisibility descriptor,
            @NotNull BindingTrace trace,
            @NotNull JetSimpleNameExpression referenceExpression,
            @NotNull JetScope scopeToCheckVisibility
    ) {
        if (!Visibilities.isVisible(ReceiverValue.IRRELEVANT_RECEIVER, descriptor, scopeToCheckVisibility.getContainingDeclaration())) {
            Visibility visibility = descriptor.getVisibility();
            if (PsiTreeUtil.getParentOfType(referenceExpression, JetImportDirective.class) != null && !visibility.mustCheckInImports()) return;
            //noinspection ConstantConditions
            trace.report(INVISIBLE_REFERENCE.on(referenceExpression, descriptor, visibility, descriptor.getContainingDeclaration()));
        }
    }

    private static class LookupResult {
        private final Collection<DeclarationDescriptor> descriptors;
        private final JetScope resolutionScope;
        private final boolean packageLevel;

        public LookupResult(
                @NotNull Collection<DeclarationDescriptor> descriptors,
                @NotNull JetScope resolutionScope,
                boolean packageLevel
        ) {
            this.descriptors = descriptors;
            this.resolutionScope = resolutionScope;
            this.packageLevel = packageLevel;
        }
    }
}
