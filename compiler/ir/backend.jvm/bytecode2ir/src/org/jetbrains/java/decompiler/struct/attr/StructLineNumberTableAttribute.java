// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.java.decompiler.struct.attr;

import org.jetbrains.java.decompiler.code.BytecodeVersion;
import org.jetbrains.java.decompiler.struct.consts.ConstantPool;
import org.jetbrains.java.decompiler.util.DataInputFullStream;
import org.jetbrains.java.decompiler.util.InterpreterUtil;

import java.io.IOException;

/**
 * u2 line_number_table_length;
 * {  u2 start_pc;
 *    u2 line_number;
 * } line_number_table[line_number_table_length];
 *
 * Created by Egor on 05.10.2014.
 */
public class StructLineNumberTableAttribute extends StructGeneralAttribute {
  private int[] myLineInfo = InterpreterUtil.EMPTY_INT_ARRAY;

  @Override
  public void initContent(DataInputFullStream data, ConstantPool pool, BytecodeVersion version) throws IOException {
    int len = data.readUnsignedShort() * 2;
    if (len > 0) {
      myLineInfo = new int[len];
      for (int i = 0; i < len; i += 2) {
        myLineInfo[i] = data.readUnsignedShort();
        myLineInfo[i + 1] = data.readUnsignedShort();
      }
    }
    else if (myLineInfo.length > 0) {
      myLineInfo = InterpreterUtil.EMPTY_INT_ARRAY;
    }
  }

  public int findLineNumber(int pc) {
    if (myLineInfo.length >= 2) {
      for (int i = myLineInfo.length - 2; i >= 0; i -= 2) {
        if (pc >= myLineInfo[i]) {
          return myLineInfo[i + 1];
        }
      }
    }
    return -1;
  }

  public int[] getRawData() {
    return myLineInfo;
  }
}