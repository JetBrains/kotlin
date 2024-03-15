// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.java.decompiler.modules.decompiler.vars;

import org.jetbrains.java.decompiler.code.CodeConstants;
import org.jetbrains.java.decompiler.main.ClassesProcessor.ClassNode;
import org.jetbrains.java.decompiler.main.DecompilerContext;
import org.jetbrains.java.decompiler.main.collectors.VarNamesCollector;
import org.jetbrains.java.decompiler.main.extern.IFernflowerPreferences;
import org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor;
import org.jetbrains.java.decompiler.modules.decompiler.exps.*;
import org.jetbrains.java.decompiler.modules.decompiler.stats.*;
import org.jetbrains.java.decompiler.modules.decompiler.vars.VarTypeProcessor.FinalType;
import org.jetbrains.java.decompiler.struct.StructClass;
import org.jetbrains.java.decompiler.struct.StructMethod;
import org.jetbrains.java.decompiler.struct.attr.StructGeneralAttribute;
import org.jetbrains.java.decompiler.struct.attr.StructLocalVariableTableAttribute;
import org.jetbrains.java.decompiler.struct.attr.StructLocalVariableTableAttribute.LocalVariable;
import org.jetbrains.java.decompiler.struct.attr.StructMethodParametersAttribute;
import org.jetbrains.java.decompiler.struct.gen.MethodDescriptor;
import org.jetbrains.java.decompiler.struct.gen.VarType;
import org.jetbrains.java.decompiler.struct.gen.generics.GenericType;
import org.jetbrains.java.decompiler.util.ArrayHelper;
import org.jetbrains.java.decompiler.util.StatementIterator;

import java.util.*;
import java.util.Map.Entry;

public class VarDefinitionHelper {

  private final HashMap<Integer, Statement> mapVarDefStatements;

  // statement.id, defined vars
  private final HashMap<Integer, HashSet<Integer>> mapStatementVars;

  private final HashSet<Integer> implDefVars;

  private final VarProcessor varproc;

  private final Statement root;
  private final StructMethod mt;
  private final Map<VarVersionPair, String> clashingNames = new HashMap<>();

  public VarDefinitionHelper(Statement root, StructMethod mt, VarProcessor varproc) {
    this(root, mt, varproc, true);
  }

  public VarDefinitionHelper(Statement root, StructMethod mt, VarProcessor varproc, boolean run) {

    mapVarDefStatements = new HashMap<>();
    mapStatementVars = new HashMap<>();
    implDefVars = new HashSet<>();

    this.varproc = varproc;
    this.root = root;
    this.mt = mt;

    // If we are asking for a pure invocation, don't run the analysis
    if (!run) {
      return;
    }

    VarNamesCollector vc = varproc.getVarNamesCollector();

    boolean thisvar = !mt.hasModifier(CodeConstants.ACC_STATIC);

    MethodDescriptor md = MethodDescriptor.parseDescriptor(mt.getDescriptor());

    int paramcount = 0;
    if (thisvar) {
      paramcount = 1;
    }
    paramcount += md.params.length;

    // method parameters are implicitly defined
    int varindex = 0;
    for (int i = 0; i < paramcount; i++) {
      implDefVars.add(varindex);
      VarVersionPair vpp = new VarVersionPair(varindex, 0);
      if (varindex != 0 || !thisvar) {
        varproc.setVarName(vpp, vc.getFreeName(varindex));
      }

      if (thisvar) {
        if (i == 0) {
          varindex++;
        }
        else {
          varindex += md.params[i - 1].stackSize;
        }
      }
      else {
        varindex += md.params[i].stackSize;
      }
    }

    if (thisvar) {
      StructClass current_class = (StructClass)DecompilerContext.getProperty(DecompilerContext.CURRENT_CLASS);

      varproc.getThisVars().put(new VarVersionPair(0, 0), current_class.qualifiedName);
      varproc.setVarName(new VarVersionPair(0, 0), "this");
      vc.addName("this");
    }

    mergeVars(root);

    // catch variables are implicitly defined
    LinkedList<Statement> stack = new LinkedList<>();
    stack.add(root);

    while (!stack.isEmpty()) {
      Statement st = stack.removeFirst();

      List<VarExprent> lstVars = st.getImplicitlyDefinedVars();

      if (lstVars != null) {
        for (VarExprent var : lstVars) {
          implDefVars.add(var.getIndex());
          varproc.setVarName(new VarVersionPair(var), vc.getFreeName(var.getIndex()));
          var.setDefinition(true);
        }
      }

      stack.addAll(st.getStats());
    }

    initStatement(root);
  }

  public void setVarDefinitions() {
    VarNamesCollector vc = varproc.getVarNamesCollector();

    for (Entry<Integer, Statement> en : mapVarDefStatements.entrySet()) {
      Statement stat = en.getValue();
      int index = en.getKey();

      if (implDefVars.contains(index)) {
        // already implicitly defined
        continue;
      }

      varproc.setVarName(new VarVersionPair(index, 0), vc.getFreeName(index));

      // special case for
      if (stat instanceof DoStatement) {
        DoStatement dstat = (DoStatement)stat;
        if (dstat.getLooptype() == DoStatement.Type.FOR) {

          if (dstat.getInitExprent() != null && setDefinition(dstat.getInitExprent(), index)) {
            continue;
          }
          else {
            List<Exprent> lstSpecial = Arrays.asList(dstat.getConditionExprent(), dstat.getIncExprent());
            for (VarExprent var : getAllVars(lstSpecial)) {
              if (var.getIndex() == index) {
                stat = stat.getParent();
                break;
              }
            }
          }
        }
        else if (dstat.getLooptype() == DoStatement.Type.FOR_EACH) {
          if (dstat.getInitExprent() != null && dstat.getInitExprent() instanceof VarExprent) {
            VarExprent var = (VarExprent)dstat.getInitExprent();
            if (var.getIndex() == index) {
              var.setDefinition(true);
              continue;
            }
          }
        }
      }

      Statement first = findFirstBlock(stat, index);

      List<Exprent> lst;
      if (first == null) {
        lst = stat.getVarDefinitions();
      }
      else if (first.getExprents() == null) {
        lst = first.getVarDefinitions();
      }
      else {
        lst = first.getExprents();
      }

      boolean defset = false;

      // search for the first assignment to var [index]
      int addindex = 0;
      for (Exprent expr : lst) {
        if (setDefinition(expr, index)) {
          defset = true;
          break;
        }
        else {
          boolean foundvar = false;
          for (Exprent exp : expr.getAllExprents(true)) {
            if (exp instanceof VarExprent && ((VarExprent)exp).getIndex() == index) {
              foundvar = true;
              break;
            }
          }
          if (foundvar) {
            break;
          }
        }
        addindex++;
      }

      if (!defset) {
        VarExprent var = new VarExprent(index, varproc.getVarType(new VarVersionPair(index, 0)), varproc);
        var.setDefinition(true);

        LocalVariable lvt = findLVT(index, stat);
        if (lvt != null) {
          var.setLVT(lvt);
        }

        lst.add(addindex, var);
      }
    }

    mergeVars(root);
    propogateLVTs(root);
    setNonFinal(root, new HashSet<>());
    remapClashingNames(root);
  }


