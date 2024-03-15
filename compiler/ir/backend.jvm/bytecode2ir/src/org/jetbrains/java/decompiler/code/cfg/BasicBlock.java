// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.java.decompiler.code.cfg;

import org.jetbrains.java.decompiler.code.Instruction;
import org.jetbrains.java.decompiler.code.InstructionSequence;
import org.jetbrains.java.decompiler.code.SimpleInstructionSequence;
import org.jetbrains.java.decompiler.main.DecompilerContext;
import org.jetbrains.java.decompiler.modules.decompiler.decompose.IGraphNode;

import java.util.ArrayList;
import java.util.List;

public class BasicBlock implements IGraphNode {

  // *****************************************************************************
  // public fields
  // *****************************************************************************

  public final int id;
  public int mark = 0;

  // *****************************************************************************
  // private fields
  // *****************************************************************************

  private InstructionSequence seq = new SimpleInstructionSequence();

  private final List<BasicBlock> preds = new ArrayList<>();
  private final List<BasicBlock> succs = new ArrayList<>();
  private final List<Integer> instrOldOffsets = new ArrayList<>();
  private final List<BasicBlock> predExceptions = new ArrayList<>();
  private final List<BasicBlock> succExceptions = new ArrayList<>();

  public BasicBlock(int id) {
    this.id = id;
  }

  // *****************************************************************************
  // public methods
  // *****************************************************************************

  public Instruction getInstruction(int index) {
    return seq.getInstr(index);
  }

  public Instruction getLastInstruction() {
    if (seq.isEmpty()) {
      return null;
    }
    else {
      return seq.getLastInstr();
    }
  }

  public Integer getOldOffset(int index) {
    if(index < instrOldOffsets.size()) {
      return instrOldOffsets.get(index);
    } else {
      return -1;
    }
  }

  public int size() {
    return seq.length();
  }

  public void addPredecessor(BasicBlock block) {
    preds.add(block);
  }

  public void removePredecessor(BasicBlock block) {
    while (preds.remove(block)) /**/;
  }

  public void addSuccessor(BasicBlock block) {
    succs.add(block);
    block.addPredecessor(this);
  }

  public void removeSuccessor(BasicBlock block) {
    while (succs.remove(block)) /**/;
    block.removePredecessor(this);
  }

  // FIXME: unify block comparisons: id or direct equality
  public void replaceSuccessor(BasicBlock oldBlock, BasicBlock newBlock) {
    if (oldBlock.equals(newBlock)) {
      return;
    }

    for (int i = 0; i < succs.size(); i++) {
      if (succs.get(i).id == oldBlock.id) {
        succs.set(i, newBlock);
        oldBlock.removePredecessor(this);
        newBlock.addPredecessor(this);
      }
    }

    for (int i = 0; i < succExceptions.size(); i++) {
      if (succExceptions.get(i).id == oldBlock.id) {
        succExceptions.set(i, newBlock);
        oldBlock.removePredecessorException(this);
        newBlock.addPredecessorException(this);
      }
    }
  }

  public void addPredecessorException(BasicBlock block) {
    predExceptions.add(block);
  }

  public void removePredecessorException(BasicBlock block) {
    while (predExceptions.remove(block)) /**/;
  }

  public void addSuccessorException(BasicBlock block) {
    if (!succExceptions.contains(block)) {
      succExceptions.add(block);
      block.addPredecessorException(this);
    }
  }

  public void removeSuccessorException(BasicBlock block) {
    while (succExceptions.remove(block)) /**/;
    block.removePredecessorException(this);
  }

  public String toString() {
    return toString(0);
  }

  public String toString(int indent) {

    String new_line_separator = DecompilerContext.getNewLineSeparator();

    return this.id + ":" + new_line_separator + this.seq.toString(indent);
  }

  public boolean isSuccessor(BasicBlock block) {
    for (BasicBlock succ : succs) {
      if (succ.id == block.id) {
        return true;
      }
    }
    return false;
  }

  // *****************************************************************************
  // package methods
  // *****************************************************************************

  BasicBlock cloneBlock(int id) {
    BasicBlock block = new BasicBlock(id);

    block.setSeq(this.seq.clone());
    block.instrOldOffsets.addAll(this.instrOldOffsets);

    return block;
  }

  // *****************************************************************************
  // getter and setter methods
  // *****************************************************************************

  public List<Integer> getInstrOldOffsets() {
    return instrOldOffsets;
  }

  /**
   * Only used for printing debugging strings
   */
  public int getId() {
    return this.id;
  }

  @Override
  public List<? extends IGraphNode> getPredecessors() {
    List<BasicBlock> lst = new ArrayList<>(preds);
    lst.addAll(predExceptions);
    return lst;
  }

  public List<BasicBlock> getPreds() {
    return preds;
  }

  public InstructionSequence getSeq() {
    return seq;
  }

  public void setSeq(InstructionSequence seq) {
    this.seq = seq;
  }

  public List<BasicBlock> getSuccs() {
    return succs;
  }

  public List<BasicBlock> getSuccExceptions() {
    return succExceptions;
  }

  public List<BasicBlock> getPredExceptions() {
    return predExceptions;
  }

  public int getStartInstruction() {
      if (seq.isEmpty()) {
          return 0;
      }
      return instrOldOffsets.get(0);
  }

  public int getEndInstruction() {
      if (seq.isEmpty()) {
          return 0;
      }
      int end = seq.getLastInstr().length;
      return end + instrOldOffsets.get(size() -1);
  }
}