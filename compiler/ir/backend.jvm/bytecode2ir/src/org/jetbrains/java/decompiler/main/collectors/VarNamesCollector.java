// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.java.decompiler.main.collectors;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class VarNamesCollector {

  private final Set<String> usedNames = new HashSet<>();

  public VarNamesCollector() { }

  public VarNamesCollector(Collection<String> setNames) {
    addNames(setNames);
  }

  public void addName(String value) {
    usedNames.add(value);
  }

  public void addNames(Collection<String> names) {
    usedNames.addAll(names);
  }

  public String getFreeName(int index) {
    return getFreeName("var" + index);
  }

  public String getFreeName(String proposition) {
    while (usedNames.contains(proposition)) {
      proposition += "x";
    }
    addName(proposition);
    return proposition;
  }

  public Set<String> getUsedNames() {
    return Collections.unmodifiableSet(usedNames);
  }
}
