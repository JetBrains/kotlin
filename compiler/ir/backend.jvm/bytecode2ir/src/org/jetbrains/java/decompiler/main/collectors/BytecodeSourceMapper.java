// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.java.decompiler.main.collectors;

import org.jetbrains.java.decompiler.main.DecompilerContext;
import org.jetbrains.java.decompiler.util.TextBuffer;

import java.util.*;
import java.util.Map.Entry;

public class BytecodeSourceMapper {
  private int offset_total;

  // class, method, bytecode offset, source line
  private final Map<String, Map<String, Map<Integer, Integer>>> mapping = new LinkedHashMap<>();

  // original line to decompiled line
  private final Map<Integer, Integer> linesMapping = new HashMap<>();
  private final Set<Integer> unmappedLines = new TreeSet<>();

  public void addMapping(String className, String methodName, int bytecodeOffset, int sourceLine) {
    Map<String, Map<Integer, Integer>> class_mapping = mapping.computeIfAbsent(className, k -> new LinkedHashMap<>()); // need to preserve order
    Map<Integer, Integer> method_mapping = class_mapping.computeIfAbsent(methodName, k -> new HashMap<>());

    // don't overwrite
    method_mapping.putIfAbsent(bytecodeOffset, sourceLine);
  }

  public void addTracer(String className, String methodName, BytecodeMappingTracer tracer) {
    for (Entry<Integer, Integer> entry : tracer.getMapping().entrySet()) {
      addMapping(className, methodName, entry.getKey(), entry.getValue());
    }
    linesMapping.putAll(tracer.getOriginalLinesMapping());
    unmappedLines.addAll(tracer.getUnmappedLines());
  }

  public void dumpMapping(TextBuffer buffer, boolean offsetsToHex) {
    if (mapping.isEmpty() && linesMapping.isEmpty()) {
      return;
    }

    String lineSeparator = DecompilerContext.getNewLineSeparator();

    for (Entry<String, Map<String, Map<Integer, Integer>>> class_entry : mapping.entrySet()) {
      Map<String, Map<Integer, Integer>> class_mapping = class_entry.getValue();
      buffer.append("class '" + class_entry.getKey() + "' {" + lineSeparator);

      boolean is_first_method = true;
      for (Entry<String, Map<Integer, Integer>> method_entry : class_mapping.entrySet()) {
        Map<Integer, Integer> method_mapping = method_entry.getValue();

        if (!is_first_method) {
          buffer.appendLineSeparator();
        }

        buffer.appendIndent(1).append("method '" + method_entry.getKey() + "' {" + lineSeparator);

        List<Integer> lstBytecodeOffsets = new ArrayList<>(method_mapping.keySet());
        Collections.sort(lstBytecodeOffsets);

        for (int offset : lstBytecodeOffsets) {
          Integer line = method_mapping.get(offset);

          String strOffset = offsetsToHex ? Integer.toHexString(offset) : line.toString();
          buffer.appendIndent(2).append(strOffset).appendIndent(2).append((line + offset_total) + lineSeparator);
        }
        buffer.appendIndent(1).append("}").appendLineSeparator();

        is_first_method = false;
      }

      buffer.append("}").appendLineSeparator().appendLineSeparator();
    }

    // lines mapping
    buffer.append("Lines mapping:").appendLineSeparator();
    Map<Integer, Integer> sorted = new TreeMap<>(linesMapping);
    for (Entry<Integer, Integer> entry : sorted.entrySet()) {
      buffer.append(entry.getKey()).append(" <-> ").append(entry.getValue() + offset_total + 1).appendLineSeparator();
    }

    if (!unmappedLines.isEmpty()) {
      buffer.append("Not mapped:").appendLineSeparator();
      for (int line : unmappedLines) {
        if (!linesMapping.containsKey(line)) {
          buffer.append(line).appendLineSeparator();
        }
      }
    }
  }

  public void addTotalOffset(int offset_total) {
    this.offset_total += offset_total;
  }

  /**
   * Original to decompiled line mapping.
   */
  public int[] getOriginalLinesMapping() {
    int[] res = new int[linesMapping.size() * 2];
    int i = 0;
    for (Entry<Integer, Integer> entry : linesMapping.entrySet()) {
      res[i] = entry.getKey();
      unmappedLines.remove(entry.getKey());
      res[i + 1] = entry.getValue() + offset_total + 1; // make it 1 based
      i += 2;
    }
    return res;
  }
}