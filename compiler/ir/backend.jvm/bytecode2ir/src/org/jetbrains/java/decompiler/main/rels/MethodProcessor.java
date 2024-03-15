// Copyright 2000_2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.java.decompiler.main.rels;

import org.jetbrains.java.decompiler.code.CodeConstants;
import org.jetbrains.java.decompiler.code.InstructionSequence;
import org.jetbrains.java.decompiler.code.cfg.ControlFlowGraph;
import org.jetbrains.java.decompiler.main.DecompilerContext;
import org.jetbrains.java.decompiler.main.collectors.CounterContainer;
import org.jetbrains.java.decompiler.main.extern.IFernflowerLogger;
import org.jetbrains.java.decompiler.main.extern.IFernflowerPreferences;
import org.jetbrains.java.decompiler.modules.code.DeadCodeHelper;
import org.jetbrains.java.decompiler.modules.decompiler.*;
import org.jetbrains.java.decompiler.modules.decompiler.decompose.DomHelper;
import org.jetbrains.java.decompiler.modules.decompiler.deobfuscator.ExceptionDeobfuscator;
import org.jetbrains.java.decompiler.modules.decompiler.flow.DirectGraph;
import org.jetbrains.java.decompiler.modules.decompiler.flow.FlattenStatementsHelper;
import org.jetbrains.java.decompiler.modules.decompiler.stats.RootStatement;
import org.jetbrains.java.decompiler.modules.decompiler.vars.VarProcessor;
import org.jetbrains.java.decompiler.struct.StructClass;
import org.jetbrains.java.decompiler.struct.StructMethod;
import org.jetbrains.java.decompiler.struct.gen.MethodDescriptor;
import org.jetbrains.java.decompiler.util.DotExporter;

import java.io.IOException;

public class MethodProcessor implements Runnable {
  public static ThreadLocal<RootStatement> debugCurrentlyDecompiling = ThreadLocal.withInitial(() -> null);
  public static ThreadLocal<ControlFlowGraph> debugCurrentCFG = ThreadLocal.withInitial(() -> null);
  public static ThreadLocal<DecompileRecord> debugCurrentDecompileRecord = ThreadLocal.withInitial(() -> null);
  public final Object lock = new Object();

  private final StructClass klass;
  private final StructMethod method;
  private final MethodDescriptor methodDescriptor;
  private final VarProcessor varProc;
  private final DecompilerContext parentContext;

  private volatile RootStatement root;
  private volatile Throwable error;
  private volatile boolean finished = false;

  public MethodProcessor(StructClass klass,
                         StructMethod method,
                         MethodDescriptor methodDescriptor,
                         VarProcessor varProc,
                         DecompilerContext parentContext) {
    this.klass = klass;
    this.method = method;
    this.methodDescriptor = methodDescriptor;
    this.varProc = varProc;
    this.parentContext = parentContext;
  }

  @Override
  public void run() {
    error = null;
    root = null;

    try {
      DecompilerContext.setCurrentContext(parentContext);
      root = codeToJava(klass, method, methodDescriptor, varProc);
    }
    catch (Throwable t) {
      error = t;
    }
    finally {
      DecompilerContext.setCurrentContext(null);
    }

    finished = true;
    synchronized (lock) {
      lock.notifyAll();
    }
  }

