// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.java.decompiler.modules.decompiler.decompose;

import java.util.List;

public interface IGraphNode {

  List<? extends IGraphNode> getPredecessors();
}