  // *****************************************************************************
  // private methods
  // *****************************************************************************

  private LocalVariable findLVT(int index, Statement stat) {
    if (stat.getExprents() == null) {
      for (Object obj : stat.getSequentialObjects()) {
        if (obj instanceof Statement) {
          LocalVariable lvt = findLVT(index, (Statement)obj);
          if (lvt != null) {
            return lvt;
          }
        }
        else if (obj instanceof Exprent) {
          LocalVariable lvt = findLVT(index, (Exprent)obj);
          if (lvt != null) {
            return lvt;
          }
        }
      }
    }
    else {
      for (Exprent exp : stat.getExprents()) {
        LocalVariable lvt = findLVT(index, exp);
        if (lvt != null) {
          return lvt;
        }
      }
    }
    return null;
  }

  private LocalVariable findLVT(int index, Exprent exp) {
    for (Exprent e: exp.getAllExprents(false)) {
      LocalVariable lvt = findLVT(index, e);
      if (lvt != null) {
        return lvt;
      }
    }

    if (!(exp instanceof VarExprent)) {
      return null;
    }

    VarExprent var = (VarExprent)exp;
    return var.getIndex() == index ? var.getLVT() : null;
  }

  private Statement findFirstBlock(Statement stat, int varindex) {

    LinkedList<Statement> stack = new LinkedList<>();
    stack.add(stat);

    while (!stack.isEmpty()) {
      Statement st = stack.remove(0);

      if (stack.isEmpty() || mapStatementVars.get(st.id).contains(varindex)) {

        if (st.isLabeled() && !stack.isEmpty()) {
          return st;
        }

        if (st.getExprents() != null) {
          return st;
        }
        else {
          stack.clear();

          switch (st.type) {
            case SEQUENCE:
              stack.addAll(0, st.getStats());
              break;
            case IF:
            case ROOT:
            case SWITCH:
            case SYNCHRONIZED:
              stack.add(st.getFirst());
              break;
            default:
              return st;
          }
        }
      }
    }

    return null;
  }

  private Set<Integer> initStatement(Statement stat) {

    HashMap<Integer, Integer> mapCount = new HashMap<>();

    List<VarExprent> condlst;

    if (stat.getExprents() == null) {

      // recurse on children statements
      List<Integer> childVars = new ArrayList<>();
      List<Exprent> currVars = new ArrayList<>();

      for (Object obj : stat.getSequentialObjects()) {
        if (obj instanceof Statement) {
          Statement st = (Statement)obj;
          childVars.addAll(initStatement(st));

          if (st instanceof DoStatement) {
            DoStatement dost = (DoStatement)st;
            if (dost.getLooptype() != DoStatement.Type.FOR &&
                dost.getLooptype() != DoStatement.Type.FOR_EACH &&
                dost.getLooptype() != DoStatement.Type.INFINITE) {
              currVars.add(dost.getConditionExprent());
            }
          }
          else if (st instanceof CatchAllStatement) {
            CatchAllStatement fin = (CatchAllStatement)st;
            if (fin.isFinally() && fin.getMonitor() != null) {
              currVars.add(fin.getMonitor());
            }
          }
        }
        else if (obj instanceof Exprent) {
          currVars.add((Exprent)obj);
        }
      }

      // children statements
      for (Integer index : childVars) {
        Integer count = mapCount.get(index);
        if (count == null) {
          count = 0;
        }
        mapCount.put(index, count + 1);
      }

      condlst = getAllVars(currVars);
    }
    else {
      condlst = getAllVars(stat.getExprents());
    }

    // this statement
    for (VarExprent var : condlst) {
      mapCount.put(var.getIndex(), 2);
    }


    HashSet<Integer> set = new HashSet<>(mapCount.keySet());

    // put all variables defined in this statement into the set
    for (Entry<Integer, Integer> en : mapCount.entrySet()) {
      if (en.getValue() > 1) {
        mapVarDefStatements.put(en.getKey(), stat);
      }
    }

    mapStatementVars.put(stat.id, set);

    return set;
  }

  private static List<VarExprent> getAllVars(List<Exprent> lst) {

    List<VarExprent> res = new ArrayList<>();
    List<Exprent> listTemp = new ArrayList<>();

    for (Exprent expr : lst) {
      listTemp.addAll(expr.getAllExprents(true));
      listTemp.add(expr);
    }

    for (Exprent exprent : listTemp) {
      if (exprent instanceof VarExprent) {
        res.add((VarExprent)exprent);
      }
    }

    return res;
  }

  private boolean setDefinition(Exprent expr, int index) {
    if (expr instanceof AssignmentExprent) {
      Exprent left = ((AssignmentExprent)expr).getLeft();
      if (left instanceof VarExprent) {
        VarExprent var = (VarExprent)left;
        if (var.getIndex() == index) {
          var.setDefinition(true);
          return true;
        }
      }
    }
    return false;
  }

