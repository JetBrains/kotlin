// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.java.decompiler.modules.decompiler.exps;

import org.jetbrains.java.decompiler.code.CodeConstants;
import org.jetbrains.java.decompiler.main.ClassesProcessor.ClassNode;
import org.jetbrains.java.decompiler.main.DecompilerContext;
import org.jetbrains.java.decompiler.main.extern.IFernflowerPreferences;
import org.jetbrains.java.decompiler.main.rels.ClassWrapper;
import org.jetbrains.java.decompiler.main.rels.MethodWrapper;
import org.jetbrains.java.decompiler.modules.decompiler.vars.VarVersionPair;
import org.jetbrains.java.decompiler.struct.StructClass;
import org.jetbrains.java.decompiler.struct.attr.StructGeneralAttribute;
import org.jetbrains.java.decompiler.struct.attr.StructInnerClassesAttribute;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ExprUtil {
  public static List<VarVersionPair> getSyntheticParametersMask(String className, String descriptor, int parameters) {
    ClassNode node = DecompilerContext.getClassProcessor().getMapRootClasses().get(className);
    return node != null ? getSyntheticParametersMask(node, descriptor, parameters) : null;
  }

  public static List<VarVersionPair> getSyntheticParametersMask(ClassNode node, String descriptor, int parameters) {
    List<VarVersionPair> mask = null;

    ClassWrapper wrapper = node.getWrapper();
    if (wrapper != null) {
      // own class
      MethodWrapper methodWrapper = wrapper.getMethodWrapper(CodeConstants.INIT_NAME, descriptor);
      if (methodWrapper == null) {
        if (DecompilerContext.getOption(IFernflowerPreferences.IGNORE_INVALID_BYTECODE)) {
          return null;
        }
        throw new RuntimeException("Constructor " + node.classStruct.qualifiedName + "." + CodeConstants.INIT_NAME + descriptor + " not found");
      }
      mask = methodWrapper.synthParameters;
    }
    else if (parameters > 0 && node.type == ClassNode.Type.MEMBER && !isStatic(node.classStruct)) {
      // non-static member class
      mask = new ArrayList<>(Collections.nCopies(parameters, null));
      mask.set(0, new VarVersionPair(-1, 0));
    }

    return mask;
  }

  private static boolean isStatic(StructClass struct) {
    if (struct.hasModifier(CodeConstants.ACC_STATIC))
      return true;
    if (struct.hasAttribute(StructGeneralAttribute.ATTRIBUTE_INNER_CLASSES)) {
      StructInnerClassesAttribute attr = (StructInnerClassesAttribute)struct.getAttribute(StructGeneralAttribute.ATTRIBUTE_INNER_CLASSES);
      for (StructInnerClassesAttribute.Entry entry : attr.getEntries()) {
        if (entry.innerName != null && entry.innerName.equals(struct.qualifiedName)) {
          return (entry.accessFlags & CodeConstants.ACC_STATIC) == CodeConstants.ACC_STATIC;
        }
      }
    }
    return false;
  }
}
