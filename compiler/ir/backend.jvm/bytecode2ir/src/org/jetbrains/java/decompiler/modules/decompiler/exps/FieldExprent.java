/*
 * Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */
package org.jetbrains.java.decompiler.modules.decompiler.exps;

import org.jetbrains.java.decompiler.code.CodeConstants;
import org.jetbrains.java.decompiler.main.ClassesProcessor.ClassNode;
import org.jetbrains.java.decompiler.main.DecompilerContext;
import org.jetbrains.java.decompiler.main.rels.MethodWrapper;
import org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor;
import org.jetbrains.java.decompiler.modules.decompiler.vars.VarVersionPair;
import org.jetbrains.java.decompiler.struct.StructClass;
import org.jetbrains.java.decompiler.struct.StructField;
import org.jetbrains.java.decompiler.struct.attr.StructLocalVariableTableAttribute;
import org.jetbrains.java.decompiler.struct.consts.LinkConstant;
import org.jetbrains.java.decompiler.struct.gen.FieldDescriptor;
import org.jetbrains.java.decompiler.struct.gen.VarType;
import org.jetbrains.java.decompiler.struct.gen.generics.GenericType;
import org.jetbrains.java.decompiler.struct.match.MatchEngine;
import org.jetbrains.java.decompiler.struct.match.MatchNode;
import org.jetbrains.java.decompiler.struct.match.MatchNode.RuleValue;
import org.jetbrains.java.decompiler.util.InterpreterUtil;
import org.jetbrains.java.decompiler.util.TextBuffer;
import org.jetbrains.java.decompiler.util.TextUtil;

import java.util.*;

public class FieldExprent extends Exprent {
  private final String name;
  private final String classname;
  private final boolean isStatic;
  private Exprent instance;
  private final FieldDescriptor descriptor;
  private boolean forceQualified = false;
  private boolean isQualifier = false;
  private boolean wasCondy = false;

  public FieldExprent(LinkConstant cn, Exprent instance, BitSet bytecodeOffsets) {
    this(cn.elementname, cn.classname, instance == null, instance, FieldDescriptor.parseDescriptor(cn.descriptor), bytecodeOffsets);
  }

  public FieldExprent(String name, String classname, boolean isStatic, Exprent instance, FieldDescriptor descriptor, BitSet bytecodeOffsets) {
    this(name, classname, isStatic, instance, descriptor, bytecodeOffsets, false, false);
  }

  public FieldExprent(String name, String classname, boolean isStatic, Exprent instance, FieldDescriptor descriptor, BitSet bytecodeOffsets, boolean forceQualified, boolean wasCondy) {
    super(Type.FIELD);
    this.name = name;
    this.classname = classname;
    this.isStatic = isStatic;
    this.instance = instance;
    this.descriptor = descriptor;
    this.forceQualified = forceQualified;
    this.wasCondy = wasCondy;

    addBytecodeOffsets(bytecodeOffsets);
  }

  @Override
  public VarType getExprType() {
    return descriptor.type;
  }

  @Override
  public VarType getInferredExprType(VarType upperBound) {
    StructClass cl = DecompilerContext.getStructContext().getClass(classname);
    Map<String, Map<VarType, VarType>> types = cl == null ? Collections.emptyMap() : cl.getAllGenerics();

    StructField ft = null;
    while(cl != null) {
      ft = cl.getField(name, descriptor.descriptorString);
      if (ft != null)
        break;
      cl = cl.superClass == null ? null : DecompilerContext.getStructContext().getClass((String)cl.superClass.value);
    }

    if (ft != null && ft.getSignature() != null) {
      VarType ret =  ft.getSignature().type.remap(types.getOrDefault(cl.qualifiedName, Collections.emptyMap()));

      if (instance != null && cl.getSignature() != null) {
        VarType instType = instance.getInferredExprType(null);

        if (instType.isGeneric() && instType.type != CodeConstants.TYPE_GENVAR) {
          GenericType ginstance = (GenericType)instType;

          cl = DecompilerContext.getStructContext().getClass(instType.value);
          if (cl != null && cl.getSignature() != null) {
            Map<VarType, VarType> tempMap = new HashMap<>();
            cl.getSignature().genericType.mapGenVarsTo(ginstance, tempMap);
            VarType _new = ret.remap(tempMap);

            if (_new != null) {
              ret = _new;
            }
          }
        }
      }

      return ret;
    }

    return getExprType();
  }

  @Override
  public int getExprentUse() {
    //Revert the following line it produces messy code as follows:
    //-            this.field_225230_a[l + i1 * this.field_225231_b] &= 16777215;
    //+            int[] aint = this.field_225230_a;
    //+            int j1 = l + i1 * this.field_225231_b;
    //+            aint[j1] &= 16777215;
    //return 0; // multiple references to a field considered dangerous in a multithreaded environment, thus no Exprent.MULTIPLE_USES set here
    return instance == null ? Exprent.MULTIPLE_USES : instance.getExprentUse() & Exprent.MULTIPLE_USES;
  }

  @Override
  public List<Exprent> getAllExprents(List<Exprent> lst) {
    if (instance != null) {
      lst.add(instance);
    }
    return lst;
  }

