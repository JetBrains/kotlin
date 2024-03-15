// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.java.decompiler.modules.decompiler.vars;

import org.jetbrains.java.decompiler.modules.decompiler.ValidationHelper;

public class VarVersionEdge { // FIXME: can be removed?

  public static final int EDGE_GENERAL = 0;
  public static final int EDGE_PHANTOM = 1;

  public final int type;

  public final VarVersionNode source;

  public final VarVersionNode dest;

  private final int hashCode;

  public VarVersionEdge(int type, VarVersionNode source, VarVersionNode dest) {
    ValidationHelper.notNull(source);
    ValidationHelper.notNull(dest);

    this.type = type;
    this.source = source;
    this.dest = dest;
    this.hashCode = source.hashCode() ^ dest.hashCode() + type;
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) return true;
    if (!(o instanceof VarVersionEdge)) return false;

    VarVersionEdge edge = (VarVersionEdge)o;
    return type == edge.type && source == edge.source && dest == edge.dest;
  }

  @Override
  public int hashCode() {
    return hashCode;
  }

  @Override
  public String toString() {
    return source.toString() + " ->" + type + "-> " + dest.toString();
  }
}
