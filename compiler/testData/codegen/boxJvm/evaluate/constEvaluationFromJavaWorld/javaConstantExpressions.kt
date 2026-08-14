// TARGET_BACKEND: JVM_IR
// Every Java constant below is written in a form which is *not* a plain literal: a narrowing cast,
// an operator expression, a reference to another `static final` field (own class, other class,
// static import) or a concatenation. The Kotlin side consumes each of them from a `const val` and
// from an annotation argument, so the value has to be known to the frontend.

// FILE: test/Consts.java
package test;

public class Consts {
    public static final int TEN = 10;
    public static final String HEL = "hel";
}

// FILE: test/Bar.java
package test;

import static test.Consts.HEL;
import static test.Consts.TEN;

public class Bar {
    public static final int LOCAL = 3;

    // JLS 5.4: a cast to a primitive type is part of the constant expression
    public static final byte BYTE_CAST = (byte) 300;
    public static final short SHORT_CAST = (short) 70000;
    public static final char CHAR_CAST = (char) 65;
    public static final byte BYTE_CAST_OF_EXPRESSION = (byte) (LOCAL * 100);
    public static final int INT_CAST_OF_DOUBLE = (int) 2.75;
    public static final long LONG_CAST_OF_DOUBLE = (long) 1.5e3;
    public static final char CHAR_CAST_OF_CHAR_ARITHMETIC = (char) ('A' + 1);

    // the same narrowing, driven by the declared type instead of a cast
    public static final byte BYTE_WITHOUT_CAST = 7;
    public static final short SHORT_WITHOUT_CAST = 8;

    // operators
    public static final int SHIFTED = 10 >> 1;
    public static final long LONG_SHIFTED = 1L << 40;
    public static final int MASKED = 0xF0 & 0x3C;
    public static final int PARENTHESIZED = (1 + 2) * 3;
    public static final int NEGATED = -LOCAL;
    public static final int INVERTED = ~LOCAL;
    public static final boolean COMPARED = LOCAL > 0;
    public static final boolean CONJUNCTION = true && !false;
    public static final double HALF = 1 / 2.0;
    public static final float QUARTER = 1f / 4;

    // references to other constants
    public static final int SUM_WITH_STATIC_IMPORT = LOCAL + TEN;
    public static final int FROM_OTHER_CLASS = Consts.TEN * 2;

    // polyadic concatenation, with a reference and with a parenthesized operand
    public static final String CONCATENATED = HEL + "l" + "o";
    public static final String CONCATENATED_WITH_NUMBER = "n=" + (LOCAL + 1);
}

// FILE: usages.kt
import test.Bar

const val BYTE_CAST: Byte = Bar.BYTE_CAST
const val SHORT_CAST: Short = Bar.SHORT_CAST
const val CHAR_CAST: Char = Bar.CHAR_CAST
const val BYTE_CAST_OF_EXPRESSION: Byte = Bar.BYTE_CAST_OF_EXPRESSION
const val INT_CAST_OF_DOUBLE: Int = Bar.INT_CAST_OF_DOUBLE
const val LONG_CAST_OF_DOUBLE: Long = Bar.LONG_CAST_OF_DOUBLE
const val CHAR_CAST_OF_CHAR_ARITHMETIC: Char = Bar.CHAR_CAST_OF_CHAR_ARITHMETIC

const val BYTE_WITHOUT_CAST: Byte = Bar.BYTE_WITHOUT_CAST
const val SHORT_WITHOUT_CAST: Short = Bar.SHORT_WITHOUT_CAST

const val SHIFTED: Int = Bar.SHIFTED
const val LONG_SHIFTED: Long = Bar.LONG_SHIFTED
const val MASKED: Int = Bar.MASKED
const val PARENTHESIZED: Int = Bar.PARENTHESIZED
const val NEGATED: Int = Bar.NEGATED
const val INVERTED: Int = Bar.INVERTED
const val COMPARED: Boolean = Bar.COMPARED
const val CONJUNCTION: Boolean = Bar.CONJUNCTION
const val HALF: Double = Bar.HALF
const val QUARTER: Float = Bar.QUARTER

