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

package org.jetbrains.jet.lang.resolve.scopes;

import kotlin.KotlinPackage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.ClassifierDescriptor;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.ReceiverParameterDescriptor;
import org.jetbrains.jet.lang.resolve.name.Name;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class InnerClassesScopeWrapper extends AbstractScopeAdapter {
    private final JetScope actualScope;

    public InnerClassesScopeWrapper(@NotNull JetScope actualScope) {
        this.actualScope = actualScope;
    }

    @NotNull
    @Override
    protected JetScope getWorkerScope() {
        return actualScope;
    }

    @Override
    public ClassifierDescriptor getClassifier(@NotNull Name name) {
        ClassifierDescriptor classifier = actualScope.getClassifier(name);
        return classifier instanceof ClassDescriptor ? classifier : null;
    }

    @NotNull
    @Override
    @SuppressWarnings("unchecked")
    public Collection<DeclarationDescriptor> getDeclarationsByLabel(@NotNull Name labelName) {
        return (Collection) KotlinPackage.filterIsInstance(actualScope.getDeclarationsByLabel(labelName), ClassDescriptor.class);
    }

    @NotNull
    @Override
    @SuppressWarnings("unchecked")
    public Collection<DeclarationDescriptor> getAllDescriptors() {
        return (Collection) KotlinPackage.filterIsInstance(actualScope.getAllDescriptors(), ClassDescriptor.class);
    }

    @NotNull
    @Override
    public List<ReceiverParameterDescriptor> getImplicitReceiversHierarchy() {
        return Collections.emptyList();
    }

    @Override
    public String toString() {
        return "Classes from " + actualScope;
    }
}
