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

package org.jetbrains.jet.lang.resolve.java.header;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.asm4.AnnotationVisitor;
import org.jetbrains.asm4.ClassVisitor;
import org.jetbrains.asm4.Opcodes;
import org.jetbrains.jet.lang.resolve.java.AbiVersionUtil;
import org.jetbrains.jet.lang.resolve.java.JvmAnnotationNames;
import org.jetbrains.jet.lang.resolve.java.JvmClassName;
import org.jetbrains.jet.lang.resolve.name.FqName;

import java.util.ArrayList;
import java.util.List;

import static org.jetbrains.jet.lang.resolve.java.AbiVersionUtil.isAbiVersionCompatible;

/* package */ class ReadDataFromAnnotationVisitor extends ClassVisitor {
    private static final Logger LOG = Logger.getInstance(ReadDataFromAnnotationVisitor.class);

    @SuppressWarnings("deprecation")
    private enum HeaderType {
        CLASS(JvmAnnotationNames.KOTLIN_CLASS),
        PACKAGE(JvmAnnotationNames.KOTLIN_PACKAGE),
        OLD_CLASS(JvmAnnotationNames.OLD_JET_CLASS_ANNOTATION),
        OLD_PACKAGE(JvmAnnotationNames.OLD_JET_PACKAGE_CLASS_ANNOTATION);

        @NotNull
        private final JvmClassName annotation;

        private HeaderType(@NotNull JvmClassName annotation) {
            this.annotation = annotation;
        }

        @Nullable
        private static HeaderType byDescriptor(@NotNull String desc) {
            for (HeaderType headerType : HeaderType.values()) {
                if (desc.equals(headerType.annotation.getDescriptor())) {
                    return headerType;
                }
            }
            return null;
        }
    }

    private int version = AbiVersionUtil.INVALID_VERSION;
    @Nullable
    private String[] annotationData = null;
    @Nullable
    private HeaderType foundType = null;
    @Nullable
    private FqName fqName = null;

    public ReadDataFromAnnotationVisitor() {
        super(Opcodes.ASM4);
    }

    @Nullable
    public KotlinClassFileHeader createHeader(@NotNull VirtualFile virtualFile) {
        if (foundType == null) {
            return null;
        }

        if (fqName == null) {
            LOG.error("Class doesn't have a name in the bytecode: " + virtualFile);
            return null;
        }

        switch (foundType) {
            case CLASS:
                return SerializedDataHeader.create(version, annotationData, SerializedDataHeader.Kind.CLASS, fqName);
            case PACKAGE:
                return SerializedDataHeader.create(version, annotationData, SerializedDataHeader.Kind.PACKAGE, fqName);
            case OLD_CLASS:
            case OLD_PACKAGE:
                return new OldAnnotationHeader(fqName);
            default:
                throw new UnsupportedOperationException("Unknown HeaderType: " + foundType);
        }
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        fqName = JvmClassName.byInternalName(name).getFqName();
    }

    @Override
    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
        HeaderType newType = HeaderType.byDescriptor(desc);
        if (newType == null) return null;

        if (foundType != null) {
            LOG.error("Both annotations are present for compiled Kotlin file: " + foundType + " and " + newType);
            return null;
        }

        foundType = newType;

        if (newType == HeaderType.CLASS || newType == HeaderType.PACKAGE) {
            return kotlinClassOrPackageVisitor(desc);
        }

        return null;
    }

    @NotNull
    private AnnotationVisitor kotlinClassOrPackageVisitor(final String desc) {
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
