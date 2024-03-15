// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.java.decompiler.struct.attr;

import org.jetbrains.java.decompiler.code.BytecodeVersion;
import org.jetbrains.java.decompiler.struct.StructRecordComponent;
import org.jetbrains.java.decompiler.struct.consts.ConstantPool;
import org.jetbrains.java.decompiler.util.DataInputFullStream;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/*
  Record_attribute {
      u2 attribute_name_index;
      u4 attribute_length;
      u2 components_count;
      record_component_info components[components_count];
  }
 */
public class StructRecordAttribute extends StructGeneralAttribute {
  List<StructRecordComponent> components;
  
  @Override
  public void initContent(DataInputFullStream data, ConstantPool pool, BytecodeVersion version) throws IOException {
    int componentCount = data.readUnsignedShort();
    StructRecordComponent[] components = new StructRecordComponent[componentCount];
    for (int i = 0; i < componentCount; i++) {
      components[i] = StructRecordComponent.create(data, pool, version);
    }
    this.components = Arrays.asList(components);
  }

  public List<StructRecordComponent> getComponents() {
    return Collections.unmodifiableList(components);
  }
}