  private void populateTypeBounds(VarProcessor proc, Statement stat) {
    Map<VarVersionPair, VarType> mapExprentMinTypes = varproc.getVarVersions().getTypeProcessor().getMapExprentMinTypes();
    Map<VarVersionPair, VarType> mapExprentMaxTypes = varproc.getVarVersions().getTypeProcessor().getMapExprentMaxTypes();
    LinkedList<Statement> stack = new LinkedList<>();
    stack.add(root);

    while (!stack.isEmpty()) {
      Statement st = stack.removeFirst();

      if (st.getExprents() != null) {
        LinkedList<Exprent> exps = new LinkedList<>();
        exps.addAll(st.getExprents());
        while (!exps.isEmpty()) {
          Exprent exp = exps.removeFirst();

          switch (exp.type) {
            case INVOCATION:
            case FIELD:
            case EXIT:
              Exprent instance = null;
              String target = null;
              if (exp instanceof InvocationExprent) {
                instance = ((InvocationExprent)exp).getInstance();
                target = ((InvocationExprent)exp).getClassname();
              } else if (exp instanceof FieldExprent) {
                instance = ((FieldExprent)exp).getInstance();
                target = ((FieldExprent)exp).getClassname();
              } else if (exp instanceof ExitExprent) {
                ExitExprent exit = (ExitExprent)exp;
                if (exit.getExitType() == ExitExprent.Type.RETURN) {
                  instance = exit.getValue();
                  target = exit.getRetType().value;
                }
              }

              if ("java/lang/Object".equals(target))
                  continue; //This is dirty, but if we don't then too many things become object...

              if (instance != null && instance instanceof VarExprent) {
                VarVersionPair key = ((VarExprent)instance).getVarVersionPair();
                VarType newType = new VarType(CodeConstants.TYPE_OBJECT, 0, target);
                VarType oldMin = mapExprentMinTypes.get(key);
                VarType oldMax = mapExprentMaxTypes.get(key);

                /* Everything goes to Object with this... Need a better filter?
                if (!newType.equals(oldMin)) {
                  if (oldMin != null && oldMin.type == CodeConstants.TYPE_OBJECT) {
                    // If the old min is an instanceof the new target, EXA: ArrayList -> List
                    if (DecompilerContext.getStructContext().instanceOf(oldMin.value, newType.value))
                      mapExprentMinTypes.put(key, newType);
                  } else
                    mapExprentMinTypes.put(key, newType);
                }
                */

                if (!newType.equals(oldMax)) {
                  if (oldMax != null && oldMax.type == CodeConstants.TYPE_OBJECT) {
                    // If the old min is an instanceof the new target, EXA: List -> ArrayList
                    if (DecompilerContext.getStructContext().instanceOf(newType.value, oldMax.value))
                      mapExprentMaxTypes.put(key, newType);
                  } else
                    mapExprentMaxTypes.put(key, newType);
                }
              }

              break;
            default:
              exps.addAll(exp.getAllExprents());
          }
        }
      }

      stack.addAll(st.getStats());
    }
  }

  private VPPEntry mergeVars(Statement stat) {
    Map<Integer, VarVersionPair> parent = new HashMap<>(); // Always empty dua!
    MethodDescriptor md = MethodDescriptor.parseDescriptor(mt.getDescriptor());

    int index = 0;
    // this var
    if (!mt.hasModifier(CodeConstants.ACC_STATIC)) {
      parent.put(index, new VarVersionPair(index++, 0));
    }

    for (VarType var : md.params) {
      parent.put(index, new VarVersionPair(index, 0));
      index += var.stackSize;
    }

    populateTypeBounds(varproc, stat);

    Map<VarVersionPair, VarVersionPair> denylist = new HashMap<>();
    VPPEntry remap = mergeVars(stat, parent, new HashMap<>(), denylist);
    while (remap != null) {
      //System.out.println("Remapping: " + remap.getKey() + " -> " + remap.getValue());
      if (!remapVar(stat, remap.getKey(), remap.getValue())) {
        denylist.put(remap.getKey(), remap.getValue());
      }

      remap = mergeVars(stat, parent, new HashMap<>(), denylist);
    }
    return null;
  }


