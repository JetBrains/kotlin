// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.java.decompiler.code.interpreter;

import org.jetbrains.java.decompiler.code.CodeConstants;
import org.jetbrains.java.decompiler.code.Instruction;
import org.jetbrains.java.decompiler.struct.consts.ConstantPool;
import org.jetbrains.java.decompiler.struct.consts.LinkConstant;
import org.jetbrains.java.decompiler.struct.consts.PooledConstant;
import org.jetbrains.java.decompiler.struct.consts.PrimitiveConstant;
import org.jetbrains.java.decompiler.struct.gen.DataPoint;
import org.jetbrains.java.decompiler.struct.gen.FieldDescriptor;
import org.jetbrains.java.decompiler.struct.gen.MethodDescriptor;
import org.jetbrains.java.decompiler.struct.gen.VarType;
import org.jetbrains.java.decompiler.util.ListStack;

public final class InstructionImpact {

  // {read, write}
  private static final int[][][] stack_impact = {

    {null, null},                                                //		public final static int		opc_nop = 0;
    null,                                                                //		public final static int		opc_aconst_null = 1;
    null,                                                                //		public final static int		opc_iconst_m1 = 2;
    null,                                                                //		public final static int		opc_iconst_0 = 3;
    null,                                                                //		public final static int		opc_iconst_1 = 4;
    null,                                                                //		public final static int		opc_iconst_2 = 5;
    null,                        //		public final static int		opc_iconst_3 = 6;
    null,                        //		public final static int		opc_iconst_4 = 7;
    null,                        //		public final static int		opc_iconst_5 = 8;
    {null, {CodeConstants.TYPE_LONG}},                        //		public final static int		opc_lconst_0 = 9;
    {null, {CodeConstants.TYPE_LONG}},                        //		public final static int		opc_lconst_1 = 10;
    {null, {CodeConstants.TYPE_FLOAT}},                        //		public final static int		opc_fconst_0 = 11;
    {null, {CodeConstants.TYPE_FLOAT}},                        //		public final static int		opc_fconst_1 = 12;
    {null, {CodeConstants.TYPE_FLOAT}},                        //		public final static int		opc_fconst_2 = 13;
    {null, {CodeConstants.TYPE_DOUBLE}},                        //		public final static int		opc_dconst_0 = 14;
    {null, {CodeConstants.TYPE_DOUBLE}},                        //		public final static int		opc_dconst_1 = 15;
    {null, {CodeConstants.TYPE_INT}},                        //		public final static int		opc_bipush = 16;
    {null, {CodeConstants.TYPE_INT}},                        //		public final static int		opc_sipush = 17;
    null,                        //		public final static int		opc_ldc = 18;
    null,                        //		public final static int		opc_ldc_w = 19;
    null,                        //		public final static int		opc_ldc2_w = 20;
    {null, {CodeConstants.TYPE_INT}},                        //		public final static int		opc_iload = 21;
    {null, {CodeConstants.TYPE_LONG}},                        //		public final static int		opc_lload = 22;
    {null, {CodeConstants.TYPE_FLOAT}},                        //		public final static int		opc_fload = 23;
    {null, {CodeConstants.TYPE_DOUBLE}},                        //		public final static int		opc_dload = 24;
    null,                        //		public final static int		opc_aload = 25;
    null,                        //		public final static int		opc_iload_0 = 26;
    null,                        //		public final static int		opc_iload_1 = 27;
    null,                        //		public final static int		opc_iload_2 = 28;
    null,                        //		public final static int		opc_iload_3 = 29;
    null,                        //		public final static int		opc_lload_0 = 30;
    null,                        //		public final static int		opc_lload_1 = 31;
    null,                        //		public final static int		opc_lload_2 = 32;
    null,                        //		public final static int		opc_lload_3 = 33;
    null,                        //		public final static int		opc_fload_0 = 34;
    null,                        //		public final static int		opc_fload_1 = 35;
    null,                        //		public final static int		opc_fload_2 = 36;
    null,                        //		public final static int		opc_fload_3 = 37;
    null,                        //		public final static int		opc_dload_0 = 38;
    null,                        //		public final static int		opc_dload_1 = 39;
    null,                        //		public final static int		opc_dload_2 = 40;
    null,                        //		public final static int		opc_dload_3 = 41;
    null,                        //		public final static int		opc_aload_0 = 42;
    null,                        //		public final static int		opc_aload_1 = 43;
    null,                        //		public final static int		opc_aload_2 = 44;
    null,                        //		public final static int		opc_aload_3 = 45;
    {{CodeConstants.TYPE_OBJECT, CodeConstants.TYPE_INT}, {CodeConstants.TYPE_INT}},
    //		public final static int		opc_iaload = 46;
    {{CodeConstants.TYPE_OBJECT, CodeConstants.TYPE_INT}, {CodeConstants.TYPE_LONG}},
    //		public final static int		opc_laload = 47;
    {{CodeConstants.TYPE_OBJECT, CodeConstants.TYPE_INT}, {CodeConstants.TYPE_FLOAT}},
    //		public final static int		opc_faload = 48;
    {{CodeConstants.TYPE_OBJECT, CodeConstants.TYPE_INT}, {CodeConstants.TYPE_DOUBLE}},
    //		public final static int		opc_daload = 49;
    null,                        //		public final static int		opc_aaload = 50;
    {{CodeConstants.TYPE_OBJECT, CodeConstants.TYPE_INT}, {CodeConstants.TYPE_INT}},
    //		public final static int		opc_baload = 51;
    {{CodeConstants.TYPE_OBJECT, CodeConstants.TYPE_INT}, {CodeConstants.TYPE_INT}},
    //		public final static int		opc_caload = 52;
    {{CodeConstants.TYPE_OBJECT, CodeConstants.TYPE_INT}, {CodeConstants.TYPE_INT}},
    //		public final static int		opc_saload = 53;
    {{CodeConstants.TYPE_INT}, null},                        //		public final static int		opc_istore = 54;
    {{CodeConstants.TYPE_LONG}, null},                        //		public final static int		opc_lstore = 55;
    {{CodeConstants.TYPE_FLOAT}, null},                        //		public final static int		opc_fstore = 56;
    {{CodeConstants.TYPE_DOUBLE}, null},                        //		public final static int		opc_dstore = 57;
    null,                        //		public final static int		opc_astore = 58;
    null,                        //		public final static int		opc_istore_0 = 59;
    null,                        //		public final static int		opc_istore_1 = 60;
    null,                        //		public final static int		opc_istore_2 = 61;
    null,                        //		public final static int		opc_istore_3 = 62;
    null,                        //		public final static int		opc_lstore_0 = 63;
    null,                        //		public final static int		opc_lstore_1 = 64;
    null,                        //		public final static int		opc_lstore_2 = 65;
    null,                        //		public final static int		opc_lstore_3 = 66;
    null,                        //		public final static int		opc_fstore_0 = 67;
    null,                        //		public final static int		opc_fstore_1 = 68;
    null,                        //		public final static int		opc_fstore_2 = 69;
    null,                        //		public final static int		opc_fstore_3 = 70;
    null,                        //		public final static int		opc_dstore_0 = 71;
    null,                        //		public final static int		opc_dstore_1 = 72;
    null,                        //		public final static int		opc_dstore_2 = 73;
    null,                        //		public final static int		opc_dstore_3 = 74;
    null,                        //		public final static int		opc_astore_0 = 75;
    null,                        //		public final static int		opc_astore_1 = 76;
    null,                        //		public final static int		opc_astore_2 = 77;
    null,                        //		public final static int		opc_astore_3 = 78;
    {{CodeConstants.TYPE_OBJECT, CodeConstants.TYPE_INT, CodeConstants.TYPE_INT}, null},
    //		public final static int		opc_iastore = 79;
    {{CodeConstants.TYPE_OBJECT, CodeConstants.TYPE_INT, CodeConstants.TYPE_LONG}, null},
    //		public final static int		opc_lastore = 80;
    {{CodeConstants.TYPE_OBJECT, CodeConstants.TYPE_INT, CodeConstants.TYPE_FLOAT}, null},
    //		public final static int		opc_fastore = 81;
    {{CodeConstants.TYPE_OBJECT, CodeConstants.TYPE_INT, CodeConstants.TYPE_DOUBLE}, null},
    //		public final static int		opc_dastore = 82;
    {{CodeConstants.TYPE_OBJECT, CodeConstants.TYPE_INT, CodeConstants.TYPE_OBJECT}, null},
    //		public final static int		opc_aastore = 83;
    {{CodeConstants.TYPE_OBJECT, CodeConstants.TYPE_INT, CodeConstants.TYPE_INT}, null},
    //		public final static int		opc_bastore = 84;
    {{CodeConstants.TYPE_OBJECT, CodeConstants.TYPE_INT, CodeConstants.TYPE_INT}, null},
    //		public final static int		opc_castore = 85;
    {{CodeConstants.TYPE_OBJECT, CodeConstants.TYPE_INT, CodeConstants.TYPE_INT}, null},
    //		public final static int		opc_sastore = 86;
    {{CodeConstants.TYPE_ANY}, null},                        //		public final static int		opc_pop = 87;
    {{CodeConstants.TYPE_ANY, CodeConstants.TYPE_ANY}, null},                        //		public final static int		opc_pop2 = 88;
    null,                        //		public final static int		opc_dup = 89;
    null,                        //		public final static int		opc_dup_x1 = 90;
    null,                        //		public final static int		opc_dup_x2 = 91;
    null,                        //		public final static int		opc_dup2 = 92;
    null,                        //		public final static int		opc_dup2_x1 = 93;
    null,                        //		public final static int		opc_dup2_x2 = 94;
    null,                        //		public final static int		opc_swap = 95;
    {{CodeConstants.TYPE_INT, CodeConstants.TYPE_INT}, {CodeConstants.TYPE_INT}},
    //		public final static int		opc_iadd = 96;
    {{CodeConstants.TYPE_LONG, CodeConstants.TYPE_LONG}, {CodeConstants.TYPE_LONG}},
    //		public final static int		opc_ladd = 97;
    {{CodeConstants.TYPE_FLOAT, CodeConstants.TYPE_FLOAT}, {CodeConstants.TYPE_FLOAT}},
    //		public final static int		opc_fadd = 98;
    {{CodeConstants.TYPE_DOUBLE, CodeConstants.TYPE_DOUBLE}, {CodeConstants.TYPE_DOUBLE}},
    //		public final static int		opc_dadd = 99;
    {{CodeConstants.TYPE_INT, CodeConstants.TYPE_INT}, {CodeConstants.TYPE_INT}},
    //		public final static int		opc_isub = 100;
    {{CodeConstants.TYPE_LONG, CodeConstants.TYPE_LONG}, {CodeConstants.TYPE_LONG}},
    //		public final static int		opc_lsub = 101;
    {{CodeConstants.TYPE_FLOAT, CodeConstants.TYPE_FLOAT}, {CodeConstants.TYPE_FLOAT}},
    //		public final static int		opc_fsub = 102;
    {{CodeConstants.TYPE_DOUBLE, CodeConstants.TYPE_DOUBLE}, {CodeConstants.TYPE_DOUBLE}},
    //		public final static int		opc_dsub = 103;
    {{CodeConstants.TYPE_INT, CodeConstants.TYPE_INT}, {CodeConstants.TYPE_INT}},
    //		public final static int		opc_imul = 104;
    {{CodeConstants.TYPE_LONG, CodeConstants.TYPE_LONG}, {CodeConstants.TYPE_LONG}},
    //		public final static int		opc_lmul = 105;
    {{CodeConstants.TYPE_FLOAT, CodeConstants.TYPE_FLOAT}, {CodeConstants.TYPE_FLOAT}},
    //		public final static int		opc_fmul = 106;
    {{CodeConstants.TYPE_DOUBLE, CodeConstants.TYPE_DOUBLE}, {CodeConstants.TYPE_DOUBLE}},
    //		public final static int		opc_dmul = 107;
    {{CodeConstants.TYPE_INT, CodeConstants.TYPE_INT}, {CodeConstants.TYPE_INT}},
    //		public final static int		opc_idiv = 108;
    {{CodeConstants.TYPE_LONG, CodeConstants.TYPE_LONG}, {CodeConstants.TYPE_LONG}},
    //		public final static int		opc_ldiv = 109;
    {{CodeConstants.TYPE_FLOAT, CodeConstants.TYPE_FLOAT}, {CodeConstants.TYPE_FLOAT}},
    //		public final static int		opc_fdiv = 110;
    {{CodeConstants.TYPE_DOUBLE, CodeConstants.TYPE_DOUBLE}, {CodeConstants.TYPE_DOUBLE}},
    //		public final static int		opc_ddiv = 111;
    {{CodeConstants.TYPE_INT, CodeConstants.TYPE_INT}, {CodeConstants.TYPE_INT}},
    //		public final static int		opc_irem = 112;
    {{CodeConstants.TYPE_LONG, CodeConstants.TYPE_LONG}, {CodeConstants.TYPE_LONG}},
    //		public final static int		opc_lrem = 113;
    {{CodeConstants.TYPE_FLOAT, CodeConstants.TYPE_FLOAT}, {CodeConstants.TYPE_FLOAT}},
    //		public final static int		opc_frem = 114;
    {{CodeConstants.TYPE_DOUBLE, CodeConstants.TYPE_DOUBLE}, {CodeConstants.TYPE_DOUBLE}},
    //		public final static int		opc_drem = 115;
    {{CodeConstants.TYPE_INT}, {CodeConstants.TYPE_INT}},                        //		public final static int		opc_ineg = 116;
    {{CodeConstants.TYPE_LONG}, {CodeConstants.TYPE_LONG}},                        //		public final static int		opc_lneg = 117;
    {{CodeConstants.TYPE_FLOAT}, {CodeConstants.TYPE_FLOAT}},                        //		public final static int		opc_fneg = 118;
    {{CodeConstants.TYPE_DOUBLE}, {CodeConstants.TYPE_DOUBLE}},                        //		public final static int		opc_dneg = 119;
    {{CodeConstants.TYPE_INT, CodeConstants.TYPE_INT}, {CodeConstants.TYPE_INT}},
    //		public final static int		opc_ishl = 120;
    {{CodeConstants.TYPE_LONG, CodeConstants.TYPE_INT}, {CodeConstants.TYPE_LONG}},
    //		public final static int		opc_lshl = 121;
    {{CodeConstants.TYPE_INT, CodeConstants.TYPE_INT}, {CodeConstants.TYPE_INT}},
    //		public final static int		opc_ishr = 122;
    {{CodeConstants.TYPE_LONG, CodeConstants.TYPE_INT}, {CodeConstants.TYPE_LONG}},
    //		public final static int		opc_lshr = 123;
    {{CodeConstants.TYPE_INT, CodeConstants.TYPE_INT}, {CodeConstants.TYPE_INT}},
    //		public final static int		opc_iushr = 124;
    {{CodeConstants.TYPE_LONG, CodeConstants.TYPE_INT}, {CodeConstants.TYPE_LONG}},
    //		public final static int		opc_lushr = 125;
    {{CodeConstants.TYPE_INT, CodeConstants.TYPE_INT}, {CodeConstants.TYPE_INT}},
    //		public final static int		opc_iand = 126;
    {{CodeConstants.TYPE_LONG, CodeConstants.TYPE_LONG}, {CodeConstants.TYPE_LONG}},
    //		public final static int		opc_land = 127;
    {{CodeConstants.TYPE_INT, CodeConstants.TYPE_INT}, {CodeConstants.TYPE_INT}},
    //		public final static int		opc_ior = 128;
    {{CodeConstants.TYPE_LONG, CodeConstants.TYPE_LONG}, {CodeConstants.TYPE_LONG}},
    //		public final static int		opc_lor = 129;
    {{CodeConstants.TYPE_INT, CodeConstants.TYPE_INT}, {CodeConstants.TYPE_INT}},
    //		public final static int		opc_ixor = 130;
    {{CodeConstants.TYPE_LONG, CodeConstants.TYPE_LONG}, {CodeConstants.TYPE_LONG}},
    //		public final static int		opc_lxor = 131;
    {null, null},                        //		public final static int		opc_iinc = 132;
    {{CodeConstants.TYPE_INT}, {CodeConstants.TYPE_LONG}},                        //		public final static int		opc_i2l = 133;
    {{CodeConstants.TYPE_INT}, {CodeConstants.TYPE_FLOAT}},                        //		public final static int		opc_i2f = 134;
    {{CodeConstants.TYPE_INT}, {CodeConstants.TYPE_DOUBLE}},                        //		public final static int		opc_i2d = 135;
    {{CodeConstants.TYPE_LONG}, {CodeConstants.TYPE_INT}},                        //		public final static int		opc_l2i = 136;
    {{CodeConstants.TYPE_LONG}, {CodeConstants.TYPE_FLOAT}},                        //		public final static int		opc_l2f = 137;
    {{CodeConstants.TYPE_LONG}, {CodeConstants.TYPE_DOUBLE}},                        //		public final static int		opc_l2d = 138;
    {{CodeConstants.TYPE_FLOAT}, {CodeConstants.TYPE_INT}},                        //		public final static int		opc_f2i = 139;
    {{CodeConstants.TYPE_FLOAT}, {CodeConstants.TYPE_LONG}},                        //		public final static int		opc_f2l = 140;
    {{CodeConstants.TYPE_FLOAT}, {CodeConstants.TYPE_DOUBLE}},                        //		public final static int		opc_f2d = 141;
    {{CodeConstants.TYPE_DOUBLE}, {CodeConstants.TYPE_INT}},                        //		public final static int		opc_d2i = 142;
    {{CodeConstants.TYPE_DOUBLE}, {CodeConstants.TYPE_LONG}},                        //		public final static int		opc_d2l = 143;
    {{CodeConstants.TYPE_DOUBLE}, {CodeConstants.TYPE_FLOAT}},                        //		public final static int		opc_d2f = 144;
    {{CodeConstants.TYPE_INT}, {CodeConstants.TYPE_INT}},                        //		public final static int		opc_i2b = 145;
    {{CodeConstants.TYPE_INT}, {CodeConstants.TYPE_INT}},                        //		public final static int		opc_i2c = 146;
    {{CodeConstants.TYPE_INT}, {CodeConstants.TYPE_INT}},                        //		public final static int		opc_i2s = 147;
    {{CodeConstants.TYPE_LONG, CodeConstants.TYPE_LONG}, {CodeConstants.TYPE_INT}},
    //		public final static int		opc_lcmp = 148;
    {{CodeConstants.TYPE_FLOAT, CodeConstants.TYPE_FLOAT}, {CodeConstants.TYPE_INT}},
    //		public final static int		opc_fcmpl = 149;
    {{CodeConstants.TYPE_FLOAT, CodeConstants.TYPE_FLOAT}, {CodeConstants.TYPE_INT}},
    //		public final static int		opc_fcmpg = 150;
    {{CodeConstants.TYPE_DOUBLE, CodeConstants.TYPE_DOUBLE}, {CodeConstants.TYPE_INT}},
    //		public final static int		opc_dcmpl = 151;
    {{CodeConstants.TYPE_DOUBLE, CodeConstants.TYPE_DOUBLE}, {CodeConstants.TYPE_INT}},
    //		public final static int		opc_dcmpg = 152;
    {{CodeConstants.TYPE_INT}, null},                        //		public final static int		opc_ifeq = 153;
    {{CodeConstants.TYPE_INT}, null},                        //		public final static int		opc_ifne = 154;
    {{CodeConstants.TYPE_INT}, null},                        //		public final static int		opc_iflt = 155;
    {{CodeConstants.TYPE_INT}, null},                        //		public final static int		opc_ifge = 156;
    {{CodeConstants.TYPE_INT}, null},                        //		public final static int		opc_ifgt = 157;
    {{CodeConstants.TYPE_INT}, null},                        //		public final static int		opc_ifle = 158;
    {{CodeConstants.TYPE_INT, CodeConstants.TYPE_INT}, null},                        //		public final static int		opc_if_icmpeq = 159;
    {{CodeConstants.TYPE_INT, CodeConstants.TYPE_INT}, null},                        //		public final static int		opc_if_icmpne = 160;
    {{CodeConstants.TYPE_INT, CodeConstants.TYPE_INT}, null},                        //		public final static int		opc_if_icmplt = 161;
    {{CodeConstants.TYPE_INT, CodeConstants.TYPE_INT}, null},                        //		public final static int		opc_if_icmpge = 162;
    {{CodeConstants.TYPE_INT, CodeConstants.TYPE_INT}, null},                        //		public final static int		opc_if_icmpgt = 163;
    {{CodeConstants.TYPE_INT, CodeConstants.TYPE_INT}, null},                        //		public final static int		opc_if_icmple = 164;
    {{CodeConstants.TYPE_OBJECT, CodeConstants.TYPE_OBJECT}, null},
    //		public final static int		opc_if_acmpeq = 165;
    {{CodeConstants.TYPE_OBJECT, CodeConstants.TYPE_OBJECT}, null},
    //		public final static int		opc_if_acmpne = 166;
    {null, null},                        //		public final static int		opc_goto = 167;
    {null, {CodeConstants.TYPE_ADDRESS}},                        //		public final static int		opc_jsr = 168;
    {null, null},                        //		public final static int		opc_ret = 169;
    {{CodeConstants.TYPE_INT}, null},                        //		public final static int		opc_tableswitch = 170;
    {{CodeConstants.TYPE_INT}, null},                        //		public final static int		opc_lookupswitch = 171;
    {{CodeConstants.TYPE_INT}, null},                        //		public final static int		opc_ireturn = 172;
    {{CodeConstants.TYPE_LONG}, null},                        //		public final static int		opc_lreturn = 173;
    {{CodeConstants.TYPE_FLOAT}, null},                        //		public final static int		opc_freturn = 174;
    {{CodeConstants.TYPE_DOUBLE}, null},                        //		public final static int		opc_dreturn = 175;
    {{CodeConstants.TYPE_OBJECT}, null},                        //		public final static int		opc_areturn = 176;
    {null, null},                        //		public final static int		opc_return = 177;
    null,                        //		public final static int		opc_getstatic = 178;
    null,                        //		public final static int		opc_putstatic = 179;
    null,                        //		public final static int		opc_getfield = 180;
    null,                        //		public final static int		opc_putfield = 181;
    null,                        //		public final static int		opc_invokevirtual = 182;
    null,                        //		public final static int		opc_invokespecial = 183;
    null,                        //		public final static int		opc_invokestatic = 184;
    null,                        //		public final static int		opc_invokeinterface = 185;
    null,                        //		public final static int		opc_xxxunusedxxx = 186;
    null,                        //		public final static int		opc_new = 187;
    null,                        //		public final static int		opc_newarray = 188;
    null,                        //		public final static int		opc_anewarray = 189;
    {{CodeConstants.TYPE_OBJECT}, {CodeConstants.TYPE_INT}},                        //		public final static int		opc_arraylength = 190;
    null,
    //		public final static int		opc_athrow = 191;
    null,
    //		public final static int		opc_checkcast = 192;
    null,
    //		public final static int		opc_instanceof = 193;
    {{CodeConstants.TYPE_OBJECT}, null},                                //		public final static int		opc_monitorenter = 194;
    {{CodeConstants.TYPE_OBJECT}, null},                                //		public final static int		opc_monitorexit = 195;
    null,
    //		public final static int		opc_wide = 196;
    null,
    //		public final static int		opc_multianewarray = 197;
    {{CodeConstants.TYPE_OBJECT}, null},                                //		public final static int		opc_ifnull = 198;
    {{CodeConstants.TYPE_OBJECT}, null},                                //		public final static int		opc_ifnonnull = 199;
    {null, null},                                                                        //		public final static int		opc_goto_w = 200;
    {null, {CodeConstants.TYPE_ADDRESS}},                        //		public final static int		opc_jsr_w = 201;
  };

