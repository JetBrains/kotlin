// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.java.decompiler.main.collectors;

public class CounterContainer {
  public static final int STATEMENT_COUNTER = 0;
  public static final int EXPRESSION_COUNTER = 1;
  public static final int VAR_COUNTER = 2;

  private final int[] values = new int[]{1, 1, 1};

  public void setCounter(int counter, int value) {
    values[counter] = value;
  }

  public int getCounter(int counter) {
    return values[counter];
  }

  public int getCounterAndIncrement(int counter) {
    return values[counter]++;
  }
}