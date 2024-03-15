// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.java.decompiler.modules.decompiler;

import org.jetbrains.java.decompiler.modules.decompiler.stats.RootStatement;
import org.jetbrains.java.decompiler.modules.decompiler.stats.Statement;

import java.util.LinkedList;


public final class ClearStructHelper {

  public static void clearStatements(RootStatement root) {

    LinkedList<Statement> stack = new LinkedList<>();
    stack.add(root);

    while (!stack.isEmpty()) {

      Statement stat = stack.removeFirst();

      stat.clearTempInformation();

      stack.addAll(stat.getStats());
    }
  }
}
