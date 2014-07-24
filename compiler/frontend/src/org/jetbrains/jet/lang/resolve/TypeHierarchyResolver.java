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

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiNameIdentifierOwner;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.descriptors.impl.*;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.name.SpecialNames;
import org.jetbrains.jet.lang.resolve.scopes.ChainedScope;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.resolve.scopes.WritableScope;
import org.jetbrains.jet.lang.resolve.scopes.WriteThroughScope;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.storage.LockBasedStorageManager;
import org.jetbrains.jet.utils.DFS;

import javax.inject.Inject;
import java.util.*;

import static org.jetbrains.jet.lang.diagnostics.Errors.*;
import static org.jetbrains.jet.lang.resolve.BindingContext.FQNAME_TO_CLASS_DESCRIPTOR;
import static org.jetbrains.jet.lang.resolve.BindingContext.TYPE;
import static org.jetbrains.jet.lang.resolve.DescriptorUtils.isEnumEntry;
import static org.jetbrains.jet.lang.resolve.DescriptorUtils.isObject;
import static org.jetbrains.jet.lang.resolve.ModifiersChecker.getDefaultClassVisibility;
import static org.jetbrains.jet.lang.resolve.ModifiersChecker.resolveVisibilityFromModifiers;
import static org.jetbrains.jet.lang.resolve.name.SpecialNames.getClassObjectName;
import static org.jetbrains.jet.lang.resolve.source.SourcePackage.toSourceElement;

public class TypeHierarchyResolver {
    private static final DFS.Neighbors<ClassDescriptor> CLASS_INHERITANCE_EDGES = new DFS.Neighbors<ClassDescriptor>() {
        @NotNull
        @Override
        public Iterable<ClassDescriptor> getNeighbors(ClassDescriptor current) {
            List<ClassDescriptor> result = new ArrayList<ClassDescriptor>();
            for (JetType supertype : current.getDefaultType().getConstructor().getSupertypes()) {
                DeclarationDescriptor descriptor = supertype.getConstructor().getDeclarationDescriptor();
                if (descriptor instanceof ClassDescriptor) {
                    result.add((ClassDescriptor) descriptor);
                }
            }
            DeclarationDescriptor container = current.getContainingDeclaration();
            if (container instanceof ClassDescriptor) {
                result.add((ClassDescriptor) container);
            }
            return result;
        }
    };

    @NotNull
    private ImportsResolver importsResolver;
    @NotNull
    private DescriptorResolver descriptorResolver;
    @NotNull
    private ScriptHeaderResolver scriptHeaderResolver;
    @NotNull
    private MutablePackageFragmentProvider packageFragmentProvider;
    @NotNull
    private BindingTrace trace;

    @Inject
    public void setImportsResolver(@NotNull ImportsResolver importsResolver) {
        this.importsResolver = importsResolver;
    }

    @Inject
    public void setDescriptorResolver(@NotNull DescriptorResolver descriptorResolver) {
        this.descriptorResolver = descriptorResolver;
    }

    // SCRIPT: inject script header resolver
    @Inject
    public void setScriptHeaderResolver(@NotNull ScriptHeaderResolver scriptHeaderResolver) {
        this.scriptHeaderResolver = scriptHeaderResolver;
    }

    @Inject
    public void setPackageFragmentProvider(@NotNull MutablePackageFragmentProvider packageFragmentProvider) {
        this.packageFragmentProvider = packageFragmentProvider;
    }

    @Inject
    public void setTrace(@NotNull BindingTrace trace) {
        this.trace = trace;
    }

