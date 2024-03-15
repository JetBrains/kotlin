// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.java.decompiler.struct;

import org.jetbrains.java.decompiler.code.*;
import org.jetbrains.java.decompiler.main.DecompilerContext;
import org.jetbrains.java.decompiler.main.extern.IFernflowerPreferences;
import org.jetbrains.java.decompiler.main.extern.IVariableNameProvider;
import org.jetbrains.java.decompiler.struct.attr.StructCodeAttribute;
import org.jetbrains.java.decompiler.struct.attr.StructGeneralAttribute;
import org.jetbrains.java.decompiler.struct.attr.StructGenericSignatureAttribute;
import org.jetbrains.java.decompiler.struct.attr.StructLocalVariableTableAttribute;
import org.jetbrains.java.decompiler.struct.consts.ConstantPool;
import org.jetbrains.java.decompiler.struct.gen.generics.GenericMain;
import org.jetbrains.java.decompiler.struct.gen.generics.GenericMethodDescriptor;
import org.jetbrains.java.decompiler.util.DataInputFullStream;
import org.jetbrains.java.decompiler.util.VBStyleCollection;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.jetbrains.java.decompiler.code.CodeConstants.*;

/*
  method_info {
    u2 access_flags;
    u2 name_index;
    u2 descriptor_index;
    u2 attributes_count;
    attribute_info attributes[attributes_count];
  }
*/
public class StructMethod extends StructMember {
  public static StructMethod create(DataInputFullStream in, ConstantPool pool, String clQualifiedName, BytecodeVersion bytecodeVersion, boolean own) throws IOException {
    int accessFlags = in.readUnsignedShort();
    int nameIndex = in.readUnsignedShort();
    int descriptorIndex = in.readUnsignedShort();

    String[] values = pool.getClassElement(ConstantPool.METHOD, clQualifiedName, nameIndex, descriptorIndex);

    Map<String, StructGeneralAttribute> attributes = readAttributes(in, pool, own, bytecodeVersion);
    StructCodeAttribute code = (StructCodeAttribute)attributes.remove(StructGeneralAttribute.ATTRIBUTE_CODE.name);
    if (code != null) {
      attributes.putAll(code.codeAttributes);
    }

    GenericMethodDescriptor signature = null;
    if (DecompilerContext.getOption(IFernflowerPreferences.DECOMPILE_GENERIC_SIGNATURES)) {
      StructGenericSignatureAttribute signatureAttr = (StructGenericSignatureAttribute)attributes.get(StructGeneralAttribute.ATTRIBUTE_SIGNATURE.name);
      if (signatureAttr != null) {
        signature = GenericMain.parseMethodSignature(signatureAttr.getSignature());
      }
    }

    return new StructMethod(accessFlags, attributes, values[0], values[1], bytecodeVersion, own ? code : null, clQualifiedName, signature);
  }

  private static final int[] opr_iconst = {-1, 0, 1, 2, 3, 4, 5};
  private static final int[] opr_loadstore = {0, 1, 2, 3, 0, 1, 2, 3, 0, 1, 2, 3, 0, 1, 2, 3, 0, 1, 2, 3};
  private static final int[] opcs_load = {opc_iload, opc_lload, opc_fload, opc_dload, opc_aload};
  private static final int[] opcs_store = {opc_istore, opc_lstore, opc_fstore, opc_dstore, opc_astore};

  private final String name;
  private final String descriptor;
  private final BytecodeVersion bytecodeVersion;
  private final int localVariables;
  private final byte[] codeAndExceptions;
  private InstructionSequence seq = null;
  private boolean expanded = false;
  private final String classQualifiedName;
  private final GenericMethodDescriptor signature;
  private IVariableNameProvider renamer;

  private StructMethod(int accessFlags,
                       Map<String, StructGeneralAttribute> attributes,
                       String name,
                       String descriptor,
                       BytecodeVersion bytecodeVersion,
                       StructCodeAttribute code,
                       String classQualifiedName,
                       GenericMethodDescriptor signature) {
    super(accessFlags, attributes);
    this.name = name;
    this.descriptor = descriptor;
    this.bytecodeVersion = bytecodeVersion;
    if (code != null) {
      this.localVariables = code.localVariables;
      this.codeAndExceptions = code.codeAndExceptionData;
    } else {
      this.localVariables = -1;
      this.codeAndExceptions = null;
    }
    this.classQualifiedName = classQualifiedName;
    this.signature = signature;
  }

