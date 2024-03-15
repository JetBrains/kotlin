/*
 * Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */
package org.jetbrains.java.decompiler.modules.decompiler.exps;

import org.jetbrains.java.decompiler.modules.decompiler.exps.FunctionExprent.FunctionType;
import org.jetbrains.java.decompiler.struct.gen.VarType;
import org.jetbrains.java.decompiler.util.InterpreterUtil;
import org.jetbrains.java.decompiler.util.ListStack;
import org.jetbrains.java.decompiler.util.TextBuffer;

import java.util.BitSet;
import java.util.List;

public class IfExprent extends Exprent {
  public enum Type {
    // The order of these matters, see getNegative()
    EQ(FunctionType.EQ),
    NE(FunctionType.NE),
    LT(FunctionType.LT),
    GE(FunctionType.GE),
    GT(FunctionType.GT),
    LE(FunctionType.LE),
    NULL(FunctionType.EQ),
    NONNULL(FunctionType.NE),
    ICMPEQ(FunctionType.EQ),
    ICMPNE(FunctionType.NE),
    ICMPLT(FunctionType.LT),
    ICMPGE(FunctionType.GE),
    ICMPGT(FunctionType.GT),
    ICMPLE(FunctionType.LE),
    ACMPEQ(FunctionType.EQ),
    ACMPNE(FunctionType.NE),
    VALUE(null),
    ;

    private static final Type[] VALUES = values();

    final FunctionType functionType;

    Type(FunctionType functionType) {
      this.functionType = functionType;
    }

    public Type getNegative() {
      if (this == VALUE) throw new IllegalArgumentException();
      // All types except VALUE are paired up with their inverse,
      // the XOR selects the other item within that pair
      return VALUES[ordinal() ^ 1];
    }
  }

  private Exprent condition;

  public IfExprent(Type ifType, ListStack<Exprent> stack, BitSet bytecodeOffsets) {
    this(null, bytecodeOffsets);

    if (ifType.ordinal() <= Type.LE.ordinal()) {
      stack.push(new ConstExprent(0, true, null));
    }
    else if (ifType.ordinal() <= Type.NONNULL.ordinal()) {
      stack.push(new ConstExprent(VarType.VARTYPE_NULL, null, null));
    }

    condition = ifType.functionType == null ? stack.pop() : new FunctionExprent(ifType.functionType, stack, bytecodeOffsets);
  }

  private IfExprent(Exprent condition, BitSet bytecodeOffsets) {
    super(Exprent.Type.IF);
    this.condition = condition;

    addBytecodeOffsets(bytecodeOffsets);
  }

  @Override
  public Exprent copy() {
    return new IfExprent(condition.copy(), bytecode);
  }

  @Override
  public List<Exprent> getAllExprents(List<Exprent> lst) {
    lst.add(condition);
    return lst;
  }

  @Override
  public TextBuffer toJava(int indent) {
    TextBuffer buf = condition.toJava(indent);
    buf.pushNewlineGroup(indent, 1);
    buf.appendPossibleNewline();
    buf.enclose("if (", ")");
    buf.appendPossibleNewline("", true);
    buf.popNewlineGroup();
    buf.addStartBytecodeMapping(bytecode);
    return buf;
  }

  @Override
  public void replaceExprent(Exprent oldExpr, Exprent newExpr) {
    if (oldExpr == condition) {
      condition = newExpr;
    }
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) return true;
    if (!(o instanceof IfExprent)) return false;

    IfExprent ie = (IfExprent)o;
    return InterpreterUtil.equalObjects(condition, ie.getCondition());
  }

  public IfExprent negateIf() {
    condition = new FunctionExprent(FunctionType.BOOL_NOT, condition, condition.bytecode);
    return this;
  }

  public Exprent getCondition() {
    return condition;
  }

  public void setCondition(Exprent condition) {
    this.condition = condition;
  }

  @Override
  public void getBytecodeRange(BitSet values) {
    measureBytecode(values, condition);
    measureBytecode(values);
  }
}
