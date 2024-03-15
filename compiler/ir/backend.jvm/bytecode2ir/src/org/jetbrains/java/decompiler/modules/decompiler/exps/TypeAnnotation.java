// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.java.decompiler.modules.decompiler.exps;

public class TypeAnnotation {
  public static final int CLASS_TYPE_PARAMETER = 0x00;
  public static final int METHOD_TYPE_PARAMETER = 0x01;
  public static final int SUPER_TYPE_REFERENCE = 0x10;
  public static final int CLASS_TYPE_PARAMETER_BOUND = 0x11;
  public static final int METHOD_TYPE_PARAMETER_BOUND = 0x12;
  public static final int FIELD = 0x13;
  public static final int METHOD_RETURN_TYPE = 0x14;
  public static final int METHOD_RECEIVER = 0x15;
  public static final int METHOD_PARAMETER = 0x16;
  public static final int THROWS_REFERENCE = 0x17;
  public static final int LOCAL_VARIABLE = 0x40;
  public static final int RESOURCE_VARIABLE = 0x41;
  public static final int CATCH_CLAUSE = 0x42;
  public static final int EXPR_INSTANCEOF = 0x43;
  public static final int EXPR_NEW = 0x44;
  public static final int EXPR_CONSTRUCTOR_REF = 0x45;
  public static final int EXPR_METHOD_REF = 0x46;
  public static final int TYPE_ARG_CAST = 0x47;
  public static final int TYPE_ARG_CONSTRUCTOR_CALL = 0x48;
  public static final int TYPE_ARG_METHOD_CALL = 0x49;
  public static final int TYPE_ARG_CONSTRUCTOR_REF = 0x4A;
  public static final int TYPE_ARG_METHOD_REF = 0x4B;

  private final int target;
  private final byte[] path;
  private final AnnotationExprent annotation;

  public TypeAnnotation(int target, byte[] path, AnnotationExprent annotation) {
    this.target = target;
    this.path = path;
    this.annotation = annotation;
  }

  public int getTargetType() {
    return target >> 24;
  }

  public int getIndex() {
    return target & 0x0FFFF;
  }

  public boolean isTopLevel() {
    return path == null;
  }

  public AnnotationExprent getAnnotation() {
    return annotation;
  }
}