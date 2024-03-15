// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.java.decompiler.modules.decompiler.exps;

import org.jetbrains.java.decompiler.code.CodeConstants;
import org.jetbrains.java.decompiler.main.DecompilerContext;
import org.jetbrains.java.decompiler.main.extern.IFernflowerPreferences;
import org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor;
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
import java.util.Map.Entry;
import java.util.function.Function;

public class ConstExprent extends Exprent {
  private static final Map<Integer, String> CHAR_ESCAPES = new HashMap<>();
  private static final Map<Double, Function<BitSet, TextBuffer>> UNINLINED_DOUBLES = new HashMap<>();
  private static final Map<Float, Function<BitSet, TextBuffer>> UNINLINED_FLOATS = new HashMap<>();
  private static final Set<Object> NO_PAREN_VALUES = new HashSet<>();

  static {
    CHAR_ESCAPES.put(0x8, "\\b");   /* \u0008: backspace BS */
    CHAR_ESCAPES.put(0x9, "\\t");   /* \u0009: horizontal tab HT */
    CHAR_ESCAPES.put(0xA, "\\n");   /* \u000a: linefeed LF */
    CHAR_ESCAPES.put(0xC, "\\f");   /* \u000c: form feed FF */
    CHAR_ESCAPES.put(0xD, "\\r");   /* \u000d: carriage return CR */
    //CHAR_ESCAPES.put(0x22, "\\\""); /* \u0022: double quote " */
    CHAR_ESCAPES.put(0x27, "\\'"); /* \u0027: single quote ' */
    CHAR_ESCAPES.put(0x5C, "\\\\"); /* \u005c: backslash \ */

    // Store float and double values that need to get uninlined.
    // These values are better represented by their original values, to improve code readability.
    // Some values need multiple float versions, as they can vary slightly due to where the cast to float is placed.
    // This patch is based on work in ForgeFlower submitted by Pokechu22.

    // Positive and negative e
    UNINLINED_DOUBLES.put(Math.E, bytecode -> getDouble(bytecode, "E", "java/lang/Math"));
    UNINLINED_DOUBLES.put(-Math.E, bytecode -> getDouble(bytecode, "E", "java/lang/Math").prepend("-"));

    // Positive and negative pi
    UNINLINED_DOUBLES.put(Math.PI, ConstExprent::getPiDouble);
    UNINLINED_DOUBLES.put(-Math.PI, bytecode -> getPiDouble(bytecode).prepend("-"));

    NO_PAREN_VALUES.addAll(UNINLINED_DOUBLES.keySet());
    UNINLINED_DOUBLES.keySet().forEach(d -> NO_PAREN_VALUES.add(d.floatValue()));

    // Positive and negative pi divisors
    for (int i = 2; i <= 20; i++) {
      int finalI = i;
      UNINLINED_DOUBLES.put(Math.PI / i, bytecode -> getPiDouble(bytecode).append(" / "+ finalI));
      UNINLINED_DOUBLES.put(-Math.PI / i, bytecode -> getPiDouble(bytecode).append(" / "+ finalI).prepend("-"));
    }

    // Positive and negative pi multipliers
    for (int i = 2; i <= 20; i++) {
      int finalI = i;
      UNINLINED_DOUBLES.put(Math.PI * i, bytecode -> getPiDouble(bytecode).append(" * "+ finalI));
      UNINLINED_DOUBLES.put(-Math.PI * i, bytecode -> getPiDouble(bytecode).append(" * "+ finalI).prepend("-"));
    }

    // Extra pi values on the unit circle
    for (double numerator = 2; numerator < 13; numerator++) {
      for (double denominator = 2; denominator < 13; denominator++) {
        double gcd = gcd(numerator, denominator);
        if (gcd == 1) {
          double finalNumerator = numerator;
          double finalDenominator = denominator;
          UNINLINED_DOUBLES.put(Math.PI * (numerator / denominator), bytecode -> getPiDouble(bytecode).append(" * " + finalNumerator + " / " + finalDenominator));
          UNINLINED_DOUBLES.put(-Math.PI * (numerator / denominator), bytecode -> getPiDouble(bytecode).append(" * " + finalNumerator + " / " + finalDenominator).prepend("-"));

          if ((float) Math.PI * (float) numerator / (float) denominator != (float) (Math.PI * numerator / denominator)) {
            UNINLINED_FLOATS.put((float) Math.PI * ((float) numerator / (float) denominator), bytecode -> getPiDouble(bytecode).append(" * " + finalNumerator + "F / " + finalDenominator + "F").prepend("(float) "));
            UNINLINED_FLOATS.put((float) -Math.PI * ((float) numerator / (float) denominator), bytecode -> getPiDouble(bytecode).append(" * " + finalNumerator + "F / " + finalDenominator + "F").prepend("(float) -"));
          }
        }
      }
    }

    // Positive and negative 180 / pi
    UNINLINED_DOUBLES.put(180.0 / Math.PI, bytecode -> getPiDouble(bytecode).prepend("180.0 / "));
    UNINLINED_DOUBLES.put(-180.0 / Math.PI, bytecode -> getPiDouble(bytecode).prepend("-180.0 / "));

    UNINLINED_FLOATS.put((float)(180.0F / Math.PI), bytecode -> getPiDouble(bytecode).prepend("180.0F / "));
    UNINLINED_FLOATS.put((float)(-180.0F / Math.PI), bytecode -> getPiDouble(bytecode).prepend("-180.0F / "));

    UNINLINED_FLOATS.put((float)(180.0F / (float)Math.PI), bytecode -> getPiDouble(bytecode).prepend("180.0F / (float)"));
    UNINLINED_FLOATS.put((float)(-180.0F / (float)Math.PI), bytecode -> getPiDouble(bytecode).prepend("-180.0F / (float)"));

    // Positive and negative pi / 180
    UNINLINED_DOUBLES.put(Math.PI / 180.0, bytecode -> getPiDouble(bytecode).append(" / 180.0"));
    UNINLINED_DOUBLES.put(-Math.PI / 180.0, bytecode -> getPiDouble(bytecode).append(" / 180.0").prepend("-"));

    UNINLINED_FLOATS.put((float)(Math.PI / 180.0), bytecode -> getPiDouble(bytecode).append(" / 180.0").prepend("(float) "));
    UNINLINED_FLOATS.put((float)(-Math.PI / 180.0), bytecode -> getPiDouble(bytecode).append(" / 180.0").prepend("-").prepend("(float) "));

    UNINLINED_DOUBLES.forEach((key, valueFunction) -> {
      UNINLINED_FLOATS.put(key.floatValue(), bytecode -> {
        TextBuffer doubleValue = valueFunction.apply(bytecode);
        if (doubleValue.count(" ", 0) > 0) { // As long as all uninlined double values with more than one expression have a space in it, this'll work.
          doubleValue.encloseWithParens();
        }
        return doubleValue.prepend("(float) ");
      });
    });

    // Double and Float constants
    UNINLINED_DOUBLES.put(Double.POSITIVE_INFINITY, bytecode -> getDouble(bytecode, "POSITIVE_INFINITY", "java/lang/Double"));
    UNINLINED_DOUBLES.put(Double.NEGATIVE_INFINITY, bytecode -> getDouble(bytecode, "NEGATIVE_INFINITY", "java/lang/Double"));
    UNINLINED_DOUBLES.put(Double.MAX_VALUE, bytecode -> getDouble(bytecode, "MAX_VALUE", "java/lang/Double"));
    UNINLINED_DOUBLES.put(Double.MIN_NORMAL, bytecode -> getDouble(bytecode, "MIN_NORMAL", "java/lang/Double"));
    UNINLINED_DOUBLES.put(Double.MIN_VALUE, bytecode -> getDouble(bytecode, "MIN_VALUE", "java/lang/Double"));
    UNINLINED_DOUBLES.put(-Double.MAX_VALUE, bytecode -> getDouble(bytecode, "MAX_VALUE", "java/lang/Double").prepend("-"));
    UNINLINED_DOUBLES.put(-Double.MIN_NORMAL, bytecode -> getDouble(bytecode, "MIN_NORMAL", "java/lang/Double").prepend("-"));
    UNINLINED_DOUBLES.put(-Double.MIN_VALUE, bytecode -> getDouble(bytecode, "MIN_VALUE", "java/lang/Double").prepend("-"));

    UNINLINED_FLOATS.put(Float.POSITIVE_INFINITY, bytecode -> getFloat(bytecode, "POSITIVE_INFINITY", "java/lang/Float"));
    UNINLINED_FLOATS.put(Float.NEGATIVE_INFINITY, bytecode -> getFloat(bytecode, "NEGATIVE_INFINITY", "java/lang/Float"));
    UNINLINED_FLOATS.put(Float.MAX_VALUE, bytecode -> getFloat(bytecode, "MAX_VALUE", "java/lang/Float"));
    UNINLINED_FLOATS.put(Float.MIN_NORMAL, bytecode -> getFloat(bytecode, "MIN_NORMAL", "java/lang/Float"));
    UNINLINED_FLOATS.put(Float.MIN_VALUE, bytecode -> getFloat(bytecode, "MIN_VALUE", "java/lang/Float"));
    UNINLINED_FLOATS.put(-Float.MAX_VALUE, bytecode -> getFloat(bytecode, "MAX_VALUE", "java/lang/Float").prepend("-"));
    UNINLINED_FLOATS.put(-Float.MIN_NORMAL, bytecode -> getFloat(bytecode, "MIN_NORMAL", "java/lang/Float").prepend("-"));
    UNINLINED_FLOATS.put(-Float.MIN_VALUE, bytecode -> getFloat(bytecode, "MIN_VALUE", "java/lang/Float").prepend("-"));
  }

