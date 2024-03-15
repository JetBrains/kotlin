// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.java.decompiler.modules.decompiler;

import org.jetbrains.java.decompiler.code.CodeConstants;
import org.jetbrains.java.decompiler.code.Instruction;
import org.jetbrains.java.decompiler.code.InstructionSequence;
import org.jetbrains.java.decompiler.code.cfg.BasicBlock;
import org.jetbrains.java.decompiler.main.DecompilerContext;
import org.jetbrains.java.decompiler.util.ListStack;
import org.jetbrains.java.decompiler.util.TextBuffer;
import org.jetbrains.java.decompiler.modules.decompiler.exps.*;
import org.jetbrains.java.decompiler.modules.decompiler.exps.FunctionExprent.FunctionType;
import org.jetbrains.java.decompiler.modules.decompiler.flow.DirectGraph;
import org.jetbrains.java.decompiler.modules.decompiler.flow.DirectNode;
import org.jetbrains.java.decompiler.modules.decompiler.flow.FlattenStatementsHelper;
import org.jetbrains.java.decompiler.modules.decompiler.flow.FlattenStatementsHelper.FinallyPathWrapper;
import org.jetbrains.java.decompiler.modules.decompiler.stats.*;
import org.jetbrains.java.decompiler.modules.decompiler.vars.VarProcessor;
import org.jetbrains.java.decompiler.struct.StructClass;
import org.jetbrains.java.decompiler.struct.attr.StructBootstrapMethodsAttribute;
import org.jetbrains.java.decompiler.struct.attr.StructGeneralAttribute;
import org.jetbrains.java.decompiler.struct.consts.ConstantPool;
import org.jetbrains.java.decompiler.struct.consts.LinkConstant;
import org.jetbrains.java.decompiler.struct.consts.PooledConstant;
import org.jetbrains.java.decompiler.struct.consts.PrimitiveConstant;
import org.jetbrains.java.decompiler.struct.gen.MethodDescriptor;
import org.jetbrains.java.decompiler.struct.gen.VarType;
import org.jetbrains.java.decompiler.struct.gen.generics.GenericType;
import org.jetbrains.java.decompiler.util.TextUtil;

import java.util.*;

public class ExprProcessor implements CodeConstants {
  public static final String UNDEFINED_TYPE_STRING = "<undefinedtype>";
  public static final String UNKNOWN_TYPE_STRING = "<unknown>";
  public static final String NULL_TYPE_STRING = "<null>";

  private static final Map<Integer, FunctionType> mapConsts = new HashMap<>();
  static {
    mapConsts.put(opc_arraylength, FunctionType.ARRAY_LENGTH);
    mapConsts.put(opc_checkcast, FunctionType.CAST);
    mapConsts.put(opc_instanceof, FunctionType.INSTANCEOF);
  }

  private static final VarType[] consts = {
    VarType.VARTYPE_INT, VarType.VARTYPE_FLOAT, VarType.VARTYPE_LONG, VarType.VARTYPE_DOUBLE, VarType.VARTYPE_CLASS, VarType.VARTYPE_STRING
  };

  private static final VarType[] varTypes = {
    VarType.VARTYPE_INT, VarType.VARTYPE_LONG, VarType.VARTYPE_FLOAT, VarType.VARTYPE_DOUBLE, VarType.VARTYPE_OBJECT
  };

  private static final VarType[] arrTypes = {
    VarType.VARTYPE_INT, VarType.VARTYPE_LONG, VarType.VARTYPE_FLOAT, VarType.VARTYPE_DOUBLE, VarType.VARTYPE_OBJECT,
    VarType.VARTYPE_BOOLEAN, VarType.VARTYPE_CHAR, VarType.VARTYPE_SHORT
  };

  private static final FunctionType[] func1 = {
    FunctionType.ADD, FunctionType.SUB, FunctionType.MUL, FunctionType.DIV,
    FunctionType.REM
  };
  private static final FunctionType[] func2 = {
    FunctionType.SHL, FunctionType.SHR, FunctionType.USHR, FunctionType.AND,
    FunctionType.OR, FunctionType.XOR
  };
  private static final FunctionType[] func3 = {
    FunctionType.I2L, FunctionType.I2F, FunctionType.I2D, FunctionType.L2I,
    FunctionType.L2F, FunctionType.L2D, FunctionType.F2I, FunctionType.F2L,
    FunctionType.F2D, FunctionType.D2I, FunctionType.D2L, FunctionType.D2F,
    FunctionType.I2B, FunctionType.I2C, FunctionType.I2S
  };
  private static final FunctionType[] func4 = {
    FunctionType.LCMP, FunctionType.FCMPL, FunctionType.FCMPG, FunctionType.DCMPL,
    FunctionType.DCMPG
  };
  private static final IfExprent.Type[] func5 = {
    IfExprent.Type.EQ, IfExprent.Type.NE, IfExprent.Type.LT, IfExprent.Type.GE, IfExprent.Type.GT, IfExprent.Type.LE
  };
  private static final IfExprent.Type[] func6 = {
    IfExprent.Type.ICMPEQ, IfExprent.Type.ICMPNE, IfExprent.Type.ICMPLT, IfExprent.Type.ICMPGE, IfExprent.Type.ICMPGT, IfExprent.Type.ICMPLE,
    IfExprent.Type.ACMPEQ, IfExprent.Type.ACMPNE
  };
  private static final IfExprent.Type[] func7 = {IfExprent.Type.NULL, IfExprent.Type.NONNULL};
  private static final MonitorExprent.Type[] func8 = {MonitorExprent.Type.ENTER, MonitorExprent.Type.EXIT};

  private static final int[] arrTypeIds = {
    CodeConstants.TYPE_BOOLEAN, CodeConstants.TYPE_CHAR, CodeConstants.TYPE_FLOAT, CodeConstants.TYPE_DOUBLE,
    CodeConstants.TYPE_BYTE, CodeConstants.TYPE_SHORT, CodeConstants.TYPE_INT, CodeConstants.TYPE_LONG
  };

  private static final String[] typeNames = {"byte", "char", "double", "float", "int", "long", "short", "boolean"};

