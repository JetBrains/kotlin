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

package org.jetbrains.kotlin.codegen.optimization;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.codegen.ClassBuilder;
import org.jetbrains.kotlin.codegen.DelegatingClassBuilder;
import org.jetbrains.kotlin.config.JVMConstructorCallNormalizationMode;
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOrigin;
import org.jetbrains.org.objectweb.asm.MethodVisitor;

public class OptimizationClassBuilder extends DelegatingClassBuilder {
    private final ClassBuilder delegate;
    private final boolean disableOptimization;
    private final JVMConstructorCallNormalizationMode constructorCallNormalizationMode;

    public OptimizationClassBuilder(
            @NotNull ClassBuilder delegate,
            boolean disableOptimization,
            JVMConstructorCallNormalizationMode constructorCallNormalizationMode
    ) {
        this.delegate = delegate;
        this.disableOptimization = disableOptimization;
        this.constructorCallNormalizationMode = constructorCallNormalizationMode;
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
                disableOptimization, constructorCallNormalizationMode,
                access, name, desc, signature, exceptions
        );
    }
}
