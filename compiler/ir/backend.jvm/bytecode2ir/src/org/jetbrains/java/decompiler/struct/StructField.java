// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.java.decompiler.struct;

import org.jetbrains.java.decompiler.code.BytecodeVersion;
import org.jetbrains.java.decompiler.main.DecompilerContext;
import org.jetbrains.java.decompiler.main.extern.IFernflowerPreferences;
import org.jetbrains.java.decompiler.struct.attr.StructGeneralAttribute;
import org.jetbrains.java.decompiler.struct.attr.StructGenericSignatureAttribute;
import org.jetbrains.java.decompiler.struct.consts.ConstantPool;
import org.jetbrains.java.decompiler.struct.gen.generics.GenericFieldDescriptor;
import org.jetbrains.java.decompiler.struct.gen.generics.GenericMain;
import org.jetbrains.java.decompiler.struct.gen.generics.GenericMethodDescriptor;
import org.jetbrains.java.decompiler.util.DataInputFullStream;

import java.io.IOException;
import java.util.Map;

/*
  field_info {
    u2 access_flags;
    u2 name_index;
    u2 descriptor_index;
    u2 attributes_count;
    attribute_info attributes[attributes_count];
   }
*/
public class StructField extends StructMember {
  public static StructField create(DataInputFullStream in, ConstantPool pool, String clQualifiedName, BytecodeVersion version) throws IOException {
    int accessFlags = in.readUnsignedShort();
    int nameIndex = in.readUnsignedShort();
    int descriptorIndex = in.readUnsignedShort();

    String[] values = pool.getClassElement(ConstantPool.FIELD, clQualifiedName, nameIndex, descriptorIndex);

    Map<String, StructGeneralAttribute> attributes = readAttributes(in, pool, version);
    GenericFieldDescriptor signature = null;
    if (DecompilerContext.getOption(IFernflowerPreferences.DECOMPILE_GENERIC_SIGNATURES)) {
      StructGenericSignatureAttribute signatureAttr = (StructGenericSignatureAttribute)attributes.get(StructGeneralAttribute.ATTRIBUTE_SIGNATURE.name);
      if (signatureAttr != null) {
        signature = GenericMain.parseFieldSignature(signatureAttr.getSignature());
      }
    }

    return new StructField(accessFlags, attributes, values[0], values[1], signature, version);
  }

  private final String name;
  private final String descriptor;
  private final GenericFieldDescriptor signature;
  private final BytecodeVersion version;

  protected StructField(int accessFlags, Map<String, StructGeneralAttribute> attributes, String name, String descriptor, BytecodeVersion version) {
    this(accessFlags, attributes, name, descriptor, null, version);
  }

  protected StructField(int accessFlags, Map<String, StructGeneralAttribute> attributes, String name, String descriptor, GenericFieldDescriptor signature, BytecodeVersion version) {
    super(accessFlags, attributes);
    this.name = name;
    this.descriptor = descriptor;
    this.signature = signature;
    this.version = version;
  }

  public final String getName() {
    return name;
  }

  public final String getDescriptor() {
    return descriptor;
  }

  @Override
  public String toString() {
    return name;
  }

  public GenericFieldDescriptor getSignature() {
    return signature;
  }

  @Override
  protected BytecodeVersion getVersion() {
    return this.version;
  }
}