  private final MethodDescriptor methodDescriptor;
  private final VarProcessor varProcessor;

  public ExprProcessor(MethodDescriptor md, VarProcessor varProc) {
    methodDescriptor = md;
    varProcessor = varProc;
  }

  public void processStatement(RootStatement root, StructClass cl) {
    FlattenStatementsHelper flatthelper = new FlattenStatementsHelper();
    DirectGraph dgraph = flatthelper.buildDirectGraph(root);

    ValidationHelper.validateDGraph(dgraph, root);

    // collect finally entry points
    Set<String> setFinallyShortRangeEntryPoints = new HashSet<>();
    for (List<FinallyPathWrapper> lst : dgraph.mapShortRangeFinallyPaths.values()) {
      for (FinallyPathWrapper finwrap : lst) {
        setFinallyShortRangeEntryPoints.add(finwrap.entry);
      }
    }

    Set<String> setFinallyLongRangeEntryPaths = new HashSet<>();
    for (List<FinallyPathWrapper> lst : dgraph.mapLongRangeFinallyPaths.values()) {
      for (FinallyPathWrapper finwrap : lst) {
        setFinallyLongRangeEntryPaths.add(finwrap.source + "##" + finwrap.entry);
      }
    }

    Map<String, VarExprent> mapCatch = new HashMap<>();
    collectCatchVars(root, flatthelper, mapCatch);

    Map<DirectNode, Map<String, PrimitiveExprsList>> mapData = new HashMap<>();

    LinkedList<DirectNode> stack = new LinkedList<>();
    LinkedList<LinkedList<String>> stackEntryPoint = new LinkedList<>();

    stack.add(dgraph.first);
    stackEntryPoint.add(new LinkedList<>());

    Map<String, PrimitiveExprsList> map = new HashMap<>();
    map.put(null, new PrimitiveExprsList());
    mapData.put(dgraph.first, map);

    Set<DirectNode> seen = new HashSet<>();

    while (!stack.isEmpty()) {

      DirectNode node = stack.removeFirst();
      LinkedList<String> entrypoints = stackEntryPoint.removeFirst();

      PrimitiveExprsList data;
      if (mapCatch.containsKey(node.id)) {
        data = getExpressionData(mapCatch.get(node.id));
      }
      else {
        data = mapData.get(node).get(buildEntryPointKey(entrypoints));
      }

      BasicBlockStatement block = node.block;
      if (block != null) {
        processBlock(block, data, cl);
        block.setExprents(data.getLstExprents());
      }

      String currentEntrypoint = entrypoints.isEmpty() ? null : entrypoints.getLast();

      for (DirectNode nd : node.succs()) {

        boolean isSuccessor = true;
        if (currentEntrypoint != null && dgraph.mapLongRangeFinallyPaths.containsKey(node.id)) {
          isSuccessor = false;
          for (FinallyPathWrapper finwraplong : dgraph.mapLongRangeFinallyPaths.get(node.id)) {
            if (finwraplong.source.equals(currentEntrypoint) && finwraplong.destination.equals(nd.id)) {
              isSuccessor = true;
              break;
            }
          }
        }

        if (!seen.contains(nd) && isSuccessor) {
          Map<String, PrimitiveExprsList> mapSucc = mapData.computeIfAbsent(nd, k -> new HashMap<>());
          LinkedList<String> ndentrypoints = new LinkedList<>(entrypoints);

          if (setFinallyLongRangeEntryPaths.contains(node.id + "##" + nd.id)) {
            ndentrypoints.addLast(node.id);
          }
          else if (!setFinallyShortRangeEntryPoints.contains(nd.id) && dgraph.mapLongRangeFinallyPaths.containsKey(node.id)) {
            ndentrypoints.removeLast(); // currentEntrypoint should
            // not be null at this point
          }

          // handling of entry point loops
          int succ_entry_index = ndentrypoints.indexOf(nd.id);
          if (succ_entry_index >=
              0) { // we are in a loop (e.g. continue in a finally block), drop all entry points in the list beginning with succ_entry_index
            for (int elements_to_remove = ndentrypoints.size() - succ_entry_index; elements_to_remove > 0; elements_to_remove--) {
              ndentrypoints.removeLast();
            }
          }

          seen.add(nd);
          String ndentrykey = buildEntryPointKey(ndentrypoints);
          if (!mapSucc.containsKey(ndentrykey)) {

            mapSucc.put(ndentrykey, copyVarExprents(data.copyStack()));

            stack.add(nd);
            stackEntryPoint.add(ndentrypoints);
          }
        }
      }
    }

    initStatementExprents(root);
  }

  // FIXME: Ugly code, to be rewritten. A tuple class is needed.
  private static String buildEntryPointKey(LinkedList<String> entrypoints) {
    if (entrypoints.isEmpty()) {
      return null;
    }
    else {
      StringBuilder buffer = new StringBuilder();
      for (String point : entrypoints) {
        buffer.append(point);
        buffer.append(":");
      }
      return buffer.toString();
    }
  }

  private static PrimitiveExprsList copyVarExprents(PrimitiveExprsList data) {
    ListStack<Exprent> stack = data.getStack();
    copyEntries(stack);
    return data;
  }

  public static void copyEntries(List<Exprent> stack) {
    for (int i = 0; i < stack.size(); i++) {
      stack.set(i, stack.get(i).copy());
    }
  }

  private static void collectCatchVars(Statement stat, FlattenStatementsHelper flatthelper, Map<String, VarExprent> map) {

    List<VarExprent> lst = null;

    if (stat instanceof CatchAllStatement) {
      CatchAllStatement catchall = (CatchAllStatement)stat;
      if (!catchall.isFinally()) {
        lst = catchall.getVars();
      }
    }
    else if (stat instanceof CatchStatement) {
      lst = ((CatchStatement)stat).getVars();
    }

    if (lst != null) {
      for (int i = 1; i < stat.getStats().size(); i++) {
        map.put(flatthelper.getMapDestinationNodes().get(stat.getStats().get(i).id)[0], lst.get(i - 1));
      }
    }

    for (Statement st : stat.getStats()) {
      collectCatchVars(st, flatthelper, map);
    }
  }

