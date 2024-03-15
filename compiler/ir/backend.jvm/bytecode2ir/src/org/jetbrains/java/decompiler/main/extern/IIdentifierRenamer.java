// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.java.decompiler.main.extern;

public interface IIdentifierRenamer {

  enum Type {ELEMENT_CLASS, ELEMENT_FIELD, ELEMENT_METHOD}

  boolean toBeRenamed(Type elementType, String className, String element, String descriptor);

  String getNextClassName(String fullName, String shortName);

  String getNextFieldName(String className, String field, String descriptor);

  String getNextMethodName(String className, String method, String descriptor);
}
