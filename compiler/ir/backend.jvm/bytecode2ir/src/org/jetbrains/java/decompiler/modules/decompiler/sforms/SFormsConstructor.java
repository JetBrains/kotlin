package org.jetbrains.java.decompiler.modules.decompiler.sforms;

import org.jetbrains.java.decompiler.api.SFormsCreator;
import org.jetbrains.java.decompiler.code.CodeConstants;
import org.jetbrains.java.decompiler.modules.decompiler.ValidationHelper;
import org.jetbrains.java.decompiler.modules.decompiler.exps.*;
import org.jetbrains.java.decompiler.modules.decompiler.flow.*;
import org.jetbrains.java.decompiler.modules.decompiler.stats.*;
import org.jetbrains.java.decompiler.modules.decompiler.vars.VarVersionEdge;
import org.jetbrains.java.decompiler.modules.decompiler.vars.VarVersionNode;
import org.jetbrains.java.decompiler.modules.decompiler.vars.VarVersionPair;
import org.jetbrains.java.decompiler.modules.decompiler.vars.VarVersionsGraph;
import org.jetbrains.java.decompiler.struct.StructMethod;
import org.jetbrains.java.decompiler.struct.gen.MethodDescriptor;
import org.jetbrains.java.decompiler.util.*;
import org.jetbrains.java.decompiler.util.FastSparseSetFactory.FastSparseSet;

import java.util.*;

import static org.jetbrains.java.decompiler.modules.decompiler.sforms.VarMapHolder.mergeMaps;

public abstract class SFormsConstructor implements SFormsCreator {

  private final boolean incrementOnUsage;
  private final boolean simplePhi;
  private final boolean trackFieldVars;
  private final boolean trackPhantomPPNodes;
  private final boolean trackPhantomExitNodes;
  private final boolean trackSsuVersions;
  private final boolean doLiveVariableAnalysisRound;
  private final boolean trackDirectAssignments;
  private final boolean blockFieldPropagation;
  @Deprecated
  private final boolean ssau;


  // node id, var, version
  final HashMap<String, SFormsFastMapDirect> inVarVersions = new HashMap<>();

  // node id, var, version (direct branch)
  final HashMap<String, SFormsFastMapDirect> outVarVersions = new HashMap<>();

  // node id, var, version (negative branch)
  final HashMap<String, SFormsFastMapDirect> outNegVarVersions = new HashMap<>();

  // node id, var, version
  final HashMap<String, SFormsFastMapDirect> extraVarVersions = new HashMap<>();

  // node id, var, version
  final HashMap<String, SFormsFastMapDirect> catchableVersions = new HashMap<>();

  // var, version
  final HashMap<Integer, Integer> lastversion = new HashMap<>();

  // set factory
  FastSparseSetFactory<Integer> factory;


  // (var, version), version
  private final HashMap<VarVersionPair, FastSparseSet<Integer>> phi;

  // version, protected ranges (catch, finally)
  private final Map<VarVersionPair, Integer> mapVersionFirstRange;

  // version, version
  private final Map<VarVersionPair, VarVersionPair> phantomppnodes; // ++ and --

  // node.id, version, version
  private final Map<String, HashMap<VarVersionPair, VarVersionPair>> phantomexitnodes; // finally exits

  // versions memory dependencies
  private final VarVersionsGraph ssuversions;

  // field access vars (exprent id, var id)
  private final Map<Integer, Integer> mapFieldVars;

  // field access counter
  private int fieldvarcounter = -1;

  // track assignments for finding effectively final vars (left var, right var)
  private final HashMap<VarVersionPair, VarVersionPair> varAssignmentMap;

  private SFormsFastMapDirect currentCatchableMap = null;


  private RootStatement root;
  private StructMethod mt;
  private DirectGraph dgraph;

  public SFormsConstructor(
    boolean incrementOnUsage,
    boolean simplePhi,
    boolean trackFieldVars,
    boolean trackPhantomPPNodes,
    boolean trackPhantomExitNodes,
    boolean trackSsuVersions,
    boolean doLiveVariableAnalysisRound,
    boolean trackDirectAssignments,
    boolean blockFieldPropagation,
    boolean ssau) {
    this.incrementOnUsage = incrementOnUsage;
    this.simplePhi = simplePhi;
    this.trackFieldVars = trackFieldVars;
    this.trackPhantomPPNodes = trackPhantomPPNodes;
    this.trackPhantomExitNodes = trackPhantomExitNodes;
    this.trackSsuVersions = trackSsuVersions;
    this.doLiveVariableAnalysisRound = doLiveVariableAnalysisRound;
    this.trackDirectAssignments = trackDirectAssignments;
    this.blockFieldPropagation = blockFieldPropagation;
    this.ssau = ssau;


    this.phi = simplePhi ? new HashMap<>() : null;
    this.mapVersionFirstRange = ssau ? new HashMap<>() : null;
    this.phantomppnodes = trackPhantomPPNodes ? new HashMap<>() : null;
    this.phantomexitnodes = trackPhantomExitNodes ? new HashMap<>() : null;
    this.ssuversions = trackSsuVersions ? new VarVersionsGraph() : null;
    this.mapFieldVars = trackFieldVars ? new HashMap<>() : null;
    this.varAssignmentMap = trackDirectAssignments ? new HashMap<>() : null;


    ValidationHelper.assertTrue(
      !this.doLiveVariableAnalysisRound || this.trackSsuVersions,
      "doLiveVariableAnalysisRound -> trackSsuVersions: We need ssu versions to do live variable analysis");
    ValidationHelper.assertTrue(
      !this.incrementOnUsage || this.trackSsuVersions,
      "incrementOnUsage -> trackSsuVersions: We need ssu versions to be able to increment on usage");
    ValidationHelper.assertTrue(
      this.incrementOnUsage || this.simplePhi,
      "!incrementOnUsage -> simplePhi: We need to know if when nodes are already a phi node or not.");
  }

