// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.java.decompiler.struct.attr;

import org.jetbrains.java.decompiler.code.BytecodeVersion;
import org.jetbrains.java.decompiler.code.CodeConstants;
import org.jetbrains.java.decompiler.modules.decompiler.exps.*;
import org.jetbrains.java.decompiler.struct.consts.ConstantPool;
import org.jetbrains.java.decompiler.struct.consts.PrimitiveConstant;
import org.jetbrains.java.decompiler.struct.gen.FieldDescriptor;
import org.jetbrains.java.decompiler.struct.gen.VarType;
import org.jetbrains.java.decompiler.util.DataInputFullStream;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class StructAnnotationAttribute extends StructGeneralAttribute {
  private List<AnnotationExprent> annotations;

  @Override
  public void initContent(DataInputFullStream data, ConstantPool pool, BytecodeVersion version) throws IOException {
    annotations = parseAnnotations(pool, data);
  }

  public static List<AnnotationExprent> parseAnnotations(ConstantPool pool, DataInputStream data) throws IOException {
    int len = data.readUnsignedShort();
    if (len > 0) {
      List<AnnotationExprent> annotations = new ArrayList<>(len);
      for (int i = 0; i < len; i++) {
        annotations.add(parseAnnotation(data, pool));
      }
      return annotations;
    }
    else {
      return Collections.emptyList();
    }
  }

  public static AnnotationExprent parseAnnotation(DataInputStream data, ConstantPool pool) throws IOException {
    String className = pool.getPrimitiveConstant(data.readUnsignedShort()).getString();

    List<String> names;
    List<Exprent> values;
    int len = data.readUnsignedShort();
    if (len > 0) {
      names = new ArrayList<>(len);
      values = new ArrayList<>(len);
      for (int i = 0; i < len; i++) {
        names.add(pool.getPrimitiveConstant(data.readUnsignedShort()).getString());
        values.add(parseAnnotationElement(data, pool));
      }
    }
    else {
      names = Collections.emptyList();
      values = Collections.emptyList();
    }

    return new AnnotationExprent(new VarType(className).value, names, values);
  }

  public static Exprent parseAnnotationElement(DataInputStream data, ConstantPool pool) throws IOException {
    int tag = data.readUnsignedByte();

    switch (tag) {
      case 'e': // enum constant
        String className = pool.getPrimitiveConstant(data.readUnsignedShort()).getString();
        String constName = pool.getPrimitiveConstant(data.readUnsignedShort()).getString();
        FieldDescriptor descr = FieldDescriptor.parseDescriptor(className);
        return new FieldExprent(constName, descr.type.value, true, null, descr, null);

      case 'c': // class
        String descriptor = pool.getPrimitiveConstant(data.readUnsignedShort()).getString();
        VarType type = FieldDescriptor.parseDescriptor(descriptor).type;

        String value;
        switch (type.type) {
          case CodeConstants.TYPE_OBJECT:
            value = type.value;
            break;
          case CodeConstants.TYPE_BYTE:
            value = byte.class.getName();
            break;
          case CodeConstants.TYPE_CHAR:
            value = char.class.getName();
            break;
          case CodeConstants.TYPE_DOUBLE:
            value = double.class.getName();
            break;
          case CodeConstants.TYPE_FLOAT:
            value = float.class.getName();
            break;
          case CodeConstants.TYPE_INT:
            value = int.class.getName();
            break;
          case CodeConstants.TYPE_LONG:
            value = long.class.getName();
            break;
          case CodeConstants.TYPE_SHORT:
            value = short.class.getName();
            break;
          case CodeConstants.TYPE_BOOLEAN:
            value = boolean.class.getName();
            break;
          case CodeConstants.TYPE_VOID:
            value = void.class.getName();
            break;
          default:
            throw new RuntimeException("invalid class type: " + type.type);
        }
        return new ConstExprent(VarType.VARTYPE_CLASS, value, null);

      case '[': // array
        List<Exprent> elements = Collections.emptyList();
        int len = data.readUnsignedShort();
        if (len > 0) {
          elements = new ArrayList<>(len);
          for (int i = 0; i < len; i++) {
            elements.add(parseAnnotationElement(data, pool));
          }
        }

        VarType newType;
        if (elements.isEmpty()) {
          newType = new VarType(CodeConstants.TYPE_OBJECT, 1, "java/lang/Object");
        }
        else {
          VarType elementType = elements.get(0).getExprType();
          newType = new VarType(elementType.type, 1, elementType.value);
        }

        NewExprent newExpr = new NewExprent(newType, Collections.emptyList(), null);
        newExpr.setDirectArrayInit(true);
        newExpr.setLstArrayElements(elements);
        return newExpr;

      case '@': // annotation
        return parseAnnotation(data, pool);

      default:
        PrimitiveConstant cn = pool.getPrimitiveConstant(data.readUnsignedShort());
        switch (tag) {
          case 'B':
            return new ConstExprent(VarType.VARTYPE_BYTE, cn.value, null);
          case 'C':
            return new ConstExprent(VarType.VARTYPE_CHAR, cn.value, null);
          case 'D':
            return new ConstExprent(VarType.VARTYPE_DOUBLE, cn.value, null);
          case 'F':
            return new ConstExprent(VarType.VARTYPE_FLOAT, cn.value, null);
          case 'I':
            return new ConstExprent(VarType.VARTYPE_INT, cn.value, null);
          case 'J':
            return new ConstExprent(VarType.VARTYPE_LONG, cn.value, null);
          case 'S':
            return new ConstExprent(VarType.VARTYPE_SHORT, cn.value, null);
          case 'Z':
            return new ConstExprent(VarType.VARTYPE_BOOLEAN, cn.value, null);
          case 's':
            return new ConstExprent(VarType.VARTYPE_STRING, cn.value, null);
          default:
            throw new RuntimeException("invalid element type!");
        }
    }
  }

  public List<AnnotationExprent> getAnnotations() {
    return annotations;
  }
}