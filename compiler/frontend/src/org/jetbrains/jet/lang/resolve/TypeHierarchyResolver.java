package org.jetbrains.jet.lang.resolve;

import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiNameIdentifierOwner;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.cfg.JetFlowInformationProvider;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.types.*;
import org.jetbrains.jet.lexer.JetTokens;

import java.util.*;

import static org.jetbrains.jet.lang.resolve.BindingContext.DESCRIPTOR_TO_DECLARATION;
import static org.jetbrains.jet.lang.resolve.BindingContext.TYPE;

/**
 * @author abreslav
 */
public class TypeHierarchyResolver {
    private final TopDownAnalysisContext context;
    private LinkedList<MutableClassDescriptor> topologicalOrder;


    public TypeHierarchyResolver(TopDownAnalysisContext context) {
        this.context = context;
    }

    public void process(@NotNull JetScope outerScope, NamespaceLike owner, @NotNull List<JetDeclaration> declarations) {
        collectNamespacesAndClassifiers(outerScope, owner, declarations); // namespaceScopes, classes

        createTypeConstructors(); // create type constructors for classes and generic parameters, supertypes are not filled in
        resolveTypesInClassHeaders(); // Generic bounds and types in supertype lists (no expressions or constructor resolution)

        topologicalOrder = topologicallySortClassesAndObjects();

        // Detect and disconnect all loops in the hierarchy
        detectAndDisconnectLoops();

        // At this point, there are no loops in the type hierarchy

        checkSupertypesForConsistency();
//        computeSuperclasses();

        // Add supertypes to resolution scopes of classes
        addSupertypesToScopes();

        checkTypesInClassHeaders(); // Check bounds in the types used in generic bounds and supertype lists
    }

