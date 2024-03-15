// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.java.decompiler.modules.decompiler;

import org.jetbrains.java.decompiler.code.CodeConstants;
import org.jetbrains.java.decompiler.main.DecompilerContext;
import org.jetbrains.java.decompiler.main.collectors.CounterContainer;
import org.jetbrains.java.decompiler.main.extern.IFernflowerPreferences;
import org.jetbrains.java.decompiler.modules.decompiler.exps.*;
import org.jetbrains.java.decompiler.modules.decompiler.exps.FunctionExprent.FunctionType;
import org.jetbrains.java.decompiler.modules.decompiler.stats.IfStatement;
import org.jetbrains.java.decompiler.modules.decompiler.stats.Statement;
import org.jetbrains.java.decompiler.modules.decompiler.vars.VarProcessor;
import org.jetbrains.java.decompiler.modules.decompiler.vars.VarVersionPair;
import org.jetbrains.java.decompiler.struct.gen.VarType;

import java.util.*;

public final class SecondaryFunctionsHelper {

  private static final Map<FunctionType, FunctionType> funcsnot = new HashMap<>();
  static {
    funcsnot.put(FunctionType.EQ, FunctionType.NE);
    funcsnot.put(FunctionType.NE, FunctionType.EQ);
    funcsnot.put(FunctionType.LT, FunctionType.GE);
    funcsnot.put(FunctionType.GE, FunctionType.LT);
    funcsnot.put(FunctionType.GT, FunctionType.LE);
    funcsnot.put(FunctionType.LE, FunctionType.GT);
    funcsnot.put(FunctionType.BOOLEAN_AND, FunctionType.BOOLEAN_OR);
    funcsnot.put(FunctionType.BOOLEAN_OR, FunctionType.BOOLEAN_AND);
  }

  private static final HashMap<FunctionType, FunctionType[]> mapNumComparisons = new HashMap<>();

  static {
    mapNumComparisons.put(FunctionType.EQ, new FunctionType[]{FunctionType.LT, FunctionType.EQ, FunctionType.GT});
    mapNumComparisons.put(FunctionType.NE, new FunctionType[]{FunctionType.GE, FunctionType.NE, FunctionType.LE});
    mapNumComparisons.put(FunctionType.GT, new FunctionType[]{FunctionType.GE, FunctionType.GT, null});
    mapNumComparisons.put(FunctionType.GE, new FunctionType[]{null, FunctionType.GE, FunctionType.GT});
    mapNumComparisons.put(FunctionType.LT, new FunctionType[]{null, FunctionType.LT, FunctionType.LE});
    mapNumComparisons.put(FunctionType.LE, new FunctionType[]{FunctionType.LT, FunctionType.LE, null});
  }

