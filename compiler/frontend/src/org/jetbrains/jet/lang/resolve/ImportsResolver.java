package org.jetbrains.jet.lang.resolve;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.ClassifierDescriptor;
import org.jetbrains.jet.lang.descriptors.NamespaceDescriptor;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.resolve.scopes.WritableScope;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.expressions.ExpressionTypingServices;

import java.util.List;

import static org.jetbrains.jet.lang.diagnostics.Errors.UNRESOLVED_REFERENCE;
import static org.jetbrains.jet.lang.diagnostics.Errors.UNSUPPORTED;

/**
 * @author abreslav
 */
public class ImportsResolver {
    private final TopDownAnalysisContext context;

    public ImportsResolver(@NotNull TopDownAnalysisContext context) {
        this.context = context;
    }

    public void processTypeImports() {
        for (JetNamespace jetNamespace : context.getNamespaceDescriptors().keySet()) {
            processImports(jetNamespace, context.getNamespaceScopes().get(jetNamespace), context.getDeclaringScopes().get(jetNamespace));
        }
    }

    private void processImports(@NotNull JetNamespace namespace, @NotNull WritableScope namespaceScope, @NotNull JetScope outerScope) {
        context.getConfiguration().addDefaultImports(context.getTrace(), namespaceScope);

        List<JetImportDirective> importDirectives = namespace.getImportDirectives();
        for (JetImportDirective importDirective : importDirectives) {
            if (importDirective.isAbsoluteInRootNamespace()) {
                context.getTrace().report(UNSUPPORTED.on(importDirective, "TypeHierarchyResolver")); // TODO
                continue;
            }
            if (importDirective.isAllUnder()) {
                JetExpression importedReference = importDirective.getImportedReference();
                if (importedReference != null) {
                    ExpressionTypingServices typeInferrerServices = context.getSemanticServices().getTypeInferrerServices(context.getTrace());
                    JetType type = typeInferrerServices.getTypeWithNamespaces(namespaceScope, importedReference);
                    if (type != null) {
                        namespaceScope.importScope(type.getMemberScope());
                    }
                }
            }
            else {
                ClassifierDescriptor classifierDescriptor = null;
                NamespaceDescriptor namespaceDescriptor = null;
                JetSimpleNameExpression referenceExpression = null;

                JetExpression importedReference = importDirective.getImportedReference();
                if (importedReference instanceof JetDotQualifiedExpression) {
                    JetDotQualifiedExpression reference = (JetDotQualifiedExpression) importedReference;
                    JetType type = context.getSemanticServices().getTypeInferrerServices(context.getTrace()).getTypeWithNamespaces(namespaceScope, reference.getReceiverExpression());
                    JetExpression selectorExpression = reference.getSelectorExpression();
                    if (selectorExpression != null) {
                        referenceExpression = (JetSimpleNameExpression) selectorExpression;
                        String referencedName = referenceExpression.getReferencedName();
                        if (type != null && referencedName != null) {
                            classifierDescriptor = type.getMemberScope().getClassifier(referencedName);
                            namespaceDescriptor = type.getMemberScope().getNamespace(referencedName);
                        }
                    }
                }
                else {
                    assert importedReference instanceof JetSimpleNameExpression;
                    referenceExpression = (JetSimpleNameExpression) importedReference;

                    String referencedName = referenceExpression.getReferencedName();
                    if (referencedName != null) {
                        classifierDescriptor = outerScope.getClassifier(referencedName);
                        namespaceDescriptor = outerScope.getNamespace(referencedName);

                        if (classifierDescriptor == null && namespaceDescriptor == null) {
                            context.getTrace().report(UNRESOLVED_REFERENCE.on(referenceExpression));
                        }
                    }
                }

                String aliasName = importDirective.getAliasName();
                if (aliasName == null) {
                    aliasName = referenceExpression != null ? referenceExpression.getReferencedName() : null;
                }
                if (classifierDescriptor != null) {
                    context.getTrace().record(BindingContext.REFERENCE_TARGET, referenceExpression, classifierDescriptor);

                    if (aliasName != null) {
                        namespaceScope.importClassifierAlias(aliasName, classifierDescriptor);
                    }
                }
                if (namespaceDescriptor != null) {
                    if (classifierDescriptor == null) {
                        context.getTrace().record(BindingContext.REFERENCE_TARGET, referenceExpression, namespaceDescriptor);
                    }
                    if (aliasName != null) {
                        namespaceScope.importNamespaceAlias(aliasName, namespaceDescriptor);
                    }
                }
            }
        }
    }
}
