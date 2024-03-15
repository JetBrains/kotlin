// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.java.decompiler.main;

import org.jetbrains.java.decompiler.code.CodeConstants;
import org.jetbrains.java.decompiler.main.rels.ClassWrapper;
import org.jetbrains.java.decompiler.main.rels.MethodWrapper;
import org.jetbrains.java.decompiler.modules.decompiler.exps.Exprent;
import org.jetbrains.java.decompiler.modules.decompiler.exps.InvocationExprent;
import org.jetbrains.java.decompiler.modules.decompiler.stats.Statement;
import org.jetbrains.java.decompiler.modules.decompiler.stats.Statements;
import org.jetbrains.java.decompiler.struct.StructClass;
import org.jetbrains.java.decompiler.struct.StructField;
import org.jetbrains.java.decompiler.struct.StructMethod;
import org.jetbrains.java.decompiler.util.InterpreterUtil;

public final class EnumProcessor {
  public static void clearEnum(ClassWrapper wrapper) {
    StructClass cl = wrapper.getClassStruct();

    // hide values/valueOf methods and super() invocations
    for (MethodWrapper method : wrapper.getMethods()) {
      StructMethod mt = method.methodStruct;
      String name = mt.getName();
      String descriptor = mt.getDescriptor();

      if ("values".equals(name)) {
        if (descriptor.equals("()[L" + cl.qualifiedName + ";")) {
          wrapper.getHiddenMembers().add(InterpreterUtil.makeUniqueKey(name, descriptor));
        }
      }
      else if ("valueOf".equals(name)) {
        if (descriptor.equals("(Ljava/lang/String;)L" + cl.qualifiedName + ";")) {
          wrapper.getHiddenMembers().add(InterpreterUtil.makeUniqueKey(name, descriptor));
        }
      }
      else if (CodeConstants.INIT_NAME.equals(name)) {
        Statement firstData = Statements.findFirstData(method.root);
        if (firstData != null && !firstData.getExprents().isEmpty()) {
          Exprent exprent = firstData.getExprents().get(0);
          if (exprent instanceof InvocationExprent) {
            InvocationExprent invExpr = (InvocationExprent)exprent;
            if (Statements.isInvocationInitConstructor(invExpr, method, wrapper, false)) {
              firstData.getExprents().remove(0);
            }
          }
        }
      }
    }

    // hide synthetic fields of enum and it's constants
    for (StructField fd : cl.getFields()) {
      String descriptor = fd.getDescriptor();
      if (fd.isSynthetic() && descriptor.equals("[L" + cl.qualifiedName + ";")) {
        wrapper.getHiddenMembers().add(InterpreterUtil.makeUniqueKey(fd.getName(), descriptor));
      }
    }
  }
}