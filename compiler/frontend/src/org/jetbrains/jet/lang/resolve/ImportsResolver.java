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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.resolve.scopes.WritableScope;
import org.jetbrains.jet.lang.types.JetType;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.jetbrains.jet.lang.diagnostics.Errors.*;

/**
 * @author abreslav
 * @author svtk
 */
public class ImportsResolver {
    private final TopDownAnalysisContext context;

    public ImportsResolver(@NotNull TopDownAnalysisContext context) {
        this.context = context;
    }

    public void processTypeImports() {
        processImports(true);
    }

    public void processMembersImports() {
        processImports(false);
    }

    private void processImports(boolean firstPhase) {
        ImportResolver importResolver = new ImportResolver(context.getTrace(), firstPhase);
        for (JetFile file : context.getNamespaceDescriptors().keySet()) {
            WritableScope namespaceScope = context.getNamespaceScopes().get(file);
            Importer.DelayedImporter delayedImporter = new Importer.DelayedImporter(namespaceScope, firstPhase);
            if (!firstPhase) {
                namespaceScope.clearImports();
            }
            context.getConfiguration().addDefaultImports(context.getTrace(), namespaceScope, delayedImporter);
            Map<JetImportDirective, DeclarationDescriptor> resolvedDirectives = Maps.newHashMap();

            List<JetImportDirective> importDirectives = file.getImportDirectives();
            for (JetImportDirective importDirective : importDirectives) {
                Collection<? extends DeclarationDescriptor> descriptors = importResolver.processImportReference(importDirective, namespaceScope, delayedImporter);
                if (descriptors != null && descriptors.size() == 1) {
                    resolvedDirectives.put(importDirective, descriptors.iterator().next());
                }
            }
            delayedImporter.processImports();

            if (firstPhase) continue;
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
        if (importedReference == null) return null;
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
        if (importedReference == null || !resolvedDirectives.containsKey(importDirective)) return;
        String aliasName = getAliasName(importDirective);
        if (aliasName == null) return;

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

    public static class ImportResolver {
        private final BindingTrace trace;
        /* On first phase all classes and objects are imported,
        on second phase previous imports are thrown and everything (including functions and properties at namespace level) is imported */
        private final boolean firstPhase;

        public ImportResolver(BindingTrace trace, boolean firstPhase) {
            this.trace = trace;
            this.firstPhase = firstPhase;
        }

        @Nullable
        public Collection<? extends DeclarationDescriptor> processImportReference(@NotNull JetImportDirective importDirective, @NotNull JetScope scope, @NotNull Importer importer) {
            if (importDirective.isAbsoluteInRootNamespace()) {
                trace.report(UNSUPPORTED.on(importDirective, "TypeHierarchyResolver")); // TODO
                return null;
            }
            JetExpression importedReference = importDirective.getImportedReference();
            if (importedReference == null) return null;
            Collection<? extends DeclarationDescriptor> descriptors;
            if (importedReference instanceof JetQualifiedExpression) {
                descriptors = lookupDescriptorsForQualifiedExpression((JetQualifiedExpression) importedReference, scope);
            }
            else {
                assert importedReference instanceof JetSimpleNameExpression;
                descriptors = lookupDescriptorsForSimpleNameReference((JetSimpleNameExpression) importedReference, scope, true);
            }

            JetSimpleNameExpression referenceExpression = getLastReference(importedReference);
            if (importDirective.isAllUnder()) {
                if (referenceExpression == null || !canImportMembersFrom(descriptors, referenceExpression)) return null;
                for (DeclarationDescriptor descriptor : descriptors) {
                    importer.addAllUnderImport(descriptor);
                }
                return null;
            }

            String aliasName = getAliasName(importDirective);
            if (aliasName == null) return null;

            for (DeclarationDescriptor descriptor : descriptors) {
                importer.addAliasImport(descriptor, aliasName);
            }
            return descriptors;
        }

        @NotNull
        private Collection<? extends DeclarationDescriptor> lookupDescriptorsForQualifiedExpression(
                @NotNull JetQualifiedExpression importedReference, @NotNull JetScope outerScope) {

            JetExpression receiverExpression = importedReference.getReceiverExpression();
            Collection<? extends DeclarationDescriptor> declarationDescriptors;
            if (receiverExpression instanceof JetQualifiedExpression) {
                declarationDescriptors = lookupDescriptorsForQualifiedExpression((JetQualifiedExpression) receiverExpression, outerScope);
            }
            else {
                assert receiverExpression instanceof JetSimpleNameExpression;
                declarationDescriptors = lookupDescriptorsForSimpleNameReference((JetSimpleNameExpression) receiverExpression, outerScope, true);
            }

            JetExpression selectorExpression = importedReference.getSelectorExpression();
            assert selectorExpression instanceof JetSimpleNameExpression;
            JetSimpleNameExpression selector = (JetSimpleNameExpression) selectorExpression;
            JetSimpleNameExpression lastReference = getLastReference(receiverExpression);
            if (lastReference == null || !canImportMembersFrom(declarationDescriptors, lastReference)) {
                return Collections.emptyList();
            }
            Collection<? extends DeclarationDescriptor> result;
            for (DeclarationDescriptor declarationDescriptor : declarationDescriptors) {
                if (declarationDescriptor instanceof NamespaceDescriptor) {
                    result = lookupDescriptorsForSimpleNameReference(selector, ((NamespaceDescriptor) declarationDescriptor).getMemberScope(), true);
                    if (!result.isEmpty()) return result;
                }
                if (declarationDescriptor instanceof ClassDescriptor) {
                    result = lookupObjectMembers((ClassDescriptor) declarationDescriptor, selector);
                    if (!result.isEmpty()) return result;
                }
                if (declarationDescriptor instanceof VariableDescriptor) {
                    result = lookupVariableMembers((VariableDescriptor) declarationDescriptor, selector);
                    if (!result.isEmpty()) return result;
                }
            }
            return Collections.emptyList();
        }

        private boolean canImportMembersFrom(@NotNull Collection<? extends DeclarationDescriptor> descriptors, @NotNull JetSimpleNameExpression reference) {
            if (firstPhase) return true;
            if (descriptors.size() == 1) {
                return canImportMembersFrom(descriptors.iterator().next(), reference, trace);
            }
            TemporaryBindingTrace temporaryTrace = TemporaryBindingTrace.create(trace);
            boolean canImport = false;
            for (DeclarationDescriptor descriptor : descriptors) {
                canImport |= canImportMembersFrom(descriptor, reference, temporaryTrace);
            }
            if (!canImport) {
                temporaryTrace.commit();
            }
            return canImport;
        }
        
        private boolean canImportMembersFrom(@NotNull DeclarationDescriptor descriptor,
                                             @NotNull JetSimpleNameExpression reference,
                                             @NotNull BindingTrace trace) {
            assert !firstPhase;
            if (descriptor instanceof NamespaceDescriptor) {
                return true;
            }
            if (descriptor instanceof ClassDescriptor) {
                ClassDescriptor classDescriptor = (ClassDescriptor) descriptor;
                JetType classObjectType = classDescriptor.getClassObjectType();
                if (classObjectType == null) {
                    trace.report(NO_CLASS_OBJECT.on(reference, classDescriptor));
                    return false;
                }
                return true;
            }
            if (descriptor instanceof VariableDescriptor && ((VariableDescriptor) descriptor).isObjectDeclaration()) {
                return true;
            }
            trace.report(CANNOT_IMPORT_FROM_ELEMENT.on(reference, descriptor));
            return false;
        }

        @NotNull
        private Collection<? extends DeclarationDescriptor> lookupObjectMembers(@NotNull ClassDescriptor classDescriptor, @NotNull JetSimpleNameExpression memberReference) {
            if (firstPhase) {
                ClassDescriptor objectDescriptor = DescriptorUtils.getObjectIfObjectOrClassObjectDescriptor(classDescriptor);
                if (objectDescriptor == null) return Collections.emptyList();
                return getInnerClassesAndObjectsByName(objectDescriptor, memberReference);
            }
            //on second phase class descriptor is only a descriptor for class object
            JetType classObjectType = classDescriptor.getClassObjectType();
            if (classObjectType == null) return Collections.emptyList();
            return lookupDescriptorsForSimpleNameReference(memberReference, classObjectType.getMemberScope(), false);
        }

        @NotNull
        private Collection<? extends DeclarationDescriptor> getInnerClassesAndObjectsByName(@NotNull ClassDescriptor classDescriptor, @NotNull JetSimpleNameExpression memberReference) {
            assert classDescriptor.getKind() == ClassKind.OBJECT;
            Collection<? extends DeclarationDescriptor> descriptors;
            ClassDescriptor innerClass = classDescriptor.getInnerClassOrObject(memberReference.getReferencedName());
            if (innerClass == null) return Collections.emptyList();
            descriptors = Collections.<DeclarationDescriptor>singletonList(innerClass);
            return filterResolutionResult(descriptors, memberReference, JetScope.EMPTY, false);
        }

        private Collection<? extends DeclarationDescriptor> lookupVariableMembers(@NotNull VariableDescriptor variableDescriptor, @NotNull JetSimpleNameExpression memberReference) {
            if (firstPhase) return Collections.emptyList();

            JetType variableType = variableDescriptor.getReturnType();
            if (variableType == null) return Collections.emptyList();
            return lookupDescriptorsForSimpleNameReference(memberReference, variableType.getMemberScope(), false);
        }

        @NotNull
        private Collection<? extends DeclarationDescriptor> lookupDescriptorsForSimpleNameReference(
                @NotNull JetSimpleNameExpression referenceExpression,
                @NotNull JetScope outerScope,
                boolean namespaceLevel) {

            String referencedName = referenceExpression.getReferencedName();
            if (referencedName == null) return Collections.emptyList();

            List<DeclarationDescriptor> descriptors = Lists.newArrayList();
            NamespaceDescriptor namespaceDescriptor = outerScope.getNamespace(referencedName);
            if (namespaceDescriptor != null) descriptors.add(namespaceDescriptor);

            ClassifierDescriptor classifierDescriptor = outerScope.getClassifier(referencedName);
            if (classifierDescriptor != null) descriptors.add(classifierDescriptor);

            if (firstPhase) {
                descriptors.add(outerScope.getObjectDescriptor(referencedName));
            }
            else {
                descriptors.addAll(outerScope.getFunctions(referencedName));

                descriptors.addAll(outerScope.getProperties(referencedName));

                VariableDescriptor localVariable = outerScope.getLocalVariable(referencedName);
                if (localVariable != null) descriptors.add(localVariable);
            }
            return filterResolutionResult(descriptors, referenceExpression, outerScope, namespaceLevel);
        }

        private Collection<? extends DeclarationDescriptor> filterResolutionResult(
                @NotNull Collection<? extends DeclarationDescriptor> descriptors,
                @NotNull JetSimpleNameExpression referenceExpression,
                @NotNull JetScope resolutionScope,
                //functions and properties can be imported if namespaceLevel == true
                boolean namespaceLevel) {
            if (firstPhase) {
                return Collections2.filter(descriptors, new Predicate<DeclarationDescriptor>() {
                    @Override
                    public boolean apply(@Nullable DeclarationDescriptor descriptor) {
                        return (descriptor instanceof ClassDescriptor) || 
                               (descriptor instanceof NamespaceDescriptor);
                    }
                });
            }
            Collection<? extends DeclarationDescriptor> filteredDescriptors;
            if (namespaceLevel) {
                filteredDescriptors = descriptors;
            }
            else {
                filteredDescriptors = Collections2.filter(descriptors, new Predicate<DeclarationDescriptor>() {
                    @Override
                    public boolean apply(@Nullable DeclarationDescriptor descriptor) {
                        return (descriptor instanceof NamespaceDescriptor) ||
                               (descriptor instanceof ClassDescriptor) ||
                               (descriptor instanceof VariableDescriptor && ((VariableDescriptor) descriptor).isObjectDeclaration());
                    }
                });
            }
            storeResolutionResult(descriptors, filteredDescriptors, referenceExpression, resolutionScope);
            return filteredDescriptors;
        }

        private void storeResolutionResult(
                @NotNull Collection<? extends DeclarationDescriptor> descriptors,
                @NotNull Collection<? extends DeclarationDescriptor> filteredDescriptors,
                @NotNull JetSimpleNameExpression referenceExpression,
                @NotNull JetScope resolutionScope) {

            if (descriptors.size() == 1 && filteredDescriptors.size() <= 1) {
                trace.record(BindingContext.REFERENCE_TARGET, referenceExpression, descriptors.iterator().next());
            }
            if (filteredDescriptors.isEmpty()) {
                if (!descriptors.isEmpty()) {
                    trace.report(CANNOT_BE_IMPORTED.on(referenceExpression, descriptors.iterator().next()));
                }
                else {
                    trace.report(UNRESOLVED_REFERENCE.on(referenceExpression));
                }
            }
            else if (filteredDescriptors.size() > 1) {
                trace.record(BindingContext.AMBIGUOUS_REFERENCE_TARGET, referenceExpression, descriptors);
            }

            // If it's not an ambiguous case - store resolution scope
            if (descriptors.size() <= 1) {
                trace.record(BindingContext.RESOLUTION_SCOPE, referenceExpression, resolutionScope);
            }
        }
    }
}
