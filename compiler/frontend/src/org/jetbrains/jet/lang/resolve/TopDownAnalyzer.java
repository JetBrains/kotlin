package org.jetbrains.jet.lang.resolve;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.JetSemanticServices;
import org.jetbrains.jet.lang.cfg.pseudocode.JetControlFlowDataTraceFactory;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.resolve.scopes.WritableScope;

import java.util.Collections;
import java.util.List;

/**
 * @author abreslav
 */
public class TopDownAnalyzer {

    private TopDownAnalyzer() {}

    public static void process(
            @NotNull JetSemanticServices semanticServices,
            @NotNull BindingTrace trace,
            @NotNull JetScope outerScope,
            NamespaceLike owner,
            @NotNull List<? extends JetDeclaration> declarations,
            @NotNull JetControlFlowDataTraceFactory flowDataTraceFactory) {
        process(semanticServices, trace, outerScope, owner, declarations, flowDataTraceFactory, false);
    }

    private static void process(
            @NotNull JetSemanticServices semanticServices,
            @NotNull BindingTrace trace,
            @NotNull JetScope outerScope,
            NamespaceLike owner,
            @NotNull List<? extends JetDeclaration> declarations,
            @NotNull JetControlFlowDataTraceFactory flowDataTraceFactory,
            boolean declaredLocally) {
        TopDownAnalysisContext context = new TopDownAnalysisContext(semanticServices, trace);
        new TypeHierarchyResolver(context).process(outerScope, owner, declarations);
        new DeclarationResolver(context).process();
        new DelegationResolver(context).process();
        new OverrideResolver(context).process();
        new BodyResolver(context).resolveBehaviorDeclarationBodies();
        new ControlFlowAnalyzer(context, flowDataTraceFactory, declaredLocally).process();
        new DeclarationsChecker(context).process();
    }

    public static void processStandardLibraryNamespace(
            @NotNull JetSemanticServices semanticServices,
            @NotNull BindingTrace trace,
            @NotNull WritableScope outerScope, @NotNull NamespaceDescriptorImpl standardLibraryNamespace, @NotNull JetNamespace namespace) {
        TopDownAnalysisContext context = new TopDownAnalysisContext(semanticServices, trace);
        context.getNamespaceScopes().put(namespace, standardLibraryNamespace.getMemberScope());
        context.getNamespaceDescriptors().put(namespace, standardLibraryNamespace);
        context.getDeclaringScopes().put(namespace, outerScope);

        new TypeHierarchyResolver(context).process(outerScope, standardLibraryNamespace, namespace.getDeclarations());
        new DeclarationResolver(context).process();
        new DelegationResolver(context).process();
        OverrideResolver overrideResolver = new OverrideResolver(context) {
            @Override
            protected void checkOverridesInAClass(MutableClassDescriptor classDescriptor, JetClassOrObject klass) {
            }
        };
        overrideResolver.process();
        new BodyResolver(context).resolveBehaviorDeclarationBodies();
    }

    public static void processObject(
            @NotNull JetSemanticServices semanticServices,
            @NotNull BindingTrace trace,
            @NotNull JetScope outerScope,
            @NotNull DeclarationDescriptor containingDeclaration,
            @NotNull JetObjectDeclaration object) {
        process(semanticServices, trace, outerScope, new NamespaceLike.Adapter(containingDeclaration) {

            @Override
            public NamespaceDescriptorImpl getNamespace(String name) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void addNamespace(@NotNull NamespaceDescriptor namespaceDescriptor) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void addClassifierDescriptor(@NotNull MutableClassDescriptor classDescriptor) {

            }

            @Override
            public void addFunctionDescriptor(@NotNull FunctionDescriptor functionDescriptor) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void addPropertyDescriptor(@NotNull PropertyDescriptor propertyDescriptor) {

            }

            @Override
            public ClassObjectStatus setClassObjectDescriptor(@NotNull MutableClassDescriptor classObjectDescriptor) {
                return ClassObjectStatus.NOT_ALLOWED;
            }
        }, Collections.<JetDeclaration>singletonList(object), JetControlFlowDataTraceFactory.EMPTY, true);
    }

}


