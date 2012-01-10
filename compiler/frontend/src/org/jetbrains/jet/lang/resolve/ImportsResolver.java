package org.jetbrains.jet.lang.resolve;

import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.resolve.scopes.ScopeBoundToReceiver;
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
            if (firstPhase) {
                context.getConfiguration().addDefaultImports(context.getTrace(), namespaceScope);
            }

            List<JetImportDirective> importDirectives = file.getImportDirectives();
            for (JetImportDirective importDirective : importDirectives) {
                importResolver.processImportReference(importDirective, namespaceScope);
            }
        }
    }

    public static class ImportResolver {
        private final BindingTrace trace;
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
            Collection<DeclarationDescriptor> descriptors;
            if (importedReference instanceof JetQualifiedExpression) {
                descriptors = lookupDescriptorsForQualifiedExpression((JetQualifiedExpression) importedReference, namespaceScope);
            }
            else {
                assert importedReference instanceof JetSimpleNameExpression;

                if (importDirective.isAllUnder()) {
                    // Example: import java.*
                    descriptors = lookupDescriptorsForSimpleNameReference((JetSimpleNameExpression) importedReference, namespaceScope);
                }
                else {
                    // Example: import IDENTIFIER
                    // Should be unresolved error
                    descriptors = lookupDescriptorsForSimpleNameReference((JetSimpleNameExpression) importedReference, JetScope.EMPTY);
                    trace.record(BindingContext.RESOLUTION_SCOPE, importedReference, namespaceScope);
                }
            }
            JetSimpleNameExpression referenceExpression = getLastReference(importedReference);
            if (importDirective.isAllUnder()) {
                if (referenceExpression == null || !canImportMembersFrom(descriptors, referenceExpression)) return;
                for (DeclarationDescriptor descriptor : descriptors) {
                    if (firstPhase) {
                        if (descriptor instanceof NamespaceDescriptor) {
                            namespaceScope.importScope(((NamespaceDescriptor) descriptor).getMemberScope());
                        }
                        continue;
                    }
                    if (descriptor instanceof VariableDescriptor) {
                        JetType type = ((VariableDescriptor) descriptor).getOutType();
                        namespaceScope.importScope(ScopeBoundToReceiver.create(descriptor, type.getMemberScope()));
                    }
                    else if (descriptor instanceof ClassDescriptor) {
                        namespaceScope.importScope(ScopeBoundToReceiver.create(descriptor, ((ClassDescriptor) descriptor).getDefaultType().getMemberScope()));
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
        private Collection<DeclarationDescriptor> lookupDescriptorsForQualifiedExpression(@NotNull JetQualifiedExpression importedReference, @NotNull JetScope outerScope) {
            JetExpression receiverExpression = importedReference.getReceiverExpression();
            Collection<DeclarationDescriptor> declarationDescriptors;
            if (receiverExpression instanceof JetQualifiedExpression) {
                declarationDescriptors = lookupDescriptorsForQualifiedExpression((JetQualifiedExpression) receiverExpression, outerScope);
            }
            else {
                assert receiverExpression instanceof JetSimpleNameExpression;
                declarationDescriptors = lookupDescriptorsForSimpleNameReference((JetSimpleNameExpression) receiverExpression, outerScope);
            }
            JetExpression selectorExpression = importedReference.getSelectorExpression();
            assert selectorExpression instanceof JetSimpleNameExpression;
            JetSimpleNameExpression selector = (JetSimpleNameExpression) selectorExpression;
            JetSimpleNameExpression lastReference = getLastReference(receiverExpression);
            if (lastReference == null || !canImportMembersFrom(declarationDescriptors, lastReference)) {
                return Collections.emptyList();
            }
            for (DeclarationDescriptor declarationDescriptor : declarationDescriptors) {
                if (declarationDescriptor instanceof NamespaceDescriptor) {
                    return lookupDescriptorsForSimpleNameReference(selector, ((NamespaceDescriptor) declarationDescriptor).getMemberScope());
                }
                if (declarationDescriptor instanceof ClassDescriptor) {
                    return lookupObjectMembers(selector, (ClassDescriptor) declarationDescriptor);
                }
                if (declarationDescriptor instanceof VariableDescriptor) {
                    return lookupVariableMembers(selector, (VariableDescriptor) declarationDescriptor);
                }
            }

            return Collections.emptyList();
        }

        private boolean canImportMembersFrom(@NotNull Collection<DeclarationDescriptor> descriptors, @NotNull JetSimpleNameExpression reference) {
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
        
        private boolean canImportMembersFrom(@NotNull DeclarationDescriptor descriptor, @NotNull JetSimpleNameExpression reference, @NotNull BindingTrace trace) {
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
            if (descriptor instanceof VariableDescriptor && DescriptorUtils.isObjectDescriptor((VariableDescriptor) descriptor, trace)) {
                return true;
            }
            trace.report(CANNOT_IMPORT_FROM_ELEMENT.on(reference, descriptor));
            return false;
        }

        @NotNull
        private Collection<DeclarationDescriptor> lookupObjectMembers(@NotNull JetSimpleNameExpression memberReference,
                                                                      @NotNull ClassDescriptor classDescriptor) {
            if (firstPhase) return Collections.emptyList();
            JetType classObjectType = classDescriptor.getClassObjectType();
            assert classObjectType != null;
            Collection<DeclarationDescriptor> members = lookupDescriptorsForSimpleNameReference(memberReference, classObjectType.getMemberScope());
            return addBoundToReceiver(members, classDescriptor);
        }

        private Collection<DeclarationDescriptor> lookupVariableMembers(@NotNull JetSimpleNameExpression memberReference, @NotNull VariableDescriptor variableDescriptor) {
            if (firstPhase) return Collections.emptyList();

            JetType variableType = variableDescriptor.getReturnType();
            if (variableType == null) return Collections.emptyList();
            Collection<DeclarationDescriptor> members = lookupDescriptorsForSimpleNameReference(memberReference, variableType.getMemberScope());
            return addBoundToReceiver(members, variableDescriptor);
        }

        @NotNull
        public static Collection<DeclarationDescriptor> addBoundToReceiver(@NotNull Collection<DeclarationDescriptor> descriptors, @NotNull final DeclarationDescriptor receiver) {
            return Collections2.transform(descriptors, DescriptorUtils.getAddBoundToReceiverFunction(receiver));
        }

        @NotNull
        private Collection<DeclarationDescriptor> lookupDescriptorsForSimpleNameReference(@NotNull JetSimpleNameExpression referenceExpression, @NotNull JetScope outerScope) {

            List<DeclarationDescriptor> descriptors = Lists.newArrayList();
            String referencedName = referenceExpression.getReferencedName();
            if (referencedName != null) {
                NamespaceDescriptor namespaceDescriptor = outerScope.getNamespace(referencedName);
                if (namespaceDescriptor != null) descriptors.add(namespaceDescriptor);

                ClassifierDescriptor classifierDescriptor = outerScope.getClassifier(referencedName);
                if (classifierDescriptor != null) descriptors.add(classifierDescriptor);

                descriptors.addAll(outerScope.getFunctions(referencedName));

                descriptors.addAll(outerScope.getProperties(referencedName));
            }
            if (!firstPhase) {
                if (descriptors.size() == 1) {
                    trace.record(BindingContext.REFERENCE_TARGET, referenceExpression, descriptors.get(0));
                }
                else if (descriptors.size() > 1) {
                    trace.record(BindingContext.AMBIGUOUS_REFERENCE_TARGET, referenceExpression, descriptors);
                }
                else {
                    trace.report(UNRESOLVED_REFERENCE.on(referenceExpression));
                }

                // If it's not an ambiguous case - store resolution scope
                if (descriptors.size() <= 1 && outerScope != JetScope.EMPTY) {
                    trace.record(BindingContext.RESOLUTION_SCOPE, referenceExpression, outerScope);
                }
            }
            return descriptors;
        }
    }
}
