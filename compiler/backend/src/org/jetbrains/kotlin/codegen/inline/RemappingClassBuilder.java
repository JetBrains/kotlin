/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen.inline;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.codegen.ClassBuilder;
import org.jetbrains.kotlin.codegen.DelegatingClassBuilder;
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOrigin;
import org.jetbrains.org.objectweb.asm.AnnotationVisitor;
import org.jetbrains.org.objectweb.asm.ClassVisitor;
import org.jetbrains.org.objectweb.asm.FieldVisitor;
import org.jetbrains.org.objectweb.asm.MethodVisitor;
import org.jetbrains.org.objectweb.asm.commons.*;
import org.jetbrains.org.objectweb.asm.commons.FieldRemapper;

import java.util.HashMap;
import java.util.Map;

public class RemappingClassBuilder extends DelegatingClassBuilder {
    private final ClassBuilder builder;
    private final Remapper remapper;
    private final Map<String, FieldVisitor> spilledCoroutineVariables = new HashMap<>();

    public RemappingClassBuilder(@NotNull ClassBuilder builder, @NotNull Remapper remapper) {
        this.builder = builder;
        this.remapper = remapper;
    }

    @Override
    @NotNull
    protected ClassBuilder getDelegate() {
        return builder;
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
        super.defineClass(origin, version, access, remapper.mapType(name), remapper.mapSignature(signature, false),
                          remapper.mapType(superName), remapper.mapTypes(interfaces));
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
        if (spilledCoroutineVariables.containsKey(name)) return spilledCoroutineVariables.get(name);

        FieldRemapper field = new FieldRemapper(
                builder.newField(origin, access, name, this.remapper.mapDesc(desc), this.remapper.mapSignature(signature, true), value),
                this.remapper
        );
        if (isSpilledCoroutineVariableName(name)) {
            spilledCoroutineVariables.put(name, field);
        }
        return field;
    }

    private static boolean isSpilledCoroutineVariableName(String name) {
        if (name.length() < 3) return false;
        switch (name.charAt(0)) {
            case 'L':
            case 'Z':
            case 'C':
            case 'B':
            case 'S':
            case 'I':
            case 'F':
            case 'J':
            case 'D':
                break;
            default:
                return false;
        }
        if (name.charAt(1) != '$') return false;
        for (int i = 2; i < name.length(); ++i) {
            if (!Character.isDigit(name.charAt(i))) return false;
        }
        return true;
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
        String newDescriptor = remapper.mapMethodDesc(desc);
        // MethodRemapper doesn't extends LocalVariablesSorter, but RemappingMethodAdapter does.
        // So wrapping with LocalVariablesSorter to keep old behavior.
        // TODO: investigate LocalVariablesSorter removing (see also same code in MethodInliner)
        return new MethodRemapper(
                new LocalVariablesSorter(
                        access, newDescriptor,
                        builder.newMethod(origin, access, name, newDescriptor, remapper.mapSignature(signature, false),
                                          exceptions)),
                remapper
        );
    }

    @Override
    @NotNull
    public AnnotationVisitor newAnnotation(@NotNull String desc, boolean visible) {
        return new AnnotationRemapper(builder.newAnnotation(remapper.mapDesc(desc), visible), remapper);
    }

    @Override
    @NotNull
    public ClassVisitor getVisitor() {
        return new ClassRemapper(builder.getVisitor(), remapper);
    }
}
