/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.debugger.test.util;

import com.intellij.xdebugger.frame.XCompositeNode;
import com.intellij.xdebugger.frame.XValue;
import com.intellij.xdebugger.frame.XValueChildrenList;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class XTestCompositeNode extends XTestContainer<XValue> implements XCompositeNode {
  @Override
  public void addChildren(@NotNull XValueChildrenList children, boolean last) {
    final List<XValue> list = new ArrayList<>();
    for (int i = 0; i < children.size(); i++) {
      list.add(children.getValue(i));
    }
    addChildren(list, last);
  }

  @Override
  public void setAlreadySorted(boolean alreadySorted) {
  }
}
