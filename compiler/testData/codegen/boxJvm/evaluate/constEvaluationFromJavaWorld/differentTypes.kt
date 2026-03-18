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
const val CHAR: Char = '1' + 1
const val BOOL: Boolean = true
const val BYTE: Byte = (1.toByte() + 1).toByte()
const val SHORT: Short = (1.toShort() + 1).toShort()
const val INT: Int = 1 + 1
const val LONG: Long = 1L + 1L
const val FLOAT: Float = 1.5f + .5f
const val DOUBLE: Double = 1.5 + 0.5
const val STRING: String = "1" + "2"

// FILE: usages.kt
const val CHAR_JAVA: Char = Bar.CHAR + 1
const val BOOL_JAVA: Boolean = Bar.BOOL
const val BYTE_JAVA: Byte = (Bar.BYTE + 1).toByte()
const val SHORT_JAVA: Short = (Bar.SHORT + 1).toShort()
const val INT_JAVA: Int = Bar.INT + 1
const val LONG_JAVA: Long = Bar.LONG + 1L
const val FLOAT_JAVA: Float = Bar.FLOAT + 1.0f
const val DOUBLE_JAVA: Double = Bar.DOUBLE + 1.0
const val STRING_JAVA: String = Bar.STRING + "4"

fun box(): String {
    return "OK"
}
