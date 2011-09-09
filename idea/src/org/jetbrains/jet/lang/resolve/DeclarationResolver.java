package org.jetbrains.jet.lang.resolve;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.JetSemanticServices;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.psi.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.jetbrains.jet.lang.resolve.BindingContext.ANNOTATION;

/**
* @author abreslav
*/
public class DeclarationResolver {
    private final BindingTrace trace;
    private final AnnotationResolver annotationResolver;
    private final TypeHierarchyResolver typeHierarchyResolver;
    private final ClassDescriptorResolver classDescriptorResolver;

    private final Map<JetNamedFunction, FunctionDescriptorImpl> functions = Maps.newLinkedHashMap();
    private final Map<JetDeclaration, ConstructorDescriptor> constructors = Maps.newLinkedHashMap();
    private final Map<JetProperty, PropertyDescriptor> properties = Maps.newLinkedHashMap();
    private final Set<PropertyDescriptor> primaryConstructorParameterProperties = Sets.newHashSet();
    private final Map<JetDeclaration, JetScope> declaringScopes = Maps.newHashMap();

    public DeclarationResolver(JetSemanticServices semanticServices, BindingTrace trace, TypeHierarchyResolver typeHierarchyResolver) {
        this.trace = trace;
        this.typeHierarchyResolver = typeHierarchyResolver;
        this.annotationResolver = new AnnotationResolver(semanticServices, trace);
        this.classDescriptorResolver = semanticServices.getClassDescriptorResolver(trace);
    }

    public void process() {
        resolveConstructorHeaders();
        resolveAnnotationStubsOnClassesAndConstructors();
        resolveFunctionAndPropertyHeaders();
    }

    private void resolveConstructorHeaders() {
        for (Map.Entry<JetClass, MutableClassDescriptor> entry : typeHierarchyResolver.getClasses().entrySet()) {
            JetClass jetClass = entry.getKey();
            MutableClassDescriptor classDescriptor = entry.getValue();

            processPrimaryConstructor(classDescriptor, jetClass);
            for (JetConstructor jetConstructor : jetClass.getSecondaryConstructors()) {
                processSecondaryConstructor(classDescriptor, jetConstructor);
            }
        }
    }

    private void resolveAnnotationStubsOnClassesAndConstructors() {
        for (Map.Entry<JetClass, MutableClassDescriptor> entry : typeHierarchyResolver.getClasses().entrySet()) {
            JetClass jetClass = entry.getKey();
            MutableClassDescriptor mutableClassDescriptor = entry.getValue();

            JetModifierList modifierList = jetClass.getModifierList();
            if (modifierList != null) {
                List<JetAnnotationEntry> annotationEntries = modifierList.getAnnotationEntries();
                for (JetAnnotationEntry annotationEntry : annotationEntries) {
                    AnnotationDescriptor annotationDescriptor = trace.get(ANNOTATION, annotationEntry);
                    if (annotationDescriptor != null) {
                        annotationResolver.resolveAnnotationStub(mutableClassDescriptor.getScopeForSupertypeResolution(), annotationEntry, annotationDescriptor);
                    }
                }
            }
        }
    }

    private void resolveFunctionAndPropertyHeaders() {
        for (Map.Entry<JetNamespace, WritableScope> entry : typeHierarchyResolver.getNamespaceScopes().entrySet()) {
            JetNamespace namespace = entry.getKey();
            WritableScope namespaceScope = entry.getValue();
            NamespaceLike namespaceDescriptor = typeHierarchyResolver.getNamespaceDescriptors().get(namespace);

            resolveFunctionAndPropertyHeaders(namespace.getDeclarations(), namespaceScope, namespaceDescriptor);
        }
        for (Map.Entry<JetClass, MutableClassDescriptor> entry : typeHierarchyResolver.getClasses().entrySet()) {
            JetClass jetClass = entry.getKey();
            MutableClassDescriptor classDescriptor = entry.getValue();

            resolveFunctionAndPropertyHeaders(jetClass.getDeclarations(), classDescriptor.getScopeForMemberResolution(), classDescriptor);
//            processPrimaryConstructor(classDescriptor, jetClass);
//            for (JetConstructor jetConstructor : jetClass.getSecondaryConstructors()) {
//                processSecondaryConstructor(classDescriptor, jetConstructor);
//            }
        }
        for (Map.Entry<JetObjectDeclaration, MutableClassDescriptor> entry : typeHierarchyResolver.getObjects().entrySet()) {
            JetObjectDeclaration object = entry.getKey();
            MutableClassDescriptor classDescriptor = entry.getValue();

            resolveFunctionAndPropertyHeaders(object.getDeclarations(), classDescriptor.getScopeForMemberResolution(), classDescriptor);
        }

        // TODO : Extensions
    }

