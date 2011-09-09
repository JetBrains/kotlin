package org.jetbrains.jet.lang.resolve;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.JetSemanticServices;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.psi.JetNamedFunction;
import org.jetbrains.jet.lang.psi.JetNamespace;
import org.jetbrains.jet.lang.psi.JetProperty;

/**
 * @author svtk
 */
//public class TopDownAnalyzerForStandardLibrary extends TopDownAnalyzer {
//
//    public TopDownAnalyzerForStandardLibrary(JetSemanticServices semanticServices, @NotNull BindingTrace bindingTrace) {
//        super(semanticServices, bindingTrace);
//    }
//
//    public void processStandardLibraryNamespace(@NotNull WritableScope outerScope, @NotNull NamespaceDescriptorImpl standardLibraryNamespace, @NotNull JetNamespace namespace) {
//        namespaceScopes.put(namespace, standardLibraryNamespace.getMemberScope());
//        namespaceDescriptors.put(namespace, standardLibraryNamespace);
//        TypeHierarchyResolver typeHierarchyResolver = new TypeHierarchyResolver(semanticServices, trace);
//        typeHierarchyResolver.process(outerScope, standardLibraryNamespace, namespace.getDeclarations());
//
//        DeclarationResolver declarationResolver = new DeclarationResolver(semanticServices, trace, typeHierarchyResolver);
//        declarationResolver.process();
//
//        new BodyResolver(semanticServices, trace, typeHierarchyResolver, declarationResolver).resolveBehaviorDeclarationBodies();
//    }
//
//    @Override
//    protected void checkProperty(JetProperty property, PropertyDescriptor propertyDescriptor, ClassDescriptor classDescriptor) {}
//
//
//    @Override
//    protected void checkFunction(JetNamedFunction function, FunctionDescriptor functionDescriptor, DeclarationDescriptor containingDescriptor) {}
//}