  private static void initStatementExprents(Statement stat) {
    stat.initExprents();

    for (Statement st : stat.getStats()) {
      initStatementExprents(st);
    }
  }

  public void processBlock(BasicBlockStatement stat, PrimitiveExprsList data, StructClass cl) {

    ConstantPool pool = cl.getPool();
    StructBootstrapMethodsAttribute bootstrap = cl.getAttribute(StructGeneralAttribute.ATTRIBUTE_BOOTSTRAP_METHODS);

    BasicBlock block = stat.getBlock();

    ListStack<Exprent> stack = data.getStack();
    List<Exprent> exprlist = data.getLstExprents();

    InstructionSequence seq = block.getSeq();

    for (int i = 0; i < seq.length(); i++) {

      Instruction instr = seq.getInstr(i);
      Integer bytecode_offset = block.getOldOffset(i);
      BitSet bytecode_offsets = null;
      if (bytecode_offset >= 0) {
        bytecode_offsets = new BitSet();
        bytecode_offsets.set(bytecode_offset);
        int end_offset = block.getOldOffset(i+1);
        if (end_offset > bytecode_offset) {
          bytecode_offsets.set(bytecode_offset, end_offset);
        }
      }

      switch (instr.opcode) {
        case opc_aconst_null:
          pushEx(stack, exprlist, new ConstExprent(VarType.VARTYPE_NULL, null, bytecode_offsets));
          break;
        case opc_bipush:
        case opc_sipush:
          pushEx(stack, exprlist, new ConstExprent(instr.operand(0), true, bytecode_offsets));
          break;
        case opc_lconst_0:
        case opc_lconst_1:
          pushEx(stack, exprlist, new ConstExprent(VarType.VARTYPE_LONG, (long)(instr.opcode - opc_lconst_0), bytecode_offsets));
          break;
        case opc_fconst_0:
        case opc_fconst_1:
        case opc_fconst_2:
          pushEx(stack, exprlist, new ConstExprent(VarType.VARTYPE_FLOAT, (float)(instr.opcode - opc_fconst_0), bytecode_offsets));
          break;
        case opc_dconst_0:
        case opc_dconst_1:
          pushEx(stack, exprlist, new ConstExprent(VarType.VARTYPE_DOUBLE, (double)(instr.opcode - opc_dconst_0), bytecode_offsets));
          break;
        case opc_ldc:
        case opc_ldc_w:
        case opc_ldc2_w:
          PooledConstant cn = pool.getConstant(instr.operand(0));
          if (cn instanceof PrimitiveConstant) {
            pushEx(stack, exprlist, new ConstExprent(consts[cn.type - CONSTANT_Integer], ((PrimitiveConstant)cn).value, bytecode_offsets));
          }
          else if (cn instanceof LinkConstant && cn.type == CodeConstants.CONSTANT_Dynamic) {
            LinkConstant invoke_constant = (LinkConstant) cn;

            LinkConstant bootstrapMethod = null;
            List<PooledConstant> bootstrap_arguments = null;
            if (bootstrap != null) {
              bootstrapMethod = bootstrap.getMethodReference(invoke_constant.index1);
              bootstrap_arguments = bootstrap.getMethodArguments(invoke_constant.index1);
            }

            InvocationExprent exprinv = new InvocationExprent(instr.opcode, invoke_constant, bootstrapMethod, bootstrap_arguments, stack, bytecode_offsets);
            if (exprinv.getDescriptor().ret.type == CodeConstants.TYPE_VOID) {
              exprlist.add(exprinv);
            }
            else {
              pushEx(stack, exprlist, CondyHelper.simplifyCondy(exprinv));
            }
          }
          else if (cn instanceof LinkConstant) {
            //TODO: for now treat Links as Strings
            pushEx(stack, exprlist, new ConstExprent(VarType.VARTYPE_STRING, ((LinkConstant)cn).elementname, bytecode_offsets));
          }
          break;
        case opc_iload:
        case opc_lload:
        case opc_fload:
        case opc_dload:
        case opc_aload:
          VarExprent varExprent = new VarExprent(instr.operand(0), varTypes[instr.opcode - opc_iload], varProcessor, bytecode_offsets);
          varExprent.setBackingInstr(instr);
          varProcessor.findLVT(varExprent, bytecode_offset + instr.length);
          pushEx(stack, exprlist, varExprent);
          break;
        case opc_iaload:
        case opc_laload:
        case opc_faload:
        case opc_daload:
        case opc_aaload:
        case opc_baload:
        case opc_caload:
        case opc_saload:
          Exprent index = stack.pop();
          Exprent arr = stack.pop();

          VarType vartype = null;
          switch (instr.opcode) {
            case opc_laload:
              vartype = VarType.VARTYPE_LONG;
              break;
            case opc_daload:
              vartype = VarType.VARTYPE_DOUBLE;
          }
          pushEx(stack, exprlist, new ArrayExprent(arr, index, arrTypes[instr.opcode - opc_iaload], bytecode_offsets), vartype);
          break;
        case opc_istore:
        case opc_lstore:
        case opc_fstore:
        case opc_dstore:
        case opc_astore:
          Exprent expr = stack.pop();
          int varindex = instr.operand(0);
          if (bytecode_offsets != null) { //TODO: Figure out why this nulls in some cases
            bytecode_offsets.set(bytecode_offset, bytecode_offset + instr.length);
          }
          varExprent = new VarExprent(varindex, varTypes[instr.opcode - opc_istore], varProcessor, bytecode_offsets);
          varExprent.setBackingInstr(instr);
          varProcessor.findLVT(varExprent, bytecode_offset + instr.length);
          AssignmentExprent assign = new AssignmentExprent(varExprent, expr, bytecode_offsets);
          exprlist.add(assign);
          break;
        case opc_iastore:
        case opc_lastore:
        case opc_fastore:
        case opc_dastore:
        case opc_aastore:
        case opc_bastore:
        case opc_castore:
        case opc_sastore:
          Exprent value = stack.pop();
          Exprent index_store = stack.pop();
          Exprent arr_store = stack.pop();
          AssignmentExprent arrassign =
            new AssignmentExprent(new ArrayExprent(arr_store, index_store, arrTypes[instr.opcode - opc_iastore], bytecode_offsets), value,
                                  bytecode_offsets);
          exprlist.add(arrassign);
          break;
        case opc_iadd:
        case opc_ladd:
        case opc_fadd:
        case opc_dadd:
        case opc_isub:
        case opc_lsub:
        case opc_fsub:
        case opc_dsub:
        case opc_imul:
        case opc_lmul:
        case opc_fmul:
        case opc_dmul:
        case opc_idiv:
        case opc_ldiv:
        case opc_fdiv:
        case opc_ddiv:
        case opc_irem:
        case opc_lrem:
        case opc_frem:
        case opc_drem:
          pushEx(stack, exprlist, new FunctionExprent(func1[(instr.opcode - opc_iadd) / 4], stack, bytecode_offsets));
          break;
        case opc_ishl:
        case opc_lshl:
        case opc_ishr:
        case opc_lshr:
        case opc_iushr:
        case opc_lushr:
        case opc_iand:
        case opc_land:
        case opc_ior:
        case opc_lor:
        case opc_ixor:
        case opc_lxor:
          pushEx(stack, exprlist, new FunctionExprent(func2[(instr.opcode - opc_ishl) / 2], stack, bytecode_offsets));
          break;
        case opc_ineg:
        case opc_lneg:
        case opc_fneg:
        case opc_dneg:
          pushEx(stack, exprlist, new FunctionExprent(FunctionType.NEG, stack, bytecode_offsets));
          break;
        case opc_iinc:
          VarExprent vevar = new VarExprent(instr.operand(0), VarType.VARTYPE_INT, varProcessor, bytecode_offsets);
          vevar.setBackingInstr(instr);
          varProcessor.findLVT(vevar, bytecode_offset + instr.length);
          exprlist.add(new AssignmentExprent(vevar, new FunctionExprent(
            instr.operand(1) < 0 ? FunctionType.SUB : FunctionType.ADD, Arrays
            .asList(vevar.copy(), new ConstExprent(VarType.VARTYPE_INT, Math.abs(instr.operand(1)), null)),
            bytecode_offsets), bytecode_offsets));
          break;
        case opc_i2l:
        case opc_i2f:
        case opc_i2d:
        case opc_l2i:
        case opc_l2f:
        case opc_l2d:
        case opc_f2i:
        case opc_f2l:
        case opc_f2d:
        case opc_d2i:
        case opc_d2l:
        case opc_d2f:
        case opc_i2b:
        case opc_i2c:
        case opc_i2s:
          pushEx(stack, exprlist, new FunctionExprent(func3[instr.opcode - opc_i2l], stack, bytecode_offsets));
          break;
        case opc_lcmp:
        case opc_fcmpl:
        case opc_fcmpg:
        case opc_dcmpl:
        case opc_dcmpg:
          pushEx(stack, exprlist, new FunctionExprent(func4[instr.opcode - opc_lcmp], stack, bytecode_offsets));
          break;
        case opc_ifeq:
        case opc_ifne:
        case opc_iflt:
        case opc_ifge:
        case opc_ifgt:
        case opc_ifle:
          exprlist.add(new IfExprent(func5[instr.opcode - opc_ifeq].getNegative(), stack, bytecode_offsets));
          break;
        case opc_if_icmpeq:
        case opc_if_icmpne:
        case opc_if_icmplt:
        case opc_if_icmpge:
        case opc_if_icmpgt:
        case opc_if_icmple:
        case opc_if_acmpeq:
        case opc_if_acmpne:
          exprlist.add(new IfExprent(func6[instr.opcode - opc_if_icmpeq].getNegative(), stack, bytecode_offsets));
          break;
        case opc_ifnull:
        case opc_ifnonnull:
          exprlist.add(new IfExprent(func7[instr.opcode - opc_ifnull].getNegative(), stack, bytecode_offsets));
          break;
        case opc_tableswitch:
        case opc_lookupswitch:
          exprlist.add(new SwitchHeadExprent(stack.pop(), bytecode_offsets));
          break;
        case opc_ireturn:
        case opc_lreturn:
        case opc_freturn:
        case opc_dreturn:
        case opc_areturn:
        case opc_return:
        case opc_athrow:
          exprlist.add(new ExitExprent(instr.opcode == opc_athrow ? ExitExprent.Type.THROW : ExitExprent.Type.RETURN,
                                       instr.opcode == opc_return ? null : stack.pop(),
                                       instr.opcode == opc_athrow ? null : methodDescriptor.ret,
                                       bytecode_offsets, methodDescriptor));
          break;
        case opc_monitorenter:
        case opc_monitorexit:
          MonitorExprent monitor = new MonitorExprent(func8[instr.opcode - opc_monitorenter], stack.pop(), bytecode_offsets);

          if (instr.opcode == opc_monitorexit && stat.isRemovableMonitorexit()) {
            monitor.setRemove(true);
          }

          exprlist.add(monitor);
          break;
        case opc_checkcast:
        case opc_instanceof:
          stack.push(new ConstExprent(new VarType(pool.getPrimitiveConstant(instr.operand(0)).getString(), true), null, null));
        case opc_arraylength:
          pushEx(stack, exprlist, new FunctionExprent(mapConsts.get(instr.opcode), stack, bytecode_offsets));
          break;
        case opc_getstatic:
        case opc_getfield:
          pushEx(stack, exprlist,
                 new FieldExprent(pool.getLinkConstant(instr.operand(0)), instr.opcode == opc_getstatic ? null : stack.pop(),
                                  bytecode_offsets));
          break;
        case opc_putstatic:
        case opc_putfield:
          Exprent valfield = stack.pop();
          Exprent exprfield =
            new FieldExprent(pool.getLinkConstant(instr.operand(0)), instr.opcode == opc_putstatic ? null : stack.pop(),
                             bytecode_offsets);
          exprlist.add(new AssignmentExprent(exprfield, valfield, bytecode_offsets));
          break;
        case opc_invokevirtual:
        case opc_invokespecial:
        case opc_invokestatic:
        case opc_invokeinterface:
        case opc_invokedynamic:
          if (instr.opcode != opc_invokedynamic || instr.bytecodeVersion.hasInvokeDynamic()) {
            LinkConstant invoke_constant = pool.getLinkConstant(instr.operand(0));

            LinkConstant bootstrapMethod = null;
            List<PooledConstant> bootstrap_arguments = null;
            if (instr.opcode == opc_invokedynamic && bootstrap != null) {
              bootstrapMethod = bootstrap.getMethodReference(invoke_constant.index1);
              bootstrap_arguments = bootstrap.getMethodArguments(invoke_constant.index1);
            }

            InvocationExprent exprinv = new InvocationExprent(instr.opcode, invoke_constant, bootstrapMethod, bootstrap_arguments, stack, bytecode_offsets);
            if (exprinv.getDescriptor().ret.type == CodeConstants.TYPE_VOID) {
              exprlist.add(exprinv);
            }
            else {
              pushEx(stack, exprlist, exprinv);
            }
          }
          break;
        case opc_new:
        case opc_anewarray:
        case opc_multianewarray:
          int dimensions = (instr.opcode == opc_new) ? 0 : (instr.opcode == opc_anewarray) ? 1 : instr.operand(1);
          VarType arrType = new VarType(pool.getPrimitiveConstant(instr.operand(0)).getString(), true);
          if (instr.opcode != opc_multianewarray) {
            arrType = arrType.resizeArrayDim(arrType.arrayDim + dimensions);
          }
          pushEx(stack, exprlist, new NewExprent(arrType, stack, dimensions, bytecode_offsets));
          break;
        case opc_newarray:
          pushEx(stack, exprlist, new NewExprent(new VarType(arrTypeIds[instr.operand(0) - 4], 1), stack, 1, bytecode_offsets));
          break;
        case opc_dup:
          pushEx(stack, exprlist, stack.getByOffset(-1).copy());
          break;
        case opc_dup_x1:
          insertByOffsetEx(-2, stack, exprlist, -1);
          break;
        case opc_dup_x2:
          if (stack.getByOffset(-2).getExprType().stackSize == 2) {
            insertByOffsetEx(-2, stack, exprlist, -1);
          }
          else {
            insertByOffsetEx(-3, stack, exprlist, -1);
          }
          break;
        case opc_dup2:
          if (stack.getByOffset(-1).getExprType().stackSize == 2) {
            pushEx(stack, exprlist, stack.getByOffset(-1).copy());
          }
          else {
            pushEx(stack, exprlist, stack.getByOffset(-2).copy());
            pushEx(stack, exprlist, stack.getByOffset(-2).copy());
          }
          break;
        case opc_dup2_x1:
          if (stack.getByOffset(-1).getExprType().stackSize == 2) {
            insertByOffsetEx(-2, stack, exprlist, -1);
          }
          else {
            insertByOffsetEx(-3, stack, exprlist, -2);
            insertByOffsetEx(-3, stack, exprlist, -1);
          }
          break;
        case opc_dup2_x2:
          if (stack.getByOffset(-1).getExprType().stackSize == 2) {
            if (stack.getByOffset(-2).getExprType().stackSize == 2) {
              insertByOffsetEx(-2, stack, exprlist, -1);
            }
            else {
              insertByOffsetEx(-3, stack, exprlist, -1);
            }
          }
          else {
            if (stack.getByOffset(-3).getExprType().stackSize == 2) {
              insertByOffsetEx(-3, stack, exprlist, -2);
              insertByOffsetEx(-3, stack, exprlist, -1);
            }
            else {
              insertByOffsetEx(-4, stack, exprlist, -2);
              insertByOffsetEx(-4, stack, exprlist, -1);
            }
          }
          break;
        case opc_swap:
          insertByOffsetEx(-2, stack, exprlist, -1);
          stack.pop();
          break;
        case opc_pop:
          stack.pop();
          // check for synthetic getClass and requireNonNull calls added by the compiler
          // see https://stackoverflow.com/a/20130641
          if (!exprlist.isEmpty()) {
            Exprent last = exprlist.get(exprlist.size() - 1);
            // Our heuristic is checking for an assignment and the type of the assignment is an invocation.
            // This roughly corresponds to a pattern of DUP [nullcheck] POP.
            if (last instanceof AssignmentExprent && ((AssignmentExprent)last).getRight() instanceof InvocationExprent) {
              InvocationExprent invocation = (InvocationExprent) ((AssignmentExprent) last).getRight();

              // Check to make sure there's still more opcodes after this one
              if (i + 1 < seq.length()) {
                // Match either this.getClass() or Objects.requireNonNull([value]);
                if ((!invocation.isStatic() && invocation.getName().equals("getClass") && invocation.getStringDescriptor().equals("()Ljava/lang/Class;")) // J8
                  || (invocation.isStatic() && invocation.getClassname().equals("java/util/Objects") && invocation.getName().equals("requireNonNull") && invocation.getStringDescriptor().equals("(Ljava/lang/Object;)Ljava/lang/Object;"))) { // J9+

                  // Ensure that these null checks are constant loads, LDC opcodes, null loads, or bi/sipushes.
                  int nextOpc = seq.getInstr(i + 1).opcode;
                  if (nextOpc >= opc_aconst_null && nextOpc <= opc_ldc2_w) {
                    invocation.setSyntheticNullCheck();
                  }
                }
              }
            }
          }
          break;
        case opc_pop2:
          if (stack.getByOffset(-1).getExprType().stackSize == 1) {
            // Since value at the top of the stack is a value of category 1 (JVMS9 2.11.1)
            // we should remove one more item from the stack.
            // See JVMS9 pop2 chapter.
            stack.pop();
          }
          stack.pop();
          break;
      }
    }
  }

