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
import kotlin.jvm.functions.Function4;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.builtins.jvm.JavaToKotlinClassMap;
import org.jetbrains.kotlin.descriptors.SourceElement;
import org.jetbrains.kotlin.load.kotlin.header.KotlinClassHeader;
import org.jetbrains.kotlin.load.kotlin.header.ReadKotlinClassHeaderAnnotationVisitor;
import org.jetbrains.kotlin.name.ClassId;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.resolve.constants.ClassLiteralValue;
import org.jetbrains.kotlin.resolve.jvm.JvmPrimitiveType;
import org.jetbrains.org.objectweb.asm.*;

import java.util.*;

import static org.jetbrains.org.objectweb.asm.ClassReader.*;
import static org.jetbrains.org.objectweb.asm.Opcodes.ASM5;

public abstract class FileBasedKotlinClass implements KotlinJvmBinaryClass {
    private final ClassId classId;
    private final int classVersion;
    private final KotlinClassHeader classHeader;
    private final InnerClassesInfo innerClasses;

    protected FileBasedKotlinClass(
            @NotNull ClassId classId,
            int classVersion,
            @NotNull KotlinClassHeader classHeader,
            @NotNull InnerClassesInfo innerClasses
    ) {
        this.classId = classId;
        this.classVersion = classVersion;
        this.classHeader = classHeader;
        this.innerClasses = innerClasses;
    }

    public static class OuterAndInnerName {
        public final String outerInternalName;
        public final String innerSimpleName;

        private OuterAndInnerName(@Nullable String outerInternalName, @Nullable String innerSimpleName) {
            this.outerInternalName = outerInternalName;
            this.innerSimpleName = innerSimpleName;
        }
    }

    public static class InnerClassesInfo {
        private Map<String, OuterAndInnerName> map = null;