  private VPPEntry mergeVars(Statement stat, Map<Integer, VarVersionPair> parent, Map<Integer, VarVersionPair> leaked, Map<VarVersionPair, VarVersionPair> denylist) {
    Map<Integer, VarVersionPair> this_vars = new HashMap<>();
    if (parent.size() > 0)
      this_vars.putAll(parent);

    if (stat.getVarDefinitions().size() > 0) {
      for (int x = 0; x < stat.getVarDefinitions().size(); x++) {
        Exprent exp = stat.getVarDefinitions().get(x);
        if (exp instanceof VarExprent) {
          VarExprent var = (VarExprent)exp;
          Integer index = varproc.getVarOriginalIndex(var.getIndex());
          if (index != null) {
            if (this_vars.containsKey(index)) {
              stat.getVarDefinitions().remove(x);
              return new VPPEntry(var, this_vars.get(index));
            }
            this_vars.put(index, new VarVersionPair(var));
            leaked.put(index, new VarVersionPair(var));
          } else {
            RootStatement root = stat.getTopParent();

            root.addComment("$VF: One or more variable merging failures!", true);
          }
        }
      }
    }

    Map<Integer, VarVersionPair> scoped = null;
    switch (stat.type) { // These are the type of statements that leak vars
      case BASIC_BLOCK:
      case GENERAL:
      case ROOT:
      case SEQUENCE:
        scoped = leaked;
    }

    if (stat.getExprents() == null) {
      List<Object> objs = stat.getSequentialObjects();
      for (int i = 0; i < objs.size(); i++) {
        Object obj = objs.get(i);
        if (obj instanceof Statement) {
          Statement st = (Statement)obj;

          //Map<VarVersionPair, VarVersionPair> denylist_n = new HashMap<VarVersionPair, VarVersionPair>();
          Map<Integer, VarVersionPair> leaked_n = new HashMap<Integer, VarVersionPair>();
          VPPEntry remap = mergeVars(st, this_vars, leaked_n, denylist);

          if (remap != null) {
            return remap;
          }
          /* TODO: See if we can optimize and only go up till needed.
          while (remap != null) {
            System.out.println("Remapping: " + remap.getKey() + " -> " + remap.getValue());
            VarVersionPair var = parent.get(varproc.getRemapped(remap.getValue().var));
            if (remap.getValue().equals(var)) { //Drill up to original declaration.
              return remap;
            }
            if (!remapVar(stat, remap.getKey(), remap.getValue())) {
              denylist_n.put(remap.getKey(), remap.getValue());
            }
            leaked_n.clear();
            remap = mergeVars(st, this_vars, leaked_n, denylist_n);
          }
          */

          if (!leaked_n.isEmpty()) {
            if (stat instanceof IfStatement) {
              IfStatement ifst = (IfStatement)stat;
              if (obj == ifst.getIfstat() || obj == ifst.getElsestat()) {
                leaked_n.clear(); // Force no leaking at the end of if blocks
                // We may need to do this for Switches as well.. But havent run into that issue yet...
              }
              else if (obj == ifst.getFirst()) {
                leaked.putAll(leaked_n); //First is outside the scope so leak!
              }
            } else if (stat instanceof SwitchStatement ||
                       stat instanceof SynchronizedStatement) {
              if (obj == stat.getFirst()) {
                leaked.putAll(leaked_n); //First is outside the scope so leak!
              }
              else {
                leaked_n.clear();
              }
            }
            else if (stat instanceof CatchStatement || stat instanceof CatchAllStatement) {
              leaked_n.clear(); // Catches can't leak anything
            }
            this_vars.putAll(leaked_n);
          }
        }
        else if (obj instanceof Exprent) {
          VPPEntry ret = processExprent((Exprent)obj, this_vars, scoped, denylist);
          if (ret != null && isVarReadFirst(ret.getValue(), stat, i + 1)) {
            VarType t1 = this.varproc.getVarType(ret.getKey());
            VarType t2 = this.varproc.getVarType(ret.getValue());

            if (t1.isSuperset(t2) || t2.isSuperset(t1)) {
              return ret;
            }
          }
        }
      }
    }
    else {
      List<Exprent> exps = stat.getExprents();
      for (int i = 0; i < exps.size(); i++) {
        Exprent exp = exps.get(i);
        VPPEntry ret = processExprent(exp, this_vars, scoped, denylist);
        if (ret != null && !isVarReadFirst(ret.getValue(), stat, i + 1)) {
          // TODO: this is where seperate int and bool types are merged

          VarType t1 = this.varproc.getVarType(ret.getKey());
          VarType t2 = this.varproc.getVarType(ret.getValue());

          if (t1.isSuperset(t2) || t2.isSuperset(t1)) {
            // TODO: this only checks for totally disjoint types, there are instances where merging is incorrect with primitives

            boolean ok = true;
            if (DecompilerContext.getOption(IFernflowerPreferences.VERIFY_VARIABLE_MERGES)) {
              if (exp instanceof AssignmentExprent) {
                AssignmentExprent assign = (AssignmentExprent) exp;
                if (assign.getLeft() instanceof VarExprent) {
                  VarExprent var = (VarExprent) assign.getLeft();

                  if (var.getIndex() == ret.getKey().var) {
                    // Matched:
                    //   var<ret.key.idx> = ...

                    if (assign.getRight().containsVar(ret.getValue())) {
                      // What we're remapping to is used in the rhs!
                      // We need to iterate down the scope tree to make sure the old var isn't used anywhere else.

                      if (isVarReadRemote(identifyParent(stat), ret.getKey(), false, stat)) {
                        // The var is used elsewhere, we can't remap it
                        ok = false;
                      }
                    } else {
                      if (isVarReadRemote(identifyParent(stat), ret.getKey(), true, stat)) {
                        // The var is used elsewhere, we can't remap it
                        ok = false;
                      }
                    }
                  }
                }
              }
            }

            if (ok) {
              return ret;
            }
          }
        }
      }
    }
    return null; // We made it with no remaps!!!!!!!
  }

  private static Statement identifyParent(Statement stat) {
    Statement parent = stat.getParent();

    if (parent instanceof IfStatement || parent instanceof SwitchStatement) {
      if (parent.getBasichead() == stat) {
        return parent.getParent();
      }
    }

    // TODO: do ?

    return parent;
  }

  private static boolean isVarReadRemote(Statement stat, VarVersionPair var, boolean checkAssign, Statement... filter) {
    for (Statement st : stat.getStats()) {
      if (isVarReadRemote(st, var, checkAssign, filter)) {
        return true;
      }
    }

    if (ArrayHelper.containsByRef(filter, stat)) {
      return false;
    }

    if (stat instanceof BasicBlockStatement) {
      if (checkAssign) {
        for (Exprent ex : stat.getExprents()) {
          for (Exprent e : ex.getAllExprents(true, true)) {
            if (e instanceof AssignmentExprent) {
              AssignmentExprent assign = (AssignmentExprent)e;
              if (assign.getLeft() instanceof VarExprent) {
                VarExprent var2 = (VarExprent)assign.getLeft();
                if (var2.getIndex() == var.var) {
                  return true;
                }
              }
            }

            if (e instanceof FunctionExprent) {
              FunctionExprent func = (FunctionExprent)e;
              if (func.getFuncType().isPPMM()) {
                if (func.getLstOperands().get(0) instanceof VarExprent) {
                  VarExprent var2 = (VarExprent)func.getLstOperands().get(0);
                  if (var2.getIndex() == var.var) {
                    return true;
                  }
                }
              }
            }
          }
        }
      } else {
        for (Exprent ex : stat.getExprents()) {
          if (ex.containsVar(var)) {
            return true;
          }
        }
      }
    }


    return false;
  }

  private VPPEntry processExprent(Exprent exp, Map<Integer, VarVersionPair> this_vars, Map<Integer, VarVersionPair> leaked, Map<VarVersionPair, VarVersionPair> denylist) {
    VarExprent var = null;

    if (exp instanceof AssignmentExprent) {
      AssignmentExprent ass = (AssignmentExprent)exp;
      if (!(ass.getLeft() instanceof VarExprent)) {
        return null;
      }

      var = (VarExprent)ass.getLeft();
    }
    else if (exp instanceof VarExprent) {
      var = (VarExprent)exp;
    }

    if (var == null) {
      return null;
    }

    if (!var.isDefinition()) {
      return null;
    }

    Integer index = varproc.getVarOriginalIndex(var.getIndex());
    VarVersionPair new_ = this_vars.get(index);
    if (new_ != null) {
      VarVersionPair old = new VarVersionPair(var);
      VarVersionPair deny = denylist.get(old);
      if (deny == null || !deny.equals(new_)) {
        return new VPPEntry(var, this_vars.get(index));
      }
    }
    this_vars.put(index, new VarVersionPair(var));

    if (leaked != null) {
      leaked.put(index, new VarVersionPair(var));
    }

    return null;
  }

