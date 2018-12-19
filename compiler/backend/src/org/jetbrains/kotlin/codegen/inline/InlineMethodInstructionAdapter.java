/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen.inline;

import org.jetbrains.org.objectweb.asm.MethodVisitor;
import org.jetbrains.org.objectweb.asm.Opcodes;
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter;

public class InlineMethodInstructionAdapter extends InstructionAdapter {

    InlineMethodInstructionAdapter(MethodVisitor methodVisitor) {
        super(Opcodes.API_VERSION, methodVisitor);
    }

    public void visitAnnotableParameterCount(int parameterCount, boolean visible) {

    }
}