  private void pushEx(ListStack<Exprent> stack, List<Exprent> exprlist, Exprent exprent) {
    pushEx(stack, exprlist, exprent, null);
  }

  private void pushEx(ListStack<Exprent> stack, List<Exprent> exprlist, Exprent exprent, VarType vartype) {
    ValidationHelper.notNull(exprent);

    int varindex = VarExprent.STACK_BASE + stack.size();
    VarExprent var = new VarExprent(varindex, vartype == null ? exprent.getExprType() : vartype, varProcessor);
    var.setStack(true);

    exprlist.add(new AssignmentExprent(var, exprent, null));
    stack.push(var.copy());
  }

  private void insertByOffsetEx(int offset, ListStack<Exprent> stack, List<Exprent> exprlist, int copyoffset) {

    int base = VarExprent.STACK_BASE + stack.size();

    LinkedList<VarExprent> lst = new LinkedList<>();

    for (int i = -1; i >= offset; i--) {
      Exprent varex = stack.pop();
      VarExprent varnew = new VarExprent(base + i + 1, varex.getExprType(), varProcessor);
      varnew.setStack(true);
      exprlist.add(new AssignmentExprent(varnew, varex, null));
      lst.add(0, (VarExprent)varnew.copy());
    }

    Exprent exprent = lst.get(lst.size() + copyoffset).copy();
    VarExprent var = new VarExprent(base + offset, exprent.getExprType(), varProcessor);
    var.setStack(true);
    exprlist.add(new AssignmentExprent(var, exprent, null));
    lst.add(0, (VarExprent)var.copy());

    for (VarExprent expr : lst) {
      stack.push(expr);
    }
  }