  private boolean remapVar(Statement stat, VarVersionPair from, VarVersionPair to) {
    if (from.equals(to))
      throw new IllegalStateException("Trying to remap var version " + from + " in statement " + stat + " to itself!");
    boolean success = false;
    if (stat.getExprents() == null) {
      for (Object obj : stat.getSequentialObjects()) {
        if (obj instanceof Statement) {
          success |= remapVar((Statement)obj, from, to);
        }
        else if (obj instanceof Exprent) {
          if (remapVar((Exprent)obj, from, to)) {
            success = true;
          }
        }
      }
    }
    else {
      boolean remapped = false;
      for (int x = 0; x < stat.getExprents().size(); x++) {
        Exprent exp = stat.getExprents().get(x);
        if (remapVar(exp, from, to)) {
          remapped = true;
          if (exp instanceof VarExprent) {
            if (!((VarExprent)exp).isDefinition()) {
              stat.getExprents().remove(x);
              x--;
            }
          }
        }
      }
      success |= remapped;
    }

    if (success) {
      Iterator<Exprent> itr = stat.getVarDefinitions().iterator();
      while (itr.hasNext()) {
        Exprent exp = itr.next();
        if (exp instanceof VarExprent) {
          VarExprent var = (VarExprent)exp;
          if (from.equals(var.getVarVersionPair())) {
            itr.remove();
          }
          else if (to.var == var.getIndex() && to.version == var.getVersion()) {
            VarType merged = getMergedType(from, to);

            if (merged == null) { // Something went wrong.. This SHOULD be non-null
              continue;
            }

            var.setVarType(merged);
          }
        }
      }
    }

    return success;
  }

  private boolean remapVar(Exprent exprent, VarVersionPair from, VarVersionPair to) {
    if (exprent == null) { // Sometimes there are null exprents?
      return false;
    }
    List<Exprent> lst = exprent.getAllExprents(true);
    lst.add(exprent);

    boolean remapped = false;

    for (Exprent expr : lst) {
      if (expr instanceof AssignmentExprent) {
        AssignmentExprent ass = (AssignmentExprent)expr;
        if (ass.getLeft() instanceof VarExprent && ass.getRight() instanceof ConstExprent) {
          VarVersionPair left = new VarVersionPair((VarExprent)ass.getLeft());
          if (!left.equals(from) && !left.equals(to)) {
            continue;
          }

          ConstExprent right = (ConstExprent)ass.getRight();
          if (right.getConstType() == VarType.VARTYPE_NULL) {
            continue;
          }
          VarType merged = getMergedType(from, to);
          if (merged == null) { // Types incompatible, do not merge
            continue;
          }

          // Merged constant assignment, attempt to set the constant type to ensure that it's correct

          VarType type = right.getConstType();

          // We can only do this if the merged type is a superset of the old type
          if (merged.isSuperset(type)) {
            right.setConstType(merged);
          }
        }
      }
      else if (expr instanceof VarExprent) {
        VarExprent var = (VarExprent)expr;
        VarVersionPair old = new VarVersionPair(var);
        if (!old.equals(from)) {
          continue;
        }
        VarType merged = getMergedType(from, to);
        if (merged == null) { // Types incompatible, do not merge
          continue;
        }

        var.setIndex(to.var);
        var.setVersion(to.version);
        var.setVarType(merged);
        if (var.isDefinition()) {
          var.setDefinition(false);
        }
        varproc.setVarType(to, merged);
        remapped = true;
      }
    }
    return remapped;
  }

  private VarType getMergedType(VarVersionPair from, VarVersionPair to) {
    Map<VarVersionPair, VarType> minTypes = varproc.getVarVersions().getTypeProcessor().getMapExprentMinTypes();
    Map<VarVersionPair, VarType> maxTypes = varproc.getVarVersions().getTypeProcessor().getMapExprentMaxTypes();

    return getMergedType(minTypes.get(from), minTypes.get(to), maxTypes.get(from), maxTypes.get(to));
  }

  private VarType getMergedType(VarType fromMin, VarType toMin, VarType fromMax, VarType toMax) {
    if (fromMin != null && fromMin.equals(toMin)) {
      return fromMin; // Short circuit this for simplicities sake
    }
    VarType type = fromMin == null ? toMin : (toMin == null ? fromMin : VarType.getCommonSupertype(fromMin, toMin));
    if (type == null || fromMin == null || toMin == null) {
      return null; // no common supertype, skip the remapping
    }
    if (type.type == CodeConstants.TYPE_OBJECT) {
      if (toMax != null) { // The target var is used in direct invocations
        if (fromMax != null) {
          // Max types are the highest class that this variable is used as a direct instance of without any casts.
          // This will pull up the to var type if the from requires a higher class type.
          // EXA: Collection -> List
          if (DecompilerContext.getStructContext().instanceOf(fromMax.value, toMax.value))
            return fromMax;
        } else if (fromMin != null) {
          // Pull to up to from: List -> ArrayList
          if (DecompilerContext.getStructContext().instanceOf(fromMin.value, toMax.value))
            return fromMin;
        }
      } else if (toMin != null) {
        if (fromMax != null) {
          if (DecompilerContext.getStructContext().instanceOf(fromMax.value, toMin.value))
            return fromMax;
        } else if (fromMin != null) {
          if (DecompilerContext.getStructContext().instanceOf(toMin.value, fromMin.value))
            return toMin;
        }
      }
      return null;
    } else {

      // Special case: merging from false boolean to integral type, should be correct always
      if (toMin.typeFamily == CodeConstants.TYPE_FAMILY_INTEGER && fromMin.isFalseBoolean()) {
        return toMin;
      }

      // Both nonnull at this point
      if (!fromMin.isStrictSuperset(toMin)) {
        // If type we're merging into the old type isn't a strict superset of the old type, we cannot merge
        return null;
      }

      return type;
    }
  }

