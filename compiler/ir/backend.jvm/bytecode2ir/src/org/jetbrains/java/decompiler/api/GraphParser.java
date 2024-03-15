package org.jetbrains.java.decompiler.api;

import org.jetbrains.java.decompiler.code.cfg.ControlFlowGraph;
import org.jetbrains.java.decompiler.modules.decompiler.stats.RootStatement;
import org.jetbrains.java.decompiler.struct.StructMethod;

// Turns a control flow graph into a structured statement
public interface GraphParser {
  RootStatement createStatement(ControlFlowGraph graph, StructMethod mt);
}