  public static boolean canonicalizeCasts(RootStatement stat) {
    boolean res = false;
    while (canonicalizeCasts((Statement) stat)) {
      res = true;
    }

    return res;
  }

  private static boolean canonicalizeCasts(Statement stat) {
    boolean res = false;
    for (Statement st : stat.getStats()) {
      res |= canonicalizeCasts(st);
    }

    if (stat instanceof BasicBlockStatement) {
      for (Exprent exprent : stat.getExprents()) {
        for (Exprent ex : exprent.getAllExprents(true, true)) {

          // Remove Checkcast(Type, Checkcast(Type, ...)) and turn it just into Checkcast(Type, ...) where both have the same type
          // The extra checkcast causes issues with generic type decompilation
          if (ex instanceof FunctionExprent && ((FunctionExprent)ex).getFuncType() == FunctionExprent.FunctionType.CAST) {
            FunctionExprent func = (FunctionExprent)ex;
            Exprent inner = func.getLstOperands().get(0);
            Exprent cast = func.getLstOperands().get(1);

            if (inner instanceof FunctionExprent && ((FunctionExprent)inner).getFuncType() == FunctionExprent.FunctionType.CAST) {
              FunctionExprent func2 = (FunctionExprent)inner;
              Exprent inner2 = func2.getLstOperands().get(0);
              Exprent cast2 = func2.getLstOperands().get(1);

              if (cast.getExprType().equals(cast2.getExprType())) {
                ex.replaceExprent(inner, inner2);
                ex.addBytecodeOffsets(inner2.bytecode);
                ex.addBytecodeOffsets(inner.bytecode);
                res = true;
              }
            }
          }
        }
      }
    }

    return res;
  }

