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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.ReceiverParameterDescriptor;

import java.util.Collections;
import java.util.List;

/**
 * Members of the class object are accessible from the class.
 * Scope lazily delegates requests to class object scope.
 */
public class ClassObjectMixinScope extends AbstractScopeAdapter {
    private final ClassDescriptor classObjectDescriptor;

    public ClassObjectMixinScope(ClassDescriptor classObjectDescriptor) {
        this.classObjectDescriptor = classObjectDescriptor;
    }

    @NotNull
    @Override
    protected JetScope getWorkerScope() {
        return classObjectDescriptor.getDefaultType().getMemberScope();
    }

    @NotNull
    @Override
    public List<ReceiverParameterDescriptor> getImplicitReceiversHierarchy() {
        return Collections.singletonList(classObjectDescriptor.getThisAsReceiverParameter());
    }
}
