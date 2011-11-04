package org.jetbrains.jet.codegen;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.util.TraceClassVisitor;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * @author max
 */
public interface ClassBuilderFactory {
    ClassBuilder newClassBuilder();
    String asText(ClassBuilder builder);
    byte[] asBytes(ClassBuilder builder);
    
    ClassBuilderFactory TEXT = new ClassBuilderFactory() {
        @Override
        public ClassBuilder newClassBuilder() {
            return new ClassBuilder(new TraceClassVisitor(new PrintWriter(new StringWriter())));
        }

        @Override
        public String asText(ClassBuilder builder) {
            TraceClassVisitor visitor = (TraceClassVisitor) builder.getVisitor();
    
            StringWriter writer = new StringWriter();
            visitor.print(new PrintWriter(writer));
    
            return writer.toString();
        }

        @Override
        public byte[] asBytes(ClassBuilder builder) {
            throw new UnsupportedOperationException("TEXT generator asked for bytes");
        }
    };
    
    ClassBuilderFactory BINARIES = new ClassBuilderFactory() {
        @Override
        public ClassBuilder newClassBuilder() {
            return new ClassBuilder(new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS));
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

}