        public void add(@NotNull String name, @Nullable String outerName, @Nullable String innerName) {
            if (map == null) {
                map = new HashMap<>();
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

    // TODO public to be accessible in companion object of subclass, workaround for KT-3974
    @Nullable
    public static <T extends FileBasedKotlinClass> T create(
            @NotNull byte[] fileContents,
            @NotNull Function4<ClassId, Integer, KotlinClassHeader, InnerClassesInfo, T> factory
    ) {
        ReadKotlinClassHeaderAnnotationVisitor readHeaderVisitor = new ReadKotlinClassHeaderAnnotationVisitor();
        Ref<String> classNameRef = Ref.create();
        Ref<Integer> classVersion = Ref.create();
        InnerClassesInfo innerClasses = new InnerClassesInfo();
        new ClassReader(fileContents).accept(new ClassVisitor(ASM5) {
            @Override
            public void visit(int version, int access, @NotNull String name, String signature, String superName, String[] interfaces) {
                classNameRef.set(name);
                classVersion.set(version);
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
        return factory.invoke(id, classVersion.get(), header, innerClasses);
    }

    @NotNull
    @Override
    public ClassId getClassId() {
        return classId;
    }

    public int getClassVersion() {
        return classVersion;
    }

    @NotNull
    @Override
    public KotlinClassHeader getClassHeader() {
        return classHeader;
    }

    @Override
    public void loadClassAnnotations(@NotNull AnnotationVisitor annotationVisitor, @Nullable byte[] cachedContents) {
        byte[] fileContents = cachedContents != null ? cachedContents : getFileContents();
        new ClassReader(fileContents).accept(new ClassVisitor(ASM5) {
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
    public static org.jetbrains.org.objectweb.asm.AnnotationVisitor convertAnnotationVisitor(
            @NotNull AnnotationVisitor visitor, @NotNull String desc, @NotNull InnerClassesInfo innerClasses
    ) {
        AnnotationArgumentVisitor v = visitor.visitAnnotation(resolveNameByDesc(desc, innerClasses), SourceElement.NO_SOURCE);
        return v == null ? null : convertAnnotationVisitor(v, innerClasses);
    }

    @NotNull
    private static org.jetbrains.org.objectweb.asm.AnnotationVisitor convertAnnotationVisitor(
            @NotNull AnnotationArgumentVisitor v, @NotNull InnerClassesInfo innerClasses
    ) {
        return new org.jetbrains.org.objectweb.asm.AnnotationVisitor(ASM5) {
            @Override
            public void visit(String name, @NotNull Object value) {
                if (value instanceof Type) {
                    v.visitClassLiteral(Name.identifier(name), resolveKotlinNameByType((Type) value, innerClasses));
                }
                else {
                    v.visit(name == null ? null : Name.identifier(name), value);
                }
            }

            @Override
            public org.jetbrains.org.objectweb.asm.AnnotationVisitor visitArray(String name) {
                AnnotationArrayArgumentVisitor arv = v.visitArray(Name.identifier(name));
                return arv == null ? null : new org.jetbrains.org.objectweb.asm.AnnotationVisitor(ASM5) {
                    @Override
                    public void visit(String name, @NotNull Object value) {
                        if (value instanceof Type) {
                            arv.visitClassLiteral(resolveKotlinNameByType((Type) value, innerClasses));
                        }
                        else {
                            arv.visit(value);
                        }
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
            public org.jetbrains.org.objectweb.asm.AnnotationVisitor visitAnnotation(String name, @NotNull String desc) {
                AnnotationArgumentVisitor arv = v.visitAnnotation(Name.identifier(name), resolveNameByDesc(desc, innerClasses));
                return arv == null ? null : convertAnnotationVisitor(arv, innerClasses);
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
    public void visitMembers(@NotNull MemberVisitor memberVisitor, @Nullable byte[] cachedContents) {
        byte[] fileContents = cachedContents != null ? cachedContents : getFileContents();
        new ClassReader(fileContents).accept(new ClassVisitor(ASM5) {
            @Override
            public FieldVisitor visitField(int access, @NotNull String name, @NotNull String desc, String signature, Object value) {
                AnnotationVisitor v = memberVisitor.visitField(Name.identifier(name), desc, value);
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
                MethodAnnotationVisitor v = memberVisitor.visitMethod(Name.identifier(name), desc);
                if (v == null) return null;

                return new MethodVisitor(ASM5) {
                    @Override
                    public org.jetbrains.org.objectweb.asm.AnnotationVisitor visitAnnotation(@NotNull String desc, boolean visible) {
                        return convertAnnotationVisitor(v, desc, innerClasses);
                    }

                    @Override
                    public org.jetbrains.org.objectweb.asm.AnnotationVisitor visitParameterAnnotation(int parameter, @NotNull String desc, boolean visible) {
                        AnnotationArgumentVisitor av = v.visitParameterAnnotation(parameter, resolveNameByDesc(desc, innerClasses), SourceElement.NO_SOURCE);
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
    private static ClassLiteralValue resolveKotlinNameByType(@NotNull Type type, @NotNull InnerClassesInfo innerClasses) {
        String typeDesc = type.getDescriptor();
        int nestedness = typeDesc.charAt(0) == '[' ? type.getDimensions() : 0;
        String elementDesc = nestedness == 0 ? typeDesc : type.getElementType().getDescriptor();
        JvmPrimitiveType primType = JvmPrimitiveType.getByDesc(elementDesc);
        if (primType != null) {
            return new ClassLiteralValue(ClassId.topLevel(primType.getPrimitiveType().getTypeFqName()), nestedness);
        }
        ClassId javaClassId = resolveNameByDesc(elementDesc, innerClasses);
        ClassId kotlinClassId = JavaToKotlinClassMap.INSTANCE.mapJavaToKotlin(javaClassId.asSingleFqName());
        return new ClassLiteralValue(kotlinClassId != null ? kotlinClassId : javaClassId, nestedness);
    }

    @NotNull
    private static ClassId resolveNameByInternalName(@NotNull String name, @NotNull InnerClassesInfo innerClasses) {
        if (!name.contains("$")) {
            return ClassId.topLevel(new FqName(name.replace('/', '.')));
        }

        List<String> classes = new ArrayList<>(1);
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
        FqName relativeClassName = FqName.fromSegments(classes);
        return new ClassId(packageFqName, relativeClassName, local);
    }

    @Override
    public abstract int hashCode();

    @Override
    public abstract boolean equals(Object obj);

    @Override
    public abstract String toString();
}
