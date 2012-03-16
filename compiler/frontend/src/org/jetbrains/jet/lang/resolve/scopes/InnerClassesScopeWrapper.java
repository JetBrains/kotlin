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

package org.jetbrains.jet.lang.resolve.scopes;

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.ClassKind;
import org.jetbrains.jet.lang.descriptors.ClassifierDescriptor;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverDescriptor;

import java.util.Collection;
import java.util.List;

/**
 * @author svtk
 */
public class InnerClassesScopeWrapper extends JetScopeImpl {
    private final JetScope actualScope;

    public InnerClassesScopeWrapper(JetScope actualScope) {
        this.actualScope = actualScope;
    }

    private boolean isClass(DeclarationDescriptor descriptor) {
        return descriptor instanceof ClassDescriptor && ((ClassDescriptor) descriptor).getKind() != ClassKind.OBJECT;
    }

    @Override
    public ClassifierDescriptor getClassifier(@NotNull String name) {
        ClassifierDescriptor classifier = actualScope.getClassifier(name);
        if (isClass(classifier)) return classifier;
        return null;
    }

    @NotNull
    @Override
    public DeclarationDescriptor getContainingDeclaration() {
        return actualScope.getContainingDeclaration();
    }

    @NotNull
    @Override
    public Collection<DeclarationDescriptor> getDeclarationsByLabel(String labelName) {
        Collection<DeclarationDescriptor> declarationsByLabel = actualScope.getDeclarationsByLabel(labelName);
        return Collections2.filter(declarationsByLabel, new Predicate<DeclarationDescriptor>() {
            @Override
            public boolean apply(@Nullable DeclarationDescriptor descriptor) {
                return isClass(descriptor);
            }
        });
    }

    @NotNull
    @Override
    public Collection<DeclarationDescriptor> getAllDescriptors() {
        Collection<DeclarationDescriptor> allDescriptors = actualScope.getAllDescriptors();
        return Collections2.filter(allDescriptors, new Predicate<DeclarationDescriptor>() {
            @Override
            public boolean apply(@Nullable DeclarationDescriptor descriptor) {
                return isClass(descriptor);
            }
        });
    }

    @NotNull
    @Override
    public ReceiverDescriptor getImplicitReceiver() {
        return actualScope.getImplicitReceiver();
    }

    @Override
    public void getImplicitReceiversHierarchy(@NotNull List<ReceiverDescriptor> result) {
        actualScope.getImplicitReceiversHierarchy(result);
    }
}