  public static void markExprOddities(RootStatement root) {
    // We shouldn't have to do this, but turns out getting cast names is not pure. Sigh.
    DecompilerContext.getImportCollector().setWriteLocked(true);

    markExprOddities(root, root);

    DecompilerContext.getImportCollector().setWriteLocked(false);
  }

  private static void markExprOddities(RootStatement root, Statement stat) {
    for (Statement st : stat.getStats()) {
      markExprOddities(root, st);
    }

    for (Exprent ex : stat.getVarDefinitions()) {
      markExprOddity(root, ex);
    }

    if (stat instanceof BasicBlockStatement) {
      for (Exprent ex : stat.getExprents()) {
        markExprOddity(root, ex);
      }
    }
  }

  private static void markExprOddity(RootStatement root, Exprent ex) {
    if (ex instanceof MonitorExprent) {
      root.addComment("$VF: Could not create synchronized statement, marking monitor enters and exits", true);
    }
    if (ex instanceof IfExprent) {
      root.addComment("$VF: Accidentally destroyed if statement, the decompiled code is not correct!", true);
    }

    for (Exprent e : ex.getAllExprents(true, true)) {
      if (e instanceof VarExprent) {
        VarExprent var = (VarExprent)e;
        if (var.isDefinition() && isInvalidTypeName(var.getDefinitionType()) || var.getExprType() == VarType.VARTYPE_UNKNOWN) {
          root.addComment("$VF: Could not properly define all variable types!", true);
        }
      } else if (e instanceof FunctionExprent) {
        FunctionExprent func = (FunctionExprent)e;
        if (func.getFuncType() == FunctionType.CAST && func.doesCast()) {
          List<Exprent> operands = func.getLstOperands();
          if (isInvalidTypeName(operands.get(1).toString())) {
            root.addComment("$VF: Could not properly define all variable types!", true);
          }
        }
      }
    }
  }

  public static String getTypeName(VarType type) {
    return getTypeName(type, true);
  }

  public static String getTypeName(VarType type, boolean getShort) {
    int tp = type.type;
    if (tp <= CodeConstants.TYPE_BOOLEAN) {
      return typeNames[tp];
    }
    else if (tp == CodeConstants.TYPE_UNKNOWN) {
      return UNKNOWN_TYPE_STRING; // INFO: should not occur
    }
    else if (tp == CodeConstants.TYPE_NULL) {
      return NULL_TYPE_STRING; // INFO: should not occur
    }
    else if (tp == CodeConstants.TYPE_VOID) {
      return "void";
    }
    else if (tp == CodeConstants.TYPE_GENVAR && type.isGeneric()) {
      return type.value;
    }
    else if (tp == CodeConstants.TYPE_OBJECT) {
      if (type.isGeneric()) {
        return ((GenericType)type).getCastName();
      }
      String ret = buildJavaClassName(type.value);
      if (getShort) {
        ret = DecompilerContext.getImportCollector().getShortName(ret);
      }

      if (ret == null) {
        ret = UNDEFINED_TYPE_STRING;
      }
      return ret;
    }

    throw new RuntimeException("invalid type: " + tp);
  }

  public static boolean isInvalidTypeName(String name) {
    return UNDEFINED_TYPE_STRING.equals(name) || NULL_TYPE_STRING.equals(name) || UNKNOWN_TYPE_STRING.equals(name);
  }

