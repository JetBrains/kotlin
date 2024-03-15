// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.java.decompiler.struct.gen.generics;

import org.jetbrains.java.decompiler.struct.gen.VarType;

public class GenericFieldDescriptor {
  public final VarType type;

  public GenericFieldDescriptor(VarType type) {
    this.type = type;
  }
}