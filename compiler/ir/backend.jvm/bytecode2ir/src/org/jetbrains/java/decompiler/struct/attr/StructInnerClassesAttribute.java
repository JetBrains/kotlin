// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.java.decompiler.struct.attr;

import org.jetbrains.java.decompiler.code.BytecodeVersion;
import org.jetbrains.java.decompiler.struct.consts.ConstantPool;
import org.jetbrains.java.decompiler.util.DataInputFullStream;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class StructInnerClassesAttribute extends StructGeneralAttribute {
  public static final class Entry {
    public final int outerNameIdx;
    public final int simpleNameIdx;
    public final int accessFlags;
    public final String innerName;
    public final String enclosingName;
    public final String simpleName;

    private Entry(int outerNameIdx, int simpleNameIdx, int accessFlags, String innerName, String enclosingName, String simpleName) {
      this.outerNameIdx = outerNameIdx;
      this.simpleNameIdx = simpleNameIdx;
      this.accessFlags = accessFlags;
      this.innerName = innerName;
      this.enclosingName = enclosingName;
      this.simpleName = simpleName;
    }
  }

  private List<Entry> entries;

  @Override
  public void initContent(DataInputFullStream data, ConstantPool pool, BytecodeVersion version) throws IOException {
    int len = data.readUnsignedShort();
    if (len > 0) {
      entries = new ArrayList<>(len);

      for (int i = 0; i < len; i++) {
        int innerNameIdx = data.readUnsignedShort();
        int outerNameIdx = data.readUnsignedShort();
        int simpleNameIdx = data.readUnsignedShort();
        int accessFlags = data.readUnsignedShort();

        String innerName = pool.getPrimitiveConstant(innerNameIdx).getString();
        String outerName = outerNameIdx != 0 ? pool.getPrimitiveConstant(outerNameIdx).getString() : null;
        String simpleName = simpleNameIdx != 0 ? pool.getPrimitiveConstant(simpleNameIdx).getString() : null;

        entries.add(new Entry(outerNameIdx, simpleNameIdx, accessFlags, innerName, outerName, simpleName));
      }
    }
    else {
      entries = Collections.emptyList();
    }
  }

  public List<Entry> getEntries() {
    return entries;
  }
}