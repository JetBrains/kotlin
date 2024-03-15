// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.java.decompiler.modules.decompiler;

import org.jetbrains.java.decompiler.code.CodeConstants;
import org.jetbrains.java.decompiler.main.ClassesProcessor.ClassNode;
import org.jetbrains.java.decompiler.main.DecompilerContext;
import org.jetbrains.java.decompiler.main.extern.IFernflowerPreferences;
import org.jetbrains.java.decompiler.main.rels.ClassWrapper;
import org.jetbrains.java.decompiler.modules.decompiler.exps.*;
import org.jetbrains.java.decompiler.modules.decompiler.exps.FunctionExprent.FunctionType;
import org.jetbrains.java.decompiler.modules.decompiler.sforms.SSAConstructorSparseEx;
import org.jetbrains.java.decompiler.modules.decompiler.stats.BasicBlockStatement;
import org.jetbrains.java.decompiler.modules.decompiler.stats.IfStatement;
import org.jetbrains.java.decompiler.modules.decompiler.stats.Statement;
import org.jetbrains.java.decompiler.modules.decompiler.vars.VarVersionPair;
import org.jetbrains.java.decompiler.struct.StructClass;
import org.jetbrains.java.decompiler.struct.gen.VarType;
import org.jetbrains.java.decompiler.struct.match.MatchEngine;
import org.jetbrains.java.decompiler.util.FastSparseSetFactory.FastSparseSet;
import org.jetbrains.java.decompiler.util.InterpreterUtil;
import org.jetbrains.java.decompiler.util.Pair;

import java.util.*;
import java.util.Map.Entry;

public class SimplifyExprentsHelper {
  @SuppressWarnings("SpellCheckingInspection")
  private static final MatchEngine class14Builder = new MatchEngine(
    "statement type:if iftype:if exprsize:-1\n" +
    " exprent position:head type:if\n" +
    "  exprent type:function functype:eq\n" +
    "   exprent type:field name:$fieldname$\n" +
    "   exprent type:constant consttype:null\n" +
    " statement type:basicblock\n" +
    "  exprent position:-1 type:assignment ret:$assignfield$\n" +
    "   exprent type:var index:$var$\n" +
    "   exprent type:field name:$fieldname$\n" +
    " statement type:sequence statsize:2\n" +
    "  statement type:trycatch\n" +
    "   statement type:basicblock exprsize:1\n" +
    "    exprent type:assignment\n" +
    "     exprent type:var index:$var$\n" +
    "     exprent type:invocation invclass:java/lang/Class signature:forName(Ljava/lang/String;)Ljava/lang/Class;\n" +
    "      exprent position:0 type:constant consttype:string constvalue:$classname$\n" +
    "   statement type:basicblock exprsize:1\n" +
    "    exprent type:exit exittype:throw\n" +
    "  statement type:basicblock exprsize:1\n" +
    "   exprent type:assignment\n" +
    "    exprent type:field name:$fieldname$ ret:$field$\n" +
    "    exprent type:var index:$var$");

  public static boolean simplifyStackVarsStatement(
    Statement stat,
    Set<Integer> setReorderedIfs,
    SSAConstructorSparseEx ssa,
    StructClass cl,
    boolean firstInvocation
  ) {
    boolean res = false;

    List<Exprent> expressions = stat.getExprents();
    if (expressions == null) {
      boolean processClass14 = DecompilerContext.getOption(IFernflowerPreferences.DECOMPILE_CLASS_1_4);

      while (true) {
        boolean changed = false;

        for (Statement st : stat.getStats()) {
          res |= simplifyStackVarsStatement(st, setReorderedIfs, ssa, cl, firstInvocation);

          changed = IfHelper.mergeIfs(st, setReorderedIfs) ||  // collapse composed if's
                    buildIff(st, ssa) ||  // collapse iff ?: statement
                    processClass14 && collapseInlinedClass14(st);  // collapse inlined .class property in version 1.4 and before

          if (changed) {
            break;
          }

          if (!st.getStats().isEmpty() && hasQualifiedNewGetClass(st, st.getStats().get(0))) {
            break;
          }
        }

        res |= changed;

        if (!changed) {
          break;
        }
      }
    } else {
      res = simplifyStackVarsExprents(expressions, cl, firstInvocation);
    }

    return res;
  }

