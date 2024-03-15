package org.jetbrains.java.decompiler.modules.decompiler;

import org.jetbrains.java.decompiler.main.DecompilerContext;
import org.jetbrains.java.decompiler.main.collectors.CounterContainer;
import org.jetbrains.java.decompiler.modules.decompiler.exps.*;
import org.jetbrains.java.decompiler.modules.decompiler.stats.BasicBlockStatement;
import org.jetbrains.java.decompiler.modules.decompiler.stats.RootStatement;
import org.jetbrains.java.decompiler.modules.decompiler.stats.Statement;
import org.jetbrains.java.decompiler.modules.decompiler.stats.SynchronizedStatement;
import org.jetbrains.java.decompiler.modules.decompiler.vars.VarProcessor;
import org.jetbrains.java.decompiler.struct.gen.VarType;
import org.jetbrains.java.decompiler.struct.gen.generics.GenericType;

public final class SynchronizedHelper {
  public static boolean cleanSynchronizedVar(Statement stat) {
    boolean res = false;
    for (Statement st : stat.getStats()) {
      res |= cleanSynchronizedVar(st);
    }

    if (stat instanceof SynchronizedStatement) {
      SynchronizedStatement sync = (SynchronizedStatement)stat;

      if (sync.getHeadexprentList().get(0) instanceof MonitorExprent) {
        MonitorExprent mon = (MonitorExprent)sync.getHeadexprentList().get(0);

        for (Exprent e : sync.getFirst().getExprents()) {
          if (e instanceof AssignmentExprent) {
            AssignmentExprent ass = (AssignmentExprent)e;

            if (ass.getLeft() instanceof VarExprent) {
              VarExprent var = (VarExprent)ass.getLeft();

              if (ass.getRight().equals(mon.getValue()) && !var.isVarReferenced(stat.getParent())) {
                sync.getFirst().getExprents().remove(e);
                res = true;
                break;
              }
            }
          }
        }
      }
    }

    return res;
  }

  public static boolean insertSink(RootStatement root, VarProcessor varProcessor, Statement stat) {
    boolean res = false;
    for (Statement st : stat.getStats()) {
      res |= insertSink(root, varProcessor, st);
    }

    if (stat instanceof SynchronizedStatement) {
      MonitorExprent mon = (MonitorExprent) ((SynchronizedStatement)stat).getHeadexprent();
      Exprent value = mon.getValue();

      if (value instanceof ConstExprent && ((ConstExprent)value).getConstType() != VarType.VARTYPE_STRING && !(((ConstExprent)value).getConstType() instanceof GenericType)) {
        // Somehow created a const monitor, add assignment of object to ensure that it functions
        int var = DecompilerContext.getCounterContainer().getCounterAndIncrement(CounterContainer.VAR_COUNTER);

        VarExprent varEx = new VarExprent(var, VarType.VARTYPE_OBJECT, varProcessor);
        // Doesn't track var type without this, ends up as <unknown>!
        varProcessor.setVarType(varEx.getVarVersionPair(), VarType.VARTYPE_OBJECT);

        AssignmentExprent assign = new AssignmentExprent(varEx, value, null);
        mon.replaceExprent(value, assign);
        assign.addBytecodeOffsets(value.bytecode);
        root.addComment("$VF: Added assignment to ensure synchronized validity");
      } else if (value instanceof InvocationExprent) {
        // Force boxing for monitor
        InvocationExprent inv = (InvocationExprent)value;

        if (inv.isBoxingCall()) {
          inv.markUsingBoxingResult();
        }
      }
    }

    return res;
  }
}
