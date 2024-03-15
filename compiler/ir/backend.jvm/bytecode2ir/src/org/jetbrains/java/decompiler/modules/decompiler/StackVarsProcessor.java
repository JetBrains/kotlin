// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.java.decompiler.modules.decompiler;

import org.jetbrains.java.decompiler.code.CodeConstants;
import org.jetbrains.java.decompiler.main.ClassesProcessor.ClassNode;
import org.jetbrains.java.decompiler.main.DecompilerContext;
import org.jetbrains.java.decompiler.main.extern.IFernflowerPreferences;
import org.jetbrains.java.decompiler.modules.decompiler.exps.*;
import org.jetbrains.java.decompiler.modules.decompiler.flow.DirectGraph;
import org.jetbrains.java.decompiler.modules.decompiler.flow.DirectNode;
import org.jetbrains.java.decompiler.modules.decompiler.flow.DirectNodeType;
import org.jetbrains.java.decompiler.modules.decompiler.flow.FlattenStatementsHelper;
import org.jetbrains.java.decompiler.modules.decompiler.exps.FunctionExprent.FunctionType;
import org.jetbrains.java.decompiler.modules.decompiler.sforms.*;
import org.jetbrains.java.decompiler.modules.decompiler.stats.DoStatement;
import org.jetbrains.java.decompiler.modules.decompiler.stats.RootStatement;
import org.jetbrains.java.decompiler.modules.decompiler.stats.Statement;
import org.jetbrains.java.decompiler.modules.decompiler.vars.VarVersionEdge;
import org.jetbrains.java.decompiler.modules.decompiler.vars.VarVersionNode;
import org.jetbrains.java.decompiler.modules.decompiler.vars.VarVersionPair;
import org.jetbrains.java.decompiler.modules.decompiler.vars.VarVersionsGraph;
import org.jetbrains.java.decompiler.struct.StructClass;
import org.jetbrains.java.decompiler.struct.StructMethod;
import org.jetbrains.java.decompiler.struct.gen.MethodDescriptor;
import org.jetbrains.java.decompiler.struct.gen.VarType;
import org.jetbrains.java.decompiler.util.FastSparseSetFactory.FastSparseSet;
import org.jetbrains.java.decompiler.util.InterpreterUtil;
import org.jetbrains.java.decompiler.util.SFormsFastMapDirect;

import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

public class StackVarsProcessor {
  public void simplifyStackVars(RootStatement root, StructMethod mt, StructClass cl) {
    Set<Integer> setReorderedIfs = new HashSet<>();
    SSAUConstructorSparseEx ssau = null;

    while (true) {
      boolean found = false;
      boolean first = ssau == null;

      SSAConstructorSparseEx ssa = new SSAConstructorSparseEx();
      ssa.splitVariables(root, mt);

      while (SimplifyExprentsHelper.simplifyStackVarsStatement(root, setReorderedIfs, ssa, cl, first)) {
        ValidationHelper.validateStatement(root);
        found = true;
      }

      setVersionsToNull(root);

      SequenceHelper.condenseSequences(root);
      ValidationHelper.validateStatement(root);

      ssau = new SSAUConstructorSparseEx();
      ssau.splitVariables(root, mt);

      if (first) {
        setEffectivelyFinalVars(root, ssau, new HashMap<>());
        ValidationHelper.validateStatement(root);
      }

      if (iterateStatements(root, ssau)) {
        ValidationHelper.validateStatement(root);
        found = true;
      }

      setVersionsToNull(root);

      if (!found) {
        break;
      }
    }

    // remove unused assignments
    ssau = new SSAUConstructorSparseEx();
    ssau.splitVariables(root, mt);
    ValidationHelper.validateStatement(root);

    iterateStatements(root, ssau);

    setVersionsToNull(root);
  }

  public static void setVersionsToNull(Statement stat) {
    if (stat.getExprents() == null) {
      for (Object obj : stat.getSequentialObjects()) {
        if (obj instanceof Statement) {
          setVersionsToNull((Statement)obj);
        }
        else if (obj instanceof Exprent) {
          setExprentVersionsToNull((Exprent)obj);
        }
      }
    }
    else {
      for (Exprent exprent : stat.getExprents()) {
        setExprentVersionsToNull(exprent);
      }
    }
  }

  private static void setExprentVersionsToNull(Exprent exprent) {
    List<Exprent> lst = exprent.getAllExprents(true);
    lst.add(exprent);

    for (Exprent expr : lst) {
      if (expr instanceof VarExprent) {
        ((VarExprent)expr).setVersion(0);
      }
    }
  }

