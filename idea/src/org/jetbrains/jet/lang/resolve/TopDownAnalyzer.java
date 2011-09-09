package org.jetbrains.jet.lang.resolve;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.JetSemanticServices;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.psi.*;

import java.util.Collections;
import java.util.List;

/**
 * @author abreslav
 */
public class TopDownAnalyzer {

    private TopDownAnalyzer() {}

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
                }, Collections.<JetDeclaration>singletonList(object));
    }

    public static void process(
            @NotNull JetSemanticServices semanticServices,
            @NotNull BindingTrace trace,
            @NotNull JetScope outerScope, NamespaceLike owner, @NotNull List<JetDeclaration> declarations) {
        TypeHierarchyResolver typeHierarchyResolver = new TypeHierarchyResolver(semanticServices, trace);
        typeHierarchyResolver.process(outerScope, owner, declarations);

        DeclarationResolver declarationResolver = new DeclarationResolver(semanticServices, trace, typeHierarchyResolver);
        declarationResolver.process();

        new BodyResolver(semanticServices, trace, typeHierarchyResolver, declarationResolver).resolveBehaviorDeclarationBodies();
    }

    public static void processStandardLibraryNamespace(
            @NotNull JetSemanticServices semanticServices,
            @NotNull BindingTrace trace,
            @NotNull WritableScope outerScope, @NotNull NamespaceDescriptorImpl standardLibraryNamespace, @NotNull JetNamespace namespace) {
        TypeHierarchyResolver typeHierarchyResolver = new TypeHierarchyResolver(semanticServices, trace);
        typeHierarchyResolver.getNamespaceScopes().put(namespace, standardLibraryNamespace.getMemberScope());
        typeHierarchyResolver.getNamespaceDescriptors().put(namespace, standardLibraryNamespace);
        typeHierarchyResolver.process(outerScope, standardLibraryNamespace, namespace.getDeclarations());

        DeclarationResolver declarationResolver = new DeclarationResolver(semanticServices, trace, typeHierarchyResolver);
        declarationResolver.process();

        BodyResolver bodyResolver = new BodyResolver(semanticServices, trace, typeHierarchyResolver, declarationResolver) {
            @Override
            protected void checkProperty(JetProperty property, PropertyDescriptor propertyDescriptor, @Nullable ClassDescriptor classDescriptor) {
            }

            @Override
            protected void checkFunction(JetDeclarationWithBody function, FunctionDescriptor functionDescriptor) {
            }
        };
        bodyResolver.resolveBehaviorDeclarationBodies();
    }

}


