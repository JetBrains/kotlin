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

import com.intellij.openapi.util.Ref;
import kotlin.Function2;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.resolve.java.JvmClassName;
import org.jetbrains.jet.lang.resolve.kotlin.header.KotlinClassHeader;
import org.jetbrains.jet.lang.resolve.kotlin.header.ReadKotlinClassHeaderAnnotationVisitor;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.org.objectweb.asm.ClassReader;
import org.jetbrains.org.objectweb.asm.ClassVisitor;
import org.jetbrains.org.objectweb.asm.FieldVisitor;
import org.jetbrains.org.objectweb.asm.MethodVisitor;

import static org.jetbrains.org.objectweb.asm.ClassReader.*;
import static org.jetbrains.org.objectweb.asm.Opcodes.ASM5;

public abstract class FileBasedKotlinClass implements KotlinJvmBinaryClass {
    private final JvmClassName className;
    private final KotlinClassHeader classHeader;

    protected FileBasedKotlinClass(@NotNull JvmClassName className, @NotNull KotlinClassHeader classHeader) {
        this.className = className;
        this.classHeader = classHeader;
    }

    @NotNull
    protected abstract byte[] getFileContents();

    @Nullable
    protected static <T extends FileBasedKotlinClass> T create(
            @NotNull byte[] fileContents,
            @NotNull Function2<JvmClassName, KotlinClassHeader, T> factory
    ) {
        final ReadKotlinClassHeaderAnnotationVisitor readHeaderVisitor = new ReadKotlinClassHeaderAnnotationVisitor();
        final Ref<JvmClassName> classNameRef = Ref.create();
        new ClassReader(fileContents).accept(new ClassVisitor(ASM5) {
            @Override
            public void visit(int version, int access, @NotNull String name, String signature, String superName, String[] interfaces) {
                classNameRef.set(JvmClassName.byInternalName(name));
            }

            @Override
            public org.jetbrains.org.objectweb.asm.AnnotationVisitor visitAnnotation(@NotNull String desc, boolean visible) {
                return convertAnnotationVisitor(readHeaderVisitor, desc);
            }

            @Override
            public void visitEnd() {
                readHeaderVisitor.visitEnd();
            }
        }, SKIP_CODE | SKIP_DEBUG | SKIP_FRAMES);

        JvmClassName className = classNameRef.get();
        if (className == null) return null;

        KotlinClassHeader header = readHeaderVisitor.createHeader();
        if (header == null) return null;

        return factory.invoke(className, header);
    }

    @NotNull
    @Override
    public JvmClassName getClassName() {
        return className;
    }

    @NotNull
    @Override
    public KotlinClassHeader getClassHeader() {
        return classHeader;
    }

    @Override
    public void loadClassAnnotations(@NotNull final AnnotationVisitor annotationVisitor) {
        new ClassReader(getFileContents()).accept(new ClassVisitor(ASM5) {
            @Override
            public org.jetbrains.org.objectweb.asm.AnnotationVisitor visitAnnotation(@NotNull String desc, boolean visible) {
                return convertAnnotationVisitor(annotationVisitor, desc);
            }

            @Override
            public void visitEnd() {
                annotationVisitor.visitEnd();
            }
        }, SKIP_CODE | SKIP_DEBUG | SKIP_FRAMES);
    }

    @Nullable
    private static org.jetbrains.org.objectweb.asm.AnnotationVisitor convertAnnotationVisitor(@NotNull AnnotationVisitor visitor, @NotNull String desc) {
        AnnotationArgumentVisitor v = visitor.visitAnnotation(classNameFromAsmDesc(desc));
        return v == null ? null : convertAnnotationVisitor(v);
    }

    @NotNull
    private static org.jetbrains.org.objectweb.asm.AnnotationVisitor convertAnnotationVisitor(@NotNull final AnnotationArgumentVisitor v) {
        return new org.jetbrains.org.objectweb.asm.AnnotationVisitor(ASM5) {
            @Override
            public void visit(String name, @NotNull Object value) {
                v.visit(name == null ? null : Name.identifier(name), value);
            }

            @Override
            public org.jetbrains.org.objectweb.asm.AnnotationVisitor visitArray(String name) {
                final AnnotationArrayArgumentVisitor arv = v.visitArray(Name.guess(name));
                return arv == null ? null : new org.jetbrains.org.objectweb.asm.AnnotationVisitor(ASM5) {
                    @Override
                    public void visit(String name, @NotNull Object value) {
                        arv.visit(value);
                    }

                    @Override
                    public void visitEnum(String name, @NotNull String desc, @NotNull String value) {
                        arv.visitEnum(classNameFromAsmDesc(desc), Name.identifier(value));
                    }

                    @Override
                    public void visitEnd() {
                        arv.visitEnd();
                    }
                };
            }

            @Override
            public void visitEnum(String name, @NotNull String desc, @NotNull String value) {
                v.visitEnum(Name.identifier(name), classNameFromAsmDesc(desc), Name.identifier(value));
            }

            @Override
            public void visitEnd() {
                v.visitEnd();
            }
        };
    }

    @Override
    public void visitMembers(@NotNull final MemberVisitor memberVisitor) {
        new ClassReader(getFileContents()).accept(new ClassVisitor(ASM5) {
            @Override
            public FieldVisitor visitField(int access, @NotNull String name, @NotNull String desc, String signature, Object value) {
                final AnnotationVisitor v = memberVisitor.visitField(Name.guess(name), desc, value);
                if (v == null) return null;

                return new FieldVisitor(ASM5) {
                    @Override
                    public org.jetbrains.org.objectweb.asm.AnnotationVisitor visitAnnotation(@NotNull String desc, boolean visible) {
                        return convertAnnotationVisitor(v, desc);
                    }

                    @Override
                    public void visitEnd() {
                        v.visitEnd();
                    }
                };
            }

            @Override
            public MethodVisitor visitMethod(int access, @NotNull String name, @NotNull String desc, String signature, String[] exceptions) {
                final MethodAnnotationVisitor v = memberVisitor.visitMethod(Name.guess(name), desc);
                if (v == null) return null;

                return new MethodVisitor(ASM5) {
                    @Override
                    public org.jetbrains.org.objectweb.asm.AnnotationVisitor visitAnnotation(@NotNull String desc, boolean visible) {
                        return convertAnnotationVisitor(v, desc);
                    }

                    @Override
                    public org.jetbrains.org.objectweb.asm.AnnotationVisitor visitParameterAnnotation(int parameter, @NotNull String desc, boolean visible) {
                        AnnotationArgumentVisitor av = v.visitParameterAnnotation(parameter, classNameFromAsmDesc(desc));
                        return av == null ? null : convertAnnotationVisitor(av);
                    }

                    @Override
                    public void visitEnd() {
                        v.visitEnd();
                    }
                };
            }
        }, SKIP_CODE | SKIP_DEBUG | SKIP_FRAMES);
    }

    @NotNull
    private static JvmClassName classNameFromAsmDesc(@NotNull String desc) {
        assert desc.startsWith("L") && desc.endsWith(";") : "Not a JVM descriptor: " + desc;
        return JvmClassName.byInternalName(desc.substring(1, desc.length() - 1));
    }

    @Override
    public abstract int hashCode();

    @Override
    public abstract boolean equals(Object obj);

    @Override
    public abstract String toString();
}
