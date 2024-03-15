/*
 * Copyright 2000-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jetbrains.java.decompiler.util;

import java.util.List;

import org.jetbrains.java.decompiler.modules.decompiler.exps.Exprent;
import org.jetbrains.java.decompiler.modules.decompiler.flow.DirectGraph.ExprentIterator;
import org.jetbrains.java.decompiler.modules.decompiler.stats.Statement;

public class StatementIterator {
  public static void iterate(Statement stat, ExprentIterator itr) {
    if (stat == null) {
      return;
    }

    for (Exprent exp : stat.getVarDefinitions()) {
      iterate(exp, itr);
    }

    if (stat.getExprents() == null) {
      for (Object obj : stat.getSequentialObjects()) {
        if (obj instanceof Statement) {
          iterate((Statement)obj, itr);
        }
        else if (obj instanceof Exprent) {
          iterate((Exprent)obj, itr);
        }
      }
    }
    else {
      for (Exprent exp : stat.getExprents()) {
        iterate(exp, itr);
      }
    }
  }

  private static void iterate(Exprent exp, ExprentIterator itr) {
    List<Exprent> lst = exp.getAllExprents(true);
    lst.add(exp);
    for (Exprent exprent : lst) {
      itr.processExprent(exprent);
    }
  }
}