  private boolean iterateStatements(RootStatement root, SSAUConstructorSparseEx ssa) {
    FlattenStatementsHelper flatthelper = new FlattenStatementsHelper();
    DirectGraph dgraph = flatthelper.buildDirectGraph(root);

    boolean res = false;

    Set<DirectNode> setVisited = new HashSet<>();
    LinkedList<DirectNode> stack = new LinkedList<>();
    LinkedList<Map<VarVersionPair, Exprent>> stackMaps = new LinkedList<>();

    stack.add(dgraph.first);
    stackMaps.add(new HashMap<>());

    while (!stack.isEmpty()) {
      DirectNode nd = stack.removeFirst();
      Map<VarVersionPair, Exprent> mapVarValues = stackMaps.removeFirst();

      if (setVisited.contains(nd)) {
        continue;
      }

      setVisited.add(nd);

      List<List<Exprent>> lstLists = new ArrayList<>();

      if (!nd.exprents.isEmpty()) {
        lstLists.add(nd.exprents);
      }

      if (nd.succs().size() == 1) {
        DirectNode ndsucc = nd.succs().get(0);

        if (ndsucc.type == DirectNodeType.TAIL && !ndsucc.exprents.isEmpty()) {
          lstLists.add(nd.succs().get(0).exprents);
          nd = ndsucc;
        }
      }

      // To handle stacks created by some duplicated bytecode (dup, dup_x2, etc.) better, we run the simplification algorithm in 2 passes.
      // The first pass is the classic algorithm, and the second pass is a more aggressive one that allows some slightly unsafe operations, such as simplifying across variables.
      // To ensure the second pass doesn't break good bytecode, if the first pass manages to update any exprent it will cancel the second pass.
      // This behavior can be turned off with a fernflower preference.
      for (int stackStage = 0; stackStage < 2; stackStage++) {
        // If instructed to not use the second pass, set it to 2 here to prevent the loop from working
        if (!DecompilerContext.getOption(IFernflowerPreferences.SIMPLIFY_STACK_SECOND_PASS)) {
          stackStage = 2;
        }

        for (int i = 0; i < lstLists.size(); i++) {
          List<Exprent> lst = lstLists.get(i);

          int index = 0;
          while (index < lst.size()) {
            Exprent next = null;

            if (index == lst.size() - 1) {
              if (i < lstLists.size() - 1) {
                next = lstLists.get(i + 1).get(0);
              }
            } else {
              next = lst.get(index + 1);
            }

            boolean simplifyAcrossStack = stackStage == 1;

            // {newIndex, changed}
            int[] ret = iterateExprent(lst, index, next, mapVarValues, ssa, simplifyAcrossStack);

            // If index is specified, set to that
            if (ret[0] >= 0) {
              index = ret[0];
            } else {
              // Otherwise, continue to next index
              index++;
            }

            // Mark if we changed
            boolean changed = ret[1] == 1;
            res |= changed;

            // We only want to simplify across stack bounds if we were not able to change *anything*
            if (changed) {
              // Cancel the second pass by setting the stage to 2, preventing the check next time it runs
              stackStage = 2;
            }
            // An (unintentional) side effect of this implementation is that as soon as the second pass is able to change the stack, it'll cancel further iteration of the second pass, preventing it from creating wrong code by accident.
          }
        }
      }

      for (DirectNode ndx : nd.succs()) {
        stack.add(ndx);
        stackMaps.add(new HashMap<>(mapVarValues));
      }

      // make sure the 3 special exprent lists in a loop (init, condition, increment) are not empty
      // change loop type if necessary
      if (nd.exprents.isEmpty() &&
          (nd.type == DirectNodeType.INIT || nd.type == DirectNodeType.CONDITION || nd.type == DirectNodeType.INCREMENT)) {
        nd.exprents.add(null);

        if (nd.statement instanceof DoStatement) {
          DoStatement loop = (DoStatement)nd.statement;

          if (loop.getLooptype() == DoStatement.Type.FOR &&
              loop.getInitExprent() == null &&
              loop.getIncExprent() == null) { // "downgrade" loop to 'while'
            loop.setLooptype(DoStatement.Type.WHILE);
          }
        }
      }
    }

    return res;
  }

  private static Exprent isReplaceableVar(Exprent exprent, Map<VarVersionPair, Exprent> mapVarValues) {
    Exprent dest = null;

    if (exprent instanceof VarExprent) {
      VarExprent var = (VarExprent)exprent;
      dest = mapVarValues.get(new VarVersionPair(var));
    }

    return dest;
  }

