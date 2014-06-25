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

package org.jetbrains.jet.codegen;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.codegen.optimization.boxing.RedundantBoxingMethodTransformer;
import org.jetbrains.jet.codegen.optimization.transformer.ClassMethodsTransformer;
import org.jetbrains.jet.lang.resolve.java.diagnostics.JvmDeclarationOrigin;
import org.jetbrains.org.objectweb.asm.ClassWriter;
import org.jetbrains.org.objectweb.asm.util.TraceClassVisitor;

import java.io.PrintWriter;
import java.io.StringWriter;

@SuppressWarnings("IOResourceOpenedButNotSafelyClosed")
public class ClassBuilderFactories {
    @NotNull
    public static ClassBuilderFactory THROW_EXCEPTION = new ClassBuilderFactory() {
        @NotNull
        @Override
        public ClassBuilderMode getClassBuilderMode() {
            return ClassBuilderMode.FULL;
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
    };

    @NotNull
    public static ClassBuilderFactory TEST = new ClassBuilderFactory() {
        @NotNull
        @Override
        public ClassBuilderMode getClassBuilderMode() {
            return ClassBuilderMode.FULL;
        }

        @NotNull
        @Override
        public ClassBuilder newClassBuilder(@NotNull JvmDeclarationOrigin origin) {
            return new TraceBuilder(new BinaryClassWriter());
        }

        @Override
        public String asText(ClassBuilder builder) {
            TraceClassVisitor visitor = (TraceClassVisitor) builder.getVisitor();

            StringWriter writer = new StringWriter();
            visitor.p.print(new PrintWriter(writer));

            return writer.toString();
        }

        @Override
        public byte[] asBytes(ClassBuilder builder) {
            return ((TraceBuilder) builder).binary.toByteArray();
        }
    };

    @NotNull
    public static ClassBuilderFactory BINARIES = new ClassBuilderFactory() {
        @NotNull
        @Override
        public ClassBuilderMode getClassBuilderMode() {
            return ClassBuilderMode.FULL;
        }

        @NotNull
        @Override
        public ClassBuilder newClassBuilder(@NotNull JvmDeclarationOrigin origin) {
            return new AbstractClassBuilder.Concrete(new BinaryClassWriter());
        }

        @Override
        public String asText(ClassBuilder builder) {
            throw new UnsupportedOperationException("BINARIES generator asked for text");
        }

        @Override
        public byte[] asBytes(ClassBuilder builder) {
            ClassWriter visitor = (ClassWriter) builder.getVisitor();
            return visitor.toByteArray();
        }
    };

    private ClassBuilderFactories() {
    }

    private static class BinaryClassWriter extends ClassWriter {
        public BinaryClassWriter() {
            super(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        }

        @Override
        protected String getCommonSuperClass(String type1, String type2) {
            try {
                return super.getCommonSuperClass(type1, type2);
            }
            catch (Throwable t) {
                // @todo we might need at some point do more sophisticated handling
                return "java/lang/Object";
            }
        }
    }

    private static final ClassMethodsTransformer defaultOptimizationsClassMethodsTransformer = new ClassMethodsTransformer(
        new RedundantBoxingMethodTransformer(null)
    );

    private static class BinaryClassWriterOptimizationsDecorator extends BinaryClassWriter {
        private final ClassMethodsTransformer classMethodsTransformer = defaultOptimizationsClassMethodsTransformer;
        public BinaryClassWriterOptimizationsDecorator() {
            super();
        }

        @Override
        public byte[] toByteArray() {
            return classMethodsTransformer.transform(super.toByteArray());
        }
    }

    private static class TraceBuilder extends AbstractClassBuilder.Concrete {
        public final BinaryClassWriter binary;

        public TraceBuilder(BinaryClassWriter binary) {
            super(new TraceClassVisitor(binary, new PrintWriter(new StringWriter())));
            this.binary = binary;
        }
    }
}
