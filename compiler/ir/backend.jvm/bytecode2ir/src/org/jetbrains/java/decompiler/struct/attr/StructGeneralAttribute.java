// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.java.decompiler.struct.attr;

import org.jetbrains.java.decompiler.code.BytecodeVersion;
import org.jetbrains.java.decompiler.struct.consts.ConstantPool;
import org.jetbrains.java.decompiler.util.DataInputFullStream;

import java.io.IOException;

/*
  attribute_info {
    u2 attribute_name_index;
    u4 attribute_length;
    u1 info[attribute_length];
  }
*/
public class StructGeneralAttribute {
  public static final Key<StructCodeAttribute> ATTRIBUTE_CODE = new Key<>("Code");
  public static final Key<StructInnerClassesAttribute> ATTRIBUTE_INNER_CLASSES = new Key<>("InnerClasses");
  public static final Key<StructGenericSignatureAttribute> ATTRIBUTE_SIGNATURE = new Key<>("Signature");
  public static final Key<StructAnnDefaultAttribute> ATTRIBUTE_ANNOTATION_DEFAULT = new Key<>("AnnotationDefault");
  public static final Key<StructExceptionsAttribute> ATTRIBUTE_EXCEPTIONS = new Key<>("Exceptions");
  public static final Key<StructEnclosingMethodAttribute> ATTRIBUTE_ENCLOSING_METHOD = new Key<>("EnclosingMethod");
  public static final Key<StructAnnotationAttribute> ATTRIBUTE_RUNTIME_VISIBLE_ANNOTATIONS = new Key<>("RuntimeVisibleAnnotations");
  public static final Key<StructAnnotationAttribute> ATTRIBUTE_RUNTIME_INVISIBLE_ANNOTATIONS = new Key<>("RuntimeInvisibleAnnotations");
  public static final Key<StructAnnotationParameterAttribute> ATTRIBUTE_RUNTIME_VISIBLE_PARAMETER_ANNOTATIONS = new Key<>("RuntimeVisibleParameterAnnotations");
  public static final Key<StructAnnotationParameterAttribute> ATTRIBUTE_RUNTIME_INVISIBLE_PARAMETER_ANNOTATIONS = new Key<>("RuntimeInvisibleParameterAnnotations");
  public static final Key<StructTypeAnnotationAttribute> ATTRIBUTE_RUNTIME_VISIBLE_TYPE_ANNOTATIONS = new Key<>("RuntimeVisibleTypeAnnotations");
  public static final Key<StructTypeAnnotationAttribute> ATTRIBUTE_RUNTIME_INVISIBLE_TYPE_ANNOTATIONS = new Key<>("RuntimeInvisibleTypeAnnotations");
  public static final Key<StructLocalVariableTableAttribute> ATTRIBUTE_LOCAL_VARIABLE_TABLE = new Key<>("LocalVariableTable");
  public static final Key<StructLocalVariableTypeTableAttribute> ATTRIBUTE_LOCAL_VARIABLE_TYPE_TABLE = new Key<>("LocalVariableTypeTable");
  public static final Key<StructConstantValueAttribute> ATTRIBUTE_CONSTANT_VALUE = new Key<>("ConstantValue");
  public static final Key<StructBootstrapMethodsAttribute> ATTRIBUTE_BOOTSTRAP_METHODS = new Key<>("BootstrapMethods");
  public static final Key<StructGeneralAttribute> ATTRIBUTE_SYNTHETIC = new Key<>("Synthetic");
  public static final Key<StructGeneralAttribute> ATTRIBUTE_DEPRECATED = new Key<>("Deprecated");
  public static final Key<StructLineNumberTableAttribute> ATTRIBUTE_LINE_NUMBER_TABLE = new Key<>("LineNumberTable");
  public static final Key<StructMethodParametersAttribute> ATTRIBUTE_METHOD_PARAMETERS = new Key<>("MethodParameters");
  public static final Key<StructModuleAttribute> ATTRIBUTE_MODULE = new Key<>("Module");
  public static final Key<StructRecordAttribute> ATTRIBUTE_RECORD = new Key<>("Record");
  public static final Key<StructPermittedSubclassesAttribute> ATTRIBUTE_PERMITTED_SUBCLASSES = new Key<>("PermittedSubclasses");
  public static final Key<StructSourceFileAttribute> ATTRIBUTE_SOURCE_FILE = new Key<>("SourceFile");