  private void propogateLVTs(Statement stat) {
    MethodDescriptor md = MethodDescriptor.parseDescriptor(mt.getDescriptor());
    Map<VarVersionPair, VarInfo> types = new LinkedHashMap<>();

    if (varproc.hasLVT()) {
      int index = 0;
      if (!mt.hasModifier(CodeConstants.ACC_STATIC)) {
        List<LocalVariable> lvt = varproc.getCandidates(index); // Some enums give incomplete lvts?
        if (lvt != null && lvt.size() > 0) {
          types.put(new VarVersionPair(index, 0), new VarInfo(lvt.get(0), null));
        }
        index++;
      }

      for (VarType var : md.params) {
        List<LocalVariable> lvt = varproc.getCandidates(index); // Some enums give incomplete lvts?
        if (lvt != null && lvt.size() > 0) {
          types.put(new VarVersionPair(index, 0), new VarInfo(lvt.get(0), null));
        }
        index += var.stackSize;
      }
    }

    findTypes(stat, types);

    Map<VarVersionPair,String> typeNames = new LinkedHashMap<VarVersionPair,String>();
    for (Entry<VarVersionPair, VarInfo> e : types.entrySet()) {
      typeNames.put(e.getKey(), e.getValue().getCast());
    }

    Map<VarVersionPair, String> renames = this.mt.getVariableNamer().rename(typeNames);

    // Stuff the parent context into enclosed child methods
    StatementIterator.iterate(root, (exprent) -> {
      List<StructMethod> methods = new ArrayList<>();
      if (exprent instanceof VarExprent) {
        VarExprent var = (VarExprent)exprent;
        if (var.isClassDef()) {
          ClassNode child = DecompilerContext.getClassProcessor().getMapRootClasses().get(var.getVarType().value);
          if (child != null)
            methods.addAll(child.classStruct.getMethods());
        }
      }
      else if (exprent instanceof NewExprent) {
        NewExprent _new = (NewExprent)exprent;
        if (_new.isAnonymous()) { //TODO: Check for Lambda here?
          ClassNode child = DecompilerContext.getClassProcessor().getMapRootClasses().get(_new.getNewType().value);
          if (child != null) {
            if (_new.isLambda()) {
              if (child.lambdaInformation.is_method_reference) {
                //methods.add(child.getWrapper().getClassStruct().getMethod(child.lambdaInformation.content_method_key));
              } else {
                methods.add(child.classStruct.getMethod(child.lambdaInformation.content_method_name, child.lambdaInformation.content_method_descriptor));
              }
            } else {
              methods.addAll(child.classStruct.getMethods());
            }
          }
        }
      }

      for (StructMethod meth : methods) {
        meth.getVariableNamer().addParentContext(VarDefinitionHelper.this.mt.getVariableNamer());
      }
      return 0;
    });

    Map<VarVersionPair, LocalVariable> lvts = new HashMap<>();

    for (Entry<VarVersionPair, VarInfo> e : types.entrySet()) {
      VarVersionPair idx = e.getKey();
      // skip this. we can't rename it
      if (idx.var == 0 && !mt.hasModifier(CodeConstants.ACC_STATIC)) {
        continue;
      }
      LocalVariable lvt = e.getValue().getLVT();
      String rename = renames == null ? null : renames.get(idx);

      if (rename != null) {
        varproc.setVarName(idx, rename);
      }

      if (lvt != null) {
        if (rename != null) {
          lvt = lvt.rename(rename);
        }
        varproc.setVarLVT(idx, lvt);
        lvts.put(idx, lvt);
      }
    }


    applyTypes(stat, lvts);
  }

  private void findTypes(Statement stat, Map<VarVersionPair, VarInfo> types) {
    if (stat == null) {
      return;
    }

    for (Exprent exp : stat.getVarDefinitions()) {
      findTypes(exp, types);
    }

    if (stat.getExprents() == null) {
      for (Object obj : stat.getSequentialObjects()) {
        if (obj instanceof Statement) {
          findTypes((Statement)obj, types);
        }
        else if (obj instanceof Exprent) {
          findTypes((Exprent)obj, types);
        }
      }
    }
    else {
      for (Exprent exp : stat.getExprents()) {
        findTypes(exp, types);
      }
    }
  }

  private void findTypes(Exprent exp, Map<VarVersionPair, VarInfo> types) {
    List<Exprent> lst = exp.getAllExprents(true);
    lst.add(exp);

    for (Exprent exprent : lst) {
      if (exprent instanceof VarExprent) {
        VarExprent var = (VarExprent)exprent;
        VarVersionPair ver = new VarVersionPair(var);
        if (var.isDefinition()) {
          types.put(ver, new VarInfo(var.getLVT(), var.getVarType()));
        } else {
          VarInfo existing = types.get(ver);
          if (existing == null)
            existing = new VarInfo(var.getLVT(), var.getVarType());
          else if (existing.getLVT() == null && var.getLVT() != null)
            existing = new VarInfo(var.getLVT(), existing.getType());
          types.put(ver, existing);
        }
      }
    }
  }

  private void applyTypes(Statement stat, Map<VarVersionPair, LocalVariable> types) {
    if (stat == null || types.size() == 0) {
      return;
    }

    for (Exprent exp : stat.getVarDefinitions()) {
      applyTypes(exp, types);
    }

    if (stat.getExprents() == null) {
      for (Object obj : stat.getSequentialObjects()) {
        if (obj instanceof Statement) {
          applyTypes((Statement)obj, types);
        }
        else if (obj instanceof Exprent) {
          applyTypes((Exprent)obj, types);
        }
      }
    }
    else {
      for (Exprent exp : stat.getExprents()) {
        applyTypes(exp, types);
      }
    }
  }

  private void applyTypes(Exprent exprent, Map<VarVersionPair, LocalVariable> types) {
    if (exprent == null) {
      return;
    }
    List<Exprent> lst = exprent.getAllExprents(true);
    lst.add(exprent);

    for (Exprent expr : lst) {
      if (expr instanceof VarExprent) {
        VarExprent var = (VarExprent)expr;
        LocalVariable lvt = types.get(new VarVersionPair(var));
        if (lvt != null) {
          var.setLVT(lvt);
        } else {
          System.currentTimeMillis();
        }
      }
    }
  }

  //Helper classes because Java is dumb and doesn't have a Pair<K,V> class
  private static class SimpleEntry<K, V> implements Entry<K, V> {
    private K key;
    private V value;
    public SimpleEntry(K key, V value) {
      this.key = key;
      this.value = value;
    }
    @Override public K getKey() { return key; }
    @Override public V getValue() { return value; }
    @Override
    public V setValue(V value) {
      V tmp = this.value;
      this.value = value;
      return tmp;
    }
  }
  private static class VPPEntry extends SimpleEntry<VarVersionPair, VarVersionPair> {
    private VPPEntry(VarExprent key, VarVersionPair value) {
        super(new VarVersionPair(key), value);
    }
  }

  private static class VarInfo {
    private LocalVariable lvt;
    private String cast;
    private VarType type;

    private VarInfo(LocalVariable lvt, VarType type) {
      if (lvt != null && lvt.getSignature() != null)
        this.cast = ExprProcessor.getCastTypeName(GenericType.parse(lvt.getSignature()), false);
      else if (lvt != null)
        this.cast = ExprProcessor.getCastTypeName(lvt.getVarType(), false);
      else if (type != null)
        this.cast = ExprProcessor.getCastTypeName(type, false);
      else
        this.cast = "this";
      this.lvt = lvt;
      this.type = type;
    }

