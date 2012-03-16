/*
 * Copyright 2010-2012 JetBrains s.r.o.
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
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.ModuleConfiguration;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.resolve.scopes.WritableScope;

import javax.inject.Inject;
import java.util.*;

import static org.jetbrains.jet.lang.diagnostics.Errors.*;

/**
 * @author abreslav
 * @author svtk
 */
public class ImportsResolver {
    @NotNull
    private TopDownAnalysisContext context;
    @NotNull
    private ModuleConfiguration configuration;
    @NotNull
    private SingleImportResolver singleImportResolver;

    @Inject
    public void setContext(@NotNull TopDownAnalysisContext context) {
        this.context = context;
    }

    @Inject
    public void setConfiguration(@NotNull ModuleConfiguration configuration) {
        this.configuration = configuration;
    }

    public ImportsResolver() {
        this.singleImportResolver = new SingleImportResolver();
    }

    public void processTypeImports() {
        processImports(true);
    }

    public void processMembersImports() {
        processImports(false);
    }

    // On first phase all classes and objects are imported,
    // on second phase previous imports are thrown and everything (including functions and properties at namespace level) is imported
    private void processImports(boolean firstPhase) {
        TemporaryBindingTrace temporaryTrace = TemporaryBindingTrace.create(context.getTrace()); //not to trace errors of default imports
        for (JetFile file : context.getNamespaceDescriptors().keySet()) {
            WritableScope namespaceScope = context.getNamespaceScopes().get(file);
            Importer.DelayedImporter delayedImporter = new Importer.DelayedImporter(namespaceScope, firstPhase);
            if (!firstPhase) {
                namespaceScope.clearImports();
            }
            Map<JetImportDirective, DeclarationDescriptor> resolvedDirectives = Maps.newHashMap();
            Collection<JetImportDirective> defaultImportDirectives = Lists.newArrayList();
            configuration.addDefaultImports(namespaceScope, defaultImportDirectives);
            for (JetImportDirective defaultImportDirective : defaultImportDirectives) {
                singleImportResolver.processImportReference(defaultImportDirective, namespaceScope, delayedImporter, temporaryTrace, firstPhase);
            }

            List<JetImportDirective> importDirectives = file.getImportDirectives();
            for (JetImportDirective importDirective : importDirectives) {
                Collection<? extends DeclarationDescriptor> descriptors = singleImportResolver.processImportReference(importDirective, namespaceScope, delayedImporter, context.getTrace(), firstPhase);
                if (descriptors.size() == 1) {
                    resolvedDirectives.put(importDirective, descriptors.iterator().next());
                }
            }
            delayedImporter.processImports();

            if (firstPhase) {
                continue;
            }
            for (JetImportDirective importDirective : importDirectives) {
                reportUselessImport(importDirective, namespaceScope, resolvedDirectives);
            }
        }
    }

    @Nullable
    private static JetSimpleNameExpression getLastReference(@NotNull JetExpression importedReference) {
        if (importedReference instanceof JetDotQualifiedExpression) {
            JetDotQualifiedExpression reference = (JetDotQualifiedExpression) importedReference;
            JetExpression selectorExpression = reference.getSelectorExpression();
            return (selectorExpression != null) ? (JetSimpleNameExpression) selectorExpression : null;
        }
        assert importedReference instanceof JetSimpleNameExpression;
        return (JetSimpleNameExpression) importedReference;
    }


    @Nullable
    private static String getAliasName(@NotNull JetImportDirective importDirective) {
        String aliasName = importDirective.getAliasName();
        JetExpression importedReference = importDirective.getImportedReference();
        if (importedReference == null) {
            return null;
        }
        JetSimpleNameExpression referenceExpression = getLastReference(importedReference);
        if (aliasName == null) {
            aliasName = referenceExpression != null ? referenceExpression.getReferencedName() : null;
        }
        return aliasName;
    }