    public void process(
            @NotNull TopDownAnalysisContext c,
            @NotNull JetScope outerScope,
            @NotNull PackageLikeBuilder owner,
            @NotNull Collection<? extends PsiElement> declarations
    ) {

        {
            // TODO: Very temp code - main goal is to remove recursion from collectPackageFragmentsAndClassifiers
            Queue<JetDeclarationContainer> forDeferredResolve = new LinkedList<JetDeclarationContainer>();
            forDeferredResolve.addAll(collectPackageFragmentsAndClassifiers(c, outerScope, owner, declarations));

            while (!forDeferredResolve.isEmpty()) {
                JetDeclarationContainer declarationContainer = forDeferredResolve.poll();
                assert declarationContainer != null;

                DeclarationDescriptor descriptorForDeferredResolve = c.forDeferredResolver.get(declarationContainer);
                JetScope scope = c.normalScope.get(declarationContainer);

                // Even more temp code
                if (descriptorForDeferredResolve instanceof MutableClassDescriptor) {
                    forDeferredResolve.addAll(
                            collectPackageFragmentsAndClassifiers(
                                    c,
                                    scope,
                                    ((MutableClassDescriptor) descriptorForDeferredResolve).getBuilder(),
                                    declarationContainer.getDeclarations()));
                }
                else if (descriptorForDeferredResolve instanceof MutablePackageFragmentDescriptor) {
                    forDeferredResolve.addAll(
                            collectPackageFragmentsAndClassifiers(
                                    c,
                                    scope,
                                    ((MutablePackageFragmentDescriptor) descriptorForDeferredResolve).getBuilder(),
                                    declarationContainer.getDeclarations()));
                }
                else {
                    assert false;
                }
            }
        }

        importsResolver.processTypeImports(c);

        createTypeConstructors(c); // create type constructors for classes and generic parameters, supertypes are not filled in
        resolveTypesInClassHeaders(c); // Generic bounds and types in supertype lists (no expressions or constructor resolution)

        c.setClassesTopologicalOrder(topologicallySortClassesAndObjects(c));

        // Detect and disconnect all loops in the hierarchy
        detectAndDisconnectLoops(c);
    }

