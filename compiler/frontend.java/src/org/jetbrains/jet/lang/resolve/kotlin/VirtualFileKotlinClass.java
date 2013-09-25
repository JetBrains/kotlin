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

package org.jetbrains.jet.lang.resolve.kotlin;

import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.asm4.ClassReader;
import org.jetbrains.asm4.ClassVisitor;
import org.jetbrains.asm4.FieldVisitor;
import org.jetbrains.asm4.MethodVisitor;
import org.jetbrains.jet.lang.resolve.java.JvmClassName;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.utils.ExceptionUtils;

import java.io.IOException;

import static org.jetbrains.asm4.ClassReader.*;
import static org.jetbrains.asm4.Opcodes.ASM4;

public class VirtualFileKotlinClass implements KotlinJvmBinaryClass {
    private final VirtualFile file;

    public VirtualFileKotlinClass(@NotNull VirtualFile file) {
        this.file = file;
    }

    @NotNull
    @Override
    public VirtualFile getFile() {
        return file;
    }

    @Override
    public void loadClassAnnotations(@NotNull final AnnotationVisitor annotationVisitor) {
        try {
            new ClassReader(file.contentsToByteArray()).accept(new ClassVisitor(ASM4) {
                @Override
                public org.jetbrains.asm4.AnnotationVisitor visitAnnotation(String desc, boolean visible) {
                    return convertAnnotationVisitor(annotationVisitor, desc);
                }

                @Override
                public void visitEnd() {
                    annotationVisitor.visitEnd();
                }
            }, SKIP_CODE | SKIP_DEBUG | SKIP_FRAMES);
        }
        catch (IOException e) {
            throw ExceptionUtils.rethrow(e);
        }
    }

    @Nullable
    private static org.jetbrains.asm4.AnnotationVisitor convertAnnotationVisitor(
            @NotNull AnnotationVisitor visitor,
            @NotNull String desc
    ) {
        final AnnotationArgumentVisitor v = visitor.visitAnnotation(classNameFromAsmDesc(desc));
        if (v == null) return null;

        return new org.jetbrains.asm4.AnnotationVisitor(ASM4) {
            @Override
            public void visit(String name, Object value) {
                v.visit(Name.identifier(name), value);
            }

            @Override
            public void visitEnum(String name, String desc, String value) {
                v.visitEnum(Name.identifier(name), classNameFromAsmDesc(desc), Name.identifier(value));
            }

            @Override
            public void visitEnd() {
                v.visitEnd();
            }
        };
    }

    @Override
    public void loadMemberAnnotations(@NotNull final MemberVisitor memberVisitor) {
        try {
            new ClassReader(file.contentsToByteArray()).accept(new ClassVisitor(ASM4) {
                @Override
                public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
                    final AnnotationVisitor v = memberVisitor.visitField(Name.guess(name), desc);
                    if (v == null) return null;

                    return new FieldVisitor(ASM4) {
                        @Override
                        public org.jetbrains.asm4.AnnotationVisitor visitAnnotation(String desc, boolean visible) {
                            return convertAnnotationVisitor(v, desc);
                        }

                        @Override
                        public void visitEnd() {
                            v.visitEnd();
                        }
                    };
                }

                @Override
                public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
                    final AnnotationVisitor v = memberVisitor.visitMethod(Name.guess(name), desc);
                    if (v == null) return null;

                    return new MethodVisitor(ASM4) {
                        @Override
                        public org.jetbrains.asm4.AnnotationVisitor visitAnnotation(String desc, boolean visible) {
                            return convertAnnotationVisitor(v, desc);
                        }

                        @Override
                        public void visitEnd() {
                            super.visitEnd();
                        }
                    };
                }
            }, SKIP_CODE | SKIP_DEBUG | SKIP_FRAMES);
        }
        catch (IOException e) {
            throw ExceptionUtils.rethrow(e);
        }
    }

    @NotNull
    private static JvmClassName classNameFromAsmDesc(@NotNull String desc) {
        assert desc.startsWith("L") && desc.endsWith(";") : "Not a JVM descriptor: " + desc;
        return JvmClassName.byInternalName(desc.substring(1, desc.length() - 1));
    }

    @Override
    public int hashCode() {
        return file.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof VirtualFileKotlinClass && ((VirtualFileKotlinClass) obj).file.equals(file);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + ": " + file.toString();
    }
}
