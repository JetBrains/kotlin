// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.java.decompiler.modules.decompiler;

import org.jetbrains.java.decompiler.code.CodeConstants;
import org.jetbrains.java.decompiler.code.cfg.BasicBlock;
import org.jetbrains.java.decompiler.main.DecompilerContext;
import org.jetbrains.java.decompiler.main.collectors.CounterContainer;
import org.jetbrains.java.decompiler.modules.decompiler.exps.*;
import org.jetbrains.java.decompiler.modules.decompiler.exps.FunctionExprent.FunctionType;
import org.jetbrains.java.decompiler.modules.decompiler.stats.*;
import org.jetbrains.java.decompiler.struct.StructMethod;
import org.jetbrains.java.decompiler.struct.attr.StructAnnotationAttribute;
import org.jetbrains.java.decompiler.struct.attr.StructAnnotationParameterAttribute;
import org.jetbrains.java.decompiler.struct.attr.StructGeneralAttribute;
import org.jetbrains.java.decompiler.struct.gen.MethodDescriptor;

import java.util.ArrayList;
import java.util.List;

public final class IdeaNotNullHelper {


  public static boolean removeHardcodedChecks(RootStatement root, StructMethod mt) {

    boolean checks_removed = false;

    // parameter @NotNull annotations
    while (findAndRemoveParameterCheck(root, mt)) { // iterate until nothing found. Each invocation removes one parameter check.
      checks_removed = true;

      // we modified so check again
      ValidationHelper.validateStatement(root);
    }

    // method @NotNull annotation
    while (findAndRemoveReturnCheck(root, mt)) { // iterate until nothing found. Each invocation handles one method exit check.
      checks_removed = true;

      // we modified so check again
      ValidationHelper.validateStatement(root);
    }

    return checks_removed;
  }

  private static boolean findAndRemoveParameterCheck(Statement stat, StructMethod mt) {

    Statement st = stat.getFirst();
    while (st instanceof SequenceStatement) {
      st = st.getFirst();
    }

    if (st instanceof IfStatement) {

      IfStatement ifstat = (IfStatement)st;
      Statement ifbranch = ifstat.getIfstat();

      Exprent if_condition = ifstat.getHeadexprent().getCondition();

      boolean is_notnull_check = false;

      // TODO: FunctionType.NE also possible if reversed order (in theory)
      if (ifbranch != null &&
          if_condition instanceof FunctionExprent &&
          ((FunctionExprent)if_condition).getFuncType() == FunctionType.EQ &&
          ifbranch instanceof BasicBlockStatement &&
          ifbranch.getExprents().size() == 1 &&
          ifbranch.getExprents().get(0) instanceof InvocationExprent) {

        FunctionExprent func = (FunctionExprent)if_condition;
        Exprent first_param = func.getLstOperands().get(0);
        Exprent second_param = func.getLstOperands().get(1);

        if (second_param instanceof ConstExprent &&
            second_param.getExprType().type == CodeConstants.TYPE_NULL) { // TODO: reversed parameter order
          if (first_param instanceof VarExprent && ((InvocationExprent)ifbranch.getExprents().get(0)).getName().equals("$$$reportNull$$$0")) {
            VarExprent var = (VarExprent)first_param;

            boolean thisvar = !mt.hasModifier(CodeConstants.ACC_STATIC);

            MethodDescriptor md = MethodDescriptor.parseDescriptor(mt.getDescriptor());

            // parameter annotations
            StructAnnotationParameterAttribute param_annotations =
              mt.getAttribute(StructGeneralAttribute.ATTRIBUTE_RUNTIME_INVISIBLE_PARAMETER_ANNOTATIONS);
            if (param_annotations != null) {

              List<List<AnnotationExprent>> param_annotations_lists = param_annotations.getParamAnnotations();
              int method_param_number = md.params.length;

              int index = thisvar ? 1 : 0;
              for (int i = 0; i < method_param_number; i++) {

                if (index == var.getIndex()) {
                  if (param_annotations_lists.size() >= method_param_number - i) {
                    int shift = method_param_number -
                                param_annotations_lists
                                  .size(); // NOTE: workaround for compiler bug, count annotations starting with the last parameter

                    List<AnnotationExprent> annotations = param_annotations_lists.get(i - shift);

                    for (AnnotationExprent ann : annotations) {
                      if (ann.getClassName().equals("org/jetbrains/annotations/NotNull")) {
                        is_notnull_check = true;
                        break;
                      }
                    }
                  }

                  break;
                }

                index += md.params[i].stackSize;
              }
            }
          }
        }
      }

      if (!is_notnull_check) {
        return false;
      }

      removeParameterCheck(stat);

      return true;
    }

    return false;
  }

