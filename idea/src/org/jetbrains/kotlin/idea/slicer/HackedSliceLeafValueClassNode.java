/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.slicer;

import com.intellij.openapi.project.Project;
import com.intellij.slicer.JavaSliceUsage;
import com.intellij.slicer.SliceLeafValueRootNode;
import com.intellij.slicer.SliceNode;
import com.intellij.slicer.SliceUsageCellRendererBase;
import com.intellij.ui.SimpleTextAttributes;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.ArrayList;

class HackedSliceLeafValueClassNode extends SliceLeafValueRootNode {
  private final String myClassName;

  HackedSliceLeafValueClassNode(@NotNull Project project, @NotNull SliceNode root, @NotNull String className) {
    super(project,
          root,
          JavaSliceUsage.createRootUsage(root.getValue().getElement(), root.getValue().params),
          new ArrayList<>());
    myClassName = className;
  }

  @Override
  public boolean canNavigate() {
    return false;
  }

  @Override
  public boolean canNavigateToSource() {
    return false;
  }

  @Override
  public void customizeCellRenderer(@NotNull SliceUsageCellRendererBase renderer,
          @NotNull JTree tree,
          Object value,
          boolean selected,
          boolean expanded,
          boolean leaf,
          int row,
          boolean hasFocus) {
    renderer.append(myClassName, SimpleTextAttributes.DARK_TEXT);
  }

  @Override
  public String getNodeText() {
    return myClassName;
  }
}