  public void splitVariables(RootStatement root, StructMethod mt) {
    this.root = root;
    this.mt = mt;

    FlattenStatementsHelper flatthelper = new FlattenStatementsHelper();
    DirectGraph dgraph = flatthelper.buildDirectGraph(root);
    this.dgraph = dgraph;
    ValidationHelper.validateDGraph(dgraph, root);
    ValidationHelper.validateAllVarVersionsAreNull(dgraph, root);

    DotExporter.toDotFile(dgraph, mt, "ssaSplitVariables");

    List<Integer> setInit = new ArrayList<>();
    for (int i = 0; i < 64; i++) {
      setInit.add(i);
    }
    this.factory = new FastSparseSetFactory<>(setInit);

    this.extraVarVersions.put(dgraph.first.id, this.createFirstMap());

    this.setCatchMaps(root, dgraph, flatthelper);

    int iteration = 1;
    HashSet<String> updated = new HashSet<>();
    do {
      // System.out.println("~~~~~~~~~~~~~ \r\n"+root.toJava());
      this.ssaStatements(dgraph, updated, false, mt, iteration++);
      // System.out.println("~~~~~~~~~~~~~ \r\n"+root.toJava());
    }
    while (!updated.isEmpty());

    if (this.doLiveVariableAnalysisRound) {
      this.ssaStatements(dgraph, updated, true, mt, iteration);

      this.ssuversions.initDominators();
    }

    if (this.trackSsuVersions && this.trackDirectAssignments) {
      // Validation testing
      ValidationHelper.validateVarVersionsGraph(this.ssuversions, root, this.varAssignmentMap);
    }
  }

  private void ssaStatements(DirectGraph dgraph, HashSet<String> updated, boolean calcLiveVars, StructMethod mt, int iteration) {

    DotExporter.toDotFile(dgraph, mt, "ssaStatements_" + iteration, this.outVarVersions);

    for (DirectNode node : dgraph.nodes) {

      updated.remove(node.id);
      this.mergeInVarMaps(node, dgraph);

      SFormsFastMapDirect varmap = this.inVarVersions.get(node.id);
      VarMapHolder varmaps = VarMapHolder.ofNormal(varmap);
      this.currentCatchableMap = null;

      if (node.hasSuccessors(DirectEdgeType.EXCEPTION)) {
        this.currentCatchableMap = new SFormsFastMapDirect(varmap);
        this.currentCatchableMap.removeAllStacks(); // stack gets cleared when throwing
        this.currentCatchableMap.removeAllFields(); // fields gets invalidated when throwing
        this.catchableVersions.put(node.id, this.currentCatchableMap);
      }

      // Foreach init node: mark as assignment!
      if (node.type == DirectNodeType.FOREACH_VARDEF && node.exprents.get(0) instanceof VarExprent) {
        this.updateVarExprent((VarExprent) node.exprents.get(0), node.statement, varmaps.getNormal(), calcLiveVars);
      } else if (node.exprents != null) {
        for (Exprent expr : node.exprents) {
          varmaps.toNormal(); // make sure we are in normal form
          this.processExprent(expr, varmaps, node.statement, calcLiveVars);
        }
      }

      if (this.blockFieldPropagation) {
        // quick solution: 'dummy' field variables should not cross basic block borders (otherwise problems e.g. with finally loops - usage without assignment in a loop)
        // For the full solution consider adding a dummy assignment at the entry point of the method
        if (node.hasSuccessors(DirectEdgeType.REGULAR)) {
          List<DirectEdge> successors = node.getSuccessors(DirectEdgeType.REGULAR);
          if (successors.size() != 1) {
            varmaps.removeAllFields();
          } else if (successors.get(0).getDestination().hasPredecessors(DirectEdgeType.REGULAR) &&
                     successors.get(0).getDestination().getPredecessors(DirectEdgeType.REGULAR).size() != 1) {
            varmaps.removeAllFields();
          }
        }
      }

      if (this.hasUpdated(node, varmaps)) {
        this.outVarVersions.put(node.id, varmaps.getIfTrue());
        if (dgraph.mapNegIfBranch.containsKey(node.id)) {
          this.outNegVarVersions.put(node.id, varmaps.getIfFalse());
        }

        // Don't update the node if it wasn't discovered normally, as that can lead to infinite recursion due to bad ordering!
        if (!dgraph.extraNodes.contains(node)) {
          for (DirectEdge nd : node.getSuccessors(DirectEdgeType.REGULAR)) {
            updated.add(nd.getDestination().id);
          }

          for (DirectEdge nd : node.getSuccessors(DirectEdgeType.EXCEPTION)) {
            updated.add(nd.getDestination().id);
          }
        }
      }
    }
  }

