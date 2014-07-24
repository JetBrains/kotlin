/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.codegen.inline;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.org.objectweb.asm.AnnotationVisitor;
import org.jetbrains.org.objectweb.asm.MethodVisitor;
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter;

public class InliningInstructionAdapter extends InstructionAdapter {

    public InliningInstructionAdapter(MethodVisitor mv) {
        super(InlineCodegenUtil.API, mv);
    }

    @Override
    public AnnotationVisitor visitAnnotationDefault() {
        return null;
    }

    @Override
    public void visitMaxs(int maxStack, int maxLocals) {

    }

    @Override
    public AnnotationVisitor visitAnnotation(@NotNull String desc, boolean visible) {
        return null;
    }

    @Override
    public AnnotationVisitor visitParameterAnnotation(int parameter, @NotNull String desc, boolean visible) {
        return null;
    }

    @Override
    public void visitEnd() {

    }
}
