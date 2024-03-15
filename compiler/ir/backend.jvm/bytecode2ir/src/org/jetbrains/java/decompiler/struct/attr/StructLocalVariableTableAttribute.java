// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.java.decompiler.struct.attr;

import org.jetbrains.java.decompiler.code.BytecodeVersion;
import org.jetbrains.java.decompiler.modules.decompiler.stats.Statement;
import org.jetbrains.java.decompiler.modules.decompiler.vars.VarVersionPair;
import org.jetbrains.java.decompiler.struct.consts.ConstantPool;
import org.jetbrains.java.decompiler.struct.gen.VarType;
import org.jetbrains.java.decompiler.util.DataInputFullStream;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/*
  u2 local_variable_table_length;
  local_variable {
    u2 start_pc;
    u2 length;
    u2 name_index;
    u2 descriptor_index;
    u2 index;
  }
*/
public class StructLocalVariableTableAttribute extends StructGeneralAttribute {
  private List<LocalVariable> localVariables = Collections.emptyList();
  private Map<Integer, Integer> indexVersion = new HashMap<>();

  @Override
  public void initContent(DataInputFullStream data, ConstantPool pool, BytecodeVersion version) throws IOException {
    int len = data.readUnsignedShort();
    if (len > 0) {
      localVariables = new ArrayList<>(len);
      indexVersion = new HashMap<>();

      for (int i = 0; i < len; i++) {
        int start_pc = data.readUnsignedShort();
        int length = data.readUnsignedShort();
        int nameIndex = data.readUnsignedShort();
        int descriptorIndex = data.readUnsignedShort();
        int varIndex = data.readUnsignedShort();
        localVariables.add(new LocalVariable(start_pc,
                                             length,
                                             pool.getPrimitiveConstant(nameIndex).getString(),
                                             pool.getPrimitiveConstant(descriptorIndex).getString(),
                                             varIndex));
      }
      Collections.sort(localVariables);
      versionVariables(localVariables);
    }
    else {
      localVariables = Collections.emptyList();
    }
  }

  public void add(StructLocalVariableTableAttribute attr) {
    localVariables.addAll(attr.localVariables);
    versionVariables(localVariables);
  }

  public String getName(int index, int visibleOffset) {
    return matchingVars(index, visibleOffset).map(v -> v.name).findFirst().orElse(null);
  }

  public String getDescriptor(int index, int visibleOffset) {
    return matchingVars(index, visibleOffset).map(v -> v.descriptor).findFirst().orElse(null);
  }

  public Stream<LocalVariable> matchingVars(int index, int visibleOffset) {
    return localVariables.stream()
      .filter(v -> v.index == index && (visibleOffset >= v.start_pc && visibleOffset < v.start_pc + v.length));
  }

  public Stream<LocalVariable> matchingVars(int index) {
    return localVariables.stream().filter(v -> v.index == index);
  }

  public Stream<LocalVariable> matchingVars(Statement stat) {
    BitSet values = new BitSet();
    stat.getOffset(values);
    return getRange(values.nextSetBit(0), values.length() - 1);
  }

  public Stream<LocalVariable> getRange(int start, int end) {
    return localVariables.stream().filter(v -> v.getStart() >= start && v.getEnd() <= end);
  }

  public boolean containsName(String name) {
    return localVariables.stream().anyMatch(v -> Objects.equals(v.name, name));
  }

  public Map<VarVersionPair, String> getMapNames() {
    return localVariables.stream().collect(Collectors.toMap(v -> v.version, v -> v.name, (n1, n2) -> n2));
  }

  public Stream<LocalVariable> getVariables() {
    return localVariables.stream();
  }
  
  private void versionVariables(List<LocalVariable> vars) {
    for (LocalVariable var : vars) {
      Integer version = indexVersion.get(var.index);
      version = version == null ? 1 : version++;
      indexVersion.put(var.index, version);
      var.version = new VarVersionPair(var.index, version.intValue());
    }
  }

  public void mergeSignatures(StructLocalVariableTypeTableAttribute lvtt) {
      lvtt.backingAttribute.localVariables.stream().forEach(type -> localVariables.stream().filter(t -> t.compareTo(type) == 0).findFirst().ifPresent(lv -> lv.signature = type.descriptor));
  }

  public static class LocalVariable implements Comparable<LocalVariable> {
    final int start_pc;
    final int length;
    final String name;
    final String descriptor;
    final int index;
    private String signature;
    private VarVersionPair version;

    private LocalVariable(int start_pc, int length, String name, String descriptor, int index) {
      this.start_pc = start_pc;
      this.length = length;
      this.name = name;
      this.descriptor = descriptor;
      this.index = index;
      this.version = new VarVersionPair(index, 0);
    }

    @Override
    public int compareTo(LocalVariable o) {
      if (this.index != o.index) return this.index - o.index;
      if (this.start_pc != o.start_pc) return this.start_pc - o.start_pc;
      return this.length - o.length;
    }
    
    public String getName() {
      return name;
    }

    public String getDescriptor() {
      return descriptor;
    }

    public String getSignature() {
      return signature;
    }
    
    public int getStart() {
      return start_pc;
    }

    public int getEnd() {
      return start_pc + length;
    }

    public VarVersionPair getVersion() {
      return version;
    }

    public VarType getVarType() {
      return new VarType(descriptor);
    }
    
    @Override
    public String toString() {
      return "\'("+index+","+start_pc+'-'+getEnd()+")"+descriptor+(signature!=null ? "<"+signature+"> ":" ")+name+"\'";
    }

    public LocalVariable rename(String newName) {
      LocalVariable ret = new LocalVariable(start_pc, length, newName, descriptor, index);
      ret.signature = signature;
      return ret;
    }
  }
}