  public static RootStatement codeToJava(StructClass cl, StructMethod mt, MethodDescriptor md, VarProcessor varProc) throws IOException {
    debugCurrentlyDecompiling.set(null);
    debugCurrentCFG.set(null);
    debugCurrentDecompileRecord.set(null);

    boolean isInitializer = CodeConstants.CLINIT_NAME.equals(mt.getName()); // for now static initializer only

    mt.expandData(cl);
    InstructionSequence seq = mt.getInstructionSequence();
    ControlFlowGraph graph = new ControlFlowGraph(seq);
    debugCurrentCFG.set(graph);
    DotExporter.toDotFile(graph, mt, "cfgConstructed", true);

    graph.inlineJsr(cl, mt);

    // TODO: move to the start, before jsr inlining
    DeadCodeHelper.connectDummyExitBlock(graph);

    DeadCodeHelper.removeGotos(graph);

    ExceptionDeobfuscator.removeCircularRanges(graph);

    ExceptionDeobfuscator.restorePopRanges(graph);

    //		ExceptionDeobfuscator.restorePopRanges(graph);
    ExceptionDeobfuscator.insertEmptyExceptionHandlerBlocks(graph);

    DeadCodeHelper.mergeBasicBlocks(graph);

    DecompilerContext.getCounterContainer().setCounter(CounterContainer.VAR_COUNTER, mt.getLocalVariables());

    if (ExceptionDeobfuscator.hasObfuscatedExceptions(graph)) {
      DecompilerContext.getLogger().writeMessage("Heavily obfuscated exception ranges found!", IFernflowerLogger.Severity.WARN);
      DotExporter.toDotFile(graph, mt, "cfgExceptionsPre", true);

      if (!ExceptionDeobfuscator.handleMultipleEntryExceptionRanges(graph)) {
        DecompilerContext.getLogger().writeMessage("Found multiple entry exception ranges which could not be splitted", IFernflowerLogger.Severity.WARN);
        graph.addComment("$VF: Could not handle exception ranges with multiple entries");
        graph.addErrorComment = true;
      }

      DotExporter.toDotFile(graph, mt, "cfgMultipleExceptionEntry", true);
      ExceptionDeobfuscator.insertDummyExceptionHandlerBlocks(graph, mt.getBytecodeVersion());
      DotExporter.toDotFile(graph, mt, "cfgMultipleExceptionDummyHandlers", true);
    }

    DotExporter.toDotFile(graph, mt, "cfgParsed", true);
    RootStatement root = DomHelper.parseGraph(graph, mt);

    DecompileRecord decompileRecord = new DecompileRecord(mt);
    debugCurrentDecompileRecord.set(decompileRecord);

    decompileRecord.add("Initial", root);

    debugCurrentlyDecompiling.set(root);

    FinallyProcessor fProc = new FinallyProcessor(mt, md, varProc);
    int finallyProcessed = 0;

    while (fProc.iterateGraph(cl, mt, root, graph)) {
      finallyProcessed++;
      RootStatement oldRoot = root;
      decompileRecord.add("ProcessFinallyOld_" + finallyProcessed, root);
      DotExporter.toDotFile(graph, mt, "cfgProcessFinally_" + finallyProcessed, true);

      root = DomHelper.parseGraph(graph, mt);
      root.addComments(oldRoot);

      decompileRecord.add("ProcessFinally_" + finallyProcessed, root);

      debugCurrentCFG.set(graph);
      debugCurrentlyDecompiling.set(root);
    }

    decompileRecord.add("ProcessFinally_Post", root);

    // remove synchronized exception handler
    // not until now because of comparison between synchronized statements in the finally cycle
    if (DomHelper.removeSynchronizedHandler(root)) {
      decompileRecord.add("RemoveSynchronizedHandler", root);
    }

    root.buildContentFlags();

    //		LabelHelper.lowContinueLabels(root, new HashSet<StatEdge>());

    SequenceHelper.condenseSequences(root);
    decompileRecord.add("CondenseSequences", root);

    ClearStructHelper.clearStatements(root);
    decompileRecord.add("ClearStatements", root);

    // Put exprents in statements
    ExprProcessor proc = new ExprProcessor(md, varProc);
    proc.processStatement(root, cl);
    decompileRecord.add("ProcessStatement", root);

    SequenceHelper.condenseSequences(root);
    decompileRecord.add("CondenseSequences_1", root);

    StackVarsProcessor stackProc = new StackVarsProcessor();

    // Process and simplify variables on the stack
    int stackVarsProcessed = 0;
    do {
      stackVarsProcessed++;

      stackProc.simplifyStackVars(root, mt, cl);
      decompileRecord.add("SimplifyStackVars_PPMM_" + stackVarsProcessed, root);

      varProc.setVarVersions(root);
      decompileRecord.add("SetVarVersions_PPMM_" + stackVarsProcessed, root);
    } while (new PPandMMHelper(varProc).findPPandMM(root));

    // Inline ppi/mmi that we may have missed
    if (PPandMMHelper.inlinePPIandMMIIf(root)) {
      decompileRecord.add("InlinePPIandMMI", root);
    }

    // Process invokedynamic string concat
    if (cl.getVersion().hasIndyStringConcat()) {
      ConcatenationHelper.simplifyStringConcat(root);
      decompileRecord.add("SimplifyStringConcat", root);
    }


    // Main loop
    while (true) {
      decompileRecord.incrementMainLoop();
      decompileRecord.add("Start", root);

      if (root.hasLoops()) {
        // Merge loop
        while (true) {

          decompileRecord.incrementMergeLoop();
          decompileRecord.add("MergeLoopStart", root);

          if (EliminateLoopsHelper.eliminateLoops(root, cl)) {
            decompileRecord.add("EliminateLoops", root);
            continue;
          }

          MergeHelper.enhanceLoops(root);
          decompileRecord.add("EnhanceLoops", root);

          if (LoopExtractHelper.extractLoops(root)) {
            decompileRecord.add("ExtractLoops", root);
            continue;
          }

          if (IfHelper.mergeAllIfs(root)) {
            decompileRecord.add("MergeAllIfs", root);
            // Continues with merge loop
          } else {
            break;
          }
        }

        decompileRecord.resetMergeLoop();
        decompileRecord.add("MergeLoopEnd", root);
      }

      stackProc.simplifyStackVars(root, mt, cl);
      decompileRecord.add("SimplifyStackVars", root);

      varProc.setVarVersions(root);
      decompileRecord.add("SetVarVersions", root);

      if (root.hasTryCatch() && TryHelper.enhanceTryStats(root, cl)) {
        decompileRecord.add("EnhanceTry", root);
        continue;
      }

      if (InlineSingleBlockHelper.inlineSingleBlocks(root)) {
        decompileRecord.add("InlineSingleBlocks", root);
        continue;
      }

      // this has to be done last so it does not screw up the formation of for loops
      if (root.hasLoops() && MergeHelper.makeDoWhileLoops(root)) {
        decompileRecord.add("MatchDoWhile", root);
        continue;
      }

      if (root.hasLoops() && MergeHelper.condenseInfiniteLoopsWithReturn(root)) {
        decompileRecord.add("CondenseDo", root);
        continue;
      }

      // initializer may have at most one return point, so no transformation of method exits permitted
      if (isInitializer || !ExitHelper.condenseExits(root)) {
        break;
      } else {
        decompileRecord.add("CondenseExits", root);
      }

      // FIXME: !!
      //if(!EliminateLoopsHelper.eliminateLoops(root)) {
      //  break;
      //}
    }
    decompileRecord.resetMainLoop();
    decompileRecord.add("MainLoopEnd", root);

    // this has to be done after all inlining is done so the case values do not get reverted
    if (root.hasSwitch() && SwitchHelper.simplifySwitches(root, mt, root)) {
      SequenceHelper.condenseSequences(root); // remove empty blocks
      decompileRecord.add("SimplifySwitches", root);
    }

    // Makes constant returns the same type as the method descriptor
    if (ExitHelper.adjustReturnType(root, md)) {
      decompileRecord.add("AdjustReturnType", root);
    }

    // Remove returns that don't need to exist
    if (ExitHelper.removeRedundantReturns(root)) {
      decompileRecord.add("RedundantReturns", root);
    }

    // Apply post processing transformations
    if (SecondaryFunctionsHelper.identifySecondaryFunctions(root, varProc)) {
      decompileRecord.add("IdentifySecondary", root);
    }

    // Improve synchronized monitor assignments
    if (SynchronizedHelper.cleanSynchronizedVar(root)) {
      decompileRecord.add("ClearSynchronized", root);
    }

    if (SynchronizedHelper.insertSink(root, varProc, root)) {
      decompileRecord.add("InsertSynchronizedAssignments", root);
    }

    varProc.setVarDefinitions(root);
    decompileRecord.add("SetVarDefinitions", root);

    // Make sure to update assignments after setting the var definitions!
    if (SecondaryFunctionsHelper.updateAssignments(root)) {
      decompileRecord.add("UpdateAssignments", root);
    }

    if (GenericsProcessor.qualifyChains(root)) {
      decompileRecord.add("QualifyGenericChains", root);
    }

    if (ExprProcessor.canonicalizeCasts(root)) {
      decompileRecord.add("CanonicalizeCasts", root);
    }

    // Mark oddities in the decompiled code (left behind monitors, <unknown> variables, etc.)
    // No decompile record as statement structure is not modified
    ExprProcessor.markExprOddities(root);

    DotExporter.toDotFile(root, mt, "finalStatement");

    if (DotExporter.DUMP_DOTS) {
      FlattenStatementsHelper flatten = new FlattenStatementsHelper();
      DirectGraph digraph = flatten.buildDirectGraph(root);
      DotExporter.toDotFile(digraph, mt, "finalStatementDigraph");
    }

    // Debug print the decompile record
    DotExporter.toDotFile(decompileRecord, mt, "decompileRecord", false);

    mt.releaseResources();

    return root;
  }

  public RootStatement getResult() throws Throwable {
    Throwable t = error;
    if (t != null) throw t;
    return root;
  }

  public boolean isFinished() {
    return finished;
  }
}
