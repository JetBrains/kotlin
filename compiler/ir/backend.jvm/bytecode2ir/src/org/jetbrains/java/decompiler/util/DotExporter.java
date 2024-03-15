package org.jetbrains.java.decompiler.util;

import org.jetbrains.java.decompiler.code.cfg.BasicBlock;
import org.jetbrains.java.decompiler.code.cfg.ControlFlowGraph;
import org.jetbrains.java.decompiler.code.cfg.ExceptionRangeCFG;
import org.jetbrains.java.decompiler.main.DecompilerContext;
import org.jetbrains.java.decompiler.main.rels.DecompileRecord;
import org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor;
import org.jetbrains.java.decompiler.modules.decompiler.StatEdge;
import org.jetbrains.java.decompiler.modules.decompiler.decompose.DominatorEngine;
import org.jetbrains.java.decompiler.modules.decompiler.flow.DirectEdge;
import org.jetbrains.java.decompiler.modules.decompiler.flow.DirectEdgeType;
import org.jetbrains.java.decompiler.modules.decompiler.flow.DirectGraph;
import org.jetbrains.java.decompiler.modules.decompiler.flow.DirectNode;
import org.jetbrains.java.decompiler.modules.decompiler.stats.*;
import org.jetbrains.java.decompiler.modules.decompiler.vars.VarVersionEdge;
import org.jetbrains.java.decompiler.modules.decompiler.vars.VarVersionNode;
import org.jetbrains.java.decompiler.modules.decompiler.vars.VarVersionPair;
import org.jetbrains.java.decompiler.modules.decompiler.vars.VarVersionsGraph;
import org.jetbrains.java.decompiler.struct.StructMethod;
import org.jetbrains.java.decompiler.util.FastSparseSetFactory.FastSparseSet;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.*;
import java.util.Map.Entry;

public class DotExporter {
  private static final String DOTS_FOLDER = System.getProperty("DOT_EXPORT_DIR", null);
  private static final String DOTS_ERROR_FOLDER = System.getProperty("DOT_ERROR_EXPORT_DIR", null);
  public static final boolean DUMP_DOTS = DOTS_FOLDER != null && !DOTS_FOLDER.trim().isEmpty();
  public static final boolean DUMP_ERROR_DOTS = DOTS_ERROR_FOLDER != null && !DOTS_ERROR_FOLDER.trim().isEmpty();

  private static final boolean EXTENDED_MODE = false;
  private static final boolean STATEMENT_LR_MODE = false;
  private static final boolean SAME_RANK_MODE = false;
  // http://graphs.grevian.org/graph is a nice visualizer for the outputed dots.

