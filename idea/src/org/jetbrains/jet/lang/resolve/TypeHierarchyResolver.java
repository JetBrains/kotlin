package org.jetbrains.jet.lang.resolve;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.cfg.JetFlowInformationProvider;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.JetTypeInferrer;
import org.jetbrains.jet.lexer.JetTokens;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author abreslav
 */
public class TypeHierarchyResolver {
    private final TopDownAnalysisContext context;

    public TypeHierarchyResolver(TopDownAnalysisContext context) {
        this.context = context;
    }

    public void process(@NotNull JetScope outerScope, NamespaceLike owner, @NotNull List<JetDeclaration> declarations) {
        collectNamespacesAndClassifiers(outerScope, owner, declarations); // namespaceScopes, classes

        createTypeConstructors(); // create type constructors for classes and generic parameters
        resolveTypesInClassHeaders(); // Generic bounds and types in supertype lists (no expressions or constructor resolution)
        checkTypesInClassHeaders(); // Generic bounds and supertype lists
    }

    private void collectNamespacesAndClassifiers(
            @NotNull final JetScope outerScope,
            @NotNull final NamespaceLike owner,
            @NotNull Collection<JetDeclaration> declarations) {
        for (JetDeclaration declaration : declarations) {
            declaration.accept(new JetVisitorVoid() {
                @Override
                public void visitNamespace(JetNamespace namespace) {

                    String name = namespace.getName();
                    if (name == null) {
                        name = "<no name provided>";
                    }
                    NamespaceDescriptorImpl namespaceDescriptor = owner.getNamespace(name);
                    if (namespaceDescriptor == null) {
                        namespaceDescriptor = new NamespaceDescriptorImpl(
                                owner.getOriginal(),
                                Collections.<AnnotationDescriptor>emptyList(), // TODO
                                name
                        );
                        namespaceDescriptor.initialize(new WritableScopeImpl(JetScope.EMPTY, namespaceDescriptor, context.getTrace().getErrorHandler()).setDebugName("Namespace member scope"));
                        owner.addNamespace(namespaceDescriptor);
                        context.getTrace().record(BindingContext.NAMESPACE, namespace, namespaceDescriptor);
                    }
                    context.getNamespaceDescriptors().put(namespace, namespaceDescriptor);

                    WriteThroughScope namespaceScope = new WriteThroughScope(outerScope, namespaceDescriptor.getMemberScope(), context.getTrace().getErrorHandler());
                    context.getNamespaceScopes().put(namespace, namespaceScope);

                    processImports(namespace, namespaceScope, outerScope);

                    collectNamespacesAndClassifiers(namespaceScope, namespaceDescriptor, namespace.getDeclarations());
                }

                @Override
                public void visitClass(JetClass klass) {
                    MutableClassDescriptor mutableClassDescriptor = new MutableClassDescriptor(context.getTrace(), owner, outerScope, getClassKind(klass));

                    if (klass.hasModifier(JetTokens.ENUM_KEYWORD)) {
                        MutableClassDescriptor classObjectDescriptor = new MutableClassDescriptor(context.getTrace(), mutableClassDescriptor, outerScope, ClassKind.OBJECT);
                        classObjectDescriptor.setName("class-object-for-" + klass.getName());
                        classObjectDescriptor.setModality(Modality.FINAL);
                        classObjectDescriptor.createTypeConstructor();
                        createPrimaryConstructor(classObjectDescriptor);
                        mutableClassDescriptor.setClassObjectDescriptor(classObjectDescriptor);
                    }
                    visitClassOrObject(
                            klass,
                            (Map) context.getClasses(),
                            owner,
                            outerScope,
                            mutableClassDescriptor);
                    owner.addClassifierDescriptor(mutableClassDescriptor);
                }

                @Override
                public void visitObjectDeclaration(JetObjectDeclaration declaration) {
                    createClassDescriptorForObject(declaration, owner);
                }

                @Override
                public void visitEnumEntry(JetEnumEntry enumEntry) {
                    MutableClassDescriptor classObjectDescriptor = ((MutableClassDescriptor) owner).getClassObjectDescriptor();
                    assert classObjectDescriptor != null : enumEntry.getParent().getText();
                    if (enumEntry.getPrimaryConstructorParameterList() == null) {
                        MutableClassDescriptor classDescriptor = createClassDescriptorForObject(enumEntry, classObjectDescriptor);
                        context.getObjects().remove(enumEntry);
                        context.getClasses().put(enumEntry, classDescriptor);
                    }
                    else {
                        MutableClassDescriptor mutableClassDescriptor = new MutableClassDescriptor(context.getTrace(), classObjectDescriptor, outerScope, ClassKind.CLASS); // TODO : Special kind for enum entry classes?
                        visitClassOrObject(
                                enumEntry,
                                (Map) context.getClasses(),
                                classObjectDescriptor,
                                outerScope,
                                mutableClassDescriptor);
                        classObjectDescriptor.addClassifierDescriptor(mutableClassDescriptor);
                    }
                }

                private MutableClassDescriptor createClassDescriptorForObject(@NotNull JetClassOrObject declaration, @NotNull NamespaceLike owner) {
                    MutableClassDescriptor mutableClassDescriptor = new MutableClassDescriptor(context.getTrace(), owner, outerScope, ClassKind.OBJECT) {
                        @Override
                        public ClassObjectStatus setClassObjectDescriptor(@NotNull MutableClassDescriptor classObjectDescriptor) {
                            return ClassObjectStatus.NOT_ALLOWED;
                        }
                    };
                    visitClassOrObject(declaration, (Map) context.getObjects(), owner, outerScope, mutableClassDescriptor);
                    createPrimaryConstructor(mutableClassDescriptor);
                    context.getTrace().record(BindingContext.CLASS, declaration, mutableClassDescriptor);
                    return mutableClassDescriptor;
                }

                private void createPrimaryConstructor(MutableClassDescriptor mutableClassDescriptor) {
                    ConstructorDescriptorImpl constructorDescriptor = new ConstructorDescriptorImpl(mutableClassDescriptor, Collections.<AnnotationDescriptor>emptyList(), true);
                    constructorDescriptor.initialize(Collections.<TypeParameterDescriptor>emptyList(), Collections.<ValueParameterDescriptor>emptyList(),
                                                     Modality.FINAL);
                    // TODO : make the constructor private?
                    mutableClassDescriptor.setPrimaryConstructor(constructorDescriptor);
                }

                private void visitClassOrObject(@NotNull JetClassOrObject declaration, Map<JetClassOrObject, MutableClassDescriptor> map, NamespaceLike owner, JetScope outerScope, MutableClassDescriptor mutableClassDescriptor) {
                    mutableClassDescriptor.setName(JetPsiUtil.safeName(declaration.getName()));

                    map.put(declaration, mutableClassDescriptor);
//                    declaringScopes.put((JetDeclaration) declaration, outerScope);

                    JetScope classScope = mutableClassDescriptor.getScopeForMemberResolution();
                    collectNamespacesAndClassifiers(classScope, mutableClassDescriptor, declaration.getDeclarations());
                }

                @Override
                public void visitTypedef(JetTypedef typedef) {
                    context.getTrace().getErrorHandler().genericError(typedef.getNode(), "Unsupported [TopDownAnalyzer]");
                }

                @Override
                public void visitClassObject(JetClassObject classObject) {
                    JetObjectDeclaration objectDeclaration = classObject.getObjectDeclaration();
                    if (objectDeclaration != null) {
                        NamespaceLike.ClassObjectStatus status = owner.setClassObjectDescriptor(createClassDescriptorForObject(objectDeclaration, owner));
                        switch (status) {
                            case DUPLICATE:
                                context.getTrace().getErrorHandler().genericError(classObject.getNode(), "Only one class object is allowed per class");
                                break;
                            case NOT_ALLOWED:
                                context.getTrace().getErrorHandler().genericError(classObject.getNode(), "A class object is not allowed here");
                                break;
                        }
                    }
                }
            });
        }
    }