  // processes exprents, much like section 16.1. of the java language specifications
  // (Definite Assignment and Expressions).
  private void processExprent(Exprent expr, VarMapHolder varMaps, Statement stat, boolean calcLiveVars) {

    if (expr == null) {
      return;
    }

    // The var map data can't depend yet on the result of this expression.
    varMaps.assertIsNormal();

    switch (expr.type) {
      case IF: {
        // EXPRENT_IF is a wrapper for the head exprent of an if statement.
        // Therefore, the map needs to stay split, unlike with most other exprents.
        IfExprent ifexpr = (IfExprent) expr;
        this.processExprent(ifexpr.getCondition(), varMaps, stat, calcLiveVars);
        return;
      }
      case ASSIGNMENT: {
        // Assigning a local overrides all the readable versions of that node.

        AssignmentExprent assexpr = (AssignmentExprent) expr;

        if (assexpr.getCondType() != null) {
          throw new IllegalStateException("Didn't expect compound assignment yet");
        }

        Exprent dest = assexpr.getLeft();
        switch (dest.type) {
          case VAR: {
            final VarExprent destVar = (VarExprent) dest;

            this.processExprent(assexpr.getRight(), varMaps, stat, calcLiveVars);
            this.updateVarExprent(destVar, stat, varMaps.getNormal(), calcLiveVars);
            if (this.trackDirectAssignments) {

              switch (assexpr.getRight().type) {
                case VAR: {
                  VarVersionPair rightpaar = ((VarExprent) assexpr.getRight()).getVarVersionPair();
                  this.varAssignmentMap.put(destVar.getVarVersionPair(), rightpaar);
                  break;
                }
                case FIELD: {
                  int index = this.mapFieldVars.get(assexpr.getRight().id);
                  VarVersionPair rightpaar = new VarVersionPair(index, 0);
                  this.varAssignmentMap.put(destVar.getVarVersionPair(), rightpaar);
                  break;
                }
              }
            }

            return;
          }
          case FIELD: {
            this.processExprent(assexpr.getLeft(), varMaps, stat, calcLiveVars);
            varMaps.assertIsNormal(); // the left side of an assignment can't be a boolean expression
            this.processExprent(assexpr.getRight(), varMaps, stat, calcLiveVars);
            varMaps.toNormal();
            if (this.trackFieldVars) {
              varMaps.getNormal().removeAllFields();
              // assignment to a field resets all fields.
            }
            return;
          }
          default: {
            this.processExprent(assexpr.getLeft(), varMaps, stat, calcLiveVars);
            varMaps.assertIsNormal(); // the left side of an assignment can't be a boolean expression
            this.processExprent(assexpr.getRight(), varMaps, stat, calcLiveVars);
            varMaps.toNormal();
            return;
          }
        }

      }
      case FUNCTION: {
        FunctionExprent func = (FunctionExprent) expr;
        switch (func.getFuncType()) {
          case TERNARY: {
            // `a ? b : c`
            // Java language spec: 16.1.5.
            this.processExprent(func.getLstOperands().get(0), varMaps, stat, calcLiveVars);

            VarMapHolder bVarMaps = VarMapHolder.ofNormal(varMaps.getIfTrue());
            this.processExprent(func.getLstOperands().get(1), bVarMaps, stat, calcLiveVars);

            // reuse the varMaps for the false branch.
            varMaps.setNormal(varMaps.getIfFalse());
            this.processExprent(func.getLstOperands().get(2), varMaps, stat, calcLiveVars);

            if (bVarMaps.isNormal() && varMaps.isNormal()) {
              varMaps.mergeNormal(bVarMaps.getNormal());
            } else if (!varMaps.isNormal()) {
              // b and c are boolean expression and at least c had an assignment.
              varMaps.mergeIfTrue(bVarMaps.getIfTrue());
              varMaps.mergeIfFalse(bVarMaps.getIfFalse());
            } else {
              // b and c are boolean expression and at b had an assignment.
              // avoid cloning the c varmap.
              bVarMaps.mergeIfTrue(varMaps.getNormal());
              bVarMaps.mergeIfFalse(varMaps.getNormal());

              varMaps.set(bVarMaps); // move over the maps.
            }

            return;
          }
          case BOOLEAN_AND: {
            // `a && b`
            // Java language spec: 16.1.2.
            this.processExprent(func.getLstOperands().get(0), varMaps, stat, calcLiveVars);

            varMaps.makeFullyMutable();
            SFormsFastMapDirect ifFalse = varMaps.getIfFalse();
            varMaps.setNormal(varMaps.getIfTrue());

            this.processExprent(func.getLstOperands().get(1), varMaps, stat, calcLiveVars);
            varMaps.mergeIfFalse(ifFalse);
            return;
          }
          case BOOLEAN_OR: {
            // `a || b`
            // Java language spec: 16.1.3.
            this.processExprent(func.getLstOperands().get(0), varMaps, stat, calcLiveVars);

            varMaps.makeFullyMutable();
            SFormsFastMapDirect ifTrue = varMaps.getIfTrue();
            varMaps.setNormal(varMaps.getIfFalse());

            this.processExprent(func.getLstOperands().get(1), varMaps, stat, calcLiveVars);
            varMaps.mergeIfTrue(ifTrue);
            return;
          }
          case BOOL_NOT: {
            // `!a`
            // Java language spec: 16.1.4.
            this.processExprent(func.getLstOperands().get(0), varMaps, stat, calcLiveVars);
            varMaps.swap();

            return;
          }
          case INSTANCEOF: {
            // `a instanceof B`
            // pattern matching instanceof creates a new variable when true.
            this.processExprent(func.getLstOperands().get(0), varMaps, stat, calcLiveVars);
            varMaps.toNormal();

            if (func.getLstOperands().size() == 3) {
              // pattern matching
              // `a instanceof B b`
              // pattern matching variables are explained in different parts of the spec,
              // but it comes down to the same ideas.
              varMaps.makeFullyMutable();

              VarExprent var = (VarExprent) func.getLstOperands().get(2);

              this.updateVarExprent(var, stat, varMaps.getIfTrue(), calcLiveVars);
            }

            return;
          }
          case IMM:
          case MMI:
          case IPP:
          case PPI: {
            // process the var/field/array access
            // Note that ++ and -- are both reads and writes.
            this.processExprent(func.getLstOperands().get(0), varMaps, stat, calcLiveVars);
            // Can't have ++ or -- on a boolean expression.
            SFormsFastMapDirect varmap = varMaps.getNormal();

            switch (func.getLstOperands().get(0).type) {
              case VAR: {
                // TODO: why doesn't ssa need to process these?
                if (!this.trackPhantomPPNodes) {
                  return;
                }

                VarExprent var = (VarExprent) func.getLstOperands().get(0);

                int varIndex = var.getIndex();
                VarVersionPair varVersion = new VarVersionPair(varIndex, var.getVersion());

                // ssu graph, make sure this is still considered an assignment.
                // a phi node is made between the two versions, to make sure that
                // no code tries to rename the read variable without willing to
                // rename the written to variable.
                VarVersionPair phantomVersion = this.phantomppnodes.get(varVersion);
                if (phantomVersion == null) {
                  // get next version
                  int nextver = this.getNextFreeVersion(varIndex, null);
                  phantomVersion = new VarVersionPair(varIndex, nextver);
                  //ssuversions.createOrGetNode(phantomVersion);
                  this.ssuversions.createNode(phantomVersion);

                  VarVersionNode verNode = this.ssuversions.nodes.getWithKey(varVersion);

                  FastSparseSet<Integer> versions = this.factory.createEmptySet();
                  if (verNode.preds.size() == 1) {
                    versions.add(verNode.preds.iterator().next().source.version);
                  } else {
                    for (VarVersionEdge edge : verNode.preds) {
                      versions.add(edge.source.preds.iterator().next().source.version);
                    }
                  }
                  versions.add(nextver);
                  this.createOrUpdatePhiNode(varVersion, versions, stat);
                  this.phantomppnodes.put(varVersion, phantomVersion);
                }
                if (calcLiveVars) {
                  this.varMapToGraph(varVersion, varmap);
                }
                this.setCurrentVar(varmap, varIndex, var.getVersion());
                return;
              }
              case FIELD: {
                if (this.trackFieldVars) {
                  // assignment to a field resets all fields.
                  varmap.removeAllFields();
                }
                return;
              }
              default:
                return;
            }
          }
        }
        break;
      }
      case FIELD: {
        FieldExprent field = (FieldExprent) expr;
        this.processExprent(field.getInstance(), varMaps, stat, calcLiveVars);

        // a read of a field variable.
        if (this.trackFieldVars) {
          int index;
          if (this.mapFieldVars.containsKey(expr.id)) {
            index = this.mapFieldVars.get(expr.id);
          } else {
            index = this.fieldvarcounter--;
            this.mapFieldVars.put(expr.id, index);

            // ssu graph
            if (this.trackSsuVersions) {
              this.ssuversions.createNode(new VarVersionPair(index, 1));
            }
          }

          this.setCurrentVar(varMaps.getNormal(), index, 1);
        }
        return;
      }
      case VAR: {
        // a read of a variable.
        VarExprent vardest = (VarExprent) expr;
        final SFormsFastMapDirect varmap = varMaps.getNormal();

        int varindex = vardest.getIndex();
        int current_vers = vardest.getVersion();

        FastSparseSet<Integer> vers = varmap.get(varindex);

        int cardinality = vers != null ? vers.getCardinality() : 0;
        switch (cardinality) {
          case 0: { // size == 0 (var has no discovered assignments yet)
            this.updateVarExprent(vardest, stat, varmap, calcLiveVars);
            break;
          }
          case 1: { // size == 1 (var has only one discovered assignment)
            if (!this.incrementOnUsage) {
              // simply increment
              int it = vers.iterator().next();
              vardest.setVersion(it);
            } else {
              if (current_vers == 0) {
                // split last version
                int usever = this.getNextFreeVersion(varindex, stat);

                // set version
                vardest.setVersion(usever);
                this.setCurrentVar(varmap, varindex, usever);

                // ssu graph
                int lastver = vers.iterator().next();
                VarVersionNode prenode = this.ssuversions.nodes.getWithKey(new VarVersionPair(varindex, lastver));
                VarVersionNode usenode = this.ssuversions.createNode(new VarVersionPair(varindex, usever));
                VarVersionEdge edge = new VarVersionEdge(VarVersionEdge.EDGE_GENERAL, prenode, usenode);
                prenode.addSuccessor(edge);
                usenode.addPredecessor(edge);
              } else {
                if (calcLiveVars) {
                  this.varMapToGraph(new VarVersionPair(varindex, current_vers), varmap);
                }

                this.setCurrentVar(varmap, varindex, current_vers);
              }
            }
            break;
          }
          case 2:  // size > 1 (var has more than one assignment)
            if (!this.incrementOnUsage) {
              VarVersionPair varVersion = new VarVersionPair(varindex, current_vers);
              if (current_vers != 0 && this.phi.containsKey(varVersion)) {
                this.setCurrentVar(varmap, varindex, current_vers);
                // keep phi node up to date of all inputs
                this.phi.get(varVersion).union(vers);
              } else {
                // increase version
                int nextver = this.getNextFreeVersion(varindex, stat);
                // set version
                vardest.setVersion(nextver);

                this.setCurrentVar(varmap, varindex, nextver);
                // create new phi node
                this.phi.put(new VarVersionPair(varindex, nextver), vers);
              }
            } else {
              if (current_vers != 0) {
                if (calcLiveVars) {
                  this.varMapToGraph(new VarVersionPair(varindex, current_vers), varmap);
                }
                this.setCurrentVar(varmap, varindex, current_vers);
              } else {
                // split version
                int usever = this.getNextFreeVersion(varindex, stat);
                // set version
                vardest.setVersion(usever);

                // ssu node
                this.ssuversions.createNode(new VarVersionPair(varindex, usever));

                this.setCurrentVar(varmap, varindex, usever);

                current_vers = usever;
              }

              this.createOrUpdatePhiNode(new VarVersionPair(varindex, current_vers), vers, stat);
            }
            break;
        }
      }
    }

    for (Exprent ex : expr.getAllExprents()) {
      this.processExprent(ex, varMaps, stat, calcLiveVars);
      varMaps.toNormal();
    }

    if (this.trackFieldVars && makesFieldsDirty(expr)) {
      varMaps.getNormal().removeAllFields();
    }
  }


