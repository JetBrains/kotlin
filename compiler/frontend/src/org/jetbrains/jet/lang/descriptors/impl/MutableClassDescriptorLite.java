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

package org.jetbrains.jet.lang.descriptors.impl;

import com.google.common.collect.Lists;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.resolve.DescriptorResolver;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.scopes.InnerClassesScopeWrapper;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.resolve.scopes.WritableScope;
import org.jetbrains.jet.lang.types.*;
import org.jetbrains.jet.renderer.DescriptorRenderer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public abstract class MutableClassDescriptorLite extends ClassDescriptorBase {

    private List<AnnotationDescriptor> annotations = Lists.newArrayList();

    private List<TypeParameterDescriptor> typeParameters;
    private Collection<JetType> supertypes = Lists.newArrayList();

    private TypeConstructor typeConstructor;

    private Modality modality;
    private Visibility visibility;

    private final boolean isInner;

    private MutableClassDescriptorLite classObjectDescriptor;
    private JetType classObjectType;
    private final ClassKind kind;

    private JetScope scopeForMemberLookup;
    private JetScope innerClassesScope;

    private ReceiverParameterDescriptor implicitReceiver;

    private Name name;
    private final DeclarationDescriptor containingDeclaration;

    public MutableClassDescriptorLite(@NotNull DeclarationDescriptor containingDeclaration,
                                      @NotNull ClassKind kind,
                                      boolean isInner
    ) {
        this.containingDeclaration = containingDeclaration;
        this.kind = kind;
        this.isInner = isInner;
    }

    @NotNull
    @Override
    public DeclarationDescriptor getContainingDeclaration() {
        return containingDeclaration;
    }

    @NotNull
    @Override
    public Name getName() {
        return name;
    }

    public void setName(@NotNull Name name) {
        assert this.name == null : this.name;
        this.name = name;
    }

    @NotNull
    @Override
    public DeclarationDescriptor getOriginal() {
        return this;
    }

    @NotNull
    @Override
    public TypeConstructor getTypeConstructor() {
        return typeConstructor;
    }

    public void setScopeForMemberLookup(JetScope scopeForMemberLookup) {
        this.scopeForMemberLookup = scopeForMemberLookup;
        this.innerClassesScope = new InnerClassesScopeWrapper(scopeForMemberLookup);
    }

    public void createTypeConstructor() {
        assert typeConstructor == null : typeConstructor;
        this.typeConstructor = new TypeConstructorImpl(
                this,
                Collections.<AnnotationDescriptor>emptyList(), // TODO : pass annotations from the class?
                !getModality().isOverridable(),
                getName().getName(),
                typeParameters,
                supertypes);
    }

    private WritableScope getScopeForMemberLookupAsWritableScope() {
        // hack
        return (WritableScope) scopeForMemberLookup;
    }


    @NotNull
    public JetScope getScopeForMemberLookup() {
        return scopeForMemberLookup;
    }

    @Override
    public JetType getClassObjectType() {
        if (classObjectType == null && classObjectDescriptor != null) {
            classObjectType = classObjectDescriptor.getDefaultType();
        }
        return classObjectType;
    }

    @Override
    public boolean isClassObjectAValue() {
        return true;
    }


    @NotNull
    @Override
    public ClassKind getKind() {
        return kind;
    }

    public void setModality(Modality modality) {
        this.modality = modality;
    }

    public void setVisibility(Visibility visibility) {
        this.visibility = visibility;
    }

    @Override
    @NotNull
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

    public Collection<JetType> getSupertypes() {
        return supertypes;
    }

    public void setSupertypes(@NotNull Collection<JetType> supertypes) {
        this.supertypes = supertypes;
    }


    @Override
    @Nullable
    public MutableClassDescriptorLite getClassObjectDescriptor() {
        return classObjectDescriptor;
    }



    @NotNull
    @Override
    public JetScope getUnsubstitutedInnerClassesScope() {
        return innerClassesScope;
    }


    public void addSupertype(@NotNull JetType supertype) {
        assert !ErrorUtils.isErrorType(supertype) : "Error types must be filtered out in DescriptorResolver";
        if (TypeUtils.getClassDescriptor(supertype) != null) {
            // See the Errors.SUPERTYPE_NOT_A_CLASS_OR_TRAIT
            supertypes.add(supertype);
        }
    }

    public void setTypeParameterDescriptors(List<TypeParameterDescriptor> typeParameters) {
        if (this.typeParameters != null) {
            throw new IllegalStateException();
        }
        this.typeParameters = new ArrayList<TypeParameterDescriptor>();
        for (TypeParameterDescriptor typeParameterDescriptor : typeParameters) {
            this.typeParameters.add(typeParameterDescriptor);
        }
    }

    public void lockScopes() {
        getScopeForMemberLookupAsWritableScope().changeLockLevel(WritableScope.LockLevel.READING);
        if (classObjectDescriptor != null) {
            classObjectDescriptor.lockScopes();
        }
    }

    @NotNull
    @Override
    public ReceiverParameterDescriptor getThisAsReceiverParameter() {
        if (implicitReceiver == null) {
            implicitReceiver = DescriptorResolver.createLazyReceiverParameterDescriptor(this);
        }
        return implicitReceiver;
    }

    @Override
    public String toString() {
        try {
            return DescriptorRenderer.TEXT.render(this) + "[" + getClass().getCanonicalName() + "@" + System.identityHashCode(this) + "]";
        } catch (Throwable e) {
            return super.toString();
        }
    }

    @Override
    public List<AnnotationDescriptor> getAnnotations() {
        return annotations;
    }

    public void setAnnotations(List<AnnotationDescriptor> annotations) {
        this.annotations = annotations;
    }

    private NamespaceLikeBuilder builder = null;
    public NamespaceLikeBuilder getBuilder() {
        if (builder == null) {
            builder = new NamespaceLikeBuilderDummy() {
                @NotNull
                @Override
                public DeclarationDescriptor getOwnerForChildren() {
                    return MutableClassDescriptorLite.this;
                }

                @Override
                public void addClassifierDescriptor(@NotNull MutableClassDescriptorLite classDescriptor) {
                    getScopeForMemberLookupAsWritableScope().addClassifierDescriptor(classDescriptor);
                }

                @Override
                public void addObjectDescriptor(@NotNull MutableClassDescriptorLite objectDescriptor) {
                    getScopeForMemberLookupAsWritableScope().addObjectDescriptor(objectDescriptor);
                }

                @Override
                public void addFunctionDescriptor(@NotNull SimpleFunctionDescriptor functionDescriptor) {
                    getScopeForMemberLookupAsWritableScope().addFunctionDescriptor(functionDescriptor);
                }

                @Override
                public void addPropertyDescriptor(@NotNull PropertyDescriptor propertyDescriptor) {
                    getScopeForMemberLookupAsWritableScope().addPropertyDescriptor(propertyDescriptor);
                }

                @Override
                public ClassObjectStatus setClassObjectDescriptor(@NotNull MutableClassDescriptorLite classObjectDescriptor) {
                    if (getKind().isObject() || isInner()) {
                        return ClassObjectStatus.NOT_ALLOWED;
                    }

                    if (MutableClassDescriptorLite.this.classObjectDescriptor != null) {
                        return ClassObjectStatus.DUPLICATE;
                    }

                    assert classObjectDescriptor.getKind() == ClassKind.CLASS_OBJECT;
                    MutableClassDescriptorLite.this.classObjectDescriptor = classObjectDescriptor;

                    return ClassObjectStatus.OK;
                }
            };
        }

        return builder;
    }
}
