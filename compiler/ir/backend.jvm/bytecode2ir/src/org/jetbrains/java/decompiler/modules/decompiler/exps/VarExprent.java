// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.java.decompiler.modules.decompiler.exps;

import org.jetbrains.java.decompiler.code.CodeConstants;
import org.jetbrains.java.decompiler.code.Instruction;
import org.jetbrains.java.decompiler.main.ClassWriter;
import org.jetbrains.java.decompiler.main.ClassesProcessor.ClassNode;
import org.jetbrains.java.decompiler.main.DecompilerContext;
import org.jetbrains.java.decompiler.main.extern.IFernflowerPreferences;
import org.jetbrains.java.decompiler.main.rels.MethodWrapper;
import org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor;
import org.jetbrains.java.decompiler.modules.decompiler.stats.Statement;
import org.jetbrains.java.decompiler.modules.decompiler.vars.CheckTypesResult;
import org.jetbrains.java.decompiler.modules.decompiler.vars.VarProcessor;
import org.jetbrains.java.decompiler.modules.decompiler.vars.VarTypeProcessor.FinalType;
import org.jetbrains.java.decompiler.modules.decompiler.vars.VarVersionPair;
import org.jetbrains.java.decompiler.struct.StructClass;
import org.jetbrains.java.decompiler.struct.attr.StructGeneralAttribute;
import org.jetbrains.java.decompiler.struct.attr.StructLocalVariableTableAttribute;
import org.jetbrains.java.decompiler.struct.attr.StructLocalVariableTableAttribute.LocalVariable;
import org.jetbrains.java.decompiler.struct.attr.StructLocalVariableTypeTableAttribute;
import org.jetbrains.java.decompiler.struct.gen.VarType;
import org.jetbrains.java.decompiler.struct.gen.generics.GenericFieldDescriptor;
import org.jetbrains.java.decompiler.struct.gen.generics.GenericMain;
import org.jetbrains.java.decompiler.struct.gen.generics.GenericType;
import org.jetbrains.java.decompiler.struct.match.MatchEngine;
import org.jetbrains.java.decompiler.struct.match.MatchNode;
import org.jetbrains.java.decompiler.struct.match.MatchNode.RuleValue;
import org.jetbrains.java.decompiler.util.InterpreterUtil;
import org.jetbrains.java.decompiler.util.TextBuffer;

import java.util.BitSet;
import java.util.List;
import java.util.stream.Collectors;

public class VarExprent extends Exprent {
  public static final int STACK_BASE = 10000;
  public static final String VAR_NAMELESS_ENCLOSURE = "<VAR_NAMELESS_ENCLOSURE>";
  private static final boolean FORCE_VARVER_NAME = false; // Debug only!

  private int index;
  private VarType varType;
  private boolean definition = false;
  private final VarProcessor processor;
  private int version = 0;
  private boolean classDef = false;
  private boolean stack = false;
  private LocalVariable lvt = null;
  // Only relevant for first stage of decompilation, used with finally processing
  // Applies only to real vars, not stack vars
  private Instruction backing = null;
  private boolean isEffectivelyFinal = false;
  private VarType boundType;

  public VarExprent(int index, VarType varType, VarProcessor processor) {
    this(index, varType, processor, null);
  }

  public VarExprent(int index, VarType varType, VarProcessor processor, BitSet bytecode) {
    super(Type.VAR);
    this.index = index;
    this.varType = varType;
    this.processor = processor;
    this.addBytecodeOffsets(bytecode);
  }

  @Override
  public VarType getExprType() {
    return getVarType();
  }

  @Override
  public VarType getInferredExprType(VarType upperBound) {
    if (lvt != null && lvt.getSignature() != null) {
      // TODO; figure out why it's crashing, ugly fix for now
      try {
        return GenericType.parse(lvt.getSignature());
      } catch (StringIndexOutOfBoundsException ex) {
        ex.printStackTrace();
      }
    }
    else if (lvt != null) {
      return lvt.getVarType();
    }
    return getVarType();
  }

  @Override
  public int getExprentUse() {
    return Exprent.MULTIPLE_USES | Exprent.SIDE_EFFECTS_FREE;
  }

  @Override
  public List<Exprent> getAllExprents(List<Exprent> lst) {
    return lst;
  }

  @Override
  public Exprent copy() {
    VarExprent var = new VarExprent(index, getVarType(), processor, bytecode);
    var.setDefinition(definition);
    var.setVersion(version);
    var.setClassDef(classDef);
    var.setStack(stack);
    var.setLVT(lvt);
    var.setEffectivelyFinal(isEffectivelyFinal);
    return var;
  }