  public static boolean identifySecondaryFunctions(Statement stat, VarProcessor varProc) {
    if (stat.getExprents() == null) {
      // if(){;}else{...} -> if(!){...}
      if (stat instanceof IfStatement) {
        IfStatement ifelsestat = (IfStatement)stat;
        Statement ifstat = ifelsestat.getIfstat();

        if (ifelsestat.iftype == IfStatement.IFTYPE_IFELSE && ifstat.getExprents() != null &&
            ifstat.getExprents().isEmpty() && (ifstat.getAllSuccessorEdges().isEmpty() || !ifstat.getFirstSuccessor().explicit)) {

          // move else to the if position
          ifelsestat.getStats().removeWithKey(ifstat.id);

          ifelsestat.iftype = IfStatement.IFTYPE_IF;
          ifelsestat.setIfstat(ifelsestat.getElsestat());
          ifelsestat.setElsestat(null);

          if (ifelsestat.getAllSuccessorEdges().isEmpty() && !ifstat.getAllSuccessorEdges().isEmpty()) {
            StatEdge endedge = ifstat.getFirstSuccessor();

            ifstat.removeSuccessor(endedge);
            endedge.setSource(ifelsestat);
            if (endedge.closure != null) {
              ifelsestat.getParent().addLabeledEdge(endedge);
            }
            ifelsestat.addSuccessor(endedge);
          }

          ifelsestat.getFirst().removeSuccessor(ifelsestat.getIfEdge());

          ifelsestat.setIfEdge(ifelsestat.getElseEdge());
          ifelsestat.setElseEdge(null);

          // negate head expression
          ifelsestat.setNegated(!ifelsestat.isNegated());
          ifelsestat.getHeadexprentList().set(0, ((IfExprent)ifelsestat.getHeadexprent().copy()).negateIf());

          return true;
        } else if (ifelsestat.iftype == IfStatement.IFTYPE_IF && ifstat != null && ifstat.getExprents() != null &&
          ifstat.getExprents().isEmpty() && (ifstat.hasAnySuccessor() && ifstat.getFirstSuccessor().getType() == StatEdge.TYPE_FINALLYEXIT)) {

          // Inlining blocks into finally statements will cause some if statements to have inconsistent semantics.
          // A block with a finallyexit can be inlined via InlineSingleBlocks to where a break once was, making improper control flow
          // e.g.
          // if (!<cond>) {
          //   ;
          // }
          // break;
          //
          // becomes
          //
          // if (<cond>) {
          //   break;
          // }
          // When inside a finally
          // FIXME: fix the underlying issue here
          // see also: TestLoopFinally


          if (ifelsestat.hasSuccessor(StatEdge.TYPE_BREAK)) {

            SequenceHelper.destroyStatementContent(ifstat, true);
            ifelsestat.setIfstat(null);

            StatEdge edge = ifelsestat.getSuccessorEdges(StatEdge.TYPE_BREAK).get(0);

            edge.changeSource(ifelsestat.getFirst());

            ifelsestat.setIfEdge(edge);

            // negate head expression
            ifelsestat.setNegated(!ifelsestat.isNegated());
            ifelsestat.getHeadexprentList().set(0, ((IfExprent)ifelsestat.getHeadexprent().copy()).negateIf());
          }

        }
      }
    }

    boolean ret = false;
    boolean replaced = true;
    while (replaced) {
      replaced = false;

      List<Object> lstObjects = new ArrayList<>(stat.getExprents() == null ? stat.getSequentialObjects() : stat.getExprents());

      for (int i = 0; i < lstObjects.size(); i++) {
        Object obj = lstObjects.get(i);

        if (obj instanceof Statement) {
          if (identifySecondaryFunctions((Statement)obj, varProc)) {
            ret = true;
            replaced = true;
            break;
          }
        }
        else if (obj instanceof Exprent) {
          Exprent retexpr = identifySecondaryFunctions((Exprent)obj, true, varProc);
          if (retexpr != null) {
            if (stat.getExprents() == null) {
              // only head expressions can be replaced!
              stat.replaceExprent((Exprent)obj, retexpr);
            }
            else {
              stat.getExprents().set(i, retexpr);
            }
            ret = true;
            replaced = true;
            break;
          }
        }
      }
    }

    return ret;
  }