  public static String getCastTypeName(VarType type) {
    return getCastTypeName(type, true);
  }

  public static String getCastTypeName(VarType type, boolean getShort) {
    StringBuilder s = new StringBuilder(getTypeName(type, getShort));
    TextUtil.append(s, "[]", type.arrayDim);
    return s.toString();
  }

  public static PrimitiveExprsList getExpressionData(VarExprent var) {
    PrimitiveExprsList prlst = new PrimitiveExprsList();
    VarExprent vartmp = new VarExprent(VarExprent.STACK_BASE, var.getExprType(), var.getProcessor());
    vartmp.setStack(true);

    prlst.getLstExprents().add(new AssignmentExprent(vartmp, var.copy(), null));
    prlst.getStack().push(vartmp.copy());
    return prlst;
  }

  public static boolean endsWithSemicolon(Exprent expr) {
    return !(expr instanceof SwitchHeadExprent ||
             expr instanceof MonitorExprent ||
             expr instanceof IfExprent ||
             (expr instanceof VarExprent && ((VarExprent)expr).isClassDef()));
  }

  private static void addDeletedGotoInstructionMapping(Statement stat, TextBuffer buffer) {
    if (stat instanceof BasicBlockStatement) {
      BasicBlock block = ((BasicBlockStatement)stat).getBlock();
      List<Integer> offsets = block.getInstrOldOffsets();
      if (!offsets.isEmpty() &&
          offsets.size() > block.getSeq().length()) { // some instructions have been deleted, but we still have offsets
        buffer.addBytecodeMapping(offsets.get(offsets.size() - 1)); // add the last offset
      }
    }
  }

  public static TextBuffer jmpWrapper(Statement stat, int indent, boolean semicolon) {
    TextBuffer buf = stat.toJava(indent);

    List<StatEdge> lstSuccs = stat.getSuccessorEdges(Statement.STATEDGE_DIRECT_ALL);
    if (lstSuccs.size() == 1) {
      StatEdge edge = lstSuccs.get(0);
      if (edge.getType() != StatEdge.TYPE_REGULAR && edge.explicit && !(edge.getDestination() instanceof DummyExitStatement)) {
        buf.appendIndent(indent);

        switch (edge.getType()) {
          case StatEdge.TYPE_BREAK:
            addDeletedGotoInstructionMapping(stat, buf);
            buf.append("break");
            break;
          case StatEdge.TYPE_CONTINUE:
            addDeletedGotoInstructionMapping(stat, buf);
            buf.append("continue");
        }

        if (edge.labeled) {
          buf.append(" label").append(edge.closure.id);
        }
        buf.append(";").appendLineSeparator();
      }
    }

    if (buf.length() == 0 && semicolon) {
      buf.appendIndent(indent).append(";").appendLineSeparator();
    }

    return buf;
  }

  public static String buildJavaClassName(String name) {
    String res = name.replace('/', '.');

    if (res.contains("$")) { // attempt to invoke foreign member
      // classes correctly
      StructClass cl = DecompilerContext.getStructContext().getClass(name);
      if (cl == null || !cl.isOwn()) {
        res = res.replace('$', '.');
      }
    }

    return res;
  }

  public static TextBuffer listToJava(List<? extends Exprent> lst, int indent) {
    if (lst == null || lst.isEmpty()) {
      return new TextBuffer();
    }

    TextBuffer buf = new TextBuffer();
    lst = Exprent.sortIndexed(lst);

    for (Exprent expr : lst) {
      if (buf.length() > 0 && expr instanceof VarExprent && ((VarExprent)expr).isClassDef()) {
        // separates local class definition from previous statements
        buf.appendLineSeparator();
      }

      expr.getInferredExprType(null);

      TextBuffer content = expr.toJava(indent);

      if (content.length() > 0) {
        if (!(expr instanceof VarExprent) || !((VarExprent)expr).isClassDef()) {
          buf.appendIndent(indent);
        }
        buf.append(content);
        if (expr instanceof MonitorExprent && ((MonitorExprent)expr).getMonType() == MonitorExprent.Type.ENTER) {
          buf.append("{} // $VF: monitorenter "); // empty synchronized block
        }
        if (endsWithSemicolon(expr)) {
          buf.append(";");
        }
        buf.appendLineSeparator();
      }
    }

    return buf;
  }

  public static ConstExprent getDefaultArrayValue(VarType arrType) {
    ConstExprent defaultVal;
    if (arrType.type == CodeConstants.TYPE_OBJECT || arrType.arrayDim > 0) {
      defaultVal = new ConstExprent(VarType.VARTYPE_NULL, null, null);
    }
    else if (arrType.type == CodeConstants.TYPE_FLOAT) {
      defaultVal = new ConstExprent(VarType.VARTYPE_FLOAT, 0f, null);
    }
    else if (arrType.type == CodeConstants.TYPE_LONG) {
      defaultVal = new ConstExprent(VarType.VARTYPE_LONG, 0L, null);
    }
    else if (arrType.type == CodeConstants.TYPE_DOUBLE) {
      defaultVal = new ConstExprent(VarType.VARTYPE_DOUBLE, 0d, null);
    }
    else { // integer types
      defaultVal = new ConstExprent(0, true, null);
    }
    return defaultVal;
  }

  public static boolean getCastedExprent(Exprent exprent,
                                         VarType leftType,
                                         TextBuffer buffer,
                                         int indent,
                                         boolean castNull) {
    return getCastedExprent(exprent, leftType, buffer, indent, castNull ? NullCastType.CAST : NullCastType.DONT_CAST, false, false, false);
  }

