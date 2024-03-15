package org.jetbrains.java.decompiler.struct.attr;

import org.jetbrains.java.decompiler.code.BytecodeVersion;
import org.jetbrains.java.decompiler.struct.consts.ConstantPool;
import org.jetbrains.java.decompiler.util.DataInputFullStream;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/*
  PermittedSubclasses_attribute {
      u2 attribute_name_index;
      u4 attribute_length;
      u2 number_of_classes;
      u2 classes[number_of_classes];
  }
 */
public class StructPermittedSubclassesAttribute extends StructGeneralAttribute {
  private final List<String> classes = new ArrayList<>();

  public List<String> getClasses() {
    return Collections.unmodifiableList(classes);
  }

  @Override
  public void initContent(DataInputFullStream data, ConstantPool pool, BytecodeVersion version) throws IOException {
    int count = data.readUnsignedShort();
    for (int i = 0; i < count; i++) {
      classes.add(pool.getPrimitiveConstant(data.readUnsignedShort()).getString());
    }
  }
}
