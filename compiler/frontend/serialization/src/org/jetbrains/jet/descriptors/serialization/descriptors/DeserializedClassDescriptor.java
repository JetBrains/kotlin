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

package org.jetbrains.jet.descriptors.serialization.descriptors;

import jet.Function0;
import jet.Function1;
import kotlin.KotlinPackage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.descriptors.serialization.*;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.descriptors.impl.AbstractClassDescriptor;
import org.jetbrains.jet.lang.descriptors.impl.ConstructorDescriptorImpl;
import org.jetbrains.jet.lang.descriptors.impl.EnumEntrySyntheticClassDescriptor;
import org.jetbrains.jet.lang.descriptors.impl.MutableClassDescriptor;
import org.jetbrains.jet.lang.resolve.DescriptorFactory;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.OverridingUtil;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.types.ErrorUtils;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.TypeConstructor;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;
import org.jetbrains.jet.storage.MemoizedFunctionToNullable;
import org.jetbrains.jet.storage.NotNullLazyValue;
import org.jetbrains.jet.storage.NullableLazyValue;
import org.jetbrains.jet.storage.StorageManager;

import java.util.*;

import static org.jetbrains.jet.descriptors.serialization.TypeDeserializer.TypeParameterResolver.NONE;
import static org.jetbrains.jet.lang.resolve.name.SpecialNames.getClassObjectName;

public class DeserializedClassDescriptor extends AbstractClassDescriptor implements ClassDescriptor {

    private final ClassId classId;
    private final ProtoBuf.Class classProto;
    private final StorageManager storageManager;
    private final TypeDeserializer typeDeserializer;
    private final DescriptorDeserializer deserializer;
    private final DeserializedMemberScope memberScope;

    private final NullableLazyValue<ConstructorDescriptor> primaryConstructor;

    private final AnnotationDeserializer annotationDeserializer;
    private final NotNullLazyValue<List<AnnotationDescriptor>> annotations;

    private final NullableLazyValue<ClassDescriptor> classObjectDescriptor;

    private final NestedClassDescriptors nestedClasses;

    private final NotNullLazyValue<DeclarationDescriptor> containingDeclaration;
    private final DeserializedClassTypeConstructor typeConstructor;
    private final Modality modality;
    private final Visibility visibility;
    private final ClassKind kind;
    private final boolean isInner;
    private final DescriptorFinder descriptorFinder;
    private final PackageFragmentProvider packageFragmentProvider;

    public DeserializedClassDescriptor(
            @NotNull StorageManager storageManager,
            @NotNull AnnotationDeserializer annotationResolver,
            @NotNull DescriptorFinder descriptorFinder,
            @NotNull PackageFragmentProvider packageFragmentProvider,
            @NotNull NameResolver nameResolver,
            @NotNull ProtoBuf.Class classProto
    ) {
        super(storageManager, nameResolver.getClassId(classProto.getFqName()).getRelativeClassName().shortName());
        this.classProto = classProto;
        this.classId = nameResolver.getClassId(classProto.getFqName());
        this.storageManager = storageManager;
        this.packageFragmentProvider = packageFragmentProvider;
        this.descriptorFinder = descriptorFinder;

        TypeDeserializer notNullTypeDeserializer = new TypeDeserializer(storageManager, null, nameResolver,
                                                                        descriptorFinder, "Deserializer for class " + getName(), NONE);
        DescriptorDeserializer outerDeserializer = DescriptorDeserializer.create(storageManager, notNullTypeDeserializer,
                                                                                 this, nameResolver, annotationResolver);
        List<TypeParameterDescriptor> typeParameters = new ArrayList<TypeParameterDescriptor>(classProto.getTypeParameterCount());
        this.deserializer = outerDeserializer.createChildDeserializer(this, classProto.getTypeParameterList(), typeParameters);
        this.typeDeserializer = deserializer.getTypeDeserializer();

        this.containingDeclaration = storageManager.createLazyValue(new Function0<DeclarationDescriptor>() {
            @Override
            public DeclarationDescriptor invoke() {
                return computeContainingDeclaration();
            }
        });

        this.typeConstructor = new DeserializedClassTypeConstructor(typeParameters);
        this.memberScope = new DeserializedClassMemberScope(storageManager, this);

        int flags = classProto.getFlags();
        this.modality = DescriptorDeserializer.modality(Flags.MODALITY.get(flags));
        this.visibility = DescriptorDeserializer.visibility(Flags.VISIBILITY.get(flags));
        this.kind = DescriptorDeserializer.classKind(Flags.CLASS_KIND.get(flags));
        this.isInner = Flags.INNER.get(flags);

        this.annotationDeserializer = annotationResolver;
        this.annotations = storageManager.createLazyValue(new Function0<List<AnnotationDescriptor>>() {
            @Override
            public List<AnnotationDescriptor> invoke() {
                return computeAnnotations();
            }
        });

        this.primaryConstructor = storageManager.createNullableLazyValue(new Function0<ConstructorDescriptor>() {
            @Override
            public ConstructorDescriptor invoke() {
                return computePrimaryConstructor();
            }
        });

        this.classObjectDescriptor = storageManager.createNullableLazyValue(new Function0<ClassDescriptor>() {
            @Override
            public ClassDescriptor invoke() {
                return computeClassObjectDescriptor();
            }
        });

        this.nestedClasses = new NestedClassDescriptors();
    }