  private static boolean simplifyStackVarsExprents(List<Exprent> list, StructClass cl, boolean firstInvocation) {
    boolean res = false;

    int index = 0;
    while (index < list.size()) {
      Exprent current = list.get(index);

      Exprent ret = isSimpleConstructorInvocation(current);
      if (ret != null) {
        list.set(index, ret);
        res = true;
        continue;
      }

      // lambda expression (Java 8)
      ret = isLambda(current, cl);
      if (ret != null) {
        list.set(index, ret);
        res = true;
        continue;
      }

      // remove monitor exit
      if (isMonitorExit(current)) {
        list.remove(index);
        res = true;
        continue;
      }

      // trivial assignment of a stack variable
      if (isTrivialStackAssignment(current)) {
        list.remove(index);
        res = true;
        continue;
      }

      if (index == list.size() - 1) {
        break;
      }

      Exprent next = list.get(index + 1);

      if (index > 0) {
        Exprent prev = list.get(index - 1);

        if (isSwapConstructorInvocation(prev, current, next)) {
          list.remove(index - 1);
          list.remove(index);
          res = true;
          continue;
        }
      }

      if (isAssignmentReturn(current, next)) {
        list.remove(index);
        res = true;
        continue;
      }

      if (isMethodArrayAssign(current, next)) {
        list.remove(index);
        res = true;
        continue;
      }

      // constructor invocation
      if (isConstructorInvocationRemote(list, index)) {
        list.remove(index);
        res = true;
        continue;
      }

      // remove getClass() invocation, which is part of a qualified new
      if (DecompilerContext.getOption(IFernflowerPreferences.REMOVE_GET_CLASS_NEW)) {
        if (isQualifiedNewGetClass(current, next)) {
          list.remove(index);
          res = true;
          continue;
        }
      }

      // direct initialization of an array
      int arrCount = isArrayInitializer(list, index);
      if (arrCount > 0) {
        for (int i = 0; i < arrCount; i++) {
          list.remove(index + 1);
        }
        res = true;
        continue;
      }

      // add array initializer expression
      if (addArrayInitializer(current, next)) {
        list.remove(index + 1);
        res = true;
        continue;
      }

      // integer ++expr and --expr  (except for vars!)
      Exprent func = isPPIorMMI(current);
      if (func != null) {
        list.set(index, func);
        res = true;
        continue;
      }

      // expr++ and expr--
      if (isIPPorIMM(current, next) || isIPPorIMM2(current, next)) {
        list.remove(index + 1);
        res = true;
        continue;
      }

      // assignment on stack
      if (isStackAssignment(current, next)) {
        list.remove(index + 1);
        res = true;
        continue;
      }

      if (!firstInvocation && isStackAssignment2(current, next)) {
        list.remove(index + 1);
        res = true;
        continue;
      }

      if (firstInvocation && inlinePPIAndMMI(current, next)) {
        list.remove(index);
        res = true;
        continue;
      }

      index++;
    }

    return res;
  }

