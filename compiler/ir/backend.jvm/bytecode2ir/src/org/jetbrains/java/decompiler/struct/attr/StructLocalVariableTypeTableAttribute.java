// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.java.decompiler.struct.attr;

import org.jetbrains.java.decompiler.code.BytecodeVersion;
import org.jetbrains.java.decompiler.struct.consts.ConstantPool;
import org.jetbrains.java.decompiler.util.DataInputFullStream;

import java.io.IOException;

/*
  u2 local_variable_type_table_length;
    {   u2 start_pc;
        u2 length;
        u2 name_index;
        u2 signature_index;
        u2 index;
    } local_variable_type_table[local_variable_type_table_length];
*/
public class StructLocalVariableTypeTableAttribute extends StructGeneralAttribute {
  // store signature instead of descriptor
  final StructLocalVariableTableAttribute backingAttribute = new StructLocalVariableTableAttribute();

  @Override
  public void initContent(DataInputFullStream data, ConstantPool pool, BytecodeVersion version) throws IOException {
    backingAttribute.initContent(data, pool, version);
  }

  public void add(StructLocalVariableTypeTableAttribute attr) {
    backingAttribute.add(attr.backingAttribute);
  }

  public String getSignature(int index, int visibleOffset) {
    return backingAttribute.getDescriptor(index, visibleOffset);
  }
}
