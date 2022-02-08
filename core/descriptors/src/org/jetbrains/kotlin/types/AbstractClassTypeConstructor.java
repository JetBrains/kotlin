/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.types;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.builtins.KotlinBuiltIns;
import org.jetbrains.kotlin.descriptors.ClassDescriptor;
import org.jetbrains.kotlin.descriptors.ClassifierDescriptor;
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor;
import org.jetbrains.kotlin.descriptors.ModalityUtilsKt;
import org.jetbrains.kotlin.resolve.descriptorUtil.DescriptorUtilsKt;
import org.jetbrains.kotlin.storage.StorageManager;
import org.jetbrains.kotlin.utils.SmartList;

import java.util.Collection;
import java.util.Collections;

public abstract class AbstractClassTypeConstructor extends AbstractTypeConstructor implements TypeConstructor {
    public AbstractClassTypeConstructor(@NotNull StorageManager storageManager) {
        super(storageManager);
    }

    @NotNull
    @Override
    public abstract ClassDescriptor getDeclarationDescriptor();

    @Override
    public final boolean isFinal() {
        ClassDescriptor descriptor = getDeclarationDescriptor();
        return ModalityUtilsKt.isFinalClass(descriptor) && !descriptor.isExpect();
    }

    @NotNull
    @Override
    public KotlinBuiltIns getBuiltIns() {
        return DescriptorUtilsKt.getBuiltIns(getDeclarationDescriptor());
    }

    @Override
    protected boolean isSameClassifier(@NotNull ClassifierDescriptor classifier) {
        return classifier instanceof ClassDescriptor && areFqNamesEqual(getDeclarationDescriptor(), classifier);
    }

    @NotNull
    @Override
    protected Collection<KotlinType> getAdditionalNeighboursInSupertypeGraph(boolean useCompanions) {
        DeclarationDescriptor containingDeclaration = getDeclarationDescriptor().getContainingDeclaration();

        if (!(containingDeclaration instanceof ClassDescriptor)) {
            return Collections.emptyList();
        }

        Collection<KotlinType> additionalNeighbours = new SmartList<KotlinType>();

        // We suppose that there is an edge from C to A in graph when disconnecting loops in supertypes,
        // because such cyclic declarations should be prohibited (see p.10.2.1 of Kotlin spec)
        // class A : B {
        //   static class C {}
        // }
        // class B : A.C {}
        ClassDescriptor containingClassDescriptor = (ClassDescriptor) containingDeclaration;
        additionalNeighbours.add(containingClassDescriptor.getDefaultType());

        // Also we add edge from host-class to companion object. Together with previous edges
        // (from nesteds to containing class), they can create visibility loops like in the
        // following example:
        //
        // class ContainingClass {
        //   open class Nested {}  // to create scope for resolving Nested, we have to resolve CO header
        //   companion object : Nested() {} // to resolve CO header, we have to resolve Nested
        // }
        //
        // Relates to KT-21515
        ClassDescriptor companion = containingClassDescriptor.getCompanionObjectDescriptor();
        if (useCompanions && companion != null) {
            additionalNeighbours.add(companion.getDefaultType());
        }

        return additionalNeighbours;
    }

    @Nullable
    @Override
    protected KotlinType defaultSupertypeIfEmpty() {
        if (KotlinBuiltIns.isSpecialClassWithNoSupertypes(this.getDeclarationDescriptor())) {
            return null;
        } else {
            return getBuiltIns().getAnyType();
        }
    }
}
