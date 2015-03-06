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

package org.jetbrains.kotlin.load.kotlin;

import com.intellij.openapi.util.Ref;
import kotlin.Function3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.load.java.JvmAnnotationNames;
import org.jetbrains.kotlin.load.kotlin.header.KotlinClassHeader;
import org.jetbrains.kotlin.load.kotlin.header.ReadKotlinClassHeaderAnnotationVisitor;
import org.jetbrains.kotlin.name.ClassId;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.name.FqNameUnsafe;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.org.objectweb.asm.ClassReader;
import org.jetbrains.org.objectweb.asm.ClassVisitor;
import org.jetbrains.org.objectweb.asm.FieldVisitor;
import org.jetbrains.org.objectweb.asm.MethodVisitor;

import java.util.*;

import static org.jetbrains.org.objectweb.asm.ClassReader.*;
import static org.jetbrains.org.objectweb.asm.Opcodes.ASM5;

public abstract class FileBasedKotlinClass implements KotlinJvmBinaryClass {
    private final ClassId classId;
    private final KotlinClassHeader classHeader;
    private final InnerClassesInfo innerClasses;

    protected FileBasedKotlinClass(
            @NotNull ClassId classId,
            @NotNull KotlinClassHeader classHeader,
            @NotNull InnerClassesInfo innerClasses
    ) {
        this.classId = classId;
        this.classHeader = classHeader;
        this.innerClasses = innerClasses;
    }

    private static class OuterAndInnerName {
        public final String outerInternalName;
        public final String innerSimpleName;

        private OuterAndInnerName(@Nullable String outerInternalName, @Nullable String innerSimpleName) {
            this.outerInternalName = outerInternalName;
            this.innerSimpleName = innerSimpleName;
        }
    }

    protected static class InnerClassesInfo {
        private Map<String, OuterAndInnerName> map = null;

        public void add(@NotNull String name, @Nullable String outerName, @Nullable String innerName) {
            if (map == null) {
                map = new HashMap<String, OuterAndInnerName>();
            }
            map.put(name, new OuterAndInnerName(outerName, innerName));
        }

        @Nullable
        public OuterAndInnerName get(@NotNull String name) {
            return map == null ? null : map.get(name);
        }
    }

    @NotNull
    protected abstract byte[] getFileContents();

    // TODO public to be accessible in default object of subclass, workaround for KT-3974
    @Nullable
    public static <T extends FileBasedKotlinClass> T create(
            @NotNull byte[] fileContents,
            @NotNull Function3<ClassId, KotlinClassHeader, InnerClassesInfo, T> factory
    ) {
        final ReadKotlinClassHeaderAnnotationVisitor readHeaderVisitor = new ReadKotlinClassHeaderAnnotationVisitor();
        final Ref<String> classNameRef = Ref.create();
        final InnerClassesInfo innerClasses = new InnerClassesInfo();
        new ClassReader(fileContents).accept(new ClassVisitor(ASM5) {
            @Override
            public void visit(int version, int access, @NotNull String name, String signature, String superName, String[] interfaces) {
                classNameRef.set(name);
            }

            @Override
            public void visitInnerClass(@NotNull String name, String outerName, String innerName, int access) {
                innerClasses.add(name, outerName, innerName);
            }

            @Override
            public org.jetbrains.org.objectweb.asm.AnnotationVisitor visitAnnotation(@NotNull String desc, boolean visible) {
                return convertAnnotationVisitor(readHeaderVisitor, desc, innerClasses);
            }

            @Override
            public void visitEnd() {
                readHeaderVisitor.visitEnd();
            }
        }, SKIP_CODE | SKIP_DEBUG | SKIP_FRAMES);

        String className = classNameRef.get();
        if (className == null) return null;

        KotlinClassHeader header = readHeaderVisitor.createHeader();
        if (header == null) return null;

        ClassId id = resolveNameByInternalName(className, innerClasses);
        return factory.invoke(id, header, innerClasses);
    }