  @Override
  public Exprent copy() {
    return new FieldExprent(name, classname, isStatic, instance == null ? null : instance.copy(), descriptor, bytecode, forceQualified, wasCondy);
  }

  private boolean isAmbiguous() {
    MethodWrapper method = (MethodWrapper)DecompilerContext.getProperty(DecompilerContext.CURRENT_METHOD_WRAPPER);
    if (method != null) {
      StructLocalVariableTableAttribute attr = method.methodStruct.getLocalVariableAttr();
      if (attr != null) {
        return attr.containsName(name);
      }
    }

    return false;
  }

  @Override
  public TextBuffer toJava(int indent) {
    TextBuffer buf = new TextBuffer();

    if (wasCondy) {
      buf.append("/* $VF: constant dynamic */ ");
    }

    if (isStatic) {
      if (useQualifiedStatic()) {
        buf.append(DecompilerContext.getImportCollector().getShortNameInClassContext(ExprProcessor.buildJavaClassName(classname)));
        buf.append(".");
      }
    }
    else {
      String super_qualifier = null;

      if (instance instanceof VarExprent) {
        VarExprent instVar = (VarExprent)instance;
        VarVersionPair pair = new VarVersionPair(instVar);

        MethodWrapper currentMethod = (MethodWrapper)DecompilerContext.getProperty(DecompilerContext.CURRENT_METHOD_WRAPPER);

        if (currentMethod != null) { // FIXME: remove
          String this_classname = currentMethod.varproc.getThisVars().get(pair);

          if (this_classname != null) {
            if (!classname.equals(this_classname)) { // TODO: direct comparison to the super class?
              super_qualifier = this_classname;
            }
          }
        }
      }

      if (super_qualifier != null) {
        TextUtil.writeQualifiedSuper(buf, super_qualifier);
      }
      else {
        if (!isQualifier) {
          buf.pushNewlineGroup(indent, 1);
        }
        if (instance != null) {
          instance.setIsQualifier();
        }
        TextBuffer buff = new TextBuffer();
        boolean casted = ExprProcessor.getCastedExprent(instance, new VarType(CodeConstants.TYPE_OBJECT, 0, classname), buff, indent, true);

        if (casted || instance.getPrecedence() > getPrecedence()) {
          buff.encloseWithParens();
        }

        buf.append(buff);
        if (instance != null && instance.allowNewlineAfterQualifier()) {
          buf.appendPossibleNewline();
        }
        if (!isQualifier) {
          buf.popNewlineGroup();
        }
      }

      if (buf.contentEquals(
        VarExprent.VAR_NAMELESS_ENCLOSURE)) { // FIXME: workaround for field access of an anonymous enclosing class. Find a better way.
        buf.setLength(0);
      }
      else {
        buf.append(".");
      }
    }

    buf.addBytecodeMapping(bytecode);

    buf.append(name);

    return buf;
  }

  private boolean useQualifiedStatic() {
    ClassNode node = (ClassNode)DecompilerContext.getProperty(DecompilerContext.CURRENT_CLASS_NODE);
    return node == null || !classname.equals(node.classStruct.qualifiedName) || isAmbiguous() || forceQualified;
  }

  @Override
  public void replaceExprent(Exprent oldExpr, Exprent newExpr) {
    if (oldExpr == instance) {
      instance = newExpr;
    }
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) return true;
    if (!(o instanceof FieldExprent)) return false;

    FieldExprent ft = (FieldExprent)o;
    return InterpreterUtil.equalObjects(name, ft.getName()) &&
           InterpreterUtil.equalObjects(classname, ft.getClassname()) &&
           isStatic == ft.isStatic() &&
           InterpreterUtil.equalObjects(instance, ft.getInstance()) &&
           InterpreterUtil.equalObjects(descriptor, ft.getDescriptor());
  }

  public String getClassname() {
    return classname;
  }

  public FieldDescriptor getDescriptor() {
    return descriptor;
  }

  public Exprent getInstance() {
    return instance;
  }

  public boolean isStatic() {
    return isStatic;
  }

  public String getName() {
    return name;
  }

  public void forceQualified(boolean value) {
    this.forceQualified = value;
  }

  @Override
  public void getBytecodeRange(BitSet values) {
    measureBytecode(values, instance);
    measureBytecode(values);
  }

  @Override
  public void setIsQualifier() {
    isQualifier = true;
  }

  @Override
  public boolean allowNewlineAfterQualifier() {
    if (isStatic && !useQualifiedStatic()) {
      return false;
    }
    return super.allowNewlineAfterQualifier();
  }

  // *****************************************************************************
  // IMatchable implementation
  // *****************************************************************************

  @Override
  public boolean match(MatchNode matchNode, MatchEngine engine) {
    if (!super.match(matchNode, engine)) {
      return false;
    }

    RuleValue rule = matchNode.getRules().get(MatchProperties.EXPRENT_FIELD_NAME);
    if (rule != null) {
      if (rule.isVariable()) {
        return engine.checkAndSetVariableValue((String)rule.value, this.name);
      }
      else {
        return rule.value.equals(this.name);
      }
    }

    return true;
  }
}