    @NotNull
    @Override
    public DeclarationDescriptor getContainingDeclaration() {
        return containingDeclaration.invoke();
    }

    @NotNull
    private DeclarationDescriptor computeContainingDeclaration() {
        if (classId.isTopLevelClass()) {
            List<PackageFragmentDescriptor> fragments = packageFragmentProvider.getPackageFragments(classId.getPackageFqName());
            assert fragments.size() == 1 : "there should be exactly one package: " + fragments;
            return fragments.iterator().next();
        }
        else {
            ClassOrNamespaceDescriptor result = descriptorFinder.findClass(classId.getOuterClassId());
            return result != null ? result : ErrorUtils.getErrorModule();
        }
    }

    @NotNull
    @Override
    public TypeConstructor getTypeConstructor() {
        return typeConstructor;
    }


    @NotNull
    @Override
    public ClassKind getKind() {
        return kind;
    }

    @NotNull
    @Override
    public Modality getModality() {
        return modality;
    }

    @NotNull
    @Override
    public Visibility getVisibility() {
        return visibility;
    }

    @Override
    public boolean isInner() {
        return isInner;
    }

    private List<AnnotationDescriptor> computeAnnotations() {
        if (!Flags.HAS_ANNOTATIONS.get(classProto.getFlags())) {
            return Collections.emptyList();
        }
        return annotationDeserializer.loadClassAnnotations(this, classProto);
    }

    @NotNull
    @Override
    public List<AnnotationDescriptor> getAnnotations() {
        return annotations.invoke();
    }

    @NotNull
    @Override
    protected JetScope getScopeForMemberLookup() {
        return memberScope;
    }

    @Nullable
    private ConstructorDescriptor computePrimaryConstructor() {
        if (!classProto.hasPrimaryConstructor()) return null;

        ProtoBuf.Class.PrimaryConstructor constructorProto = classProto.getPrimaryConstructor();
        if (!constructorProto.hasData()) {
            ConstructorDescriptorImpl descriptor = DescriptorFactory.createPrimaryConstructorForObject(this);
            descriptor.setReturnType(getDefaultType());
            return descriptor;
        }

        return (ConstructorDescriptor) deserializer.loadCallable(constructorProto.getData());
    }

    @Nullable
    @Override
    public ConstructorDescriptor getUnsubstitutedPrimaryConstructor() {
        return primaryConstructor.invoke();
    }

    @NotNull
    @Override
    public Collection<ConstructorDescriptor> getConstructors() {
        ConstructorDescriptor constructor = getUnsubstitutedPrimaryConstructor();
        if (constructor == null) {
            return Collections.emptyList();
        }
        // TODO: other constructors
        return Collections.singletonList(constructor);
    }

    @Nullable
    private ClassDescriptor computeClassObjectDescriptor() {
        if (!classProto.hasClassObject()) {
            return null;
        }

        if (getKind() == ClassKind.ENUM_CLASS) {
            return createEnumClassObject();
        }

        if (getKind() == ClassKind.OBJECT) {
            ProtoBuf.Class.ClassObject classObjectProto = classProto.getClassObject();
            if (!classObjectProto.hasData()) {
                throw new IllegalStateException("Object should have a serialized class object: " + classId);
            }

            return new DeserializedClassDescriptor(storageManager, annotationDeserializer, descriptorFinder, packageFragmentProvider, 
                                                   deserializer.getNameResolver(), classObjectProto.getData());
        }

        return descriptorFinder.findClass(classId.createNestedClassId(getClassObjectName(getName())));
    }