    private void resolveFunctionAndPropertyHeaders(@NotNull List<JetDeclaration> declarations, final @NotNull JetScope scope, final @NotNull NamespaceLike namespaceLike) {
        for (JetDeclaration declaration : declarations) {
            declaration.accept(new JetVisitorVoid() {
                @Override
                public void visitNamedFunction(JetNamedFunction function) {
                    FunctionDescriptorImpl functionDescriptor = classDescriptorResolver.resolveFunctionDescriptor(namespaceLike, scope, function);
                    namespaceLike.addFunctionDescriptor(functionDescriptor);
                    functions.put(function, functionDescriptor);
                    declaringScopes.put(function, scope);
                }

                @Override
                public void visitProperty(JetProperty property) {
                    PropertyDescriptor propertyDescriptor = classDescriptorResolver.resolvePropertyDescriptor(namespaceLike, scope, property);
                    namespaceLike.addPropertyDescriptor(propertyDescriptor);
                    properties.put(property, propertyDescriptor);
                    declaringScopes.put(property, scope);
                }

                @Override
                public void visitObjectDeclaration(JetObjectDeclaration declaration) {
                    PropertyDescriptor propertyDescriptor = classDescriptorResolver.resolveObjectDeclarationAsPropertyDescriptor(namespaceLike, declaration, typeHierarchyResolver.getObjects().get(declaration));
                    namespaceLike.addPropertyDescriptor(propertyDescriptor);
                }

                @Override
                public void visitEnumEntry(JetEnumEntry enumEntry) {
                    if (enumEntry.getPrimaryConstructorParameterList() == null) {
                        PropertyDescriptor propertyDescriptor = classDescriptorResolver.resolveObjectDeclarationAsPropertyDescriptor(namespaceLike, enumEntry, typeHierarchyResolver.getClasses().get(enumEntry));
                        MutableClassDescriptor classObjectDescriptor = ((MutableClassDescriptor) namespaceLike).getClassObjectDescriptor();
                        assert classObjectDescriptor != null;
                        classObjectDescriptor.addPropertyDescriptor(propertyDescriptor);
                    }
                }
            });
        }
    }

    private void processPrimaryConstructor(MutableClassDescriptor classDescriptor, JetClass klass) {
        if (!klass.hasPrimaryConstructor()) return;

        if (classDescriptor.getKind() == ClassKind.TRAIT) {
            trace.getErrorHandler().genericError(klass.getPrimaryConstructorParameterList().getNode(), "A trait may not have a constructor");
        }

        // TODO : not all the parameters are real properties
        JetScope memberScope = classDescriptor.getScopeForSupertypeResolution();
        ConstructorDescriptor constructorDescriptor = classDescriptorResolver.resolvePrimaryConstructorDescriptor(memberScope, classDescriptor, klass);
        for (JetParameter parameter : klass.getPrimaryConstructorParameters()) {
            PropertyDescriptor propertyDescriptor = classDescriptorResolver.resolvePrimaryConstructorParameterToAProperty(
                    classDescriptor,
                    memberScope,
                    parameter
            );
            classDescriptor.addPropertyDescriptor(propertyDescriptor);
            primaryConstructorParameterProperties.add(propertyDescriptor);
        }
        if (constructorDescriptor != null) {
            classDescriptor.setPrimaryConstructor(constructorDescriptor);
        }
    }

    private void processSecondaryConstructor(MutableClassDescriptor classDescriptor, JetConstructor constructor) {
        if (classDescriptor.getKind() == ClassKind.TRAIT) {
            trace.getErrorHandler().genericError(constructor.getNameNode(), "A trait may not have a constructor");
        }
        ConstructorDescriptor constructorDescriptor = classDescriptorResolver.resolveSecondaryConstructorDescriptor(
                classDescriptor.getScopeForMemberResolution(),
                classDescriptor,
                constructor);
        classDescriptor.addConstructor(constructorDescriptor);
        constructors.put(constructor, constructorDescriptor);
        declaringScopes.put(constructor, classDescriptor.getScopeForMemberLookup());
    }

    public Set<PropertyDescriptor> getPrimaryConstructorParameterProperties() {
        return primaryConstructorParameterProperties;
    }

    public Map<JetDeclaration, ConstructorDescriptor> getConstructors() {
        return constructors;
    }

    public Map<JetProperty, PropertyDescriptor> getProperties() {
        return properties;
    }

    public Map<JetDeclaration, JetScope> getDeclaringScopes() {
        return declaringScopes;
    }

    public Map<JetNamedFunction, FunctionDescriptorImpl> getFunctions() {
        return functions;
    }
}