  private static Exprent identifySecondaryFunctions(Exprent exprent, boolean statement_level, VarProcessor varProc) {
    if (exprent instanceof FunctionExprent) {
      FunctionExprent fexpr = (FunctionExprent)exprent;

      switch (fexpr.getFuncType()) {
        case BOOL_NOT:

          Exprent retparam = propagateBoolNot(fexpr);

          if (retparam != null) {
            return retparam;
          }

          break;
        case EQ:
        case NE:
        case GT:
        case GE:
        case LT:
        case LE:
          Exprent expr1 = fexpr.getLstOperands().get(0);
          Exprent expr2 = fexpr.getLstOperands().get(1);

          if (expr1 instanceof ConstExprent) {
            expr2 = expr1;
            expr1 = fexpr.getLstOperands().get(1);
          }

          if (expr1 instanceof FunctionExprent && expr2 instanceof ConstExprent) {
            FunctionExprent funcexpr = (FunctionExprent)expr1;
            ConstExprent cexpr = (ConstExprent)expr2;

            FunctionType functype = funcexpr.getFuncType();
            if (functype == FunctionType.LCMP || functype == FunctionType.FCMPG ||
                functype == FunctionType.FCMPL || functype == FunctionType.DCMPG ||
                functype == FunctionType.DCMPL) {

              FunctionType desttype = null;

              FunctionType[] destcons = mapNumComparisons.get(fexpr.getFuncType());
              if (destcons != null) {
                int index = cexpr.getIntValue() + 1;
                if (index >= 0 && index <= 2) {
                  FunctionType destcon = destcons[index];
                  if (destcon != null) {
                    desttype = destcon;
                  }
                }
              }

              if (desttype != null) {
                if (functype != FunctionType.LCMP) {
                  boolean oneForNan = functype == FunctionType.DCMPL || functype == FunctionType.FCMPL;
                  boolean trueForOne = desttype == FunctionType.LT || desttype == FunctionType.LE;
                  boolean trueForNan = oneForNan == trueForOne;
                  if (trueForNan) {
                    List<Exprent> operands = new ArrayList<>();
                    operands.add(new FunctionExprent(funcsnot.get(desttype), funcexpr.getLstOperands(), funcexpr.bytecode));
                    return new FunctionExprent(FunctionType.BOOL_NOT, operands, funcexpr.bytecode);
                  }
                }
                return new FunctionExprent(desttype, funcexpr.getLstOperands(), funcexpr.bytecode);
              }
            }
          }
      }
    }


    boolean replaced = true;
    while (replaced) {
      replaced = false;

      for (Exprent expr : exprent.getAllExprents()) {
        Exprent retexpr = identifySecondaryFunctions(expr, false, varProc);
        if (retexpr != null) {
          exprent.replaceExprent(expr, retexpr);
          retexpr.addBytecodeOffsets(expr.bytecode);
          replaced = true;
          break;
        }
      }
    }

    switch (exprent.type) {
      case FUNCTION:
        FunctionExprent fexpr = (FunctionExprent)exprent;
        List<Exprent> lstOperands = fexpr.getLstOperands();

        switch (fexpr.getFuncType()) {
          case XOR:
            for (int i = 0; i < 2; i++) {
              Exprent operand = lstOperands.get(i);
              VarType operandtype = operand.getExprType();

              if (operand instanceof ConstExprent &&
                  operandtype.type != CodeConstants.TYPE_BOOLEAN) {
                ConstExprent cexpr = (ConstExprent)operand;
                long val;
                if (operandtype.type == CodeConstants.TYPE_LONG) {
                  val = (Long)cexpr.getValue();
                }
                else {
                  val = (Integer)cexpr.getValue();
                }

                if (val == -1) {
                  List<Exprent> lstBitNotOperand = new ArrayList<>();
                  lstBitNotOperand.add(lstOperands.get(1 - i));
                  return new FunctionExprent(FunctionType.BIT_NOT, lstBitNotOperand, fexpr.bytecode);
                }
              }
            }
            break;
          case EQ:
          case NE:
            if (lstOperands.get(0).getExprType().type == CodeConstants.TYPE_BOOLEAN &&
                lstOperands.get(1).getExprType().type == CodeConstants.TYPE_BOOLEAN) {
              for (int i = 0; i < 2; i++) {
                if (lstOperands.get(i) instanceof ConstExprent) {
                  ConstExprent cexpr = (ConstExprent)lstOperands.get(i);
                  int val = (Integer)cexpr.getValue();

                  if ((fexpr.getFuncType() == FunctionType.EQ && val == 1) ||
                      (fexpr.getFuncType() == FunctionType.NE && val == 0)) {
                    return lstOperands.get(1 - i);
                  }
                  else {
                    List<Exprent> lstNotOperand = new ArrayList<>();
                    lstNotOperand.add(lstOperands.get(1 - i));
                    return new FunctionExprent(FunctionType.BOOL_NOT, lstNotOperand, fexpr.bytecode);
                  }
                }
              }
            }
            break;
          case BOOL_NOT:
            if (lstOperands.get(0) instanceof ConstExprent) {
              int val = ((ConstExprent)lstOperands.get(0)).getIntValue();
              if (val == 0) {
                return new ConstExprent(VarType.VARTYPE_BOOLEAN, 1, fexpr.bytecode);
              }
              else {
                return new ConstExprent(VarType.VARTYPE_BOOLEAN, 0, fexpr.bytecode);
              }
            }
            break;
          case TERNARY:
            Exprent expr0 = lstOperands.get(0);
            Exprent expr1 = lstOperands.get(1);
            Exprent expr2 = lstOperands.get(2);

            if (expr1 instanceof ConstExprent && expr2 instanceof ConstExprent) {
              ConstExprent cexpr1 = (ConstExprent)expr1;
              ConstExprent cexpr2 = (ConstExprent)expr2;

              if (cexpr1.getExprType().type == CodeConstants.TYPE_BOOLEAN &&
                  cexpr2.getExprType().type == CodeConstants.TYPE_BOOLEAN) {

                if (cexpr1.getIntValue() == 0 && cexpr2.getIntValue() != 0) {
                  return new FunctionExprent(FunctionType.BOOL_NOT, lstOperands.get(0), fexpr.bytecode);
                }
                else if (cexpr1.getIntValue() != 0 && cexpr2.getIntValue() == 0) {
                  return lstOperands.get(0);
                }
              }
            }
            break;
          case LCMP:
          case FCMPL:
          case FCMPG:
          case DCMPL:
          case DCMPG:
            int var = DecompilerContext.getCounterContainer().getCounterAndIncrement(CounterContainer.VAR_COUNTER);
            VarType type = lstOperands.get(0).getExprType();

            FunctionExprent iff = new FunctionExprent(FunctionType.TERNARY, Arrays.asList(
              new FunctionExprent(FunctionType.LT, Arrays.asList(new VarExprent(var, type, varProc),
                ConstExprent.getZeroConstant(type.type)), null),
              new ConstExprent(VarType.VARTYPE_INT, -1, null),
              new ConstExprent(VarType.VARTYPE_INT, 1, null)), null);

            FunctionExprent head = new FunctionExprent(FunctionType.EQ, Arrays.asList(
              new AssignmentExprent(new VarExprent(var, type, varProc),
                                    new FunctionExprent(FunctionType.SUB, Arrays.asList(lstOperands.get(0), lstOperands.get(1)), null),
                                    null),
              ConstExprent.getZeroConstant(type.type)), null);

            varProc.setVarType(new VarVersionPair(var, 0), type);

            return new FunctionExprent(FunctionType.TERNARY, Arrays.asList(
              head, new ConstExprent(VarType.VARTYPE_INT, 0, null), iff), fexpr.bytecode);
        }
        break;
      case ASSIGNMENT: // check for conditional assignment
        AssignmentExprent asexpr = (AssignmentExprent)exprent;

        if(asexpr.getCondType() != null)
          return null;

        Exprent right = asexpr.getRight();
        Exprent left = asexpr.getLeft();

        if (right instanceof FunctionExprent) {
          FunctionExprent func = (FunctionExprent)right;

          VarType midlayer = null;
          if (func.getFuncType().castType != null) {
            right = func.getLstOperands().get(0);
            midlayer = func.getSimpleCastType();
            if (right instanceof FunctionExprent) {
              func = (FunctionExprent)right;
            }
            else {
              return null;
            }
          }

          List<Exprent> lstFuncOperands = func.getLstOperands();

          Exprent cond = null;

          switch (func.getFuncType()) {
            case ADD:
            case AND:
            case OR:
            case XOR:
              if (left.equals(lstFuncOperands.get(1))) {
                cond = lstFuncOperands.get(0);
                break;
              }
            case SUB:
            case MUL:
            case DIV:
            case REM:
            case SHL:
            case SHR:
            case USHR:
              if (left.equals(lstFuncOperands.get(0))) {
                cond = lstFuncOperands.get(1);
              }
          }

          if (cond != null && (midlayer == null || midlayer.equals(cond.getExprType()))) {
            asexpr.setRight(cond);
            asexpr.setCondType(func.getFuncType());
          }
        }
        break;
      case INVOCATION:
        if (!statement_level) { // simplify if exprent is a real expression. The opposite case is pretty absurd, can still happen however (and happened at least once).
          Exprent retexpr = ConcatenationHelper.contractStringConcat(exprent);
          if (!exprent.equals(retexpr)) {
            return retexpr;
          }
        }
    }

    return null;
  }

