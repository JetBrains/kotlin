package org.jetbrains.jet.lang.resolve.java.resolver;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.asm4.AnnotationVisitor;
import org.jetbrains.asm4.ClassReader;
import org.jetbrains.asm4.ClassVisitor;
import org.jetbrains.asm4.Opcodes;
import org.jetbrains.jet.lang.resolve.java.AbiVersionUtil;
import org.jetbrains.jet.lang.resolve.java.JvmAnnotationNames;
import org.jetbrains.jet.lang.resolve.java.JvmClassName;
import org.jetbrains.jet.lang.resolve.name.FqName;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.jetbrains.asm4.ClassReader.*;
import static org.jetbrains.jet.lang.resolve.java.AbiVersionUtil.isAbiVersionCompatible;

public final class KotlinClassFileHeader {
    private static final Logger LOG = Logger.getInstance(KotlinClassFileHeader.class);

    @NotNull
    public static KotlinClassFileHeader readKotlinHeaderFromClassFile(@NotNull VirtualFile virtualFile) {
        try {
            byte[] bytes = virtualFile.contentsToByteArray();
            ClassReader reader = new ClassReader(bytes);
            KotlinClassFileHeader classFileData = new KotlinClassFileHeader();
            reader.accept(classFileData.new ReadDataFromAnnotationVisitor(), SKIP_CODE | SKIP_FRAMES | SKIP_DEBUG);
            return classFileData;
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("deprecation")
    public enum HeaderType {
        CLASS(JvmAnnotationNames.KOTLIN_CLASS),
        PACKAGE(JvmAnnotationNames.KOTLIN_PACKAGE),
        OLD_CLASS(JvmAnnotationNames.OLD_JET_CLASS_ANNOTATION),
        OLD_PACKAGE(JvmAnnotationNames.OLD_JET_PACKAGE_CLASS_ANNOTATION),
        NONE(null);

        @Nullable
        private final JvmClassName correspondingAnnotation;

        private HeaderType(@Nullable JvmClassName annotation) {
            correspondingAnnotation = annotation;
        }

        private boolean isValidAnnotation() {
            return this == CLASS || this == PACKAGE;
        }

        @NotNull
        private static HeaderType byDescriptor(@NotNull String desc) {
            for (HeaderType headerType : HeaderType.values()) {
                JvmClassName annotation = headerType.correspondingAnnotation;
                if (annotation == null) {
                    continue;
                }
                if (desc.equals(annotation.getDescriptor())) {
                    return headerType;
                }
            }
            return NONE;
        }
    }

    private int version = AbiVersionUtil.INVALID_VERSION;

    @Nullable
    private String[] annotationData = null;
    @NotNull
    private HeaderType type = HeaderType.NONE;
    @Nullable
    private JvmClassName jvmClassName = null;

    public int getVersion() {
        return version;
    }

    @NotNull
    public HeaderType getType() {
        return type;
    }

    /**
     * @return true if this is a header for compiled Kotlin file with correct abi version which can be processed by compiler or the IDE
     */
    public boolean isCompatibleKotlinCompiledFile() {
        return type.isValidAnnotation() && isAbiVersionCompatible(version);
    }

    /**
     * @return FQ name for class header or package class FQ name for package header (e.g. <code>test.TestPackage</code>)
     */
    @NotNull
    public FqName getFqName() {
        assert jvmClassName != null;
        return jvmClassName.getFqName();
    }

    @Nullable
    public String[] getAnnotationData() {
        if (annotationData == null && type != HeaderType.NONE) {
            LOG.error("Data for annotations " + type.correspondingAnnotation + " was not read.");
        }
        return annotationData;
    }

    private class ReadDataFromAnnotationVisitor extends ClassVisitor {

        public ReadDataFromAnnotationVisitor() {
            super(Opcodes.ASM4);
        }

        @Override
        public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
            jvmClassName = JvmClassName.byInternalName(name);
        }

        @Override
        public AnnotationVisitor visitAnnotation(final String desc, boolean visible) {
            HeaderType headerTypeByAnnotation = HeaderType.byDescriptor(desc);
            if (headerTypeByAnnotation == HeaderType.NONE) {
                return null;
            }
            if (headerTypeByAnnotation.isValidAnnotation() && type.isValidAnnotation()) {
                throw new IllegalStateException("Both " + type.correspondingAnnotation + " and "
                                                 + headerTypeByAnnotation.correspondingAnnotation + " present!");
            }
            if (!type.isValidAnnotation()) {
                type = headerTypeByAnnotation;
            }
            if (!headerTypeByAnnotation.isValidAnnotation()) {
                return null;
            }
            return new AnnotationVisitor(Opcodes.ASM4) {
                @Override
                public void visit(String name, Object value) {
                    if (name.equals(JvmAnnotationNames.ABI_VERSION_FIELD_NAME)) {
                        version = (Integer) value;
                    }
                    else if (isAbiVersionCompatible(version)) {
                        throw new IllegalStateException("Unexpected argument " + name + " for annotation " + desc);
                    }
                }

                @Override
                public AnnotationVisitor visitArray(String name) {
                    if (name.equals(JvmAnnotationNames.DATA_FIELD_NAME)) {
                        return stringArrayVisitor();
                    }
                    else if (isAbiVersionCompatible(version)) {
                        throw new IllegalStateException("Unexpected array argument " + name + " for annotation " + desc);
                    }

                    return super.visitArray(name);
                }

                @NotNull
                private AnnotationVisitor stringArrayVisitor() {
                    final List<String> strings = new ArrayList<String>(1);
                    return new AnnotationVisitor(Opcodes.ASM4) {
                        @Override
                        public void visit(String name, Object value) {
                            if (!(value instanceof String)) {
                                throw new IllegalStateException("Unexpected argument value: " + value);
                            }

                            strings.add((String) value);
                        }

                        @Override
                        public void visitEnd() {
                            annotationData = strings.toArray(new String[strings.size()]);
                        }
                    };
                }
            };
        }
    }

    private KotlinClassFileHeader() {
    }
}