  private VarType constType;
  private final Object value;
  private final boolean boolPermitted;
  private boolean wasCondy = false;

  public ConstExprent(int val, boolean boolPermitted, BitSet bytecodeOffsets) {
    this(guessType(val, boolPermitted), val, boolPermitted, bytecodeOffsets);
  }

  public ConstExprent(VarType constType, Object value, BitSet bytecodeOffsets) {
    this(constType, value, false, bytecodeOffsets);
  }

  public ConstExprent(VarType constType, Object value, BitSet bytecodeOffsets, boolean wasCondy) {
    this(constType, value, false, bytecodeOffsets);
    this.wasCondy = wasCondy;
  }

  private ConstExprent(VarType constType, Object value, boolean boolPermitted, BitSet bytecodeOffsets) {
    super(Type.CONST);
    this.constType = constType;
    this.value = value;
    this.boolPermitted = boolPermitted;
    addBytecodeOffsets(bytecodeOffsets);

    if (constType.equals(VarType.VARTYPE_CLASS) && value != null) {
      String stringVal = value.toString();
      List<VarType> args = Collections.singletonList(new VarType(stringVal, !stringVal.startsWith("[")));
      this.constType = new GenericType(constType.type, constType.arrayDim, constType.value, null, args, GenericType.WILDCARD_NO);
    }
  }

