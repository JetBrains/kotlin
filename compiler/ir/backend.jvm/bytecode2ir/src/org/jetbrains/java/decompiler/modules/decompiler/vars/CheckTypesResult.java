// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.java.decompiler.modules.decompiler.vars;

import org.jetbrains.java.decompiler.modules.decompiler.ValidationHelper;
import org.jetbrains.java.decompiler.modules.decompiler.exps.Exprent;
import org.jetbrains.java.decompiler.struct.gen.VarType;

import java.util.ArrayList;
import java.util.List;

public class CheckTypesResult {
  private final List<ExprentTypePair> lstMaxTypeExprents = new ArrayList<>();
  private final List<ExprentTypePair> lstMinTypeExprents = new ArrayList<>();

  public void addMaxTypeExprent(Exprent exprent, VarType type) {
    lstMaxTypeExprents.add(new ExprentTypePair(exprent, type));
  }

  public void addMinTypeExprent(Exprent exprent, VarType type) {
    lstMinTypeExprents.add(new ExprentTypePair(exprent, type));
  }

  public List<ExprentTypePair> getLstMaxTypeExprents() {
    return lstMaxTypeExprents;
  }

  public List<ExprentTypePair> getLstMinTypeExprents() {
    return lstMinTypeExprents;
  }

  @Override
  public String toString() {
    return "Min: " + this.lstMinTypeExprents + " Max: " + this.lstMaxTypeExprents;
  }

  public static class ExprentTypePair {
    public final Exprent exprent;
    public final VarType type;

    public ExprentTypePair(Exprent exprent, VarType type) {
      ValidationHelper.notNull(exprent);
      ValidationHelper.notNull(type);

      this.exprent = exprent;
      this.type = type;
    }

    @Override
    public String toString() {
      return "<" + exprent.toJava().convertToStringAndAllowDataDiscard() + ", " + type + ">";
    }
  }
}