  private static void replaceSingleVar(Exprent parent, VarExprent var, Exprent dest, SSAUConstructorSparseEx ssau) {
    parent.replaceExprent(var, dest);
    dest.addBytecodeOffsets(var.bytecode);

    // live sets
    SFormsFastMapDirect liveMap = ssau.getLiveVarVersionsMap(new VarVersionPair(var));

    // Find all var versions found in the destination variable (that was just replaced)
    Set<VarVersionPair> setVars = getAllVersions(dest);

    for (VarVersionPair pair : setVars) {
      VarVersionNode node = ssau.getSsuVersions().nodes.getWithKey(pair);

      // Account for optimization done in SSAUConstructorSparseEx
      if (node.live == null) {
        continue;
      }

      // Iterate through live variables
      for (Entry<Integer, FastSparseSet<Integer>> entry : node.live.entryList()) {
        // real var, stack var, or field var (0, 1, 2 respectively)
        Integer key = entry.getKey();

        // If the live map doesn't contain this type of var, remove the entry
        if (!liveMap.containsKey(key)) {
          node.live.remove(key);
        } else {
          FastSparseSet<Integer> set = entry.getValue();

          set.complement(liveMap.get(key));

          if (set.isEmpty()) {
            node.live.remove(key);
          }
        }
      }
    }
  }

  // {nextIndex, (changed ? 1 : 0)}
  private int[] iterateExprent(List<Exprent> lstExprents,
                               int index,
                               Exprent next,
                               Map<VarVersionPair, Exprent> mapVarValues,
                               SSAUConstructorSparseEx ssau,
                               boolean simplifyAcrossStack) {
    Exprent exprent = lstExprents.get(index);

    int changed = 0;

    for (Exprent expr : exprent.getAllExprents()) {
      while (true) {
        Object[] arr = iterateChildExprent(expr, exprent, next, mapVarValues, ssau);
        Exprent retexpr = (Exprent)arr[0];
        changed |= (Boolean)arr[1] ? 1 : 0;

        boolean isReplaceable = (Boolean)arr[2];
        if (retexpr != null) {
          if (isReplaceable) {
            replaceSingleVar(exprent, (VarExprent)expr, retexpr, ssau);
            expr = retexpr;
          } else {
            exprent.replaceExprent(expr, retexpr);
            retexpr.addBytecodeOffsets(expr.bytecode);
          }

          changed = 1;
        }

        if (!isReplaceable) {
          break;
        }
      }
    }

    // no var on the highest level, so no replacing

    VarExprent left = null;
    Exprent right = null;

    if (exprent instanceof AssignmentExprent) {
      AssignmentExprent as = (AssignmentExprent)exprent;
      if (as.getLeft() instanceof VarExprent) {
        left = (VarExprent)as.getLeft();
        right = as.getRight();
      }
    }

    // No variable assignment found or variable assignment is to an effectively final variable, stop
    if (left == null || left.isEffectivelyFinal()) {
      return new int[]{-1, changed};
    }

    VarVersionPair leftVar = new VarVersionPair(left);

    List<VarVersionNode> usedVers = new ArrayList<>();
    boolean notdom = getUsedVersions(ssau, leftVar, usedVers);

    if (!notdom && usedVers.isEmpty()) {
      if (left.isStack() && (right instanceof InvocationExprent ||
                             right instanceof AssignmentExprent || right instanceof NewExprent)) {
        if (right instanceof NewExprent) {
          // new Object(); permitted
          NewExprent nexpr = (NewExprent)right;
          if (
            // TODO: why is this here? anonymous vars should be simplfiend!
//            nexpr.isAnonymous() ||
              nexpr.getNewType().arrayDim > 0 ||
              nexpr.getNewType().type != CodeConstants.TYPE_OBJECT
          ) {
            return new int[]{-1, changed};
          }
        }

        lstExprents.set(index, right);
        return new int[]{index + 1, 1};
      } else if (right instanceof VarExprent) {
        lstExprents.remove(index);
        return new int[]{index, 1};
      } else if (left.isStack() && right instanceof FunctionExprent) {
        FunctionExprent func = (FunctionExprent) right;

        if (func.getFuncType().isPostfixPPMM()) {
          // Unused IPP or IMM, typically from arrays
          lstExprents.set(index, right);
          return new int[]{index, 1};
        } else if (func.getFuncType() == FunctionType.CAST) {
          // Unused cast, remove
          lstExprents.remove(index);
          return new int[]{index, 1};
        }

        return new int[]{-1, changed};
      } else if (left.isStack() && right instanceof FieldExprent) {
        // Unused field access, remove
        // Field access is pure so this should be safe
        // This technically hides that there is a field access though!
        // TODO: fernflower preference?
        lstExprents.remove(index);
        return new int[]{index, 1};
      } else {
        return new int[]{-1, changed};
      }
    }

    int useflags = right.getExprentUse();

    // stack variables only
    if (!left.isStack() &&
        (!(right instanceof VarExprent) || ((VarExprent)right).isStack())) { // special case catch(... ex)
      return new int[]{-1, changed};
    }

    if ((useflags & Exprent.MULTIPLE_USES) == 0 && (notdom || usedVers.size() > 1)) {
      return new int[]{-1, changed};
    }

    Map<Integer, Set<VarVersionPair>> mapVars = getAllVarVersions(leftVar, right, ssau);

    boolean isSelfReference = mapVars.containsKey(leftVar.var);
    if (isSelfReference && notdom) {
      return new int[]{-1, changed};
    }

    // Aggressive second pass, see if it's possible that we can simplify across the next exprent to find the exprent 2 indices away
    if (simplifyAcrossStack) {
      Exprent simplifiedAcrossStack = simplifyAcrossStackExprent(lstExprents, index, next, right, left);

      if (simplifiedAcrossStack != null) {
        next = simplifiedAcrossStack;
      }
    }

    Set<VarVersionPair> setNextVars = next == null ? null : getAllVersions(next);

    // FIXME: fix the entire method!
    if (!(right instanceof ConstExprent) &&
        !(right instanceof VarExprent) &&
        setNextVars != null &&
        mapVars.containsKey(leftVar.var)) {
      for (VarVersionNode usedvar : usedVers) {
        if (!setNextVars.contains(new VarVersionPair(usedvar.var, usedvar.version))) {
          return new int[]{-1, changed};
        }
      }
    }

    mapVars.remove(leftVar.var);

    boolean vernotreplaced = false;
    boolean verreplaced = false;

    Set<VarVersionPair> setTempUsedVers = new HashSet<>();

    for (VarVersionNode usedvar : usedVers) {
      VarVersionPair usedver = new VarVersionPair(usedvar.var, usedvar.version);

      if (isVersionToBeReplaced(usedver, mapVars, ssau, leftVar) &&
          (right instanceof ConstExprent || right instanceof VarExprent || right instanceof FieldExprent
           || setNextVars == null || setNextVars.contains(usedver))) {

        setTempUsedVers.add(usedver);
        verreplaced = true;
      } else {
        vernotreplaced = true;
      }
    }

    if (isSelfReference && vernotreplaced) {
      return new int[]{-1, changed};
    } else {
      for (VarVersionPair usedver : setTempUsedVers) {
        Exprent copy = right.copy();

        if (right instanceof FieldExprent && ssau.getMapFieldVars().containsKey(right.id)) {
          ssau.getMapFieldVars().put(copy.id, ssau.getMapFieldVars().get(right.id));
        }

        mapVarValues.put(usedver, copy);
      }
    }

    if (!notdom && !vernotreplaced) {
      // remove assignment
      lstExprents.remove(index);
      return new int[]{index, 1};
    } else if (verreplaced) {
      return new int[]{index + 1, changed};
    } else {
      return new int[]{-1, changed};
    }
  }

