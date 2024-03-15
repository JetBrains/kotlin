package org.jetbrains.java.decompiler.modules.decompiler;

import org.jetbrains.java.decompiler.code.CodeConstants;
import org.jetbrains.java.decompiler.main.DecompilerContext;
import org.jetbrains.java.decompiler.main.collectors.CounterContainer;
import org.jetbrains.java.decompiler.main.extern.IFernflowerPreferences;
import org.jetbrains.java.decompiler.modules.decompiler.exps.*;
import org.jetbrains.java.decompiler.modules.decompiler.stats.*;
import org.jetbrains.java.decompiler.struct.consts.PooledConstant;
import org.jetbrains.java.decompiler.struct.consts.PrimitiveConstant;
import org.jetbrains.java.decompiler.struct.gen.FieldDescriptor;
import org.jetbrains.java.decompiler.struct.gen.VarType;
import org.jetbrains.java.decompiler.util.Pair;

import java.util.*;

public final class SwitchPatternMatchProcessor {
  public static boolean processPatternMatching(Statement root) {
    boolean ret = processPatternMatchingRec(root, root);

    if (ret) {
      SequenceHelper.condenseSequences(root);
    }

    return ret;
  }

  private static boolean processPatternMatchingRec(Statement stat, Statement root) {
    ValidationHelper.validateStatement((RootStatement) root);

    boolean ret = false;
    for (Statement st : new ArrayList<>(stat.getStats())) {
      ret |= processPatternMatchingRec(st, root);
      ValidationHelper.validateStatement((RootStatement) root);
    }

    if (stat instanceof SwitchStatement) {
      ret |= processStatement((SwitchStatement) stat, root);
      ValidationHelper.validateStatement((RootStatement) root);
    }

    return ret;
  }