  private static boolean makesFieldsDirty(Exprent expr) {
    switch (expr.type) {
      case INVOCATION:
        return true;
      // already handled
//      case FUNCTION: {
//        FunctionExprent fexpr = (FunctionExprent) expr;
//        if (fexpr.getFuncType() >= FunctionExprent.FUNCTION_IMM && fexpr.getFuncType() <= FunctionExprent.FUNCTION_PPI) {
//          if (fexpr.getLstOperands().get(0) instanceof FieldExprent) {
//            return true;
//          }
//        }
//        break;
//      }
      // already handled
//      case ASSIGNMENT:
//        if (((AssignmentExprent) expr).getLeft() instanceof FieldExprent) {
//          return true;
//        }
//        break;
      case NEW:
        if (((NewExprent) expr).getNewType().type == CodeConstants.TYPE_OBJECT) {
          return true;
        }
        break;
    }
    return false;
  }

  private void updateVarExprent(VarExprent varassign, Statement stat, SFormsFastMapDirect varmap, boolean calcLiveVars) {
    int varindex = varassign.getIndex();

    if (varassign.getVersion() == 0) {
      // get next version
      int nextver = this.getNextFreeVersion(varindex, stat);

      // set version
      varassign.setVersion(nextver);

      if (this.trackSsuVersions) {
        // ssu graph
        this.ssuversions.createNode(new VarVersionPair(varindex, nextver), varassign.getLVT());
      }

    } else {
      if (calcLiveVars) {
        this.varMapToGraph(new VarVersionPair(varindex, varassign.getVersion()), varmap);
      }

    }

    this.setCurrentVar(varmap, varindex, varassign.getVersion());

    // update catchables map for normal vars only
    if (this.currentCatchableMap != null && varindex < VarExprent.STACK_BASE && varindex >= 0) {

      if (this.currentCatchableMap.containsKey(varindex)) {
        this.currentCatchableMap.get(varindex).add(varassign.getVersion());
      } else {
        FastSparseSet<Integer> set = this.factory.createEmptySet();
        set.add(varassign.getVersion());
        varmap.put(varindex, set);
      }
    }
  }

