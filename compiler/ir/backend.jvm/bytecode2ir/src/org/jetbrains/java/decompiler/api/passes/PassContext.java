package org.jetbrains.java.decompiler.api.passes;

import org.jetbrains.java.decompiler.code.cfg.ControlFlowGraph;
import org.jetbrains.java.decompiler.main.rels.DecompileRecord;
import org.jetbrains.java.decompiler.modules.decompiler.stats.RootStatement;
import org.jetbrains.java.decompiler.modules.decompiler.vars.VarProcessor;
import org.jetbrains.java.decompiler.struct.StructClass;
import org.jetbrains.java.decompiler.struct.StructMethod;

public final class PassContext {
  private RootStatement root;
  private final ControlFlowGraph graph;
  private final StructMethod mt;
  private final StructClass cl;
  private final VarProcessor varProc;
  private final DecompileRecord rec;

  public PassContext(RootStatement root, ControlFlowGraph graph, StructMethod mt, StructClass cl, VarProcessor varProc, DecompileRecord rec) {
    this.root = root;
    this.graph = graph;
    this.mt = mt;
    this.cl = cl;
    this.varProc = varProc;
    this.rec = rec;
  }

  public RootStatement getRoot() {
    return root;
  }

  public void setRoot(RootStatement root) {
    this.root = root;
  }

  public ControlFlowGraph getGraph() {
    return graph;
  }

  public StructMethod getMethod() {
    return mt;
  }

  public StructClass getEnclosingClass() {
    return cl;
  }

  public VarProcessor getVarProc() {
    return varProc;
  }

  public DecompileRecord getRec() {
    return rec;
  }
}