  private static VarType guessType(int val, boolean boolPermitted) {
    if (boolPermitted) {
      VarType constType = VarType.VARTYPE_BOOLEAN;
      if (val != 0 && val != 1) {
        constType = constType.copy(true);
      }
      return constType;
    }
    else if (0 <= val && val <= 127) {
      return VarType.VARTYPE_BYTECHAR;
    }
    else if (-128 <= val && val <= 127) {
      return VarType.VARTYPE_BYTE;
    }
    else if (0 <= val && val <= 32767) {
      return VarType.VARTYPE_SHORTCHAR;
    }
    else if (-32768 <= val && val <= 32767) {
      return VarType.VARTYPE_SHORT;
    }
    else if (0 <= val && val <= 0xFFFF) {
      return VarType.VARTYPE_CHAR;
    }
    else {
      return VarType.VARTYPE_INT;
    }
  }

  private static double gcd(double a, double b) {
    return b == 0 ? a : gcd(b, a%b);
  }

  @Override
  public Exprent copy() {
    return new ConstExprent(constType, value, bytecode, wasCondy);
  }

  @Override
  public VarType getExprType() {
    return constType;
  }

  @Override
  public int getExprentUse() {
    return Exprent.MULTIPLE_USES | Exprent.SIDE_EFFECTS_FREE;
  }

  @Override
  public List<Exprent> getAllExprents(List<Exprent> list) {
    return list;
  }

