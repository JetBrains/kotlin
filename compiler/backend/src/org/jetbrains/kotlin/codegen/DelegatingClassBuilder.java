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

package org.jetbrains.kotlin.codegen;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.codegen.inline.FileMapping;
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOrigin;
import org.jetbrains.org.objectweb.asm.AnnotationVisitor;
import org.jetbrains.org.objectweb.asm.ClassVisitor;
import org.jetbrains.org.objectweb.asm.FieldVisitor;
import org.jetbrains.org.objectweb.asm.MethodVisitor;

public abstract class DelegatingClassBuilder implements ClassBuilder {
    @NotNull
    protected abstract ClassBuilder getDelegate();

    @NotNull
    @Override
    public FieldVisitor newField(
            @NotNull JvmDeclarationOrigin origin,
            int access,
            @NotNull String name,
            @NotNull String desc,
            @Nullable String signature,
            @Nullable Object value
    ) {
        return getDelegate().newField(origin, access, name, desc, signature, value);
    }

    @NotNull
    @Override
    public MethodVisitor newMethod(
            @NotNull JvmDeclarationOrigin origin,
            int access,
            @NotNull String name,
            @NotNull String desc,
            @Nullable String signature,
            @Nullable String[] exceptions
    ) {
        return getDelegate().newMethod(origin, access, name, desc, signature, exceptions);
    }

    @NotNull
    @Override
    public JvmSerializationBindings getSerializationBindings() {
        return getDelegate().getSerializationBindings();
    }

    @NotNull
    @Override
    public AnnotationVisitor newAnnotation(@NotNull String desc, boolean visible) {
        return getDelegate().newAnnotation(desc, visible);
    }

    @Override
    public void done() {
        getDelegate().done();
    }

    @NotNull
    @Override
    public ClassVisitor getVisitor() {
        return getDelegate().getVisitor();
    }

    @Override
    public void defineClass(
            @Nullable PsiElement origin,
            int version,
            int access,
            @NotNull String name,
            @Nullable String signature,
            @NotNull String superName,
            @NotNull String[] interfaces
    ) {
        getDelegate().defineClass(origin, version, access, name, signature, superName, interfaces);
    }

    @Override
    public void visitSource(@NotNull String name, @Nullable String debug) {
        getDelegate().visitSource(name, debug);
    }

    @Override
    public void visitOuterClass(@NotNull String owner, @Nullable String name, @Nullable String desc) {
        getDelegate().visitOuterClass(owner, name, desc);
    }

    @Override
    public void visitInnerClass(@NotNull String name, @Nullable String outerName, @Nullable String innerName, int access) {
        getDelegate().visitInnerClass(name, outerName, innerName, access);
    }

    @NotNull
    @Override
    public String getThisName() {
        return getDelegate().getThisName();
    }

    @Override
    public void addSMAP(FileMapping mapping) {
        getDelegate().addSMAP(mapping);
    }
}