  private static boolean addArrayInitializer(Exprent first, Exprent second) {
    if (first instanceof AssignmentExprent) {
      AssignmentExprent as = (AssignmentExprent) first;

      if (as.getRight() instanceof NewExprent && as.getLeft() instanceof VarExprent) {
        NewExprent newExpr = (NewExprent) as.getRight();

        if (!newExpr.getLstArrayElements().isEmpty()) {
          VarExprent arrVar = (VarExprent) as.getLeft();

          if (second instanceof AssignmentExprent) {
            AssignmentExprent aas = (AssignmentExprent) second;
            if (aas.getLeft() instanceof ArrayExprent) {
              ArrayExprent arrExpr = (ArrayExprent) aas.getLeft();
              if (arrExpr.getArray() instanceof VarExprent &&
                  arrVar.equals(arrExpr.getArray()) &&
                  arrExpr.getIndex() instanceof ConstExprent) {
                int constValue = ((ConstExprent) arrExpr.getIndex()).getIntValue();

                if (constValue < newExpr.getLstArrayElements().size()) {
                  Exprent init = newExpr.getLstArrayElements().get(constValue);
                  if (init instanceof ConstExprent) {
                    ConstExprent cinit = (ConstExprent) init;
                    VarType arrType = newExpr.getNewType().decreaseArrayDim();
                    ConstExprent defaultVal = ExprProcessor.getDefaultArrayValue(arrType);

                    if (cinit.equals(defaultVal)) {
                      Exprent tempExpr = aas.getRight();

                      if ((tempExpr.getExprentUse() & Exprent.SIDE_EFFECTS_FREE) == 0){
                        for (int i = constValue + 1; i < newExpr.getLstArrayElements().size(); i++) {
                          final Exprent exprent = newExpr.getLstArrayElements().get(i);
                          if ((exprent.getExprentUse() & Exprent.SIDE_EFFECTS_FREE) == 0){
                            // can't reorder non-side-effect free expressions
                            return false;
                          }
                        }
                      }

                      if (!tempExpr.containsExprent(arrVar)) {
                        newExpr.getLstArrayElements().set(constValue, tempExpr);

                        if (tempExpr instanceof NewExprent) {
                          NewExprent tempNewExpr = (NewExprent) tempExpr;
                          int dims = newExpr.getNewType().arrayDim;
                          if (dims > 1 && !tempNewExpr.getLstArrayElements().isEmpty()) {
                            tempNewExpr.setDirectArrayInit(true);
                          }
                        }

                        return true;
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }
    }

    return false;
  }

  private static int isArrayInitializer(List<Exprent> list, int index) {
    Exprent current = list.get(index);
    if (current instanceof AssignmentExprent) {
      AssignmentExprent as = (AssignmentExprent) current;

      if (as.getRight() instanceof NewExprent && as.getLeft() instanceof VarExprent) {
        NewExprent newExpr = (NewExprent) as.getRight();

        if (newExpr.getExprType().arrayDim > 0 && newExpr.getLstDims().size() == 1 && newExpr.getLstArrayElements().isEmpty() &&
            newExpr.getLstDims().get(0) instanceof ConstExprent) {

          int size = (Integer) ((ConstExprent) newExpr.getLstDims().get(0)).getValue();
          if (size == 0) {
            return 0;
          }

          VarExprent arrVar = (VarExprent) as.getLeft();
          Map<Integer, Exprent> mapInit = new HashMap<>();

          int i = 1;
          int lastImpure = -1;
          while (index + i < list.size() && i <= size) {
            Exprent expr = list.get(index + i);
            if (expr instanceof AssignmentExprent) {
              AssignmentExprent aas = (AssignmentExprent) expr;
              if (aas.getLeft() instanceof ArrayExprent) {
                ArrayExprent arrExpr = (ArrayExprent) aas.getLeft();
                if (arrExpr.getArray() instanceof VarExprent && arrVar.equals(arrExpr.getArray()) &&
                    arrExpr.getIndex() instanceof ConstExprent) {
                  // TODO: check for a number type. Failure extremely improbable, but nevertheless...
                  int constValue = ((ConstExprent) arrExpr.getIndex()).getIntValue();
                  if (constValue < size && !mapInit.containsKey(constValue)) {
                    if ((aas.getRight().getExprentUse() & Exprent.SIDE_EFFECTS_FREE) == 0) {
                      if(constValue < lastImpure) {
                        // can't reorder non-side-effect free expressions
                        break;
                      }
                      lastImpure = constValue;
                    }
                    if (!aas.getRight().containsExprent(arrVar)) {
                      mapInit.put(constValue, aas.getRight());
                      i++;
                      continue;
                    }
                  }
                }
              }
            }
            break;
          }

          double fraction = ((double) mapInit.size()) / size;

          if ((arrVar.isStack() && fraction > 0) || (size <= 7 && fraction >= 0.3) || (size > 7 && fraction >= 0.7)) {
            List<Exprent> lstRet = new ArrayList<>();

            VarType arrayType = newExpr.getNewType().decreaseArrayDim();
            ConstExprent defaultVal = ExprProcessor.getDefaultArrayValue(arrayType);
            for (int j = 0; j < size; j++) {
              lstRet.add(defaultVal.copy());
            }

            int dims = newExpr.getNewType().arrayDim;
            for (Entry<Integer, Exprent> ent : mapInit.entrySet()) {
              Exprent tempExpr = ent.getValue();
              lstRet.set(ent.getKey(), tempExpr);

              if (tempExpr instanceof NewExprent) {
                NewExprent tempNewExpr = (NewExprent) tempExpr;
                if (dims > 1 && !tempNewExpr.getLstArrayElements().isEmpty()) {
                  tempNewExpr.setDirectArrayInit(true);
                }
              }
            }

            newExpr.setLstArrayElements(lstRet);

            return mapInit.size();
          }
        }
      }
    }

    return 0;
  }

  /*
   * Check for the following pattern:
   * var1 = xxx;
   * return var1;
   * Where var1 is not a stack variable.
   * Turn it into:
   * return xxx;
   *
   * Note that this is transformation will result into java that is less like the original.
   * TODO: put this behind a compiler option.
   */
  private static boolean isAssignmentReturn(Exprent first, Exprent second) {
    //If assignment then exit.
    if (first instanceof AssignmentExprent
        && second instanceof ExitExprent) {
      AssignmentExprent assignment = (AssignmentExprent) first;
      ExitExprent exit = (ExitExprent) second;
      //if simple assign and exit is return and return isn't void
      if (assignment.getCondType() == null
          && exit.getExitType() == ExitExprent.Type.RETURN
          && exit.getValue() != null
          && assignment.getLeft() instanceof VarExprent
          && exit.getValue() instanceof VarExprent) {
        VarExprent assignmentLeft = (VarExprent) assignment.getLeft();
        VarExprent exitValue = (VarExprent) exit.getValue();
        //If the assignment before the return is immediately used in the return, inline it.
        if (assignmentLeft.equals(exitValue) && !assignmentLeft.isStack() && !exitValue.isStack()) {
          exit.replaceExprent(exitValue, assignment.getRight());
          return true;
        }
      }
    }
    return false;
  }

  /*
   * remove assignments of the form:
   * var10001 = var10001;
   */
  private static boolean isTrivialStackAssignment(Exprent first) {
    if (first instanceof AssignmentExprent) {
      AssignmentExprent asf = (AssignmentExprent) first;

      if (isStackVar(asf.getLeft()) && isStackVar(asf.getRight())) {
        VarExprent left = (VarExprent) asf.getLeft();
        VarExprent right = (VarExprent) asf.getRight();
        return left.getIndex() == right.getIndex();
      }
    }

    return false;
  }

  /*
   * Check for the following pattern:
   * var10001 = xxx;
   * yyy = var10001;
   * and replace it with:
   * var10001 = yyy = xxx;
   *
   * TODO: shouldn't this check if var10001 is used in `yyy`?
   */
  private static boolean isStackAssignment2(Exprent first, Exprent second) {  // e.g. 1.4-style class invocation
    if (first instanceof AssignmentExprent && second instanceof AssignmentExprent) {
      AssignmentExprent asf = (AssignmentExprent) first;
      AssignmentExprent ass = (AssignmentExprent) second;

      if (isStackVar(asf.getLeft()) && !isStackVar(ass.getLeft()) && asf.getLeft().equals(ass.getRight())) {
        asf.setRight(new AssignmentExprent(ass.getLeft(), asf.getRight(), ass.bytecode));
        return true;
      }
    }

    return false;
  }

  private static boolean isStackVar(Exprent exprent) {
    return exprent instanceof VarExprent && ((VarExprent) exprent).isStack();
  }

  /*
   * If the assignment is of the form:
   * var10001 = xxx;
   * c = xxx // where c IS NOT a stack variable (e.g. a local variable, or array element)
   * and c does not contain var10001, then var10001 is replaced by c, and the calling function
   * will remove the second assignment, essentially removing the first one.
   *
   * This is also applied to the case where the assignment is of the form:
   * a = var10001 = xxx;
   * c = xxx
   * into
   * a = c = xxx;
   * or
   * a = b = var10001 = xxx;
   * c = xxx
   * into
   * a = b = c = xxx;
   * This is also why it replaces the first assignment, and deleting the second, instead of
   * just deleting the second.
   */
  private static boolean isStackAssignment(Exprent first, Exprent second) {
    if (first instanceof AssignmentExprent && second instanceof AssignmentExprent) {
      AssignmentExprent asf = (AssignmentExprent) first;
      AssignmentExprent ass = (AssignmentExprent) second;

      while (true) {
        if (asf.getRight().equals(ass.getRight())) {
          if (isStackVar (asf.getLeft()) && !isStackVar(ass.getLeft())) {
            if (!ass.getLeft().containsExprent(asf.getLeft())) {
              asf.setRight(ass);
              return true;
            }
          }
        }
        if (asf.getRight() instanceof AssignmentExprent) {
          asf = (AssignmentExprent) asf.getRight();
        } else {
          break;
        }
      }
    }

    return false;
  }

  /*
   * Looking for the following pattern:
   * xxx = xxx + 1; // or xxx - 1, or 1 + xxx (in which case the arguments are swapped)
   * where xxx is not a var exprent
   */
  private static Exprent isPPIorMMI(Exprent first) {
    if (first instanceof AssignmentExprent) {
      AssignmentExprent as = (AssignmentExprent) first;

      if (as.getRight() instanceof FunctionExprent) {
        FunctionExprent func = (FunctionExprent) as.getRight();

        if (func.getFuncType() == FunctionType.ADD || func.getFuncType() == FunctionType.SUB) {
          Exprent econd = func.getLstOperands().get(0);
          Exprent econst = func.getLstOperands().get(1);

          if (!(econst instanceof ConstExprent) && econd instanceof ConstExprent &&
              func.getFuncType() == FunctionType.ADD) {
            econd = econst;
            econst = func.getLstOperands().get(0);
          }

          if (econst instanceof ConstExprent && ((ConstExprent) econst).hasValueOne()) {
            Exprent left = as.getLeft();

            if (!(left instanceof VarExprent) && left.equals(econd)) {
              FunctionType type = func.getFuncType() == FunctionType.ADD ? FunctionType.PPI : FunctionType.MMI;
              FunctionExprent ret = new FunctionExprent(type, econd, func.bytecode);
              ret.setImplicitType(VarType.VARTYPE_INT);
              return ret;
            }
          }
        }
      }
    }

    return null;
  }

  /*
   * Looking for the following pattern:
   * xxx = yyy
   * ++yyy; // or --yyy;
   * and turn it into:
   * xxx = yyy++; // or xxx = yyy--;
   */
  private static boolean isIPPorIMM(Exprent first, Exprent second) {
    if (first instanceof AssignmentExprent && second instanceof FunctionExprent) {
      AssignmentExprent as = (AssignmentExprent) first;
      FunctionExprent in = (FunctionExprent) second;

      if ((in.getFuncType() == FunctionType.MMI || in.getFuncType() == FunctionType.PPI) &&
          in.getLstOperands().get(0).equals(as.getRight())) {

        if (in.getFuncType() == FunctionType.MMI) {
          in.setFuncType(FunctionType.IMM);
        } else {
          in.setFuncType(FunctionType.IPP);
        }
        as.setRight(in);

        return true;
      }
    }

    return false;
  }

  /*
   * Looking for the following pattern:
   * xxx = yyy
   * yyy = xxx + 1; // or a - 1 or 1 + a
   * and xxx is used elsewhere
   * then turn it into:
   * xxx = yyy++;
   */
  private static boolean isIPPorIMM2(Exprent first, Exprent second) {
    if (!(first instanceof AssignmentExprent && second instanceof AssignmentExprent)) {
      return false;
    }

    AssignmentExprent af = (AssignmentExprent) first;
    AssignmentExprent as = (AssignmentExprent) second;

    if (!(as.getRight() instanceof FunctionExprent)) {
      return false;
    }

    FunctionExprent func = (FunctionExprent) as.getRight();

    if (func.getFuncType() != FunctionType.ADD && func.getFuncType() != FunctionType.SUB) {
      return false;
    }

    Exprent econd = func.getLstOperands().get(0);
    Exprent econst = func.getLstOperands().get(1);

    if (!(econst instanceof ConstExprent) && econd instanceof ConstExprent && func.getFuncType() == FunctionType.ADD) {
      econd = econst;
      econst = func.getLstOperands().get(0);
    }

    if (econst instanceof ConstExprent &&
        ((ConstExprent) econst).hasValueOne() &&
        af.getLeft().equals(econd) &&
        af.getRight().equals(as.getLeft()) &&
        (af.getLeft().getExprentUse() & Exprent.MULTIPLE_USES) != 0) {
      FunctionType type = func.getFuncType() == FunctionType.ADD ? FunctionType.IPP : FunctionType.IMM;

      FunctionExprent ret = new FunctionExprent(type, af.getRight(), func.bytecode);
      ret.setImplicitType(VarType.VARTYPE_INT);

      af.setRight(ret);
      return true;
    }

    return false;
  }


  // Inlines PPI into the next expression, to make stack var simplificiation easier
  //
  // ++i;
  // array[i] = 2;
  //
  // turns into
  //
  // array[++i] = 2;
  //
  // While this helps simplify stack vars, it also has can potentially make invalid code! When evaluating ppmm correctness, this is a good place to start.
  // TODO: fernflower preference?
  private static boolean inlinePPIAndMMI(Exprent expr, Exprent next) {
    if (expr instanceof FunctionExprent) {
      FunctionExprent func = (FunctionExprent) expr;

      if (func.getFuncType() == FunctionType.PPI || func.getFuncType() == FunctionType.MMI) {
        if (func.getLstOperands().get(0) instanceof VarExprent) {
          VarExprent var = (VarExprent) func.getLstOperands().get(0);

          // Can't inline ppmm into next ppmm
          if (next instanceof FunctionExprent) {
            if (isPPMM((FunctionExprent) next)) {
              return false;
            }
          }

          // Try to find the next use of the variable
          Pair<Exprent, VarExprent> usage = findFirstValidUsage(var, next);

          // Found usage
          if (usage != null) {
            // Replace exprent
            usage.a.replaceExprent(usage.b, func);
            return true;
          }
        }
      }
    }

    return false;
  }

  // Try to find the first valid usage of a variable for PPMM inlining.
  // Returns Pair{parent exprent, variable exprent to replace}
  private static Pair<Exprent, VarExprent> findFirstValidUsage(VarExprent match, Exprent next) {
    Deque<Exprent> stack = new LinkedList<>();
    stack.add(next);

    while (!stack.isEmpty()) {
      Exprent expr = stack.removeLast();

      List<Exprent> exprents = expr.getAllExprents();

      if (expr instanceof FunctionExprent) {
        FunctionExprent func = (FunctionExprent) expr;

        // Don't inline ppmm into more ppmm
        if (isPPMM(func)) {
          continue;
        }

        // Don't consider || or &&
        if (func.getFuncType() == FunctionType.BOOLEAN_OR || func.getFuncType() == FunctionType.BOOLEAN_AND) {
          return null;
        }

        // Don't consider ternaries
        if (func.getFuncType() == FunctionType.TERNARY) {
          return null;
        }

        // Subtraction and division make it hard to deduce which variable is used, especially without SSAU, so cancel if we find
        if (func.getFuncType() == FunctionType.SUB || func.getFuncType() == FunctionType.DIV) {
          return null;
        }
      }

      // Reverse iteration to ensure DFS
      for (int i = exprents.size() - 1; i >= 0; i--) {
        Exprent ex = exprents.get(i);
        boolean add = true;

        // Skip LHS of assignment as it is invalid
        if (expr instanceof AssignmentExprent) {
          add = ex != ((AssignmentExprent) expr).getLeft();
        }

        // Check var if we find
        if (add && ex instanceof VarExprent) {
          VarExprent ve = (VarExprent) ex;

          if (ve.getIndex() == match.getIndex() && ve.getVersion() == match.getVersion()) {
            return Pair.of(expr, ve);
          }
        }

        // Ignore ++/-- exprents as they aren't valid usages to replace
        if (ex instanceof FunctionExprent) {
          add = !isPPMM((FunctionExprent) ex);
        }

        if (add) {
          stack.add(ex);
        }
      }
    }

    // Couldn't find
    return null;
  }

  private static boolean isPPMM(FunctionExprent func) {
    return
      func.getFuncType() == FunctionType.PPI ||
      func.getFuncType() == FunctionType.MMI ||
      func.getFuncType() == FunctionType.IPP ||
      func.getFuncType() == FunctionType.IMM;
  }

  private static boolean isMonitorExit(Exprent first) {
    if (first instanceof MonitorExprent) {
      MonitorExprent expr = (MonitorExprent) first;
      return expr.getMonType() == MonitorExprent.Type.EXIT &&
             expr.getValue() instanceof VarExprent &&
             !((VarExprent) expr.getValue()).isStack() &&
             expr.isRemovable();
    }

    return false;
  }

  private static boolean hasQualifiedNewGetClass(Statement parent, Statement child) {
    if (child instanceof BasicBlockStatement && child.getExprents() != null && !child.getExprents().isEmpty()) {
      Exprent firstExpr = child.getExprents().get(child.getExprents().size() - 1);

      if (parent instanceof IfStatement) {
        if (isQualifiedNewGetClass(firstExpr, ((IfStatement) parent).getHeadexprent().getCondition())) {
          child.getExprents().remove(firstExpr);
          return true;
        }
      }
      // TODO DoStatements ?
    }
    return false;
  }

  private static boolean isQualifiedNewGetClass(Exprent first, Exprent second) {
    if (first instanceof InvocationExprent) {
      InvocationExprent invocation = (InvocationExprent) first;

      if ((!invocation.isStatic() &&
           invocation.getName().equals("getClass") && invocation.getStringDescriptor().equals("()Ljava/lang/Class;")) // J8
          || (invocation.isStatic() && invocation.getClassname().equals("java/util/Objects") && invocation.getName().equals("requireNonNull")
              && invocation.getStringDescriptor().equals("(Ljava/lang/Object;)Ljava/lang/Object;"))) { // J9+

        if (invocation.isSyntheticNullCheck()) {
          return true;
        }

        LinkedList<Exprent> lstExprents = new LinkedList<>();
        lstExprents.add(second);

        final Exprent target;
        if (invocation.isStatic()) { // Objects.requireNonNull(target) (J9+)
          // detect target type
          target = invocation.getLstParameters().get(0);
        } else { // target.getClass() (J8)
          target = invocation.getInstance();
        }

        while (!lstExprents.isEmpty()) {
          Exprent expr = lstExprents.removeFirst();
          lstExprents.addAll(expr.getAllExprents());
          if (expr instanceof NewExprent) {
            NewExprent newExpr = (NewExprent) expr;
            if (newExpr.getConstructor() != null && !newExpr.getConstructor().getLstParameters().isEmpty() &&
                (newExpr.getConstructor().getLstParameters().get(0).equals(target) ||
                 isUnambiguouslySameParam(invocation.isStatic(), target, newExpr.getConstructor().getLstParameters()))) {

              String classname = newExpr.getNewType().value;
              ClassNode node = DecompilerContext.getClassProcessor().getMapRootClasses().get(classname);
              if (node != null && node.type != ClassNode.Type.ROOT) {
                return true;
              }
            }
          }
        }
      }
    }

    return false;
  }

  private static boolean isUnambiguouslySameParam(boolean isStatic, Exprent target, List<Exprent> parameters) {
    boolean firstParamOfSameType = parameters.get(0).getExprType().equals(target.getExprType());
    if (!isStatic) { // X.getClass()/J8, this is less likely to overlap with legitimate use
      return firstParamOfSameType;
    }
    // Calling Objects.requireNonNull and discarding the result is a common pattern in normal code, so we have to be a bit more
    // cautious about stripping calls when a constructor takes parameters of the instance type
    // ex. given a class X, `Objects.requireNonNull(someInstanceOfX); new X(someInstanceOfX)` should not have the rNN stripped.
    if (!firstParamOfSameType) {
      return false;
    }

    for (int i = 1; i < parameters.size(); i++) {
      if (parameters.get(i).getExprType().equals(target.getExprType())) {
        return false;
      }
    }

    return true;
  }

  // var10000 = get()
  // var10000[0] = var10000[0] + 10;
  //
  // becomes
  //
  // get()[0] = get()[0] + 10;
  //
  // which then becomes
  //
  // get()[0] += 10;
  //
  // when assignments are updated at the very end of the processing pipeline. This method assumes assignment updating will always happen, otherwise it'll lead to duplicated code execution!
  // FIXME: Move to a more reasonable place or implement assignment merging in StackVarsProcessor!
  private static boolean isMethodArrayAssign(Exprent expr, Exprent next) {
    if (expr instanceof AssignmentExprent && next instanceof AssignmentExprent) {
      Exprent firstLeft = ((AssignmentExprent) expr).getLeft();
      Exprent secondLeft = ((AssignmentExprent) next).getLeft();


      if (firstLeft instanceof VarExprent && secondLeft instanceof ArrayExprent) {
        Exprent secondBase = ((ArrayExprent) secondLeft).getArray();

        if (secondBase instanceof VarExprent && ((VarExprent) firstLeft).getIndex() == ((VarExprent) secondBase).getIndex() && ((VarExprent) secondBase).isStack()) {

          boolean foundAssign = false;
          Exprent secondRight = ((AssignmentExprent) next).getRight();
          for (Exprent exprent : secondRight.getAllExprents()) {
            if (exprent instanceof ArrayExprent &&
                ((ArrayExprent) exprent).getArray() instanceof VarExprent &&
                ((VarExprent) ((ArrayExprent) exprent).getArray()).getIndex() == ((VarExprent) firstLeft).getIndex()) {
              exprent.replaceExprent(((ArrayExprent) exprent).getArray(), ((AssignmentExprent) expr).getRight().copy());
              foundAssign = true;
            }
          }

          if (foundAssign) {
            secondLeft.replaceExprent(secondBase, ((AssignmentExprent) expr).getRight());
            return true;
          }
        }
      }
    }

    return false;
  }

  // propagate (var = new X) forward to the <init> invocation
  private static boolean isConstructorInvocationRemote(List<Exprent> list, int index) {
    Exprent current = list.get(index);

    if (current instanceof AssignmentExprent) {
      AssignmentExprent as = (AssignmentExprent) current;

      if (as.getLeft() instanceof VarExprent && as.getRight() instanceof NewExprent) {

        NewExprent newExpr = (NewExprent) as.getRight();
        VarType newType = newExpr.getNewType();
        VarVersionPair leftPair = new VarVersionPair((VarExprent) as.getLeft());

        if (newType.type == CodeConstants.TYPE_OBJECT && newType.arrayDim == 0 && newExpr.getConstructor() == null) {
          for (int i = index + 1; i < list.size(); i++) {
            Exprent remote = list.get(i);

            // <init> invocation
            if (remote instanceof InvocationExprent) {
              InvocationExprent in = (InvocationExprent) remote;

              if (in.getFunctype() == InvocationExprent.Type.INIT &&
                  in.getInstance() instanceof VarExprent &&
                  as.getLeft().equals(in.getInstance())) {
                newExpr.setConstructor(in);
                in.setInstance(null);

                list.set(i, as.copy());

                return true;
              }
            }

            // check for variable in use
            Set<VarVersionPair> setVars = remote.getAllVariables();
            if (setVars.contains(leftPair)) { // variable used somewhere in between -> exit, need a better reduced code
              return false;
            }
          }
        }
      }
    }

    return false;
  }

  // Some constructor invocations use swap to call <init>.
  //
  // Type type = new Type;
  // var = type;
  // type.<init>(...);
  //
  // turns into
  //
  // var = new Type(...);
  //
  private static boolean isSwapConstructorInvocation(Exprent last, Exprent expr, Exprent next) {
    if (last instanceof AssignmentExprent && expr instanceof AssignmentExprent && next instanceof InvocationExprent) {
      AssignmentExprent asLast = (AssignmentExprent) last;
      AssignmentExprent asExpr = (AssignmentExprent) expr;
      InvocationExprent inNext = (InvocationExprent) next;

      // Make sure the next invocation is a constructor invocation!
      if (inNext.getFunctype() != InvocationExprent.Type.INIT) {
        return false;
      }

      if (asLast.getLeft() instanceof VarExprent && asExpr.getRight() instanceof VarExprent && inNext.getInstance() != null && inNext.getInstance() instanceof VarExprent) {
        VarExprent varLast = (VarExprent) asLast.getLeft();
        VarExprent varExpr = (VarExprent) asExpr.getRight();
        VarExprent varNext = (VarExprent) inNext.getInstance();

        if (varLast.getIndex() == varExpr.getIndex() && varExpr.getIndex() == varNext.getIndex()) {
          if (asLast.getRight() instanceof NewExprent) {
            // Create constructor
            inNext.setInstance(null);
            NewExprent newExpr = (NewExprent) asLast.getRight();
            newExpr.setConstructor(inNext);

            asExpr.setRight(newExpr);

            return true;
          }
        }
      }
    }


    return false;
  }

  private static Exprent isLambda(Exprent exprent, StructClass cl) {
    List<Exprent> lst = exprent.getAllExprents();
    for (Exprent expr : lst) {
      Exprent ret = isLambda(expr, cl);
      if (ret != null) {
        exprent.replaceExprent(expr, ret);
      }
    }

    if (exprent instanceof InvocationExprent) {
      InvocationExprent in = (InvocationExprent) exprent;

      if (in.getInvocationType() == InvocationExprent.InvocationType.DYNAMIC) {
        String lambda_class_name = cl.qualifiedName + in.getInvokeDynamicClassSuffix();
        ClassNode lambda_class = DecompilerContext.getClassProcessor().getMapRootClasses().get(lambda_class_name);

        if (lambda_class != null) { // real lambda class found, replace invocation with an anonymous class
          NewExprent newExpr = new NewExprent(new VarType(lambda_class_name, true), null, 0, in.bytecode);
          newExpr.setConstructor(in);
          // note: we don't set the instance to null with in.setInstance(null) like it is done for a common constructor invocation
          // lambda can also be a reference to a virtual method (e.g. String x; ...(x::toString);)
          // in this case instance will hold the corresponding object

          return newExpr;
        }
      }
    }

    return null;
  }

  private static Exprent isSimpleConstructorInvocation(Exprent exprent) {
    List<Exprent> lst = exprent.getAllExprents();
    for (Exprent expr : lst) {
      Exprent ret = isSimpleConstructorInvocation(expr);
      if (ret != null) {
        exprent.replaceExprent(expr, ret);
      }
    }

    if (exprent instanceof InvocationExprent) {
      InvocationExprent in = (InvocationExprent) exprent;
      if (in.getFunctype() == InvocationExprent.Type.INIT && in.getInstance() instanceof NewExprent) {
        NewExprent newExpr = (NewExprent) in.getInstance();
        newExpr.setConstructor(in);
        in.setInstance(null);
        return newExpr;
      }
    }

    return null;
  }

  private static boolean buildIff(Statement stat, SSAConstructorSparseEx ssa) {
    if (stat instanceof IfStatement && stat.getExprents() == null) {
      IfStatement statement = (IfStatement) stat;
      Exprent ifHeadExpr = statement.getHeadexprent();
      BitSet ifHeadExprBytecode = (ifHeadExpr == null ? null : ifHeadExpr.bytecode);

      if (statement.iftype == IfStatement.IFTYPE_IFELSE) {
        Statement ifStatement = statement.getIfstat();
        Statement elseStatement = statement.getElsestat();

        if (ifStatement.getExprents() != null && ifStatement.getExprents().size() == 1 &&
            elseStatement.getExprents() != null && elseStatement.getExprents().size() == 1 &&
            ifStatement.getAllSuccessorEdges().size() == 1 && elseStatement.getAllSuccessorEdges().size() == 1 &&
            ifStatement.getAllSuccessorEdges().get(0).getDestination() == elseStatement.getAllSuccessorEdges().get(0).getDestination()) {
          Exprent ifExpr = ifStatement.getExprents().get(0);
          Exprent elseExpr = elseStatement.getExprents().get(0);

          if (ifExpr instanceof AssignmentExprent && elseExpr instanceof AssignmentExprent) {
            AssignmentExprent ifAssign = (AssignmentExprent) ifExpr;
            AssignmentExprent elseAssign = (AssignmentExprent) elseExpr;

            if (ifAssign.getLeft() instanceof VarExprent && elseAssign.getLeft() instanceof VarExprent) {
              VarExprent ifVar = (VarExprent) ifAssign.getLeft();
              VarExprent elseVar = (VarExprent) elseAssign.getLeft();

              if (ifVar.getIndex() == elseVar.getIndex() && ifVar.isStack()) { // ifVar.getIndex() >= VarExprent.STACK_BASE) {
                boolean found = false;

                // Can happen in EliminateLoopsHelper
                if (ssa == null) {
                  throw new IllegalStateException("Trying to make ternary but have no SSA-Form! How is this possible?");
                }

                for (Entry<VarVersionPair, FastSparseSet<Integer>> ent : ssa.getPhi().entrySet()) {
                  if (ent.getKey().var == ifVar.getIndex()) {
                    if (ent.getValue().contains(ifVar.getVersion()) && ent.getValue().contains(elseVar.getVersion())) {
                      found = true;
                      break;
                    }
                  }
                }

                if (found) {
                  List<Exprent> data = new ArrayList<>(statement.getFirst().getExprents());

                  List<Exprent> operands = Arrays.asList(statement.getHeadexprent().getCondition(), ifAssign.getRight(), elseAssign.getRight());
                  data.add(new AssignmentExprent(ifVar, new FunctionExprent(FunctionType.TERNARY, operands, ifHeadExprBytecode), ifHeadExprBytecode));
                  statement.setExprents(data);

                  if (statement.getAllSuccessorEdges().isEmpty()) {
                    StatEdge ifEdge = ifStatement.getAllSuccessorEdges().get(0);
                    StatEdge edge = new StatEdge(ifEdge.getType(), statement, ifEdge.getDestination());

                    statement.addSuccessor(edge);
                    if (ifEdge.closure != null) {
                      ifEdge.closure.addLabeledEdge(edge);
                    }
                  }

                  SequenceHelper.destroyAndFlattenStatement(statement);

                  return true;
                }
              }
            }
          } else if (ifExpr instanceof ExitExprent && elseExpr instanceof ExitExprent) {
            ExitExprent ifExit = (ExitExprent) ifExpr;
            ExitExprent elseExit = (ExitExprent) elseExpr;

            if (ifExit.getExitType() == elseExit.getExitType() && ifExit.getValue() != null && elseExit.getValue() != null &&
                ifExit.getExitType() == ExitExprent.Type.RETURN) {
              // throw is dangerous, because of implicit casting to a common superclass
              // e.g. throws IOException and throw true?new RuntimeException():new IOException(); won't work
              if (ifExit.getExitType() == ExitExprent.Type.THROW &&
                  !ifExit.getValue().getExprType().equals(elseExit.getValue().getExprType())) {  // note: getExprType unreliable at this point!
                return false;
              }

              // avoid flattening to 'iff' if any of the branches is an 'iff' already
              if (isIff(ifExit.getValue()) || isIff(elseExit.getValue())) {
                return false;
              }

              List<Exprent> data = new ArrayList<>(statement.getFirst().getExprents());

              data.add(new ExitExprent(ifExit.getExitType(), new FunctionExprent(FunctionType.TERNARY,
                Arrays.asList(
                  statement.getHeadexprent().getCondition(),
                  ifExit.getValue(),
                  elseExit.getValue()), ifHeadExprBytecode), ifExit.getRetType(), ifHeadExprBytecode, ifExit.getMethodDescriptor()));
              statement.setExprents(data);

              StatEdge retEdge = ifStatement.getAllSuccessorEdges().get(0);
              Statement closure = retEdge.closure == statement ? statement.getParent() : retEdge.closure;
              statement.addSuccessor(new StatEdge(StatEdge.TYPE_BREAK, statement, retEdge.getDestination(), closure));

              SequenceHelper.destroyAndFlattenStatement(statement);

              return true;
            }
          }
        }
      }
    }

    return false;
  }

  private static boolean isIff(Exprent exp) {
    return exp instanceof FunctionExprent && ((FunctionExprent) exp).getFuncType() == FunctionType.TERNARY;
  }

  private static boolean collapseInlinedClass14(Statement stat) {
    boolean ret = class14Builder.match(stat);
    if (ret) {
      String class_name = (String) class14Builder.getVariableValue("$classname$");
      AssignmentExprent assignment = (AssignmentExprent) class14Builder.getVariableValue("$assignfield$");
      FieldExprent fieldExpr = (FieldExprent) class14Builder.getVariableValue("$field$");

      assignment.replaceExprent(assignment.getRight(), new ConstExprent(VarType.VARTYPE_CLASS, class_name, null));

      List<Exprent> data = new ArrayList<>(stat.getFirst().getExprents());

      stat.setExprents(data);

      SequenceHelper.destroyAndFlattenStatement(stat);

      ClassWrapper wrapper = (ClassWrapper) DecompilerContext.getProperty(DecompilerContext.CURRENT_CLASS_WRAPPER);
      if (wrapper != null) {
        wrapper.getHiddenMembers().add(InterpreterUtil.makeUniqueKey(fieldExpr.getName(), fieldExpr.getDescriptor().descriptorString));
      }
    }

    return ret;
  }
}