  public static boolean getCastedExprent(Exprent exprent,
                                         VarType leftType,
                                         TextBuffer buffer,
                                         int indent,
                                         NullCastType castNull,
                                         boolean castAlways,
                                         boolean castNarrowing,
                                         boolean unbox) {

    if (unbox) {
      // "unbox" invocation parameters, e.g. 'byteSet.add((byte)123)' or 'new ShortContainer((short)813)'
      if (exprent instanceof InvocationExprent) {
        InvocationExprent invocationExprent = (InvocationExprent) exprent;
        if (invocationExprent.isBoxingCall() && !invocationExprent.shouldForceBoxing()) {
          exprent = invocationExprent.getLstParameters().get(0);
          int paramType = invocationExprent.getDescriptor().params[0].type;
          if (exprent instanceof ConstExprent && ((ConstExprent) exprent).getConstType().type != paramType) {
            leftType = new VarType(paramType);
          }
        }
      }
    }

    VarType rightType = exprent.getInferredExprType(leftType);
    exprent = narrowGenericCastType(exprent, leftType);

    boolean doCast = (!leftType.isSuperset(rightType) && (rightType.equals(VarType.VARTYPE_OBJECT) || leftType.type != CodeConstants.TYPE_OBJECT));
    boolean doCastNull = (castNull.cast && rightType.type == CodeConstants.TYPE_NULL && !UNDEFINED_TYPE_STRING.equals(getTypeName(leftType)));
    boolean doCastNarrowing = (castNarrowing && isIntConstant(exprent) && isNarrowedIntType(leftType));
    boolean doCastGenerics = doesContravarianceNeedCast(leftType, rightType);

    boolean cast = castAlways || doCast || doCastNull || doCastNarrowing || doCastGenerics;

    if (castNull == NullCastType.DONT_CAST_AT_ALL && rightType.type == CodeConstants.TYPE_NULL) {
      cast = castAlways;
    }

    boolean castLambda = !cast && exprent instanceof NewExprent && !leftType.equals(rightType) &&
                          lambdaNeedsCast(leftType, (NewExprent)exprent);

    boolean quote = cast && exprent.getPrecedence() >= FunctionType.CAST.precedence;

    // cast instead to 'byte' / 'short' when int constant is used as a value for 'Byte' / 'Short'
    if (castNarrowing && exprent instanceof ConstExprent && !((ConstExprent) exprent).isNull()) {
      if (leftType.equals(VarType.VARTYPE_BYTE_OBJ)) {
        leftType = VarType.VARTYPE_BYTE;
      }
      else if (leftType.equals(VarType.VARTYPE_SHORT_OBJ)) {
        leftType = VarType.VARTYPE_SHORT;
      }
    }

    if (cast) {
      buffer.append('(').append(getCastTypeName(leftType)).append(')');
    }

    if (castLambda) {
      buffer.append('(').append(getCastTypeName(rightType)).append(')');
    }

    if (quote) {
      buffer.append('(');
    }

    if (exprent instanceof ConstExprent) {
      ((ConstExprent) exprent).adjustConstType(leftType);
    }

    buffer.append(exprent.toJava(indent));

    if (quote) {
      buffer.append(')');
    }

    return cast;
  }

  public enum NullCastType {
    CAST(true), // old boolean true
    DONT_CAST(false), // old booean false
    DONT_CAST_AT_ALL(false); // old boolean false and don't cast

    private final boolean cast;

    NullCastType(boolean cast) {
      this.cast = cast;
    }
  }

  // (Obj)expr; -> (Obj<T>)expr;
  public static Exprent narrowGenericCastType(Exprent expr, VarType type) {
    if (type.isGeneric() && expr instanceof FunctionExprent && ((FunctionExprent)expr).getFuncType() == FunctionType.CAST) {
      FunctionExprent func = (FunctionExprent) expr;
      VarType funcType = func.getExprType();

      GenericType genType = (GenericType) type;
      if (funcType.value.equals(type.value) && !genType.getArguments().isEmpty()) {
        // Trying to cast to a generic type but the cast isn't generic- invalid!
        if (!funcType.isGeneric()) {
          ConstExprent cast = ((ConstExprent) func.getLstOperands().get(1));
          cast.setConstType(type);
        } else if (genType.equalsExact(funcType) && !func.doesCast()) {
          func.setNeedsCast(true);
        }
      }
    }

    return expr;
  }

  // Obj<T> var = type; -> Obj<T> var = (Obj<T>) type; Where type is Obj<? super T>
  public static boolean doesContravarianceNeedCast(VarType left, VarType right) {
    if (left != null && right != null && left.isGeneric() && right.isGeneric()) {
      GenericType leftGeneric = (GenericType) left;
      GenericType rightGeneric = (GenericType) right;

      if (leftGeneric.getArguments().size() != rightGeneric.getArguments().size()) {
        return false;
      }

      for (int i = 0; i < leftGeneric.getArguments().size(); i++) {
        VarType leftType = leftGeneric.getArguments().get(i);
        VarType rightType = rightGeneric.getArguments().get(i);

        if (leftType != null && rightType != null && leftType.isSuperset(rightType) &&
          (leftType.isGeneric() && rightType.isGeneric()) &&
          (((GenericType) leftType).getWildcard() == GenericType.WILDCARD_NO || ((GenericType) leftType).getWildcard() == GenericType.WILDCARD_EXTENDS) &&
          ((GenericType) rightType).getWildcard() == GenericType.WILDCARD_SUPER) {
          return true;
        }
      }
    }

    return false;
  }

  private static boolean isIntConstant(Exprent exprent) {
    if (exprent instanceof ConstExprent) {
      switch (((ConstExprent)exprent).getConstType().type) {
        case CodeConstants.TYPE_BYTE:
        case CodeConstants.TYPE_BYTECHAR:
        case CodeConstants.TYPE_SHORT:
        case CodeConstants.TYPE_SHORTCHAR:
        case CodeConstants.TYPE_INT:
          return true;
      }
    }

    return false;
  }

  private static boolean isNarrowedIntType(VarType type) {
    return VarType.VARTYPE_INT.isStrictSuperset(type) ||
           type.equals(VarType.VARTYPE_BYTE_OBJ) || type.equals(VarType.VARTYPE_SHORT_OBJ);
  }

  private static boolean lambdaNeedsCast(VarType left, NewExprent exprent) {
    if (exprent.isLambda() && !exprent.isMethodReference()) {
      StructClass cls = DecompilerContext.getStructContext().getClass(left.value);
      return cls == null || cls.getMethod(exprent.getLambdaMethodKey()) == null;
    }
    return false;
  }
}
