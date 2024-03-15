// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.java.decompiler.main.rels;

import org.jetbrains.java.decompiler.code.CodeConstants;
import org.jetbrains.java.decompiler.main.ClassesProcessor.ClassNode;
import org.jetbrains.java.decompiler.main.DecompilerContext;
import org.jetbrains.java.decompiler.main.collectors.CounterContainer;
import org.jetbrains.java.decompiler.main.collectors.VarNamesCollector;
import org.jetbrains.java.decompiler.main.extern.IFernflowerPreferences;
import org.jetbrains.java.decompiler.modules.decompiler.exps.*;
import org.jetbrains.java.decompiler.modules.decompiler.flow.DirectGraph;
import org.jetbrains.java.decompiler.modules.decompiler.flow.DirectNode;
import org.jetbrains.java.decompiler.modules.decompiler.vars.VarVersionPair;
import org.jetbrains.java.decompiler.struct.StructMethod;
import org.jetbrains.java.decompiler.struct.gen.MethodDescriptor;
import org.jetbrains.java.decompiler.util.InterpreterUtil;

import java.util.*;

public class NestedMemberAccess {

  private enum MethodAccess {NORMAL, FIELD_GET, FIELD_SET, METHOD, FUNCTION}

  private boolean noSynthFlag;
  private final Map<MethodWrapper, MethodAccess> mapMethodType = new HashMap<>();


  public void propagateMemberAccess(ClassNode root) {
    if (root.nested.isEmpty()) {
      return;
    }

    noSynthFlag = DecompilerContext.getOption(IFernflowerPreferences.SYNTHETIC_NOT_SET);

    computeMethodTypes(root);

    eliminateStaticAccess(root);
  }


  private void computeMethodTypes(ClassNode node) {
    if (node.type == ClassNode.Type.LAMBDA) {
      return;
    }

    for (ClassNode nd : node.nested) {
      computeMethodTypes(nd);
    }

    for (MethodWrapper method : node.getWrapper().getMethods()) {
      computeMethodType(node, method);
    }
  }