    public LocalVariable getLVT() {
      return this.lvt;
    }

    public String getCast() {
      return this.cast;
    }

    public VarType getType() {
      return this.type;
    }
  }

  private static boolean isVarReadFirst(VarVersionPair var, Statement stat, int index, VarExprent... allowlist) {
    if (stat.getExprents() == null) {
      List<Object> objs = stat.getSequentialObjects();
      for (int x = index; x < objs.size(); x++) {
        Object obj = objs.get(x);
        if (obj instanceof Statement) {
          if (isVarReadFirst(var, (Statement)obj, 0, allowlist)) {
            return true;
          }
        } else if (obj instanceof Exprent) {
          if (isVarReadFirst(var, (Exprent)obj, allowlist)) {
            return true;
          }
        }
      }
    } else {
      for (int x = index; x < stat.getExprents().size(); x++) {
        if (isVarReadFirst(var, stat.getExprents().get(x), allowlist)) {
          return true;
        }
      }
    }
    return false;
  }

  private static boolean isVarReadFirst(VarVersionPair target, Exprent exp, VarExprent... allowlist) {
    AssignmentExprent assign = exp instanceof AssignmentExprent ? (AssignmentExprent)exp : null;
    FunctionExprent func = exp instanceof FunctionExprent ? (FunctionExprent)exp : null;

    if (func != null && !func.getFuncType().isPPMM()) {
      func = null;
    }

    List<Exprent> lst = exp.getAllExprents(true, true);

    for (Exprent ex : lst) {
      if (ex instanceof VarExprent) {
        VarExprent var = (VarExprent)ex;
        if (var.getIndex() == target.var && var.getVersion() == target.version) {
          boolean allowed = false;

          if (assign != null) {
            if (var == assign.getLeft()) {
              allowed = true;
            }
          }

          if (func != null) {
            if (var == func.getLstOperands().get(0)) {
              allowed = true;
            }
          }

          for (VarExprent allow : allowlist) {
            if (var == allow) {
              allowed = true;
            }
          }

          if (!allowed) {
            return true;
          }
        }
      }
    }

    return false;
  }

  private void setNonFinal(Statement stat, Set<VarVersionPair> unInitialized) {
    if (stat.getExprents() != null && !stat.getExprents().isEmpty()) {
      for (Exprent exp : stat.getExprents()) {
        if (exp instanceof VarExprent) {
          unInitialized.add(new VarVersionPair((VarExprent)exp));
        }
        else {
          setNonFinal(exp, unInitialized);
        }
      }
    }

    if (!stat.getVarDefinitions().isEmpty()) {
      if (stat instanceof DoStatement) {
        for (Exprent var : stat.getVarDefinitions()) {
          unInitialized.add(new VarVersionPair((VarExprent)var));
        }
      }
    }

    if (stat instanceof DoStatement) {
      DoStatement dostat = (DoStatement)stat;
      if (dostat.getInitExprentList() != null) {
        setNonFinal(dostat.getInitExprent(), unInitialized);
      }
      if (dostat.getIncExprentList() != null) {
        setNonFinal(dostat.getIncExprent(), unInitialized);
      }
    }
    else if (stat instanceof IfStatement) {
      IfStatement ifstat = (IfStatement)stat;
      if (ifstat.getIfstat() != null && ifstat.getElsestat() != null) {
        setNonFinal(ifstat.getFirst(), unInitialized);
        setNonFinal(ifstat.getIfstat(), new HashSet<>(unInitialized));
        setNonFinal(ifstat.getElsestat(), unInitialized);
        return;
      }
    }

    for (Statement st : stat.getStats()) {
      setNonFinal(st, unInitialized);
    }
  }

  private void setNonFinal(Exprent exp, Set<VarVersionPair> unInitialized) {
    VarExprent var = null;

    if (exp == null) {
      return;
    }

    if (exp instanceof AssignmentExprent) {
      AssignmentExprent assign = (AssignmentExprent)exp;
      if (assign.getLeft() instanceof VarExprent) {
        var = (VarExprent)assign.getLeft();
      }
    }
    else if (exp instanceof FunctionExprent) {
      FunctionExprent func = (FunctionExprent)exp;
      if (func.getFuncType().isPPMM()) {
        if (func.getLstOperands().get(0) instanceof VarExprent) {
          var = (VarExprent)func.getLstOperands().get(0);
        }
      }
    }

    if (var != null && !var.isDefinition() && !unInitialized.remove(var.getVarVersionPair())) {
      var.getProcessor().setVarFinal(var.getVarVersionPair(), FinalType.NON_FINAL);
    }

    for (Exprent ex : exp.getAllExprents()) {
      setNonFinal(ex, unInitialized);
    }
  }

  public void remapClashingNames(Statement root) {
    Map<Statement, Set<VarVersionPair>> varDefinitions = new HashMap<>();
    Set<VarVersionPair> liveVarDefs = new HashSet<>();
    Map<VarVersionPair, String> nameMap = new HashMap<>();
    nameMap.putAll(this.varproc.getInheritedNames());

    StructMethodParametersAttribute paramAttribute = ((RootStatement) root).mt.getAttribute(StructGeneralAttribute.ATTRIBUTE_METHOD_PARAMETERS);
    StructLocalVariableTableAttribute lvtAttribute = ((RootStatement) root).mt.getAttribute(StructGeneralAttribute.ATTRIBUTE_LOCAL_VARIABLE_TABLE);
    if (paramAttribute != null && lvtAttribute != null) {
      for (StructMethodParametersAttribute.Entry entry : paramAttribute.getEntries()) {
        // for every method param, find lvt entry and put it into the name map
        Optional<LocalVariable> lvtEntry = lvtAttribute.getVariables().filter(s -> s.getName().equals(entry.myName)).findFirst();
        lvtEntry.ifPresent(localVariable -> nameMap.put(localVariable.getVersion(), entry.myName));
      }
    }

    iterateClashingNames(root, varDefinitions, liveVarDefs, nameMap);
  }

