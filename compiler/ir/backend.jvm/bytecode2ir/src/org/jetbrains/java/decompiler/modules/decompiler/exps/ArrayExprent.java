// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.java.decompiler.modules.decompiler.exps;

import org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor;
import org.jetbrains.java.decompiler.modules.decompiler.vars.CheckTypesResult;
import org.jetbrains.java.decompiler.struct.gen.VarType;
import org.jetbrains.java.decompiler.util.InterpreterUtil;
import org.jetbrains.java.decompiler.util.TextBuffer;

import java.util.BitSet;
import java.util.List;

public class ArrayExprent extends Exprent {
  private Exprent array;
  private Exprent index;
  private final VarType hardType;

  public ArrayExprent(Exprent array, Exprent index, VarType hardType, BitSet bytecodeOffsets) {
    super(Type.ARRAY);
    this.array = array;
    this.index = index;
    this.hardType = hardType;

    addBytecodeOffsets(bytecodeOffsets);
  }

  @Override
  public Exprent copy() {
    return new ArrayExprent(array.copy(), index.copy(), hardType, bytecode);
  }

  @Override
  public VarType getExprType() {
    VarType exprType = array.getExprType();
    if (exprType.equals(VarType.VARTYPE_NULL)) {
      return hardType.copy();
    }
    else {
      return exprType.decreaseArrayDim();
    }
  }

  @Override
  public VarType getInferredExprType(VarType upperBound) {
    VarType exprType = array.getInferredExprType(upperBound);
    if (exprType.equals(VarType.VARTYPE_NULL)) {
      return hardType.copy();
    }
    else {
      return exprType.decreaseArrayDim();
    }
  }

  @Override
  public int getExprentUse() {
    return array.getExprentUse() & index.getExprentUse() & Exprent.MULTIPLE_USES;
  }

  @Override
  public CheckTypesResult checkExprTypeBounds() {
    CheckTypesResult result = new CheckTypesResult();
    result.addMinTypeExprent(index, VarType.VARTYPE_BYTECHAR);
    result.addMaxTypeExprent(index, VarType.VARTYPE_INT);
    return result;
  }

  @Override
  public List<Exprent> getAllExprents(List<Exprent> lst) {
    lst.add(array);
    lst.add(index);
    return lst;
  }

  @Override
  public TextBuffer toJava(int indent) {
    TextBuffer res = array.toJava(indent);

    if (array.getPrecedence() > getPrecedence() && !canSkipParenEnclose(array)) { // array precedence equals 0
      res.encloseWithParens();
    }

    VarType arrType = array.getExprType();
    if (arrType.arrayDim == 0) {
      VarType objArr = VarType.VARTYPE_OBJECT.resizeArrayDim(1); // type family does not change
      res.enclose("((" + ExprProcessor.getCastTypeName(objArr) + ")", ")");
    }

    res.addBytecodeMapping(bytecode);

    return res.append('[').append(index.toJava(indent)).append(']');
  }

  private boolean canSkipParenEnclose(Exprent instance) {
    if (!(instance instanceof NewExprent)) {
      return false;
    }

    NewExprent newExpr = (NewExprent) instance;

    return newExpr.isDirectArrayInit() || !newExpr.getLstArrayElements().isEmpty();
  }

  @Override
  public void replaceExprent(Exprent oldExpr, Exprent newExpr) {
    if (oldExpr == array) {
      array = newExpr;
    }
    if (oldExpr == index) {
      index = newExpr;
    }
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) return true;
    if (!(o instanceof ArrayExprent)) return false;

    ArrayExprent arr = (ArrayExprent)o;
    return InterpreterUtil.equalObjects(array, arr.getArray()) &&
           InterpreterUtil.equalObjects(index, arr.getIndex());
  }

  public Exprent getArray() {
    return array;
  }

  public Exprent getIndex() {
    return index;
  }
  
  @Override
  public void getBytecodeRange(BitSet values) {
    measureBytecode(values, array);
    measureBytecode(values, index);
    measureBytecode(values);
  }
}