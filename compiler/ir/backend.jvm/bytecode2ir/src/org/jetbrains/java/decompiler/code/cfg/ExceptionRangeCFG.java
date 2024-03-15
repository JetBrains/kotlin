// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.java.decompiler.code.cfg;

import org.jetbrains.java.decompiler.main.DecompilerContext;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ExceptionRangeCFG {
  private final List<BasicBlock> protectedRange; // FIXME: replace with set
  private BasicBlock handler;
  private List<String> exceptionTypes;

  public ExceptionRangeCFG(List<BasicBlock> protectedRange, BasicBlock handler, List<String> exceptionType) {
    this.protectedRange = protectedRange;
    this.handler = handler;

    if (exceptionType != null) {
      this.exceptionTypes = new ArrayList<>(exceptionType);
    }
  }

  public ExceptionRangeCFG(List<BasicBlock> protectedRange, BasicBlock handler, String exceptionType) {
    this.protectedRange = protectedRange;
    this.handler = handler;

    if (exceptionType != null) {
      this.exceptionTypes = new ArrayList<>();
      this.exceptionTypes.add(exceptionType);
    }
  }

  public boolean isCircular() {
    return protectedRange.contains(handler);
  }

  @Override
  public String toString() {
    String new_line_separator = DecompilerContext.getNewLineSeparator();
    StringBuilder buf = new StringBuilder();

    buf.append("exceptionType:");

    if (exceptionTypes == null) {
      buf.append(" null");
    }
    else {
      for (String exception_type : exceptionTypes) {
        buf.append(" ").append(exception_type);
      }
    }

    buf.append(new_line_separator);

    buf.append("handler: ").append(handler.getId()).append(new_line_separator);
    buf.append("range: ");
    for (BasicBlock block : protectedRange) {
      buf.append(block.getId()).append(" ");
    }
    buf.append(new_line_separator);

    return buf.toString();
  }

  public BasicBlock getHandler() {
    return handler;
  }

  public void setHandler(BasicBlock handler) {
    this.handler = handler;
  }

  public List<BasicBlock> getProtectedRange() {
    return protectedRange;
  }

  public List<String> getExceptionTypes() {
    return this.exceptionTypes;
  }

  public void addExceptionType(String exceptionType) {
    if (this.exceptionTypes == null) {
      return;
    }

    if (exceptionType == null) {
      this.exceptionTypes = null;
    }
    else {
      this.exceptionTypes.add(exceptionType);
    }
  }

  public String getUniqueExceptionsString() {
    return exceptionTypes != null ? exceptionTypes.stream().distinct().collect(Collectors.joining(":")) : null;
  }
}