  private Exprent simplifyAcrossStackExprent(List<Exprent> exprents, int index, Exprent next, Exprent right, VarExprent left) {
    Exprent ret = null;

    if (next != null && next instanceof AssignmentExprent && index < exprents.size() - 2) {
      Exprent nextRight = ((AssignmentExprent) next).getRight();

      // Exprent trees
      List<Exprent> allRight = right.getAllExprents(true);
      List<Exprent> allNextRight = nextRight.getAllExprents(true);

      // Preliminary check: make sure both trees are of the same size and they're not empty
      if (allRight.size() == allNextRight.size() && !allRight.isEmpty()) {
        // Iterate through both trees and check if they're equal
        boolean ok = areTreesEqual(left, allRight, allNextRight);

        // Exprent trees equal, find the exprent 2 indices over
        if (ok) {
          ret = exprents.get(index + 2);
        }
      } else if (allNextRight.size() > allRight.size()) {
        // The next tree has a larger tree than the current one, check if the current one is a subtree of the next one

        // Crawl through the tree to see if any subtrees match
        for (Exprent exprent : allNextRight) {
          List<Exprent> subtree = exprent.getAllExprents(true);

          if (allRight.size() == subtree.size() && !allRight.isEmpty()) {
            // Iterate through both trees and check if they're equal
            boolean ok = areTreesEqual(left, allRight, subtree);

            // Exprent trees equal, find the exprent 2 indices over
            if (ok) {
              ret = exprents.get(index + 2);

              break;
            }
          }
        }
      }
    }

    return ret;
  }

