/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.generators.tests.generator;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

public class DelegatingTestClassModel implements TestClassModel {
    private final TestClassModel delegate;

    public DelegatingTestClassModel(TestClassModel delegate) {
        this.delegate = delegate;
    }

    @NotNull
    @Override
    public String getName() {
        return delegate.getName();
    }

    @NotNull
    @Override
    public Collection<TestClassModel> getInnerTestClasses() {
        return delegate.getInnerTestClasses();
    }

    @NotNull
    @Override
    public Collection<MethodModel> getMethods() {
        return delegate.getMethods();
    }

    @Override
    public boolean isEmpty() {
        return delegate.isEmpty();
    }

    @Nullable
    @Override
    public String getDataPathRoot() {
        return delegate.getDataPathRoot();
    }

    @Override
    public String getDataString() {
        return delegate.getDataString();
    }
}