  // Outputs a statement and as much of its information as possible into a dot formatted string.
  // Nodes represent statements, their id, their type, and their code.
  // Black arrows represent a statement's successors.
  // Dotted arrows represent a statement's exception successors.
  // Blue arrows represent a statement's predecessors. The arrow points towards the predecessor.
  // Red arrows represent the statement tree. The arrows points to the statement's children.
  // Black arrows with a diamond head represent a statement's closure, or the statement that it's enclosed in.
  // Purple arrows with a bar head represent a statement's neighbors. TODO: needs backwards as well
  // Statements with no successors or predecessors (but still contained in the tree) will be in a subgraph titled "Isolated statements".
  // Statements that aren't found will be circular, and will have a message stating so.
  // Nodes with green borders are the canonical exit of method, but these may not always be emitted.
  private static String statToDot(Statement stat, String name) {
    DecompilerContext.getImportCollector().setWriteLocked(true);
    StringBuffer buffer = new StringBuffer();
    // List<String> subgraph = new ArrayList<>();
    Set<Integer> visitedNodes = new HashSet<>();
    Set<Integer> exits = new HashSet<>();
    Set<Integer> referenced = new HashSet<>();

    buffer.append("digraph " + name + " {\r\n");

    if (STATEMENT_LR_MODE) {
      buffer.append("  rankdir = LR;\r\n");
    }

    List<Statement> stats = new ArrayList<>();
    stats.add(stat);
    findAllStats(stats, stat);

    DummyExitStatement exit = null;
    if (stat instanceof RootStatement) {
      exit = ((RootStatement)stat).getDummyExit();
    }

    // Pre process
    Map<StatEdge, String> extraData = new HashMap<>();
    Set<StatEdge> extraDataSeen = new HashSet<>();

    for (Statement st : stats) {
      if (st instanceof IfStatement) {
        IfStatement ifs = (IfStatement) st;

        if (ifs.getIfEdge() != null) {
          extraData.put(ifs.getIfEdge(), "If Edge");
        }

        if (ifs.getElseEdge() != null) {
          extraData.put(ifs.getElseEdge(), "Else Edge");
        }
      }
      if (SAME_RANK_MODE && st.getStats().size() > 1){
        buffer.append(" subgraph { rank = same; ");
        for (Statement s : st.getStats()) {
          if (st instanceof IfStatement || st instanceof SwitchStatement) {
            if (s == st.getFirst()){
              continue;
            }
          }

          buffer.append(s.id + "; ");
        }
        buffer.append("}\r\n");
      }
    }

    for(Statement st : stats) {
      String sourceId = st.id + (st.getSuccessorEdges(StatEdge.TYPE_EXCEPTION).isEmpty()?"":"000000");

      boolean edges = false;
      for(StatEdge edge : st.getSuccessorEdges(Statement.STATEDGE_ALL)) {
        String destId = edge.getDestination().id + (edge.getDestination().getSuccessorEdges(StatEdge.TYPE_EXCEPTION).isEmpty()?"":"000000");

        String edgeType = getEdgeType(edge);
        String meta = getEdgeMeta(edge);

        // Add extra edge data
        // TODO do same for predecessors?
        for (Entry<StatEdge, String> entry : extraData.entrySet()) {
          if (edge.getSource().id == entry.getKey().getSource().id && edge.getDestination().id == entry.getKey().getDestination().id) {
            edgeType = edgeType == null ? entry.getValue() : edgeType + " (" + entry.getValue() + ")";
            extraDataSeen.add(entry.getKey());
          }
        }

        if (edge.closure != null ) {
          edgeType = edgeType == null ? "Closure: " + edge.closure.id : edgeType + " (Closure: " + edge.closure.id + ")";
        }

        buffer.append(sourceId + "->" + destId + (edgeType != null ? "[label=\"" + edgeType + "\", " + meta + "]" : "[" + meta + "]") + ";\n");

        if (EXTENDED_MODE && edge.closure != null) {
          String clsId = edge.closure.id + (edge.getDestination().getSuccessorEdges(StatEdge.TYPE_EXCEPTION).isEmpty()?"":"000000");
          buffer.append(sourceId + "->" + clsId + " [arrowhead=diamond,label=\"Closure\"];\r\n");
        }

        if (edge.getType() == StatEdge.TYPE_FINALLYEXIT || edge.getType() == StatEdge.TYPE_BREAK) {
          exits.add(edge.getDestination().id);
        }

        referenced.add(edge.getDestination().id);

        edges = true;
      }

      if(EXTENDED_MODE) {
        for (StatEdge labelEdge : st.getLabelEdges()) {
          String src = labelEdge.getSource().id + (labelEdge.getSource().getSuccessorEdges(StatEdge.TYPE_EXCEPTION).isEmpty() ? "" : "000000");
          String destId = labelEdge.getDestination().id + (labelEdge.getDestination().getSuccessorEdges(StatEdge.TYPE_EXCEPTION).isEmpty() ? "" : "000000");
          String data = "";
          if (labelEdge.labeled) {
            data += "Labeled";
          }
          if (labelEdge.labeled && labelEdge.explicit) {
            data += ", ";
          }
          if (labelEdge.explicit) {
            data += "Explicit";
          }
          buffer.append(src + "->" + destId + " [color=orange,label=\"Label Edge (" + data + ") (Contained by " + st.id + ")\"];\r\n");
        }
      }

      // Neighbor set is redundant
//      for (Statement neighbour : st.getNeighbours(Statement.STATEDGE_ALL, Statement.DIRECTION_FORWARD)) {
//        String destId = neighbour.id + (neighbour.getSuccessorEdges(StatEdge.TYPE_EXCEPTION).isEmpty()?"":"000000");
//        buffer.append(sourceId + "->" + destId + " [arrowhead=tee,color=purple];\r\n");
//      }

      if (EXTENDED_MODE) {
        for(StatEdge edge : st.getPredecessorEdges(Statement.STATEDGE_ALL)) {
          String destId = edge.getSource().id + (edge.getSource().getSuccessorEdges(StatEdge.TYPE_EXCEPTION).isEmpty() ? "" : "000000");

          String edgeType = getEdgeType(edge);

          buffer.append(sourceId + "->" + destId + "[color=blue" + (edgeType != null ? ",fontcolor=blue,label=\"" + edgeType + "\"" : "") + "];\r\n");

          referenced.add(edge.getSource().id);

          edges = true;
        }
      }

      for(StatEdge edge : st.getSuccessorEdges(StatEdge.TYPE_EXCEPTION)) {
        String destId = edge.getDestination().id + (edge.getDestination().getSuccessorEdges(StatEdge.TYPE_EXCEPTION).isEmpty()?"":"000000");

        buffer.append(sourceId + " -> " + destId + " [style=dotted];\r\n");
        referenced.add(edge.getDestination().id);

        edges = true;
      }

      // Special case if edges
      // TODO: add labels onto existing successors

      // Graph tree
      boolean foundFirst = false;
      boolean isIf = st instanceof IfStatement;
      boolean foundIf = false;
      boolean foundElse = false;
      for (Statement s : st.getStats()) {
        String destId = s.id + (s.getSuccessorEdges(StatEdge.TYPE_EXCEPTION).isEmpty()?"":"000000");

        String label = "";

        if (s == st.getFirst()) {
          label = "First";
          foundFirst = true;
        }

        if (st instanceof IfStatement) {
          IfStatement ifs = (IfStatement) st;
          if (s == ifs.getIfstat()) {
            label = "If stat";
            foundIf = true;
          }
          if (s == ifs.getElsestat()) {
            label = "Else stat";
            foundElse = true;
          }
        }

        buffer.append(sourceId + " -> " + destId + " [arrowhead=vee,color=red" + (!label.equals("") ? ",fontcolor=red,label=\"" + label + "\"" : "") + "];\r\n");
        referenced.add(s.id);
      }

      if (!foundFirst && st.getFirst() != null) {
        String destId = st.getFirst().id + (st.getFirst().getSuccessorEdges(StatEdge.TYPE_EXCEPTION).isEmpty()?"":"000000");
        buffer.append(sourceId + "->" + destId + " [arrowhead=vee,color=red,fontcolor=red,label=\"Dangling First statement!\"];\r\n");
      }

      if (isIf) {
        if (!foundIf && ((IfStatement) st).getIfstat() != null) {
          String destId = ((IfStatement) st).getIfstat().id + (st.getFirst().getSuccessorEdges(StatEdge.TYPE_EXCEPTION).isEmpty()?"":"000000");
          buffer.append(sourceId + "->" + destId + " [arrowhead=vee,color=red,fontcolor=red,label=\"Dangling If statement!\"];\r\n");
        }

        if (!foundElse && ((IfStatement) st).getElsestat() != null) {
          String destId = ((IfStatement) st).getElsestat().id + (((IfStatement) st).getElsestat().getSuccessorEdges(StatEdge.TYPE_EXCEPTION).isEmpty()?"":"000000");
          buffer.append(sourceId + "->" + destId + " [arrowhead=vee,color=red,fontcolor=red,label=\"Dangling Else statement!\"];\r\n");
        }
      }

      visitedNodes.add(st.id);

      String node = sourceId + " [shape=box,label=\"" + st.id + " (" + getStatType(st) + ")\\n" + toJava(st) + "\"" + (st == stat ? ",color=red" : "") + "];\n";
//      if (edges || st == stat) {
        buffer.append(node);
//      } else {
//        subgraph.add(node);
//      }
    }

    // Exits
    if (exit != null) {
      buffer.append(exit.id + " [color=green,label=\"" + exit.id + " (Canonical Return)\"];\n");
      referenced.remove(exit.id);
    }

//    for (Integer exit : exits) {
//      if (!visitedNodes.contains(exit)) {
//        buffer.append(exit + " [color=green,label=\"" + exit + " (Canonical Return)\"];\r\n");
//      }
//
//      referenced.remove(exit);
//    }

    referenced.removeAll(visitedNodes);

    // Unresolved statement references
    for (Integer integer : referenced) {
      buffer.append(integer + " [color=red,label=\"" + integer + " (Unknown statement!)\"];\r\n");
    }

    for (StatEdge labelEdge : extraData.keySet()) {
      if (extraDataSeen.contains(labelEdge)) {
        continue;
      }

      String src = labelEdge.getSource().id + (labelEdge.getSource().getSuccessorEdges(StatEdge.TYPE_EXCEPTION).isEmpty() ? "" : "000000");
      String destId = labelEdge.getDestination().id + (labelEdge.getDestination().getSuccessorEdges(StatEdge.TYPE_EXCEPTION).isEmpty() ? "" : "000000");
      String label = "Floating extra edge: ("  + extraData.get(labelEdge) + ")";

      buffer.append(src + " -> " + destId + " [arrowhead=vee,color=red,fontcolor=red,label=\"" + label + "\"];\r\n");
    }

//    if (subgraph.size() > 0) {
//      buffer.append("subgraph cluster_non_parented {\r\n\tlabel=\"Isolated statements\";\r\n");
//
//      for (String s : subgraph) {
//        buffer.append("\t"+s);
//      }
//
//      buffer.append("\t}\r\n");
//    }

    buffer.append("}");

    DecompilerContext.getImportCollector().setWriteLocked(false);

    return buffer.toString();
  }