    private void reportUselessImport(@NotNull JetImportDirective importDirective,
                                     @NotNull WritableScope namespaceScope,
                                     @NotNull Map<JetImportDirective, DeclarationDescriptor> resolvedDirectives) {

        JetExpression importedReference = importDirective.getImportedReference();
        if (importedReference == null || !resolvedDirectives.containsKey(importDirective)) {
            return;
        }
        String aliasName = getAliasName(importDirective);
        if (aliasName == null) {
            return;
        }

        DeclarationDescriptor wasResolved = resolvedDirectives.get(importDirective);
        DeclarationDescriptor isResolved = null;
        if (wasResolved instanceof ClassDescriptor) {
            isResolved = namespaceScope.getClassifier(aliasName);
        }
        else if (wasResolved instanceof VariableDescriptor) {
            isResolved = namespaceScope.getLocalVariable(aliasName);
        }
        else if (wasResolved instanceof NamespaceDescriptor) {
            isResolved = namespaceScope.getNamespace(aliasName);
        }
        if (isResolved != null && isResolved != wasResolved) {
            context.getTrace().report(USELESS_HIDDEN_IMPORT.on(importedReference));
        }
        if (!importDirective.isAllUnder() && importedReference instanceof JetSimpleNameExpression && importDirective.getAliasName() == null) {
            context.getTrace().report(USELESS_SIMPLE_IMPORT.on(importedReference));
        }
    }

    public static Collection<? extends DeclarationDescriptor> analyseImportReference(
            @NotNull JetImportDirective importDirective,
            @NotNull JetScope scope, @NotNull BindingTrace trace
    ) {
        ImportsResolver.SingleImportResolver importResolver = new ImportsResolver.SingleImportResolver();
        return importResolver.processImportReference(importDirective, scope, Importer.DO_NOTHING, trace, false);
    }

    private static class SingleImportResolver {
        @NotNull
        public Collection<? extends DeclarationDescriptor> processImportReference(@NotNull JetImportDirective importDirective,
                                                                                  @NotNull JetScope scope,
                                                                                  @NotNull Importer importer,
                                                                                  @NotNull BindingTrace trace,
                                                                                  boolean firstPhase) {
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
                descriptors = lookupDescriptorsForQualifiedExpression((JetQualifiedExpression) importedReference, scope, trace, firstPhase);
            }
            else {
                assert importedReference instanceof JetSimpleNameExpression;
                descriptors = lookupDescriptorsForSimpleNameReference((JetSimpleNameExpression) importedReference, scope, trace, firstPhase, true);
            }

            JetSimpleNameExpression referenceExpression = getLastReference(importedReference);
            if (importDirective.isAllUnder()) {
                if (referenceExpression == null || !canImportMembersFrom(descriptors, referenceExpression, trace,firstPhase)) {
                    return Collections.emptyList();
                }

                for (DeclarationDescriptor descriptor : descriptors) {
                    importer.addAllUnderImport(descriptor);
                }
                return Collections.emptyList();
            }

            String aliasName = getAliasName(importDirective);
            if (aliasName == null) {
                return Collections.emptyList();
            }

            for (DeclarationDescriptor descriptor : descriptors) {
                importer.addAliasImport(descriptor, aliasName);
            }

            return descriptors;
        }