  public static Exprent propagateBoolNot(Exprent exprent) {

    if (exprent instanceof FunctionExprent) {
      FunctionExprent fexpr = (FunctionExprent)exprent;

      if (fexpr.getFuncType() == FunctionType.BOOL_NOT) {

        Exprent param = fexpr.getLstOperands().get(0);

        if (param instanceof FunctionExprent) {
          FunctionExprent fparam = (FunctionExprent)param;

          FunctionType ftype = fparam.getFuncType();
          boolean canSimplify = false;
          switch (ftype) {
            case BOOL_NOT:
              Exprent newexpr = fparam.getLstOperands().get(0);
              Exprent retexpr = propagateBoolNot(newexpr);
              return retexpr == null ? newexpr : retexpr;
            case TERNARY:
              // Wrap branches
              FunctionExprent fex1 = new FunctionExprent(FunctionType.BOOL_NOT, fparam.getLstOperands().get(1), null);
              FunctionExprent fex2 = new FunctionExprent(FunctionType.BOOL_NOT, fparam.getLstOperands().get(2), null);

              // Propagate both branches
              Exprent ex1 = propagateBoolNot(fex1);
              Exprent ex2 = propagateBoolNot(fex2);

              // Set both branches to new version if it was created, or old if it wasn't
              fparam.getLstOperands().set(1, ex1 == null ? fex1 : ex1);
              fparam.getLstOperands().set(2, ex2 == null ? fex2 : ex2);

              return fparam;
            case BOOLEAN_AND:
            case BOOLEAN_OR:
              List<Exprent> operands = fparam.getLstOperands();
              for (int i = 0; i < operands.size(); i++) {
                Exprent newparam = new FunctionExprent(FunctionType.BOOL_NOT, operands.get(i), operands.get(i).bytecode);

                Exprent retparam = propagateBoolNot(newparam);
                operands.set(i, retparam == null ? newparam : retparam);
              }
            case EQ:
            case NE:
              canSimplify = true;
            case LT:
            case GE:
            case GT:
            case LE:
              if (!canSimplify) {
                operands = fparam.getLstOperands();
                VarType left = operands.get(0).getExprType();
                VarType right = operands.get(1).getExprType();
                VarType commonSupertype = VarType.getCommonSupertype(left, right);
                if (commonSupertype != null) {
                  canSimplify = commonSupertype.type != CodeConstants.TYPE_FLOAT && commonSupertype.type != CodeConstants.TYPE_DOUBLE;
                }
              }
              if (canSimplify) {
                fparam.setFuncType(funcsnot.get(ftype));
                return fparam;
              }
          }
        }
      }
    }

    return null;
  }

