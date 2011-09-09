package org.jetbrains.jet.lang.resolve;

import com.google.common.collect.Maps;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.JetSemanticServices;
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

    private final BindingTrace trace;
    private final JetSemanticServices semanticServices;
    private final ClassDescriptorResolver classDescriptorResolver;

    private final Map<JetClass, MutableClassDescriptor> classes = Maps.newLinkedHashMap();
    private final Map<JetObjectDeclaration, MutableClassDescriptor> objects = Maps.newLinkedHashMap();
    protected final Map<JetNamespace, WritableScope> namespaceScopes = Maps.newHashMap();
    protected final Map<JetNamespace, NamespaceDescriptorImpl> namespaceDescriptors = Maps.newHashMap();

    public TypeHierarchyResolver(JetSemanticServices semanticServices, BindingTrace trace) {
        this.trace = trace;
        this.semanticServices = semanticServices;
        this.classDescriptorResolver = semanticServices.getClassDescriptorResolver(trace);
    }

    public Map<JetClass, MutableClassDescriptor> getClasses() {
        return classes;
    }

    public Map<JetObjectDeclaration, MutableClassDescriptor> getObjects() {
        return objects;
    }

    public Map<JetNamespace, WritableScope> getNamespaceScopes() {
        return namespaceScopes;
    }

    public Map<JetNamespace, NamespaceDescriptorImpl> getNamespaceDescriptors() {
        return namespaceDescriptors;
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
                        namespaceDescriptor.initialize(new WritableScopeImpl(JetScope.EMPTY, namespaceDescriptor, trace.getErrorHandler()).setDebugName("Namespace member scope"));
                        owner.addNamespace(namespaceDescriptor);
                        trace.record(BindingContext.NAMESPACE, namespace, namespaceDescriptor);
                    }
                    namespaceDescriptors.put(namespace, namespaceDescriptor);

                    WriteThroughScope namespaceScope = new WriteThroughScope(outerScope, namespaceDescriptor.getMemberScope(), trace.getErrorHandler());
                    namespaceScopes.put(namespace, namespaceScope);

                    processImports(namespace, namespaceScope, outerScope);

                    collectNamespacesAndClassifiers(namespaceScope, namespaceDescriptor, namespace.getDeclarations());
                }

                @Override
                public void visitClass(JetClass klass) {
                    MutableClassDescriptor mutableClassDescriptor = new MutableClassDescriptor(trace, owner, outerScope, getClassKind(klass));

                    if (klass.hasModifier(JetTokens.ENUM_KEYWORD)) {
                        MutableClassDescriptor classObjectDescriptor = new MutableClassDescriptor(trace, mutableClassDescriptor, outerScope, ClassKind.OBJECT);
                        classObjectDescriptor.setName("class-object-for-" + klass.getName());
                        classObjectDescriptor.setModality(Modality.FINAL);
                        classObjectDescriptor.createTypeConstructor();
                        createPrimaryConstructor(classObjectDescriptor);
                        mutableClassDescriptor.setClassObjectDescriptor(classObjectDescriptor);
                    }
                    visitClassOrObject(
                            klass,
                            (Map) classes,
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
                        objects.remove(enumEntry);
                        classes.put(enumEntry, classDescriptor);
                    }
                    else {
                        MutableClassDescriptor mutableClassDescriptor = new MutableClassDescriptor(trace, classObjectDescriptor, outerScope, ClassKind.CLASS); // TODO : Special kind for enum entry classes?
                        visitClassOrObject(
                                enumEntry,
                                (Map) classes,
                                classObjectDescriptor,
                                outerScope,
                                mutableClassDescriptor);
                        classObjectDescriptor.addClassifierDescriptor(mutableClassDescriptor);
                    }
                }

                private MutableClassDescriptor createClassDescriptorForObject(@NotNull JetClassOrObject declaration, @NotNull NamespaceLike owner) {
                    MutableClassDescriptor mutableClassDescriptor = new MutableClassDescriptor(trace, owner, outerScope, ClassKind.OBJECT) {
                        @Override
                        public ClassObjectStatus setClassObjectDescriptor(@NotNull MutableClassDescriptor classObjectDescriptor) {
                            return ClassObjectStatus.NOT_ALLOWED;
                        }
                    };
                    visitClassOrObject(declaration, (Map) objects, owner, outerScope, mutableClassDescriptor);
                    createPrimaryConstructor(mutableClassDescriptor);
                    trace.record(BindingContext.CLASS, declaration, mutableClassDescriptor);
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
                    trace.getErrorHandler().genericError(typedef.getNode(), "Unsupported [TopDownAnalyzer]");
                }

                @Override
                public void visitClassObject(JetClassObject classObject) {
                    JetObjectDeclaration objectDeclaration = classObject.getObjectDeclaration();
                    if (objectDeclaration != null) {
                        NamespaceLike.ClassObjectStatus status = owner.setClassObjectDescriptor(createClassDescriptorForObject(objectDeclaration, owner));
                        switch (status) {
                            case DUPLICATE:
                                trace.getErrorHandler().genericError(classObject.getNode(), "Only one class object is allowed per class");
                                break;
                            case NOT_ALLOWED:
                                trace.getErrorHandler().genericError(classObject.getNode(), "A class object is not allowed here");
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
                trace.getErrorHandler().genericError(namespace.getNode(), "Unsupported by TDA"); // TODO
                continue;
            }
            if (importDirective.isAllUnder()) {
                JetExpression importedReference = importDirective.getImportedReference();
                if (importedReference != null) {
                    JetTypeInferrer.Services typeInferrerServices = semanticServices.getTypeInferrerServices(trace, JetFlowInformationProvider.THROW_EXCEPTION);
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
                    JetType type = semanticServices.getTypeInferrerServices(trace, JetFlowInformationProvider.THROW_EXCEPTION).getTypeWithNamespaces(namespaceScope, reference.getReceiverExpression());
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
                    trace.record(BindingContext.REFERENCE_TARGET, referenceExpression, classifierDescriptor);

                    String aliasName = importDirective.getAliasName();
                    String importedClassifierName = aliasName != null ? aliasName : classifierDescriptor.getName();
                    namespaceScope.importClassifierAlias(importedClassifierName, classifierDescriptor);
                }
            }
        }
    }

    private void createTypeConstructors() {
        for (Map.Entry<JetClass, MutableClassDescriptor> entry : classes.entrySet()) {
            JetClass jetClass = entry.getKey();
            MutableClassDescriptor descriptor = entry.getValue();
            classDescriptorResolver.resolveMutableClassDescriptor(jetClass, descriptor);
            descriptor.createTypeConstructor();
        }
        for (Map.Entry<JetObjectDeclaration, MutableClassDescriptor> entry : objects.entrySet()) {
            MutableClassDescriptor descriptor = entry.getValue();
            descriptor.setModality(Modality.FINAL);
            descriptor.createTypeConstructor();
        }
    }

    private void resolveTypesInClassHeaders() {
        for (Map.Entry<JetClass, MutableClassDescriptor> entry : classes.entrySet()) {
            JetClass jetClass = entry.getKey();
            MutableClassDescriptor descriptor = entry.getValue();
            classDescriptorResolver.resolveGenericBounds(jetClass, descriptor.getScopeForSupertypeResolution(), descriptor.getTypeConstructor().getParameters());
            classDescriptorResolver.resolveSupertypes(jetClass, descriptor);
        }
        for (Map.Entry<JetObjectDeclaration, MutableClassDescriptor> entry : objects.entrySet()) {
            JetClassOrObject jetClass = entry.getKey();
            MutableClassDescriptor descriptor = entry.getValue();
            classDescriptorResolver.resolveSupertypes(jetClass, descriptor);
        }
    }

    private void checkTypesInClassHeaders() {
        for (Map.Entry<JetClass, MutableClassDescriptor> entry : classes.entrySet()) {
            JetClass jetClass = entry.getKey();

            for (JetDelegationSpecifier delegationSpecifier : jetClass.getDelegationSpecifiers()) {
                JetTypeReference typeReference = delegationSpecifier.getTypeReference();
                if (typeReference != null) {
                    JetType type = trace.getBindingContext().get(BindingContext.TYPE, typeReference);
                    if (type != null) {
                        classDescriptorResolver.checkBounds(typeReference, type);
                    }
                }
            }

            for (JetTypeParameter jetTypeParameter : jetClass.getTypeParameters()) {
                JetTypeReference extendsBound = jetTypeParameter.getExtendsBound();
                if (extendsBound != null) {
                    JetType type = trace.getBindingContext().get(BindingContext.TYPE, extendsBound);
                    if (type != null) {
                        classDescriptorResolver.checkBounds(extendsBound, type);
                    }
                }
            }

            for (JetTypeConstraint constraint : jetClass.getTypeConstaints()) {
                JetTypeReference extendsBound = constraint.getBoundTypeReference();
                if (extendsBound != null) {
                    JetType type = trace.getBindingContext().get(BindingContext.TYPE, extendsBound);
                    if (type != null) {
                        classDescriptorResolver.checkBounds(extendsBound, type);
                    }
                }
            }
        }
    }



}