    private void collectNamespacesAndClassifiers(
            @NotNull final JetScope outerScope,
            @NotNull final NamespaceLike owner,
            @NotNull Collection<JetDeclaration> declarations) {
        for (JetDeclaration declaration : declarations) {
            declaration.accept(new JetVisitorVoid() {
                @Override
                public void visitNamespace(JetNamespace namespace) {
                    String name = JetPsiUtil.safeName(namespace.getName());

                    NamespaceDescriptorImpl namespaceDescriptor = owner.getNamespace(name);
                    if (namespaceDescriptor == null) {
                        namespaceDescriptor = new NamespaceDescriptorImpl(
                                owner.getOriginal(),
                                Collections.<AnnotationDescriptor>emptyList(), // TODO: annotations
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

    private LinkedList<MutableClassDescriptor> topologicallySortClassesAndObjects() {
        // A topsort is needed only for better diagnostics:
        //    edges that get removed to disconnect loops are more reasonable in this case
        LinkedList<MutableClassDescriptor> topologicalOrder = Lists.newLinkedList();
        Set<ClassDescriptor> visited = Sets.newHashSet();
        for (MutableClassDescriptor mutableClassDescriptor : context.getClasses().values()) {
            topologicallySort(mutableClassDescriptor, visited, topologicalOrder);
        }
        for (MutableClassDescriptor mutableClassDescriptor : context.getObjects().values()) {
            topologicallySort(mutableClassDescriptor, visited, topologicalOrder);
        }
        return topologicalOrder;
    }

    private void detectAndDisconnectLoops() {
        // Loop detection and disconnection
        Set<ClassDescriptor> visited = Sets.newHashSet();
        Set<ClassDescriptor> beingProcessed = Sets.newHashSet();
        List<ClassDescriptor> currentPath = Lists.newArrayList();
        for (MutableClassDescriptor mutableClassDescriptor : topologicalOrder) {
            traverseTypeHierarchy(mutableClassDescriptor, visited, beingProcessed, currentPath);
        }
    }

    private static void topologicallySort(MutableClassDescriptor mutableClassDescriptor, Set<ClassDescriptor> visited, LinkedList<MutableClassDescriptor> topologicalOrder) {
        if (!visited.add(mutableClassDescriptor)) {
            return;
        }
        for (JetType supertype : mutableClassDescriptor.getSupertypes()) {
            DeclarationDescriptor declarationDescriptor = supertype.getConstructor().getDeclarationDescriptor();
            if (declarationDescriptor instanceof MutableClassDescriptor) {
                MutableClassDescriptor classDescriptor = (MutableClassDescriptor) declarationDescriptor;
                topologicallySort(classDescriptor, visited, topologicalOrder);
            }
        }
        topologicalOrder.addFirst(mutableClassDescriptor);
    }

    private void traverseTypeHierarchy(MutableClassDescriptor currentClass, Set<ClassDescriptor> visited, Set<ClassDescriptor> beingProcessed, List<ClassDescriptor> currentPath) {
        if (!visited.add(currentClass)) {
            if (beingProcessed.contains(currentClass)) {
                markCycleErrors(currentPath, currentClass);
                assert !currentPath.isEmpty() : "Cycle cannot be found on an empty currentPath";
                ClassDescriptor subclassOfCurrent = currentPath.get(currentPath.size() - 1);
                assert subclassOfCurrent instanceof MutableClassDescriptor;
                // Disconnect the loop
                for (Iterator<JetType> iterator = ((MutableClassDescriptor) subclassOfCurrent).getSupertypes().iterator(); iterator.hasNext(); ) {
                    JetType type = iterator.next();
                    if (type.getConstructor() == currentClass.getTypeConstructor()) {
                        iterator.remove();
                        break;
                    }
                }
            }
            return;
        }

        beingProcessed.add(currentClass);
        currentPath.add(currentClass);
        for (JetType supertype : Lists.newArrayList(currentClass.getSupertypes())) {
            DeclarationDescriptor declarationDescriptor = supertype.getConstructor().getDeclarationDescriptor();
            if (declarationDescriptor instanceof MutableClassDescriptor) {
                MutableClassDescriptor mutableClassDescriptor = (MutableClassDescriptor) declarationDescriptor;
                traverseTypeHierarchy(mutableClassDescriptor, visited, beingProcessed, currentPath);
            }
        }
        beingProcessed.remove(currentClass);
        currentPath.remove(currentPath.size() - 1);
    }

    private void markCycleErrors(List<ClassDescriptor> currentPath, @NotNull ClassDescriptor current) {
        int size = currentPath.size();
        boolean found = false;
        for (int i = 0; i < size; i++) {
            ClassDescriptor classDescriptor = currentPath.get(i);
            if (classDescriptor == current) found = true;
            if (!found) continue;

            ClassDescriptor superclass = (i < size - 1) ? currentPath.get(i + 1) : current;
            PsiElement psiElement = context.getTrace().get(DESCRIPTOR_TO_DECLARATION, classDescriptor);

            ASTNode node = null;
            if (psiElement instanceof JetClassOrObject) {
                JetClassOrObject classOrObject = (JetClassOrObject) psiElement;
                for (JetDelegationSpecifier delegationSpecifier : classOrObject.getDelegationSpecifiers()) {
                    JetTypeReference typeReference = delegationSpecifier.getTypeReference();
                    if (typeReference == null) continue;
                    JetType supertype = context.getTrace().get(TYPE, typeReference);
                    if (supertype != null && supertype.getConstructor() == superclass.getTypeConstructor()) {
                        node = typeReference.getNode();
                    }
                }
            }
            if (node == null && psiElement instanceof PsiNameIdentifierOwner) {
                PsiNameIdentifierOwner namedElement = (PsiNameIdentifierOwner) psiElement;
                PsiElement nameIdentifier = namedElement.getNameIdentifier();
                if (nameIdentifier != null) {
                    node = nameIdentifier.getNode();
                }
            }
            if (node != null) {
                context.getTrace().getErrorHandler().genericError(node, "There's a cycle in the inheritance hierarchy for this type");
            }
        }
    }

    private void checkSupertypesForConsistency() {
        for (MutableClassDescriptor mutableClassDescriptor : topologicalOrder) {
            Multimap<TypeConstructor, TypeProjection> multimap = TypeUtils.buildDeepSubstitutionMultimap(mutableClassDescriptor.getDefaultType());
            for (Map.Entry<TypeConstructor, Collection<TypeProjection>> entry : multimap.asMap().entrySet()) {
                Collection<TypeProjection> projections = entry.getValue();
                if (projections.size() > 1) {
                    TypeConstructor typeConstructor = entry.getKey();
                    DeclarationDescriptor declarationDescriptor = typeConstructor.getDeclarationDescriptor();
                    assert declarationDescriptor instanceof TypeParameterDescriptor : declarationDescriptor;
                    TypeParameterDescriptor typeParameterDescriptor = (TypeParameterDescriptor) declarationDescriptor;

                    // Immediate arguments of supertypes cannot be projected
                    Set<JetType> conflictingTypes = Sets.newLinkedHashSet();
                    for (TypeProjection projection : projections) {
                        conflictingTypes.add(projection.getType());
                    }
                    switch (typeParameterDescriptor.getVariance()) {
                        case INVARIANT:
                            // Leave conflicting types as is
                            break;
                        case IN_VARIANCE:
                            // Filter out those who have supertypes in this set (common supertype)
                            Filter.REMOVE_IF_SUPERTYPE_IN_THE_SET.proceed(conflictingTypes);
                            break;
                        case OUT_VARIANCE:
                            // Filter out those who have subtypes in this set (common subtype)
                            Filter.REMOVE_IF_SUBTYPE_IN_THE_SET.proceed(conflictingTypes);
                            break;
                    }
                    
                    if (conflictingTypes.size() > 1) {
                        DeclarationDescriptor containingDeclaration = typeParameterDescriptor.getContainingDeclaration();
                        assert containingDeclaration instanceof ClassDescriptor : containingDeclaration;
                        PsiElement psiElement = context.getTrace().get(DESCRIPTOR_TO_DECLARATION, mutableClassDescriptor);
                        assert psiElement instanceof JetClassOrObject : psiElement;
                        JetClassOrObject declaration = (JetClassOrObject) psiElement;
                        JetDelegationSpecifierList delegationSpecifierList = declaration.getDelegationSpecifierList();
                        assert delegationSpecifierList != null;
                        context.getTrace().getErrorHandler().genericError(delegationSpecifierList.getNode(), "Type parameter " + typeParameterDescriptor.getName() + " of " + containingDeclaration.getName() + " has inconsistent values: " + conflictingTypes);
                    }
                }
            }
        }
    }

    private enum Filter {
        REMOVE_IF_SUBTYPE_IN_THE_SET {
            @Override
            public boolean removeNeeded(JetType subject, JetType other) {
                return JetTypeChecker.INSTANCE.isSubtypeOf(other, subject);
            }
        },
        REMOVE_IF_SUPERTYPE_IN_THE_SET {
            @Override
            public boolean removeNeeded(JetType subject, JetType other) {
                return JetTypeChecker.INSTANCE.isSubtypeOf(subject, other);
            }
        };
        
        private void proceed(Set<JetType> conflictingTypes) {
            for (Iterator<JetType> iterator = conflictingTypes.iterator(); iterator.hasNext(); ) {
                JetType type = iterator.next();
                for (JetType otherType : conflictingTypes) {
                    boolean subtypeOf = removeNeeded(type, otherType);
                    if (type != otherType && subtypeOf) {
                        iterator.remove();
                        break;
                    }
                }
            }
        }

        public abstract boolean removeNeeded(JetType subject, JetType other);
    }
    
    private void addSupertypesToScopes() {
        for (MutableClassDescriptor mutableClassDescriptor : context.getClasses().values()) {
            mutableClassDescriptor.addSupertypesToScopeForMemberLookup();
        }
        for (MutableClassDescriptor mutableClassDescriptor : context.getObjects().values()) {
            mutableClassDescriptor.addSupertypesToScopeForMemberLookup();
        }
    }

    private void checkTypesInClassHeaders() {
        for (Map.Entry<JetClass, MutableClassDescriptor> entry : context.getClasses().entrySet()) {
            JetClass jetClass = entry.getKey();

            for (JetDelegationSpecifier delegationSpecifier : jetClass.getDelegationSpecifiers()) {
                JetTypeReference typeReference = delegationSpecifier.getTypeReference();
                if (typeReference != null) {
                    JetType type = context.getTrace().getBindingContext().get(TYPE, typeReference);
                    if (type != null) {
                        context.getClassDescriptorResolver().checkBounds(typeReference, type);
                    }
                }
            }

            for (JetTypeParameter jetTypeParameter : jetClass.getTypeParameters()) {
                JetTypeReference extendsBound = jetTypeParameter.getExtendsBound();
                if (extendsBound != null) {
                    JetType type = context.getTrace().getBindingContext().get(TYPE, extendsBound);
                    if (type != null) {
                        context.getClassDescriptorResolver().checkBounds(extendsBound, type);
                    }
                }
            }

            for (JetTypeConstraint constraint : jetClass.getTypeConstaints()) {
                JetTypeReference extendsBound = constraint.getBoundTypeReference();
                if (extendsBound != null) {
                    JetType type = context.getTrace().getBindingContext().get(TYPE, extendsBound);
                    if (type != null) {
                        context.getClassDescriptorResolver().checkBounds(extendsBound, type);
                    }
                }
            }
        }
    }
}
