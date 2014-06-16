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

import kotlin.Function0;
import kotlin.Function1;
import kotlin.KotlinPackage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.descriptors.serialization.*;
import org.jetbrains.jet.descriptors.serialization.context.ContextPackage;
import org.jetbrains.jet.descriptors.serialization.context.DeserializationContext;
import org.jetbrains.jet.descriptors.serialization.context.DeserializationContextWithTypes;
import org.jetbrains.jet.descriptors.serialization.context.DeserializationGlobalContext;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.descriptors.annotations.Annotations;
import org.jetbrains.jet.lang.descriptors.impl.AbstractClassDescriptor;
import org.jetbrains.jet.lang.descriptors.impl.ConstructorDescriptorImpl;
import org.jetbrains.jet.lang.descriptors.impl.EnumEntrySyntheticClassDescriptor;
import org.jetbrains.jet.lang.descriptors.impl.MutableClassDescriptor;
import org.jetbrains.jet.lang.resolve.DescriptorFactory;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.OverridingUtil;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.types.AbstractClassTypeConstructor;
import org.jetbrains.jet.lang.types.ErrorUtils;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.TypeConstructor;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;
import org.jetbrains.jet.storage.MemoizedFunctionToNullable;
import org.jetbrains.jet.storage.NotNullLazyValue;
import org.jetbrains.jet.storage.NullableLazyValue;
import org.jetbrains.jet.storage.StorageManager;

import java.util.*;

import static org.jetbrains.jet.descriptors.serialization.SerializationPackage.*;
import static org.jetbrains.jet.lang.resolve.name.SpecialNames.getClassObjectName;

public class DeserializedClassDescriptor extends AbstractClassDescriptor implements ClassDescriptor {

    private final ClassId classId;
    private final ProtoBuf.Class classProto;
    private final DeserializedMemberScope memberScope;

    private final NullableLazyValue<ConstructorDescriptor> primaryConstructor;

    private final NotNullLazyValue<Annotations> annotations;

    private final NullableLazyValue<ClassDescriptor> classObjectDescriptor;

    private final NestedClassDescriptors nestedClasses;

    private final NotNullLazyValue<DeclarationDescriptor> containingDeclaration;
    private final DeserializedClassTypeConstructor typeConstructor;
    private final Modality modality;
    private final Visibility visibility;
    private final ClassKind kind;
    private final boolean isInner;
    private final DeserializationContextWithTypes context;

    public DeserializedClassDescriptor(@NotNull DeserializationGlobalContext globalContext, @NotNull ClassData classData) {
        this(globalContext.withNameResolver(classData.getNameResolver()), classData.getClassProto());
    }

