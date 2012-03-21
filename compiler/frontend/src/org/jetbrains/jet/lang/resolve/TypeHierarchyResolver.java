/*
 * Copyright 2010-2012 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.jet.lang.resolve;

import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiNameIdentifierOwner;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.resolve.scopes.WritableScope;
import org.jetbrains.jet.lang.resolve.scopes.WriteThroughScope;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.TypeConstructor;
import org.jetbrains.jet.lang.types.TypeProjection;
import org.jetbrains.jet.lang.types.TypeUtils;
import org.jetbrains.jet.lang.types.checker.JetTypeChecker;
import org.jetbrains.jet.lexer.JetTokens;

import javax.inject.Inject;
import java.util.*;

import static org.jetbrains.jet.lang.diagnostics.Errors.*;
import static org.jetbrains.jet.lang.resolve.BindingContext.*;

/**
 * @author abreslav
 */
public class TypeHierarchyResolver {
    @NotNull
    private TopDownAnalysisContext context;
    @NotNull
    private ImportsResolver importsResolver;
    @NotNull
    private DescriptorResolver descriptorResolver;
    @NotNull
    private NamespaceFactoryImpl namespaceFactory;
    @NotNull
    private BindingTrace trace;

    // state
    private LinkedList<MutableClassDescriptor> topologicalOrder;

    @Inject
    public void setContext(@NotNull TopDownAnalysisContext context) {
        this.context = context;
    }

    @Inject
    public void setImportsResolver(@NotNull ImportsResolver importsResolver) {
        this.importsResolver = importsResolver;
    }

    @Inject
    public void setDescriptorResolver(@NotNull DescriptorResolver descriptorResolver) {
        this.descriptorResolver = descriptorResolver;
    }

    @Inject
    public void setNamespaceFactory(@NotNull NamespaceFactoryImpl namespaceFactory) {
        this.namespaceFactory = namespaceFactory;
    }

    @Inject
    public void setTrace(@NotNull ObservableBindingTrace trace) {
        this.trace = trace;
    }



    public void process(@NotNull JetScope outerScope, @NotNull NamespaceLikeBuilder owner, @NotNull Collection<? extends PsiElement> declarations) {
        collectNamespacesAndClassifiers(outerScope, outerScope, owner, declarations); // namespaceScopes, classes

        importsResolver.processTypeImports();

        createTypeConstructors(); // create type constructors for classes and generic parameters, supertypes are not filled in
        resolveTypesInClassHeaders(); // Generic bounds and types in supertype lists (no expressions or constructor resolution)

        topologicalOrder = topologicallySortClassesAndObjects();

        // Detect and disconnect all loops in the hierarchy
        detectAndDisconnectLoops();

        // At this point, there are no loops in the type hierarchy

        checkSupertypesForConsistency();
//        computeSuperclasses();

        checkTypesInClassHeaders(); // Check bounds in the types used in generic bounds and supertype lists
    }