  private int getNextFreeVersion(int var, Statement stat) {
    final int nextver = this.lastversion.compute(var, (k, v) -> v == null ? 1 : v + 1);

    // save the first protected range, containing current statement
    if (this.ssau && stat != null) { // null iff phantom version
      Statement firstRange = getFirstProtectedRange(stat);

      if (firstRange != null) {
        this.mapVersionFirstRange.put(new VarVersionPair(var, nextver), firstRange.id);
      }
    }

    return nextver;
  }

  private void mergeInVarMaps(DirectNode node, DirectGraph dgraph) {

    SFormsFastMapDirect mapNew = new SFormsFastMapDirect();

    for (DirectEdge pred : node.getPredecessors(DirectEdgeType.REGULAR)) {
      SFormsFastMapDirect mapOut = this.getFilteredOutMap(node.id, pred.getSource().id, dgraph, node.id);
      if (mapNew.isEmpty()) {
        mapNew = mapOut.getCopy();
      } else {
        mergeMaps(mapNew, mapOut);
      }
    }

    for (DirectEdge pred : node.getPredecessors(DirectEdgeType.EXCEPTION)) {
      // TODO: interact with finally?
      SFormsFastMapDirect mapOut = this.catchableVersions.get(pred.getSource().id);
      if (mapOut != null) {
        if (mapNew.isEmpty()) {
          mapNew = mapOut.getCopy();
        } else {
          mergeMaps(mapNew, mapOut);
        }
      }
    }

    if (this.extraVarVersions.containsKey(node.id)) {
      SFormsFastMapDirect mapExtra = this.extraVarVersions.get(node.id);
      if (mapNew.isEmpty()) {
        mapNew = mapExtra.getCopy();
      } else {
        mergeMaps(mapNew, mapExtra);
      }
    }

    this.inVarVersions.put(node.id, mapNew);
  }

