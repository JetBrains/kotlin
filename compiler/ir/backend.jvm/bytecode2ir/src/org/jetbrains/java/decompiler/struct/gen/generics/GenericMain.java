// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.java.decompiler.struct.gen.generics;

import org.jetbrains.java.decompiler.code.CodeConstants;
import org.jetbrains.java.decompiler.main.DecompilerContext;
import org.jetbrains.java.decompiler.main.extern.IFernflowerLogger;
import org.jetbrains.java.decompiler.util.TextUtil;
import org.jetbrains.java.decompiler.struct.gen.VarType;

import java.util.ArrayList;
import java.util.List;

public final class GenericMain {

  private static final String[] typeNames = {
    "byte",
    "char",
    "double",
    "float",
    "int",
    "long",
    "short",
    "boolean",
  };

  public static GenericClassDescriptor parseClassSignature(String qualifiedName, String signature) {
    String original = signature;
    try {
      GenericClassDescriptor descriptor = new GenericClassDescriptor();

      signature = parseFormalParameters(signature, descriptor.fparameters, descriptor.fbounds);

      String superCl = GenericType.getNextType(signature);
      descriptor.superclass = GenericType.parse(superCl);

      signature = signature.substring(superCl.length());
      while (signature.length() > 0) {
        String superIf = GenericType.getNextType(signature);
        descriptor.superinterfaces.add(GenericType.parse(superIf));
        signature = signature.substring(superIf.length());
      }

      StringBuilder buf = new StringBuilder();
      buf.append('L').append(qualifiedName).append('<');
      for (String t : descriptor.fparameters) {
        buf.append('T').append(t).append(';');
      }
      buf.append(">;");
      descriptor.genericType = (GenericType)GenericType.parse(buf.toString());

      return descriptor;
    }
    catch (RuntimeException e) {
      DecompilerContext.getLogger().writeMessage("Invalid signature: " + original, IFernflowerLogger.Severity.WARN);
      return null;
    }
  }

  public static GenericFieldDescriptor parseFieldSignature(String signature) {
    try {
      return new GenericFieldDescriptor(GenericType.parse(signature));
    }
    catch (RuntimeException e) {
      DecompilerContext.getLogger().writeMessage("Invalid signature: " + signature, IFernflowerLogger.Severity.WARN);
      return null;
    }
  }

  public static GenericMethodDescriptor parseMethodSignature(String signature) {
    String original = signature;
    try {
      List<String> typeParameters = new ArrayList<>();
      List<List<VarType>> typeParameterBounds = new ArrayList<>();
      signature = parseFormalParameters(signature, typeParameters, typeParameterBounds);

      int to = signature.indexOf(")");
      String parameters = signature.substring(1, to);
      signature = signature.substring(to + 1);

      List<VarType> parameterTypes = new ArrayList<>();
      while (parameters.length() > 0) {
        String par = GenericType.getNextType(parameters);
        parameterTypes.add(GenericType.parse(par));
        parameters = parameters.substring(par.length());
      }

      String ret = GenericType.getNextType(signature);
      VarType returnType = GenericType.parse(ret);
      signature = signature.substring(ret.length());

      List<VarType> exceptionTypes = new ArrayList<>();
      if (signature.length() > 0) {
        String[] exceptions = signature.split("\\^");
        for (int i = 1; i < exceptions.length; i++) {
          exceptionTypes.add(GenericType.parse(exceptions[i]));
        }
      }

      return new GenericMethodDescriptor(typeParameters, typeParameterBounds, parameterTypes, returnType, exceptionTypes);
    }
    catch (RuntimeException e) {
      DecompilerContext.getLogger().writeMessage("Invalid signature: " + original, IFernflowerLogger.Severity.WARN);
      return null;
    }
  }

  private static String parseFormalParameters(String signature, List<String> parameters, List<List<VarType>> bounds) {
    if (signature.charAt(0) != '<') {
      return signature;
    }

    int counter = 1;
    int index = 1;

    loop:
    while (index < signature.length()) {
      switch (signature.charAt(index)) {
        case '<':
          counter++;
          break;
        case '>':
          counter--;
          if (counter == 0) {
            break loop;
          }
      }

      index++;
    }

    String value = signature.substring(1, index);
    signature = signature.substring(index + 1);

    while (value.length() > 0) {
      int to = value.indexOf(":");

      String param = value.substring(0, to);
      value = value.substring(to + 1);

      List<VarType> lstBounds = new ArrayList<>();

      while (true) {
        if (value.charAt(0) == ':') {
          // empty superclass, skip
          value = value.substring(1);
        }

        String bound = GenericType.getNextType(value);
        lstBounds.add(GenericType.parse(bound));
        value = value.substring(bound.length());


        if (value.length() == 0 || value.charAt(0) != ':') {
          break;
        }
        else {
          value = value.substring(1);
        }
      }

      parameters.add(param);
      bounds.add(lstBounds);
    }

    return signature;
  }

  public static String getGenericCastTypeName(GenericType type) {
    StringBuilder s = new StringBuilder(getTypeName(type));
    TextUtil.append(s, "[]", type.arrayDim);
    return s.toString();
  }

  private static String getTypeName(GenericType type) {
    int tp = type.type;
    if (tp <= CodeConstants.TYPE_BOOLEAN) {
      return typeNames[tp];
    }
    else if (tp == CodeConstants.TYPE_VOID) {
      return "void";
    }
    else if (tp == CodeConstants.TYPE_GENVAR) {
      return type.value;
    }
    else if (tp == CodeConstants.TYPE_OBJECT) {
      return type.getCastName();
    }

    throw new RuntimeException("Invalid type: " + type);
  }
}