  private static String statementHierarchy(Statement stat) {
    StringBuffer buffer = new StringBuffer();

    buffer.append("digraph G {\r\n");

    buffer.append("subgraph cluster_root {\r\n");

    buffer.append(stat.id + " [shape=box,label=\"" + stat.id + " (" + getStatType(stat) + ")\r\n" + toJava(stat) + "\"" + "];\r\n");

    recursivelyGraphStatement(buffer, stat);

    buffer.append("}\r\n");

    buffer.append("}");

    return buffer.toString();
  }

  private static void recursivelyGraphStatement(StringBuffer buffer, Statement statement) {
    buffer.append("subgraph cluster_" + statement.id + " {\r\n");
    buffer.append("label=\"" + statement.id + " (" + getStatType(statement) + ")\r\n" + toJava(statement) + "\"" + ";\r\n");

    for (Statement stat : statement.getStats()) {
      buffer.append(stat.id + " [shape=box,label=\"" + stat.id + " (" + getStatType(stat) + ")\r\n" + toJava(stat) + "\"" + "];\r\n");

      recursivelyGraphStatement(buffer, stat);
    }


    buffer.append("}\r\n");
  }

  private static String toJava(Statement statement) {
    try {
      String java = statement.toJava().convertToStringAndAllowDataDiscard()
        .replace("\"", "\\\"")
        .replace("\r", "")
        .replace("\n", "\\l");
      if (statement instanceof BasicBlockStatement) {
        if (statement.getExprents() == null || statement.getExprents().isEmpty()) {
          java = "<" + (statement.getExprents() == null ? "null" : "empty") + " basic block>\\n" + java;
        }
      }

      return java;
    } catch (Exception e) {
      return "Could not get content";
    }
  }