  private static void removeParameterCheck(Statement stat) {

    Statement st = stat.getFirst();
    while (st instanceof SequenceStatement) {
      st = st.getFirst();
    }

    IfStatement ifstat = (IfStatement)st;

    // In the format of
    //
    // if (param == null) {
    //   $$$reportNull$$$(index); // index is the index of reportNull appearing in the code
    // }
    //
    if (ifstat.getElsestat() == null) {
      StatEdge ifedge = ifstat.getIfEdge();

      Statement ifbranch = ifstat.getIfstat();

      ifstat.getFirst().removeSuccessor(ifedge);

      ifstat.getStats().removeWithKey(ifbranch.id);

      if (ifbranch.hasAnySuccessor()) {
        ifbranch.removeSuccessor(ifbranch.getFirstSuccessor());
      }

      // Replace statement with empty block
      BasicBlockStatement block = new BasicBlockStatement(new BasicBlock(
        DecompilerContext.getCounterContainer().getCounterAndIncrement(CounterContainer.STATEMENT_COUNTER)));
      block.setExprents(new ArrayList<>());

      ifstat.getParent().replaceStatement(ifstat, block);
      ifstat.getParent().setAllParent();

      // Remove the empty block that we just inserted
      SequenceHelper.condenseSequences(stat.getTopParent());
    }
  }

  private static boolean findAndRemoveReturnCheck(Statement stat, StructMethod mt) {

    boolean is_notnull_check = false;

    // method annotation, refers to the return value
    StructAnnotationAttribute attr = mt.getAttribute(StructGeneralAttribute.ATTRIBUTE_RUNTIME_INVISIBLE_ANNOTATIONS);
    if (attr != null) {
      List<AnnotationExprent> annotations = attr.getAnnotations();

      for (AnnotationExprent ann : annotations) {
        if (ann.getClassName().equals("org/jetbrains/annotations/NotNull")) {
          is_notnull_check = true;
          break;
        }
      }
    }

    return is_notnull_check && removeReturnCheck(stat, mt);
  }


