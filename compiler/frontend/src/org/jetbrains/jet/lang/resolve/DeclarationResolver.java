package org.jetbrains.jet.lang.resolve;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.resolve.scopes.WritableScope;

import java.util.List;
import java.util.Map;

import static org.jetbrains.jet.lang.diagnostics.Errors.CONSTRUCTOR_IN_TRAIT;
import static org.jetbrains.jet.lang.resolve.BindingContext.ANNOTATION;

/**
* @author abreslav
*/
public class DeclarationResolver {
    private final AnnotationResolver annotationResolver;
    private final TopDownAnalysisContext context;

    public DeclarationResolver(TopDownAnalysisContext context) {
        this.context = context;
        this.annotationResolver = new AnnotationResolver(context.getSemanticServices(), context.getTrace());
    }

    public void process() {
        resolveConstructorHeaders();
        resolveAnnotationStubsOnClassesAndConstructors();
        resolveFunctionAndPropertyHeaders();
    }

    private void resolveConstructorHeaders() {
        for (Map.Entry<JetClass, MutableClassDescriptor> entry : context.getClasses().entrySet()) {
            JetClass jetClass = entry.getKey();
            MutableClassDescriptor classDescriptor = entry.getValue();

            processPrimaryConstructor(classDescriptor, jetClass);
            for (JetConstructor jetConstructor : jetClass.getSecondaryConstructors()) {
                processSecondaryConstructor(classDescriptor, jetConstructor);
            }
        }
    }

    private void resolveAnnotationStubsOnClassesAndConstructors() {
        for (Map.Entry<JetClass, MutableClassDescriptor> entry : context.getClasses().entrySet()) {
            JetClass jetClass = entry.getKey();
            MutableClassDescriptor mutableClassDescriptor = entry.getValue();

            JetModifierList modifierList = jetClass.getModifierList();
            if (modifierList != null) {
                List<JetAnnotationEntry> annotationEntries = modifierList.getAnnotationEntries();
                for (JetAnnotationEntry annotationEntry : annotationEntries) {
                    AnnotationDescriptor annotationDescriptor = context.getTrace().get(ANNOTATION, annotationEntry);
                    if (annotationDescriptor != null) {
                        annotationResolver.resolveAnnotationStub(mutableClassDescriptor.getScopeForSupertypeResolution(), annotationEntry, annotationDescriptor);
                    }
                }
            }
        }
    }

    private void resolveFunctionAndPropertyHeaders() {
        for (Map.Entry<JetNamespace, WritableScope> entry : context.getNamespaceScopes().entrySet()) {
            JetNamespace namespace = entry.getKey();
            WritableScope namespaceScope = entry.getValue();
            NamespaceLike namespaceDescriptor = context.getNamespaceDescriptors().get(namespace);

            resolveFunctionAndPropertyHeaders(namespace.getDeclarations(), namespaceScope, namespaceDescriptor);
        }
        for (Map.Entry<JetClass, MutableClassDescriptor> entry : context.getClasses().entrySet()) {
            JetClass jetClass = entry.getKey();
            MutableClassDescriptor classDescriptor = entry.getValue();

            resolveFunctionAndPropertyHeaders(jetClass.getDeclarations(), classDescriptor.getScopeForMemberResolution(), classDescriptor);
//            processPrimaryConstructor(classDescriptor, jetClass);
//            for (JetConstructor jetConstructor : jetClass.getSecondaryConstructors()) {
//                processSecondaryConstructor(classDescriptor, jetConstructor);
//            }
        }
        for (Map.Entry<JetObjectDeclaration, MutableClassDescriptor> entry : context.getObjects().entrySet()) {
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
                    FunctionDescriptorImpl functionDescriptor = context.getClassDescriptorResolver().resolveFunctionDescriptor(namespaceLike, scope, function);
                    namespaceLike.addFunctionDescriptor(functionDescriptor);
                    context.getFunctions().put(function, functionDescriptor);
                    context.getDeclaringScopes().put(function, scope);
                }

                @Override
                public void visitProperty(JetProperty property) {
                    PropertyDescriptor propertyDescriptor = context.getClassDescriptorResolver().resolvePropertyDescriptor(namespaceLike, scope, property);
                    namespaceLike.addPropertyDescriptor(propertyDescriptor);
                    context.getProperties().put(property, propertyDescriptor);
                    context.getDeclaringScopes().put(property, scope);
                }

                @Override
                public void visitObjectDeclaration(JetObjectDeclaration declaration) {
                    PropertyDescriptor propertyDescriptor = context.getClassDescriptorResolver().resolveObjectDeclarationAsPropertyDescriptor(namespaceLike, declaration, context.getObjects().get(declaration));
                    namespaceLike.addPropertyDescriptor(propertyDescriptor);
                }

                @Override
                public void visitEnumEntry(JetEnumEntry enumEntry) {
                    if (enumEntry.getPrimaryConstructorParameterList() == null) {
                        PropertyDescriptor propertyDescriptor = context.getClassDescriptorResolver().resolveObjectDeclarationAsPropertyDescriptor(namespaceLike, enumEntry, context.getClasses().get(enumEntry));
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
//           context.getTrace().getErrorHandler().genericError(klass.getPrimaryConstructorParameterList().getNode(), "A trait may not have a constructor");
            context.getTrace().report(CONSTRUCTOR_IN_TRAIT.on(klass.getPrimaryConstructorParameterList()));
        }

        // TODO : not all the parameters are real properties
        JetScope memberScope = classDescriptor.getScopeForSupertypeResolution();
        ConstructorDescriptor constructorDescriptor = context.getClassDescriptorResolver().resolvePrimaryConstructorDescriptor(memberScope, classDescriptor, klass);
        for (JetParameter parameter : klass.getPrimaryConstructorParameters()) {
            if (parameter.getValOrVarNode() != null) {
                PropertyDescriptor propertyDescriptor = context.getClassDescriptorResolver().resolvePrimaryConstructorParameterToAProperty(
                        classDescriptor,
                        memberScope,
                        parameter
                );
                classDescriptor.addPropertyDescriptor(propertyDescriptor);
                context.getPrimaryConstructorParameterProperties().add(propertyDescriptor);
            }
        }
        if (constructorDescriptor != null) {
            classDescriptor.setPrimaryConstructor(constructorDescriptor);
        }
    }

    private void processSecondaryConstructor(MutableClassDescriptor classDescriptor, JetConstructor constructor) {
        if (classDescriptor.getKind() == ClassKind.TRAIT) {
//            context.getTrace().getErrorHandler().genericError(constructor.getNameNode(), "A trait may not have a constructor");
            context.getTrace().report(CONSTRUCTOR_IN_TRAIT.on(constructor.getNameNode()));
        }
        ConstructorDescriptor constructorDescriptor = context.getClassDescriptorResolver().resolveSecondaryConstructorDescriptor(
                classDescriptor.getScopeForMemberResolution(),
                classDescriptor,
                constructor);
        classDescriptor.addConstructor(constructorDescriptor);
        context.getConstructors().put(constructor, constructorDescriptor);
        context.getDeclaringScopes().put(constructor, classDescriptor.getScopeForMemberLookup());
    }

}
