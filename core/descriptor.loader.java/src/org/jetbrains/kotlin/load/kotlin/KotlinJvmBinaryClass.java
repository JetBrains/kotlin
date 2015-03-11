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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.load.kotlin.header.KotlinClassHeader;
import org.jetbrains.kotlin.name.ClassId;
import org.jetbrains.kotlin.name.Name;

public interface KotlinJvmBinaryClass {
    @NotNull
    ClassId getClassId();

    /**
     * @return path to the class file (to be reported to the user upon error)
     */
    @NotNull
    String getLocation();

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
        AnnotationArgumentVisitor visitAnnotation(@NotNull ClassId classId);

        void visitEnd();
    }

    interface MethodAnnotationVisitor extends AnnotationVisitor {
        @Nullable
        AnnotationArgumentVisitor visitParameterAnnotation(int index, @NotNull ClassId classId);
    }

    interface AnnotationArgumentVisitor {
        // TODO: annotations, java.lang.Class
        void visit(@Nullable Name name, @Nullable Object value);

        void visitEnum(@NotNull Name name, @NotNull ClassId enumClassId, @NotNull Name enumEntryName);

        @Nullable
        AnnotationArrayArgumentVisitor visitArray(@NotNull Name name);

        void visitEnd();
    }

    interface AnnotationArrayArgumentVisitor {
        void visit(@Nullable Object value);

        void visitEnum(@NotNull ClassId enumClassId, @NotNull Name enumEntryName);

        void visitEnd();
    }
}