    @NotNull
    private MutableClassDescriptor createEnumClassObject() {
        MutableClassDescriptor classObject = new MutableClassDescriptor(this, getScopeForMemberLookup(), ClassKind.CLASS_OBJECT,
                                                                        false, getClassObjectName(getName()));
        classObject.setModality(Modality.FINAL);
        classObject.setVisibility(DescriptorUtils.getSyntheticClassObjectVisibility());
        classObject.setTypeParameterDescriptors(Collections.<TypeParameterDescriptor>emptyList());
        classObject.setPrimaryConstructor(DescriptorFactory.createPrimaryConstructorForObject(classObject));
        classObject.createTypeConstructor();

        JetType enumType = getDefaultType();
        JetType enumArrayType = KotlinBuiltIns.getInstance().getArrayType(enumType);
        classObject.getBuilder().addFunctionDescriptor(DescriptorFactory.createEnumClassObjectValuesMethod(classObject, enumArrayType));
        classObject.getBuilder().addFunctionDescriptor(DescriptorFactory.createEnumClassObjectValueOfMethod(classObject, enumType));

        return classObject;
    }

    @Nullable
    @Override
    public ClassDescriptor getClassObjectDescriptor() {
        return classObjectDescriptor.invoke();
    }

    private Collection<JetType> computeSuperTypes() {
        List<JetType> supertypes = new ArrayList<JetType>(classProto.getSupertypeCount());
        for (ProtoBuf.Type supertype : classProto.getSupertypeList()) {
            supertypes.add(typeDeserializer.type(supertype));
        }
        return supertypes;
    }

    @Override
    public String toString() {
        return "deserialized class " + getName().toString();
    }

    private class DeserializedClassTypeConstructor implements TypeConstructor {
        private final Collection<JetType> supertypes = computeSuperTypes();
        private final List<TypeParameterDescriptor> parameters;

        public DeserializedClassTypeConstructor(@NotNull List<TypeParameterDescriptor> typeParameters) {
            parameters = typeParameters;
        }

        @NotNull
        @Override
        public List<TypeParameterDescriptor> getParameters() {
            return parameters;
        }

        @NotNull
        @Override
        public Collection<JetType> getSupertypes() {
            return supertypes;
        }

        @Override
        public boolean isFinal() {
            return !getModality().isOverridable();
        }

        @Override
        public boolean isDenotable() {
            return true;
        }

        @Nullable
        @Override
        public ClassifierDescriptor getDeclarationDescriptor() {
            return DeserializedClassDescriptor.this;
        }

        @NotNull
        @Override
        public List<AnnotationDescriptor> getAnnotations() {
            return Collections.emptyList(); // TODO
        }

        @Override
        public String toString() {
            return getName().toString();
        }
    }

    private static class DeserializedClassMemberScope extends DeserializedMemberScope {
        private final DeserializedClassDescriptor classDescriptor;

        public DeserializedClassMemberScope(@NotNull StorageManager storageManager, @NotNull DeserializedClassDescriptor classDescriptor) {
            super(storageManager, classDescriptor, classDescriptor.deserializer, classDescriptor.classProto.getMemberList());
            this.classDescriptor = classDescriptor;
        }

        @Override
        protected void computeNonDeclaredFunctions(
                @NotNull Name name, @NotNull Collection<FunctionDescriptor> functions
        ) {
            Collection<FunctionDescriptor> fromSupertypes = new ArrayList<FunctionDescriptor>();
            for (JetType supertype : classDescriptor.getTypeConstructor().getSupertypes()) {
                fromSupertypes.addAll(supertype.getMemberScope().getFunctions(name));
            }
            generateFakeOverrides(name, fromSupertypes, functions);
        }

        @Override
        protected void computeNonDeclaredProperties(
                @NotNull Name name, @NotNull Collection<PropertyDescriptor> property
        ) {
            Collection<PropertyDescriptor> fromSupertypes = new ArrayList<PropertyDescriptor>();
            for (JetType supertype : classDescriptor.getTypeConstructor().getSupertypes()) {
                //noinspection unchecked
                fromSupertypes.addAll((Collection) supertype.getMemberScope().getProperties(name));
            }
            generateFakeOverrides(name, fromSupertypes, property);
        }

        private <D extends CallableMemberDescriptor> void generateFakeOverrides(
                @NotNull Name name,
                @NotNull Collection<D> fromSupertypes,
                @NotNull final Collection<D> result
        ) {
            List<CallableMemberDescriptor> fromCurrent = new ArrayList<CallableMemberDescriptor>(result);
            OverridingUtil.generateOverridesInFunctionGroup(
                    name,
                    fromSupertypes,
                    fromCurrent,
                    classDescriptor,
                    new OverridingUtil.DescriptorSink() {
                        @Override
                        public void addToScope(@NotNull CallableMemberDescriptor fakeOverride) {
                            OverridingUtil.resolveUnknownVisibilityForMember(fakeOverride, new OverridingUtil.NotInferredVisibilitySink() {
                                @Override
                                public void cannotInferVisibility(@NotNull CallableMemberDescriptor descriptor) {
                                    // Do nothing
                                    // TODO: do something
                                }
                            });
                            //noinspection unchecked
                            result.add((D) fakeOverride);
                        }

                        @Override
                        public void conflict(@NotNull CallableMemberDescriptor fromSuper, @NotNull CallableMemberDescriptor fromCurrent) {
                            // TODO report conflicts
                        }
                    }
            );
        }

