package org.jetbrains.java.decompiler.api;

import org.jetbrains.java.decompiler.modules.decompiler.flow.DirectGraph;
import org.jetbrains.java.decompiler.modules.decompiler.stats.RootStatement;

public interface GraphFlattener {
  DirectGraph buildDirectGraph(RootStatement root);
}
