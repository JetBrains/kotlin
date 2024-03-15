// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.java.decompiler.code;

import org.jetbrains.java.decompiler.util.VBStyleCollection;


public class FullInstructionSequence extends InstructionSequence {

  // *****************************************************************************
  // constructors
  // *****************************************************************************

  public FullInstructionSequence(VBStyleCollection<Instruction, Integer> collinstr, ExceptionTable extable) {
    super(collinstr);
    this.exceptionTable = extable;

    // translate raw exception handlers to instr
    for (ExceptionHandler handler : extable.getHandlers()) {
      handler.from_instr = this.getPointerByAbsOffset(handler.from);
      int toIndex = this.getPointerByAbsOffset(handler.to);
      handler.to_instr = toIndex == -1 ? this.collinstr.size() : toIndex;
      handler.handler_instr = this.getPointerByAbsOffset(handler.handler);
    }
  }
}