  private static String getEdgeType(StatEdge edge) {
    switch (edge.getType()) {
      case StatEdge.TYPE_REGULAR: return null;
      case StatEdge.TYPE_EXCEPTION: return "Exception";
      case StatEdge.TYPE_BREAK: return "Break";
      case StatEdge.TYPE_CONTINUE: return "Continue";
      case StatEdge.TYPE_FINALLYEXIT: return "Finally Exit";
      default: return "Unknown Edge (composite?)";
    }
  }

  private static String getEdgeMeta(StatEdge edge) {
    switch (edge.getType()) {
      case StatEdge.TYPE_REGULAR: return "weight=1, color=black";
      case StatEdge.TYPE_EXCEPTION: return "weight=1, color=orange, style=dashed";
      case StatEdge.TYPE_BREAK: return "weight=0.4, color=blue";
      case StatEdge.TYPE_CONTINUE: return "weight=0.2, color=green";
      case StatEdge.TYPE_FINALLYEXIT: return "weight=1, color=orange, style=dotted";
      default: return "weight=1, color=purple";
    }
  }

  private static String getStatType(Statement st) {
    switch (st.type) {
      case GENERAL: return ((GeneralStatement) st).isPlaceholder() ? "General (Placeholder)" : "General";
      case IF: return "If";
      case DO: return "Do";
      case SWITCH: return "Switch";
      case TRY_CATCH: return "Try Catch";
      case BASIC_BLOCK: return "Basic Block #" + ((BasicBlockStatement)st).getBlock().getId();
      case SYNCHRONIZED: return "Synchronized";
      case CATCH_ALL: return "Catch All";
      case ROOT: return "Root";
      case DUMMY_EXIT: return "Dummy Exit";
      case SEQUENCE: return "Sequence";
      default: return "Unknown";
    }
  }

