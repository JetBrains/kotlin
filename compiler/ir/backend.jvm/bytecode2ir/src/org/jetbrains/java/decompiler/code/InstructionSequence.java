// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.java.decompiler.code;

import org.jetbrains.java.decompiler.main.DecompilerContext;
import org.jetbrains.java.decompiler.util.TextUtil;
import org.jetbrains.java.decompiler.util.VBStyleCollection;

import java.util.Iterator;

public abstract class InstructionSequence implements Iterable<Instruction> {

  // *****************************************************************************
  // private fields
  // *****************************************************************************

  protected final VBStyleCollection<Instruction, Integer> collinstr;

  protected int pointer = 0;

  protected ExceptionTable exceptionTable = ExceptionTable.EMPTY;

  protected InstructionSequence() {
    this(new VBStyleCollection<>());
  }

  protected InstructionSequence(VBStyleCollection<Instruction, Integer> collinstr) {
    this.collinstr = collinstr;
  }

  // *****************************************************************************
  // public methods
  // *****************************************************************************

  // to nbe overwritten
  @Override
  public InstructionSequence clone() {
    return null;
  }

  public void clear() {
    collinstr.clear();
    pointer = 0;
    exceptionTable = ExceptionTable.EMPTY;
  }

  public void addInstruction(Instruction inst, int offset) {
    collinstr.addWithKey(inst, offset);
  }

  public void addInstruction(int index, Instruction inst, int offset) {
    collinstr.addWithKeyAndIndex(index, inst, offset);
  }

  public void addSequence(InstructionSequence seq) {
    for (int i = 0; i < seq.length(); i++) {
      addInstruction(seq.getInstr(i), -1); // TODO: any sensible value possible?
    }
  }

  public void removeInstruction(int index) {
    collinstr.remove(index);
  }

  public void removeInstruction(Instruction inst) {
    // VBStyle remove(Object) is not implemented
    collinstr.removeIf(i -> i == inst);
  }

  public void removeLast() {
    if (!collinstr.isEmpty()) {
      collinstr.remove(collinstr.size() - 1);
    }
  }

  public Instruction getInstr(int index) {
  return collinstr.get(index);
  }

  public Instruction getLastInstr() {
  return collinstr.getLast();
  }

  public int getOffset(int index) {
    return collinstr.getKey(index);
  }

  public int getPointerByAbsOffset(int offset) {
    if (collinstr.containsKey(offset)) {
      return collinstr.getIndexByKey(offset);
    }
    else {
      return -1;
    }
  }

  public int getPointerByRelOffset(int offset) {
    int absoffset = collinstr.getKey(pointer) + offset;
    if (collinstr.containsKey(absoffset)) {
      return collinstr.getIndexByKey(absoffset);
    }
    else {
      return -1;
    }
  }

  public int length() {
    return collinstr.size();
  }

  public boolean isEmpty() {
    return collinstr.isEmpty();
  }

  public void addToPointer(int diff) {
    this.pointer += diff;
  }

  public String toString() {
    return toString(0);
  }

  public String toString(int indent) {

    String new_line_separator = DecompilerContext.getNewLineSeparator();

    StringBuilder buf = new StringBuilder();

    for (int i = 0; i < collinstr.size(); i++) {
    buf.append(TextUtil.getIndentString(indent));
      buf.append((int) collinstr.getKey(i));
      buf.append(": ");
      buf.append(collinstr.get(i).toString());
      buf.append(new_line_separator);
    }

    return buf.toString();
  }

  // *****************************************************************************
  // getter and setter methods
  // *****************************************************************************

  public int getPointer() {
    return pointer;
  }

  public void setPointer(int pointer) {
    this.pointer = pointer;
  }

  public ExceptionTable getExceptionTable() {
    return exceptionTable;
  }

  @Override
  public Iterator<Instruction> iterator() {
    return this.collinstr.iterator();
  }
}