  private SFormsFastMapDirect getFilteredOutMap(String nodeid, String predid, DirectGraph dgraph, String destid) {

    SFormsFastMapDirect mapNew = new SFormsFastMapDirect();

    if (nodeid.equals(dgraph.mapNegIfBranch.get(predid))) {
      if (this.outNegVarVersions.containsKey(predid)) {
        mapNew = this.outNegVarVersions.get(predid).getCopy();
      }
    } else if (this.outVarVersions.containsKey(predid)) {
      mapNew = this.outVarVersions.get(predid).getCopy();
    }

    boolean isFinallyExit = dgraph.mapShortRangeFinallyPaths.containsKey(predid);

    if (isFinallyExit && !mapNew.isEmpty()) {

      SFormsFastMapDirect mapNewTemp = mapNew.getCopy();

      SFormsFastMapDirect mapTrueSource = new SFormsFastMapDirect();

      String exceptionDest = dgraph.mapFinallyMonitorExceptionPathExits.get(predid);
      boolean isExceptionMonitorExit = (exceptionDest != null && !nodeid.equals(exceptionDest));

      HashSet<String> setLongPathWrapper = new HashSet<>();
      for (FlattenStatementsHelper.FinallyPathWrapper finwraplong : dgraph.mapLongRangeFinallyPaths.get(predid)) {
        setLongPathWrapper.add(finwraplong.destination + "##" + finwraplong.source);
      }

      for (FlattenStatementsHelper.FinallyPathWrapper finwrap : dgraph.mapShortRangeFinallyPaths.get(predid)) {
        SFormsFastMapDirect map;

        boolean recFinally = dgraph.mapShortRangeFinallyPaths.containsKey(finwrap.source);

        if (recFinally) {
          // recursion
          map = this.getFilteredOutMap(finwrap.entry, finwrap.source, dgraph, destid);
        } else {
          if (finwrap.entry.equals(dgraph.mapNegIfBranch.get(finwrap.source))) {
            map = this.outNegVarVersions.get(finwrap.source);
          } else {
            map = this.outVarVersions.get(finwrap.source);
          }
        }

        // false path?
        boolean isFalsePath;

        if (recFinally) {
          isFalsePath = !finwrap.destination.equals(nodeid);
        } else {
          isFalsePath = !setLongPathWrapper.contains(destid + "##" + finwrap.source);
        }

        if (isFalsePath) {
          mapNewTemp.complement(map);
        } else {
          if (mapTrueSource.isEmpty()) {
            if (map != null) {
              mapTrueSource = map.getCopy();
            }
          } else {
            mergeMaps(mapTrueSource, map);
          }
        }
      }

      if (isExceptionMonitorExit) {

        mapNew = mapTrueSource;
      } else {

        mapNewTemp.union(mapTrueSource);

        SFormsFastMapDirect oldInMap = this.inVarVersions.get(nodeid);
        if (oldInMap != null) {
          mapNewTemp.union(oldInMap);
        }

        mapNew.intersection(mapNewTemp);

//      TODO: reimplement
//        if (this.trackPhantomExitNodes && !mapTrueSource.isEmpty() && !mapNew.isEmpty()) { // FIXME: what for??
//
//          // replace phi versions with corresponding phantom ones
//          HashMap<VarVersionPair, VarVersionPair> mapPhantom = phantomexitnodes.get(predid);
//          if (mapPhantom == null) {
//            mapPhantom = new HashMap<>();
//          }
//
//          SFormsFastMapDirect mapExitVar = mapNew.getCopy();
//          mapExitVar.complement(mapTrueSource);
//
//          for (Map.Entry<Integer, FastSparseSet<Integer>> ent : mapExitVar.entryList()) {
//            for (int version : ent.getValue()) {
//
//              int varindex = ent.getKey();
//              VarVersionPair exitvar = new VarVersionPair(varindex, version);
//              FastSparseSet<Integer> newSet = mapNew.get(varindex);
//
//              // remove the actual exit version
//              newSet.remove(version);
//
//              // get or create phantom version
//              VarVersionPair phantomvar = mapPhantom.get(exitvar);
//              if (phantomvar == null) {
//                int newversion = getNextFreeVersion(exitvar.var, null);
//                phantomvar = new VarVersionPair(exitvar.var, newversion);
//
//                VarVersionNode exitnode = ssuversions.nodes.getWithKey(exitvar);
//                VarVersionNode phantomnode = ssuversions.createNode(phantomvar);
//                phantomnode.flags |= VarVersionNode.FLAG_PHANTOM_FINEXIT;
//
//                VarVersionEdge edge = new VarVersionEdge(VarVersionEdge.EDGE_PHANTOM, exitnode, phantomnode);
//                exitnode.addSuccessor(edge);
//                phantomnode.addPredecessor(edge);
//
//                mapPhantom.put(exitvar, phantomvar);
//              }
//
//              // add phantom version
//              newSet.add(phantomvar.version);
//            }
//          }
//
//          if (!mapPhantom.isEmpty()) {
//            phantomexitnodes.put(predid, mapPhantom);
//          }
//        }
      }
    }

    return mapNew;
  }