  private static boolean removeReturnCheck(Statement stat, StructMethod mt) {

    Statement parent = stat.getParent();

    // TODO: is this check doing anything? seems like modern notnull creation doesn't cause this to be run.
    if (parent instanceof IfStatement && stat instanceof BasicBlockStatement && stat.getExprents().size() == 1) {
      Exprent exprent = stat.getExprents().get(0);
      if (exprent instanceof ExitExprent) {
        ExitExprent exit_exprent = (ExitExprent)exprent;
        if (exit_exprent.getExitType() == ExitExprent.Type.RETURN) {
          Exprent exprent_value = exit_exprent.getValue();
          //if(exprent_value instanceof VarExprent) {
          //	VarExprent var_value = (VarExprent)exprent_value;

          IfStatement ifparent = (IfStatement)parent;
          Exprent if_condition = ifparent.getHeadexprent().getCondition();

          if (ifparent.getElsestat() == stat && if_condition instanceof FunctionExprent &&
              ((FunctionExprent)if_condition).getFuncType() == FunctionType.EQ) { // TODO: reversed order possible (in theory)

            FunctionExprent func = (FunctionExprent)if_condition;
            Exprent first_param = func.getLstOperands().get(0);
            Exprent second_param = func.getLstOperands().get(1);

            StatEdge ifedge = ifparent.getIfEdge();
            StatEdge elseedge = ifparent.getElseEdge();

            Statement ifbranch = ifparent.getIfstat();
            Statement elsebranch = ifparent.getElsestat();

            if (second_param instanceof ConstExprent &&
                second_param.getExprType().type == CodeConstants.TYPE_NULL) { // TODO: reversed parameter order
              //if(first_param instanceof VarExprent && ((VarExprent)first_param).getIndex() == var_value.getIndex()) {
              if (first_param.equals(exprent_value)) {        // TODO: check for absence of side effects like method invocations etc.
                if (ifbranch instanceof BasicBlockStatement &&
                    ifbranch.getExprents().size() == 1 &&
                    // TODO: special check for IllegalStateException
                    ifbranch.getExprents().get(0) instanceof ExitExprent) {

                  ifparent.getFirst().removeSuccessor(ifedge);
                  ifparent.getFirst().removeSuccessor(elseedge);

                  ifparent.getStats().removeWithKey(ifbranch.id);
                  ifparent.getStats().removeWithKey(elsebranch.id);

                  if (ifbranch.hasAnySuccessor()) {
                    ifbranch.removeSuccessor(ifbranch.getFirstSuccessor());
                  }

                  if (!ifparent.getFirst().getExprents().isEmpty()) {
                    elsebranch.getExprents().addAll(0, ifparent.getFirst().getExprents());
                  }

                  ifparent.getParent().replaceStatement(ifparent, elsebranch);
                  ifparent.getParent().setAllParent();

                  return true;
                }
              }
            }
          }
          //}
        }
      }
    }
    else if (parent != null &&
             parent instanceof SequenceStatement &&
             stat instanceof BasicBlockStatement &&
             stat.getExprents().size() == 1) {
      Exprent exprent = stat.getExprents().get(0);
      if (exprent instanceof ExitExprent) {
        ExitExprent exit_exprent = (ExitExprent)exprent;
        if (exit_exprent.getExitType() == ExitExprent.Type.RETURN) {
          Exprent exprent_value = exit_exprent.getValue();

          SequenceStatement sequence = (SequenceStatement)parent;
          int sequence_stats_number = sequence.getStats().size();

          if (sequence_stats_number > 1 &&
              sequence.getStats().getLast() == stat &&
              sequence.getStats().get(sequence_stats_number - 2) instanceof IfStatement) {

            IfStatement ifstat = (IfStatement)sequence.getStats().get(sequence_stats_number - 2);
            Exprent if_condition = ifstat.getHeadexprent().getCondition();

            if (ifstat.iftype == IfStatement.IFTYPE_IF && if_condition instanceof FunctionExprent &&
                ((FunctionExprent)if_condition).getFuncType() == FunctionType.EQ) { // TODO: reversed order possible (in theory)

              FunctionExprent func = (FunctionExprent)if_condition;
              Exprent first_param = func.getLstOperands().get(0);
              Exprent second_param = func.getLstOperands().get(1);

              Statement ifbranch = ifstat.getIfstat();

              if (second_param instanceof ConstExprent &&
                  second_param.getExprType().type == CodeConstants.TYPE_NULL) { // TODO: reversed parameter order
                if (first_param.equals(exprent_value)) {        // TODO: check for absence of side effects like method invocations etc.
                  if (ifbranch instanceof BasicBlockStatement &&
                      ifbranch.getExprents().size() == 1 &&
                      ifbranch.getExprents().get(0) instanceof InvocationExprent && ((InvocationExprent)ifbranch.getExprents().get(0)).getName().equals("$$$reportNull$$$0")) {

                    // In the format of
                    //
                    // if (returnValue == null) {
                    //   $$$reportNull$$$(index); // index is the index of reportNull appearing in the code
                    // }
                    //
                    // return returnValue;
                    //

                    ifstat.getFirstSuccessor().remove(); // remove 'else' edge

                    if (!ifstat.getFirst().getExprents().isEmpty()) {
                      stat.getExprents().addAll(0, ifstat.getFirst().getExprents());
                    }

                    for (StatEdge edge : ifstat.getAllPredecessorEdges()) {
                      edge.changeDestination(stat);
                    }

                    ifbranch.getFirstSuccessor().remove();

                    sequence.getStats().removeWithKey(ifstat.id);
                    sequence.setFirst(sequence.getStats().get(0));

                    return true;
                  }
                }
              }
            }
          }
        }
      }
    }


    for (Statement st : stat.getStats()) {
      if (removeReturnCheck(st, mt)) {
        return true;
      }
    }

    return false;
  }
}