        @NotNull
        private Collection<? extends DeclarationDescriptor> lookupDescriptorsForQualifiedExpression(
                @NotNull JetQualifiedExpression importedReference,
                @NotNull JetScope outerScope,
                @NotNull BindingTrace trace,
                boolean firstPhase) {

            JetExpression receiverExpression = importedReference.getReceiverExpression();
            Collection<? extends DeclarationDescriptor> declarationDescriptors;
            if (receiverExpression instanceof JetQualifiedExpression) {
                declarationDescriptors = lookupDescriptorsForQualifiedExpression((JetQualifiedExpression) receiverExpression, outerScope, trace, firstPhase);
            }
            else {
                assert receiverExpression instanceof JetSimpleNameExpression;
                declarationDescriptors = lookupDescriptorsForSimpleNameReference((JetSimpleNameExpression) receiverExpression, outerScope, trace, firstPhase, true);
            }

            JetExpression selectorExpression = importedReference.getSelectorExpression();

            if (!(selectorExpression instanceof JetSimpleNameExpression)) return Collections.emptyList();

            JetSimpleNameExpression selector = (JetSimpleNameExpression) selectorExpression;
            JetSimpleNameExpression lastReference = getLastReference(receiverExpression);
            if (lastReference == null || !canImportMembersFrom(declarationDescriptors, lastReference, trace, firstPhase)) {
                return Collections.emptyList();
            }

            Set<SuccessfulLookupResult> results = Sets.newHashSet();
            for (DeclarationDescriptor declarationDescriptor : declarationDescriptors) {
                if (declarationDescriptor instanceof NamespaceDescriptor) {
                    addResult(results, lookupSimpleNameReference(selector, ((NamespaceDescriptor) declarationDescriptor).getMemberScope(), firstPhase, true));
                }
                if (declarationDescriptor instanceof ClassDescriptor) {
                    addResult(results, lookupSimpleNameReference(selector, getAppropriateScope((ClassDescriptor) declarationDescriptor, firstPhase), firstPhase, false));
                    ClassDescriptor classObjectDescriptor = ((ClassDescriptor) declarationDescriptor).getClassObjectDescriptor();
                    if (classObjectDescriptor != null) {
                        addResult(results, lookupSimpleNameReference(selector, getAppropriateScope(classObjectDescriptor, firstPhase), firstPhase, false));
                    }
                }
            }
            if (results.isEmpty()) {
                return Collections.emptyList();
            }
            return filterAndStoreResolutionResult(results, selector, trace, firstPhase);
        }

        private static JetScope getAppropriateScope(ClassDescriptor classDescriptor, boolean firstPhase) {
            return firstPhase ? classDescriptor.getUnsubstitutedInnerClassesScope() : classDescriptor.getDefaultType().getMemberScope();
        }

        private static void addResult(Set<SuccessfulLookupResult> results, LookupResult result) {
            if (result == LookupResult.EMPTY) return;
            results.add((SuccessfulLookupResult) result);
        }

        private boolean canImportMembersFrom(@NotNull Collection<? extends DeclarationDescriptor> descriptors,
                                             @NotNull JetSimpleNameExpression reference,
                                             @NotNull BindingTrace trace,
                                             boolean firstPhase) {
            if (firstPhase) {
                return true;
            }
            if (descriptors.size() == 1) {
                return canImportMembersFrom(descriptors.iterator().next(), reference, trace, firstPhase);
            }
            TemporaryBindingTrace temporaryTrace = TemporaryBindingTrace.create(trace);
            boolean canImport = false;
            for (DeclarationDescriptor descriptor : descriptors) {
                canImport |= canImportMembersFrom(descriptor, reference, temporaryTrace, firstPhase);
            }
            if (!canImport) {
                temporaryTrace.commit();
            }
            return canImport;
        }

        private boolean canImportMembersFrom(@NotNull DeclarationDescriptor descriptor,
                                             @NotNull JetSimpleNameExpression reference,
                                             @NotNull BindingTrace trace,
                                             boolean firstPhase) {
            assert !firstPhase;
            if (descriptor instanceof NamespaceDescriptor) {
                return true;
            }
            if (descriptor instanceof ClassDescriptor && ((ClassDescriptor) descriptor).getKind() != ClassKind.OBJECT) {
                return true;
            }
            trace.report(CANNOT_IMPORT_FROM_ELEMENT.on(reference, descriptor));
            return false;
        }

