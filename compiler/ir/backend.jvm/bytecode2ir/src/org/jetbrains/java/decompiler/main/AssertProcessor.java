// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.java.decompiler.main;

import org.jetbrains.java.decompiler.code.CodeConstants;
import org.jetbrains.java.decompiler.code.cfg.BasicBlock;
import org.jetbrains.java.decompiler.main.ClassesProcessor.ClassNode;
import org.jetbrains.java.decompiler.main.collectors.CounterContainer;
import org.jetbrains.java.decompiler.main.extern.IFernflowerPreferences;
import org.jetbrains.java.decompiler.main.rels.ClassWrapper;
import org.jetbrains.java.decompiler.main.rels.MethodWrapper;
import org.jetbrains.java.decompiler.modules.decompiler.SecondaryFunctionsHelper;
import org.jetbrains.java.decompiler.modules.decompiler.StatEdge;
import org.jetbrains.java.decompiler.modules.decompiler.exps.*;
import org.jetbrains.java.decompiler.modules.decompiler.exps.FunctionExprent.FunctionType;
import org.jetbrains.java.decompiler.modules.decompiler.stats.*;
import org.jetbrains.java.decompiler.struct.StructField;
import org.jetbrains.java.decompiler.struct.gen.FieldDescriptor;
import org.jetbrains.java.decompiler.struct.gen.VarType;
import org.jetbrains.java.decompiler.util.InterpreterUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class AssertProcessor {

  private static final VarType CLASS_ASSERTION_ERROR = new VarType(CodeConstants.TYPE_OBJECT, 0, "java/lang/AssertionError");

  public static void buildAssertions(ClassNode node) {

    ClassWrapper wrapper = node.getWrapper();

    StructField field = findAssertionField(node);

    if (field != null) {

      String key = InterpreterUtil.makeUniqueKey(field.getName(), field.getDescriptor());

      boolean res = false;

      for (MethodWrapper meth : wrapper.getMethods()) {
        RootStatement root = meth.root;
        if (root != null) {
          res |= replaceAssertions(root, wrapper.getClassStruct().qualifiedName, key);
        }
      }

      if (res) {
        // hide the helper field
        wrapper.getHiddenMembers().add(key);
      }
    }
  }

  private static StructField findAssertionField(ClassNode node) {

    ClassWrapper wrapper = node.getWrapper();

    boolean noSynthFlag = DecompilerContext.getOption(IFernflowerPreferences.SYNTHETIC_NOT_SET);

    for (StructField fd : wrapper.getClassStruct().getFields()) {

      String keyField = InterpreterUtil.makeUniqueKey(fd.getName(), fd.getDescriptor());

      // initializer exists
      if (wrapper.getStaticFieldInitializers().containsKey(keyField)) {

        // access flags set
        if (fd.hasModifier(CodeConstants.ACC_STATIC) && fd.hasModifier(CodeConstants.ACC_FINAL) && (noSynthFlag || fd.isSynthetic())) {

          // field type boolean
          FieldDescriptor fdescr = FieldDescriptor.parseDescriptor(fd.getDescriptor());
          if (VarType.VARTYPE_BOOLEAN.equals(fdescr.type)) {

            Exprent initializer = wrapper.getStaticFieldInitializers().getWithKey(keyField);
            if (initializer instanceof FunctionExprent) {
              FunctionExprent fexpr = (FunctionExprent)initializer;

              if (fexpr.getFuncType() == FunctionType.BOOL_NOT &&
                  fexpr.getLstOperands().get(0) instanceof InvocationExprent) {

                InvocationExprent invexpr = (InvocationExprent)fexpr.getLstOperands().get(0);

                if (invexpr.getInstance() != null &&
                    invexpr.getInstance() instanceof ConstExprent &&
                    "desiredAssertionStatus".equals(invexpr.getName()) &&
                    "java/lang/Class".equals(invexpr.getClassname()) &&
                    invexpr.getLstParameters().isEmpty()) {

                  ConstExprent cexpr = (ConstExprent)invexpr.getInstance();
                  if (VarType.VARTYPE_CLASS.equals(cexpr.getConstType())) {

                    ClassNode nd = node;
                    while (nd != null) {
                      if (nd.getWrapper().getClassStruct().qualifiedName.equals(cexpr.getValue())) {
                        break;
                      }
                      nd = nd.parent;
                    }

                    if (nd != null) { // found enclosing class with the same name
                      return fd;
                    }
                  }
                }
              }
            }
          }
        }
      }
    }


    return null;
  }


  private static boolean replaceAssertions(Statement statement, String classname, String key) {

    boolean res = false;

    for (Statement st : new ArrayList<>(statement.getStats())) {
      res |= replaceAssertions(st, classname, key);
    }

    boolean replaced = true;
    while (replaced) {
      replaced = false;

      for (Statement st : new ArrayList<>(statement.getStats())) {
        if (st instanceof IfStatement) {
          if (replaceAssertion(statement, (IfStatement)st, classname, key)) {
            replaced = true;
            break;
          }
        }
      }

      res |= replaced;
    }

    return res;
  }

  private static boolean replaceAssertion(Statement parent, IfStatement stat, String classname, String key) {

    boolean throwInIf = true;
    Statement ifstat = stat.getIfstat();
    InvocationExprent throwError = isAssertionError(ifstat);

    if (throwError == null) {
      //check else:
      Statement elsestat = stat.getElsestat();
      throwError = isAssertionError(elsestat);

      if (throwError == null) {
          return false;
      } else {
          throwInIf = false;
      }
    }

    Object[] exprres = getAssertionExprent(stat, stat.getHeadexprent().getCondition().copy(), classname, key, throwInIf);
    if (!(Boolean)exprres[1]) {
      return false;
    }

    List<Exprent> lstParams = new ArrayList<>();

    Exprent ascond = null, retcond = null;
    if (throwInIf) {
      if (exprres[0] != null) {
        ascond = new FunctionExprent(FunctionType.BOOL_NOT, (Exprent)exprres[0], throwError.bytecode);
        retcond = SecondaryFunctionsHelper.propagateBoolNot(ascond);
      }
    }
    else {
        ascond =  (Exprent) exprres[0];
        retcond = ascond;
    }


    lstParams.add(retcond == null ? ascond : retcond);
    if (!throwError.getLstParameters().isEmpty()) {
      lstParams.add(throwError.getLstParameters().get(0));
    }

    AssertExprent asexpr = new AssertExprent(lstParams);

    Statement newstat = new BasicBlockStatement(new BasicBlock(
      DecompilerContext.getCounterContainer().getCounterAndIncrement(CounterContainer.STATEMENT_COUNTER)));
    newstat.setExprents(Arrays.asList(new Exprent[]{asexpr}));

    Statement first = stat.getFirst();

    if (stat.iftype == IfStatement.IFTYPE_IFELSE ||
      (first.getExprents() != null && !first.getExprents().isEmpty())) {

      first.removeSuccessor(stat.getIfEdge());
      first.removeSuccessor(stat.getElseEdge());

      List<Statement> lstStatements = new ArrayList<>();
      if (first.getExprents() != null && !first.getExprents().isEmpty()) {
        lstStatements.add(first);
      }

      lstStatements.add(newstat);
      if (stat.iftype == IfStatement.IFTYPE_IFELSE) {
        if (throwInIf) {
          lstStatements.add(stat.getElsestat());
        } else {
          lstStatements.add(stat.getIfstat());
        }
      }

      SequenceStatement sequence = new SequenceStatement(lstStatements);
      sequence.setAllParent();

      for (int i = 0; i < sequence.getStats().size() - 1; i++) {
        sequence.getStats().get(i).addSuccessor(new StatEdge(StatEdge.TYPE_REGULAR,
                                                             sequence.getStats().get(i), sequence.getStats().get(i + 1)));
      }

      if (stat.iftype == IfStatement.IFTYPE_IFELSE || !throwInIf) {
        Statement stmts;
        if (throwInIf) {
          stmts = stat.getElsestat();
        }
        else {
          stmts = stat.getIfstat();
        }

        List<StatEdge> lstSuccs = stmts.getAllSuccessorEdges();
        if (!lstSuccs.isEmpty()) {
          StatEdge endedge = lstSuccs.get(0);
          if (endedge.closure == stat) {
            sequence.addLabeledEdge(endedge);
          }
        }
      }

      newstat = sequence;
    }

    newstat.getVarDefinitions().addAll(stat.getVarDefinitions());
    parent.replaceStatement(stat, newstat);
    if (exprres.length > 2) {
      if ((Boolean)exprres[2]) {
        parent.getParent().replaceWith(parent);
      }
    }

    return true;
  }

  private static InvocationExprent isAssertionError(Statement stat) {

    if (stat == null || stat.getExprents() == null || stat.getExprents().size() != 1) {
      return null;
    }

    Exprent expr = stat.getExprents().get(0);

    if (expr instanceof ExitExprent) {
      ExitExprent exexpr = (ExitExprent)expr;
      if (exexpr.getExitType() == ExitExprent.Type.THROW && exexpr.getValue() instanceof NewExprent) {
        NewExprent nexpr = (NewExprent)exexpr.getValue();
        if (CLASS_ASSERTION_ERROR.equals(nexpr.getNewType()) && nexpr.getConstructor() != null) {
          return nexpr.getConstructor();
        }
      }
    }

    return null;
  }

  // {Exprent, valid, needsReplacingOfParent}
  private static Object[] getAssertionExprent(Statement stat, Exprent exprent, String classname, String key, boolean throwInIf) {

    if (exprent instanceof FunctionExprent) {
      FunctionType desiredOperation = FunctionType.BOOLEAN_AND;
      if (!throwInIf) {
        desiredOperation = FunctionType.BOOLEAN_OR;
      }

      FunctionExprent fexpr = (FunctionExprent)exprent;
      if (fexpr.getFuncType() == desiredOperation) {

        for (int i = 0; i < 2; i++) {
          Exprent param = fexpr.getLstOperands().get(i);

          if (isAssertionField(param, classname, key, throwInIf)) {
            return new Object[]{fexpr.getLstOperands().get(1 - i), true};
          }
        }

        for (int i = 0; i < 2; i++) {
          Exprent param = fexpr.getLstOperands().get(i);

          Object[] res = getAssertionExprent(stat, param, classname, key, throwInIf);
          if ((Boolean)res[1]) {
            if (param != res[0]) {
              fexpr.getLstOperands().set(i, (Exprent)res[0]);
            }

            return new Object[]{fexpr, true};
          }
        }
      } else if (isAssertionField(fexpr, classname, key, throwInIf)) {
        // assert false;
        return new Object[]{null, true};
      } else if (fexpr.getFuncType() == FunctionType.BOOL_NOT) {
        // Switch expression assert

        Statement parent = parentSkippingSequences(stat);
        if (parent instanceof IfStatement) {
          Exprent param = fexpr.getLstOperands().get(0);

          if (isAssertionField(((IfStatement)parent).getHeadexprent().getCondition(), classname, key, throwInIf)) {
            // Bool not will be propagated out
            return new Object[]{fexpr, true, true};
          }
        }
      }
    }

    return new Object[]{exprent, false};
  }

  private static Statement parentSkippingSequences(Statement stat) {
    stat = stat.getParent();

    while (stat instanceof SequenceStatement) {
      stat = stat.getParent();
    }

    return stat;
  }

  private static boolean isAssertionField(Exprent exprent, String classname, String key, boolean throwInIf) {
    if (throwInIf) {
      if (exprent instanceof FunctionExprent) {
        FunctionExprent fparam = (FunctionExprent)exprent;
        if (fparam.getFuncType() == FunctionType.BOOL_NOT &&
            fparam.getLstOperands().get(0) instanceof FieldExprent) {
          FieldExprent fdparam = (FieldExprent)fparam.getLstOperands().get(0);
          return classname.equals(fdparam.getClassname()) &&
                 key.equals(InterpreterUtil.makeUniqueKey(fdparam.getName(), fdparam.getDescriptor().descriptorString));
        }
      }
    }
    else {
      if (exprent instanceof FieldExprent) {
        FieldExprent fdparam = (FieldExprent) exprent;
        return classname.equals(fdparam.getClassname()) &&
               key.equals(InterpreterUtil.makeUniqueKey(fdparam.getName(), fdparam.getDescriptor().descriptorString));
      }
    }
    return false;
  }
}
