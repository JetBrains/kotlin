/*
 * Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */
package org.jetbrains.java.decompiler.modules.decompiler.exps;

import org.jetbrains.java.decompiler.code.CodeConstants;
import org.jetbrains.java.decompiler.main.DecompilerContext;
import org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor;
import org.jetbrains.java.decompiler.modules.decompiler.vars.CheckTypesResult;
import org.jetbrains.java.decompiler.struct.gen.VarType;
import org.jetbrains.java.decompiler.struct.gen.generics.GenericType;
import org.jetbrains.java.decompiler.struct.match.MatchEngine;
import org.jetbrains.java.decompiler.struct.match.MatchNode;
import org.jetbrains.java.decompiler.util.IntHelper;
import org.jetbrains.java.decompiler.util.InterpreterUtil;
import org.jetbrains.java.decompiler.util.ListStack;
import org.jetbrains.java.decompiler.util.TextBuffer;

import java.util.*;

public class FunctionExprent extends Exprent {

  private static final int[] TYPE_PRIMITIVES = {CodeConstants.TYPE_DOUBLE, CodeConstants.TYPE_FLOAT, CodeConstants.TYPE_LONG};
  private static final VarType[] TYPES = {VarType.VARTYPE_DOUBLE, VarType.VARTYPE_FLOAT, VarType.VARTYPE_LONG};;

  public enum FunctionType {
    ADD(2, "+", 3, null),
    SUB(2, "-", 3, null),
    MUL(2, "*", 2, null),
    DIV(2, "/", 2, null),
    AND(2, "&", 7, null),
    OR(2, "|", 9, null),
    XOR(2, "^", 8, null),
    REM(2, "%", 2, null),
    SHL(2, "<<", 4, null),
    SHR(2, ">>", 4, null),
    USHR(2, ">>>", 4, null),

    BIT_NOT(1, "~", 1, null),
    BOOL_NOT(1, "!", 1, VarType.VARTYPE_BOOLEAN),
    NEG(1, "-", 1, null),

    I2L(VarType.VARTYPE_LONG),
    I2F(VarType.VARTYPE_FLOAT),
    I2D(VarType.VARTYPE_DOUBLE),
    L2I(VarType.VARTYPE_INT),
    L2F(VarType.VARTYPE_FLOAT),
    L2D(VarType.VARTYPE_DOUBLE),
    F2I(VarType.VARTYPE_INT),
    F2L(VarType.VARTYPE_LONG),
    F2D(VarType.VARTYPE_DOUBLE),
    D2I(VarType.VARTYPE_INT),
    D2L(VarType.VARTYPE_LONG),
    D2F(VarType.VARTYPE_FLOAT),
    I2B(VarType.VARTYPE_BYTE),
    I2C(VarType.VARTYPE_CHAR),
    I2S(VarType.VARTYPE_SHORT),

    CAST(2, null, 1, null),
    INSTANCEOF(2, "instanceof", 6, VarType.VARTYPE_BOOLEAN),

    ARRAY_LENGTH(1, null, 0, VarType.VARTYPE_INT),
    IMM(1, "--", 1, null),
    MMI(1, "--", 1, null),
    IPP(1, "++", 1, null),
    PPI(1, "++", 1, null),

    TERNARY(3, null, 12, null),

    LCMP(2, "__lcmp__", -1, VarType.VARTYPE_BOOLEAN),
    FCMPL(2, "__fcmpl__", -1, VarType.VARTYPE_BOOLEAN),
    FCMPG(2, "__fcmpg__", -1, VarType.VARTYPE_BOOLEAN),
    DCMPL(2, "__dcmpl__", -1, VarType.VARTYPE_BOOLEAN),
    DCMPG(2, "__dcmpg__", -1, VarType.VARTYPE_BOOLEAN),

