/*
 * Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */
package org.jetbrains.java.decompiler.modules.decompiler.exps;

import org.jetbrains.java.decompiler.main.DecompilerContext;
import org.jetbrains.java.decompiler.main.extern.IFernflowerLogger;
import org.jetbrains.java.decompiler.struct.gen.VarType;
import org.jetbrains.java.decompiler.util.InterpreterUtil;
import org.jetbrains.java.decompiler.util.TextBuffer;

import java.util.BitSet;
import java.util.List;

public class MonitorExprent extends Exprent {
  public enum Type {
    ENTER, EXIT
  }

  private boolean remove = false;
  private final Type monType;
  private Exprent value;

  public MonitorExprent(Type monType, Exprent value, BitSet bytecodeOffsets) {
    super(Exprent.Type.MONITOR);
    this.monType = monType;
    this.value = value;

    addBytecodeOffsets(bytecodeOffsets);
  }

  @Override
  public Exprent copy() {
    return new MonitorExprent(monType, value.copy(), bytecode);
  }

  @Override
  public List<Exprent> getAllExprents(List<Exprent> lst) {
    lst.add(value);
    return lst;
  }

  @Override
  public TextBuffer toJava(int indent) {
    TextBuffer buf = new TextBuffer();
    buf.addBytecodeMapping(bytecode);

    if (monType == Type.ENTER) {
      // Warn if the synchronized method is synchronizing on null, as that is invalid [https://docs.oracle.com/javase/specs/jls/se16/html/jls-14.html#jls-14.19]
      if (this.value instanceof ConstExprent && this.value.getExprType() == VarType.VARTYPE_NULL) {
        DecompilerContext.getLogger().writeMessage("Created invalid synchronize on null!" , IFernflowerLogger.Severity.WARN);
      }
      return buf.append(value.toJava(indent)).enclose("synchronized(", ")");
    }
    else {
      return buf.append("// $VF: monitorexit");
    }
  }

  @Override
  public void replaceExprent(Exprent oldExpr, Exprent newExpr) {
    if (oldExpr == value) {
      value = newExpr;
    }
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) return true;
    if (!(o instanceof MonitorExprent)) return false;

    MonitorExprent me = (MonitorExprent)o;
    return monType == me.getMonType() &&
           InterpreterUtil.equalObjects(value, me.getValue());
  }

  public Type getMonType() {
    return monType;
  }

  public Exprent getValue() {
    return value;
  }

  public boolean isRemovable() {
    return remove;
  }

  public void setRemove(boolean remove) {
    this.remove = remove;
  }

  @Override
  public void getBytecodeRange(BitSet values) {
    measureBytecode(values, value);
    measureBytecode(values);
  }
}
