// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.java.decompiler.struct.consts;

public class PrimitiveConstant extends PooledConstant {
  public int index;
  public Object value;
  public boolean isArray;

  public PrimitiveConstant(int type, Object value) {
    super(type);
    this.value = value;

    initConstant();
  }

  public PrimitiveConstant(int type, int index) {
    super(type);
    this.index = index;
  }

  private void initConstant() {
    if (type == CONSTANT_Class) {
      String className = getString();
      isArray = (className.length() > 0 && className.charAt(0) == '['); // empty string for a class name seems to be possible in some android files
    }
  }

  public String getString() {
    return (String)value;
  }

  @Override
  public void resolveConstant(ConstantPool pool) {
    if (type == CONSTANT_Class || type == CONSTANT_String || type == CONSTANT_MethodType || type == CONSTANT_Module || type == CONSTANT_Package) {
      value = pool.getPrimitiveConstant(index).getString();
      initConstant();
    }
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) return true;
    if (!(o instanceof PrimitiveConstant)) return false;

    PrimitiveConstant cn = (PrimitiveConstant)o;
    return this.type == cn.type &&
           this.isArray == cn.isArray &&
           this.value.equals(cn.value);
  }
}