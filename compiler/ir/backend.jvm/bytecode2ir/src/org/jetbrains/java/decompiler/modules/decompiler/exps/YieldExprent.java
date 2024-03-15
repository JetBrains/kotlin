package org.jetbrains.java.decompiler.modules.decompiler.exps;

import org.jetbrains.java.decompiler.code.CodeConstants;
import org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor;
import org.jetbrains.java.decompiler.modules.decompiler.vars.CheckTypesResult;
import org.jetbrains.java.decompiler.struct.gen.VarType;
import org.jetbrains.java.decompiler.util.TextBuffer;

import java.util.BitSet;
import java.util.List;

public class YieldExprent extends Exprent {
  private Exprent content;
  private final VarType retType;

  public YieldExprent(Exprent content, VarType retType) {
    super(Type.YIELD);
    this.content = content;
    this.retType = retType;
  }

  @Override
  protected List<Exprent> getAllExprents(List<Exprent> list) {
    list.add(this.content);
    return list;
  }

  @Override
  public Exprent copy() {
    return new YieldExprent(this.content.copy(), this.retType);
  }

  @Override
  public CheckTypesResult checkExprTypeBounds() {
    CheckTypesResult result = new CheckTypesResult();

    if (retType.type != CodeConstants.TYPE_VOID) {
      result.addMinTypeExprent(this.content, VarType.getMinTypeInFamily(retType.typeFamily));
      result.addMaxTypeExprent(this.content, retType);
    }

    return result;
  }

  @Override
  public TextBuffer toJava(int indent) {
    TextBuffer buf = new TextBuffer();
    buf.append("yield ");
    ExprProcessor.getCastedExprent(this.content, this.retType, buf, indent, ExprProcessor.NullCastType.DONT_CAST_AT_ALL, false, false, false);

    return buf;
  }

  public Exprent getContent() {
    return content;
  }

  @Override
  public void replaceExprent(Exprent oldExpr, Exprent newExpr) {
    if (oldExpr == this.content) {
      this.content = newExpr;
    }
  }

  @Override
  public VarType getExprType() {
    return this.retType;
  }

  @Override
  public void getBytecodeRange(BitSet values) {
    measureBytecode(values, this.content);
    measureBytecode(values);
  }
}