const val SUM_WITH_STATIC_IMPORT: Int = Bar.SUM_WITH_STATIC_IMPORT
const val FROM_OTHER_CLASS: Int = Bar.FROM_OTHER_CLASS

const val CONCATENATED: String = Bar.CONCATENATED
const val CONCATENATED_WITH_NUMBER: String = Bar.CONCATENATED_WITH_NUMBER

@Retention(AnnotationRetention.RUNTIME)
annotation class ConstArgument(
    val intValue: Int,
    val byteValue: Byte,
    val charValue: Char,
    val stringValue: String,
)

@ConstArgument(Bar.SUM_WITH_STATIC_IMPORT, Bar.BYTE_CAST, Bar.CHAR_CAST, Bar.CONCATENATED)
class Annotated

fun box(): String {
    if (BYTE_CAST != 44.toByte()) return "Fail BYTE_CAST: $BYTE_CAST"
    if (SHORT_CAST != 4464.toShort()) return "Fail SHORT_CAST: $SHORT_CAST"
    if (CHAR_CAST != 'A') return "Fail CHAR_CAST: $CHAR_CAST"
    if (BYTE_CAST_OF_EXPRESSION != 44.toByte()) return "Fail BYTE_CAST_OF_EXPRESSION: $BYTE_CAST_OF_EXPRESSION"
    if (INT_CAST_OF_DOUBLE != 2) return "Fail INT_CAST_OF_DOUBLE: $INT_CAST_OF_DOUBLE"
    if (LONG_CAST_OF_DOUBLE != 1500L) return "Fail LONG_CAST_OF_DOUBLE: $LONG_CAST_OF_DOUBLE"
    if (CHAR_CAST_OF_CHAR_ARITHMETIC != 'B') return "Fail CHAR_CAST_OF_CHAR_ARITHMETIC: $CHAR_CAST_OF_CHAR_ARITHMETIC"

    if (BYTE_WITHOUT_CAST != 7.toByte()) return "Fail BYTE_WITHOUT_CAST: $BYTE_WITHOUT_CAST"
    if (SHORT_WITHOUT_CAST != 8.toShort()) return "Fail SHORT_WITHOUT_CAST: $SHORT_WITHOUT_CAST"

    if (SHIFTED != 5) return "Fail SHIFTED: $SHIFTED"
    if (LONG_SHIFTED != 1099511627776L) return "Fail LONG_SHIFTED: $LONG_SHIFTED"
    if (MASKED != 48) return "Fail MASKED: $MASKED"
    if (PARENTHESIZED != 9) return "Fail PARENTHESIZED: $PARENTHESIZED"
    if (NEGATED != -3) return "Fail NEGATED: $NEGATED"
    if (INVERTED != -4) return "Fail INVERTED: $INVERTED"
    if (!COMPARED) return "Fail COMPARED: $COMPARED"
    if (!CONJUNCTION) return "Fail CONJUNCTION: $CONJUNCTION"
    if (HALF != 0.5) return "Fail HALF: $HALF"
    if (QUARTER != 0.25f) return "Fail QUARTER: $QUARTER"

    if (SUM_WITH_STATIC_IMPORT != 13) return "Fail SUM_WITH_STATIC_IMPORT: $SUM_WITH_STATIC_IMPORT"
    if (FROM_OTHER_CLASS != 20) return "Fail FROM_OTHER_CLASS: $FROM_OTHER_CLASS"

    if (CONCATENATED != "hello") return "Fail CONCATENATED: $CONCATENATED"
    if (CONCATENATED_WITH_NUMBER != "n=4") return "Fail CONCATENATED_WITH_NUMBER: $CONCATENATED_WITH_NUMBER"

    val annotation = Annotated::class.java.getAnnotation(ConstArgument::class.java)
    if (annotation.intValue != 13) return "Fail annotation intValue: ${annotation.intValue}"
    if (annotation.byteValue != 44.toByte()) return "Fail annotation byteValue: ${annotation.byteValue}"
    if (annotation.charValue != 'A') return "Fail annotation charValue: ${annotation.charValue}"
    if (annotation.stringValue != "hello") return "Fail annotation stringValue: ${annotation.stringValue}"

    return "OK"
}