  public static Statement getFirstProtectedRange(Statement stat) {
    while (true) {
      Statement parent = stat.getParent();

      if (parent == null) {
        break;
      }

      if (parent instanceof CatchAllStatement || parent instanceof CatchStatement) {
        if (parent.getFirst() == stat) {
          return parent;
        }
      } else if (parent instanceof SynchronizedStatement) {
        if (((SynchronizedStatement) parent).getBody() == stat) {
          return parent;
        }
      }

      stat = parent;
    }

    return null;
  }

  private void setCatchMaps(Statement stat, DirectGraph dgraph, FlattenStatementsHelper flatthelper) {

    SFormsFastMapDirect map;

    switch (stat.type) {
      case CATCH_ALL:
      case TRY_CATCH:

        List<VarExprent> lstVars;
        if (stat instanceof CatchAllStatement) {
          lstVars = ((CatchAllStatement) stat).getVars();
        } else {
          lstVars = ((CatchStatement) stat).getVars();
        }

        for (int i = 1; i < stat.getStats().size(); i++) {
          int varindex = lstVars.get(i - 1).getIndex();
          int version = this.getNextFreeVersion(varindex, stat); // == 1

          map = new SFormsFastMapDirect();
          this.setCurrentVar(map, varindex, version);

          this.extraVarVersions.put(dgraph.nodes.getWithKey(flatthelper.getMapDestinationNodes().get(stat.getStats().get(i).id)[0]).id, map);
          if (this.trackSsuVersions) {
            this.ssuversions.createNode(new VarVersionPair(varindex, version));
          }
        }
    }

    for (Statement st : stat.getStats()) {
      this.setCatchMaps(st, dgraph, flatthelper);
    }
  }

  private SFormsFastMapDirect createFirstMap() {
    boolean thisvar = !this.mt.hasModifier(CodeConstants.ACC_STATIC);

    MethodDescriptor md = MethodDescriptor.parseDescriptor(this.mt.getDescriptor());

    int paramcount = md.params.length + (thisvar ? 1 : 0);

    int varindex = 0;
    SFormsFastMapDirect map = new SFormsFastMapDirect();
    for (int i = 0; i < paramcount; i++) {
      int version = this.getNextFreeVersion(varindex, this.root); // == 1

      FastSparseSet<Integer> set = this.factory.createEmptySet();
      set.add(version);
      map.put(varindex, set);

      if (this.trackSsuVersions) {
        this.ssuversions.createNode(new VarVersionPair(varindex, version));
      }

      if (thisvar) {
        if (i == 0) {
          varindex++;
        } else {
          varindex += md.params[i - 1].stackSize;
        }
      } else {
        varindex += md.params[i].stackSize;
      }
    }

    return map;
  }

  public HashMap<VarVersionPair, FastSparseSet<Integer>> getPhi() {
    return this.phi;
  }