    private void collectNamespacesAndClassifiers(
            @NotNull final JetScope outerScope,
            @NotNull final JetScope outerScopeForStatic,
            @NotNull final NamespaceLikeBuilder owner,
            @NotNull Collection<? extends PsiElement> declarations) {
        for (PsiElement declaration : declarations) {
            declaration.accept(new JetVisitorVoid() {
                @Override
                public void visitJetFile(JetFile file) {
                    NamespaceDescriptorImpl namespaceDescriptor = namespaceFactory.createNamespaceDescriptorPathIfNeeded(file, outerScope);
                    context.getNamespaceDescriptors().put(file, namespaceDescriptor);

                    WriteThroughScope namespaceScope = new WriteThroughScope(outerScope, namespaceDescriptor.getMemberScope(), new TraceBasedRedeclarationHandler(trace));
                    namespaceScope.changeLockLevel(WritableScope.LockLevel.BOTH);
                    context.getNamespaceScopes().put(file, namespaceScope);

                    collectNamespacesAndClassifiers(namespaceScope, namespaceScope, namespaceDescriptor, file.getDeclarations());
                }

                @Override
                public void visitClass(JetClass klass) {
                    MutableClassDescriptor mutableClassDescriptor = new MutableClassDescriptor(trace, owner.getOwnerForChildren(), outerScope, getClassKind(klass));
                    trace.record(FQNAME_TO_CLASS_DESCRIPTOR, JetPsiUtil.getFQName(klass), mutableClassDescriptor);

                    if (klass.hasModifier(JetTokens.ENUM_KEYWORD)) {
                        MutableClassDescriptor classObjectDescriptor = new MutableClassDescriptor(trace, mutableClassDescriptor, outerScope, ClassKind.OBJECT);
                        classObjectDescriptor.setName("class-object-for-" + klass.getName());
                        classObjectDescriptor.setModality(Modality.FINAL);
                        classObjectDescriptor.setVisibility(DescriptorResolver.resolveVisibilityFromModifiers(klass.getModifierList()));
                        classObjectDescriptor.setTypeParameterDescriptors(new ArrayList<TypeParameterDescriptor>(0));
                        classObjectDescriptor.createTypeConstructor();
                        createPrimaryConstructorForObject(null, classObjectDescriptor);
                        mutableClassDescriptor.setClassObjectDescriptor(classObjectDescriptor);
                    }

                    visitClassOrObject(klass, (Map) context.getClasses(), mutableClassDescriptor);

                    owner.addClassifierDescriptor(mutableClassDescriptor);
                }

                @Override
                public void visitObjectDeclaration(JetObjectDeclaration declaration) {
                    final MutableClassDescriptor objectDescriptor = createClassDescriptorForObject(declaration, owner, outerScope, ClassKind.OBJECT);
                    trace.record(FQNAME_TO_CLASS_DESCRIPTOR, JetPsiUtil.getFQName(declaration), objectDescriptor);
                }

                @Override
                public void visitEnumEntry(JetEnumEntry enumEntry) {
                    MutableClassDescriptorLite classObjectDescriptor = ((MutableClassDescriptor) owner).getClassObjectDescriptor();
                    assert classObjectDescriptor != null : enumEntry.getParent().getText();
                    if (enumEntry.getPrimaryConstructorParameterList() == null) {
                        createClassDescriptorForObject(enumEntry, classObjectDescriptor, outerScopeForStatic, ClassKind.ENUM_ENTRY);
                        return;
                    }
                    MutableClassDescriptor mutableClassDescriptor = new MutableClassDescriptor(trace, classObjectDescriptor, outerScope, ClassKind.ENUM_ENTRY);
                    visitClassOrObject(
                            enumEntry,
                            (Map) context.getClasses(),
                            mutableClassDescriptor
                    );
                    classObjectDescriptor.addObjectDescriptor(mutableClassDescriptor);
                }

                private MutableClassDescriptor createClassDescriptorForObject(@NotNull JetClassOrObject declaration, @NotNull NamespaceLikeBuilder owner, JetScope scope, ClassKind classKind) {
                    MutableClassDescriptor mutableClassDescriptor = new MutableClassDescriptor(trace, owner.getOwnerForChildren(), scope, classKind) {
                        @Override
                        public ClassObjectStatus setClassObjectDescriptor(@NotNull MutableClassDescriptorLite classObjectDescriptor) {
                            return ClassObjectStatus.NOT_ALLOWED;
                        }
                    };
                    Map<JetClassOrObject, MutableClassDescriptor> map = classKind == ClassKind.OBJECT ? (Map) context.getObjects() : (Map) context.getClasses();
                    visitClassOrObject(declaration, map, mutableClassDescriptor);
                    createPrimaryConstructorForObject((JetDeclaration) declaration, mutableClassDescriptor);
                    owner.addObjectDescriptor(mutableClassDescriptor);
                    trace.record(BindingContext.CLASS, declaration, mutableClassDescriptor);
                    return mutableClassDescriptor;
                }

                private void createPrimaryConstructorForObject(@Nullable JetDeclaration object, MutableClassDescriptor mutableClassDescriptor) {
                    ConstructorDescriptorImpl constructorDescriptor = new ConstructorDescriptorImpl(mutableClassDescriptor, Collections.<AnnotationDescriptor>emptyList(), true);
                    constructorDescriptor.initialize(Collections.<TypeParameterDescriptor>emptyList(), Collections.<ValueParameterDescriptor>emptyList(),
                            Visibility.INTERNAL);//TODO check set mutableClassDescriptor.getVisibility()
                    // TODO : make the constructor private?
                    mutableClassDescriptor.setPrimaryConstructor(constructorDescriptor, trace);
                    if (object != null) {
                        trace.record(CONSTRUCTOR, object, constructorDescriptor);
                    }
                }

                private void visitClassOrObject(@NotNull JetClassOrObject declaration, Map<JetClassOrObject, MutableClassDescriptor> map, MutableClassDescriptor mutableClassDescriptor) {
                    mutableClassDescriptor.setName(JetPsiUtil.safeName(declaration.getName()));

                    map.put(declaration, mutableClassDescriptor);

                    JetScope classScope = mutableClassDescriptor.getScopeForMemberResolution();
                    JetScope scopeForStatic = declaration instanceof JetObjectDeclaration ? classScope : outerScopeForStatic;
                    collectNamespacesAndClassifiers(classScope, scopeForStatic, mutableClassDescriptor, declaration.getDeclarations());
                }

                @Override
                public void visitTypedef(JetTypedef typedef) {
                    trace.report(UNSUPPORTED.on(typedef, "TypeHierarchyResolver"));
                }

                @Override
                public void visitClassObject(JetClassObject classObject) {
                    JetObjectDeclaration objectDeclaration = classObject.getObjectDeclaration();
                    if (objectDeclaration != null) {
                        NamespaceLikeBuilder.ClassObjectStatus status = owner.setClassObjectDescriptor(createClassDescriptorForObject(objectDeclaration, owner, outerScopeForStatic, ClassKind.OBJECT));
                        switch (status) {
                            case DUPLICATE:
                                trace.report(MANY_CLASS_OBJECTS.on(classObject));
                                break;
                            case NOT_ALLOWED:
                                trace.report(CLASS_OBJECT_NOT_ALLOWED.on(classObject));
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

    private void createTypeConstructors() {
        for (Map.Entry<JetClass, MutableClassDescriptor> entry : context.getClasses().entrySet()) {
            JetClass jetClass = entry.getKey();
            MutableClassDescriptor descriptor = entry.getValue();
            descriptorResolver.resolveMutableClassDescriptor(jetClass, descriptor, trace);
            descriptor.createTypeConstructor();
        }
        for (Map.Entry<JetObjectDeclaration, MutableClassDescriptor> entry : context.getObjects().entrySet()) {
            JetObjectDeclaration objectDeclaration = entry.getKey();
            MutableClassDescriptor descriptor = entry.getValue();
            descriptor.setModality(Modality.FINAL);
            descriptor.setVisibility(DescriptorResolver.resolveVisibilityFromModifiers(objectDeclaration.getModifierList()));
            descriptor.setTypeParameterDescriptors(new ArrayList<TypeParameterDescriptor>(0));
            descriptor.createTypeConstructor();
        }
    }

    private void resolveTypesInClassHeaders() {
        for (Map.Entry<JetClass, MutableClassDescriptor> entry : context.getClasses().entrySet()) {
            JetClass jetClass = entry.getKey();
            MutableClassDescriptor descriptor = entry.getValue();
            descriptorResolver.resolveGenericBounds(jetClass, descriptor.getScopeForSupertypeResolution(), descriptor.getTypeConstructor().getParameters(), trace);
            descriptorResolver.resolveSupertypes(jetClass, descriptor, trace);
        }
        for (Map.Entry<JetObjectDeclaration, MutableClassDescriptor> entry : context.getObjects().entrySet()) {
            JetClassOrObject jetClass = entry.getKey();
            MutableClassDescriptor descriptor = entry.getValue();
            descriptorResolver.resolveSupertypes(jetClass, descriptor, trace);
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
            PsiElement psiElement = trace.get(DESCRIPTOR_TO_DECLARATION, classDescriptor);

            PsiElement elementToMark = null;
            if (psiElement instanceof JetClassOrObject) {
                JetClassOrObject classOrObject = (JetClassOrObject) psiElement;
                for (JetDelegationSpecifier delegationSpecifier : classOrObject.getDelegationSpecifiers()) {
                    JetTypeReference typeReference = delegationSpecifier.getTypeReference();
                    if (typeReference == null) continue;
                    JetType supertype = trace.get(TYPE, typeReference);
                    if (supertype != null && supertype.getConstructor() == superclass.getTypeConstructor()) {
                        elementToMark = typeReference;
                    }
                }
            }
            if (elementToMark == null && psiElement instanceof PsiNameIdentifierOwner) {
                PsiNameIdentifierOwner namedElement = (PsiNameIdentifierOwner) psiElement;
                PsiElement nameIdentifier = namedElement.getNameIdentifier();
                if (nameIdentifier != null) {
                    elementToMark = nameIdentifier;
                }
            }
            if (elementToMark != null) {
                trace.report(CYCLIC_INHERITANCE_HIERARCHY.on(elementToMark));
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
                        PsiElement psiElement = trace.get(DESCRIPTOR_TO_DECLARATION, mutableClassDescriptor);
                        assert psiElement instanceof JetClassOrObject : psiElement;
                        JetClassOrObject declaration = (JetClassOrObject) psiElement;
                        JetDelegationSpecifierList delegationSpecifierList = declaration.getDelegationSpecifierList();
                        assert delegationSpecifierList != null;
//                        trace.getErrorHandler().genericError(delegationSpecifierList.getNode(), "Type parameter " + typeParameterDescriptor.getName() + " of " + containingDeclaration.getName() + " has inconsistent values: " + conflictingTypes);
                        trace.report(INCONSISTENT_TYPE_PARAMETER_VALUES.on(delegationSpecifierList, typeParameterDescriptor, (ClassDescriptor) containingDeclaration, conflictingTypes));
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

    private void checkTypesInClassHeaders() {
        for (Map.Entry<JetClass, MutableClassDescriptor> entry : context.getClasses().entrySet()) {
            JetClass jetClass = entry.getKey();

            for (JetDelegationSpecifier delegationSpecifier : jetClass.getDelegationSpecifiers()) {
                JetTypeReference typeReference = delegationSpecifier.getTypeReference();
                if (typeReference != null) {
                    JetType type = trace.getBindingContext().get(TYPE, typeReference);
                    if (type != null) {
                        descriptorResolver.checkBounds(typeReference, type, trace);
                    }
                }
            }

            for (JetTypeParameter jetTypeParameter : jetClass.getTypeParameters()) {
                JetTypeReference extendsBound = jetTypeParameter.getExtendsBound();
                if (extendsBound != null) {
                    JetType type = trace.getBindingContext().get(TYPE, extendsBound);
                    if (type != null) {
                        descriptorResolver.checkBounds(extendsBound, type, trace);
                    }
                }
            }

            for (JetTypeConstraint constraint : jetClass.getTypeConstaints()) {
                JetTypeReference extendsBound = constraint.getBoundTypeReference();
                if (extendsBound != null) {
                    JetType type = trace.getBindingContext().get(TYPE, extendsBound);
                    if (type != null) {
                        descriptorResolver.checkBounds(extendsBound, type, trace);
                    }
                }
            }
        }
    }
}
