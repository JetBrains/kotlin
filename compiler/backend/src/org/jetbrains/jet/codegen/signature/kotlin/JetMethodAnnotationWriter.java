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

public class JetMethodAnnotationWriter {
    private final AnnotationVisitor av;

    private JetMethodAnnotationWriter(AnnotationVisitor av) {
        this.av = av;
    }

    public void writeFlags(int flags) {
        if (flags != JvmStdlibNames.FLAGS_DEFAULT_VALUE) {
            av.visit(JvmStdlibNames.JET_FLAGS_FIELD, flags);
        }
    }

    public void writeTypeParameters(@NotNull String typeParameters) {
        if (typeParameters.length() > 0) {
            av.visit(JvmStdlibNames.JET_METHOD_TYPE_PARAMETERS_FIELD, typeParameters);
        }
    }

    public void writeReturnType(@NotNull String returnType) {
        if (returnType.length() > 0) {
            av.visit(JvmStdlibNames.JET_METHOD_RETURN_TYPE_FIELD, returnType);
        }
    }

    public void writePropertyType(@NotNull String propertyType) {
        if (propertyType.length() > 0) {
            av.visit(JvmStdlibNames.JET_METHOD_PROPERTY_TYPE_FIELD, propertyType);
        }
    }

    public void visitEnd() {
        av.visitEnd();
    }

    public static JetMethodAnnotationWriter visitAnnotation(MethodVisitor mv) {
        return new JetMethodAnnotationWriter(mv.visitAnnotation(JvmStdlibNames.JET_METHOD.getDescriptor(), true));
    }
}
