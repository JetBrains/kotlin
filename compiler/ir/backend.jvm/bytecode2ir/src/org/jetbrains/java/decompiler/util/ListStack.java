// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.java.decompiler.util;

import java.util.ArrayList;
import java.util.Collection;

public class ListStack<T> extends ArrayList<T> {
  public ListStack() {
    super();
  }

  public ListStack(Collection<? extends T> list) {
    super(list);
  }

  @Override
  @SuppressWarnings("MethodDoesntCallSuperMethod")
  public ListStack<T> clone() {
    return new ListStack<>(this);
  }

  public void push(T item) {
    this.add(item);
  }

  public T peek() {
    return this.get(this.size() - 1);
  }

  public T pop() {
    return this.remove(this.size() - 1);
  }

  public T pop(int count) {
    T o = null;
    for (int i = count; i > 0; i--) {
      o = this.pop();
    }
    return o;
  }

  public void removeMultiple(int count) {
    this.pop(count);
  }

  public T getByOffset(int offset) {
    return this.get(this.size() + offset);
  }

  public void insertByOffset(int offset, T item) {
    this.add(this.size() + offset, item);
  }
}