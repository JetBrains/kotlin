// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.java.decompiler.code;

public class SwitchInstruction extends Instruction {
  private int[] destinations;
  private int[] values;
  private int defaultDestination;

  public SwitchInstruction(int opcode, int group, boolean wide, BytecodeVersion bytecodeVersion, int[] operands, int length) {
    super(opcode, group, wide, bytecodeVersion, operands, length);
  }

  @Override
  public void initInstruction(InstructionSequence seq) {
    defaultDestination = seq.getPointerByRelOffset(operands[0]);

    int prefix = opcode == CodeConstants.opc_tableswitch ? 3 : 2;
    int len = operands.length - prefix;
    int low = 0;
    if (opcode == CodeConstants.opc_lookupswitch) {
      len /= 2;
    }
    else {
      low = operands[1];
    }

    destinations = new int[len];
    values = new int[len];
    for (int i = 0, k = 0; i < len; i++, k++) {
      if (opcode == CodeConstants.opc_lookupswitch) {
        values[i] = operands[prefix + k];
        k++;
      }
      else {
        values[i] = low + k;
      }
      destinations[i] = seq.getPointerByRelOffset(operands[prefix + k]);
    }
  }

  public int[] getDestinations() {
    return destinations;
  }

  public int[] getValues() {
    return values;
  }

  public int getDefaultDestination() {
    return defaultDestination;
  }

  @Override
  public SwitchInstruction clone() {
    SwitchInstruction copy = (SwitchInstruction)super.clone();
    copy.defaultDestination = defaultDestination;
    copy.destinations = destinations.clone();
    copy.values = values.clone();
    return copy;
  }
}