  private static void findAllStats(List<Statement> list, Statement root) {
    for (Statement stat : root.getStats()) {
      if (!list.contains(stat)) {
        list.add(stat);
      }

      if (stat instanceof IfStatement) {
        IfStatement ifs = (IfStatement) stat;

        if (ifs.getIfstat() != null && !list.contains(ifs.getIfstat())) {
          list.add(ifs.getIfstat());
        }

        if (ifs.getElsestat() != null && !list.contains(ifs.getElsestat())) {
          list.add(ifs.getElsestat());
        }
      }

      findAllStats(list, stat);
    }
  }


  private static String cfgToDot(String name, ControlFlowGraph graph, boolean showMultipleEdges) {

    StringBuffer buffer = new StringBuffer();

    buffer.append("digraph " + name + " {\r\n");

    List<BasicBlock> blocks = graph.getBlocks();
    for (BasicBlock block : blocks) {
      buffer.append(block.getId() + " [shape=box,label=\"Block " + block.getId() + "\n" + block.getSeq() + "\"];\r\n");

      List<BasicBlock> suc = block.getSuccs();
      List<BasicBlock> preds = block.getPreds();

//      if(!showMultipleEdges) {
//        HashSet<BasicBlock> set = new HashSet<>();
//        set.addAll(suc);
//        suc = Collections.list(Collections.enumeration(set));
//      }

      for (BasicBlock basicBlock : suc) {
        buffer.append(block.getId() + " -> " + basicBlock.getId() + ";\r\n");
      }

//      for (BasicBlock pred : preds) {
//        buffer.append(block.getDebugId() + " -> " + pred.getDebugId() + " [color=blue];\r\n");
//      }

      suc = block.getSuccExceptions();
      preds = block.getPredExceptions();

//      if(!showMultipleEdges) {
//        HashSet<BasicBlock> set = new HashSet<>();
//        set.addAll(suc);
//        suc = Collections.list(Collections.enumeration(set));
//      }

      for (int j = 0; j < suc.size(); j++) {
        buffer.append(block.getId() + " -> " + suc.get(j).getId() + " [style=dotted];\r\n");
      }

//      for (BasicBlock pred : preds) {
//        buffer.append(block.getDebugId() + " -> " + pred.getDebugId() + " [color=blue,style=dotted];\r\n");
//      }
    }

    for (int i = 0; i < graph.getExceptions().size(); i++) {
      ExceptionRangeCFG ex = graph.getExceptions().get(i);
      buffer.append("subgraph cluster_ex_" + i + " {\r\n\tlabel=\"Exception range for Block " + ex.getHandler().getId() + " \";\r\n");
      for (BasicBlock bb : ex.getProtectedRange()) {
        buffer.append("\t" + bb.getId() + ";\r\n");
      }
      buffer.append("\t}\r\n");
    }

    buffer.append("}");

    return buffer.toString();
  }

  private static String decompileRecordToDot(DecompileRecord decompileRecord) {
    StringBuilder builder = new StringBuilder();

    List<String> names = decompileRecord.getNames();
    int size = names.size();

    builder.append("digraph decompileRecord {\n");

    for (int i = 0; i < size; i++) {
      builder.append("\t").append(i).append(" [label=\"").append(names.get(i)).append("\"];\n");
    }

    builder.append("\n");

    for (int i = 0; i < size - 1; i++) {
      builder.append("\t").append(i).append(" -> ").append(i + 1).append(";\n");
    }

    builder.append("}");

    return builder.toString();
  }
  private static String varsToDot(VarVersionsGraph graph, HashMap<VarVersionPair, VarVersionPair> varAssignmentMap) {

    StringBuffer buffer = new StringBuffer();

    buffer.append("digraph G {\r\n");

    List<VarVersionNode> blocks = graph.nodes;
    for(int i=0;i<blocks.size();i++) {
      VarVersionNode block = blocks.get(i);

      buffer.append((block.var*1000+block.version)+" [shape=box,label=\""+block.var+"_"+block.version+"\"];\r\n");

      for(VarVersionEdge edge: block.succs) {
        VarVersionNode dest = edge.dest;
        buffer.append((block.var*1000+block.version)+"->"+(dest.var*1000+dest.version)+(edge.type==VarVersionEdge.EDGE_PHANTOM?" [style=dotted]":"")+";\r\n");
      }
    }

    if (varAssignmentMap != null) {
      for (Entry<VarVersionPair, VarVersionPair> entry : varAssignmentMap.entrySet()) {
        VarVersionPair to = entry.getKey();
        VarVersionPair from = entry.getValue();
        buffer.append((from.var * 1000 + from.version) + "->" + (to.var * 1000 + to.version) + " [color=green];\r\n");
      }
    }

    buffer.append("}");

    return buffer.toString();
  }