  @Override
  public TextBuffer toJava(int indent) {
    TextBuffer buffer = new TextBuffer();

    buffer.addBytecodeMapping(bytecode);

    if (classDef) {
      ClassNode child = DecompilerContext.getClassProcessor().getMapRootClasses().get(varType.value);
      new ClassWriter().classToJava(child, buffer, indent);
    } else {
      VarVersionPair varVersion = getVarVersionPair();

      if (definition) {
        if (processor != null && processor.getVarFinal(varVersion) == FinalType.EXPLICIT_FINAL) {
          buffer.append("final ");
        }
        buffer.append(getDefinitionType());
        buffer.append(" ");
      }

      buffer.append(getName());
    }

    return buffer;
  }

  public VarVersionPair getVarVersionPair() {
    return new VarVersionPair(index, version);
  }

  /*
  public String getDebugName(StructMethod method) {
    StructLocalVariableTableAttribute attr = method.getLocalVariableAttr();
    if (attr != null && processor != null) {
      Integer origIndex = processor.getVarOriginalIndex(index);
      if (origIndex != null) {
        String name = attr.getName(origIndex, bytecode == null ? -1 : bytecode.nextSetBit(0));
        if (name != null && TextUtil.isValidIdentifier(name, method.getBytecodeVersion())) {
          return name;
        }
      }
    }
    return null;
  }
  */

  public String getDefinitionType() {
    if (DecompilerContext.getOption(IFernflowerPreferences.USE_DEBUG_VAR_NAMES)) {

      if (lvt != null) {
        if (DecompilerContext.getOption(IFernflowerPreferences.DECOMPILE_GENERIC_SIGNATURES)) {
          if (lvt.getSignature() != null) {
            GenericFieldDescriptor descriptor = GenericMain.parseFieldSignature(lvt.getSignature());
            if (descriptor != null) {
              return ExprProcessor.getCastTypeName(descriptor.type);
            }
          }
        }
        return ExprProcessor.getCastTypeName(getVarType());
      }

      MethodWrapper method = (MethodWrapper)DecompilerContext.getProperty(DecompilerContext.CURRENT_METHOD_WRAPPER);
      if (method != null) {
        Integer originalIndex = null;
        if (processor != null) {
          originalIndex = processor.getVarOriginalIndex(index);
        }
        int visibleOffset = bytecode == null ? -1 : bytecode.length();
        if (originalIndex != null) {
          // first try from signature
          if (DecompilerContext.getOption(IFernflowerPreferences.DECOMPILE_GENERIC_SIGNATURES)) {
            StructLocalVariableTypeTableAttribute attr =
              method.methodStruct.getAttribute(StructGeneralAttribute.ATTRIBUTE_LOCAL_VARIABLE_TYPE_TABLE);
            if (attr != null) {
              String signature = attr.getSignature(originalIndex, visibleOffset);
              if (signature != null) {
                GenericFieldDescriptor descriptor = GenericMain.parseFieldSignature(signature);
                if (descriptor != null) {
                  return ExprProcessor.getCastTypeName(descriptor.type);
                }
              }
            }
          }

          // then try from descriptor
          StructLocalVariableTableAttribute attr = method.methodStruct.getLocalVariableAttr();
          if (attr != null) {
            String descriptor = attr.getDescriptor(originalIndex, visibleOffset);
            if (descriptor != null) {
              return ExprProcessor.getCastTypeName(new VarType(descriptor));
            }
          }
        }
      }
    }

    return ExprProcessor.getCastTypeName(getVarType());
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) return true;
    if (!(o instanceof VarExprent)) return false;