        @NotNull
        private Collection<? extends DeclarationDescriptor> lookupDescriptorsForSimpleNameReference(
                @NotNull JetSimpleNameExpression referenceExpression,
                @NotNull JetScope outerScope,
                @NotNull BindingTrace trace,
                boolean firstPhase,
                boolean namespaceLevel) {
            LookupResult lookupResult = lookupSimpleNameReference(referenceExpression, outerScope, firstPhase, namespaceLevel);
            if (lookupResult == LookupResult.EMPTY) return Collections.emptyList();
            return filterAndStoreResolutionResult(Collections.singletonList((SuccessfulLookupResult) lookupResult), referenceExpression, trace, firstPhase);
        }

        private LookupResult lookupSimpleNameReference(
                @NotNull JetSimpleNameExpression referenceExpression,
                @NotNull JetScope outerScope,
                boolean firstPhase,
                boolean namespaceLevel) {

            String referencedName = referenceExpression.getReferencedName();
            if (referencedName == null) {
                //to store a scope where we tried to resolve this reference
                return new SuccessfulLookupResult(Collections.<DeclarationDescriptor>emptyList(), outerScope, namespaceLevel);
            }

            Set<DeclarationDescriptor> descriptors = Sets.newHashSet();
            NamespaceDescriptor namespaceDescriptor = outerScope.getNamespace(referencedName);
            if (namespaceDescriptor != null) {
                descriptors.add(namespaceDescriptor);
            }

            ClassifierDescriptor classifierDescriptor = outerScope.getClassifier(referencedName);
            if (classifierDescriptor != null) {
                descriptors.add(classifierDescriptor);
            }

            if (firstPhase) {
                descriptors.add(outerScope.getObjectDescriptor(referencedName));
            }
            else {
                descriptors.addAll(outerScope.getFunctions(referencedName));
                descriptors.addAll(outerScope.getProperties(referencedName));

                VariableDescriptor localVariable = outerScope.getLocalVariable(referencedName);
                if (localVariable != null) {
                    descriptors.add(localVariable);
                }
            }
            return new SuccessfulLookupResult(descriptors, outerScope, namespaceLevel);
        }

