// TARGET_BACKEND: JVM
// FILE: Bar.java
public class Bar {
    public static final char CHAR = MainKt.CHAR + 1;
    public static final boolean BOOL = !MainKt.BOOL;
    public static final byte BYTE = MainKt.BYTE + 1;
    public static final short SHORT = MainKt.SHORT + 1;
    public static final int INT = MainKt.INT + 1;
    public static final long LONG = MainKt.LONG + 1L;
    public static final float FLOAT = MainKt.FLOAT + 1.0f;
    public static final double DOUBLE = MainKt.DOUBLE + 1.0;
    public static final String STRING = MainKt.STRING + "3";
}

// FILE: Main.kt
const val CHAR: Char = <!EVALUATED("2")!>'1' + 1<!>
const val BOOL: Boolean = <!EVALUATED("true")!>true<!>
const val BYTE: Byte = (1.toByte() + 1).<!EVALUATED("2")!>toByte()<!>
const val SHORT: Short = (1.toShort() + 1).<!EVALUATED("2")!>toShort()<!>
const val INT: Int = <!EVALUATED("2")!>1 + 1<!>
const val LONG: Long = <!EVALUATED("2")!>1L + 1L<!>
const val FLOAT: Float = <!EVALUATED("2.0")!>1.5f + .5f<!>
const val DOUBLE: Double = <!EVALUATED("2.0")!>1.5 + 0.5<!>
const val STRING: String = <!EVALUATED("12")!>"1" + "2"<!>

// FILE: usages.kt
const val CHAR_JAVA: Char = <!EVALUATED("4")!>Bar.CHAR + 1<!>
const val BOOL_JAVA: Boolean = Bar.<!EVALUATED("false")!>BOOL<!>
const val BYTE_JAVA: Byte = (Bar.BYTE + 1).<!EVALUATED("4")!>toByte()<!>
const val SHORT_JAVA: Short = (Bar.SHORT + 1).<!EVALUATED("4")!>toShort()<!>
const val INT_JAVA: Int = <!EVALUATED("4")!>Bar.INT + 1<!>
const val LONG_JAVA: Long = <!EVALUATED("4")!>Bar.LONG + 1L<!>
const val FLOAT_JAVA: Float = <!EVALUATED("4.0")!>Bar.FLOAT + 1.0f<!>
const val DOUBLE_JAVA: Double = <!EVALUATED("4.0")!>Bar.DOUBLE + 1.0<!>
const val STRING_JAVA: String = <!EVALUATED("1234")!>Bar.STRING + "4"<!>

fun box(): String {
    return "OK"
}
