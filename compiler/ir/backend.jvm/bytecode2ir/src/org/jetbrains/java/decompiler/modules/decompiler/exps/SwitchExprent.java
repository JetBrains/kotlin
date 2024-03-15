package org.jetbrains.java.decompiler.modules.decompiler.exps;

import org.jetbrains.java.decompiler.code.CodeConstants;
import org.jetbrains.java.decompiler.main.DecompilerContext;
import org.jetbrains.java.decompiler.modules.decompiler.StatEdge;
import org.jetbrains.java.decompiler.modules.decompiler.stats.BasicBlockStatement;
import org.jetbrains.java.decompiler.modules.decompiler.stats.Statement;
import org.jetbrains.java.decompiler.modules.decompiler.stats.SwitchStatement;
import org.jetbrains.java.decompiler.struct.StructClass;
import org.jetbrains.java.decompiler.struct.StructField;
import org.jetbrains.java.decompiler.struct.gen.VarType;
import org.jetbrains.java.decompiler.util.TextBuffer;

import java.util.*;

public class SwitchExprent extends Exprent {
  private final SwitchStatement backing;
  private final VarType type;
  // TODO: is needed?
  private final boolean fallthrough;
  // Whether the switch expression returns a value, for case type coercion
  private final boolean standalone;

  public SwitchExprent(SwitchStatement backing, VarType type, boolean fallthrough, boolean standalone) {
    super(Type.SWITCH);
    this.backing = backing;
    this.type = type;
    this.fallthrough = fallthrough;
    this.standalone = standalone;
  }

  @Override
  public TextBuffer toJava(int indent) {
    // Validity checks
    if (!this.backing.isPhantom()) {
      throw new IllegalStateException("Switch expression backing statement isn't phantom!");
    }

    TextBuffer buf = new TextBuffer();

    VarType switchType = this.backing.getHeadexprent().getExprType();
    boolean isExhaustive = isExhaustive();

    buf.append(this.backing.getHeadexprent().toJava(indent)).append(" {").appendLineSeparator();
    for (int i = 0; i < this.backing.getCaseStatements().size(); i++) {
      Statement stat = this.backing.getCaseStatements().get(i);
      List<StatEdge> edges = this.backing.getCaseEdges().get(i);
      List<Exprent> values = this.backing.getCaseValues().get(i);
      Exprent guard = this.backing.getCaseGuards().size() > i ? this.backing.getCaseGuards().get(i) : null;

      boolean hasDefault = false;
      // As switch expressions can be compiled to a tableswitch, any gaps will contain a jump to the default element.
      // Switch expressions cannot have a case point to the same statement as the default, so we check for default first and don't check for cases if it exists [TestConstructorSwitchExpression1]

      for (StatEdge edge : edges) {
        if (edge == this.backing.getDefaultEdge()) {
          if (isExhaustive) {
            if (isSyntheticThrowEdge(edge)) {
              break; // just don't mark as default
            }
          }
          hasDefault = true;
          break;
        }
      }

      boolean hasEdge = false;
      for (int j = 0; j < edges.size(); j++) {
        Exprent value = edges.get(j) == backing.getDefaultEdge() && values.size() <= j
          ? new ConstExprent(VarType.VARTYPE_NULL, null, null) // a total branch in j17 would act as a null branch too
          : values.get(j);
        if (value == null) { // TODO: how can this be null? Is it trying to inject a synthetic case value in switch-on-string processing? [TestSwitchDefaultBefore]
          continue;
        }

        // only a null label changes `default` label semantics
        if (hasDefault && (value.getExprType() != VarType.VARTYPE_NULL)) {
          continue;
        }

        if (!hasEdge) {
          buf.appendIndent(indent + 1).append("case ");
        } else {
          buf.append(", ");
        }

        if (value instanceof ConstExprent && !standalone && !Objects.equals(value.getExprType(), VarType.VARTYPE_NULL)) {
          value = value.copy();
          ((ConstExprent) value).setConstType(switchType);
        }

        if (value instanceof FieldExprent && ((FieldExprent) value).isStatic()) { // enum values
          buf.append(((FieldExprent) value).getName());
        } else if (value instanceof FunctionExprent && ((FunctionExprent) value).getFuncType() == FunctionExprent.FunctionType.INSTANCEOF) {
          // Pattern matching variables
          List<Exprent> operands = ((FunctionExprent) value).getLstOperands();
          buf.append(operands.get(1).toJava(indent));
          buf.append(" ");
          // We're pasting the var type, don't do it again
          ((VarExprent)operands.get(2)).setDefinition(false);
          buf.append(operands.get(2).toJava(indent));
        } else {
          buf.append(value.toJava(indent));
        }

        hasEdge = true;
      }

      if (guard != null) {
        buf.append(" when ").append(guard.toJava());
      }

      if (hasDefault) {
        if (!hasEdge) {
          buf.appendIndent(indent + 1).append("default");
        } else {
          buf.append(", default");
        }
        hasEdge = true;
      }

      if (!hasEdge) { // if we're the synthetic throw edge, we have no cases
        continue;
      }

      buf.append(" -> ");

      boolean simple = stat instanceof BasicBlockStatement;

      if (stat.getExprents() != null && stat.getExprents().size() != 1) {
        simple = false;
      }

      // Single yield or throw
      if (simple) {
        Exprent exprent = stat.getExprents().get(0);

        if (exprent instanceof YieldExprent) {
          Exprent content = ((YieldExprent) exprent).getContent();

          if (content instanceof ConstExprent && !Objects.equals(content.getExprType(), VarType.VARTYPE_NULL)) {
            ((ConstExprent)content).setConstType(this.type);
          }

          buf.append(content.toJava(indent).append(";"));
        } else if (exprent instanceof ExitExprent) {
          ExitExprent exit = (ExitExprent) exprent;

          if (exit.getExitType() == ExitExprent.Type.THROW) {
            buf.append(exit.toJava(indent).append(";"));
          } else {
            throw new IllegalStateException("Can't have return in switch expression");
          }
        } else { // Catchall
          buf.append(exprent.toJava(indent).append(";"));
        }
      } else {
        buf.append("{");
        buf.appendLineSeparator();
        TextBuffer statBuf = stat.toJava(indent + 2);
        buf.append(statBuf);
        buf.appendIndent(indent + 1).append("}");
      }

      buf.appendLineSeparator();
    }

    buf.appendIndent(indent).append("}");

    return buf;
  }