    @NotNull
    private Collection<JetDeclarationContainer> collectPackageFragmentsAndClassifiers(
            @NotNull TopDownAnalysisContext c,
            @NotNull JetScope outerScope,
            @NotNull PackageLikeBuilder owner,
            @NotNull Iterable<? extends PsiElement> declarations
    ) {
        Collection<JetDeclarationContainer> forDeferredResolve = new ArrayList<JetDeclarationContainer>();

        ClassifierCollector collector = new ClassifierCollector(c, outerScope, owner, forDeferredResolve);

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

    private void createTypeConstructors(@NotNull TopDownAnalysisContext c) {
        for (Map.Entry<JetClassOrObject, ClassDescriptorWithResolutionScopes> entry : c.getDeclaredClasses().entrySet()) {
            JetClassOrObject classOrObject = entry.getKey();
            MutableClassDescriptor descriptor = (MutableClassDescriptor) entry.getValue();
            if (classOrObject instanceof JetClass) {
                descriptorResolver.resolveMutableClassDescriptor(
                        c.getTopDownAnalysisParameters(),
                        (JetClass) classOrObject, descriptor, trace);
            }
            else if (classOrObject instanceof JetObjectDeclaration) {
                descriptor.setModality(Modality.FINAL);
                descriptor.setVisibility(resolveVisibilityFromModifiers(classOrObject, getDefaultClassVisibility(descriptor)));
                descriptor.setTypeParameterDescriptors(Collections.<TypeParameterDescriptor>emptyList());
            }

            descriptor.createTypeConstructor();

            ClassKind kind = descriptor.getKind();
            if (kind == ClassKind.ENUM_ENTRY || kind == ClassKind.OBJECT) {
                MutableClassDescriptor classObject = (MutableClassDescriptor) descriptor.getClassObjectDescriptor();
                assert classObject != null : "Enum entries and named objects should have class objects: " + classOrObject.getText();

                // This is a clever hack: each enum entry and object declaration (i.e. singleton) has a synthetic class object.
                // We make this class object inherit from the singleton here, thus allowing to use the singleton's class object where
                // the instance of the singleton is applicable. Effectively all members of the singleton would be present in its class
                // object as fake overrides, so you can access them via standard class object notation: ObjectName.memberName()
                classObject.setSupertypes(Collections.singleton(descriptor.getDefaultType()));

                classObject.createTypeConstructor();
            }
        }
    }

    private void resolveTypesInClassHeaders(@NotNull TopDownAnalysisContext c) {
        for (Map.Entry<JetClassOrObject, ClassDescriptorWithResolutionScopes> entry : c.getDeclaredClasses().entrySet()) {
            JetClassOrObject classOrObject = entry.getKey();
            if (classOrObject instanceof JetClass) {
                ClassDescriptorWithResolutionScopes descriptor = entry.getValue();
                //noinspection unchecked
                descriptorResolver.resolveGenericBounds((JetClass) classOrObject, descriptor, descriptor.getScopeForClassHeaderResolution(),
                                                        (List) descriptor.getTypeConstructor().getParameters(), trace);
            }
        }

        for (Map.Entry<JetClassOrObject, ClassDescriptorWithResolutionScopes> entry : c.getDeclaredClasses().entrySet()) {
            descriptorResolver.resolveSupertypesForMutableClassDescriptor(entry.getKey(), (MutableClassDescriptor) entry.getValue(), trace);
        }
    }

    @NotNull
    @SuppressWarnings("unchecked")
    private static List<MutableClassDescriptor> topologicallySortClassesAndObjects(@NotNull TopDownAnalysisContext c) {
        Collection<ClassDescriptor> sourceClasses = (Collection) c.getAllClasses();
        List<ClassDescriptor> allClassesOrdered = DFS.topologicalOrder(sourceClasses, CLASS_INHERITANCE_EDGES);
        allClassesOrdered.retainAll(sourceClasses);
        return (List) allClassesOrdered;
    }

    private void detectAndDisconnectLoops(@NotNull TopDownAnalysisContext c) {
        List<Runnable> tasks = new ArrayList<Runnable>();
        for (final MutableClassDescriptor klass : c.getClassesTopologicalOrder()) {
            for (final JetType supertype : klass.getSupertypes()) {
                ClassifierDescriptor supertypeDescriptor = supertype.getConstructor().getDeclarationDescriptor();
                if (supertypeDescriptor instanceof ClassDescriptor) {
                    ClassDescriptor superclass = (ClassDescriptor) supertypeDescriptor;
                    if (isReachable(superclass, klass, new HashSet<ClassDescriptor>())) {
                        tasks.add(new Runnable() {
                            @Override
                            public void run() {
                                klass.getSupertypes().remove(supertype);
                            }
                        });
                        reportCyclicInheritanceHierarchyError(trace, klass, superclass);
                    }
                }
            }
        }

        for (Runnable task : tasks) {
            task.run();
        }
    }

    // TODO: use DFS and copy to LazyClassTypeConstructor.isReachable
    private static boolean isReachable(
            @NotNull ClassDescriptor from,
            @NotNull MutableClassDescriptor to,
            @NotNull Set<ClassDescriptor> visited
    ) {
        if (!visited.add(from)) return false;
        for (ClassDescriptor superclass : CLASS_INHERITANCE_EDGES.getNeighbors(from)) {
            if (superclass == to || isReachable(superclass, to, visited)) return true;
        }
        return false;
    }

    public static void reportCyclicInheritanceHierarchyError(
            @NotNull BindingTrace trace,
            @NotNull ClassDescriptor classDescriptor,
            @NotNull ClassDescriptor superclass
    ) {
        PsiElement psiElement = DescriptorToSourceUtils.classDescriptorToDeclaration(classDescriptor);

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

    private class ClassifierCollector extends JetVisitorVoid {
        private final TopDownAnalysisContext c;
        private final JetScope outerScope;
        private final PackageLikeBuilder owner;
        private final Collection<JetDeclarationContainer> forDeferredResolve;

        public ClassifierCollector(
                @NotNull TopDownAnalysisContext c,
                @NotNull JetScope outerScope,
                @NotNull PackageLikeBuilder owner,
                @NotNull Collection<JetDeclarationContainer> forDeferredResolve
        ) {
            this.c = c;
            this.outerScope = outerScope;
            this.owner = owner;
            this.forDeferredResolve = forDeferredResolve;
        }

        @Override
        public void visitJetFile(@NotNull JetFile file) {
            MutablePackageFragmentDescriptor packageFragment = getOrCreatePackageFragmentForFile(file);
            c.getPackageFragments().put(file, packageFragment);
            c.addFile(file);

            PackageViewDescriptor packageView = packageFragment.getContainingDeclaration().getPackage(packageFragment.getFqName());
            ChainedScope rootPlusPackageScope = new ChainedScope(packageView, "Root scope for " + file, packageView.getMemberScope(), outerScope);
            WriteThroughScope packageScope = new WriteThroughScope(rootPlusPackageScope, packageFragment.getMemberScope(),
                                                                     new TraceBasedRedeclarationHandler(trace), "package in file " + file.getName());
            packageScope.changeLockLevel(WritableScope.LockLevel.BOTH);
            c.getFileScopes().put(file, packageScope);

            if (file.isScript()) {
                // SCRIPT: process script hierarchy
                scriptHeaderResolver.processScriptHierarchy(c, file.getScript(), packageScope);
            }

            prepareForDeferredCall(packageScope, packageFragment, file);
        }

        @Override
        public void visitClass(@NotNull JetClass klass) {
            MutableClassDescriptor mutableClassDescriptor = createClassDescriptorForClass(klass, owner.getOwnerForChildren());

            owner.addClassifierDescriptor(mutableClassDescriptor);
        }

        @Override
        public void visitObjectDeclaration(@NotNull JetObjectDeclaration declaration) {
            if (declaration.isObjectLiteral()) {
                createClassDescriptorForSingleton(declaration, SpecialNames.NO_NAME_PROVIDED, ClassKind.CLASS);
                return;
            }

            MutableClassDescriptor descriptor =
                    createClassDescriptorForSingleton(declaration, JetPsiUtil.safeName(declaration.getName()), ClassKind.OBJECT);

            owner.addClassifierDescriptor(descriptor);
            trace.record(FQNAME_TO_CLASS_DESCRIPTOR, JetNamedDeclarationUtil.getUnsafeFQName(declaration), descriptor);

            descriptor.getBuilder().setClassObjectDescriptor(createSyntheticClassObjectForSingleton(descriptor));
        }

        @Override
        public void visitEnumEntry(@NotNull JetEnumEntry declaration) {
            MutableClassDescriptor descriptor =
                    createClassDescriptorForSingleton(declaration, JetPsiUtil.safeName(declaration.getName()), ClassKind.ENUM_ENTRY);

            owner.addClassifierDescriptor(descriptor);

            descriptor.getBuilder().setClassObjectDescriptor(createSyntheticClassObjectForSingleton(descriptor));
        }

        @Override
        public void visitTypedef(@NotNull JetTypedef typedef) {
            trace.report(UNSUPPORTED.on(typedef, "TypeHierarchyResolver"));
        }

        @Override
        public void visitClassObject(@NotNull JetClassObject classObject) {
            JetObjectDeclaration objectDeclaration = classObject.getObjectDeclaration();

            DeclarationDescriptor container = owner.getOwnerForChildren();

            MutableClassDescriptor classObjectDescriptor =
                    createClassDescriptorForSingleton(objectDeclaration, getClassObjectName(container.getName()), ClassKind.CLASS_OBJECT);

            PackageLikeBuilder.ClassObjectStatus status =
                    isEnumEntry(container) || isObject(container) || c.getTopDownAnalysisParameters().isDeclaredLocally() ?
                    PackageLikeBuilder.ClassObjectStatus.NOT_ALLOWED :
                    owner.setClassObjectDescriptor(classObjectDescriptor);

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

        @NotNull
        private MutablePackageFragmentDescriptor getOrCreatePackageFragmentForFile(@NotNull JetFile file) {
            JetPackageDirective packageDirective = file.getPackageDirective();
            assert packageDirective != null : "scripts are not supported";

            MutablePackageFragmentDescriptor fragment = packageFragmentProvider.getOrCreateFragment(packageDirective.getFqName());

            ModuleDescriptor module = packageFragmentProvider.getModule();
            DescriptorResolver.resolvePackageHeader(packageDirective, module, trace);
            DescriptorResolver.registerFileInPackage(trace, file);
            trace.record(BindingContext.FILE_TO_PACKAGE_FRAGMENT, file, fragment);

            return fragment;
        }


        @NotNull
        private MutableClassDescriptor createSyntheticClassObjectForSingleton(@NotNull ClassDescriptor classDescriptor) {
            MutableClassDescriptor classObject =
                    new MutableClassDescriptor(classDescriptor, outerScope, ClassKind.CLASS_OBJECT, false,
                                               getClassObjectName(classDescriptor.getName()), SourceElement.NO_SOURCE);

            classObject.setModality(Modality.FINAL);
            classObject.setVisibility(DescriptorUtils.getSyntheticClassObjectVisibility());
            classObject.setTypeParameterDescriptors(Collections.<TypeParameterDescriptor>emptyList());
            createPrimaryConstructorForObject(null, classObject);
            return classObject;
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
            MutableClassDescriptor descriptor = new MutableClassDescriptor(
                    containingDeclaration, outerScope, kind, isInner, JetPsiUtil.safeName(klass.getName()),
                    toSourceElement(klass));
            c.getDeclaredClasses().put(klass, descriptor);
            trace.record(FQNAME_TO_CLASS_DESCRIPTOR, JetNamedDeclarationUtil.getUnsafeFQName(klass), descriptor);

            if (descriptor.getKind() == ClassKind.ENUM_CLASS) {
                ClassDescriptor classObject = new EnumClassObjectDescriptor(LockBasedStorageManager.NO_LOCKS, descriptor);
                descriptor.getBuilder().setClassObjectDescriptor(classObject);
            }

            prepareForDeferredCall(descriptor.getScopeForMemberDeclarationResolution(), descriptor, klass);

            return descriptor;
        }

        @NotNull
        private MutableClassDescriptor createClassDescriptorForSingleton(
                @NotNull JetClassOrObject declaration,
                @NotNull Name name,
                @NotNull ClassKind kind
        ) {
            MutableClassDescriptor descriptor = new MutableClassDescriptor(owner.getOwnerForChildren(), outerScope, kind, false, name,
                                                                           toSourceElement(declaration));

            prepareForDeferredCall(descriptor.getScopeForMemberDeclarationResolution(), descriptor, declaration);

            createPrimaryConstructorForObject(declaration, descriptor);
            trace.record(BindingContext.CLASS, declaration, descriptor);

            c.getDeclaredClasses().put(declaration, descriptor);

            return descriptor;
        }

        @NotNull
        private ConstructorDescriptorImpl createPrimaryConstructorForObject(
                @Nullable JetClassOrObject object,
                @NotNull MutableClassDescriptor mutableClassDescriptor
        ) {
            ConstructorDescriptorImpl constructorDescriptor = DescriptorResolver
                    .createAndRecordPrimaryConstructorForObject(object, mutableClassDescriptor, trace);
            mutableClassDescriptor.setPrimaryConstructor(constructorDescriptor);
            return constructorDescriptor;
        }

        private void prepareForDeferredCall(
                @NotNull JetScope outerScope,
                @NotNull DeclarationDescriptor descriptorForDeferredResolve,
                @NotNull JetDeclarationContainer container
        ) {
            forDeferredResolve.add(container);
            c.normalScope.put(container, outerScope);
            c.forDeferredResolver.put(container, descriptorForDeferredResolve);
        }
    }
}