  private void createOrUpdatePhiNode(VarVersionPair phivar, FastSparseSet<Integer> vers, Statement stat) {

//    FastSparseSet<Integer> versCopy = vers.getCopy();
    Set<Integer> removed = new HashSet<>();
//    HashSet<Integer> phiVers = new HashSet<>();

    // take into account the corresponding mm/pp node if existing
    int ppvers = this.phantomppnodes.containsKey(phivar) ? this.phantomppnodes.get(phivar).version : -1;

    // ssu graph
    VarVersionNode phinode = this.ssuversions.nodes.getWithKey(phivar);
    List<VarVersionEdge> lstPreds = new ArrayList<>(phinode.preds);
    if (lstPreds.size() == 1) {
      // not yet a phi node
      VarVersionEdge edge = lstPreds.get(0);
      edge.source.removeSuccessor(edge);
      phinode.removePredecessor(edge);
    } else {
      for (VarVersionEdge edge : lstPreds) {
        int verssrc = edge.source.preds.iterator().next().source.version;
        if (!vers.contains(verssrc) && verssrc != ppvers) {
          edge.source.removeSuccessor(edge);
          phinode.removePredecessor(edge);
        } else {
//          versCopy.remove(verssrc);
          removed.add(verssrc);
//          phiVers.add(verssrc);
        }
      }
    }

    List<VarVersionNode> colnodes = new ArrayList<>();
    List<VarVersionPair> colpaars = new ArrayList<>();

//    for (int ver : versCopy) {
    for (int ver : vers) {
      if (removed.contains(ver)) {
        continue;
      }

      VarVersionNode prenode = this.ssuversions.nodes.getWithKey(new VarVersionPair(phivar.var, ver));

      int tempver = this.getNextFreeVersion(phivar.var, stat);

      VarVersionNode tempnode = new VarVersionNode(phivar.var, tempver);

      colnodes.add(tempnode);
      colpaars.add(new VarVersionPair(phivar.var, tempver));

      VarVersionEdge edge = new VarVersionEdge(VarVersionEdge.EDGE_GENERAL, prenode, tempnode);

      prenode.addSuccessor(edge);
      tempnode.addPredecessor(edge);


      edge = new VarVersionEdge(VarVersionEdge.EDGE_GENERAL, tempnode, phinode);
      tempnode.addSuccessor(edge);
      phinode.addPredecessor(edge);

//      phiVers.add(tempver);
    }

    this.ssuversions.addNodes(colnodes, colpaars);
  }


  private void varMapToGraph(VarVersionPair varVersion, SFormsFastMapDirect varMap) {
    ValidationHelper.assertTrue(this.trackSsuVersions, "Can't make an ssu graph without ssu tracked");

    VBStyleCollection<VarVersionNode, VarVersionPair> nodes = this.ssuversions.nodes;

    VarVersionNode node = nodes.getWithKey(varVersion);

    node.live = varMap.getCopy();
  }


  static boolean mapsEqual(SFormsFastMapDirect map1, SFormsFastMapDirect map2) {

    if (map1 == null) {
      return map2 == null;
    } else if (map2 == null) {
      return false;
    }

    if (map1.size() != map2.size()) {
      return false;
    }

    for (Map.Entry<Integer, FastSparseSet<Integer>> ent2 : map2.entryList()) {
      if (!InterpreterUtil.equalObjects(map1.get(ent2.getKey()), ent2.getValue())) {
        return false;
      }
    }

    return true;
  }

  void setCurrentVar(SFormsFastMapDirect varmap, int var, int vers) {
    FastSparseSet<Integer> set = this.factory.createEmptySet();
    set.add(vers);
    varmap.put(var, set);
  }

  boolean hasUpdated(DirectNode node, VarMapHolder varmaps) {
    return !mapsEqual(varmaps.getIfTrue(), this.outVarVersions.get(node.id))
           || (this.outNegVarVersions.containsKey(node.id) && !mapsEqual(varmaps.getIfFalse(), this.outNegVarVersions.get(node.id)));
  }


  public VarVersionsGraph getSsuVersions() {
    return this.ssuversions;
  }

  public SFormsFastMapDirect getLiveVarVersionsMap(VarVersionPair varVersion) {
    ValidationHelper.assertTrue(this.trackSsuVersions, "Can't get ssu versions if we aren't tracking ssu");

    VarVersionNode node = this.ssuversions.nodes.getWithKey(varVersion);
    if (node != null) {
      return node.live == null ? new SFormsFastMapDirect() : node.live;
    }

    return null;
  }

  public Map<VarVersionPair, Integer> getMapVersionFirstRange() {
    ValidationHelper.assertTrue(this.ssau, "This is an ssau only operation");
    return this.mapVersionFirstRange;
  }

  public Map<Integer, Integer> getMapFieldVars() {
    ValidationHelper.assertTrue(this.trackFieldVars, "Can't provide field data, if no field data was tracked");
    return this.mapFieldVars;
  }

  public Map<VarVersionPair, VarVersionPair> getVarAssignmentMap() {
    ValidationHelper.assertTrue(this.trackDirectAssignments, "Can't provide direct assignments, if no direct assignments was tracked");
    return this.varAssignmentMap;
  }
}
