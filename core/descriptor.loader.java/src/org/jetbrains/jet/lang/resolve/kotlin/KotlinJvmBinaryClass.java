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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.resolve.java.JvmClassName;
import org.jetbrains.jet.lang.resolve.kotlin.header.KotlinClassHeader;
import org.jetbrains.jet.lang.resolve.name.Name;

public interface KotlinJvmBinaryClass {
    @NotNull
    JvmClassName getClassName();

    void loadClassAnnotations(@NotNull AnnotationVisitor visitor);

    void visitMembers(@NotNull MemberVisitor visitor);

    @NotNull
    KotlinClassHeader getClassHeader();

    interface MemberVisitor {
        // TODO: abstract signatures for methods and fields instead of ASM 'desc' strings?

        @Nullable
        MethodAnnotationVisitor visitMethod(@NotNull Name name, @NotNull String desc);

        @Nullable
        AnnotationVisitor visitField(@NotNull Name name, @NotNull String desc, @Nullable Object initializer);
    }

    interface AnnotationVisitor {
        @Nullable
        AnnotationArgumentVisitor visitAnnotation(@NotNull JvmClassName className);

        void visitEnd();
    }

    interface MethodAnnotationVisitor extends AnnotationVisitor {
        @Nullable
        AnnotationArgumentVisitor visitParameterAnnotation(int index, @NotNull JvmClassName className);
    }

    interface AnnotationArgumentVisitor {
        // TODO: annotations, java.lang.Class
        void visit(@Nullable Name name, @Nullable Object value);

        void visitEnum(@NotNull Name name, @NotNull JvmClassName enumClassName, @NotNull Name enumEntryName);

        @Nullable
        AnnotationArrayArgumentVisitor visitArray(@NotNull Name name);

        void visitEnd();
    }

    interface AnnotationArrayArgumentVisitor {
        void visit(@Nullable Object value);

        void visitEnum(@NotNull JvmClassName enumClassName, @NotNull Name enumEntryName);

        void visitEnd();
    }
}