    EQ(2, "==", 6, VarType.VARTYPE_BOOLEAN),
    NE(2, "!=", 6, VarType.VARTYPE_BOOLEAN),
    LT(2, "<", 5, VarType.VARTYPE_BOOLEAN),
    GE(2, ">=", 5, VarType.VARTYPE_BOOLEAN),
    GT(2, ">", 5, VarType.VARTYPE_BOOLEAN),
    LE(2, "<=", 5, VarType.VARTYPE_BOOLEAN),

    BOOLEAN_AND(2, "&&", 10, VarType.VARTYPE_BOOLEAN),
    BOOLEAN_OR(2, "||", 11, VarType.VARTYPE_BOOLEAN),
    STR_CONCAT(2, "+", 3, VarType.VARTYPE_STRING),
    ;

    public final int arity;
    public final String operator;
    public final int precedence;
    public final VarType castType;
    final VarType type;

    FunctionType(int arity, String operator, int precedence, VarType type) {
      this(arity, operator, precedence, null, type);
    }

    FunctionType(VarType castType) {
      this(1, null, 1, castType, castType);
    }

    FunctionType(int arity, String operator, int precedence, VarType castType, VarType type) {
      this.arity = arity;
      this.operator = operator;
      this.precedence = precedence;
      this.castType = castType;
      this.type = type;
    }

    public boolean isArithmeticBinaryOperation() {
      return ordinal() <= USHR.ordinal();
    }

    public boolean isMM() {
      return this == MMI || this == IMM;
    }

    public boolean isPP() {
      return this == PPI || this == IPP;
    }

    public boolean isPPMM() {
      return isPP() || isMM();
    }

    public boolean isPostfixPPMM() {
      return this == IMM || this == IPP;
    }
  }

  private static final Set<FunctionType> ASSOCIATIVITY = new HashSet<>(Arrays.asList(
    FunctionType.ADD, FunctionType.MUL, FunctionType.AND, FunctionType.OR, FunctionType.XOR, FunctionType.BOOLEAN_AND, FunctionType.BOOLEAN_OR, FunctionType.STR_CONCAT));

  private FunctionType funcType;
  private VarType implicitType;
  private final List<Exprent> lstOperands;
  private boolean needsCast = true;
  private boolean disableNewlineGroupCreation = false;

  public FunctionExprent(FunctionType funcType, ListStack<Exprent> stack, BitSet bytecodeOffsets) {
    this(funcType, new ArrayList<>(), bytecodeOffsets);

    if (funcType.arity == 1) {
      lstOperands.add(stack.pop());
    }
    else if (funcType.arity == 2) {
      Exprent expr = stack.pop();
      lstOperands.add(stack.pop());
      lstOperands.add(expr);
    }
    else {
      throw new RuntimeException("no direct instantiation possible: " + funcType);
    }
  }

  public FunctionExprent(FunctionType funcType, List<Exprent> operands, BitSet bytecodeOffsets) {
    super(Type.FUNCTION);
    this.funcType = funcType;
    this.lstOperands = operands;

    addBytecodeOffsets(bytecodeOffsets);
  }

  public FunctionExprent(FunctionType funcType, Exprent operand, BitSet bytecodeOffsets) {
    this(funcType, new ArrayList<>(1), bytecodeOffsets);
    lstOperands.add(operand);
  }

  @Override
  public VarType getExprType() {
    VarType staticType = funcType.type;
    if (staticType != null) {
      return staticType;
    }

    switch (funcType) {
      case IMM:
      case MMI:
      case IPP:
      case PPI:
        return implicitType;
      case SHL:
      case SHR:
      case USHR:
      case BIT_NOT:
      case NEG:
        return getMaxVarType(lstOperands.get(0).getExprType());
      case ADD:
      case SUB:
      case MUL:
      case DIV:
      case REM:
        return getMaxVarType(lstOperands.get(0).getExprType(), lstOperands.get(1).getExprType());
      case AND:
      case OR:
      case XOR: {
        VarType type1 = lstOperands.get(0).getExprType();
        VarType type2 = lstOperands.get(1).getExprType();
        if (type1.type == CodeConstants.TYPE_BOOLEAN && type2.type == CodeConstants.TYPE_BOOLEAN) {
          return VarType.VARTYPE_BOOLEAN;
        } else {
          return getMaxVarType(type1, type2);
        }
      }
      case CAST:
        return lstOperands.get(1).getExprType();
      case TERNARY: {
        Exprent param1 = lstOperands.get(1);
        Exprent param2 = lstOperands.get(2);
        VarType supertype = VarType.getCommonSupertype(param1.getExprType(), param2.getExprType());
        if (supertype == null) {
          throw new IllegalStateException("No common supertype for ternary expression");
        }
        if (param1 instanceof ConstExprent && param2 instanceof ConstExprent &&
          supertype.type != CodeConstants.TYPE_BOOLEAN && VarType.VARTYPE_INT.isSuperset(supertype)) {
          return VarType.VARTYPE_INT;
        }
        else {
          return supertype;
        }
      }
      default: throw new IllegalStateException("No type for funcType=" + funcType);
    }
  }