  // Checks if 2 exprent trees are equal. Precondition: both trees have the same size
  private boolean areTreesEqual(VarExprent left, List<Exprent> treeA, List<Exprent> treeB) {
    boolean ok = true;

    for (int i = 0; i < treeA.size(); i++) {
      // Nodes of each tree
      Exprent a = treeA.get(i);
      Exprent b = treeB.get(i);

      // Disjoint types- cannot ever be equal!
      if (a.type != b.type) {
        ok = false;
        break;
      }

      // Var
      if (a instanceof VarExprent && b instanceof VarExprent) {
        VarExprent va = (VarExprent)a;
        VarExprent vb = (VarExprent)b;

        // We only care about the index, the version can be different as we've deduced it doesn't exist in the next exprent (thus no assignment or usage) TODO: check for incremented/live?
        if (va.getIndex() != vb.getIndex()) {
          // Disjoint var usage, not equal!
          ok = false;
          break;
        }

        if (vb.getIndex() == left.getIndex()) {
          // The next exprent is using the variable that the current exprent assigns to, making it unsafe to simplify!
          ok = false;
          break;
        }
      }

      // Field access
      if (a instanceof FieldExprent && b instanceof FieldExprent) {
        FieldExprent fa = (FieldExprent)a;
        FieldExprent fb = (FieldExprent)b;

        // FieldExprent#equals() minus instance check- that is handled above, with the var check
        if (
          !InterpreterUtil.equalObjects(fa.getName(), fb.getName())
          || !InterpreterUtil.equalObjects(fa.getClassname(), fb.getClassname())
          || !InterpreterUtil.equalObjects(fa.isStatic(), fb.isStatic())
          || !InterpreterUtil.equalObjects(fa.getDescriptor(), fb.getDescriptor())
        ) {
          // Disjoint field access, not equal!
          ok = false;
          break;
        }
      }

      // Constant value
      if (a instanceof ConstExprent && b instanceof ConstExprent) {
        if (!a.equals(b)) {
          // Constant not equal!
          ok = false;
          break;
        }
      }
      // TODO: how do we handle other exprents that may or may not be equal, like array access?
    }

    return ok;
  }

  // Gets all var versions found in a given exprent
  private static Set<VarVersionPair> getAllVersions(Exprent exprent) {
    Set<VarVersionPair> res = new HashSet<>();

    List<Exprent> exprents = exprent.getAllExprents(true);
    exprents.add(exprent);

    for (Exprent expr : exprents) {
      if (expr instanceof VarExprent) {
        VarExprent var = (VarExprent)expr;

        res.add(new VarVersionPair(var));
      }
    }

    return res;
  }

