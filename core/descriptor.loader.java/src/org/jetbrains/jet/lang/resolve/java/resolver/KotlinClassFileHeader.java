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
            ClassReader reader = new ClassReader(virtualFile.contentsToByteArray());
            ReadDataFromAnnotationVisitor visitor = new ReadDataFromAnnotationVisitor();
            reader.accept(visitor, SKIP_CODE | SKIP_FRAMES | SKIP_DEBUG);
            return new KotlinClassFileHeader(visitor.version, visitor.annotationData, visitor.type, visitor.fqName);
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
        OLD_PACKAGE(JvmAnnotationNames.OLD_JET_PACKAGE_CLASS_ANNOTATION);

        @Nullable
        private final JvmClassName correspondingAnnotation;

        private HeaderType(@Nullable JvmClassName annotation) {
            correspondingAnnotation = annotation;
        }

        private boolean isValidAnnotation() {
            return this == CLASS || this == PACKAGE;
        }

        @Nullable
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
            return null;
        }
    }

    private final int version;
    @Nullable
    private final String[] annotationData;
    @Nullable
    private final HeaderType type;
    @Nullable
    private final FqName fqName;

    private KotlinClassFileHeader(int version, @Nullable String[] annotationData, @Nullable HeaderType type, @Nullable FqName fqName) {
        this.version = version;
        this.annotationData = annotationData;
        this.type = type;
        this.fqName = fqName;
    }

    public int getVersion() {
        return version;
    }

    @Nullable
    public HeaderType getType() {
        return type;
    }

    /**
     * @return true if this is a header for compiled Kotlin file with correct abi version which can be processed by compiler or the IDE
     */
    public boolean isCompatibleKotlinCompiledFile() {
        return type != null && type.isValidAnnotation() && isAbiVersionCompatible(version);
    }

    /**
     * @return FQ name for class header or package class FQ name for package header (e.g. <code>test.TestPackage</code>)
     */
    @NotNull
    public FqName getFqName() {
        assert fqName != null;
        return fqName;
    }

    @Nullable
    public String[] getAnnotationData() {
        if (annotationData == null && type != null) {
            LOG.error("Data for annotations " + type.correspondingAnnotation + " was not read.");
        }
        return annotationData;
    }

    private static class ReadDataFromAnnotationVisitor extends ClassVisitor {
        private int version = AbiVersionUtil.INVALID_VERSION;
        @Nullable
        private String[] annotationData = null;
        @Nullable
        private HeaderType type = null;
        @Nullable
        private FqName fqName = null;

        public ReadDataFromAnnotationVisitor() {
            super(Opcodes.ASM4);
        }

        @Override
        public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
            fqName = JvmClassName.byInternalName(name).getFqName();
        }

        @Override
        public AnnotationVisitor visitAnnotation(final String desc, boolean visible) {
            HeaderType headerTypeByAnnotation = HeaderType.byDescriptor(desc);
            if (headerTypeByAnnotation == null) {
                return null;
            }
            boolean alreadyFoundValid = type != null && type.isValidAnnotation();
            if (headerTypeByAnnotation.isValidAnnotation() && alreadyFoundValid) {
                throw new IllegalStateException("Both " + type.correspondingAnnotation + " and "
                                                 + headerTypeByAnnotation.correspondingAnnotation + " present!");
            }
            if (!alreadyFoundValid) {
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
}