  @Override
  public TextBuffer toJava(int indent) {
    boolean literal = DecompilerContext.getOption(IFernflowerPreferences.LITERALS_AS_IS);
    boolean ascii = DecompilerContext.getOption(IFernflowerPreferences.ASCII_STRING_CHARACTERS);

    TextBuffer buf = new TextBuffer();
    buf.addBytecodeMapping(bytecode);

    if (wasCondy) {
      buf.append("/* $VF: constant dynamic */ ");
    }

    if (constType.type != CodeConstants.TYPE_NULL && value == null) {
      return buf.append(ExprProcessor.getCastTypeName(constType));
    }

    VarType unboxed = VarType.UNBOXING_TYPES.getOrDefault(constType, constType);

    switch (unboxed.type) {
      case CodeConstants.TYPE_BOOLEAN:
        return buf.append(Boolean.toString((Integer)value != 0));

      case CodeConstants.TYPE_CHAR:
        Integer val = (Integer)value;
        String ret = CHAR_ESCAPES.get(val);
        if (ret == null) {
          char c = (char)val.intValue();
          if (isPrintableAscii(c) || !ascii && TextUtil.isPrintableUnicode(c)) {
            ret = String.valueOf(c);
          }
          else {
            ret = TextUtil.charToUnicodeLiteral(c);
          }
        }
        return buf.append(ret).enclose("'", "'");

      case CodeConstants.TYPE_BYTE:
      case CodeConstants.TYPE_BYTECHAR:
      case CodeConstants.TYPE_SHORT:
      case CodeConstants.TYPE_SHORTCHAR:
      case CodeConstants.TYPE_INT:
        int intVal = (Integer)value;
        if (!literal) {
          if (intVal == Integer.MAX_VALUE) {
            return buf.append(new FieldExprent("MAX_VALUE", "java/lang/Integer", true, null, FieldDescriptor.INTEGER_DESCRIPTOR, bytecode).toJava(0));
          }
          else if (intVal == Integer.MIN_VALUE) {
            return buf.append(new FieldExprent("MIN_VALUE", "java/lang/Integer", true, null, FieldDescriptor.INTEGER_DESCRIPTOR, bytecode).toJava(0));
          }
        }
        return buf.append(value.toString());

      case CodeConstants.TYPE_LONG:

        long longVal = (Long)value;

        if (!literal) {
          if (longVal == Long.MAX_VALUE) {
            return buf.append(new FieldExprent("MAX_VALUE", "java/lang/Long", true, null, FieldDescriptor.LONG_DESCRIPTOR, bytecode).toJava(0));
          }
          else if (longVal == Long.MIN_VALUE) {
            return buf.append(new FieldExprent("MIN_VALUE", "java/lang/Long", true, null, FieldDescriptor.LONG_DESCRIPTOR, bytecode).toJava(0));
          }
        }
        return buf.append(value.toString()).append('L');

      case CodeConstants.TYPE_FLOAT:
        float floatVal = (Float)value;
        if (!literal) {
          if (Float.isNaN(floatVal)) {
            return buf.append(new FieldExprent("NaN", "java/lang/Float", true, null, FieldDescriptor.FLOAT_DESCRIPTOR, bytecode).toJava(0));
          }
          else if (UNINLINED_FLOATS.containsKey(floatVal)) {
            return buf.append(UNINLINED_FLOATS.get(floatVal).apply(bytecode));
          }
        }
        else {
          // Check for special values that can't be used directly in code
          // (and we can't replace with the constant due to the user requesting not to)
          if (Float.isNaN(floatVal)) {
            return buf.append("0.0F / 0.0F");
          }
          else if (floatVal == Float.POSITIVE_INFINITY) {
            return buf.append("1.0F / 0.0F");
          }
          else if (floatVal == Float.NEGATIVE_INFINITY) {
            return buf.append("-1.0F / 0.0F");
          }
        }
        return buf.append(trimFloat(Float.toString(floatVal), floatVal)).append('F');

      case CodeConstants.TYPE_DOUBLE:
        double doubleVal = (Double)value;
        if (!literal) {
          if (Double.isNaN(doubleVal)) {
            return buf.append(new FieldExprent("NaN", "java/lang/Double", true, null, FieldDescriptor.DOUBLE_DESCRIPTOR, bytecode).toJava(0));
          }
          else if (UNINLINED_DOUBLES.containsKey(doubleVal)) {
            return buf.append(UNINLINED_DOUBLES.get(doubleVal).apply(bytecode));
          }

          // Try to convert the double representation of the value to the float representation, to output the cleanest version of the value.
          // This patch is based on work in ForgeFlower submitted by Pokechu22.
          float floatRepresentation = (float) doubleVal;
          if (floatRepresentation == doubleVal) {
            if (trimFloat(Float.toString(floatRepresentation), floatRepresentation).length() < trimDouble(Double.toString(doubleVal), doubleVal).length()) {
              // Check the uninlined values to see if we have one of those
              if (UNINLINED_FLOATS.containsKey(floatRepresentation)) {
                return buf.append(UNINLINED_FLOATS.get(floatRepresentation).apply(bytecode));
              } else {
                // Return the standard representation if the value is not able to be uninlined
                return buf.append(trimFloat(Float.toString(floatRepresentation), floatRepresentation)).append("F");
              }
            }
          }
        }
        else if (Double.isNaN(doubleVal)) {
          return buf.append("0.0 / 0.0");
        }
        else if (doubleVal == Double.POSITIVE_INFINITY) {
          return buf.append("1.0 / 0.0");
        }
        else if (doubleVal == Double.NEGATIVE_INFINITY) {
          return buf.append("-1.0 / 0.0");
        }
        return buf.append(trimDouble(Double.toString(doubleVal), doubleVal));

      case CodeConstants.TYPE_NULL:
        return buf.append("null");

      case CodeConstants.TYPE_OBJECT:
        if (constType.equals(VarType.VARTYPE_STRING)) {
          return buf.append(convertStringToJava(value.toString(), ascii)).enclose("\"", "\"");
        }
        else if (constType.equals(VarType.VARTYPE_CLASS)) {
          String stringVal = value.toString();
          VarType type = new VarType(stringVal, !stringVal.startsWith("["));
          return buf.append(ExprProcessor.getCastTypeName(type)).append(".class");
        }
    }

    // prevent gc without discarding
    buf.convertToStringAndAllowDataDiscard();
    throw new RuntimeException("invalid constant type: " + constType);
  }

