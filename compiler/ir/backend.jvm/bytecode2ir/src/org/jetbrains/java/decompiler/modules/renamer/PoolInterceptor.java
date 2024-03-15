// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.java.decompiler.modules.renamer;

import java.util.HashMap;
import java.util.Map;

public class PoolInterceptor {
  private final Map<String, String> mapOldToNewNames = new HashMap<>();
  private final Map<String, String> mapNewToOldNames = new HashMap<>();

  public void addName(String oldName, String newName) {
    mapOldToNewNames.put(oldName, newName);
    mapNewToOldNames.put(newName, oldName);
  }

  public String getName(String oldName) {
    return mapOldToNewNames.get(oldName);
  }

  public String getOldName(String newName) {
    return mapNewToOldNames.get(newName);
  }
}