    @NotNull
    private ClassKind getClassKind(@NotNull JetClass jetClass) {
        if (jetClass.isTrait()) return ClassKind.TRAIT;
        if (jetClass.hasModifier(JetTokens.ANNOTATION_KEYWORD)) return ClassKind.ANNOTATION_CLASS;
        if (jetClass.hasModifier(JetTokens.ENUM_KEYWORD)) return ClassKind.ENUM_CLASS;
        return ClassKind.CLASS;
    }

    private void processImports(@NotNull JetNamespace namespace, @NotNull WriteThroughScope namespaceScope, @NotNull JetScope outerScope) {
        List<JetImportDirective> importDirectives = namespace.getImportDirectives();
        for (JetImportDirective importDirective : importDirectives) {
            if (importDirective.isAbsoluteInRootNamespace()) {
                context.getTrace().getErrorHandler().genericError(namespace.getNode(), "Unsupported by TDA"); // TODO
                continue;
            }
            if (importDirective.isAllUnder()) {
                JetExpression importedReference = importDirective.getImportedReference();
                if (importedReference != null) {
                    JetTypeInferrer.Services typeInferrerServices = context.getSemanticServices().getTypeInferrerServices(context.getTrace(), JetFlowInformationProvider.THROW_EXCEPTION);
                    JetType type = typeInferrerServices.getTypeWithNamespaces(namespaceScope, importedReference);
                    if (type != null) {
                        namespaceScope.importScope(type.getMemberScope());
                    }
                }
            }
            else {
                ClassifierDescriptor classifierDescriptor = null;
                JetSimpleNameExpression referenceExpression = null;

                JetExpression importedReference = importDirective.getImportedReference();
                if (importedReference instanceof JetDotQualifiedExpression) {
                    JetDotQualifiedExpression reference = (JetDotQualifiedExpression) importedReference;
                    JetType type = context.getSemanticServices().getTypeInferrerServices(context.getTrace(), JetFlowInformationProvider.THROW_EXCEPTION).getTypeWithNamespaces(namespaceScope, reference.getReceiverExpression());
                    JetExpression selectorExpression = reference.getSelectorExpression();
                    if (selectorExpression != null) {
                        referenceExpression = (JetSimpleNameExpression) selectorExpression;
                        String referencedName = referenceExpression.getReferencedName();
                        if (type != null && referencedName != null) {
                            classifierDescriptor = type.getMemberScope().getClassifier(referencedName);
                        }
                    }
                }
                else {
                    assert importedReference instanceof JetSimpleNameExpression;
                    referenceExpression = (JetSimpleNameExpression) importedReference;

                    String referencedName = referenceExpression.getReferencedName();
                    if (referencedName != null) {
                        classifierDescriptor = outerScope.getClassifier(referencedName);
                    }
                }

                if (classifierDescriptor != null) {
                    context.getTrace().record(BindingContext.REFERENCE_TARGET, referenceExpression, classifierDescriptor);

                    String aliasName = importDirective.getAliasName();
                    String importedClassifierName = aliasName != null ? aliasName : classifierDescriptor.getName();
                    namespaceScope.importClassifierAlias(importedClassifierName, classifierDescriptor);
                }
            }
        }
    }

