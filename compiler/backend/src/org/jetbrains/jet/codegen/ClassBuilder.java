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

package org.jetbrains.jet.codegen;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.asm4.AnnotationVisitor;
import org.jetbrains.asm4.ClassVisitor;
import org.jetbrains.asm4.FieldVisitor;
import org.jetbrains.asm4.MethodVisitor;

public abstract class ClassBuilder {
    private String thisName;

    public static class Concrete extends ClassBuilder {
        private final ClassVisitor v;

        public Concrete(@NotNull ClassVisitor v) {
            this.v = v;
        }

        @Override
        @NotNull
        public ClassVisitor getVisitor() {
            return v;
        }
    }

    public FieldVisitor newField(
            @Nullable PsiElement origin,
            int access,
            @NotNull String name,
            @NotNull String desc,
            @Nullable String signature,
            @Nullable Object value
    ) {
        return getVisitor().visitField(access, name, desc, signature, value);
    }

    public MethodVisitor newMethod(
            @Nullable PsiElement origin,
            int access,
            @NotNull String name,
            @NotNull String desc,
            @Nullable String signature,
            @Nullable String[] exceptions
    ) {
        return getVisitor().visitMethod(access, name, desc, signature, exceptions);
    }

    @NotNull
    public AnnotationVisitor newAnnotation(@NotNull String desc, boolean visible) {
        return getVisitor().visitAnnotation(desc, visible);
    }

    public void done() {
        getVisitor().visitEnd();
    }

    @NotNull
    public abstract ClassVisitor getVisitor();

    public void defineClass(
            @Nullable PsiElement origin,
            int version,
            int access,
            @NotNull String name,
            @Nullable String signature,
            @NotNull String superName,
            @NotNull String[] interfaces
    ) {
        thisName = name;
        getVisitor().visit(version, access, name, signature, superName, interfaces);
    }

    public void visitSource(@NotNull String name, @Nullable String debug) {
        getVisitor().visitSource(name, debug);
    }

    public void visitOuterClass(@NotNull String owner, @Nullable String name, @Nullable String desc) {
        getVisitor().visitOuterClass(owner, name, desc);
    }

    public void visitInnerClass(@NotNull String name, @Nullable String outerName, @Nullable String innerName, int access) {
        getVisitor().visitInnerClass(name, outerName, innerName, access);
    }

    @NotNull
    public String getThisName() {
        assert thisName != null : "This name isn't set";
        return thisName;
    }
}