  // {returnExprent, changed, isReplaceable}
  private static Object[] iterateChildExprent(Exprent exprent,
                                              Exprent parent,
                                              Exprent next,
                                              Map<VarVersionPair, Exprent> mapVarValues,
                                              SSAUConstructorSparseEx ssau) {
    boolean changed = false;

    for (Exprent expr : exprent.getAllExprents()) {
      while (true) {
        Object[] arr = iterateChildExprent(expr, parent, next, mapVarValues, ssau);
        Exprent retexpr = (Exprent)arr[0];
        changed |= (Boolean)arr[1];

        boolean isReplaceable = (Boolean)arr[2];
        if (retexpr != null) {
          if (isReplaceable) {
            replaceSingleVar(exprent, (VarExprent)expr, retexpr, ssau);
            expr = retexpr;
          } else {
            exprent.replaceExprent(expr, retexpr);
            retexpr.addBytecodeOffsets(expr.bytecode);
          }

          changed = true;
        }

        if (!isReplaceable) {
          break;
        }
      }
    }

    // Try to replace the exprent if it's a variable found in the var values map
    Exprent dest = isReplaceableVar(exprent, mapVarValues);
    if (dest != null) {
      return new Object[]{dest, true, true};
    }


    VarExprent left = null;
    Exprent right = null;

    // If assignment to variable gather details
    if (exprent instanceof AssignmentExprent) {
      AssignmentExprent as = (AssignmentExprent)exprent;
      if (as.getLeft() instanceof VarExprent) {
        left = (VarExprent)as.getLeft();
        right = as.getRight();
      }
    }

    // No variable assignment found or variable assignment is to an effectively final variable, stop
    if (left == null || left.isEffectivelyFinal()) {
      return new Object[]{null, changed, false};
    }

    boolean isHeadSynchronized = false;
    if (next == null && parent instanceof MonitorExprent) {
      MonitorExprent monexpr = (MonitorExprent)parent;
      if (monexpr.getMonType() == MonitorExprent.Type.ENTER && exprent.equals(monexpr.getValue())) {
        isHeadSynchronized = true;
      }
    }

    // stack variable or synchronized head exprent
    if (!left.isStack() && !isHeadSynchronized) {
      return new Object[]{null, changed, false};
    }

    VarVersionPair leftVar = new VarVersionPair(left);

    List<VarVersionNode> usedVers = new ArrayList<>();
    boolean notdom = getUsedVersions(ssau, leftVar, usedVers);

    if (!notdom && usedVers.isEmpty()) {
      return new Object[]{right, changed, false};
    }

    // stack variables only
    if (!left.isStack()) {
      return new Object[]{null, changed, false};
    }

    int useflags = right.getExprentUse();

    if ((useflags & Exprent.BOTH_FLAGS) != Exprent.BOTH_FLAGS) {
      return new Object[]{null, changed, false};
    }

    Map<Integer, Set<VarVersionPair>> mapVars = getAllVarVersions(leftVar, right, ssau);
    if (mapVars.containsKey(leftVar.var) && notdom) {
      return new Object[]{null, changed, false};
    }

    mapVars.remove(leftVar.var);

    Set<VarVersionPair> setAllowedVars = getAllVersions(parent);
    if (next != null) {
      setAllowedVars.addAll(getAllVersions(next));
    }

    boolean vernotreplaced = false;

    Set<VarVersionPair> setTempUsedVers = new HashSet<>();

    for (VarVersionNode usedvar : usedVers) {
      VarVersionPair usedver = new VarVersionPair(usedvar.var, usedvar.version);
      if (isVersionToBeReplaced(usedver, mapVars, ssau, leftVar) &&
          (right instanceof VarExprent || setAllowedVars.contains(usedver))) {

        setTempUsedVers.add(usedver);
      }
      else {
        vernotreplaced = true;
      }
    }

    if (!notdom && !vernotreplaced) {
      for (VarVersionPair usedver : setTempUsedVers) {
        Exprent copy = right.copy();
        if (right instanceof FieldExprent && ssau.getMapFieldVars().containsKey(right.id)) {
          ssau.getMapFieldVars().put(copy.id, ssau.getMapFieldVars().get(right.id));
        }

        mapVarValues.put(usedver, copy);
      }

      // remove assignment
      return new Object[]{right, changed, false};
    }

    return new Object[]{null, changed, false};
  }

  private static boolean getUsedVersions(SSAUConstructorSparseEx ssa, VarVersionPair var, List<? super VarVersionNode> res) {
    VarVersionsGraph ssu = ssa.getSsuVersions();
    VarVersionNode node = ssu.nodes.getWithKey(var);

    Set<VarVersionNode> setVisited = new HashSet<>();
    Set<VarVersionNode> setNotDoms = new HashSet<>();

    LinkedList<VarVersionNode> stack = new LinkedList<>();
    stack.add(node);

    while (!stack.isEmpty()) {
      VarVersionNode nd = stack.remove(0);
      setVisited.add(nd);

      if (nd != node && (nd.flags & VarVersionNode.FLAG_PHANTOM_FINEXIT) == 0) {
        res.add(nd);
      }

      for (VarVersionEdge edge : nd.succs) {
        VarVersionNode succ = edge.dest;

        if (!setVisited.contains(edge.dest)) {

          boolean isDominated = true;
          for (VarVersionEdge prededge : succ.preds) {
            if (!setVisited.contains(prededge.source)) {
              isDominated = false;
              break;
            }
          }

          if (isDominated) {
            stack.add(succ);
          } else {
            setNotDoms.add(succ);
          }
        }
      }
    }

    setNotDoms.removeAll(setVisited);

    return !setNotDoms.isEmpty();
  }

