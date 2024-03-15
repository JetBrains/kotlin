// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.java.decompiler.modules.decompiler.stats;

import org.jetbrains.java.decompiler.code.cfg.ControlFlowGraph;
import org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor;
import org.jetbrains.java.decompiler.struct.StructMethod;
import org.jetbrains.java.decompiler.util.StartEndPair;
import org.jetbrains.java.decompiler.util.TextBuffer;

import java.util.LinkedHashSet;
import java.util.Set;

public final class RootStatement extends Statement {
  private final DummyExitStatement dummyExit;
  public final StructMethod mt;
  public Set<String> commentLines = null;
  public boolean addErrorComment = false;
  private final ContentFlags flags = new ContentFlags();

  public RootStatement(Statement head, DummyExitStatement dummyExit, StructMethod mt) {
    super(StatementType.ROOT);

    first = head;
    this.dummyExit = dummyExit;
    this.mt = mt;
    if (this.first == null) {
      throw new IllegalStateException("Root statement has no content!");
    }

    stats.addWithKey(first, first.id);
    first.setParent(this);
  }

  @Override
  public TextBuffer toJava(int indent) {
    return ExprProcessor.listToJava(varDefinitions, indent).append(first.toJava(indent));
  }

  public DummyExitStatement getDummyExit() {
    return dummyExit;
  }

  public void addComment(String comment) {
    addComment(comment, false);
  }

  public void addComment(String comment, boolean error) {
    if (commentLines == null) {
      commentLines = new LinkedHashSet<>();
    }

    commentLines.add(comment);
    addErrorComment |= error;
  }

  public void addComments(RootStatement root) {
    if (root.commentLines != null) {
      for (String s : root.commentLines) {
        addComment(s);
      }
    }

    addErrorComment |= root.addErrorComment;
  }

  public void addComments(ControlFlowGraph graph) {
    if (graph.commentLines != null) {
      for (String s : graph.commentLines) {
        addComment(s);
      }
    }

    addErrorComment |= graph.addErrorComment;
  }

  public void buildContentFlags() {
    buildContentFlagsStat(this);
  }

  private void buildContentFlagsStat(Statement stat) {
    for (Statement st : stat.stats) {
      buildContentFlagsStat(st);
    }

    if (stat instanceof CatchStatement || stat instanceof CatchAllStatement) {
      this.flags.hasTryCatch = true;
    } else if (stat instanceof DoStatement) {
      this.flags.hasLoops = true;
    } else if (stat instanceof SwitchStatement) {
      this.flags.hasSwitch = true;
    }
  }

  public boolean hasTryCatch() {
    return this.flags.hasTryCatch;
  }

  public boolean hasLoops() {
    return this.flags.hasLoops;
  }

  public boolean hasSwitch() {
    return this.flags.hasSwitch;
  }

  @Override
  public StartEndPair getStartEndRange() {
    return StartEndPair.join(first.getStartEndRange(), dummyExit != null ? dummyExit.getStartEndRange() : null);
  }

  private static class ContentFlags {
    private boolean hasTryCatch;
    private boolean hasLoops;
    private boolean hasSwitch;
  }
}