  private void computeMethodType(ClassNode node, MethodWrapper method) {
    MethodAccess type = MethodAccess.NORMAL;

    if (method.root != null) {
      DirectGraph graph = method.getOrBuildGraph();

      StructMethod mt = method.methodStruct;
      if ((noSynthFlag || mt.isSynthetic()) && mt.hasModifier(CodeConstants.ACC_STATIC)) {
        if (graph.nodes.size() == 2) {  // incl. dummy exit node
          if (graph.first.exprents.size() == 1) {
            Exprent exprent = graph.first.exprents.get(0);

            MethodDescriptor mtdesc = MethodDescriptor.parseDescriptor(mt.getDescriptor());
            int parcount = mtdesc.params.length;

            Exprent exprCore = exprent;

            if (exprent instanceof ExitExprent) {
              ExitExprent exexpr = (ExitExprent)exprent;
              if (exexpr.getExitType() == ExitExprent.Type.RETURN && exexpr.getValue() != null) {
                exprCore = exexpr.getValue();
              }
            }

            switch (exprCore.type) {
              case FIELD:
                FieldExprent fexpr = (FieldExprent)exprCore;
                if ((parcount == 1 && !fexpr.isStatic()) ||
                    (parcount == 0 && fexpr.isStatic())) {
                  if (fexpr.getClassname().equals(node.classStruct.qualifiedName)) {  // FIXME: check for private flag of the field
                    if (fexpr.isStatic() ||
                        (fexpr.getInstance() instanceof VarExprent && ((VarExprent)fexpr.getInstance()).getIndex() == 0)) {
                      type = MethodAccess.FIELD_GET;
                    }
                  }
                }
                break;
              case VAR:  // qualified this
                if (parcount == 1) {
                  // this or final variable
                  if (((VarExprent)exprCore).getIndex() != 0) {
                    type = MethodAccess.FIELD_GET;
                  }
                }

                break;
              case FUNCTION:
                // for now detect only increment/decrement
                FunctionExprent functionExprent = (FunctionExprent)exprCore;
                if (functionExprent.getFuncType().isPPMM()) {
                  if (functionExprent.getLstOperands().get(0) instanceof FieldExprent) {
                    type = MethodAccess.FUNCTION;
                  }
                }
                break;
              case INVOCATION:
                type = MethodAccess.METHOD;
                break;
              case ASSIGNMENT:
                AssignmentExprent asexpr = (AssignmentExprent)exprCore;
                if (asexpr.getLeft() instanceof FieldExprent && asexpr.getRight() instanceof VarExprent) {
                  FieldExprent fexpras = (FieldExprent)asexpr.getLeft();
                  if ((parcount == 2 && !fexpras.isStatic()) ||
                      (parcount == 1 && fexpras.isStatic())) {
                    if (fexpras.getClassname().equals(node.classStruct.qualifiedName)) { // FIXME: check for private flag of the field
                      if (fexpras.isStatic() ||
                          (fexpras.getInstance() instanceof VarExprent && ((VarExprent)fexpras.getInstance()).getIndex() == 0)) {
                        if (((VarExprent)asexpr.getRight()).getIndex() == parcount - 1) {
                          type = MethodAccess.FIELD_SET;
                        }
                      }
                    }
                  }
                }
            }

            if (type == MethodAccess.METHOD) { // FIXME: check for private flag of the method

              type = MethodAccess.NORMAL;

              InvocationExprent invexpr = (InvocationExprent)exprCore;

              boolean isStatic = invexpr.isStatic();
              if ((isStatic && invexpr.getLstParameters().size() == parcount) ||
                  (!isStatic && invexpr.getInstance() instanceof VarExprent
                   && ((VarExprent)invexpr.getInstance()).getIndex() == 0 && invexpr.getLstParameters().size() == parcount - 1)) {

                boolean equalpars = true;

                int index = isStatic ? 0 : 1;
                for (int i = 0; i < invexpr.getLstParameters().size(); i++) {
                  Exprent parexpr = invexpr.getLstParameters().get(i);
                  if (!(parexpr instanceof VarExprent) || ((VarExprent)parexpr).getIndex() != index) {
                    equalpars = false;
                    break;
                  }
                  index += mtdesc.params[i + (isStatic ? 0 : 1)].stackSize;
                }

                if (equalpars) {
                  type = MethodAccess.METHOD;
                }
              }
            }
          }
          else if (graph.first.exprents.size() == 2) {
            Exprent exprentFirst = graph.first.exprents.get(0);
            Exprent exprentSecond = graph.first.exprents.get(1);

            if (exprentFirst instanceof AssignmentExprent &&
                exprentSecond instanceof ExitExprent) {

              MethodDescriptor mtdesc = MethodDescriptor.parseDescriptor(mt.getDescriptor());
              int parcount = mtdesc.params.length;

              AssignmentExprent asexpr = (AssignmentExprent)exprentFirst;
              if (asexpr.getLeft() instanceof FieldExprent && asexpr.getRight() instanceof VarExprent) {
                FieldExprent fexpras = (FieldExprent)asexpr.getLeft();
                if ((parcount == 2 && !fexpras.isStatic()) ||
                    (parcount == 1 && fexpras.isStatic())) {
                  if (fexpras.getClassname().equals(node.classStruct.qualifiedName)) { // FIXME: check for private flag of the field
                    if (fexpras.isStatic() ||
                        (fexpras.getInstance() instanceof VarExprent && ((VarExprent)fexpras.getInstance()).getIndex() == 0)) {
                      if (((VarExprent)asexpr.getRight()).getIndex() == parcount - 1) {

                        ExitExprent exexpr = (ExitExprent)exprentSecond;
                        if (exexpr.getExitType() == ExitExprent.Type.RETURN && exexpr.getValue() != null) {
                          if (exexpr.getValue() instanceof VarExprent &&
                              ((VarExprent)asexpr.getRight()).getIndex() == parcount - 1) {
                            type = MethodAccess.FIELD_SET;
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
      }
    }

    if (type != MethodAccess.NORMAL) {
      mapMethodType.put(method, type);
    }
    else {
      mapMethodType.remove(method);
    }
  }


  private void eliminateStaticAccess(ClassNode node) {

    if (node.type == ClassNode.Type.LAMBDA) {
      return;
    }

    for (MethodWrapper meth : node.getWrapper().getMethods()) {

      if (meth.root != null) {

        boolean replaced = false;

        DirectGraph graph = meth.getOrBuildGraph();

        HashSet<DirectNode> setVisited = new HashSet<>();
        LinkedList<DirectNode> stack = new LinkedList<>();
        stack.add(graph.first);

        while (!stack.isEmpty()) {  // TODO: replace with interface iterator?

          DirectNode nd = stack.removeFirst();

          if (setVisited.contains(nd)) {
            continue;
          }
          setVisited.add(nd);

          for (int i = 0; i < nd.exprents.size(); i++) {
            Exprent exprent = nd.exprents.get(i);

            replaced |= replaceInvocations(node, meth, exprent);

            if (exprent instanceof InvocationExprent) {
              Exprent ret = replaceAccessExprent(node, meth, (InvocationExprent)exprent);

              if (ret != null) {
                nd.exprents.set(i, ret);
                replaced = true;
              }
            }
          }

          stack.addAll(nd.succs());
        }

        if (replaced) {
          computeMethodType(node, meth);
        }
      }
    }

    for (ClassNode child : node.nested) {
      eliminateStaticAccess(child);
    }
  }


  private boolean replaceInvocations(ClassNode caller, MethodWrapper meth, Exprent exprent) {

    boolean res = false;

    for (Exprent expr : exprent.getAllExprents()) {
      res |= replaceInvocations(caller, meth, expr);
    }

    while (true) {

      boolean found = false;

      for (Exprent expr : exprent.getAllExprents()) {
        if (expr instanceof InvocationExprent) {
          Exprent newexpr = replaceAccessExprent(caller, meth, (InvocationExprent)expr);
          if (newexpr != null) {
            exprent.replaceExprent(expr, newexpr);
            found = true;
            res = true;
            break;
          }
        }
      }

      if (!found) {
        break;
      }
    }

    return res;
  }

  private static boolean sameTree(ClassNode caller, ClassNode callee) {

    if (caller.classStruct.qualifiedName.equals(callee.classStruct.qualifiedName)) {
      return false;
    }

    while (caller.parent != null) {
      caller = caller.parent;
    }

    while (callee.parent != null) {
      callee = callee.parent;
    }

    return caller == callee;
  }

  private Exprent replaceAccessExprent(ClassNode caller, MethodWrapper methdest, InvocationExprent invexpr) {
    ClassNode node = DecompilerContext.getClassProcessor().getMapRootClasses().get(invexpr.getClassname());

    MethodWrapper methsource = null;
    if (node != null && node.getWrapper() != null) {
      methsource = node.getWrapper().getMethodWrapper(invexpr.getName(), invexpr.getStringDescriptor());
    }

    if (methsource == null || !mapMethodType.containsKey(methsource)) {
      return null;
    }

    // if same method, return
    if (node.classStruct.qualifiedName.equals(caller.classStruct.qualifiedName) &&
        methsource.methodStruct.getName().equals(methdest.methodStruct.getName()) &&
        methsource.methodStruct.getDescriptor().equals(methdest.methodStruct.getDescriptor())) {
      // no recursive invocations permitted!
      return null;
    }

    MethodAccess type = mapMethodType.get(methsource);

    //		// FIXME: impossible case. MethodAccess.NORMAL is not saved in the map
    //		if(type == MethodAccess.NORMAL) {
    //			return null;
    //		}

    if (!sameTree(caller, node)) {
      return null;
    }

    DirectGraph graph = methsource.getOrBuildGraph();
    Exprent source = graph.first.exprents.get(0);

    Exprent retexprent = null;

    switch (type) {
      case FIELD_GET:
        ExitExprent exsource = (ExitExprent)source;
        if (exsource.getValue() instanceof VarExprent) { // qualified this
          VarExprent var = (VarExprent)exsource.getValue();
          String varname = methsource.varproc.getVarName(new VarVersionPair(var));

          if (!methdest.setOuterVarNames.contains(varname)) {
            VarNamesCollector vnc = new VarNamesCollector();
            vnc.addName(varname);

            methdest.varproc.refreshVarNames(vnc);
            methdest.setOuterVarNames.add(varname);
          }

          int index = methdest.counter.getCounterAndIncrement(CounterContainer.VAR_COUNTER);
          VarExprent ret = new VarExprent(index, var.getVarType(), methdest.varproc);
          methdest.varproc.setVarName(new VarVersionPair(index, 0), varname);

          retexprent = ret;
        }
        else { // field
          FieldExprent ret = (FieldExprent)exsource.getValue().copy();
          if (!ret.isStatic()) {
            ret.replaceExprent(ret.getInstance(), invexpr.getLstParameters().get(0));
          }
          retexprent = ret;
        }
        break;
      case FIELD_SET:
        AssignmentExprent ret;
        if (source instanceof ExitExprent) {
          ExitExprent extex = (ExitExprent)source;
          ret = (AssignmentExprent)extex.getValue().copy();
        }
        else {
          ret = (AssignmentExprent)source.copy();
        }
        FieldExprent fexpr = (FieldExprent)ret.getLeft();

        if (fexpr.isStatic()) {
          ret.replaceExprent(ret.getRight(), invexpr.getLstParameters().get(0));
        }
        else {
          ret.replaceExprent(ret.getRight(), invexpr.getLstParameters().get(1));
          fexpr.replaceExprent(fexpr.getInstance(), invexpr.getLstParameters().get(0));
        }

        // do not use copied bytecodes
        ret.getLeft().bytecode = null;
        ret.getRight().bytecode = null;

        retexprent = ret;
        break;
      case FUNCTION:
        retexprent = replaceFunction(invexpr, source);
        break;
      case METHOD:
        if (source instanceof ExitExprent) {
          source = ((ExitExprent)source).getValue();
        }

        InvocationExprent invret = (InvocationExprent)source.copy();

        int index = 0;
        if (!invret.isStatic()) {
          invret.replaceExprent(invret.getInstance(), invexpr.getLstParameters().get(0));
          index = 1;
        }

        for (int i = 0; i < invret.getLstParameters().size(); i++) {
          invret.replaceExprent(invret.getLstParameters().get(i), invexpr.getLstParameters().get(i + index));
        }

        retexprent = invret;
    }


    if (retexprent != null) {
      // preserve original bytecodes
      retexprent.bytecode = null;
      retexprent.addBytecodeOffsets(invexpr.bytecode);

      // hide synthetic access method
      boolean hide = true;

      if (node.type == ClassNode.Type.ROOT || (node.access & CodeConstants.ACC_STATIC) != 0) {
        StructMethod mt = methsource.methodStruct;
        if (!mt.isSynthetic()) {
          hide = false;
        }
      }
      if (hide) {
        node.getWrapper().getHiddenMembers().add(InterpreterUtil.makeUniqueKey(invexpr.getName(), invexpr.getStringDescriptor()));
      }
    }

    return retexprent;
  }

  private static Exprent replaceFunction(final InvocationExprent invexpr, final Exprent source) {
    FunctionExprent functionExprent = (FunctionExprent)((ExitExprent)source).getValue().copy();

    List<Exprent> lstParameters = invexpr.getLstParameters();

    FieldExprent fieldExprent = (FieldExprent)functionExprent.getLstOperands().get(0);
    if (fieldExprent.isStatic()) {
      if (!lstParameters.isEmpty()) {
        return null;
      }
      return functionExprent;
    }

    if (lstParameters.size() != 1) {
      return null;
    }

    fieldExprent.replaceExprent(fieldExprent.getInstance(), lstParameters.get(0));
    return functionExprent;
  }
}
