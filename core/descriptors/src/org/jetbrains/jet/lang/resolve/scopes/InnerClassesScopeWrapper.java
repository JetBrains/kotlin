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

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Collections2;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.ClassifierDescriptor;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.ReceiverParameterDescriptor;
import org.jetbrains.jet.lang.resolve.name.LabelName;
import org.jetbrains.jet.lang.resolve.name.Name;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class InnerClassesScopeWrapper extends AbstractScopeAdapter {
    private static final Predicate<Object> IS_CLASS = Predicates.instanceOf(ClassDescriptor.class);

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
        return IS_CLASS.apply(classifier) ? classifier : null;
    }

    @NotNull
    @Override
    public Collection<DeclarationDescriptor> getDeclarationsByLabel(LabelName labelName) {
        return Collections2.filter(actualScope.getDeclarationsByLabel(labelName), IS_CLASS);
    }

    @NotNull
    @Override
    public Collection<DeclarationDescriptor> getAllDescriptors() {
        return Collections2.filter(actualScope.getAllDescriptors(), IS_CLASS);
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
