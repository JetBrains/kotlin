object KoKobject {
    @JvmStatic
    val JvmStatic: Int = 1
}

fun test() {
    Integer.MIN_VALUE
    java.lang.Long.MAX_VALUE

    JClass.PrimitiveInt
    JClass.BigPrimitiveInt
    JClass.PrimitiveByte
    JClass.PrimitiveChar
    JClass.PrimitiveLong
    JClass.PrimitiveShort
    JClass.PrimitiveBool
    JClass.PrimitiveFloat
    JClass.PrimitiveDouble
    JClass.Str

    JClass.BoxedInt
    JClass.NonFinal

    JClass().NonStatic

    KoKobject.JvmStatic
}

// @TestKt.class:
// 1 LDC -2147483648
// 1 LDC 9223372036854775807
// 1 SIPUSH 9000
// 1 LDC 59000
// 1 LDC -8
// 1 LDC K
// 1 LDC 100000
// 1 LDC 900
// 1 LDC false
// 1 LDC 36.6
// 1 LDC 42.4242
// 1 LDC ":J"
// 1 GETSTATIC JClass.BoxedInt : Ljava/lang/Integer;
// 1 GETSTATIC JClass.NonFinal : I
// 1 GETFIELD JClass.NonStatic : I
// 1 INVOKESTATIC KoKobject.getJvmStatic \(\)I
// 3 POP2
// 16 POP