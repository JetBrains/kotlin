// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.java.decompiler.main.rels;

import org.jetbrains.java.decompiler.main.collectors.CounterContainer;
import org.jetbrains.java.decompiler.modules.decompiler.flow.DirectGraph;
import org.jetbrains.java.decompiler.modules.decompiler.flow.FlattenStatementsHelper;
import org.jetbrains.java.decompiler.modules.decompiler.stats.RootStatement;
import org.jetbrains.java.decompiler.modules.decompiler.vars.VarProcessor;
import org.jetbrains.java.decompiler.modules.decompiler.vars.VarVersionPair;
import org.jetbrains.java.decompiler.struct.StructClass;
import org.jetbrains.java.decompiler.struct.StructMethod;

import java.util.*;

public class MethodWrapper {
  public final RootStatement root;
  public final VarProcessor varproc;
  public final StructMethod methodStruct;
  public final StructClass classStruct;
  public final CounterContainer counter;
  public final Set<String> setOuterVarNames = new HashSet<>();

  public DirectGraph graph;
  public List<VarVersionPair> synthParameters;
  public Throwable decompileError;
  public Set<String> commentLines = null;
  public boolean addErrorComment = false;

  public MethodWrapper(RootStatement root, VarProcessor varproc, StructMethod methodStruct, StructClass classStruct, CounterContainer counter) {
    this.root = root;
    this.varproc = varproc;
    this.methodStruct = methodStruct;
    this.classStruct = classStruct;
    this.counter = counter;

    if (root != null && root.commentLines != null) {
      for (String s : root.commentLines) {
        addComment(s);
      }
      addErrorComment |= root.addErrorComment;
    }
  }

  public DirectGraph getOrBuildGraph() {
    if (graph == null && root != null) {
      graph = new FlattenStatementsHelper().buildDirectGraph(root);
    }

    return graph;
  }

  public void addComment(String comment) {
    if (commentLines == null) {
      commentLines = new LinkedHashSet<>();
    }

    commentLines.add(comment);
  }

  @Override
  public String toString() {
    return methodStruct.getName();
  }
}