        private Collection<? extends DeclarationDescriptor> filterAndStoreResolutionResult(
                @NotNull Collection<SuccessfulLookupResult> lookupResults,
                @NotNull JetSimpleNameExpression referenceExpression,
                @NotNull BindingTrace trace,
                boolean firstPhase) {
                //functions and properties can be imported if lookupResult.namespaceLevel == true
            Collection<DeclarationDescriptor> descriptors = Sets.newLinkedHashSet();
            for (SuccessfulLookupResult lookupResult : lookupResults) {
                descriptors.addAll(lookupResult.descriptors);
            }
            if (firstPhase) {
                return Collections2.filter(descriptors, new Predicate<DeclarationDescriptor>() {
                    @Override
                    public boolean apply(@Nullable DeclarationDescriptor descriptor) {
                        return (descriptor instanceof ClassDescriptor) ||
                               (descriptor instanceof NamespaceDescriptor);
                    }
                });
            }
            Collection<DeclarationDescriptor> filteredDescriptors = Sets.newLinkedHashSet();
            for (SuccessfulLookupResult lookupResult : lookupResults) {
                if (lookupResult.namespaceLevel) {
                    filteredDescriptors.addAll(lookupResult.descriptors);
                    continue;
                }
                filteredDescriptors.addAll(Collections2.filter(lookupResult.descriptors, new Predicate<DeclarationDescriptor>() {
                    @Override
                    public boolean apply(@Nullable DeclarationDescriptor descriptor) {
                        return (descriptor instanceof NamespaceDescriptor) ||
                               (descriptor instanceof ClassDescriptor) ||
                               (descriptor instanceof VariableDescriptor && ((VariableDescriptor) descriptor).isObjectDeclaration());
                    }
                }));
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
            storeResolutionResult(descriptors, filteredDescriptors, referenceExpression, possibleResolutionScopes, trace);
            return filteredDescriptors;
        }

        private void storeResolutionResult(
                @NotNull Collection<? extends DeclarationDescriptor> descriptors,
                @NotNull Collection<? extends DeclarationDescriptor> canBeImportedDescriptors,
                @NotNull JetSimpleNameExpression referenceExpression,
                @NotNull Collection<JetScope> possibleResolutionScopes,
                @NotNull BindingTrace trace) {

            assert canBeImportedDescriptors.size() <= descriptors.size();
            assert !possibleResolutionScopes.isEmpty();
            //todo completion here needs all possible resolution scopes, if there are many
            JetScope resolutionScope = possibleResolutionScopes.iterator().next();

            // A special case - will fill all trace information
            if (resolveClassNamespaceAmbiguity(canBeImportedDescriptors, referenceExpression, resolutionScope, trace)) {
                return;
            }

            // Simple case of no descriptors
            if (descriptors.isEmpty()) {
                trace.record(BindingContext.RESOLUTION_SCOPE, referenceExpression, resolutionScope);
                trace.report(UNRESOLVED_REFERENCE.on(referenceExpression));
                return;
            }

            // Decide if expression has resolved reference
            if (descriptors.size() == 1) {
                assert canBeImportedDescriptors.size() <= 1;
                trace.record(BindingContext.REFERENCE_TARGET, referenceExpression, descriptors.iterator().next());
                trace.record(BindingContext.RESOLUTION_SCOPE, referenceExpression, resolutionScope);
            }
            else if (canBeImportedDescriptors.size() == 1) {
                trace.record(BindingContext.REFERENCE_TARGET, referenceExpression, canBeImportedDescriptors.iterator().next());
                trace.record(BindingContext.RESOLUTION_SCOPE, referenceExpression, resolutionScope);
            }

            // Check for more information and additional errors
            if (canBeImportedDescriptors.isEmpty()) {
                assert descriptors.size() >= 1;
                trace.report(CANNOT_BE_IMPORTED.on(referenceExpression, descriptors.iterator().next()));
            }
            else if (canBeImportedDescriptors.size() > 1) {
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
                                                       @NotNull JetSimpleNameExpression referenceExpression,
                                                       @NotNull JetScope resolutionScope,
                                                       @NotNull BindingTrace trace) {

            if (filteredDescriptors.size() == 2) {
                NamespaceDescriptor namespaceDescriptor = null;
                ClassDescriptor classDescriptor = null;

                for (DeclarationDescriptor filteredDescriptor : filteredDescriptors) {
                    if (filteredDescriptor instanceof NamespaceDescriptor) {
                        namespaceDescriptor = (NamespaceDescriptor) filteredDescriptor;
                    }
                    else if (filteredDescriptor instanceof ClassDescriptor) {
                        classDescriptor = (ClassDescriptor) filteredDescriptor;
                    }
                }

                if (namespaceDescriptor != null && classDescriptor != null) {
                    if (DescriptorUtils.getFQName(namespaceDescriptor).equals(DescriptorUtils.getFQName(classDescriptor))) {
                        trace.record(BindingContext.REFERENCE_TARGET, referenceExpression, classDescriptor);
                        trace.record(BindingContext.RESOLUTION_SCOPE, referenceExpression, resolutionScope);
                        return true;
                    }
                }
            }

            return false;
        }

        private interface LookupResult {
            LookupResult EMPTY = new LookupResult() {};
        }

        private static class SuccessfulLookupResult implements LookupResult {
            final Collection<? extends DeclarationDescriptor> descriptors;
            final JetScope resolutionScope;
            final boolean namespaceLevel;

            private SuccessfulLookupResult(Collection<? extends DeclarationDescriptor> descriptors, JetScope resolutionScope, boolean namespaceLevel) {
                this.descriptors = descriptors;
                this.resolutionScope = resolutionScope;
                this.namespaceLevel = namespaceLevel;
            }
        }
    }
}