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

package org.jetbrains.kotlin.codegen.inline;

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

public class RemappingClassBuilder extends DelegatingClassBuilder {
    private final ClassBuilder builder;
    private final Remapper remapper;

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
    @NotNull
    public FieldVisitor newField(
            @NotNull JvmDeclarationOrigin origin,
            int access,
            @NotNull String name,
            @NotNull String desc,
            @Nullable String signature,
            @Nullable Object value
    ) {
        return new RemappingFieldAdapter(builder.newField(origin, access, name, remapper.mapDesc(desc), signature, value), remapper);
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
        return new RemappingMethodAdapter(access, desc,
                                          builder.newMethod(origin, access, name, remapper.mapMethodDesc(desc), signature, exceptions),
                                          remapper);
    }

    @Override
    @NotNull
    public AnnotationVisitor newAnnotation(@NotNull String desc, boolean visible) {
        return new RemappingAnnotationAdapter(builder.newAnnotation(remapper.mapDesc(desc), visible), remapper);
    }

    @Override
    @NotNull
    public ClassVisitor getVisitor() {
        return new RemappingClassAdapter(builder.getVisitor(), remapper);
    }

}
