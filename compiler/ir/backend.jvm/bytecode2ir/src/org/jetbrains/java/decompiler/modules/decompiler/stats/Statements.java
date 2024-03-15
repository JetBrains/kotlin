// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.java.decompiler.modules.decompiler.stats;

import org.jetbrains.java.decompiler.main.rels.ClassWrapper;
import org.jetbrains.java.decompiler.main.rels.MethodWrapper;
import org.jetbrains.java.decompiler.modules.decompiler.exps.InvocationExprent;
import org.jetbrains.java.decompiler.modules.decompiler.exps.VarExprent;
import org.jetbrains.java.decompiler.modules.decompiler.vars.VarVersionPair;

public final class Statements {
  public static Statement findFirstData(Statement stat) {
    if (stat.getExprents() != null) {
      return stat;
    }
    else if (stat.isLabeled()) { // FIXME: Why??
      return null;
    }

    switch (stat.type) {
      case SEQUENCE:
      case IF:
      case ROOT:
      case SWITCH:
      case SYNCHRONIZED:
        return findFirstData(stat.getFirst());
      default:
        return null;
    }
  }

  public static boolean isInvocationInitConstructor(InvocationExprent inv, MethodWrapper method, ClassWrapper wrapper, boolean withThis) {
    if (inv.getFunctype() == InvocationExprent.Type.INIT && inv.getInstance() instanceof VarExprent) {
      VarExprent instVar = (VarExprent)inv.getInstance();
      VarVersionPair varPair = new VarVersionPair(instVar);
      String className = method.varproc.getThisVars().get(varPair);
      if (className != null) { // any this instance. TODO: Restrict to current class?
        return withThis || !wrapper.getClassStruct().qualifiedName.equals(inv.getClassname());
      }
    }

    return false;
  }
}