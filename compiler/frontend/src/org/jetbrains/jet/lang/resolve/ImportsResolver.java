package org.jetbrains.jet.lang.resolve;

import com.google.common.base.Function;
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
                importResolver.processImportReference(importDirective, namespaceScope, JetScope.EMPTY);
            }
        }
    }

    public static class ImportResolver {
        private BindingTrace trace;
        private boolean firstPhase;

        public ImportResolver(BindingTrace trace, boolean firstPhase) {
            this.trace = trace;
            this.firstPhase = firstPhase;
        }

        public void processImportReference(JetImportDirective importDirective, WritableScope namespaceScope, JetScope outerScope) {
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
                JetScope scope = importDirective.isAllUnder() ? namespaceScope : outerScope;
                descriptors = lookupDescriptorsForSimpleNameReference((JetSimpleNameExpression) importedReference, scope);
            }
            if (importDirective.isAllUnder()) {
                for (DeclarationDescriptor descriptor : descriptors) {
                    if (firstPhase) {
                        if (descriptor instanceof NamespaceDescriptor) {
                            namespaceScope.importScope(((NamespaceDescriptor) descriptor).getMemberScope());
                        }
                    }
                    else if (descriptor instanceof VariableDescriptor) {
                        JetType type = ((VariableDescriptor) descriptor).getOutType();
                        if (type != null) {
                            namespaceScope.importScope(ScopeBoundToReceiver.create(descriptor, type.getMemberScope()));
                        }
                    }
                    else if (descriptor instanceof ClassDescriptor) {
                        namespaceScope.importScope(ScopeBoundToReceiver.create(descriptor, ((ClassDescriptor) descriptor).getDefaultType().getMemberScope()));
                    }
                }
                return;
            }
            JetSimpleNameExpression referenceExpression = getLastReference(importedReference);

            String aliasName = importDirective.getAliasName();
            if (aliasName == null) {
                aliasName = referenceExpression != null ? referenceExpression.getReferencedName() : null;
            }
            if (aliasName == null) return;

            for (DeclarationDescriptor descriptor : descriptors) {
                importDeclarationAlias(namespaceScope, aliasName, descriptor);
            }
        }

        private void importDeclarationAlias(WritableScope namespaceScope, String aliasName, DeclarationDescriptor descriptor) {
            if (firstPhase) {
                if (descriptor instanceof ClassifierDescriptor) {
                    namespaceScope.importClassifierAlias(aliasName, (ClassifierDescriptor) descriptor);
                }
                if (descriptor instanceof NamespaceDescriptor) {
                    namespaceScope.importNamespaceAlias(aliasName, (NamespaceDescriptor) descriptor);
                }
                return;
            }
            if (descriptor instanceof FunctionDescriptor) {
                namespaceScope.importFunctionAlias(aliasName, (FunctionDescriptor) descriptor);
            }
            if (descriptor instanceof VariableDescriptor) {
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
            for (DeclarationDescriptor declarationDescriptor : declarationDescriptors) {
                JetExpression selectorExpression = importedReference.getSelectorExpression();
                assert selectorExpression instanceof JetSimpleNameExpression;
                JetSimpleNameExpression selector = (JetSimpleNameExpression) selectorExpression;
                if (declarationDescriptor instanceof NamespaceDescriptor) {
                    return lookupDescriptorsForSimpleNameReference(selector, ((NamespaceDescriptor) declarationDescriptor).getMemberScope());
                }
                if (declarationDescriptor instanceof ClassDescriptor) {
                    JetSimpleNameExpression classReference = getLastReference(receiverExpression);
                    if (classReference != null) {
                        return lookupObjectMembers(selector, classReference, (ClassDescriptor) declarationDescriptor);
                    }
                }
                if (declarationDescriptor instanceof VariableDescriptor) {
                    return lookupVariableMembers(selector, (VariableDescriptor) declarationDescriptor);
                }
            }
            return Collections.emptyList();
        }

        @NotNull
        private Collection<DeclarationDescriptor> lookupObjectMembers(@NotNull JetSimpleNameExpression memberReference, @NotNull JetSimpleNameExpression classReference, @NotNull ClassDescriptor classDescriptor) {
            if (firstPhase) return Collections.emptyList();
            JetType classObjectType = classDescriptor.getClassObjectType();
            if (classObjectType == null || !classDescriptor.isClassObjectAValue()) {
                trace.report(NO_CLASS_OBJECT.on(classReference, classDescriptor));
                return Collections.emptyList();
            }
            return addBoundToReceiver(lookupDescriptorsForSimpleNameReference(memberReference, classObjectType.getMemberScope()), classDescriptor);
        }

        @NotNull
        private Collection<DeclarationDescriptor> lookupVariableMembers(@NotNull JetSimpleNameExpression memberReference, @NotNull VariableDescriptor variableDescriptor) {
            if (firstPhase) return Collections.emptyList();

            JetType variableType = variableDescriptor.getReturnType();
            if (variableType == null) return Collections.emptyList();
            return addBoundToReceiver(lookupDescriptorsForSimpleNameReference(memberReference, variableType.getMemberScope()), variableDescriptor);
        }

        @NotNull
        private static Collection<DeclarationDescriptor> addBoundToReceiver(@NotNull Collection<DeclarationDescriptor> descriptors, @NotNull final DeclarationDescriptor implicitReceiver) {
            return Collections2.transform(descriptors, new Function<DeclarationDescriptor, DeclarationDescriptor>() {
                @Override
                public DeclarationDescriptor apply(@Nullable DeclarationDescriptor descriptor) {
                    if (descriptor instanceof FunctionDescriptor) {
                        return new FunctionDescriptorBoundToReceiver((FunctionDescriptor) descriptor, implicitReceiver);
                    }
                    if (descriptor instanceof VariableDescriptor) {
                        return new VariableDescriptorBoundToReceiver((VariableDescriptor) descriptor, implicitReceiver);
                    }
                    return descriptor;
                }
            });
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
                    trace.record(BindingContext.REFERENCE_TARGET, referenceExpression, descriptors.iterator().next());
                }
                else if (descriptors.size() > 1) {
                    trace.record(BindingContext.AMBIGUOUS_REFERENCE_TARGET, referenceExpression, descriptors);
                }
                else {
                    trace.report(UNRESOLVED_REFERENCE.on(referenceExpression));
                }
            }
            return descriptors;
        }
    }
}