  @Override
  public VarType getInferredExprType(VarType upperBound) {
    if (funcType == FunctionType.CAST) {
      this.needsCast = true;
      VarType right = lstOperands.get(0).getInferredExprType(upperBound);
      VarType cast = lstOperands.get(1).getExprType();

      if (upperBound != null && (upperBound.isGeneric() || right.isGeneric())) {
        Map<VarType, List<VarType>> names = this.getNamedGenerics();
        int arrayDim = 0;

        if (upperBound.arrayDim == right.arrayDim && upperBound.arrayDim > 0) {
          arrayDim = upperBound.arrayDim;
          upperBound = upperBound.resizeArrayDim(0);
          right = right.resizeArrayDim(0);
        }

        List<VarType> types = names.get(right);
        if (types == null) {
          types = names.get(upperBound);
        }

        if (types != null) {
          boolean anyMatch = false; //TODO: allMatch instead of anyMatch?
          for (VarType type : types) {
            anyMatch |= DecompilerContext.getStructContext().instanceOf(type.value, cast.value);
          }

          if (anyMatch) {
            this.needsCast = false;
          }
        } else {
            this.needsCast = right.type == CodeConstants.TYPE_NULL || !DecompilerContext.getStructContext().instanceOf(right.value, upperBound.value) || !areGenericTypesSame(right, upperBound);
        }

        if (!this.needsCast) {
          if (arrayDim > 0) {
            right = right.resizeArrayDim(arrayDim);
          }

          return right;
        }
      } else { //TODO: Capture generics to make cast better?
        this.needsCast = right.type == CodeConstants.TYPE_NULL || !DecompilerContext.getStructContext().instanceOf(right.value, cast.value);
      }
    } else if (funcType == FunctionType.TERNARY) {
      // TODO return common generic type?
      VarType type1 = lstOperands.get(1).getInferredExprType(upperBound);
      VarType type2 = lstOperands.get(2).getInferredExprType(upperBound);

      if (type1.type == CodeConstants.TYPE_NULL) {
        return type2;
      } else if (type2.type == CodeConstants.TYPE_NULL) {
        return type1;
      }
    }

    return getExprType();
  }

  private static boolean areGenericTypesSame(VarType right, VarType upperBound) {
    if (!(right instanceof GenericType && upperBound instanceof GenericType)) {
      return true; // Prevent this from accidentally always casting
    }

    GenericType rightGeneric = (GenericType)right;
    GenericType upperBoundGeneric = (GenericType)upperBound;

    // Different argument counts, can't be the same!
    if (rightGeneric.getArguments().size() != upperBoundGeneric.getArguments().size()) {
      return false;
    }

    for (int i = 0; i < upperBoundGeneric.getArguments().size(); i++) {
      VarType upperType = upperBoundGeneric.getArguments().get(i);
      VarType rightType = rightGeneric.getArguments().get(i);

      // Trying to cast Obj<?> to Obj<T>, which is an unchecked cast- needs to be explicit
      if (upperType != null && rightType == null) {
        return false;
      }
    }

    return true;
  }