  private static String domEngineToDot(DominatorEngine doms) {
    StringBuilder builder = new StringBuilder();

    builder.append("digraph G {\r\n");

    Set<Integer> nodes = new HashSet<>();

    for (Integer key : doms.getOrderedIDoms().getLstKeys()) {
      nodes.add(key);
      nodes.add(doms.getOrderedIDoms().getWithKey(key));
      builder.append("x" + doms.getOrderedIDoms().getWithKey(key) + " -> x" + key + ";\n");
    }

    for (Integer nd : nodes) {
      builder.append("x" + nd + "[label=\"" + nd + "\"];\n");
    }

    builder.append("}");

    return builder.toString();
  }

  private static String digraphToDot(DirectGraph graph, Map<String, SFormsFastMapDirect> vars) {
    DecompilerContext.getImportCollector().setWriteLocked(true);

    StringBuffer buffer = new StringBuffer();

    buffer.append("digraph G {\r\n");

    List<DirectNode> blocks = graph.nodes;
    for (DirectNode block : blocks) {
      StringBuilder label = new StringBuilder(block.id + " in statement " + block.statement.id + " " + getStatType(block.statement));
      label.append("\\n");
      label.append(block.block != null ? toJava(block.block) : "null block");
      if (block.block == null) {
        TextBuffer buf = ExprProcessor.listToJava(block.exprents, 0);
        label.append("\\n");
        label.append(buf.convertToStringAndAllowDataDiscard());
      }
      if (vars != null && vars.containsKey(block.id)) {
        SFormsFastMapDirect map = vars.get(block.id);

        List<Entry<Integer, FastSparseSet<Integer>>> lst = map.entryList();
        if (lst != null) {
          for (Entry<Integer, FastSparseSet<Integer>> entry : lst) {
            label.append("\\n").append(entry.getKey());
            Set<Integer> set = entry.getValue().toPlainSet();
            label.append("=").append(set);
          }
        }
      }

      buffer.append("x" + (block.id)+" [shape=box,label=\""+label+"\"];\r\n");

      for (DirectEdgeType type : DirectEdgeType.TYPES) {
        for(DirectEdge dest : block.getSuccessors(type)) {
          buffer.append("x" + (block.id)+" -> x"+(dest.getDestination().id)+ (type == DirectEdgeType.EXCEPTION ? "[style=dotted]" : "") + ";\r\n");
        }
      }
    }

    buffer.append("}");

    DecompilerContext.getImportCollector().setWriteLocked(false);

    return buffer.toString();
  }

  private static File getFile(String folder, StructMethod mt, String suffix) {
    return getFile(folder, mt, "", suffix);
  }

  private static File getFile(String folder, StructMethod mt, String subdirectory, String suffix) {
    File root = new File(folder + mt.getClassQualifiedName() + (subdirectory.isEmpty() ? "" : "/" + subdirectory));
    if (!root.isDirectory()) {
      root.mkdirs();
    }

    return new File(root,
      mt.getName().replace('<', '.').replace('>', '_') +
        mt.getDescriptor().replace('/', '.') +
        '_' + suffix + ".dot");
  }

  private static File getFile(String folder, String name) {
    File root = new File(folder);
    if (!root.isDirectory())
      root.mkdirs();
    return new File(root,name + ".dot");
  }

  public static void toDotFile(DirectGraph dgraph, StructMethod mt, String suffix) {
    toDotFile(dgraph, mt, suffix, null);
  }

