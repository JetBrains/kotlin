// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.java.decompiler.struct.gen.generics;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.java.decompiler.struct.gen.VarType;

public class GenericClassDescriptor {

  public VarType superclass;

  public GenericType genericType;

  public final List<VarType> superinterfaces = new ArrayList<>();

  public final List<String> fparameters = new ArrayList<>();

  public final List<List<VarType>> fbounds = new ArrayList<>();
}
