package org.jetbrains.java.decompiler.api;

import org.jetbrains.java.decompiler.modules.decompiler.stats.RootStatement;
import org.jetbrains.java.decompiler.struct.StructMethod;

public interface SFormsCreator {
  void splitVariables(RootStatement root, StructMethod mt);
}