  public static void toDotFile(DirectGraph dgraph, StructMethod mt, String suffix, Map<String, SFormsFastMapDirect> vars) {
    if (!DUMP_DOTS)
      return;
    try{
      BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(getFile(DOTS_FOLDER, mt, suffix)));
      out.write(digraphToDot(dgraph, vars).getBytes());
      out.close();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public static void errorToDotFile(DirectGraph dgraph, StructMethod mt, String suffix) {
    errorToDotFile(dgraph, mt, suffix, null);
  }

  public static void errorToDotFile(DirectGraph dgraph, StructMethod mt, String suffix, Map<String, SFormsFastMapDirect> vars) {
    if (!DUMP_ERROR_DOTS)
      return;
    try{
      BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(getFile(DOTS_ERROR_FOLDER, mt, suffix)));
      out.write(digraphToDot(dgraph, vars).getBytes());
      out.close();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public static void toDotFile(Statement stat, StructMethod mt, String suffix) {
    toDotFile(stat, mt, "", suffix);
  }

  public static void toDotFile(Statement stat, StructMethod mt, String subdirectory, String suffix) {
    if (!DUMP_DOTS)
      return;
    try{
      BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(getFile(DOTS_FOLDER, mt, subdirectory, suffix)));
      out.write(statToDot(stat, suffix).getBytes());
      out.close();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public static void statementHierarchyToDot(Statement stat, StructMethod mt, String suffix) {
    if (!DUMP_DOTS)
      return;
    try{
      BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(getFile(DOTS_FOLDER, mt, suffix)));
      out.write(statementHierarchy(stat).getBytes());
      out.close();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public static void toDotFile(Statement stat, String name) {
    if (!DUMP_DOTS)
      return;
    try{
      BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(getFile(DOTS_FOLDER, name)));
      out.write(statToDot(stat, name).getBytes());
      out.close();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public static void errorToDotFile(Statement stat, StructMethod mt, String suffix) {
    if (!DUMP_ERROR_DOTS)
      return;
    try{
      BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(getFile(DOTS_ERROR_FOLDER, mt, suffix)));
      out.write(statToDot(stat, suffix).getBytes());
      out.close();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public static void errorToDotFile(Statement stat, String name) {
    if (!DUMP_ERROR_DOTS)
      return;
    try {
      BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(getFile(DOTS_ERROR_FOLDER, name)));
      out.write(statToDot(stat, name).getBytes());
      out.close();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public static void toDotFile(VarVersionsGraph graph, StructMethod mt, String suffix, HashMap<VarVersionPair, VarVersionPair> varAssignmentMap) {
    if (!DUMP_DOTS)
      return;
    try{
      BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(getFile(DOTS_FOLDER, mt, suffix)));
      out.write(varsToDot(graph, varAssignmentMap).getBytes());
      out.close();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public static void errorToDotFile(VarVersionsGraph graph, StructMethod mt, String suffix, HashMap<VarVersionPair, VarVersionPair> varAssignmentMap) {
    if (!DUMP_ERROR_DOTS)
      return;
    try{
      BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(getFile(DOTS_ERROR_FOLDER, mt, suffix)));
      out.write(varsToDot(graph, varAssignmentMap).getBytes());
      out.close();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public static void toDotFile(DecompileRecord decompileRecord, StructMethod mt, String suffix, boolean error) {
    if (error) {
      if (!DUMP_ERROR_DOTS) {
        return;
      }
    } else {
      if (!DUMP_DOTS) {
        return;
      }
    }

    try {
      BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(getFile(error ? DOTS_ERROR_FOLDER : DOTS_FOLDER, mt, suffix)));
      out.write(decompileRecordToDot(decompileRecord).getBytes());
      out.close();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public static void toDotFile(ControlFlowGraph graph, StructMethod mt, String suffix, boolean showMultipleEdges) {
    if (!DUMP_DOTS)
      return;
    try{
      BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(getFile(DOTS_FOLDER, mt, suffix)));
      out.write(cfgToDot(suffix, graph, showMultipleEdges).getBytes());
      out.close();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public static void errorToDotFile(ControlFlowGraph graph, StructMethod mt, String suffix) {
    if (!DUMP_ERROR_DOTS)
      return;
    try{
      BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(getFile(DOTS_ERROR_FOLDER, mt, suffix)));
      out.write(cfgToDot(suffix, graph, true).getBytes());
      out.close();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public static void toDotFile(DominatorEngine doms, StructMethod mt, String suffix) {
    if (!DUMP_DOTS)
      return;
    try{
      BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(getFile(DOTS_FOLDER, mt, suffix)));
      out.write(domEngineToDot(doms).getBytes());
      out.close();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}