  public void expandData(StructClass classStruct) throws IOException {
    if (codeAndExceptions != null && !expanded) {
      seq = parseBytecode(new DataInputFullStream(codeAndExceptions), classStruct.getPool());
      expanded = true;
    }
  }

  public void releaseResources() {
    if (codeAndExceptions != null && expanded) {
      seq = null;
      expanded = false;
    }
  }

  @SuppressWarnings("AssignmentToForLoopParameter")
  private InstructionSequence parseBytecode(DataInputFullStream in, ConstantPool pool) throws IOException {
    VBStyleCollection<Instruction, Integer> instructions = new VBStyleCollection<>();

    int length = in.readInt();
    for (int i = 0; i < length; ) {
      int offset = i;

      int opcode = in.readUnsignedByte();
      int group = GROUP_GENERAL;

      boolean wide = (opcode == opc_wide);

      if (wide) {
        i++;
        opcode = in.readUnsignedByte();
      }

      List<Integer> operands = new ArrayList<>();

      if (opcode >= opc_iconst_m1 && opcode <= opc_iconst_5) {
        operands.add(opr_iconst[opcode - opc_iconst_m1]);
        opcode = opc_bipush;
      }
      else if (opcode >= opc_iload_0 && opcode <= opc_aload_3) {
        operands.add(opr_loadstore[opcode - opc_iload_0]);
        opcode = opcs_load[(opcode - opc_iload_0) / 4];
      }
      else if (opcode >= opc_istore_0 && opcode <= opc_astore_3) {
        operands.add(opr_loadstore[opcode - opc_istore_0]);
        opcode = opcs_store[(opcode - opc_istore_0) / 4];
      }
      else {
        switch (opcode) {
          case opc_bipush:
            operands.add((int)in.readByte());
            i++;
            break;
          case opc_ldc:
          case opc_newarray:
            operands.add(in.readUnsignedByte());
            i++;
            break;
          case opc_sipush:
          case opc_ifeq:
          case opc_ifne:
          case opc_iflt:
          case opc_ifge:
          case opc_ifgt:
          case opc_ifle:
          case opc_if_icmpeq:
          case opc_if_icmpne:
          case opc_if_icmplt:
          case opc_if_icmpge:
          case opc_if_icmpgt:
          case opc_if_icmple:
          case opc_if_acmpeq:
          case opc_if_acmpne:
          case opc_goto:
          case opc_jsr:
          case opc_ifnull:
          case opc_ifnonnull:
            if (opcode != opc_sipush) {
              group = GROUP_JUMP;
            }
            operands.add((int)in.readShort());
            i += 2;
            break;
          case opc_ldc_w:
          case opc_ldc2_w:
          case opc_getstatic:
          case opc_putstatic:
          case opc_getfield:
          case opc_putfield:
          case opc_invokevirtual:
          case opc_invokespecial:
          case opc_invokestatic:
          case opc_new:
          case opc_anewarray:
          case opc_checkcast:
          case opc_instanceof:
            operands.add(in.readUnsignedShort());
            i += 2;
            if (opcode >= opc_getstatic && opcode <= opc_putfield) {
              group = GROUP_FIELDACCESS;
            }
            else if (opcode >= opc_invokevirtual && opcode <= opc_invokestatic) {
              group = GROUP_INVOCATION;
            }
            break;
          case opc_invokedynamic:
            if (bytecodeVersion.hasInvokeDynamic()) { // instruction unused in Java 6 and before
              operands.add(in.readUnsignedShort());
              in.discard(2);
              group = GROUP_INVOCATION;
              i += 4;
            }
            break;
          case opc_iload:
          case opc_lload:
          case opc_fload:
          case opc_dload:
          case opc_aload:
          case opc_istore:
          case opc_lstore:
          case opc_fstore:
          case opc_dstore:
          case opc_astore:
          case opc_ret:
            if (wide) {
              operands.add(in.readUnsignedShort());
              i += 2;
            }
            else {
              operands.add(in.readUnsignedByte());
              i++;
            }
            if (opcode == opc_ret) {
              group = GROUP_RETURN;
            }
            break;
          case opc_iinc:
            if (wide) {
              operands.add(in.readUnsignedShort());
              operands.add((int)in.readShort());
              i += 4;
            }
            else {
              operands.add(in.readUnsignedByte());
              operands.add((int)in.readByte());
              i += 2;
            }
            break;
          case opc_goto_w:
          case opc_jsr_w:
            opcode = opcode == opc_jsr_w ? opc_jsr : opc_goto;
            operands.add(in.readInt());
            group = GROUP_JUMP;
            i += 4;
            break;
          case opc_invokeinterface:
            operands.add(in.readUnsignedShort());
            operands.add(in.readUnsignedByte());
            in.discard(1);
            group = GROUP_INVOCATION;
            i += 4;
            break;
          case opc_multianewarray:
            operands.add(in.readUnsignedShort());
            operands.add(in.readUnsignedByte());
            i += 3;
            break;
          case opc_tableswitch:
            in.discard((4 - (i + 1) % 4) % 4);
            i += ((4 - (i + 1) % 4) % 4); // padding
            operands.add(in.readInt());
            i += 4;
            int low = in.readInt();
            operands.add(low);
            i += 4;
            int high = in.readInt();
            operands.add(high);
            i += 4;

            for (int j = 0; j < high - low + 1; j++) {
              operands.add(in.readInt());
              i += 4;
            }
            group = GROUP_SWITCH;

            break;
          case opc_lookupswitch:
            in.discard((4 - (i + 1) % 4) % 4);
            i += ((4 - (i + 1) % 4) % 4); // padding
            operands.add(in.readInt());
            i += 4;
            int npairs = in.readInt();
            operands.add(npairs);
            i += 4;

            for (int j = 0; j < npairs; j++) {
              operands.add(in.readInt());
              i += 4;
              operands.add(in.readInt());
              i += 4;
            }
            group = GROUP_SWITCH;
            break;
          case opc_ireturn:
          case opc_lreturn:
          case opc_freturn:
          case opc_dreturn:
          case opc_areturn:
          case opc_return:
          case opc_athrow:
            group = GROUP_RETURN;
        }
      }

      int[] ops = null;
      if (!operands.isEmpty()) {
        ops = new int[operands.size()];
        for (int j = 0; j < operands.size(); j++) {
          ops[j] = operands.get(j);
        }
      }

      i++;

      Instruction instr = Instruction.create(opcode, wide, group, bytecodeVersion, ops, i - offset);

      instructions.addWithKey(instr, offset);
    }

    // initialize exception table
    List<ExceptionHandler> lstHandlers = new ArrayList<>();

    int exception_count = in.readUnsignedShort();
    for (int i = 0; i < exception_count; i++) {
      ExceptionHandler handler = new ExceptionHandler();
      handler.from = in.readUnsignedShort();
      handler.to = in.readUnsignedShort();
      handler.handler = in.readUnsignedShort();

      int excclass = in.readUnsignedShort();
      if (excclass != 0) {
        handler.exceptionClass = pool.getPrimitiveConstant(excclass).getString();
      }

      lstHandlers.add(handler);
    }

    InstructionSequence seq = new FullInstructionSequence(instructions, new ExceptionTable(lstHandlers));

    // initialize instructions
    int i = seq.length() - 1;
    seq.setPointer(i);

    while (i >= 0) {
      Instruction instr = seq.getInstr(i--);
      if (instr.group != GROUP_GENERAL) {
        instr.initInstruction(seq);
      }
      seq.addToPointer(-1);
    }

    return seq;
  }

  public String getName() {
    return name;
  }

  public String getDescriptor() {
    return descriptor;
  }

  public BytecodeVersion getBytecodeVersion() {
    return bytecodeVersion;
  }

  public boolean containsCode() {
    return codeAndExceptions != null;
  }

  public int getLocalVariables() {
    return localVariables;
  }

  public InstructionSequence getInstructionSequence() {
    return seq;
  }

  @Override
  protected BytecodeVersion getVersion() {
    return this.bytecodeVersion;
  }

  public IVariableNameProvider getVariableNamer() {
    if (renamer == null) {
      this.renamer = DecompilerContext.getNamingFactory().createFactory(this);
    }
    return renamer;
  }

  public void clearVariableNamer() {
    this.renamer = null;
  }

  public StructLocalVariableTableAttribute getLocalVariableAttr() {
    return getAttribute(StructGeneralAttribute.ATTRIBUTE_LOCAL_VARIABLE_TABLE);
  }

  @Override
  public String toString() {
    return name;
  }

  public String getClassQualifiedName() {
    return classQualifiedName;
  }

  public GenericMethodDescriptor getSignature() {
    return signature;
  }
}
