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

import com.google.common.collect.Sets;
import jet.Function0;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptorVisitor;
import org.jetbrains.jet.lang.descriptors.TypeParameterDescriptor;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.resolve.scopes.LazyScopeAdapter;
import org.jetbrains.jet.lang.types.*;
import org.jetbrains.jet.lang.types.checker.JetTypeChecker;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;
import org.jetbrains.jet.renderer.DescriptorRenderer;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.jetbrains.jet.storage.LockBasedStorageManager.NO_LOCKS;

public class TypeParameterDescriptorImpl extends DeclarationDescriptorNonRootImpl implements TypeParameterDescriptor {
    public static TypeParameterDescriptor createWithDefaultBound(
            @NotNull DeclarationDescriptor containingDeclaration,
            @NotNull List<AnnotationDescriptor> annotations,
            boolean reified,
            @NotNull Variance variance,
            @NotNull Name name,
            int index) {
        TypeParameterDescriptorImpl typeParameterDescriptor = createForFurtherModification(containingDeclaration, annotations, reified, variance, name, index);
        typeParameterDescriptor.addUpperBound(KotlinBuiltIns.getInstance().getDefaultBound());
        typeParameterDescriptor.setInitialized();
        return typeParameterDescriptor;
    }

    public static TypeParameterDescriptorImpl createForFurtherModification(
            @NotNull DeclarationDescriptor containingDeclaration,
            @NotNull List<AnnotationDescriptor> annotations,
            boolean reified,
            @NotNull Variance variance,
            @NotNull Name name,
            int index) {
        return new TypeParameterDescriptorImpl(containingDeclaration, annotations, reified, variance, name, index);
    }

    // 0-based
    private final int index;
    private final Variance variance;
    private final Set<JetType> upperBounds;
    private JetType upperBoundsAsType;
    private final TypeConstructor typeConstructor;
    private JetType defaultType;
    private final Set<JetType> classObjectUpperBounds = Sets.newLinkedHashSet();
    private JetType classObjectBoundsAsType;

    private final boolean reified;

    private boolean initialized = false;

    private TypeParameterDescriptorImpl(
            @NotNull DeclarationDescriptor containingDeclaration,
            @NotNull List<AnnotationDescriptor> annotations,
            boolean reified,
            @NotNull Variance variance,
            @NotNull Name name,
            int index) {
        super(containingDeclaration, annotations, name);
        this.index = index;
        this.variance = variance;
        this.upperBounds = Sets.newLinkedHashSet();
        this.reified = reified;
        // TODO: Should we actually pass the annotations on to the type constructor?
        this.typeConstructor = new TypeConstructorImpl(
                this,
                annotations,
                false,
                name.asString(),
                Collections.<TypeParameterDescriptor>emptyList(),
                upperBounds);
    }

    private void checkInitialized() {
        if (!initialized) {
            throw new IllegalStateException("Type parameter descriptor is not initialized: " + nameForAssertions());
        }
    }

    private void checkUninitialized() {
        if (initialized) {
            throw new IllegalStateException("Type parameter descriptor is already initialized: " + nameForAssertions());
        }
    }

    private String nameForAssertions() {
        return getName() + " declared in " + DescriptorUtils.getFqName(getContainingDeclaration());
    }

    public void setInitialized() {
        checkUninitialized();
        initialized = true;
    }

    @Override
    public boolean isReified() {
        checkInitialized();
        return reified;
    }

    @Override
    @NotNull
    public Variance getVariance() {
        return variance;
    }

    public void addUpperBound(@NotNull JetType bound) {
        checkUninitialized();
        doAddUpperBound(bound);
    }

    private void doAddUpperBound(JetType bound) {
        upperBounds.add(bound); // TODO : Duplicates?
    }

    public void addDefaultUpperBound() {
        checkUninitialized();

        if (upperBounds.isEmpty()) {
            doAddUpperBound(KotlinBuiltIns.getInstance().getDefaultBound());
        }
    }

    @Override
    @NotNull
    public Set<JetType> getUpperBounds() {
        checkInitialized();
        return upperBounds;
    }

    @Override
    @NotNull
    public JetType getUpperBoundsAsType() {
        checkInitialized();
        if (upperBoundsAsType == null) {
            assert upperBounds != null : "Upper bound list is null in " + getName();
            assert upperBounds.size() > 0 : "Upper bound list is empty in " + getName();
            upperBoundsAsType = TypeUtils.intersect(JetTypeChecker.INSTANCE, upperBounds);
            if (upperBoundsAsType == null) {
                upperBoundsAsType = KotlinBuiltIns.getInstance().getNothingType();
            }
        }
        return upperBoundsAsType;
    }

    @Override
    @NotNull
    public Set<JetType> getLowerBounds() {
        //checkInitialized();
        return Collections.singleton(KotlinBuiltIns.getInstance().getNothingType());
    }

    @Override
    @NotNull
    public JetType getLowerBoundsAsType() {
        checkInitialized();
        return KotlinBuiltIns.getInstance().getNothingType();
    }
    
    
    @NotNull
    @Override
    public TypeConstructor getTypeConstructor() {
        //checkInitialized();
        return typeConstructor;
    }

    @Override
    public String toString() {
        try {
            return DescriptorRenderer.TEXT.render(this);
        } catch (Exception e) {
            return this.getClass().getName() + "@" + System.identityHashCode(this);
        }
    }

    @NotNull
    @Override
    @Deprecated
    public TypeParameterDescriptor substitute(@NotNull TypeSubstitutor substitutor) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <R, D> R accept(DeclarationDescriptorVisitor<R, D> visitor, D data) {
        checkInitialized();
        return visitor.visitTypeParameterDescriptor(this, data);
    }

    @NotNull
    @Override
    public JetType getDefaultType() {
        //checkInitialized();
        if (defaultType == null) {
            defaultType = new JetTypeImpl(
                            Collections.<AnnotationDescriptor>emptyList(),
                            getTypeConstructor(),
                            TypeUtils.hasNullableLowerBound(this),
                            Collections.<TypeProjection>emptyList(),
                            new LazyScopeAdapter(NO_LOCKS.createLazyValue(new Function0<JetScope>() {
                                @Override
                                public JetScope invoke() {
                                    return getUpperBoundsAsType().getMemberScope();
                                }
                            })));
        }
        return defaultType;
    }

    @Override
    public JetType getClassObjectType() {
        checkInitialized();
        if (classObjectUpperBounds.isEmpty()) return null;

        if (classObjectBoundsAsType == null) {
            classObjectBoundsAsType = TypeUtils.intersect(JetTypeChecker.INSTANCE, classObjectUpperBounds);
            if (classObjectBoundsAsType == null) {
                classObjectBoundsAsType = KotlinBuiltIns.getInstance().getNothingType();
            }
        }
        return classObjectBoundsAsType;
    }

    public void addClassObjectBound(@NotNull JetType bound) {
        checkUninitialized();
        classObjectUpperBounds.add(bound); // TODO : Duplicates?
    }

    @Override
    public int getIndex() {
        checkInitialized();
        return index;
    }
}