  private static boolean processStatement(SwitchStatement stat, Statement root) {
    if (stat.isPhantom()) {
      return false;
    }

    SwitchHeadExprent head = (SwitchHeadExprent) stat.getHeadexprent();

    boolean switchPatternMatch = isSwitchPatternMatch(head);

    if (!switchPatternMatch) {
      return false;
    }

    // Found switch pattern match, start applying basic transformations
    // replace `SwitchBootstraps.typeSwitch<...>(o, idx)` with `o`
    // if `idx` is used in one place, there's no guards and we can quickly remove it
    // otherwise, we need to look at every usage and eliminate guards
    InvocationExprent value = (InvocationExprent) head.getValue();
    List<Exprent> origParams = value.getLstParameters();
    Exprent realSelector = origParams.get(0);
    boolean guarded = true;
    boolean isEnumSwitch = value.getName().equals("enumSwitch");
    List<Pair<Statement, Exprent>> references = new ArrayList<>();
    if (origParams.get(1) instanceof VarExprent) {
      VarExprent var = (VarExprent) origParams.get(1);
      SwitchHelper.findExprents(root, Exprent.class, var::isVarReferenced, false, (st, expr) -> references.add(Pair.of(st, expr)));
      // If we have one reference...
      if (references.size() == 1) {
        // ...and its just assignment...
        Pair<Statement, Exprent> ref = references.get(0);
        if (ref.b instanceof AssignmentExprent) {
          // ...remove the variable
          ref.a.getExprents().remove(ref.b);
          guarded = false;
        }
      }
    }

    Map<List<Exprent>, Exprent> guards = new HashMap<>(0);
    if (guarded) {
      guards = new HashMap<>(references.size());
      // remove the initial assignment to 0
      boolean canEliminate = true;
      Pair<Statement, Exprent> initialUse = references.get(0);
      if (initialUse.b instanceof AssignmentExprent && ((AssignmentExprent) initialUse.b).getRight() instanceof ConstExprent) {
        ConstExprent constExprent = (ConstExprent) ((AssignmentExprent) initialUse.b).getRight();
        if (constExprent.getConstType().typeFamily == CodeConstants.TYPE_FAMILY_INTEGER && constExprent.getIntValue() == 0) {
          references.remove(0);
        } else {
          return false;
        }
      } else {
        return false;
      }
      // check every assignment of `idx`
      for (Pair<Statement, Exprent> reference : references) {
        canEliminate &= eliminateGuardRef(stat, guards, reference, true);
      }
      if (!canEliminate) {
        return false;
      }
      initialUse.a.getExprents().remove(initialUse.b);
      for (Pair<Statement, Exprent> reference : references) {
        eliminateGuardRef(stat, guards, reference, false);
      }
    }

    for (int i = 0; i < stat.getCaseStatements().size(); i++) {
      Statement caseStat = stat.getCaseStatements().get(i);

      List<Exprent> allCases = stat.getCaseValues().get(i);
      Exprent caseExpr = allCases.get(0);

      // null expression = default case, can't be shared with patterns
      if (caseExpr == null) {
        continue;
      }

      if (guards.containsKey(allCases)) {
        // add the guard to the same index as this case, padding the list with nulls as necessary
        while (stat.getCaseGuards().size() <= i) {
          stat.getCaseGuards().add(null);
        }
        stat.getCaseGuards().set(i, guards.get(allCases));
      }
      if (caseExpr instanceof ConstExprent) {
        int caseValue = ((ConstExprent) caseExpr).getIntValue();

        // -1 always means null
        if (caseValue == -1) {
          allCases.remove(caseExpr);
          ConstExprent nullConst = new ConstExprent(VarType.VARTYPE_NULL, null, null);
          // null can be shared with a pattern or default; put it at the end, but before default, to make sure it doesn't get
          // absorbed by the default or overwritten by a pattern
          if (allCases.contains(null)) {
            allCases.add(allCases.indexOf(null), nullConst);
          } else {
            allCases.add(nullConst);
          }
        }
      }

      // find the pattern variable assignment
      if (caseStat instanceof SequenceStatement) {
        Statement oldStat = caseStat;
        caseStat = caseStat.getStats().get(0);
        // we can end up with a SequenceStatement with 1 statement from guard `if` elimination, eliminate the sequence entirely
        if (oldStat.getStats().size() == 1) {
          oldStat.replaceWith(caseStat);
        }
      }
      // make instanceof from assignment
      BasicBlockStatement caseStatBlock = caseStat.getBasichead();
      if (caseStatBlock.getExprents().size() >= 1) {
        Exprent expr = caseStatBlock.getExprents().get(0);
        if (expr instanceof AssignmentExprent) {
          AssignmentExprent assign = (AssignmentExprent) expr;

          if (assign.getLeft() instanceof VarExprent) {
            VarExprent var = (VarExprent) assign.getLeft();

            if (assign.getRight() instanceof FunctionExprent && ((FunctionExprent) assign.getRight()).getFuncType() == FunctionExprent.FunctionType.CAST) {
              FunctionExprent cast = (FunctionExprent) assign.getRight();

              List<Exprent> operands = new ArrayList<>();
              operands.add(cast.getLstOperands().get(0)); // checking var
              operands.add(cast.getLstOperands().get(1)); // type
              operands.add(var); // pattern match var

              FunctionExprent func = new FunctionExprent(FunctionExprent.FunctionType.INSTANCEOF, operands, null);

              caseStatBlock.getExprents().remove(0);

              // TODO: ssau representation
              // any shared nulls will be at the end, and patterns & defaults can't be shared, so its safe to overwrite whatever's here
              allCases.set(0, func);
            }
          }
        }
      }
    }

    // go through bootstrap arguments to ensure types are correct & add enum/integer/string constants
    for (int i = 0; i < value.getBootstrapArguments().size(); i++) {
      PooledConstant bsa = value.getBootstrapArguments().get(i);
      // replace the constant with the value of i, which may not be at index i
      int replaceIndex = i;
      for (List<Exprent> caseValueSet : stat.getCaseValues()) {
        if (caseValueSet.get(0) instanceof ConstExprent) {
          ConstExprent constExpr = (ConstExprent) caseValueSet.get(0);
          if (constExpr.getValue() instanceof Integer && (Integer) constExpr.getValue() == i) {
            replaceIndex = stat.getCaseValues().indexOf(caseValueSet);
          }
        }
      }
      // either an integer, String, or Class
      if (bsa instanceof PrimitiveConstant) {
        PrimitiveConstant p = (PrimitiveConstant) bsa;
        Exprent newValue = null;
        switch (p.type) {
          case CodeConstants.CONSTANT_Integer:
            newValue = new ConstExprent((Integer) p.value, false, null);
            break;
          case CodeConstants.CONSTANT_String:
            if (isEnumSwitch) {
              String typeName = realSelector.getExprType().value;
              newValue = new FieldExprent(p.value.toString(), typeName, true, null, FieldDescriptor.parseDescriptor("L" + typeName + ";"), null, false, false);
            } else {
              newValue = new ConstExprent(VarType.VARTYPE_STRING, p.value, null);
            }
            break;
          case CodeConstants.CONSTANT_Class:
            // may happen if the switch head is a supertype of the pattern
            if (stat.getCaseValues().get(replaceIndex).stream().allMatch(x -> x instanceof ConstExprent)) {
              VarType castType = new VarType(CodeConstants.TYPE_OBJECT, 0, (String) p.value);
              List<Exprent> operands = new ArrayList<>();
              operands.add(realSelector); // checking var
              operands.add(new ConstExprent(castType, null, null)); // type
              operands.add(new VarExprent(DecompilerContext.getCounterContainer().getCounterAndIncrement(CounterContainer.VAR_COUNTER),
                castType,
                DecompilerContext.getVarProcessor()));
              newValue = new FunctionExprent(FunctionExprent.FunctionType.INSTANCEOF, operands, null);
            }
            break;
        }
        if (newValue != null) {
          int ix = i;
          Exprent nvx = newValue;
          // make sure we replace the right constant, null can be shared with anything
          stat.getCaseValues().get(replaceIndex).replaceAll(u ->
            u instanceof ConstExprent
            && u.getExprType().typeFamily == CodeConstants.TYPE_FAMILY_INTEGER
            && ((ConstExprent) u).getIntValue() == ix
              ? nvx : u);
        }
      }
    }

    head.setValue(realSelector); // SwitchBootstraps.typeSwitch(o, var1) -> o

    if (guarded && stat.getParent() instanceof DoStatement) {
      // remove the enclosing while(true) loop of a guarded switch
      stat.getParent().replaceWith(stat);
      // FIXME: this replacement code looks wrong,
      //  doesn't get any coverage in tests
      // update continue-loops into break-switches
      for (StatEdge edge : stat.getPredecessorEdges(StatEdge.TYPE_CONTINUE)) {
        edge.changeType(StatEdge.TYPE_BREAK);
      }
    }

    return true;
  }

