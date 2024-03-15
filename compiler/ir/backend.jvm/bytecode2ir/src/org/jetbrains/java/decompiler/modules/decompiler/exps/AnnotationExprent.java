/*
 * Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */
package org.jetbrains.java.decompiler.modules.decompiler.exps;

import org.jetbrains.java.decompiler.main.DecompilerContext;
import org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor;
import org.jetbrains.java.decompiler.util.InterpreterUtil;
import org.jetbrains.java.decompiler.util.TextBuffer;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

public class AnnotationExprent extends Exprent {
  public enum Type {
    NORMAL, MARKER, SINGLE_ELEMENT
  }

  private final String className;
  private final List<String> parNames;
  private final List<? extends Exprent> parValues;

  public AnnotationExprent(String className, List<String> parNames, List<? extends Exprent> parValues) {
    super(Exprent.Type.ANNOTATION);
    this.className = className;
    this.parNames = parNames;
    this.parValues = parValues;
  }

  @Override
  protected List<Exprent> getAllExprents(List<Exprent> list) {
    list.addAll(this.parValues);

    return list;
  }

  @Override
  public Exprent copy() {
    List<Exprent> exps = new ArrayList<>();
    for (Exprent v : this.parValues) {
      exps.add(v.copy());
    }

    return new AnnotationExprent(className, parNames, exps);
  }
  
  @Override
  public TextBuffer toJava(int indent) {
    TextBuffer buffer = new TextBuffer();

    buffer.appendIndent(indent);
    buffer.append('@');
    buffer.append(DecompilerContext.getImportCollector().getShortName(ExprProcessor.buildJavaClassName(className)));

    Type type = getAnnotationType();

    if (type != Type.MARKER) {
      buffer.append('(');

      boolean oneLiner = type == Type.SINGLE_ELEMENT || indent < 0;

      for (int i = 0; i < parNames.size(); i++) {
        if (!oneLiner) {
          buffer.appendLineSeparator().appendIndent(indent + 1);
        }

        if (type != Type.SINGLE_ELEMENT) {
          buffer.append(parNames.get(i));
          buffer.append(" = ");
        }

        buffer.append(parValues.get(i).toJava(0));

        if (i < parNames.size() - 1) {
          buffer.append(',');
        }
      }

      if (!oneLiner) {
        buffer.appendLineSeparator().appendIndent(indent);
      }

      buffer.append(')');
    }

    return buffer;
  }

  public String getClassName() {
    return className;
  }

  public Type getAnnotationType() {
    if (parNames.isEmpty()) {
      return Type.MARKER;
    }
    else if (parNames.size() == 1 && "value".equals(parNames.get(0))) {
      return Type.SINGLE_ELEMENT;
    }
    else {
      return Type.NORMAL;
    }
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) return true;
    if (!(o instanceof AnnotationExprent)) return false;

    AnnotationExprent ann = (AnnotationExprent)o;
    return className.equals(ann.className) &&
           InterpreterUtil.equalLists(parNames, ann.parNames) &&
           InterpreterUtil.equalLists(parValues, ann.parValues);
  }
  
  @Override
  public void getBytecodeRange(BitSet values) {
    measureBytecode(values, parValues);
    measureBytecode(values);
  }
}