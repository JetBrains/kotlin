// IGNORE_BACKEND: JVM_IR
// FILE: JClass.java

import org.jetbrains.annotations.NotNull;

public class JClass {
    public final static int PrimitiveInt = 9000;
    public final static int BigPrimitiveInt = 59000;
    public final static long PrimitiveLong = 100000;
    public final static short PrimitiveShort = 901;
    public final static boolean PrimitiveBool = false;
    public final static float PrimitiveFloat = 36.6;
    public final static double PrimitiveDouble = 42.4242;
    public final static byte PrimitiveByte = -8;
    public final static char PrimitiveChar = 'K';
    public final static String Str = ":J";

    @Nullable
    public final static String StrNullable = "nullable";

    @NotNull
    public final static Integer BoxedInt = 9500;

    public static int NonFinal = 9700;

    public final int NonStatic = 9800;
}

// FILE: test.kt

enum class EClass {
    VALUE
}
object KoKobject {
    @JvmField
    val JvmStatic: Int = 1

    @JvmField
    val JvmStaticString: String? = "123"
}

fun Any?.use() {}

fun test() {
    ("res1: " +
    Integer.MIN_VALUE + " " +
    java.lang.Long.MAX_VALUE + " " +
    JClass.PrimitiveInt + " " +
    JClass.BigPrimitiveInt + " " +
    JClass.PrimitiveByte + " " +
    JClass.PrimitiveChar + " " +
    JClass.PrimitiveLong + " " +
    JClass.PrimitiveShort + " " +
    JClass.PrimitiveBool + " " +
    JClass.PrimitiveFloat + " " +
    JClass.PrimitiveDouble + " " +
    JClass.Str + " " +
    JClass.StrNullable).use()

    "res2: " + JClass.BoxedInt
    "res3: " + JClass.NonFinal
    "res4: " + JClass().NonStatic
    "res5: " + KoKobject.JvmStatic
    "res6: " + KoKobject.JvmStaticString
    "res7: " + EClass.VALUE
    "res8: " + EClass::class
}

// 1 LDC "res1: -2147483648 9223372036854775807 9000 59000 -8 K 100000 901 false 36.6 42.4242 :J nullable"
// 1 LDC "res2: "
// 1 LDC "res3: "
// 1 LDC "res4: "
// 1 LDC "res5: "
// 1 LDC "res6: "
// 1 LDC "res7: "
// 1 LDC "res8: "