    @NotNull
    @Override
    public ClassId getClassId() {
        return classId;
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
                return convertAnnotationVisitor(annotationVisitor, desc, innerClasses);
            }

            @Override
            public void visitEnd() {
                annotationVisitor.visitEnd();
            }
        }, SKIP_CODE | SKIP_DEBUG | SKIP_FRAMES);
    }

    @Nullable
    private static org.jetbrains.org.objectweb.asm.AnnotationVisitor convertAnnotationVisitor(
            @NotNull AnnotationVisitor visitor, @NotNull String desc, @NotNull InnerClassesInfo innerClasses
    ) {
        AnnotationArgumentVisitor v = visitor.visitAnnotation(resolveNameByDesc(desc, innerClasses));
        return v == null ? null : convertAnnotationVisitor(v, innerClasses);
    }

    @NotNull
    private static org.jetbrains.org.objectweb.asm.AnnotationVisitor convertAnnotationVisitor(
            @NotNull final AnnotationArgumentVisitor v, @NotNull final InnerClassesInfo innerClasses
    ) {
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
                        arv.visitEnum(resolveNameByDesc(desc, innerClasses), Name.identifier(value));
                    }

                    @Override
                    public void visitEnd() {
                        arv.visitEnd();
                    }
                };
            }

            @Override
            public void visitEnum(String name, @NotNull String desc, @NotNull String value) {
                v.visitEnum(Name.identifier(name), resolveNameByDesc(desc, innerClasses), Name.identifier(value));
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
                        return convertAnnotationVisitor(v, desc, innerClasses);
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
                        return convertAnnotationVisitor(v, desc, innerClasses);
                    }

                    @Override
                    public org.jetbrains.org.objectweb.asm.AnnotationVisitor visitParameterAnnotation(int parameter, @NotNull String desc, boolean visible) {
                        AnnotationArgumentVisitor av = v.visitParameterAnnotation(parameter, resolveNameByDesc(desc, innerClasses));
                        return av == null ? null : convertAnnotationVisitor(av, innerClasses);
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
    private static ClassId resolveNameByDesc(@NotNull String desc, @NotNull InnerClassesInfo innerClasses) {
        assert desc.startsWith("L") && desc.endsWith(";") : "Not a JVM descriptor: " + desc;
        String name = desc.substring(1, desc.length() - 1);
        return resolveNameByInternalName(name, innerClasses);
    }

    @NotNull
    private static ClassId resolveNameByInternalName(@NotNull String name, @NotNull InnerClassesInfo innerClasses) {
        if (!name.contains("$")) {
            return ClassId.topLevel(new FqName(name.replace('/', '.')));
        }

        // TODO: this is a hack which can be dropped once JVM back-end begins to write InnerClasses attribute for all referenced classes
        if (name.equals(JvmAnnotationNames.KotlinSyntheticClass.KIND_INTERNAL_NAME)) {
            return JvmAnnotationNames.KotlinSyntheticClass.KIND_CLASS_ID;
        }
        else if (name.equals(JvmAnnotationNames.KotlinClass.KIND_INTERNAL_NAME)) {
            return JvmAnnotationNames.KotlinClass.KIND_CLASS_ID;
        }

        List<String> classes = new ArrayList<String>(1);
        boolean local = false;
        
        while (true) {
            OuterAndInnerName outer = innerClasses.get(name);
            if (outer == null) break;
            if (outer.outerInternalName == null) {
                local = true;
                break;
            }
            classes.add(outer.innerSimpleName);
            name = outer.outerInternalName;
        }

        FqName outermostClassFqName = new FqName(name.replace('/', '.'));
        classes.add(outermostClassFqName.shortName().asString());

        Collections.reverse(classes);

        FqName packageFqName = outermostClassFqName.parent();
        FqNameUnsafe relativeClassName = FqNameUnsafe.fromSegments(classes);
        return new ClassId(packageFqName, relativeClassName, local);
    }

    @Override
    public abstract int hashCode();

    @Override
    public abstract boolean equals(Object obj);

    @Override
    public abstract String toString();
}
