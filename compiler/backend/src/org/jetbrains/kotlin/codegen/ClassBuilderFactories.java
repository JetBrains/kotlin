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

package org.jetbrains.kotlin.codegen;

import kotlin.jvm.functions.Function1;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.codegen.state.GenerationState;
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOrigin;
import org.jetbrains.org.objectweb.asm.ClassWriter;
import org.jetbrains.org.objectweb.asm.util.TraceClassVisitor;

import java.io.PrintWriter;
import java.io.StringWriter;

@SuppressWarnings("IOResourceOpenedButNotSafelyClosed")
public class ClassBuilderFactories {
    @NotNull
    public static final ClassBuilderFactory THROW_EXCEPTION = new ClassBuilderFactory() {
        @NotNull
        @Override
        public ClassBuilderMode getClassBuilderMode() {
            return ClassBuilderMode.full(false);
        }

        @NotNull
        @Override
        public ClassBuilder newClassBuilder(@NotNull JvmDeclarationOrigin origin) {
            throw new IllegalStateException();
        }

        @Override
        public String asText(ClassBuilder builder) {
            throw new IllegalStateException();
        }

        @Override
        public byte[] asBytes(ClassBuilder builder) {
            throw new IllegalStateException();
        }

        @Override
        public void close() {
            throw new IllegalStateException();
        }
    };

    private ClassBuilderFactories() {
    }
}