  @Override
  public int getPrecedence() {
    if (value == null || DecompilerContext.getOption(IFernflowerPreferences.LITERALS_AS_IS)) {
      return super.getPrecedence();
    }

    VarType unboxed = VarType.UNBOXING_TYPES.getOrDefault(constType, constType);

    // FIXME: this entire system is terrible, and pi constants need to be fixed to not create field exprents

    switch (unboxed.type) {
      case CodeConstants.TYPE_FLOAT:
        float floatVal = (Float)value;

        if (UNINLINED_FLOATS.containsKey(floatVal) && !NO_PAREN_VALUES.contains(floatVal) && UNINLINED_FLOATS.get(floatVal).apply(bytecode).countChars('(') < 2) {
          return 4;
        }
        break;
      case CodeConstants.TYPE_DOUBLE:
        double doubleVal = (Double)value;

        if (UNINLINED_DOUBLES.containsKey(doubleVal) && !NO_PAREN_VALUES.contains(doubleVal) && UNINLINED_DOUBLES.get(doubleVal).apply(bytecode).countChars('(') < 2) {
          return 4;
        }
        break;
    }

    return super.getPrecedence();
  }

  private static TextBuffer getPiDouble(BitSet bytecode) {
    return getDouble(bytecode, "PI", "java/lang/Math");
  }

  private static TextBuffer getDouble(BitSet bytecode, String name, String className) {
    return new FieldExprent(name, className, true, null, FieldDescriptor.DOUBLE_DESCRIPTOR, bytecode).toJava(0);
  }

  private static TextBuffer getFloat(BitSet bytecode, String name, String className) {
    return new FieldExprent(name, className, true, null, FieldDescriptor.FLOAT_DESCRIPTOR, bytecode).toJava(0);
  }