  private static boolean isVersionToBeReplaced(VarVersionPair usedvar,
                                               Map<Integer, Set<VarVersionPair>> mapVars,
                                               SSAUConstructorSparseEx ssau,
                                               VarVersionPair leftpaar) {
    VarVersionsGraph ssuversions = ssau.getSsuVersions();

    SFormsFastMapDirect mapLiveVars = ssau.getLiveVarVersionsMap(usedvar);
    if (mapLiveVars == null) {
      // dummy version, predecessor of a phi node
      return false;
    }

    // compare protected ranges
    if (!InterpreterUtil.equalObjects(ssau.getMapVersionFirstRange().get(leftpaar),
                                      ssau.getMapVersionFirstRange().get(usedvar))) {
      return false;
    }

    for (Entry<Integer, Set<VarVersionPair>> ent : mapVars.entrySet()) {
      FastSparseSet<Integer> liveverset = mapLiveVars.get(ent.getKey());
      // TODO: checking for empty liveverset fixes <unknown> foreach, research as to why
      if (liveverset == null) {
        return false;
      }

      Set<VarVersionNode> domset = new HashSet<>();
      for (VarVersionPair verpaar : ent.getValue()) {
        domset.add(ssuversions.nodes.getWithKey(verpaar));
      }

      boolean isdom = true;

      for (int livever : liveverset) {
        VarVersionNode node = ssuversions.nodes.getWithKey(new VarVersionPair(ent.getKey(), livever));

        if (!ssuversions.isDominatorSet(node, domset)) {
          isdom = false;
          break;
        }
      }

      if (!isdom) {
        return false;
      }
    }

    return true;
  }

  private static Map<Integer, Set<VarVersionPair>> getAllVarVersions(VarVersionPair leftvar,
                                                                     Exprent exprent,
                                                                     SSAUConstructorSparseEx ssau) {
    Map<Integer, Set<VarVersionPair>> map = new HashMap<>();
    SFormsFastMapDirect mapLiveVars = ssau.getLiveVarVersionsMap(leftvar);

    List<Exprent> lst = exprent.getAllExprents(true);
    lst.add(exprent);

    for (Exprent expr : lst) {
      if (expr instanceof VarExprent) {
        int varindex = ((VarExprent)expr).getIndex();

        if (leftvar.var != varindex) {
          if (mapLiveVars.containsKey(varindex)) {
            Set<VarVersionPair> verset = new HashSet<>();
            for (int vers : mapLiveVars.get(varindex)) {
              verset.add(new VarVersionPair(varindex, vers));
            }

            map.put(varindex, verset);
          } else {
            throw new RuntimeException("inconsistent live map!");
          }
        } else {
          map.put(varindex, null);
        }
      } else if (expr instanceof FieldExprent) {
        if (ssau.getMapFieldVars().containsKey(expr.id)) {
          int varindex = ssau.getMapFieldVars().get(expr.id);

          if (mapLiveVars.containsKey(varindex)) {
            Set<VarVersionPair> verset = new HashSet<>();

            for (int vers : mapLiveVars.get(varindex)) {
              verset.add(new VarVersionPair(varindex, vers));
            }

            map.put(varindex, verset);
          }
        }
      }
    }

    return map;
  }

  private static void setEffectivelyFinalVars(Statement stat, SSAUConstructorSparseEx ssau, Map<VarVersionPair, VarExprent> varLookupMap) {
    if (stat.getExprents() != null && !stat.getExprents().isEmpty()) {
      for (int i = 0; i < stat.getExprents().size(); ++i) {
        setEffectivelyFinalVars(stat, stat.getExprents().get(i), ssau, i, stat.getExprents(), varLookupMap);
      }
    }

    for (Statement st : stat.getStats()) {
      setEffectivelyFinalVars(st, ssau, varLookupMap);
    }
  }

