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
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.codegen.ClassBuilder;
import org.jetbrains.jet.codegen.DelegatingClassBuilder;
import org.jetbrains.jet.lang.resolve.java.diagnostics.JvmDeclarationOrigin;
import org.jetbrains.org.objectweb.asm.MethodVisitor;

public class OptimizationClassBuilder extends DelegatingClassBuilder {
    private final ClassBuilder delegate;

    public OptimizationClassBuilder(@NotNull ClassBuilder delegate) {
        this.delegate = delegate;
    }

    @NotNull
    @Override
    public ClassBuilder getDelegate() {
        return delegate;
    }

    @NotNull
    @Override
    public MethodVisitor newMethod(
            @NotNull JvmDeclarationOrigin origin,
            int access,
            @NotNull String name,
            @NotNull String desc,
            @Nullable String signature,
            @Nullable String[] exceptions
    ) {
        return new OptimizationMethodVisitor(
                super.newMethod(origin, access, name, desc, signature, exceptions),
                access, name, desc, signature, exceptions
        );
    }
}