  // Different JVM implementations/version display Floats and Doubles with different number of trailing zeros.
  // This trims them all down to only the necessary amount.
  private static String trimFloat(String value, float start) {
    // Includes NaN and simple numbers
    if (value.length() <= 3) {
      return value;
    }

    String exp = "";
    int eIdx = value.indexOf('E');
    if (eIdx != -1) {
      exp = value.substring(eIdx);
      value = value.substring(0, eIdx);
    }

    // Cut off digits that don't affect the value
    String temp = value;
    int dotIdx = value.indexOf('.');
    do {
      value = temp;
      temp = value.substring(0, value.length() - 1);
    } while (!temp.isEmpty() && !"-".equals(temp) && Float.parseFloat(temp + exp) == start);

    if (dotIdx != -1 && value.indexOf('.') == -1) {
      value += ".0";
    } else if (dotIdx != -1) {
      String integer = value.substring(0, dotIdx);
      String decimal = value.substring(dotIdx + 1);

      String rounded = (Integer.parseInt(integer) + 1) + ".0" + exp;
      if (Float.parseFloat(rounded) == start)
        return rounded;

      long decimalVal = 1;
      for (int i = 0; i < decimal.length() - 1; i++) {
        decimalVal = (decimalVal - 1) * 10 + decimal.charAt(i) - '0' + 1;
        rounded = integer + '.' + decimalVal + exp;
        if (Float.parseFloat(rounded) == start)
          return rounded;
      }
    }

    return value + exp;
  }

  private static String trimDouble(String value, double start) {
    // Includes NaN and simple numbers
    if (value.length() <= 3) {
      return value;
    }

    String exp = "";
    int eIdx = value.indexOf('E');
    if (eIdx != -1) {
      exp = value.substring(eIdx);
      value = value.substring(0, eIdx);
    }

    // Cut off digits that don't affect the value
    String temp = value;
    int dotIdx = value.indexOf('.');
    do {
      value = temp;
      temp = value.substring(0, value.length() - 1);
    } while (!temp.isEmpty() && !"-".equals(temp) && Double.parseDouble(temp) == start);

    if (dotIdx != -1 && value.indexOf('.') == -1) {
      value += ".0";
    } else if (dotIdx != -1) {
      String integer = value.substring(0, dotIdx);
      String decimal = value.substring(dotIdx + 1);

      String rounded = (Long.parseLong(integer) + 1) + ".0" + exp;
      if (Double.parseDouble(rounded) == start)
        return rounded;

      long decimalVal = 1;
      for (int i = 0; i < decimal.length() - 1; i++) {
        decimalVal = (decimalVal - 1) * 10 + decimal.charAt(i) - '0' + 1;
        rounded = integer + '.' + decimalVal + exp;
        if (Double.parseDouble(rounded) == start)
          return rounded;
      }
    }

    return value + exp;
  }

  public boolean isNull() {
    return CodeConstants.TYPE_NULL == constType.type;
  }

  public static String convertStringToJava(String value, boolean ascii) {
    char[] arr = value.toCharArray();
    StringBuilder buffer = new StringBuilder(arr.length);

    for (char c : arr) {
      switch (c) {
        case '\\': //  u005c: backslash \
          buffer.append("\\\\");
          break;
        case 0x8: // "\\\\b");  //  u0008: backspace BS
          buffer.append("\\b");
          break;
        case 0x9: //"\\\\t");  //  u0009: horizontal tab HT
          buffer.append("\\t");
          break;
        case 0xA: //"\\\\n");  //  u000a: linefeed LF
          buffer.append("\\n");
          break;
        case 0xC: //"\\\\f");  //  u000c: form feed FF
          buffer.append("\\f");
          break;
        case 0xD: //"\\\\r");  //  u000d: carriage return CR
          buffer.append("\\r");
          break;
        case 0x22: //"\\\\\""); // u0022: double quote "
          buffer.append("\\\"");
          break;
        //case 0x27: //"\\\\'");  // u0027: single quote '
        //  buffer.append("\\\'");
        //  break;
        default:
          if (isPrintableAscii(c) || !ascii && TextUtil.isPrintableUnicode(c)) {
            buffer.append(c);
          }
          else {
            buffer.append(TextUtil.charToUnicodeLiteral(c));
          }
      }
    }

    return buffer.toString();
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) return true;
    if (!(o instanceof ConstExprent)) return false;

