// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.java.decompiler.code;

import org.jetbrains.java.decompiler.util.VBStyleCollection;

public class SimpleInstructionSequence extends InstructionSequence {

  public SimpleInstructionSequence() {
  }

  public SimpleInstructionSequence(VBStyleCollection<Instruction, Integer> collinstr) {
    super(collinstr);
  }

  @Override
  public SimpleInstructionSequence clone() {
    SimpleInstructionSequence newseq = new SimpleInstructionSequence(collinstr.clone());
    newseq.setPointer(this.getPointer());

    return newseq;
  }
}
