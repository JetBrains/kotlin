/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.descriptors.annotations.Annotated;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.org.objectweb.asm.Opcodes;

import java.util.Arrays;
import java.util.List;

import static org.jetbrains.kotlin.name.JvmStandardClassIds.STRICTFP_ANNOTATION_FQ_NAME;
import static org.jetbrains.kotlin.name.JvmStandardClassIds.SYNCHRONIZED_ANNOTATION_FQ_NAME;

public abstract class AnnotationCodegen {

    public static final class JvmFlagAnnotation {
        private final FqName fqName;
        private final int jvmFlag;

        public JvmFlagAnnotation(@NotNull String fqName, int jvmFlag) {
            this.fqName = new FqName(fqName);
            this.jvmFlag = jvmFlag;
        }

        public int getJvmFlag(@Nullable Annotated annotated) {
            return annotated != null && annotated.getAnnotations().hasAnnotation(fqName) ? jvmFlag : 0;
        }
    }

    public static final List<JvmFlagAnnotation> METHOD_FLAGS = Arrays.asList(
            new JvmFlagAnnotation(STRICTFP_ANNOTATION_FQ_NAME.asString(), Opcodes.ACC_STRICT),
            new JvmFlagAnnotation(SYNCHRONIZED_ANNOTATION_FQ_NAME.asString(), Opcodes.ACC_SYNCHRONIZED)
    );
}
