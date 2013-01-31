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

package org.jetbrains.jet.codegen.signature.kotlin;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.asm4.AnnotationVisitor;
import org.jetbrains.asm4.MethodVisitor;
import org.jetbrains.jet.lang.resolve.java.JvmStdlibNames;

public class JetValueParameterAnnotationWriter {
    private final AnnotationVisitor av;

    private JetValueParameterAnnotationWriter(AnnotationVisitor av) {
        this.av = av;
    }

    public void writeName(@NotNull String name) {
        if (name.length() > 0) {
            av.visit(JvmStdlibNames.JET_VALUE_PARAMETER_NAME_FIELD, name);
        }
    }

    public static JetValueParameterAnnotationWriter visitParameterAnnotation(MethodVisitor mv, int n) {
        return new JetValueParameterAnnotationWriter(
                mv.visitParameterAnnotation(n, JvmStdlibNames.JET_VALUE_PARAMETER.getDescriptor(), true));
    }

    public void writeReceiver() {
        av.visit(JvmStdlibNames.JET_VALUE_PARAMETER_RECEIVER_FIELD, true);
    }

    public void writeType(@NotNull String kotlinSignature) {
        if (kotlinSignature.length() > 0) {
            av.visit(JvmStdlibNames.JET_VALUE_PARAMETER_TYPE_FIELD, kotlinSignature);
        }
    }

    public void visitEnd() {
        av.visitEnd();
    }

    public void writeHasDefaultValue(boolean hasDefaultValue) {
        if (hasDefaultValue) {
            av.visit(JvmStdlibNames.JET_VALUE_PARAMETER_HAS_DEFAULT_VALUE_FIELD, true);
        }
    }

    public void writeVararg(boolean vararg) {
        if (vararg) {
            av.visit(JvmStdlibNames.JET_VALUE_PARAMETER_VARARG, true);
        }
    }
}
