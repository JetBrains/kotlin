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
import org.jetbrains.kotlin.codegen.inline.SMAPBuilder;
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOrigin;
import org.jetbrains.org.objectweb.asm.*;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractClassBuilder implements ClassBuilder {
    protected static final MethodVisitor EMPTY_METHOD_VISITOR = new MethodVisitor(Opcodes.ASM5) {};
    protected static final FieldVisitor EMPTY_FIELD_VISITOR = new FieldVisitor(Opcodes.ASM5) {};

    private String thisName;

    private final JvmSerializationBindings serializationBindings = new JvmSerializationBindings();

    private final List<FileMapping> fileMappings = new ArrayList<FileMapping>();

    private String sourceName;

    private String debugInfo;

    public static class Concrete extends AbstractClassBuilder {
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

    @Override
    @NotNull
    public FieldVisitor newField(
            @NotNull JvmDeclarationOrigin origin,
            int access,
            @NotNull String name,
            @NotNull String desc,
            @Nullable String signature,
            @Nullable Object value
    ) {
        FieldVisitor visitor = getVisitor().visitField(access, name, desc, signature, value);
        if (visitor == null) {
            return EMPTY_FIELD_VISITOR;
        }
        return visitor;
    }

    @Override
    @NotNull
    public MethodVisitor newMethod(
            @NotNull JvmDeclarationOrigin origin,
            int access,
            @NotNull String name,
            @NotNull String desc,
            @Nullable String signature,
            @Nullable String[] exceptions
    ) {
        MethodVisitor visitor = getVisitor().visitMethod(access, name, desc, signature, exceptions);
        if (visitor == null) {
            return EMPTY_METHOD_VISITOR;
        }
        return visitor;
    }

    @Override
    @NotNull
    public JvmSerializationBindings getSerializationBindings() {
        return serializationBindings;
    }

    @Override
    @NotNull
    public AnnotationVisitor newAnnotation(@NotNull String desc, boolean visible) {
        return getVisitor().visitAnnotation(desc, visible);
    }

    @Override
    public void done() {
        if (!fileMappings.isEmpty()) {
            FileMapping origin = fileMappings.get(0);
            assert sourceName == null || origin.getName().equals(sourceName) : "Error " + origin.getName() +  " != "  + sourceName;
            getVisitor().visitSource(origin.getName(), new SMAPBuilder(origin.getName(), origin.getPath(), fileMappings).build());
        }
        else {
            getVisitor().visitSource(sourceName, debugInfo);
        }

        getVisitor().visitEnd();
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
        thisName = name;
        getVisitor().visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    public void visitSource(@NotNull String name, @Nullable String debug) {
        sourceName = name;
        debugInfo = debug;
    }

    @Override
    public void visitOuterClass(@NotNull String owner, @Nullable String name, @Nullable String desc) {
        getVisitor().visitOuterClass(owner, name, desc);
    }

    @Override
    public void visitInnerClass(@NotNull String name, @Nullable String outerName, @Nullable String innerName, int access) {
        getVisitor().visitInnerClass(name, outerName, innerName, access);
    }

    @Override
    @NotNull
    public String getThisName() {
        assert thisName != null : "This name isn't set";
        return thisName;
    }

    @Override
    public void addSMAP(FileMapping mapping) {
        fileMappings.add(mapping);
    }
}
