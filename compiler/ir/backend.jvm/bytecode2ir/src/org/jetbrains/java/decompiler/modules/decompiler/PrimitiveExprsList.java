// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.java.decompiler.modules.decompiler;

import org.jetbrains.java.decompiler.modules.decompiler.exps.Exprent;
import org.jetbrains.java.decompiler.util.ListStack;

import java.util.ArrayList;
import java.util.List;

public class PrimitiveExprsList {

  private final List<Exprent> lstExprents = new ArrayList<>();

  private ListStack<Exprent> stack = new ListStack<>();

  public PrimitiveExprsList() {
  }

  public PrimitiveExprsList copyStack() {
    PrimitiveExprsList prlst = new PrimitiveExprsList();
    prlst.setStack(stack.clone());
    return prlst;
  }

  public List<Exprent> getLstExprents() {
    return lstExprents;
  }

  public ListStack<Exprent> getStack() {
    return stack;
  }

  public void setStack(ListStack<Exprent> stack) {
    this.stack = stack;
  }
}
