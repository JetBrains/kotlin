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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.descriptors.serialization.*;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.descriptors.impl.AbstractReceiverParameterDescriptor;
import org.jetbrains.jet.lang.descriptors.impl.ClassDescriptorBase;
import org.jetbrains.jet.lang.resolve.OverrideResolver;
import org.jetbrains.jet.lang.resolve.TraceUtil;
import org.jetbrains.jet.lang.resolve.lazy.storage.*;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ClassReceiver;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverValue;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.TypeConstructor;

import java.util.*;

public class DeserializedClassDescriptor extends ClassDescriptorBase implements ClassDescriptor {

    private final ProtoBuf.Class classProto;
    private final TypeDeserializer typeDeserializer;
    private final DescriptorDeserializer deserializer;
    private final DeserializedMemberScope memberScope;
    private final ReceiverParameterDescriptor thisAsReceiverParameter;

    private final NullableLazyValue<ConstructorDescriptor> primaryConstructor;

    private final AnnotationDeserializer annotationDeserializer;
    private final NotNullLazyValue<List<AnnotationDescriptor>> annotations;

    private final NestedClassResolver nestedClassResolver;
    private final NullableLazyValue<ClassDescriptor> classObjectDescriptor;

    private final NestedClassDescriptors nestedClasses;
    private final NestedClassDescriptors nestedObjects;

    private final Name name;
    private final DeclarationDescriptor containingDeclaration;
    private final DeserializedClassTypeConstructor typeConstructor;
    private final Modality modality;
    private final Visibility visibility;
    private final ClassKind kind;
    private final boolean isInner;

    public DeserializedClassDescriptor(
            @NotNull DeclarationDescriptor containingDeclaration,
            @NotNull NameResolver nameResolver,
            @NotNull AnnotationDeserializer annotationResolver,
            @NotNull ClassResolver classResolver,
            @NotNull NestedClassResolver _nestedClassResolver,
            @NotNull ProtoBuf.Class classProto,
            @Nullable TypeDeserializer outerTypeDeserializer
    ) {
        this.classProto = classProto;
        this.typeDeserializer = new TypeDeserializer(outerTypeDeserializer, nameResolver, classResolver);
        this.deserializer = DescriptorDeserializer.create(typeDeserializer, this, nameResolver, annotationResolver);

        this.containingDeclaration = containingDeclaration;
        this.typeConstructor = new DeserializedClassTypeConstructor();
        this.memberScope = new DeserializedClassMemberScope(this);
        this.thisAsReceiverParameter = new LazyClassReceiverParameterDescriptor();

        this.name = nameResolver.getName(classProto.getName());
        int flags = classProto.getFlags();
        this.modality = DescriptorDeserializer.modality(Flags.MODALITY.get(flags));
        this.visibility = DescriptorDeserializer.visibility(Flags.VISIBILITY.get(flags));
        this.kind = DescriptorDeserializer.classKind(Flags.CLASS_KIND.get(flags));
        this.isInner = Flags.INNER.get(flags);

        this.annotationDeserializer = annotationResolver;
        this.annotations = new NotNullLazyValueImpl<List<AnnotationDescriptor>>() {
            @NotNull
            @Override
            protected List<AnnotationDescriptor> doCompute() {
                return computeAnnotations();
            }
        };

        this.primaryConstructor = new NullableLazyValueImpl<ConstructorDescriptor>() {
            @Override
            protected ConstructorDescriptor doCompute() {
                return computePrimaryConstructor();
            }
        };

        this.nestedClassResolver = _nestedClassResolver;
        this.classObjectDescriptor = new NullableLazyValueImpl<ClassDescriptor>() {
            @Override
            protected ClassDescriptor doCompute() {
                return computeClassObjectDecriptor();
            }
        };
        this.nestedClasses = new NestedClassDescriptors(stringSet(classProto.getNestedClassNamesList(), nameResolver)) {
            @Override
            protected ClassDescriptor resolveNestedClass(@NotNull Name name) {
                return nestedClassResolver.resolveNestedClass(DeserializedClassDescriptor.this, name);
            }
        };
        this.nestedObjects = new NestedClassDescriptors(stringSet(classProto.getNestedObjectNamesList(), nameResolver)) {
            @Override
            protected ClassDescriptor resolveNestedClass(@NotNull Name name) {
                return nestedClassResolver.resolveNestedClass(DeserializedClassDescriptor.this, name);
            }
        };
    }

    @NotNull
    private static Set<String> stringSet(@NotNull List<Integer> nameIndices, @NotNull NameResolver nameResolver) {
        Set<String> result = new HashSet<String>(nameIndices.size());
        for (Integer index : nameIndices) {
            result.add(nameResolver.getName(index).asString());
        }
        return result;
    }

    @NotNull
    @Override
    public DeclarationDescriptor getOriginal() {
        return this;
    }

