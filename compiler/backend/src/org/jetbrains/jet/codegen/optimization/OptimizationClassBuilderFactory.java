/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.codegen.optimization;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.codegen.ClassBuilder;
import org.jetbrains.jet.codegen.ClassBuilderFactory;
import org.jetbrains.jet.codegen.ClassBuilderMode;
import org.jetbrains.jet.lang.resolve.java.diagnostics.JvmDeclarationOrigin;

public class OptimizationClassBuilderFactory implements ClassBuilderFactory {
    private final ClassBuilderFactory delegate;

    public OptimizationClassBuilderFactory(ClassBuilderFactory delegate) {
        this.delegate = delegate;
    }

    @NotNull
    @Override
    public ClassBuilderMode getClassBuilderMode() {
        return delegate.getClassBuilderMode();
    }

    @NotNull
    @Override
    public ClassBuilder newClassBuilder(@NotNull JvmDeclarationOrigin origin) {
        return new OptimizationClassBuilder(delegate.newClassBuilder(origin));
    }

    @Override
    public String asText(ClassBuilder builder) {
        return delegate.asText(((OptimizationClassBuilder) builder).getDelegate());
    }

    @Override
    public byte[] asBytes(ClassBuilder builder) {
        return delegate.asBytes(((OptimizationClassBuilder) builder).getDelegate());
    }
}
