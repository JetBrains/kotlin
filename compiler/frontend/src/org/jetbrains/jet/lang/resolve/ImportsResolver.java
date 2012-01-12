package org.jetbrains.jet.lang.resolve;

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
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
            if (!firstPhase) {
                namespaceScope.clearImports();
            }
            context.getConfiguration().addDefaultImports(context.getTrace(), namespaceScope);

            List<JetImportDirective> importDirectives = file.getImportDirectives();
            for (JetImportDirective importDirective : importDirectives) {
                importResolver.processImportReference(importDirective, namespaceScope);
            }
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

        public void processImportReference(JetImportDirective importDirective, WritableScope namespaceScope) {
            if (importDirective.isAbsoluteInRootNamespace()) {
                trace.report(UNSUPPORTED.on(importDirective, "TypeHierarchyResolver")); // TODO
                return;
            }
            JetExpression importedReference = importDirective.getImportedReference();
            if (importedReference == null) return;
            Collection<? extends DeclarationDescriptor> descriptors;
            if (importedReference instanceof JetQualifiedExpression) {
                descriptors = lookupDescriptorsForQualifiedExpression((JetQualifiedExpression) importedReference,
                                                                      namespaceScope, !importDirective.isAllUnder());
            }
            else {
                assert importedReference instanceof JetSimpleNameExpression;
                descriptors = lookupDescriptorsForSimpleNameReference((JetSimpleNameExpression) importedReference,
                                                                      namespaceScope, true, !importDirective.isAllUnder());
            }

            JetSimpleNameExpression referenceExpression = getLastReference(importedReference);
            if (importDirective.isAllUnder()) {
                if (referenceExpression == null || !canImportMembersFrom(descriptors, referenceExpression)) return;
                for (DeclarationDescriptor descriptor : descriptors) {
                    if (firstPhase) {
                        if (descriptor instanceof NamespaceDescriptor) {
                            namespaceScope.importScope(((NamespaceDescriptor) descriptor).getMemberScope());
                        }
                        if (descriptor instanceof ClassDescriptor) {
                            ClassDescriptor objectDescriptor;
                            if (((ClassDescriptor) descriptor).getKind() == ClassKind.OBJECT) {
                                objectDescriptor = (ClassDescriptor) descriptor;
                            }
                            else {
                                objectDescriptor = ((ClassDescriptor)descriptor).getClassObjectDescriptor();
                            }
                            if (objectDescriptor != null) {
                                Collection<? extends DeclarationDescriptor> innerClassesAndObjects = objectDescriptor.getInnerClassesAndObjects();
                                for (DeclarationDescriptor innerClassOrObject : innerClassesAndObjects) {
                                    namespaceScope.importClassifierAlias(innerClassOrObject.getName(), (ClassifierDescriptor) innerClassOrObject);
                                }
                            }
                        }
                        continue;
                    }
                    if (descriptor instanceof NamespaceDescriptor) {
                        namespaceScope.importScope(((NamespaceDescriptor) descriptor).getMemberScope());
                    }
                    if (descriptor instanceof VariableDescriptor) {
                        JetType type = ((VariableDescriptor) descriptor).getOutType();
                        namespaceScope.importScope(type.getMemberScope());
                    }
                    else if (descriptor instanceof ClassDescriptor) {
                        JetType classObjectType = ((ClassDescriptor) descriptor).getClassObjectType();
                        if (classObjectType != null) {
                            namespaceScope.importScope(classObjectType.getMemberScope());
                        }
                    }
                }
                return;
            }

            String aliasName = importDirective.getAliasName();
            if (aliasName == null) {
                aliasName = referenceExpression != null ? referenceExpression.getReferencedName() : null;
            }
            if (aliasName == null) return;

            for (DeclarationDescriptor descriptor : descriptors) {
                importDeclarationAlias(namespaceScope, aliasName, descriptor);
            }
        }

        private static void importDeclarationAlias(WritableScope namespaceScope, String aliasName, DeclarationDescriptor descriptor) {
            if (descriptor instanceof ClassifierDescriptor) {
                namespaceScope.importClassifierAlias(aliasName, (ClassifierDescriptor) descriptor);
            }
            else if (descriptor instanceof NamespaceDescriptor) {
                namespaceScope.importNamespaceAlias(aliasName, (NamespaceDescriptor) descriptor);
            }
            else if (descriptor instanceof FunctionDescriptor) {
                namespaceScope.importFunctionAlias(aliasName, (FunctionDescriptor) descriptor);
            }
            else if (descriptor instanceof VariableDescriptor) {
                namespaceScope.importVariableAlias(aliasName, (VariableDescriptor) descriptor);
            }
        }

        @Nullable
        private static JetSimpleNameExpression getLastReference(JetExpression importedReference) {
            if (importedReference instanceof JetDotQualifiedExpression) {
                JetDotQualifiedExpression reference = (JetDotQualifiedExpression) importedReference;
                JetExpression selectorExpression = reference.getSelectorExpression();
                return (selectorExpression != null) ? (JetSimpleNameExpression) selectorExpression : null;
            }
            assert importedReference instanceof JetSimpleNameExpression;
            return (JetSimpleNameExpression) importedReference;
        }

        @NotNull
        private Collection<? extends DeclarationDescriptor> lookupDescriptorsForQualifiedExpression(
                @NotNull JetQualifiedExpression importedReference, @NotNull JetScope outerScope, boolean isForLastPart) {

            JetExpression receiverExpression = importedReference.getReceiverExpression();
            Collection<? extends DeclarationDescriptor> declarationDescriptors;
            if (receiverExpression instanceof JetQualifiedExpression) {
                declarationDescriptors = lookupDescriptorsForQualifiedExpression((JetQualifiedExpression) receiverExpression, outerScope, false);
            }
            else {
                assert receiverExpression instanceof JetSimpleNameExpression;
                declarationDescriptors = lookupDescriptorsForSimpleNameReference((JetSimpleNameExpression) receiverExpression, outerScope, true, false);
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
                    result = lookupDescriptorsForSimpleNameReference(selector, ((NamespaceDescriptor) declarationDescriptor).getMemberScope(), true, isForLastPart);
                    if (!result.isEmpty()) return result;
                }
                if (declarationDescriptor instanceof ClassDescriptor) {
                    if (firstPhase) {
                        result = lookupInnerClassesAndObjectsInObject(selector, (ClassDescriptor) declarationDescriptor);
                    }
                    else {
                        result = lookupObjectMembers(selector, (ClassDescriptor) declarationDescriptor);
                    }
                    if (!result.isEmpty()) return result;
                }
                if (!firstPhase) {
                    if (declarationDescriptor instanceof VariableDescriptor) {
                        result = lookupVariableMembers(selector, (VariableDescriptor) declarationDescriptor);
                        if (!result.isEmpty()) return result;
                    }
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
        private Collection<? extends DeclarationDescriptor> lookupInnerClassesAndObjectsInObject(@NotNull JetSimpleNameExpression objectReference,
                                                                                                 @NotNull ClassDescriptor classDescriptor) {
            assert firstPhase;
            if (classDescriptor.getKind() == ClassKind.OBJECT) {
                return getInnerClassesAndObjectsByName(classDescriptor, objectReference);
            }
            MutableClassDescriptor classObjectDescriptor = ((MutableClassDescriptor) classDescriptor).getClassObjectDescriptor();
            assert classObjectDescriptor != null;
            return getInnerClassesAndObjectsByName(classObjectDescriptor, objectReference);
        }
        
        @NotNull
        private Collection<? extends DeclarationDescriptor> lookupObjectMembers(@NotNull JetSimpleNameExpression memberReference,
                                                                      @NotNull ClassDescriptor classDescriptor) {
            assert !firstPhase;
            //on second phase class descriptor is only a descriptor for class object
            JetType classObjectType = classDescriptor.getClassObjectType();
            if (classObjectType == null) return Collections.emptyList();
            return lookupDescriptorsForSimpleNameReference(memberReference, classObjectType.getMemberScope(), false, true);
        }

        @NotNull
        private Collection<? extends DeclarationDescriptor> getInnerClassesAndObjectsByName(@NotNull ClassDescriptor classDescriptor, @NotNull JetSimpleNameExpression memberReference) {
            if (classDescriptor.getKind() != ClassKind.OBJECT) return Collections.emptyList();
            Collection<? extends DeclarationDescriptor> descriptors;
            ClassDescriptor innerClass = classDescriptor.getInnerClassOrObject(memberReference.getReferencedName());
            if (innerClass == null) return Collections.emptyList();
            descriptors = Collections.<DeclarationDescriptor>singletonList(innerClass);
            return filterResolutionResult(descriptors, memberReference, JetScope.EMPTY, false, true);
        }

        private Collection<? extends DeclarationDescriptor> lookupVariableMembers(@NotNull JetSimpleNameExpression memberReference, @NotNull VariableDescriptor variableDescriptor) {
            assert !firstPhase;

            JetType variableType = variableDescriptor.getReturnType();
            if (variableType == null) return Collections.emptyList();
            return lookupDescriptorsForSimpleNameReference(memberReference, variableType.getMemberScope(), false, true);
        }

        @NotNull
        private Collection<? extends DeclarationDescriptor> lookupDescriptorsForSimpleNameReference(
                @NotNull JetSimpleNameExpression referenceExpression,
                @NotNull JetScope outerScope,
                boolean namespaceLevel,
                boolean isLastPart) {

            List<DeclarationDescriptor> descriptors = Lists.newArrayList();
            String referencedName = referenceExpression.getReferencedName();
            if (referencedName != null) {
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
                }
            }

            return filterResolutionResult(descriptors, referenceExpression, outerScope, namespaceLevel, isLastPart);
        }

        private Collection<? extends DeclarationDescriptor> filterResolutionResult(
                @NotNull Collection<? extends DeclarationDescriptor> descriptors,
                @NotNull JetSimpleNameExpression referenceExpression,
                @NotNull JetScope resolutionScope,
                boolean namespaceLevel, //functions and properties can be imported                
                boolean isLastPart) {
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
            return filteredDescriptors;
        }
    }
}