    VarExprent ve = (VarExprent)o;
    return index == ve.getIndex() &&
           version == ve.getVersion() &&
           InterpreterUtil.equalObjects(getVarType(), ve.getVarType()); // FIXME: varType comparison redundant?
  }

  public boolean equalsVersions(Object o) {
    if (o == this) return true;
    if (!(o instanceof VarExprent)) return false;

    VarExprent ve = (VarExprent)o;
    return index == ve.getIndex() && version == ve.getVersion();
  }

  @Override
  public void getBytecodeRange(BitSet values) {
    measureBytecode(values);
  }

  public int getIndex() {
    return index;
  }

  public void setIndex(int index) {
    this.index = index;
  }

  public VarType getVarType() {
    if (DecompilerContext.getOption(IFernflowerPreferences.USE_DEBUG_VAR_NAMES) && lvt != null) {
      return new VarType(lvt.getDescriptor());
    }

    VarType vt = null;
    if (processor != null) {
      String name = processor.getVarName(getVarVersionPair());
      vt = Exprent.inferredLambdaTypes.get().get(name);
      if (vt == null) {
        vt = processor.getVarType(getVarVersionPair());
        if (processor.getThisVars().containsKey(getVarVersionPair())) {
          String qaulName = processor.getThisVars().get(getVarVersionPair());
          StructClass cls = DecompilerContext.getStructContext().getClass(qaulName);
          if (cls != null && cls.getSignature() != null) {
            vt = cls.getSignature().genericType;
          }
          else if (vt == null) {
            vt = new VarType(CodeConstants.TYPE_OBJECT, 0, qaulName);
          }
        }
      }
    }

    if (vt == null || (varType != null && varType.type != CodeConstants.TYPE_UNKNOWN)) {
      vt = varType;
    }

    return vt == null ? VarType.VARTYPE_UNKNOWN : vt;
  }

  public void setVarType(VarType varType) {
    this.varType = varType;
  }

  public boolean isDefinition() {
    return definition;
  }

  public void setDefinition(boolean definition) {
    this.definition = definition;
  }

  public VarProcessor getProcessor() {
    return processor;
  }

  public int getVersion() {
    return version;
  }

  public void setVersion(int version) {
    this.version = version;
  }

  public boolean isClassDef() {
    return classDef;
  }

  public void setClassDef(boolean classDef) {
    this.classDef = classDef;
  }

  public boolean isStack() {
    return stack;
  }

  public void setStack(boolean stack) {
    this.stack = stack;
  }

  public Instruction getBackingInstr() {
    return backing;
  }

  public void setBackingInstr(Instruction backing) {
    this.backing = backing;
  }

  public void setLVT(LocalVariable var) {
    this.lvt = var;
    if (processor != null && lvt != null) {
      processor.setVarType(getVarVersionPair(), lvt.getVarType());
    }
  }

  public LocalVariable getLVT() {
    return lvt;
  }

  public void setEffectivelyFinal(boolean isEffectivelyFinal) {
    this.isEffectivelyFinal = isEffectivelyFinal;
  }

  public boolean isEffectivelyFinal() {
    return this.isEffectivelyFinal;
  }

  public String getName() {
    VarVersionPair pair = getVarVersionPair();
    if (!FORCE_VARVER_NAME) {

      if (this.processor != null) {
        String clashingName = this.processor.getClashingName(pair);

        // Clashing names take precedence over lvt names (as they are lvt names with an 'x' applied to differentiate them)
        if (clashingName != null) {
          return clashingName;
        }
      }

      if (this.lvt != null) {
        return this.lvt.getName();
      }

      if (this.processor != null) {
        String ret = this.processor.getVarName(pair);
        if (ret != null) {
          return ret;
        }
      }
    }

    return pair.version == 0 ? "var" + pair.var : "var" + pair.var + "_" + version;
  }

  public void setBoundType(VarType boundType) {
    this.boundType = boundType;
  }

  @Override
  public CheckTypesResult checkExprTypeBounds() {
    if (this.lvt != null) {
      CheckTypesResult ret = new CheckTypesResult();
      ret.addMinTypeExprent(this, this.lvt.getVarType());
      return ret;
    }

    if (this.boundType != null) {
      CheckTypesResult ret = new CheckTypesResult();
      ret.addMinTypeExprent(this, this.boundType);
      return ret;
    }

    return null;
  }

  public boolean isVarReferenced(Statement stat, VarExprent... whitelist) {
    if (stat.getExprents() == null) {
      for (Object obj : stat.getSequentialObjects()) {
        if (obj instanceof Statement) {
          if (isVarReferenced((Statement)obj, whitelist)) {
            return true;
          }
        }
        else if (obj instanceof Exprent) {
          if (isVarReferenced((Exprent)obj, whitelist)) {
            return true;
          }
        }
      }
    }
    else {
      for (Exprent exp : stat.getExprents()) {
        if (isVarReferenced(exp, whitelist)) {
          return true;
        }
      }
    }
    return false;
  }

  public boolean isVarReferenced(Exprent exp, VarExprent... whitelist) {
    List<Exprent> lst = exp.getAllExprents(true);
    lst.add(exp);
    lst = lst.stream().filter(e -> e != this && e instanceof VarExprent &&
      getVarVersionPair().equals(((VarExprent)e).getVarVersionPair()))
        .collect(Collectors.toList());

    for (Exprent var : lst) {
      boolean allowed = false;
      for (VarExprent white : whitelist) {
        if (var == white) {
          allowed = true;
          break;
        }
      }
      if (!allowed) {
        return true;
      }
    }
    return false;
  }

  @Override
  public boolean allowNewlineAfterQualifier() {
    return false;
  }

  @Override
  public String toString() {
    return "VarExprent[" + index + ',' + version + (definition ? " Def" : "") + "]: {" + super.toString() + "}";
  }

  // *****************************************************************************
  // IMatchable implementation
  // *****************************************************************************

  @Override
  public boolean match(MatchNode matchNode, MatchEngine engine) {
    if (!super.match(matchNode, engine)) {
      return false;
    }

    RuleValue rule = matchNode.getRules().get(MatchProperties.EXPRENT_VAR_INDEX);
    if (rule != null) {
      if (rule.isVariable()) {
        return engine.checkAndSetVariableValue((String)rule.value, this.index);
      }
      else {
        return this.index == Integer.valueOf((String)rule.value);
      }
    }

    return true;
  }
}