  private boolean isExhaustive() {
    // exhaustive switches have a synthetic default edge of throw new IncompatibleClassChangeException()
    // see TestInlineSwitchExpression1
    // TODO: extend to exhaustive switch statements - consider MatchException
    Set<FieldExprent> enumValuesSeen = new HashSet<>();
    for (List<Exprent> caseValue : backing.getCaseValues()) {
      for (Exprent exprent : caseValue) {
        // only enums
        if (exprent instanceof FieldExprent && ((FieldExprent) exprent).isStatic()) {
          enumValuesSeen.add((FieldExprent) exprent);
        }
      }
    }
    StructClass enumTargetClass = DecompilerContext.getStructContext().getClass(backing.getHeadexprent().getExprType().value);
    return enumTargetClass != null && enumTargetClass.hasModifier(CodeConstants.ACC_ENUM)
      && enumTargetClass.getFields()
        .stream()
        .filter(x -> x.hasModifier(CodeConstants.ACC_ENUM))
        .map(StructField::getName)
        .allMatch(fieldName -> enumValuesSeen.stream().anyMatch(found -> found.getName().equals(fieldName)));
  }

  private static boolean isSyntheticThrowEdge(StatEdge edge){
    Statement target = edge.getDestination();
    List<Exprent> targetExprs = target.getExprents();
    if (targetExprs != null && targetExprs.size() == 1) {
      Exprent targetExpr = targetExprs.get(0);
      return targetExpr instanceof ExitExprent
        && ((ExitExprent) targetExpr).getExitType() == ExitExprent.Type.THROW
        && ((ExitExprent) targetExpr).getValue().getExprType().value.equals("java/lang/IncompatibleClassChangeError");
    }
    return false;
  }

  @Override
  public int getPrecedence() {
    return 1; // Should enclose in case of invocation
  }

  @Override
  public VarType getExprType() {
    return this.type;
  }

  @Override
  public Exprent copy() {
    return new SwitchExprent(this.backing, this.type, this.fallthrough, this.standalone);
  }

  @Override
  protected List<Exprent> getAllExprents(List<Exprent> list) {
    return list;
  }

  @Override
  public void getBytecodeRange(BitSet values) {
    measureBytecode(values);
  }
}