  private static final int[] arr_type = new int[]{
    CodeConstants.TYPE_BOOLEAN,
    CodeConstants.TYPE_CHAR,
    CodeConstants.TYPE_FLOAT,
    CodeConstants.TYPE_DOUBLE,
    CodeConstants.TYPE_BYTE,
    CodeConstants.TYPE_SHORT,
    CodeConstants.TYPE_INT,
    CodeConstants.TYPE_LONG
  };


  // Sonderbehandlung
  //	null,			//		public final static int		opc_aconst_null = 1;
  //	null, 			//		public final static int		opc_ldc = 18;
  //	null, 			//		public final static int		opc_ldc_w = 19;
  //	null, 			//		public final static int		opc_ldc2_w = 20;
  //	null,			//		public final static int		opc_aload = 25;
  //	null,			//		public final static int		opc_aaload = 50;
  //	null,			//		public final static int		opc_astore = 58;
  //	null, 			//		public final static int		opc_dup = 89;
  //	null, 			//		public final static int		opc_dup_x1 = 90;
  //	null, 			//		public final static int		opc_dup_x2 = 91;
  //	null, 			//		public final static int		opc_dup2 = 92;
  //	null, 			//		public final static int		opc_dup2_x1 = 93;
  //	null, 			//		public final static int		opc_dup2_x2 = 94;
  //	null, 			//		public final static int		opc_swap = 95;
  //	null, 			//		public final static int		opc_getstatic = 178;
  //	null, 			//		public final static int		opc_putstatic = 179;
  //	null, 			//		public final static int		opc_getfield = 180;
  //	null, 			//		public final static int		opc_putfield = 181;
  //	null, 			//		public final static int		opc_invokevirtual = 182;
  //	null, 			//		public final static int		opc_invokespecial = 183;
  //	null, 			//		public final static int		opc_invokestatic = 184;
  //	null, 			//		public final static int		opc_invokeinterface = 185;
  //	null,			//		public final static int		opc_new = 187;
  //	null,			//		public final static int		opc_newarray = 188;
  //	null,			//		public final static int		opc_anewarray = 189;
  //	null, 			//		public final static int		opc_athrow = 191;
  //	null,			//		public final static int		opc_checkcast = 192;
  //	null,			//		public final static int		opc_instanceof = 193;
  //	null, 			//		public final static int		opc_multianewarray = 197;


