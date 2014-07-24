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

import com.intellij.ide.highlighter.JavaClassFileType;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.resolve.java.JvmClassName;
import org.jetbrains.jet.lang.resolve.kotlin.header.KotlinClassHeader;
import org.jetbrains.jet.lang.resolve.kotlin.header.ReadKotlinClassHeaderAnnotationVisitor;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.utils.UtilsPackage;
import org.jetbrains.org.objectweb.asm.ClassReader;
import org.jetbrains.org.objectweb.asm.ClassVisitor;
import org.jetbrains.org.objectweb.asm.FieldVisitor;
import org.jetbrains.org.objectweb.asm.MethodVisitor;

import static org.jetbrains.org.objectweb.asm.ClassReader.*;
import static org.jetbrains.org.objectweb.asm.Opcodes.ASM5;

public class VirtualFileKotlinClass implements KotlinJvmBinaryClass {
    private final static Logger LOG = Logger.getInstance(VirtualFileKotlinClass.class);

    private final VirtualFile file;
    private final JvmClassName className;
    private final KotlinClassHeader classHeader;

    private VirtualFileKotlinClass(@NotNull VirtualFile file, @NotNull JvmClassName className, @NotNull KotlinClassHeader classHeader) {
        this.file = file;
        this.className = className;
        this.classHeader = classHeader;
    }

    @Nullable
    public static Pair<JvmClassName, KotlinClassHeader> readClassNameAndHeader(@NotNull byte[] fileContents) {
        final ReadKotlinClassHeaderAnnotationVisitor readHeaderVisitor = new ReadKotlinClassHeaderAnnotationVisitor();
        final Ref<JvmClassName> classNameRef = Ref.create();
        new ClassReader(fileContents).accept(new ClassVisitor(ASM5) {
            @Override
            public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
                classNameRef.set(JvmClassName.byInternalName(name));
            }

            @Override
            public org.jetbrains.org.objectweb.asm.AnnotationVisitor visitAnnotation(String desc, boolean visible) {
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

        return Pair.create(className, header);
    }

    @Nullable
    /* package */ static VirtualFileKotlinClass create(@NotNull VirtualFile file) {
        assert file.getFileType() == JavaClassFileType.INSTANCE : "Trying to read binary data from a non-class file " + file;
        try {
            byte[] fileContents = file.contentsToByteArray();
            Pair<JvmClassName, KotlinClassHeader> nameAndHeader = readClassNameAndHeader(fileContents);
            if (nameAndHeader == null) {
                return null;
            }

            return new VirtualFileKotlinClass(file, nameAndHeader.first, nameAndHeader.second);
        }
        catch (Throwable e) {
            LOG.warn(renderFileReadingErrorMessage(file));
            return null;
        }
    }

    @Nullable
    public static KotlinClassHeader readClassHeader(@NotNull byte[] fileContents) {
        Pair<JvmClassName, KotlinClassHeader> pair = readClassNameAndHeader(fileContents);
        return pair == null ? null : pair.second;
    }

    @NotNull
    public VirtualFile getFile() {
        return file;
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
        try {
            new ClassReader(file.contentsToByteArray()).accept(new ClassVisitor(ASM5) {
                @Override
                public org.jetbrains.org.objectweb.asm.AnnotationVisitor visitAnnotation(String desc, boolean visible) {
                    return convertAnnotationVisitor(annotationVisitor, desc);
                }

                @Override
                public void visitEnd() {
                    annotationVisitor.visitEnd();
                }
            }, SKIP_CODE | SKIP_DEBUG | SKIP_FRAMES);
        }
        catch (Throwable e) {
            LOG.error(renderFileReadingErrorMessage(file), e);
            throw UtilsPackage.rethrow(e);
        }
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
            public void visit(String name, Object value) {
                v.visit(name == null ? null : Name.identifier(name), value);
            }

            @Override
            public org.jetbrains.org.objectweb.asm.AnnotationVisitor visitArray(String name) {
                final AnnotationArrayArgumentVisitor arv = v.visitArray(Name.guess(name));
                return arv == null ? null : new org.jetbrains.org.objectweb.asm.AnnotationVisitor(ASM5) {
                    @Override
                    public void visit(String name, Object value) {
                        arv.visit(value);
                    }

                    @Override
                    public void visitEnum(String name, String desc, String value) {
                        arv.visitEnum(classNameFromAsmDesc(desc), Name.identifier(value));
                    }

                    @Override
                    public void visitEnd() {
                        arv.visitEnd();
                    }
                };
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
    public void visitMembers(@NotNull final MemberVisitor memberVisitor) {
        try {
            new ClassReader(file.contentsToByteArray()).accept(new ClassVisitor(ASM5) {
                @Override
                public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
                    final AnnotationVisitor v = memberVisitor.visitField(Name.guess(name), desc, value);
                    if (v == null) return null;

                    return new FieldVisitor(ASM5) {
                        @Override
                        public org.jetbrains.org.objectweb.asm.AnnotationVisitor visitAnnotation(String desc, boolean visible) {
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
                    final MethodAnnotationVisitor v = memberVisitor.visitMethod(Name.guess(name), desc);
                    if (v == null) return null;

                    return new MethodVisitor(ASM5) {
                        @Override
                        public org.jetbrains.org.objectweb.asm.AnnotationVisitor visitAnnotation(String desc, boolean visible) {
                            return convertAnnotationVisitor(v, desc);
                        }

                        @Override
                        public org.jetbrains.org.objectweb.asm.AnnotationVisitor visitParameterAnnotation(int parameter, String desc, boolean visible) {
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
        catch (Throwable e) {
            LOG.error(renderFileReadingErrorMessage(file), e);
            throw UtilsPackage.rethrow(e);
        }
    }

    @NotNull
    private static JvmClassName classNameFromAsmDesc(@NotNull String desc) {
        assert desc.startsWith("L") && desc.endsWith(";") : "Not a JVM descriptor: " + desc;
        return JvmClassName.byInternalName(desc.substring(1, desc.length() - 1));
    }

    @NotNull
    private static String renderFileReadingErrorMessage(@NotNull VirtualFile file) {
        return "Could not read file: " + file.getPath() + "; "
               + "size in bytes: " + file.getLength() + "; "
               + "file type: " + file.getFileType().getName();
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