    public DeserializedClassDescriptor(@NotNull DeserializationContext outerContext, @NotNull ProtoBuf.Class classProto) {
        super(outerContext.getStorageManager(),
              outerContext.getNameResolver().getClassId(classProto.getFqName()).getRelativeClassName().shortName());
        this.classProto = classProto;
        this.classId = outerContext.getNameResolver().getClassId(classProto.getFqName());

        List<TypeParameterDescriptor> typeParameters = new ArrayList<TypeParameterDescriptor>(classProto.getTypeParameterCount());
        this.context = outerContext.withTypes(this).childContext(this, classProto.getTypeParameterList(), typeParameters);

        this.containingDeclaration = outerContext.getStorageManager().createLazyValue(new Function0<DeclarationDescriptor>() {
            @Override
            public DeclarationDescriptor invoke() {
                return computeContainingDeclaration();
            }
        });

        this.typeConstructor = new DeserializedClassTypeConstructor(typeParameters);
        this.memberScope = new DeserializedClassMemberScope();

        int flags = classProto.getFlags();
        this.modality = modality(Flags.MODALITY.get(flags));
        this.visibility = visibility(Flags.VISIBILITY.get(flags));
        this.kind = classKind(Flags.CLASS_KIND.get(flags));
        this.isInner = Flags.INNER.get(flags);

        this.annotations = context.getStorageManager().createLazyValue(new Function0<Annotations>() {
            @Override
            public Annotations invoke() {
                return computeAnnotations();
            }
        });

        this.primaryConstructor = context.getStorageManager().createNullableLazyValue(new Function0<ConstructorDescriptor>() {
            @Override
            public ConstructorDescriptor invoke() {
                return computePrimaryConstructor();
            }
        });

        this.classObjectDescriptor = context.getStorageManager().createNullableLazyValue(new Function0<ClassDescriptor>() {
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
            List<PackageFragmentDescriptor> fragments =
                    context.getPackageFragmentProvider().getPackageFragments(classId.getPackageFqName());
            assert fragments.size() == 1 : "there should be exactly one package: " + fragments;
            return fragments.iterator().next();
        }
        else {
            ClassDescriptor result = ContextPackage.deserializeClass(context, classId.getOuterClassId());
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

    private Annotations computeAnnotations() {
        if (!Flags.HAS_ANNOTATIONS.get(classProto.getFlags())) {
            return Annotations.EMPTY;
        }
        return context.getAnnotationLoader().loadClassAnnotations(this, classProto);
    }

    @NotNull
    @Override
    public Annotations getAnnotations() {
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

        return (ConstructorDescriptor) context.getDeserializer().loadCallable(constructorProto.getData());
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

            return new DeserializedClassDescriptor(context, classObjectProto.getData());
        }

        return ContextPackage.deserializeClass(context, classId.createNestedClassId(getClassObjectName(getName())));
    }

    @NotNull
    private ClassDescriptorWithResolutionScopes createEnumClassObject() {
        final MutableClassDescriptor classObject = new MutableClassDescriptor(this, getScopeForMemberLookup(), ClassKind.CLASS_OBJECT,
                                                                              false, getClassObjectName(getName()));
        JetType supertype = KotlinBuiltIns.getInstance().getAnyType();
        classObject.setSupertypes(Collections.singleton(supertype));
        classObject.setModality(Modality.FINAL);
        classObject.setVisibility(DescriptorUtils.getSyntheticClassObjectVisibility());
        classObject.setTypeParameterDescriptors(Collections.<TypeParameterDescriptor>emptyList());
        classObject.setPrimaryConstructor(DescriptorFactory.createPrimaryConstructorForObject(classObject));
        classObject.createTypeConstructor();

        JetType enumType = getDefaultType();
        JetType enumArrayType = KotlinBuiltIns.getInstance().getArrayType(enumType);
        classObject.getBuilder().addFunctionDescriptor(DescriptorFactory.createEnumClassObjectValuesMethod(classObject, enumArrayType));
        classObject.getBuilder().addFunctionDescriptor(DescriptorFactory.createEnumClassObjectValueOfMethod(classObject, enumType));

        OverridingUtil.DescriptorSink sink = new OverridingUtil.DescriptorSink() {
            @Override
            public void addToScope(@NotNull CallableMemberDescriptor fakeOverride) {
                OverridingUtil.resolveUnknownVisibilityForMember(fakeOverride, null);
                classObject.getBuilder().addFunctionDescriptor((SimpleFunctionDescriptor) fakeOverride);
            }

            @Override
            public void conflict(@NotNull CallableMemberDescriptor fromSuper, @NotNull CallableMemberDescriptor fromCurrent) {
                throw new IllegalStateException("Conflict on enum class object override: " + fromSuper + " vs " + fromCurrent);
            }
        };

        JetScope superScope = supertype.getMemberScope();

        for (DeclarationDescriptor descriptor : superScope.getAllDescriptors()) {
            if (descriptor instanceof FunctionDescriptor) {
                Name name = descriptor.getName();
                OverridingUtil.generateOverridesInFunctionGroup(name, superScope.getFunctions(name),
                                                                Collections.<FunctionDescriptor>emptySet(), classObject, sink);
            }
        }

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
            supertypes.add(context.getTypeDeserializer().type(supertype));
        }
        return supertypes;
    }

    @Override
    public String toString() {
        // not using descriptor render to preserve laziness
        return "deserialized class " + getName().toString();
    }

    private class DeserializedClassTypeConstructor extends AbstractClassTypeConstructor {
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
            // We cannot have error supertypes because subclasses inherit error functions from them
            // Filtering right away means copying the list every time, so we check for the rare condition first, and only then filter
            for (JetType supertype : supertypes) {
                if (supertype.isError()) {
                    return KotlinPackage.filter(supertypes, new Function1<JetType, Boolean>() {
                        @Override
                        public Boolean invoke(JetType type) {
                            return !type.isError();
                        }
                    });
                }
            }
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
        public Annotations getAnnotations() {
            return Annotations.EMPTY; // TODO
        }

        @Override
        public String toString() {
            return getName().toString();
        }
    }

    private class DeserializedClassMemberScope extends DeserializedMemberScope {
        private final DeserializedClassDescriptor classDescriptor;

        public DeserializedClassMemberScope() {
            super(context, DeserializedClassDescriptor.this.classProto.getMemberList());
            this.classDescriptor = DeserializedClassDescriptor.this;
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
                            // TODO: report "cannot infer visibility"
                            OverridingUtil.resolveUnknownVisibilityForMember(fakeOverride, null);
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

            final StorageManager storageManager = context.getStorageManager();
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
                        return ContextPackage.deserializeClass(context, classId.createNestedClassId(name));
                    }
                    return null;
                }
            });
        }

        @NotNull
        private Set<Name> nestedClassNames() {
            Set<Name> result = new HashSet<Name>();
            NameResolver nameResolver = context.getNameResolver();
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
            NameResolver nameResolver = context.getNameResolver();
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

            final NameResolver nameResolver = context.getNameResolver();
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