    ConstExprent cn = (ConstExprent)o;
    return InterpreterUtil.equalObjects(constType, cn.getConstType()) &&
           InterpreterUtil.equalObjects(value, cn.getValue());
  }

  @Override
  public int hashCode() {
    int result = constType != null ? constType.hashCode() : 0;
    result = 31 * result + (value != null ? value.hashCode() : 0);
    return result;
  }

  public boolean hasBooleanValue() {
    switch (constType.type) {
      case CodeConstants.TYPE_BOOLEAN:
      case CodeConstants.TYPE_CHAR:
      case CodeConstants.TYPE_BYTE:
      case CodeConstants.TYPE_BYTECHAR:
      case CodeConstants.TYPE_SHORT:
      case CodeConstants.TYPE_SHORTCHAR:
      case CodeConstants.TYPE_INT:
        int value = (Integer)this.value;
        return value == 0 || (DecompilerContext.getOption(IFernflowerPreferences.BOOLEAN_TRUE_ONE) && value == 1);
    }

    return false;
  }

  public boolean hasValueOne() {
    switch (constType.type) {
      case CodeConstants.TYPE_BOOLEAN:
      case CodeConstants.TYPE_CHAR:
      case CodeConstants.TYPE_BYTE:
      case CodeConstants.TYPE_BYTECHAR:
      case CodeConstants.TYPE_SHORT:
      case CodeConstants.TYPE_SHORTCHAR:
      case CodeConstants.TYPE_INT:
      case CodeConstants.TYPE_LONG:
      case CodeConstants.TYPE_DOUBLE:
      case CodeConstants.TYPE_FLOAT:
        return ((Number)value).intValue() == 1;
    }

    return false;
  }

  public static ConstExprent getZeroConstant(int type) {
    switch (type) {
      case CodeConstants.TYPE_INT:
        return new ConstExprent(VarType.VARTYPE_INT, 0, null);
      case CodeConstants.TYPE_LONG:
        return new ConstExprent(VarType.VARTYPE_LONG, 0L, null);
      case CodeConstants.TYPE_DOUBLE:
        return new ConstExprent(VarType.VARTYPE_DOUBLE, 0d, null);
      case CodeConstants.TYPE_FLOAT:
        return new ConstExprent(VarType.VARTYPE_FLOAT, 0f, null);
    }

    throw new RuntimeException("Invalid argument: " + type);
  }

  public VarType getConstType() {
    return constType;
  }

  public void setConstType(VarType constType) {
    this.constType = constType;
  }

  public void adjustConstType(VarType expectedType) {
    // BYTECHAR and SHORTCHAR => CHAR in the CHAR context
    if ((expectedType.equals(VarType.VARTYPE_CHAR) || expectedType.equals(VarType.VARTYPE_CHARACTER)) &&
            (constType.equals(VarType.VARTYPE_BYTECHAR) || constType.equals(VarType.VARTYPE_SHORTCHAR))) {
      int intValue = getIntValue();
      if (isPrintableAscii(intValue) || CHAR_ESCAPES.containsKey(intValue)) {
        setConstType(VarType.VARTYPE_CHAR);
      }
    }
    // BYTE, BYTECHAR, SHORTCHAR, SHORT, CHAR => INT in the INT context
    else if ((expectedType.equals(VarType.VARTYPE_INT) || expectedType.equals(VarType.VARTYPE_INTEGER)) &&
            constType.typeFamily == CodeConstants.TYPE_FAMILY_INTEGER) {
      setConstType(VarType.VARTYPE_INT);
    }
  }

  private static boolean isPrintableAscii(int c) {
    return c >= 32 && c < 127;
  }

  public Object getValue() {
    return value;
  }

  public int getIntValue() {
    return (Integer)value;
  }

  public boolean isBoolPermitted() {
    return boolPermitted;
  }

  @Override
  public void getBytecodeRange(BitSet values) {
    measureBytecode(values);
  }

  public ConstExprent setWasCondy(boolean wasCondy) {
    this.wasCondy = wasCondy;
    return this;
  }

  @Override
  public String toString() {
    return "const(" + toJava(0).convertToStringAndAllowDataDiscard() + ")";
  }

  // *****************************************************************************
  // IMatchable implementation
  // *****************************************************************************

  @Override
  public boolean match(MatchNode matchNode, MatchEngine engine) {
    if (!super.match(matchNode, engine)) {
      return false;
    }

    for (Entry<MatchProperties, RuleValue> rule : matchNode.getRules().entrySet()) {
      RuleValue value = rule.getValue();
      MatchProperties key = rule.getKey();

      if (key == MatchProperties.EXPRENT_CONSTTYPE) {
        if (!value.value.equals(this.constType)) {
          return false;
        }
      }
      else if (key == MatchProperties.EXPRENT_CONSTVALUE) {
        if (value.isVariable() && !engine.checkAndSetVariableValue(value.value.toString(), this.value)) {
          return false;
        }
      }
    }

    return true;
  }
}
