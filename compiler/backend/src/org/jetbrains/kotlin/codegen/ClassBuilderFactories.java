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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOrigin;
import org.jetbrains.org.objectweb.asm.ClassWriter;
import org.jetbrains.org.objectweb.asm.util.TraceClassVisitor;

import java.io.PrintWriter;
import java.io.StringWriter;

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

        @Override
        public void close() {
            throw new IllegalStateException();
        }
    };
    
    public static ClassBuilderFactory TEST = new TestClassBuilderFactory();

    public static class TestClassBuilderFactory implements ClassBuilderFactory {
        public TestClassBuilderFactory() {}

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

        @Override
        public void close() {

        }
    }
    
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

        @Override
        public void close() {}
    };

    private ClassBuilderFactories() {
    }

    private static class BinaryClassWriter extends ClassWriter {
        public BinaryClassWriter() {
            super(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        }

        @Override
        protected String getCommonSuperClass(@NotNull String type1, @NotNull String type2) {
            // This method is needed to generate StackFrameMap: bytecode metadata for JVM verification. For bytecode version 50.0 (JDK 6)
            // these maps can be invalid: in this case, JVM would generate them itself (potentially slowing class loading),
            // for bytecode 51.0+ (JDK 7+) JVM would crash with VerifyError.
            // It seems that for bytecode emitted by Kotlin compiler, it is safe to return "Object" here, because there will
            // be "checkcast" generated before making a call, anyway.

            return "java/lang/Object";
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
