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

package org.jetbrains.jet.codegen.state;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.codegen.CompilationErrorHandler;
import org.jetbrains.jet.codegen.NamespaceCodegen;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.name.FqName;

import java.util.Collection;

public class StandardGenerationStrategy implements GenerationStrategy {
    public static final GenerationStrategy INSTANCE = new StandardGenerationStrategy();

    private StandardGenerationStrategy() {
    }

    @Override
    public void generateNamespace(
            @NotNull GenerationState state,
            @NotNull FqName fqName,
            @NotNull Collection<JetFile> jetFiles,
            @NotNull CompilationErrorHandler errorHandler
    ) {
        NamespaceCodegen codegen = state.getFactory().forNamespace(fqName, jetFiles);
        codegen.generate(errorHandler);
    }

}