  private static boolean eliminateGuardRef(SwitchStatement stat, Map<List<Exprent>, Exprent> guards, Pair<Statement, Exprent> reference, boolean simulate) {
    // a guard takes the form of exactly
    // `if (!guardCond) { idx = __thisIdx + 1; break; }`
    // at the start of that branch
    // alternatively, it can be inverted as `if (guardCond) { /* regular case code... */ break; } idx = __thisIdx + 1;`
    if (reference.b instanceof AssignmentExprent) {
      Statement assignStat = reference.a;
      // Note: This can probably be checked earlier
      if (assignStat.getAllPredecessorEdges().size() > 1) {
        return false;
      }
      // check if the assignment follows the guard layout
      Statement parent = assignStat.getParent();
      // sometimes the assignment is after the `if` and it's condition is inverted [see TestSwitchPatternMatchingInstanceof1/2/3]
      boolean invert = true;
      if (parent instanceof SequenceStatement && parent.getStats().size() == 2 && parent.getStats().get(1) == assignStat) {
        parent = parent.getStats().get(0);
        invert = false;
      }
      // the assignment should be alone in a basic block, contained in an `if`, contained in a sequence, within the `switch`
      if (assignStat instanceof BasicBlockStatement
          && assignStat.getExprents().size() == 1
          && parent instanceof IfStatement
          && ((IfStatement) parent).iftype == IfStatement.IFTYPE_IF
          && ((IfStatement) parent).getIfstat() != null
          && parent.getParent() instanceof SequenceStatement
          && parent.getParent().getParent() == stat) {
        StatEdge continueEdge = assignStat.getSuccessorEdges(StatEdge.TYPE_CONTINUE).get(0);
        Statement next = continueEdge.getDestination();
        if (next == stat.getParent()) {
          IfStatement guardIf = (IfStatement) parent;
          // the condition of the `if` is the guard condition (usually inverted)
          Exprent guardExprent = guardIf.getHeadexprent().getCondition();
          // find which case branch we're in (to assign the guard to)
          List<Statement> caseStatements = stat.getCaseStatements();
          for (int i = 0; i < caseStatements.size(); i++) {
            if (caseStatements.get(i).containsStatement(reference.a)) {
              if (simulate) {
                // we're not actually removing the guard yet
                return true;
              }
              // the assignment of the pattern variable may be inside the `if`, take it out and add it to the next statement
              List<Exprent> guardExprs = guardIf.getStats().get(0).getExprents();
              // the assignment might also just not exist, if the switch head is a supertype of the pattern
              List<Exprent> carryExprs = guardExprs.size() > 0 ? Collections.singletonList(guardExprs.get(0)) : Collections.emptyList();

              // remove the continue edge
              continueEdge.remove();

              // eliminate the guard `if`
              guardIf.getParent().getStats().remove(0);

              Statement nextStat;


              if (invert) {
                // normally the guard condition is inverted, re-invert it here
                guardExprent = new FunctionExprent(FunctionExprent.FunctionType.BOOL_NOT, guardExprent, guardExprent.bytecode);
                nextStat = guardIf.getParent().getStats().get(0);
              } else {
                nextStat = guardIf.getIfstat();
                // remove assignment
                guardIf.getParent().getStats().remove(0);
                guardIf.getParent().getStats().add(nextStat);
              }

              guards.put(stat.getCaseValues().get(i), guardExprent);
              guardIf.getParent().setFirst(nextStat);
              for (StatEdge edge : nextStat.getAllPredecessorEdges()) {
                edge.remove();
              }
              // add the pattern variable assignment (or case statement for inverted cases) to next statement
              nextStat.getBasichead().getExprents().addAll(0, carryExprs);
              return true;
            }
          }
        }
      } else if (parent == stat) {
        // an `&& false` guard leaves us with nothing but an assignment and break
        // get the branch we're in
        List<Statement> caseStatements = stat.getCaseStatements();
        for (int i = 0; i < caseStatements.size(); i++) {
          if (caseStatements.get(i).containsStatement(reference.a)) {
            if (simulate) {
              return true;
            }
            guards.put(stat.getCaseValues().get(i), new ConstExprent(0, true, null));
            Statement replaced = reference.a.replaceWithEmpty();
            replaced.getFirstSuccessor().remove();
            Set<StatEdge> labelEdges = stat.getParent().getLabelEdges();

            // This block is technically unreachable, but most code doesn't
            // really handle that. So a break edge, mirroring the others
            // is added
            boolean multipleSuccessors = false;
            Statement target = null;
            for (StatEdge edge : labelEdges) {
              if (edge.getType() != StatEdge.TYPE_BREAK) {
                continue;
              }

              if (target == null) {
                target = edge.getDestination();
              } else if (edge.getDestination() != target) {
                // inconsistent break targets
                multipleSuccessors = true;
                break;
              }
            }

            if (target != null && !multipleSuccessors) {
              // all breaks go to the same place, so we also add a break to there to help other stages
              replaced.addSuccessor(new StatEdge(StatEdge.TYPE_BREAK, replaced, target, stat.getParent()));
            } else {
              // no break targets, or multiple targets to use
              // TODO: figure out how to handle this
            }
            return true;
          }
        }
      }
    }
    return false;
  }

  private static boolean isSwitchPatternMatch(SwitchHeadExprent head) {
    Exprent value = head.getValue();

    if (value instanceof InvocationExprent) {
      InvocationExprent invoc = (InvocationExprent) value;

      // TODO: test for SwitchBootstraps properly
      return invoc.getInvocationType() == InvocationExprent.InvocationType.DYNAMIC
             && (invoc.getName().equals("typeSwitch") || invoc.getName().equals("enumSwitch"));
    }

    return false;
  }

  public static boolean hasPatternMatch(RootStatement root) {
    return root.mt.getBytecodeVersion().hasSwitchPatternMatch() && DecompilerContext.getOption(IFernflowerPreferences.DECOMPILE_PREVIEW);
  }
}
