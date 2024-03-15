// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.java.decompiler.struct.gen;

import org.jetbrains.java.decompiler.code.CodeConstants;

public final class FieldDescriptor {

  public static final FieldDescriptor INTEGER_DESCRIPTOR = parseDescriptor("Ljava/lang/Integer;");
  public static final FieldDescriptor LONG_DESCRIPTOR = parseDescriptor("Ljava/lang/Long;");
  public static final FieldDescriptor FLOAT_DESCRIPTOR = parseDescriptor("Ljava/lang/Float;");
  public static final FieldDescriptor DOUBLE_DESCRIPTOR = parseDescriptor("Ljava/lang/Double;");

  public final VarType type;
  public final String descriptorString;

  private FieldDescriptor(String descriptor) {
    type = new VarType(descriptor);
    descriptorString = descriptor;
  }

  public static FieldDescriptor parseDescriptor(String descriptor) {
    return new FieldDescriptor(descriptor);
  }

  public String buildNewDescriptor(NewClassNameBuilder builder) {
    if (type.type == CodeConstants.TYPE_OBJECT) {
      String newClassName = builder.buildNewClassname(type.value);
      if (newClassName != null) {
        return new VarType(type.type, type.arrayDim, newClassName).toString();
      }
    }

    return null;
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) return true;
    if (!(o instanceof FieldDescriptor)) return false;

    FieldDescriptor fd = (FieldDescriptor)o;
    return type.equals(fd.type);
  }

  @Override
  public int hashCode() {
    return type.hashCode();
  }
}
