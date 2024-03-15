/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */
package org.jetbrains.java.decompiler.struct.attr;

import org.jetbrains.java.decompiler.code.BytecodeVersion;
import org.jetbrains.java.decompiler.struct.consts.ConstantPool;
import org.jetbrains.java.decompiler.util.DataInputFullStream;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/*
  u1 parameters_count;
  {   u2 name_index;
      u2 access_flags;
  } parameters[parameters_count];
*/
public class StructMethodParametersAttribute extends StructGeneralAttribute {
  private List<Entry> myEntries;

  @Override
  public void initContent(DataInputFullStream data, ConstantPool pool, BytecodeVersion version) throws IOException {
    int len = data.readUnsignedByte();
    List<Entry> entries;
    if (len > 0) {
      entries = new ArrayList<>(len);

      for (int i = 0; i < len; i++) {
        int nameIndex = data.readUnsignedShort();
        String name = nameIndex != 0 ? pool.getPrimitiveConstant(nameIndex).getString() : null;
        int access_flags = data.readUnsignedShort();
        entries.add(new Entry(name, access_flags));
      }
    }
    else {
      entries = Collections.emptyList();
    }
    myEntries = Collections.unmodifiableList(entries);
  }

  public List<Entry> getEntries() {
    return myEntries;
  }

  public static class Entry {
    public final String myName;
    public final int myAccessFlags;

    public Entry(String name, int accessFlags) {
      myName = name;
      myAccessFlags = accessFlags;
    }
  }
}