        @Override
        protected void addNonDeclaredDescriptors(@NotNull Collection<DeclarationDescriptor> result) {
            for (JetType supertype : classDescriptor.getTypeConstructor().getSupertypes()) {
                for (DeclarationDescriptor descriptor : supertype.getMemberScope().getAllDescriptors()) {
                    if (descriptor instanceof FunctionDescriptor) {
                        result.addAll(getFunctions(descriptor.getName()));
                    }
                    else if (descriptor instanceof PropertyDescriptor) {
                        result.addAll(getProperties(descriptor.getName()));
                    }
                    // Nothing else is inherited
                }
            }
        }

        @Nullable
        @Override
        protected ReceiverParameterDescriptor getImplicitReceiver() {
            return classDescriptor.getThisAsReceiverParameter();
        }

        @Nullable
        @Override
        protected ClassifierDescriptor getClassDescriptor(@NotNull Name name) {
            return classDescriptor.nestedClasses.findClass.invoke(name);
        }

        @Override
        protected void addAllClassDescriptors(@NotNull Collection<DeclarationDescriptor> result) {
            result.addAll(classDescriptor.nestedClasses.getAllDescriptors());
        }
    }

    private class NestedClassDescriptors {
        private final Set<Name> nestedClassNames;
        private final MemoizedFunctionToNullable<Name, ClassDescriptor> findClass;
        private final Set<Name> enumEntryNames;

        public NestedClassDescriptors() {
            this.nestedClassNames = nestedClassNames();
            this.enumEntryNames = enumEntryNames();

            final NotNullLazyValue<Collection<Name>> enumMemberNames = storageManager.createLazyValue(new Function0<Collection<Name>>() {
                @Override
                public Collection<Name> invoke() {
                    return computeEnumMemberNames();
                }
            });

            this.findClass = storageManager.createMemoizedFunctionWithNullableValues(new Function1<Name, ClassDescriptor>() {
                @Override
                public ClassDescriptor invoke(Name name) {
                    if (enumEntryNames.contains(name)) {
                        return EnumEntrySyntheticClassDescriptor
                                .create(storageManager, DeserializedClassDescriptor.this, name, enumMemberNames);
                    }
                    if (nestedClassNames.contains(name)) {
                        return descriptorFinder.findClass(classId.createNestedClassId(name));
                    }
                    return null;
                }
            });
        }

        @NotNull
        private Set<Name> nestedClassNames() {
            Set<Name> result = new HashSet<Name>();
            NameResolver nameResolver = deserializer.getNameResolver();
            for (Integer index : classProto.getNestedClassNameList()) {
                result.add(nameResolver.getName(index));
            }
            return result;
        }

        @NotNull
        private Set<Name> enumEntryNames() {
            if (getKind() != ClassKind.ENUM_CLASS) {
                return Collections.emptySet();
            }

            Set<Name> result = new HashSet<Name>();
            NameResolver nameResolver = deserializer.getNameResolver();
            for (Integer index : classProto.getEnumEntryList()) {
                result.add(nameResolver.getName(index));
            }
            return result;
        }

        @NotNull
        private Collection<Name> computeEnumMemberNames() {
            Collection<Name> result = new HashSet<Name>();

            for (JetType supertype : getTypeConstructor().getSupertypes()) {
                for (DeclarationDescriptor descriptor : supertype.getMemberScope().getAllDescriptors()) {
                    if (descriptor instanceof SimpleFunctionDescriptor || descriptor instanceof PropertyDescriptor) {
                        result.add(descriptor.getName());
                    }
                }
            }

            final NameResolver nameResolver = deserializer.getNameResolver();
            return KotlinPackage.mapTo(classProto.getMemberList(), result, new Function1<ProtoBuf.Callable, Name>() {
                @Override
                public Name invoke(@NotNull ProtoBuf.Callable callable) {
                    return nameResolver.getName(callable.getName());
                }
            });
        }

        @NotNull
        public Collection<ClassDescriptor> getAllDescriptors() {
            Collection<ClassDescriptor> result = new ArrayList<ClassDescriptor>(nestedClassNames.size() + enumEntryNames.size());
            for (Name name : nestedClassNames) {
                ClassDescriptor descriptor = findClass.invoke(name);
                if (descriptor != null) {
                    result.add(descriptor);
                }
            }
            for (Name name : enumEntryNames) {
                ClassDescriptor descriptor = findClass.invoke(name);
                if (descriptor != null) {
                    result.add(descriptor);
                }
            }
            return result;
        }
    }
}

