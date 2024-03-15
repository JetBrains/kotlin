package org.jetbrains.java.decompiler.util;

import org.jetbrains.java.decompiler.modules.decompiler.StatEdge;
import org.jetbrains.java.decompiler.modules.decompiler.exps.AssignmentExprent;
import org.jetbrains.java.decompiler.modules.decompiler.exps.Exprent;
import org.jetbrains.java.decompiler.modules.decompiler.exps.IfExprent;
import org.jetbrains.java.decompiler.modules.decompiler.exps.VarExprent;
import org.jetbrains.java.decompiler.modules.decompiler.stats.RootStatement;
import org.jetbrains.java.decompiler.modules.decompiler.stats.Statement;
import org.jetbrains.java.decompiler.modules.decompiler.vars.VarProcessor;

import java.util.BitSet;

//Debug printer useful for visualizing objects, no real functional value
public class DebugPrinter {
  public static void printMethod(Statement root, String name, VarProcessor varProc) {
    System.out.println(name + "{");
    if (root == null || root.getSequentialObjects() == null) {
      System.out.println("}");
      return;
    }

    for (Object obj : root.getSequentialObjects()) {
      if (obj instanceof Statement) {
        printStatement((Statement)obj, "  ", varProc);
      } else if (obj == null) {
        System.out.println("  null");
      } else {
        System.out.println("  " + obj.getClass().getSimpleName());
      }
    }

    if (root instanceof RootStatement) {
      printStatement(((RootStatement)root).getDummyExit(), "  ", varProc);
    }
    System.out.println("}");
  }

  public static void printStatement(Statement statement, String indent, VarProcessor varProc) {
    BitSet values = new BitSet();
    statement.getOffset(values);
    int start = values.nextSetBit(0);
    int end = values.length()-1;

    System.out.println(indent + '{' + statement.getClass().getSimpleName() + "}:" + statement.id + "  (" + start + ", " + end + ")");

    for (StatEdge edge : statement.getAllSuccessorEdges()) {
      System.out.println(indent + " Dest: " + edge.getDestination());
    }

    if (statement.getExprents() != null) {
      for(Exprent exp : statement.getExprents()) {
        System.out.println(printExprent(indent + "  ", exp, varProc));
      }
    }

    indent += "  ";
    for (Object obj : statement.getSequentialObjects()) {
      if (obj == null) {
        System.out.println(indent + " Null");
      } else if (obj instanceof Statement) {
        printStatement((Statement)obj, indent, varProc);
      } else if (obj instanceof Exprent) {
          System.out.println(printExprent(indent, (Exprent) obj, varProc));
      } else {
        System.out.println(indent + obj.getClass().getSimpleName());
      }
    }
  }

  private static String printExprent(String indent, Exprent exp, VarProcessor varProc) {
      StringBuffer sb = new StringBuffer();
      sb.append(indent);
      BitSet values = new BitSet();
      exp.getBytecodeRange(values);
      sb.append("(").append(values.nextSetBit(0)).append(", ").append(values.length()-1).append(") ");
      sb.append(exp.getClass().getSimpleName());
      sb.append(" ").append(exp.id).append(" ");
      if (exp instanceof VarExprent) {
        VarExprent varExprent = (VarExprent)exp;
        int currindex = varExprent.getIndex();
        int origindex = varProc == null ? -2 : varProc.getVarOriginalIndex(currindex);
        sb.append("[").append(currindex).append(":").append(origindex).append(", ").append(varExprent.isStack()).append("]");
        if (varProc != null) {
          sb.append(varProc.getCandidates(origindex));
        }
      } else if (exp instanceof AssignmentExprent) {
        AssignmentExprent assignmentExprent = (AssignmentExprent)exp;
        sb.append("{").append(printExprent(" ",assignmentExprent.getLeft(),varProc)).append(" =").append(printExprent(" ",assignmentExprent.getRight(),varProc)).append("}");
      } else if (exp instanceof IfExprent) {
        sb.append(' ').append(exp.toJava(0));
      }
      return sb.toString();
  }
}