    private void createTypeConstructors() {
        for (Map.Entry<JetClass, MutableClassDescriptor> entry : context.getClasses().entrySet()) {
            JetClass jetClass = entry.getKey();
            MutableClassDescriptor descriptor = entry.getValue();
            context.getClassDescriptorResolver().resolveMutableClassDescriptor(jetClass, descriptor);
            descriptor.createTypeConstructor();
        }
        for (Map.Entry<JetObjectDeclaration, MutableClassDescriptor> entry : context.getObjects().entrySet()) {
            MutableClassDescriptor descriptor = entry.getValue();
            descriptor.setModality(Modality.FINAL);
            descriptor.createTypeConstructor();
        }
    }

    private void resolveTypesInClassHeaders() {
        for (Map.Entry<JetClass, MutableClassDescriptor> entry : context.getClasses().entrySet()) {
            JetClass jetClass = entry.getKey();
            MutableClassDescriptor descriptor = entry.getValue();
            context.getClassDescriptorResolver().resolveGenericBounds(jetClass, descriptor.getScopeForSupertypeResolution(), descriptor.getTypeConstructor().getParameters());
            context.getClassDescriptorResolver().resolveSupertypes(jetClass, descriptor);
        }
        for (Map.Entry<JetObjectDeclaration, MutableClassDescriptor> entry : context.getObjects().entrySet()) {
            JetClassOrObject jetClass = entry.getKey();
            MutableClassDescriptor descriptor = entry.getValue();
            context.getClassDescriptorResolver().resolveSupertypes(jetClass, descriptor);
        }
    }

    private void checkTypesInClassHeaders() {
        for (Map.Entry<JetClass, MutableClassDescriptor> entry : context.getClasses().entrySet()) {
            JetClass jetClass = entry.getKey();

            for (JetDelegationSpecifier delegationSpecifier : jetClass.getDelegationSpecifiers()) {
                JetTypeReference typeReference = delegationSpecifier.getTypeReference();
                if (typeReference != null) {
                    JetType type = context.getTrace().getBindingContext().get(BindingContext.TYPE, typeReference);
                    if (type != null) {
                        context.getClassDescriptorResolver().checkBounds(typeReference, type);
                    }
                }
            }

            for (JetTypeParameter jetTypeParameter : jetClass.getTypeParameters()) {
                JetTypeReference extendsBound = jetTypeParameter.getExtendsBound();
                if (extendsBound != null) {
                    JetType type = context.getTrace().getBindingContext().get(BindingContext.TYPE, extendsBound);
                    if (type != null) {
                        context.getClassDescriptorResolver().checkBounds(extendsBound, type);
                    }
                }
            }

            for (JetTypeConstraint constraint : jetClass.getTypeConstaints()) {
                JetTypeReference extendsBound = constraint.getBoundTypeReference();
                if (extendsBound != null) {
                    JetType type = context.getTrace().getBindingContext().get(BindingContext.TYPE, extendsBound);
                    if (type != null) {
                        context.getClassDescriptorResolver().checkBounds(extendsBound, type);
                    }
                }
            }
        }
    }



}
