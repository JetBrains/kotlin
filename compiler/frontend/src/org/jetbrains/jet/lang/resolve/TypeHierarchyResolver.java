/*
 * Copyright 2010-2013 JetBrains s.r.o.
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
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.descriptors.impl.*;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.resolve.scopes.RedeclarationHandler;
import org.jetbrains.jet.lang.resolve.scopes.WritableScope;
import org.jetbrains.jet.lang.resolve.scopes.WriteThroughScope;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.SubstitutionUtils;
import org.jetbrains.jet.lang.types.TypeConstructor;
import org.jetbrains.jet.lang.types.TypeProjection;
import org.jetbrains.jet.lang.types.checker.JetTypeChecker;
import org.jetbrains.jet.utils.DFS;

import javax.inject.Inject;
import java.util.*;

import static org.jetbrains.jet.lang.diagnostics.Errors.*;
import static org.jetbrains.jet.lang.resolve.BindingContext.FQNAME_TO_CLASS_DESCRIPTOR;
import static org.jetbrains.jet.lang.resolve.BindingContext.TYPE;
import static org.jetbrains.jet.lang.resolve.DescriptorUtils.getClassObjectName;

public class TypeHierarchyResolver {
    @NotNull
    private TopDownAnalysisContext context;
    @NotNull
    private ImportsResolver importsResolver;
    @NotNull
    private DescriptorResolver descriptorResolver;
    @NotNull
    private ScriptHeaderResolver scriptHeaderResolver;
    @NotNull
    private NamespaceFactoryImpl namespaceFactory;
    @NotNull
    private BindingTrace trace;

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
    public void setScriptHeaderResolver(@NotNull ScriptHeaderResolver scriptHeaderResolver) {
        this.scriptHeaderResolver = scriptHeaderResolver;
    }

    @Inject
    public void setNamespaceFactory(@NotNull NamespaceFactoryImpl namespaceFactory) {
        this.namespaceFactory = namespaceFactory;
    }

    @Inject
    public void setTrace(@NotNull BindingTrace trace) {
        this.trace = trace;
    }

    public void process(
            @NotNull JetScope outerScope, @NotNull NamespaceLikeBuilder owner,
            @NotNull Collection<? extends PsiElement> declarations
    ) {

        {
            // TODO: Very temp code - main goal is to remove recursion from collectNamespacesAndClassifiers
            Queue<JetDeclarationContainer> forDeferredResolve = new LinkedList<JetDeclarationContainer>();
            forDeferredResolve.addAll(collectNamespacesAndClassifiers(outerScope, owner, declarations));

            while (!forDeferredResolve.isEmpty()) {
                JetDeclarationContainer declarationContainer = forDeferredResolve.poll();
                assert declarationContainer != null;

                DeclarationDescriptor descriptorForDeferredResolve = context.forDeferredResolver.get(declarationContainer);
                JetScope scope = context.normalScope.get(declarationContainer);

                // Even more temp code
                if (descriptorForDeferredResolve instanceof MutableClassDescriptorLite) {
                    forDeferredResolve.addAll(
                            collectNamespacesAndClassifiers(
                                    scope,
                                    ((MutableClassDescriptorLite) descriptorForDeferredResolve).getBuilder(),
                                    declarationContainer.getDeclarations()));
                }
                else if (descriptorForDeferredResolve instanceof NamespaceDescriptorImpl) {
                    forDeferredResolve.addAll(
                            collectNamespacesAndClassifiers(
                                    scope,
                                    ((NamespaceDescriptorImpl) descriptorForDeferredResolve).getBuilder(),
                                    declarationContainer.getDeclarations()));
                }
                else {
                    assert false;
                }
            }
        }

        importsResolver.processTypeImports(outerScope);

        createTypeConstructors(); // create type constructors for classes and generic parameters, supertypes are not filled in
        resolveTypesInClassHeaders(); // Generic bounds and types in supertype lists (no expressions or constructor resolution)

        context.setClassesTopologicalOrder(topologicallySortClassesAndObjects());

        // Detect and disconnect all loops in the hierarchy
        detectAndDisconnectLoops();

        // At this point, there are no loops in the type hierarchy

        checkSupertypesForConsistency();
        //        computeSuperclasses();

        checkTypesInClassHeaders(); // Check bounds in the types used in generic bounds and supertype lists
    }

    /**
     * Use nearest class object scope or namespace scope
     *
     * @param declarationElement
     * @param owner
     * @return
     */
    @SuppressWarnings("SuspiciousMethodCalls")
    @NotNull
    private JetScope getStaticScope(PsiElement declarationElement, @NotNull NamespaceLikeBuilder owner) {
        DeclarationDescriptor ownerDescriptor = owner.getOwnerForChildren();
        if (ownerDescriptor instanceof NamespaceDescriptorImpl) {
            return context.getNamespaceScopes().get(declarationElement.getContainingFile());
        }

        if (ownerDescriptor instanceof MutableClassDescriptor) {
            MutableClassDescriptor classDescriptor = (MutableClassDescriptor) ownerDescriptor;
            if (classDescriptor.getKind() == ClassKind.CLASS_OBJECT) {
                return classDescriptor.getScopeForMemberResolution();
            }

            DeclarationDescriptor declaration = classDescriptor.getContainingDeclaration();
            if (declaration instanceof NamespaceDescriptorImpl) {
                return getStaticScope(declarationElement, ((NamespaceDescriptorImpl) declaration).getBuilder());
            }

            if (declaration instanceof MutableClassDescriptorLite) {
                return getStaticScope(declarationElement, ((MutableClassDescriptorLite) declaration).getBuilder());
            }
        }

        return null;
    }

    @Nullable
    private Collection<JetDeclarationContainer> collectNamespacesAndClassifiers(
            @NotNull final JetScope outerScope,
            @NotNull final NamespaceLikeBuilder owner,
            @NotNull Iterable<? extends PsiElement> declarations
    ) {
        final Collection<JetDeclarationContainer> forDeferredResolve = new ArrayList<JetDeclarationContainer>();

        ClassifierCollector collector = new ClassifierCollector(outerScope, owner, forDeferredResolve);

        for (PsiElement declaration : declarations) {
            declaration.accept(collector);
        }

        return forDeferredResolve;
    }


    @NotNull
    private static ClassKind getClassKind(@NotNull JetClass jetClass) {
        if (jetClass.isTrait()) return ClassKind.TRAIT;
        if (jetClass.isAnnotation()) return ClassKind.ANNOTATION_CLASS;
        if (jetClass.isEnum()) return ClassKind.ENUM_CLASS;
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
            descriptor.setVisibility(ModifiersChecker.resolveVisibilityFromModifiers(objectDeclaration));
            descriptor.setTypeParameterDescriptors(new ArrayList<TypeParameterDescriptor>(0));
            descriptor.createTypeConstructor();
        }
    }

    private void resolveTypesInClassHeaders() {
        for (Map.Entry<JetClass, MutableClassDescriptor> entry : context.getClasses().entrySet()) {
            JetClass jetClass = entry.getKey();
            MutableClassDescriptor descriptor = entry.getValue();
            descriptorResolver.resolveGenericBounds(jetClass, descriptor.getScopeForSupertypeResolution(),
                                                    (List) descriptor.getTypeConstructor().getParameters(), trace);
            descriptorResolver.resolveSupertypesForMutableClassDescriptor(jetClass, descriptor, trace);
        }
        for (Map.Entry<JetObjectDeclaration, MutableClassDescriptor> entry : context.getObjects().entrySet()) {
            JetClassOrObject jetClass = entry.getKey();
            MutableClassDescriptor descriptor = entry.getValue();
            descriptorResolver.resolveSupertypesForMutableClassDescriptor(jetClass, descriptor, trace);
        }
    }

    private List<MutableClassDescriptorLite> topologicallySortClassesAndObjects() {
        // A topsort is needed only for better diagnostics:
        //    edges that get removed to disconnect loops are more reasonable in this case
        return DFS.topologicalOrder(
                ContainerUtil.<MutableClassDescriptorLite>concat(context.getClasses().values(), context.getObjects().values()),
                new DFS.Neighbors<MutableClassDescriptorLite>() {
                    @NotNull
                    @Override
                    public Iterable<MutableClassDescriptorLite> getNeighbors(MutableClassDescriptorLite current) {
                        List<MutableClassDescriptorLite> result = Lists.newArrayList();
                        for (JetType supertype : current.getSupertypes()) {
                            DeclarationDescriptor declarationDescriptor = supertype.getConstructor().getDeclarationDescriptor();
                            if (declarationDescriptor instanceof MutableClassDescriptorLite) {
                                MutableClassDescriptorLite classDescriptor = (MutableClassDescriptorLite) declarationDescriptor;
                                result.add(classDescriptor);
                            }
                        }
                        return result;
                    }
                });

    }

    private void detectAndDisconnectLoops() {
        // Loop detection and disconnection
        Set<ClassDescriptor> visited = Sets.newHashSet();
        Set<ClassDescriptor> beingProcessed = Sets.newHashSet();
        List<ClassDescriptor> currentPath = Lists.newArrayList();
        for (MutableClassDescriptorLite klass : context.getClassesTopologicalOrder()) {
            traverseTypeHierarchy(klass, visited, beingProcessed, currentPath);
        }
    }

    private void traverseTypeHierarchy(
            MutableClassDescriptorLite currentClass,
            Set<ClassDescriptor> visited,
            Set<ClassDescriptor> beingProcessed,
            List<ClassDescriptor> currentPath
    ) {
        if (!visited.add(currentClass)) {
            if (beingProcessed.contains(currentClass)) {
                markCycleErrors(currentPath, currentClass);
                assert !currentPath.isEmpty() : "Cycle cannot be found on an empty currentPath";
                ClassDescriptor subclassOfCurrent = currentPath.get(currentPath.size() - 1);
                assert subclassOfCurrent instanceof MutableClassDescriptor;
                // Disconnect the loop
                for (Iterator<JetType> iterator = ((MutableClassDescriptor) subclassOfCurrent).getSupertypes().iterator();
                     iterator.hasNext(); ) {
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
        for (int i = size - 1; i >= 0; i--) {
            ClassDescriptor classDescriptor = currentPath.get(i);

            ClassDescriptor superclass = (i < size - 1) ? currentPath.get(i + 1) : current;
            PsiElement psiElement = BindingContextUtils.classDescriptorToDeclaration(trace.getBindingContext(), classDescriptor);

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

            if (classDescriptor == current) {
                // Beginning of cycle is found
                break;
            }
        }
    }

    private void checkSupertypesForConsistency() {
        for (MutableClassDescriptorLite mutableClassDescriptor : context.getClassesTopologicalOrder()) {
            Multimap<TypeConstructor, TypeProjection> multimap = SubstitutionUtils
                    .buildDeepSubstitutionMultimap(mutableClassDescriptor.getDefaultType());
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
                        JetClassOrObject psiElement = (JetClassOrObject) BindingContextUtils
                                .classDescriptorToDeclaration(trace.getBindingContext(), mutableClassDescriptor);
                        JetDelegationSpecifierList delegationSpecifierList = psiElement.getDelegationSpecifierList();
                        assert delegationSpecifierList != null;
                        //                        trace.getErrorHandler().genericError(delegationSpecifierList.getNode(), "Type parameter " + typeParameterDescriptor.getName() + " of " + containingDeclaration.getName() + " has inconsistent values: " + conflictingTypes);
                        trace.report(INCONSISTENT_TYPE_PARAMETER_VALUES
                                             .on(delegationSpecifierList, typeParameterDescriptor, (ClassDescriptor) containingDeclaration,
                                                 conflictingTypes));
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

            for (JetTypeConstraint constraint : jetClass.getTypeConstraints()) {
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

    private class ClassifierCollector extends JetVisitorVoid {
        private final JetScope outerScope;
        private final NamespaceLikeBuilder owner;
        private final Collection<JetDeclarationContainer> forDeferredResolve;

        public ClassifierCollector(@NotNull JetScope outerScope,
                @NotNull NamespaceLikeBuilder owner,
                @NotNull Collection<JetDeclarationContainer> forDeferredResolve
        ) {
            this.outerScope = outerScope;
            this.owner = owner;
            this.forDeferredResolve = forDeferredResolve;
        }

        @Override
        public void visitJetFile(JetFile file) {
            NamespaceDescriptorImpl namespaceDescriptor = namespaceFactory.createNamespaceDescriptorPathIfNeeded(
                    file, outerScope, RedeclarationHandler.DO_NOTHING);
            context.getNamespaceDescriptors().put(file, namespaceDescriptor);

            WriteThroughScope namespaceScope = new WriteThroughScope(outerScope, namespaceDescriptor.getMemberScope(),
                                                                     new TraceBasedRedeclarationHandler(trace), "namespace");
            namespaceScope.changeLockLevel(WritableScope.LockLevel.BOTH);
            context.getNamespaceScopes().put(file, namespaceScope);

            if (file.isScript()) {
                scriptHeaderResolver.processScriptHierarchy(file.getScript(), namespaceScope);
            }

            prepareForDeferredCall(namespaceScope, namespaceDescriptor, file);
        }

        @Override
        public void visitClass(JetClass klass) {
            MutableClassDescriptor mutableClassDescriptor = createClassDescriptorForClass(klass, owner.getOwnerForChildren());

            owner.addClassifierDescriptor(mutableClassDescriptor);
        }

        @Override
        public void visitObjectDeclaration(JetObjectDeclaration declaration) {
            MutableClassDescriptor objectDescriptor =
                    createClassDescriptorForObject(declaration, owner, outerScope, JetPsiUtil.safeName(declaration.getName()),
                                                   ClassKind.OBJECT);
            owner.addObjectDescriptor(objectDescriptor);
            trace.record(FQNAME_TO_CLASS_DESCRIPTOR, JetPsiUtil.getFQName(declaration), objectDescriptor);
        }

        @Override
        public void visitEnumEntry(JetEnumEntry enumEntry) {
            // TODO: Bad casting
            MutableClassDescriptorLite ownerClassDescriptor = (MutableClassDescriptorLite) owner.getOwnerForChildren();
            MutableClassDescriptorLite classObjectDescriptor = ownerClassDescriptor.getClassObjectDescriptor();

            assert classObjectDescriptor != null : enumEntry.getParent().getText();
            createClassDescriptorForEnumEntry(enumEntry, classObjectDescriptor.getBuilder());
        }

        @Override
        public void visitTypedef(JetTypedef typedef) {
            trace.report(UNSUPPORTED.on(typedef, "TypeHierarchyResolver"));
        }

        @Override
        public void visitClassObject(JetClassObject classObject) {
            JetObjectDeclaration objectDeclaration = classObject.getObjectDeclaration();
            if (objectDeclaration != null) {
                Name classObjectName = getClassObjectName(owner.getOwnerForChildren().getName());
                MutableClassDescriptor classObjectDescriptor =
                      createClassDescriptorForObject(objectDeclaration, owner, getStaticScope(classObject, owner),
                                                     classObjectName, ClassKind.CLASS_OBJECT);
                NamespaceLikeBuilder.ClassObjectStatus status = owner.setClassObjectDescriptor(classObjectDescriptor);
                switch (status) {
                    case DUPLICATE:
                        trace.report(MANY_CLASS_OBJECTS.on(classObject));
                        break;
                    case NOT_ALLOWED:
                        trace.report(CLASS_OBJECT_NOT_ALLOWED.on(classObject));
                        break;
                    case OK:
                        // Everything is OK so no errors to trace.
                        break;
                }
            }
        }

        private void createClassObjectForEnumClass(JetClass klass, MutableClassDescriptor mutableClassDescriptor) {
            if (mutableClassDescriptor.getKind() == ClassKind.ENUM_CLASS) {
                MutableClassDescriptor classObjectDescriptor =
                        createClassObjectDescriptor(mutableClassDescriptor, ModifiersChecker.resolveVisibilityFromModifiers(klass));
                mutableClassDescriptor.getBuilder().setClassObjectDescriptor(classObjectDescriptor);
                classObjectDescriptor.getBuilder().addFunctionDescriptor(
                        DescriptorResolver.createEnumClassObjectValuesMethod(classObjectDescriptor, trace));
                classObjectDescriptor.getBuilder().addFunctionDescriptor(
                        DescriptorResolver.createEnumClassObjectValueOfMethod(classObjectDescriptor, trace));
            }
        }

        @NotNull
        private MutableClassDescriptor createClassObjectDescriptor(
                @NotNull ClassDescriptor classDescriptor,
                @NotNull Visibility visibility
        ) {
            MutableClassDescriptor classObjectDescriptor = new MutableClassDescriptor(
                    classDescriptor, outerScope, ClassKind.CLASS_OBJECT, false, getClassObjectName(classDescriptor.getName()));
            classObjectDescriptor.setModality(Modality.FINAL);
            classObjectDescriptor.setVisibility(visibility);
            classObjectDescriptor.setTypeParameterDescriptors(new ArrayList<TypeParameterDescriptor>(0));
            classObjectDescriptor.createTypeConstructor();
            ConstructorDescriptorImpl primaryConstructorForObject = createPrimaryConstructorForObject(null, classObjectDescriptor);
            primaryConstructorForObject.setReturnType(classObjectDescriptor.getDefaultType());
            return classObjectDescriptor;
        }

        @NotNull
        private MutableClassDescriptor createClassDescriptorForClass(
                @NotNull JetClass klass,
                @NotNull DeclarationDescriptor containingDeclaration
        ) {
            ClassKind kind = getClassKind(klass);
            // Kind check is needed in order to not consider enums as inner in any case
            // (otherwise it would be impossible to create a class object in the enum)
            boolean isInner = kind == ClassKind.CLASS && klass.isInner();
            MutableClassDescriptor mutableClassDescriptor = new MutableClassDescriptor(
                    containingDeclaration, outerScope, kind, isInner, JetPsiUtil.safeName(klass.getName()));
            context.getClasses().put(klass, mutableClassDescriptor);
            trace.record(FQNAME_TO_CLASS_DESCRIPTOR, JetPsiUtil.getFQName(klass), mutableClassDescriptor);

            createClassObjectForEnumClass(klass, mutableClassDescriptor);

            JetScope classScope = mutableClassDescriptor.getScopeForMemberResolution();

            prepareForDeferredCall(classScope, mutableClassDescriptor, klass);

            return mutableClassDescriptor;
        }

        @NotNull
        private MutableClassDescriptor createClassDescriptorForObject(
                @NotNull JetObjectDeclaration declaration, @NotNull NamespaceLikeBuilder owner,
                @NotNull JetScope scope, @NotNull Name name, @NotNull ClassKind kind
        ) {
            MutableClassDescriptor mutableClassDescriptor = new MutableClassDescriptor(
                    owner.getOwnerForChildren(), scope, kind, false, name);
            context.getObjects().put(declaration, mutableClassDescriptor);

            JetScope classScope = mutableClassDescriptor.getScopeForMemberResolution();

            prepareForDeferredCall(classScope, mutableClassDescriptor, declaration);

            createPrimaryConstructorForObject(declaration, mutableClassDescriptor);
            trace.record(BindingContext.CLASS, declaration, mutableClassDescriptor);
            return mutableClassDescriptor;
        }

        private MutableClassDescriptor createClassDescriptorForEnumEntry(
                @NotNull JetEnumEntry declaration,
                @NotNull NamespaceLikeBuilder owner
        ) {
            MutableClassDescriptor mutableClassDescriptor = new MutableClassDescriptor(
                    owner.getOwnerForChildren(), getStaticScope(declaration, owner), ClassKind.ENUM_ENTRY,
                    false, JetPsiUtil.safeName(declaration.getName()));
            context.getClasses().put(declaration, mutableClassDescriptor);

            prepareForDeferredCall(mutableClassDescriptor.getScopeForMemberResolution(), mutableClassDescriptor, declaration);

            // ??? - is enum entry object?
            createPrimaryConstructorForObject(declaration, mutableClassDescriptor);
            owner.addObjectDescriptor(mutableClassDescriptor);
            trace.record(BindingContext.CLASS, declaration, mutableClassDescriptor);
            return mutableClassDescriptor;
        }

        private ConstructorDescriptorImpl createPrimaryConstructorForObject(
                @Nullable PsiElement object,
                MutableClassDescriptor mutableClassDescriptor
        ) {
            ConstructorDescriptorImpl constructorDescriptor = DescriptorResolver
                    .createAndRecordPrimaryConstructorForObject(object, mutableClassDescriptor, trace);
            mutableClassDescriptor.setPrimaryConstructor(constructorDescriptor, trace);
            return constructorDescriptor;
        }

        private void prepareForDeferredCall(
                @NotNull JetScope outerScope,
                @NotNull DeclarationDescriptor descriptorForDeferredResolve,
                @NotNull JetDeclarationContainer container
        ) {
            forDeferredResolve.add(container);
            context.normalScope.put(container, outerScope);
            context.forDeferredResolver.put(container, descriptorForDeferredResolve);
        }
    }
}