  public static void stepTypes(DataPoint data, Instruction instr, ConstantPool pool) {
    ListStack<VarType> stack = data.getStack();
    int[][] arr = stack_impact[instr.opcode];

    if (arr != null) {
      // simple types only

      int[] read = arr[0];
      int[] write = arr[1];

      if (read != null) {
        int depth = 0;
        for (int type : read) {
          depth++;
          if (type == CodeConstants.TYPE_LONG ||
              type == CodeConstants.TYPE_DOUBLE) {
            depth++;
          }
        }

        stack.removeMultiple(depth);
      }

      if (write != null) {
        for (int type : write) {
          stack.push(new VarType(type));
          if (type == CodeConstants.TYPE_LONG ||
              type == CodeConstants.TYPE_DOUBLE) {
            stack.push(new VarType(CodeConstants.TYPE_GROUP2EMPTY));
          }
        }
      }
    }
    else {
      // Sonderbehandlung
      processSpecialInstructions(data, instr, pool);
    }
  }

  private static void processSpecialInstructions(DataPoint data, Instruction instr, ConstantPool pool) {

    VarType var1;
    PrimitiveConstant cn;
    LinkConstant ck;

    ListStack<VarType> stack = data.getStack();

    switch (instr.opcode) {
      case CodeConstants.opc_aconst_null:
        stack.push(new VarType(CodeConstants.TYPE_NULL, 0, null));
        break;
      case CodeConstants.opc_ldc:
      case CodeConstants.opc_ldc_w:
      case CodeConstants.opc_ldc2_w:
        PooledConstant constant = pool.getConstant(instr.operand(0));
        switch (constant.type) {
          case CodeConstants.CONSTANT_Integer:
            stack.push(new VarType(CodeConstants.TYPE_INT));
            break;
          case CodeConstants.CONSTANT_Float:
            stack.push(new VarType(CodeConstants.TYPE_FLOAT));
            break;
          case CodeConstants.CONSTANT_Long:
            stack.push(new VarType(CodeConstants.TYPE_LONG));
            stack.push(new VarType(CodeConstants.TYPE_GROUP2EMPTY));
            break;
          case CodeConstants.CONSTANT_Double:
            stack.push(new VarType(CodeConstants.TYPE_DOUBLE));
            stack.push(new VarType(CodeConstants.TYPE_GROUP2EMPTY));
            break;
          case CodeConstants.CONSTANT_String:
            stack.push(new VarType(CodeConstants.TYPE_OBJECT, 0, "java/lang/String"));
            break;
          case CodeConstants.CONSTANT_Class:
            stack.push(new VarType(CodeConstants.TYPE_OBJECT, 0, "java/lang/Class"));
            break;
          case CodeConstants.CONSTANT_MethodHandle:
            stack.push(new VarType(((LinkConstant)constant).descriptor));
            break;
          case CodeConstants.CONSTANT_Dynamic:
            ck = pool.getLinkConstant(instr.operand(0));
            FieldDescriptor fd = FieldDescriptor.parseDescriptor(ck.descriptor);
            if (fd.type.type != CodeConstants.TYPE_VOID) {
              stack.push(fd.type);
              if (fd.type.stackSize == 2) {
                stack.push(new VarType(CodeConstants.TYPE_GROUP2EMPTY));
              }
            }
            break;
        }
        break;
      case CodeConstants.opc_aload:
        var1 = data.getVariable(instr.operand(0));
        if (var1 != null) {
          stack.push(var1);
        }
        else {
          stack.push(new VarType(CodeConstants.TYPE_OBJECT, 0, null));
        }
        break;
      case CodeConstants.opc_aaload:
        var1 = stack.pop(2);
        stack.push(new VarType(var1.type, var1.arrayDim - 1, var1.value));
        break;
      case CodeConstants.opc_astore:
        data.setVariable(instr.operand(0), stack.pop());
        break;
      case CodeConstants.opc_dup:
      case CodeConstants.opc_dup_x1:
      case CodeConstants.opc_dup_x2:
        int depth1 = 88 - instr.opcode;
        stack.insertByOffset(depth1, stack.getByOffset(-1).copy());
        break;
      case CodeConstants.opc_dup2:
      case CodeConstants.opc_dup2_x1:
      case CodeConstants.opc_dup2_x2:
        int depth2 = 90 - instr.opcode;
        stack.insertByOffset(depth2, stack.getByOffset(-2).copy());
        stack.insertByOffset(depth2, stack.getByOffset(-1).copy());
        break;
      case CodeConstants.opc_swap:
        var1 = stack.pop();
        stack.insertByOffset(-1, var1);
        break;
      case CodeConstants.opc_getfield:
        stack.pop();
      case CodeConstants.opc_getstatic:
        ck = pool.getLinkConstant(instr.operand(0));
        var1 = new VarType(ck.descriptor);
        stack.push(var1);
        if (var1.stackSize == 2) {
          stack.push(new VarType(CodeConstants.TYPE_GROUP2EMPTY));
        }
        break;
      case CodeConstants.opc_putfield:
        stack.pop();
      case CodeConstants.opc_putstatic:
        ck = pool.getLinkConstant(instr.operand(0));
        var1 = new VarType(ck.descriptor);
        stack.pop(var1.stackSize);
        break;
      case CodeConstants.opc_invokevirtual:
      case CodeConstants.opc_invokespecial:
      case CodeConstants.opc_invokeinterface:
        stack.pop();
      case CodeConstants.opc_invokestatic:
      case CodeConstants.opc_invokedynamic:
        if (instr.opcode != CodeConstants.opc_invokedynamic || instr.bytecodeVersion.hasInvokeDynamic()) {
          ck = pool.getLinkConstant(instr.operand(0));
          MethodDescriptor md = MethodDescriptor.parseDescriptor(ck.descriptor);
          for (int i = 0; i < md.params.length; i++) {
            stack.pop(md.params[i].stackSize);
          }
          if (md.ret.type != CodeConstants.TYPE_VOID) {
            stack.push(md.ret);
            if (md.ret.stackSize == 2) {
              stack.push(new VarType(CodeConstants.TYPE_GROUP2EMPTY));
            }
          }
        }
        break;
      case CodeConstants.opc_new:
        cn = pool.getPrimitiveConstant(instr.operand(0));
        stack.push(new VarType(CodeConstants.TYPE_OBJECT, 0, cn.getString()));
        break;
      case CodeConstants.opc_newarray:
        stack.pop();
        stack.push(new VarType(arr_type[instr.operand(0) - 4], 1).resizeArrayDim(1));
        break;
      case CodeConstants.opc_athrow:
        var1 = stack.pop();
        stack.clear();
        stack.push(var1);
        break;
      case CodeConstants.opc_checkcast:
      case CodeConstants.opc_instanceof:
        stack.pop();
        cn = pool.getPrimitiveConstant(instr.operand(0));
        stack.push(new VarType(CodeConstants.TYPE_OBJECT, 0, cn.getString()));
        break;
      case CodeConstants.opc_anewarray:
      case CodeConstants.opc_multianewarray:
        int dimensions = (instr.opcode == CodeConstants.opc_anewarray) ? 1 : instr.operand(1);
        stack.pop(dimensions);
        cn = pool.getPrimitiveConstant(instr.operand(0));
        if (cn.isArray) {
          var1 = new VarType(CodeConstants.TYPE_OBJECT, 0, cn.getString());
          var1 = var1.resizeArrayDim(var1.arrayDim + dimensions);
          stack.push(var1);
        }
        else {
          stack.push(new VarType(CodeConstants.TYPE_OBJECT, dimensions, cn.getString()));
        }
    }
  }
}