  @Override
  public int getExprentUse() {
    if (funcType.ordinal() >= FunctionType.IMM.ordinal() && funcType.ordinal() <= FunctionType.PPI.ordinal()) {
      return 0;
    }
    else {
      int ret = Exprent.MULTIPLE_USES | Exprent.SIDE_EFFECTS_FREE;
      for (Exprent expr : lstOperands) {
        ret &= expr.getExprentUse();
      }
      return ret;
    }
  }

  @Override
  public CheckTypesResult checkExprTypeBounds() {
    CheckTypesResult result = new CheckTypesResult();

    Exprent param1 = lstOperands.get(0);
    VarType type1 = param1.getExprType();
    Exprent param2 = null;
    VarType type2 = null;

    if (lstOperands.size() > 1) {
      param2 = lstOperands.get(1);
      type2 = param2.getExprType();
    }

    switch (funcType) {
      case TERNARY:
        VarType supertype = getExprType();
        result.addMinTypeExprent(param1, VarType.VARTYPE_BOOLEAN);
        result.addMinTypeExprent(param2, VarType.getMinTypeInFamily(supertype.typeFamily));
        result.addMinTypeExprent(lstOperands.get(2), VarType.getMinTypeInFamily(supertype.typeFamily));
        break;
      case I2L:
      case I2F:
      case I2D:
      case I2B:
      case I2C:
      case I2S:
        result.addMinTypeExprent(param1, VarType.VARTYPE_BYTECHAR);
        result.addMaxTypeExprent(param1, VarType.VARTYPE_INT);
        break;
      case IMM:
      case IPP:
      case MMI:
      case PPI:
        result.addMinTypeExprent(param1, implicitType);
        result.addMaxTypeExprent(param1, implicitType);
        break;
      case ADD:
      case SUB:
      case MUL:
      case DIV:
      case REM:
      case SHL:
      case SHR:
      case USHR:
      case LT:
      case GE:
      case GT:
      case LE:
        result.addMinTypeExprent(param2, VarType.VARTYPE_BYTECHAR);
      case BIT_NOT:
        // case BOOL_NOT:
      case NEG:
        result.addMinTypeExprent(param1, VarType.VARTYPE_BYTECHAR);
        break;
      case AND:
      case OR:
      case XOR:
      case EQ:
      case NE: {
        if (type1.type == CodeConstants.TYPE_BOOLEAN) {
          if (type2.isStrictSuperset(type1)) {
            result.addMinTypeExprent(param1, VarType.VARTYPE_BYTECHAR);
          }
          else { // both are booleans
            boolean param1_false_boolean =
              type1.isFalseBoolean() || (param1 instanceof ConstExprent && !((ConstExprent)param1).hasBooleanValue());
            boolean param2_false_boolean =
              type1.isFalseBoolean() || (param2 instanceof ConstExprent && !((ConstExprent)param2).hasBooleanValue());

            if (param1_false_boolean || param2_false_boolean) {
              result.addMinTypeExprent(param1, VarType.VARTYPE_BYTECHAR);
              result.addMinTypeExprent(param2, VarType.VARTYPE_BYTECHAR);
            }
          }
        }
        else if (type2.type == CodeConstants.TYPE_BOOLEAN) {
          if (type1.isStrictSuperset(type2)) {
            result.addMinTypeExprent(param2, VarType.VARTYPE_BYTECHAR);
          }
        }
      }
    }

    return result;
  }

  @Override
  public List<Exprent> getAllExprents(List<Exprent> lst) {
    lst.addAll(this.lstOperands);
    return lst;
  }

  @Override
  public Exprent copy() {
    List<Exprent> lst = new ArrayList<>();
    for (Exprent expr : lstOperands) {
      lst.add(expr.copy());
    }
    FunctionExprent func = new FunctionExprent(funcType, lst, bytecode);
    func.setImplicitType(implicitType);

    return func;
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) return true;
    if (!(o instanceof FunctionExprent)) return false;