  private void iterateClashingNames(Statement stat, Map<Statement, Set<VarVersionPair>> varDefinitions, Set<VarVersionPair> liveVarDefs, Map<VarVersionPair, String> nameMap) {
    Set<VarVersionPair> curVarDefs = new HashSet<>();

    boolean shouldRemoveAtEnd = false;

    if (stat.getExprents() != null) {
      List<Exprent> exprents = new ArrayList<>(stat.getExprents());

      for (Exprent exprent : stat.getExprents()) {
        exprents.addAll(exprent.getAllExprents(true));
      }

      for (Exprent exprent : exprents) {
        iterateClashingExprent(stat, varDefinitions, exprent, curVarDefs, nameMap);
      }
    } else {
      // Process var definitions in statement head
      for (Object obj : stat.getSequentialObjects()) {
        if (obj instanceof Exprent) {
          List<Exprent> exprents = ((Exprent) obj).getAllExprents(true, true);

          for (Exprent exprent : exprents) {
            iterateClashingExprent(stat, varDefinitions, exprent, curVarDefs, nameMap);
          }
        }
      }

      shouldRemoveAtEnd = true;
    } // TODO: consider var defs as owned by parent, or replace shouldRemoveAtEnd with set

    liveVarDefs.addAll(curVarDefs);
    varDefinitions.put(stat, curVarDefs);

    boolean iterate = true;
    if (stat instanceof SwitchStatement) {
      SwitchStatement switchStat = (SwitchStatement)stat;
      // Phantom switch statements don't need variable remapping as switch expressions have isolated branches

      if (switchStat.isPhantom()) {
        iterate = false;
      }
    }

    List<Statement> deferred = new ArrayList<>();
    if (iterate) {
      for (Statement st : stat.getStats()) {
        if (stat instanceof IfStatement) {
          IfStatement ifstat = (IfStatement)stat;

          if (ifstat.getElsestat() == st) {
            // Defer else blocks of if statements, as they are independent from the context of the if block
            deferred.add(st);
            continue;
          }
        }

        iterateClashingNames(st, varDefinitions, liveVarDefs, nameMap);
      }
    }

    if (shouldRemoveAtEnd) {
      clearStatement(varDefinitions, liveVarDefs, nameMap, stat);
    }

    for (Statement st : new HashSet<>(varDefinitions.keySet())) {
      // TODO: consider first statements as owned by the grandparent
      if (st.getParent() == stat) {
        clearStatement(varDefinitions, liveVarDefs, nameMap, st);
      }
    }

    // Process deferred statements
    if (iterate) {
      for (Statement st : deferred) {
        iterateClashingNames(st, varDefinitions, liveVarDefs, nameMap);
      }
    }

    for (Statement st : new HashSet<>(varDefinitions.keySet())) {
      if (st.getParent() == stat && deferred.contains(st)) {
        clearStatement(varDefinitions, liveVarDefs, nameMap, st);
      }
    }
  }

  private void clearStatement(Map<Statement, Set<VarVersionPair>> varDefinitions, Set<VarVersionPair> liveVarDefs, Map<VarVersionPair, String> nameMap, Statement st) {
    Set<VarVersionPair> removed = varDefinitions.remove(st);
    liveVarDefs.removeAll(removed);

    for (VarVersionPair vvp : removed) {
      nameMap.remove(vvp);
    }
  }

  private void iterateClashingExprent(Statement stat, Map<Statement, Set<VarVersionPair>> varDefinitions, Exprent exprent, Set<VarVersionPair> curVarDefs, Map<VarVersionPair, String> nameMap) {
    if (exprent instanceof VarExprent) {
      VarExprent var = (VarExprent) exprent;

      if (var.isDefinition()) {
        curVarDefs.add(var.getVarVersionPair());

        // Only process vars that have lvt as the default var<index>_<version> names can never conflict
        if (var.getLVT() != null || this.varproc.getVarName(var.getVarVersionPair()) != null) {
          String name = var.getLVT() == null ? this.varproc.getVarName(var.getVarVersionPair()) : var.getLVT().getName();

          String originalName = name;

          while (nameMap.containsValue(name)) {
            name = name + "x";
          }

          boolean scopedSwitch = false;
          if (!originalName.equals(name)) {
            // Try to scope switch statements if possible as it's a less destructive operation when considering local variable names
            Statement parent = directParent(stat);
            if (parent instanceof SwitchStatement) {
              Set<VarVersionPair> sameVarName = new HashSet<>();

              // Find vars with the same name
              for (Entry<VarVersionPair, String> entry : nameMap.entrySet()) {
                if (entry.getValue().equals(originalName)) {
                  sameVarName.add(entry.getKey());
                }
              }

              SwitchStatement switchStat = (SwitchStatement)parent;
              // Iterate through all cases
              for (Statement st : switchStat.getCaseStatements()) {
                Set<VarVersionPair> caseVarDefs = varDefinitions.get(st);

                // Check if the case branch has var defs
                if (caseVarDefs != null) {
                  for (VarVersionPair pair : sameVarName) {
                    // Try to find var defs
                    if (caseVarDefs.contains(pair)) {
                      switchStat.scopeCaseStatement(st);
                      // Try to find the case statement that the current statement belongs to
                      Statement foundCase = findCaseOwning(stat, switchStat);

                      // If found, scope the current statement
                      if (foundCase != null) {
                        switchStat.scopeCaseStatement(foundCase);
                      }

                      // scoped switch, don't remap
                      scopedSwitch = true;
                    }
                  }
                }
              }
            }

            if (!scopedSwitch) {
              // Remapped name
              this.clashingNames.put(var.getVarVersionPair(), name);
            }
          }

          // Record the changed name if we didn't scope switch
          nameMap.put(var.getVarVersionPair(), scopedSwitch ? originalName : name);
        }
      }
    }

    // TODO: Run for lambdas with context of current statement, as their wrappers should be initialized at this point
    // For lambdas, account for standard varversion names in addition to lvt
  }

  // Finds the case statement that the given statement belongs to
  private static Statement findCaseOwning(Statement stat, SwitchStatement switchStat) {
    for (Statement caseStatement : switchStat.getCaseStatements()) {
      if (caseStatement.containsStatement(stat)) {
        return caseStatement;
      }
    }

    return null;
  }

  // Finds the owner of a statement, skipping if statement first statements as they are placed above the actual if statement
  private static Statement directParent(Statement stat) {
    Statement parent = stat.getParent();

    while (parent != null && (parent instanceof SequenceStatement || (parent.getFirst() == stat && (parent instanceof IfStatement)))) {
      parent = parent.getParent();
    }

    return parent;
  }

  public Map<VarVersionPair, String> getClashingNames() {
    return clashingNames;
  }
}
