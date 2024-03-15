// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.java.decompiler.struct.attr;

import org.jetbrains.java.decompiler.code.BytecodeVersion;
import org.jetbrains.java.decompiler.struct.consts.ConstantPool;
import org.jetbrains.java.decompiler.struct.consts.LinkConstant;
import org.jetbrains.java.decompiler.struct.consts.PooledConstant;
import org.jetbrains.java.decompiler.util.DataInputFullStream;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class StructBootstrapMethodsAttribute extends StructGeneralAttribute {

  private final List<LinkConstant> methodRefs = new ArrayList<>();
  private final List<List<PooledConstant>> methodArguments = new ArrayList<>();

  @Override
  public void initContent(DataInputFullStream data, ConstantPool pool, BytecodeVersion version) throws IOException {
    int method_number = data.readUnsignedShort();

    for (int i = 0; i < method_number; ++i) {
      int bootstrap_method_ref = data.readUnsignedShort();
      int num_bootstrap_arguments = data.readUnsignedShort();

      List<PooledConstant> list_arguments = new ArrayList<>();

      for (int j = 0; j < num_bootstrap_arguments; ++j) {
        int bootstrap_argument_ref = data.readUnsignedShort();

        list_arguments.add(pool.getConstant(bootstrap_argument_ref));
      }

      methodRefs.add(pool.getLinkConstant(bootstrap_method_ref));
      methodArguments.add(list_arguments);
    }
  }

  public int getMethodsNumber() {
    return methodRefs.size();
  }

  public LinkConstant getMethodReference(int index) {
    return methodRefs.get(index);
  }

  public List<PooledConstant> getMethodArguments(int index) {
    return methodArguments.get(index);
  }
}