    @NotNull
    @Override
    public DeclarationDescriptor getContainingDeclaration() {
        return containingDeclaration;
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

    @NotNull
    @Override
    public Name getName() {
        return name;
    }

    private List<AnnotationDescriptor> computeAnnotations() {
        if (!Flags.HAS_ANNOTATIONS.get(classProto.getFlags())) {
            return Collections.emptyList();
        }
        return annotationDeserializer.loadClassAnnotations(classProto);
    }

    @Override
    public List<AnnotationDescriptor> getAnnotations() {
        return annotations.compute();
    }

    @Override
    protected JetScope getScopeForMemberLookup() {
        return memberScope;
    }

    @NotNull
    @Override
    public JetScope getUnsubstitutedInnerClassesScope() {
        return JetScope.EMPTY; // TODO inner classes
    }

    @Nullable
    private ConstructorDescriptor computePrimaryConstructor() {
        if (!classProto.hasPrimaryConstructor()) return null;

        ProtoBuf.Callable constructorProto = classProto.getPrimaryConstructor();
        return (ConstructorDescriptor) deserializer.loadCallable(constructorProto);
    }

    @Nullable
    @Override
    public ConstructorDescriptor getUnsubstitutedPrimaryConstructor() {
        return primaryConstructor.compute();
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
    @Override
    public JetType getClassObjectType() {
        ClassDescriptor classObjectDescriptor = getClassObjectDescriptor();
        return classObjectDescriptor == null ? null : classObjectDescriptor.getDefaultType();
    }

    @Nullable
    private ClassDescriptor computeClassObjectDecriptor() {
        if (!classProto.getClassObjectPresent()) {
            return null;
        }

        return nestedClassResolver.resolveClassObject(this);
    }

    @Nullable
    @Override
    public ClassDescriptor getClassObjectDescriptor() {
        return classObjectDescriptor.compute();
    }

    @NotNull
    @Override
    public ReceiverParameterDescriptor getThisAsReceiverParameter() {
        return thisAsReceiverParameter;
    }

    private Collection<JetType> computeSuperTypes() {
        List<JetType> supertypes = new ArrayList<JetType>(classProto.getSupertypesCount());
        for (ProtoBuf.Type supertype : classProto.getSupertypesList()) {
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
        private final List<TypeParameterDescriptor> parameters = deserializer.typeParameters(classProto.getTypeParametersList());

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
        public boolean isSealed() {
            return !getModality().isOverridable();
        }

        @Nullable
        @Override
        public ClassifierDescriptor getDeclarationDescriptor() {
            return DeserializedClassDescriptor.this;
        }

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

        public DeserializedClassMemberScope(@NotNull DeserializedClassDescriptor classDescriptor) {
            super(classDescriptor, classDescriptor.deserializer, classDescriptor.classProto.getMembersList());
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
            OverrideResolver.generateOverridesInFunctionGroup(
                    name,
                    fromSupertypes,
                    fromCurrent,
                    classDescriptor,
                    new OverrideResolver.DescriptorSink() {
                        @Override
                        public void addToScope(@NotNull CallableMemberDescriptor fakeOverride) {
                            OverrideResolver.resolveUnknownVisibilityForMember(null, fakeOverride, TraceUtil.TRACE_STUB);
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
            return classDescriptor.nestedClasses.fun(name);
        }

        @Override
        protected void addAllClassDescriptors(@NotNull Collection<DeclarationDescriptor> result) {
            result.addAll(classDescriptor.nestedClasses.getAllDescriptors());
        }

        @Nullable
        @Override
        public ClassDescriptor getObjectDescriptor(@NotNull Name name) {
            return classDescriptor.nestedObjects.fun(name);
        }

        @NotNull
        @Override
        protected Collection<ClassDescriptor> computeAllObjectDescriptors() {
            return classDescriptor.nestedObjects.getAllDescriptors();
        }
    }

    private abstract static class NestedClassDescriptors extends MemoizedFunctionToNullableImpl<Name, ClassDescriptor> {
        private final Set<String> declaredNames;

        public NestedClassDescriptors(@NotNull Set<String> declaredNames) {
            this.declaredNames = declaredNames;
        }

        @Nullable
        @Override
        protected final ClassDescriptor doCompute(@NotNull Name name) {
            if (!declaredNames.contains(name.asString())) return null;

            return resolveNestedClass(name);
        }

        protected abstract ClassDescriptor resolveNestedClass(@NotNull Name name);

        @NotNull
        public Collection<ClassDescriptor> getAllDescriptors() {
            Collection<ClassDescriptor> result = new ArrayList<ClassDescriptor>(declaredNames.size());
            for (String name : declaredNames) {
                ClassDescriptor descriptor = fun(Name.identifier(name));
                if (descriptor != null) {
                    result.add(descriptor);
                }
            }
            return result;
        }
    }

    private class LazyClassReceiverParameterDescriptor extends AbstractReceiverParameterDescriptor {
        private final ClassReceiver classReceiver = new ClassReceiver(DeserializedClassDescriptor.this);

        @NotNull
        @Override
        public JetType getType() {
            return getDefaultType();
        }

        @NotNull
        @Override
        public ReceiverValue getValue() {
            return classReceiver;
        }

        @NotNull
        @Override
        public DeclarationDescriptor getContainingDeclaration() {
            return DeserializedClassDescriptor.this;
        }
    }
}