  /**
   * Simplifies assignment exprents that can be represented as a compound assignment.
   * Example: "a = a + b" becomes "a += b"
   * Iterates recursively through every statement within the statement and all assignments possible.
   * See: <a href="https://docs.oracle.com/javase/specs/jls/se16/html/jls-15.html#jls-15.26.2">JLS-15.26.2</a>
   *
   * @param stat The provided statement
   */
  public static boolean updateAssignments(Statement stat) {
    boolean res = false;
    // Get all sequential objects if the statement doesn't have exprents
    List<Object> objects = new ArrayList<>(stat.getExprents() == null ? stat.getSequentialObjects() : stat.getExprents());

    for (Object obj : objects) {
      if (obj instanceof Statement) {
        // If the object is a statement, recurse
        res |= updateAssignments((Statement) obj);
      } else if (obj instanceof Exprent) {
        // If the statement is an exprent, start processing
        Exprent exprent = (Exprent) obj;

        if (exprent instanceof AssignmentExprent) {
          AssignmentExprent assignment = (AssignmentExprent) exprent;

          List<Exprent> params = exprent.getAllExprents();

          // Get params of the assignment exprent
          Exprent lhs = params.get(0);
          Exprent rhs = params.get(1);

          // We only want expressions that are standard assignments where the left hand side is a variable and the right hand side is a function.
          if (assignment.getCondType() == null && lhs instanceof VarExprent && rhs instanceof FunctionExprent) {
            VarExprent lhsVar = (VarExprent) lhs;
            FunctionExprent rhsFunc = (FunctionExprent) rhs;

            List<Exprent> funcParams = rhsFunc.getAllExprents();

            // Make sure that the function is a mathematical or bit shift function
            if (rhsFunc.getFuncType().isArithmeticBinaryOperation() && funcParams.get(0) instanceof VarExprent) {
              // Get the left hand side of the function
              VarExprent lhsVarFunc = (VarExprent) funcParams.get(0);

              // Check if the left hand side of the assignment and the left hand side of the function are the same variable
              // TODO: maybe we should be checking for var version equality too?
              if (lhsVar.getIndex() == lhsVarFunc.getIndex()) {
                // If all the checks succeed, set the assignment to be a compound assignment and set the right hand side to be the 2nd part of the function
                assignment.setCondType(rhsFunc.getFuncType());
                assignment.setRight(funcParams.get(1));
                // TODO: doesn't hit all instances, see ClientWorld

                res = true;
              }
            }
          }
        }
      }
    }

    return res;
  }
}