    FunctionExprent fe = (FunctionExprent)o;
    return funcType == fe.getFuncType() &&
           InterpreterUtil.equalLists(lstOperands, fe.getLstOperands()); // TODO: order of operands insignificant
  }

  @Override
  public void replaceExprent(Exprent oldExpr, Exprent newExpr) {
    for (int i = 0; i < lstOperands.size(); i++) {
      if (oldExpr == lstOperands.get(i)) {
        lstOperands.set(i, newExpr);
      }
    }
  }

  @Override
  public TextBuffer toJava(int indent) {
    TextBuffer buf = new TextBuffer();
    buf.addBytecodeMapping(bytecode);

    // If we're an unsigned right shift or lower, this function can be represented as a single leftHand + functionType + rightHand operation.
    if (this.funcType.isArithmeticBinaryOperation()) {
      Exprent left = this.lstOperands.get(0);
      Exprent right = this.lstOperands.get(1);

      // Minecraft specific hot fix: If we're doing arithmetic or bitwise math by a char value, we can assume that it's wrong behavior.
      // We check for this and then fix it by resetting the param to be an int instead of a char.
      // This fixes cases where "& 65535" and "& 0xFFFF" get wrongly decompiled as "& '\uffff'".
      if (this.funcType.isArithmeticBinaryOperation()) {
        // Checks to see if the right expression is a constant and then adjust the type from char to int if the left is an int.
        // Failing that, check the left hand side and then do the same.
        if (right instanceof ConstExprent) {
          ((ConstExprent) right).adjustConstType(left.getExprType());
        } else if (left instanceof ConstExprent) {
          ((ConstExprent) left).adjustConstType(right.getExprType());
        }
      }

      // Initialize the operands with the defaults
      TextBuffer leftOperand = wrapOperandString(left, false, indent, true);
      TextBuffer rightOperand = wrapOperandString(right, true, indent, true);

      // Check for special cased integers on the right and left hand side, and then return if they are found.
      // This only applies to bitwise and as well as bitwise or functions.
      if (this.funcType == FunctionType.AND || this.funcType == FunctionType.OR) {
        // Check if the right is an int constant and adjust accordingly
        if (right instanceof ConstExprent && right.getExprType() == VarType.VARTYPE_INT) {
          Integer value = (Integer) ((ConstExprent)right).getValue();
          rightOperand.setLength(0);
          rightOperand.append(IntHelper.adjustedIntRepresentation(value));
        }

        // Check if the left is an int constant and adjust accordingly
        if (left instanceof ConstExprent && left.getExprType() == VarType.VARTYPE_INT) {
          Integer value = (Integer) ((ConstExprent)left).getValue();
          leftOperand.setLength(0);
          leftOperand.append(IntHelper.adjustedIntRepresentation(value));
        }
      }

      // Return the applied operands and operators.
      if (!disableNewlineGroupCreation) {
        buf.pushNewlineGroup(indent, 1);
      }
      buf.append(leftOperand)
        .appendPossibleNewline(" ").append(funcType.operator).append(" ")
        .append(rightOperand);
      if (!disableNewlineGroupCreation) {
        buf.popNewlineGroup();
      }

      return buf;
    }

      // try to determine more accurate type for 'char' literals
    if (funcType.ordinal() >= FunctionType.EQ.ordinal()) {
      Exprent left = lstOperands.get(0);
      Exprent right = lstOperands.get(1);

      if (funcType.ordinal() <= FunctionType.LE.ordinal()) {
        if (right instanceof ConstExprent) {
          ((ConstExprent) right).adjustConstType(left.getExprType());
        }
        else if (left instanceof ConstExprent) {
          ((ConstExprent) left).adjustConstType(right.getExprType());
        }
      }

      if (!disableNewlineGroupCreation) {
        buf.pushNewlineGroup(indent, 1);
      }
      buf.append(wrapOperandString(left, false, indent, true))
        .appendPossibleNewline(" ").append(funcType.operator).append(" ")
        .append(wrapOperandString(right, true, indent, true));
      if (!disableNewlineGroupCreation) {
        buf.popNewlineGroup();
      }
      return buf;
    }

    switch (funcType) {
      case BIT_NOT:
      case BOOL_NOT:
      case NEG:
      case MMI:
      case PPI:
        return buf.append(wrapOperandString(lstOperands.get(0), true, indent).prepend(funcType.operator));
      case IPP:
      case IMM:
        return buf.append(wrapOperandString(lstOperands.get(0), true, indent).append(funcType.operator));
      case CAST:
        if (!needsCast) {
          return buf.append(lstOperands.get(0).toJava(indent));
        }
        return buf.append(lstOperands.get(1).toJava(indent)).encloseWithParens().append(wrapOperandString(lstOperands.get(0), true, indent));
      case ARRAY_LENGTH:
        Exprent arr = lstOperands.get(0);

        buf.append(wrapOperandString(arr, false, indent));
        if (arr.getExprType().arrayDim == 0) {
          VarType objArr = VarType.VARTYPE_OBJECT.resizeArrayDim(1); // type family does not change
          buf.enclose("((" + ExprProcessor.getCastTypeName(objArr) + ")", ")");
        }
        return buf.append(".length");
      case TERNARY:
        buf.pushNewlineGroup(indent, 1);
        buf.append(wrapOperandString(lstOperands.get(0), true, indent))
          .appendPossibleNewline(" ").append("? ")
          .append(wrapOperandString(lstOperands.get(1), true, indent))
          .appendPossibleNewline(" ").append(": ")
          .append(wrapOperandString(lstOperands.get(2), true, indent));
        buf.popNewlineGroup();
        return buf;
      case INSTANCEOF:
        buf.append(wrapOperandString(lstOperands.get(0), true, indent)).append(" instanceof ").append(wrapOperandString(lstOperands.get(1), true, indent));

        if (this.lstOperands.size() > 2) {
          // Pattern instanceof creation- only happens when we have more than 2 exprents
          buf.append(" ");
          ((VarExprent)this.lstOperands.get(2)).setDefinition(false);
          buf.append(wrapOperandString(this.lstOperands.get(2), true, indent));
        }
        return buf;
      case LCMP:
      case FCMPL:
      case FCMPG:
      case DCMPL:
      case DCMPG:
        // shouldn't appear in the final code
        return buf.append(wrapOperandString(lstOperands.get(0), true, indent).prepend(funcType.operator + "("))
                 .append(", ")
                 .append(wrapOperandString(lstOperands.get(1), true, indent))
                 .append(")");
    }

    if (funcType.castType != null) {
      // We can't directly cast some object types, so we need to make sure the unboxing happens.
      // The types seem to be inconsistant but there is no harm in forcing the unboxing when not strictly needed.
      // Type   | Works | Doesn't
      // Integer| LFD   | BCS
      // Long   | FD    | I
      // Float  | D     | IL
      // Double |       | ILF
      if (lstOperands.get(0) instanceof InvocationExprent) {
        InvocationExprent inv = (InvocationExprent)lstOperands.get(0);
        if (inv.isUnboxingCall()) {
          inv.forceUnboxing(true);
        }
      }
      return buf.append(wrapOperandString(lstOperands.get(0), true, indent))
                .prepend("(" + ExprProcessor.getTypeName(funcType.castType) + ")");
    }

    //        return "<unknown function>";
    throw new RuntimeException("invalid function");
  }

  // Make sure that any boxing that is required is properly expressed
  private Exprent unwrapBoxing(Exprent expr) {
    if (expr instanceof InvocationExprent) {
      if (((InvocationExprent) expr).isUnboxingCall()) {
        Exprent inner = ((InvocationExprent) expr).getInstance();
        if (inner instanceof FunctionExprent && ((FunctionExprent)inner).funcType == FunctionType.CAST) {
          inner.addBytecodeOffsets(expr.bytecode);
          expr = inner;
        }
      }
    }

    return expr;
  }

  public void unwrapBox() {
    for (int i = 0; i < this.lstOperands.size(); i++) {
      this.lstOperands.set(i, unwrapBoxing(this.lstOperands.get(i)));
    }
  }

  @Override
  public int getPrecedence() {
    if (funcType == FunctionType.CAST && !doesCast()) {
      return lstOperands.get(0).getPrecedence();
    }
    return funcType.precedence;
  }

  public VarType getSimpleCastType() {
    return funcType.castType;
  }

  private TextBuffer wrapOperandString(Exprent expr, boolean eq, int indent) {
    return wrapOperandString(expr, eq, indent, false);
  }

  private TextBuffer wrapOperandString(Exprent expr, boolean eq, int indent, boolean newlineGroup) {
    int myprec = getPrecedence();
    int exprprec = expr.getPrecedence();

    boolean parentheses = exprprec > myprec;
    if (!parentheses && eq) {
      parentheses = (exprprec == myprec);
      if (parentheses) {
        if (expr instanceof FunctionExprent &&
            ((FunctionExprent)expr).getFuncType() == funcType) {
          parentheses = !ASSOCIATIVITY.contains(funcType);
        }
      }
    }

    if (newlineGroup && !parentheses && myprec == exprprec) {
      if (expr instanceof FunctionExprent) {
        FunctionExprent funcExpr = (FunctionExprent) expr;
        if (funcExpr.getFuncType() == FunctionType.CAST && !funcExpr.doesCast()) {
          Exprent subExpr = funcExpr.getLstOperands().get(0);
          if (subExpr instanceof FunctionExprent) {
            funcExpr = (FunctionExprent) subExpr;
          }
        }
        funcExpr.disableNewlineGroupCreation = true;
      }
    }

    TextBuffer res = expr.toJava(indent);

    if (parentheses) {
      TextBuffer oldRes = res;
      res = new TextBuffer().append("(");
      res.pushNewlineGroup(indent, 1);
      res.appendPossibleNewline();
      res.append(oldRes);
      res.appendPossibleNewline("", true);
      res.popNewlineGroup();
      res.append(")");
    }

    return res;
  }

  private static VarType getMaxVarType(VarType ...arr) {
    for (int i = 0; i < TYPE_PRIMITIVES.length; i++) {
      for (VarType anArr : arr) {
        if (anArr.type == TYPE_PRIMITIVES[i]) {
          return TYPES[i];
        }
      }
    }

    return VarType.VARTYPE_INT;
  }

  // *****************************************************************************
  // getter and setter methods
  // *****************************************************************************

  public FunctionType getFuncType() {
    return funcType;
  }

  public void setFuncType(FunctionType funcType) {
    this.funcType = funcType;
  }

  public List<Exprent> getLstOperands() {
    return lstOperands;
  }

  public void setImplicitType(VarType implicitType) {
    this.implicitType = implicitType;
  }

  public boolean doesCast() {
    return needsCast;
  }

  public void setNeedsCast(boolean needsCast) {
    this.needsCast = needsCast;
  }

  public void setInvocationInstance() {
    if (funcType == FunctionType.CAST) {
      lstOperands.get(0).setInvocationInstance();
    }
  }

  @Override
  public void setIsQualifier() {
    if (funcType == FunctionType.CAST && !doesCast()) {
      lstOperands.get(0).setIsQualifier();
    }
  }

  @Override
  public boolean allowNewlineAfterQualifier() {
    if (funcType == FunctionType.CAST && !doesCast()) {
      return lstOperands.get(0).allowNewlineAfterQualifier();
    }
    return super.allowNewlineAfterQualifier();
  }

  @Override
  public void getBytecodeRange(BitSet values) {
    measureBytecode(values, lstOperands);
    measureBytecode(values);
  }

  // *****************************************************************************
  // IMatchable implementation
  // *****************************************************************************

  @Override
  public boolean match(MatchNode matchNode, MatchEngine engine) {
    if (!super.match(matchNode, engine)) {
      return false;
    }

    FunctionType type = (FunctionType)matchNode.getRuleValue(MatchProperties.EXPRENT_FUNCTYPE);
    return type == null || this.funcType == type;
  }
}