  @SuppressWarnings("unused")
  public static class Key<T extends StructGeneralAttribute> {
    public final String name;

    public Key(String name) {
      this.name = name;
    }
  }

  public static StructGeneralAttribute createAttribute(String name) {
    if (ATTRIBUTE_CODE.name.equals(name)) {
      return new StructCodeAttribute();
    }
    else if (ATTRIBUTE_INNER_CLASSES.name.equals(name)) {
      return new StructInnerClassesAttribute();
    }
    else if (ATTRIBUTE_CONSTANT_VALUE.name.equals(name)) {
      return new StructConstantValueAttribute();
    }
    else if (ATTRIBUTE_SIGNATURE.name.equals(name)) {
      return new StructGenericSignatureAttribute();
    }
    else if (ATTRIBUTE_ANNOTATION_DEFAULT.name.equals(name)) {
      return new StructAnnDefaultAttribute();
    }
    else if (ATTRIBUTE_EXCEPTIONS.name.equals(name)) {
      return new StructExceptionsAttribute();
    }
    else if (ATTRIBUTE_ENCLOSING_METHOD.name.equals(name)) {
      return new StructEnclosingMethodAttribute();
    }
    else if (ATTRIBUTE_RUNTIME_VISIBLE_ANNOTATIONS.name.equals(name) || ATTRIBUTE_RUNTIME_INVISIBLE_ANNOTATIONS.name.equals(name)) {
      return new StructAnnotationAttribute();
    }
    else if (ATTRIBUTE_RUNTIME_VISIBLE_PARAMETER_ANNOTATIONS.name.equals(name) || ATTRIBUTE_RUNTIME_INVISIBLE_PARAMETER_ANNOTATIONS.name.equals(name)) {
      return new StructAnnotationParameterAttribute();
    }
    else if (ATTRIBUTE_RUNTIME_VISIBLE_TYPE_ANNOTATIONS.name.equals(name) || ATTRIBUTE_RUNTIME_INVISIBLE_TYPE_ANNOTATIONS.name.equals(name)) {
      return new StructTypeAnnotationAttribute();
    }
    else if (ATTRIBUTE_LOCAL_VARIABLE_TABLE.name.equals(name)) {
      return new StructLocalVariableTableAttribute();
    }
    else if (ATTRIBUTE_LOCAL_VARIABLE_TYPE_TABLE.name.equals(name)) {
      return new StructLocalVariableTypeTableAttribute();
    }
    else if (ATTRIBUTE_BOOTSTRAP_METHODS.name.equals(name)) {
      return new StructBootstrapMethodsAttribute();
    }
    else if (ATTRIBUTE_SYNTHETIC.name.equals(name) || ATTRIBUTE_DEPRECATED.name.equals(name)) {
      return new StructGeneralAttribute();
    }
    else if (ATTRIBUTE_LINE_NUMBER_TABLE.name.equals(name)) {
      return new StructLineNumberTableAttribute();
    }
    else if (ATTRIBUTE_METHOD_PARAMETERS.name.equals(name)) {
      return new StructMethodParametersAttribute();
    }
    else if (ATTRIBUTE_MODULE.name.equals(name)) {
      return new StructModuleAttribute();
    }
    else if (ATTRIBUTE_RECORD.name.equals(name)) {
      return new StructRecordAttribute();
    }
    else if (ATTRIBUTE_PERMITTED_SUBCLASSES.name.equals(name)) {
      return new StructPermittedSubclassesAttribute();
    }
    else if (ATTRIBUTE_SOURCE_FILE.name.equals(name)) {
      return new StructSourceFileAttribute();
    }
    else {
      return null; // unsupported attribute
    }
  }

  public void initContent(DataInputFullStream data, ConstantPool pool, BytecodeVersion version) throws IOException { }
}
