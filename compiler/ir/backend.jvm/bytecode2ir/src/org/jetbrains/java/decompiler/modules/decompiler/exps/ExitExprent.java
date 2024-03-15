// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.java.decompiler.modules.decompiler.exps;

import org.jetbrains.java.decompiler.code.CodeConstants;
import org.jetbrains.java.decompiler.main.ClassesProcessor.ClassNode;
import org.jetbrains.java.decompiler.main.DecompilerContext;
import org.jetbrains.java.decompiler.main.rels.MethodWrapper;
import org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor;
import org.jetbrains.java.decompiler.modules.decompiler.ValidationHelper;
import org.jetbrains.java.decompiler.modules.decompiler.vars.CheckTypesResult;
import org.jetbrains.java.decompiler.struct.attr.StructExceptionsAttribute;
import org.jetbrains.java.decompiler.struct.attr.StructGeneralAttribute;
import org.jetbrains.java.decompiler.struct.gen.MethodDescriptor;
import org.jetbrains.java.decompiler.struct.gen.VarType;
import org.jetbrains.java.decompiler.struct.match.MatchEngine;
import org.jetbrains.java.decompiler.struct.match.MatchNode;
import org.jetbrains.java.decompiler.util.InterpreterUtil;
import org.jetbrains.java.decompiler.util.TextBuffer;

import java.util.BitSet;
import java.util.List;

public class ExitExprent extends Exprent {
  public enum Type {
    RETURN, THROW
  }

  private final Type exitType;
  private Exprent value;
  private final VarType retType;
  private final MethodDescriptor methodDescriptor;

  public ExitExprent(Type exitType, Exprent value, VarType retType, BitSet bytecodeOffsets, MethodDescriptor methodDescriptor) {
    super(Exprent.Type.EXIT);
    this.exitType = exitType;
    this.value = value;
    this.retType = retType;
    this.methodDescriptor = methodDescriptor;

    addBytecodeOffsets(bytecodeOffsets);

    ValidationHelper.validateExitExprent(this);
  }

  @Override
  public Exprent copy() {
    return new ExitExprent(exitType, value == null ? null : value.copy(), retType, bytecode, methodDescriptor);
  }

  @Override
  public CheckTypesResult checkExprTypeBounds() {
    CheckTypesResult result = new CheckTypesResult();

    if (exitType == Type.RETURN && retType.type != CodeConstants.TYPE_VOID) {
      result.addMinTypeExprent(value, VarType.getMinTypeInFamily(retType.typeFamily));
      result.addMaxTypeExprent(value, retType);
    }

    return result;
  }

  @Override
  public List<Exprent> getAllExprents(List<Exprent> lst) {
    if (value != null) {
      lst.add(value);
    }
    return lst;
  }

  @Override
  public TextBuffer toJava(int indent) {
    TextBuffer buf = new TextBuffer();
    buf.addBytecodeMapping(bytecode);

    if (exitType == Type.RETURN) {
      buf.append("return");

      if (retType.type != CodeConstants.TYPE_VOID) {
        VarType ret = retType;
        if (methodDescriptor != null && methodDescriptor.genericInfo != null && methodDescriptor.genericInfo.returnType != null) {
          ret = methodDescriptor.genericInfo.returnType;
        }
        buf.append(' ');

        ExprProcessor.getCastedExprent(value, ret, buf, indent, ExprProcessor.NullCastType.DONT_CAST_AT_ALL, false, false, false);
      }

      return buf;
    }
    else {
      MethodWrapper method = (MethodWrapper)DecompilerContext.getProperty(DecompilerContext.CURRENT_METHOD_WRAPPER);
      ClassNode node = ((ClassNode)DecompilerContext.getProperty(DecompilerContext.CURRENT_CLASS_NODE));

      if (method != null && node != null) {
        StructExceptionsAttribute attr = method.methodStruct.getAttribute(StructGeneralAttribute.ATTRIBUTE_EXCEPTIONS);

        if (attr != null) {
          String classname = null;

          for (int i = 0; i < attr.getThrowsExceptions().size(); i++) {
            String exClassName = attr.getExcClassname(i, node.classStruct.getPool());
            if ("java/lang/Throwable".equals(exClassName)) {
              classname = exClassName;
              break;
            }
            else if ("java/lang/Exception".equals(exClassName)) {
              classname = exClassName;
            }
          }

          if (classname != null) {
            VarType exType = new VarType(classname, true);
            buf.append("throw ");
            ExprProcessor.getCastedExprent(value, exType, buf, indent, false);
            return buf;
          }
        }
      }

      return buf.append(value.toJava(indent)).prepend("throw ");
    }
  }

  @Override
  public void replaceExprent(Exprent oldExpr, Exprent newExpr) {
    if (oldExpr == value) {
      value = newExpr;
    }
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) return true;
    if (!(o instanceof ExitExprent)) return false;

    ExitExprent et = (ExitExprent)o;
    return exitType == et.getExitType() &&
           InterpreterUtil.equalObjects(value, et.getValue());
  }

  public Type getExitType() {
    return exitType;
  }

  public Exprent getValue() {
    return value;
  }

  public VarType getRetType() {
    return retType;
  }

  public MethodDescriptor getMethodDescriptor() {
    return this.methodDescriptor;
  }

  @Override
  public void getBytecodeRange(BitSet values) {
    measureBytecode(values, value);
    measureBytecode(values);
  }

  
  // *****************************************************************************
  // IMatchable implementation
  // *****************************************************************************

  @Override
  public boolean match(MatchNode matchNode, MatchEngine engine) {
    if (!super.match(matchNode, engine)) {
      return false;
    }

    Type type = (Type)matchNode.getRuleValue(MatchProperties.EXPRENT_EXITTYPE);
    return type == null || this.exitType == type;
  }
}