  private static void setEffectivelyFinalVars(Statement stat, Exprent exprent, SSAUConstructorSparseEx ssau, int index, List<Exprent> list, Map<VarVersionPair, VarExprent> varLookupMap) {
    if (exprent instanceof AssignmentExprent) {
      AssignmentExprent assign = (AssignmentExprent)exprent;
      if (assign.getLeft() instanceof VarExprent) {
        VarExprent var = (VarExprent)assign.getLeft();
        varLookupMap.put(var.getVarVersionPair(), var);
      }
    }
    else if (exprent instanceof NewExprent) {
      NewExprent newExpr = (NewExprent)exprent;
      if (newExpr.isAnonymous()) {
        ClassNode node = DecompilerContext.getClassProcessor().getMapRootClasses().get(newExpr.getNewType().value);

        if (node != null) {
          if (!newExpr.isLambda()) {
            for (StructMethod mt : node.classStruct.getMethods()) {
              if (mt.getName().equals(CodeConstants.INIT_NAME)) {
                List<VarType> paramTypes = Arrays.asList(MethodDescriptor.parseDescriptor(mt.getDescriptor()).params);

                for (int i = Math.max(0, index - paramTypes.size()); i < index; ++i) {
                  Exprent temp = list.get(i);
                  if (temp instanceof AssignmentExprent) {
                    Exprent left = ((AssignmentExprent)temp).getLeft();
                    if (left instanceof VarExprent) {
                      VarExprent leftVar = (VarExprent)left;
                      if (leftVar.getLVT() != null && paramTypes.contains(leftVar.getLVT().getVarType())) {
                        leftVar.setEffectivelyFinal(true);
                      }
                    }
                  }
                }
                break;
              }
            }
          }
          else if (!newExpr.isMethodReference()) {
            MethodDescriptor mdLambda = MethodDescriptor.parseDescriptor(node.lambdaInformation.method_descriptor);
            MethodDescriptor mdContent = MethodDescriptor.parseDescriptor(node.lambdaInformation.content_method_descriptor);
            int paramOffset = node.lambdaInformation.is_content_method_static ? 0 : 1;
            int varsCount = mdContent.params.length - mdLambda.params.length;

            for (int i = 0; i < varsCount; ++i) {
              Exprent param = newExpr.getConstructor().getLstParameters().get(paramOffset + i);
              if (param instanceof VarExprent) {
                VarExprent paramVar = (VarExprent)param;
                VarVersionPair vvp = paramVar.getVarVersionPair();
                VarVersionNode vvnode = ssau.getSsuVersions().nodes.getWithKey(vvp);

                // Edge case: vvnode can be null when loops aren't created properly for... some reason?
                if (vvnode == null) {
                  continue;
                }

                while (true) {
                  VarVersionNode next = null;
                  if (vvnode.var >= VarExprent.STACK_BASE) {
                    vvnode = vvnode.preds.iterator().next().source;
                    VarVersionPair nextVVP = ssau.getVarAssignmentMap().get(new VarVersionPair(vvnode.var, vvnode.version));
                    next = ssau.getSsuVersions().nodes.getWithKey(nextVVP);

                    if (nextVVP != null && nextVVP.var < 0) { // TODO check if field is final?
                      vvp = nextVVP;
                      break;
                    }
                  }
                  else {
                    final int j = i;
                    final int varIndex = vvnode.var;
                    final int varVersion = vvnode.version;
                    List<VarVersionNode> roots = getRoots(vvnode);
                    List<VarVersionNode> allRoots = ssau.getSsuVersions().nodes.stream()
                                                          .distinct()
                                                          .filter(n -> n.var == varIndex && n.preds.isEmpty())
                                                          .filter(n -> {
                                                            if (n.lvt != null) {
                                                              return mdContent.params[j].equals(new VarType(n.lvt.getDescriptor()));
                                                            }
                                                            return n.version > varVersion;
                                                          })
                                                          .collect(Collectors.toList());

                    if (roots.size() >= allRoots.size()) {
                      if (roots.size() == 1) {
                        vvnode = roots.get(0);
                        vvp = new VarVersionPair(vvnode.var, vvnode.version);
                        VarVersionPair nextVVP = ssau.getVarAssignmentMap().get(vvp);
                        next = ssau.getSsuVersions().nodes.getWithKey(nextVVP);
                        if (nextVVP != null && nextVVP.var < 0) {
                          vvp = nextVVP;
                          break;
                        }
                      }
                      else if (roots.size() == 2) {
                        VarVersionNode first = roots.get(0);
                        VarVersionNode second = roots.get(1);

                        // check for an if-else var definition
                        if (first.lvt != null && second.lvt != null && first.lvt.getVersion().equals(second.lvt.getVersion())) {
                          vvp = first.lvt.getVersion();
                          break;
                        }
                      }
                    }
                  }

                  if (next == null) {
                    break;
                  }
                  vvnode = next;
                }

                VarExprent var = varLookupMap.get(vvp);
                if (var != null) {
                  var.setEffectivelyFinal(true);
                }
              }
            }
          }
        }
      }
    }

    for (Exprent ex : exprent.getAllExprents()) {
      setEffectivelyFinalVars(stat, ex, ssau, index, list, varLookupMap);
    }
  }

  private static List<VarVersionNode> getRoots(VarVersionNode vvnode) {
    List<VarVersionNode> ret = new ArrayList<>();
    Set<VarVersionNode> visited = new HashSet<>();
    LinkedList<VarVersionNode> queue = new LinkedList<>();

    queue.add(vvnode);
    visited.add(vvnode);

    while (!queue.isEmpty()) {
      VarVersionNode next = queue.removeFirst();

      if (next.preds.isEmpty()) {
        ret.add(next);
      }
      else {
        next.preds.forEach(vvn -> {
          if (visited.add(vvn.source)) {
            queue.add(vvn.source);
          }
        });
      }
    }

    return ret;
  }
}