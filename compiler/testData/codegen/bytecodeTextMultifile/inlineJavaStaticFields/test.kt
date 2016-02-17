object KoKobject {
    @JvmField
    val JvmStatic: Int = 1

    @JvmField
    val JvmStaticString: String? = "123"
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
    JClass.StrNullable

    JClass().NonStatic

    KoKobject.JvmStatic
    KoKobject.JvmStaticString
}

// @TestKt.class:
// 1 LDC -2147483648
// 1 LDC 9223372036854775807
// 1 SIPUSH 9000
// 1 LDC 59000
// 1 BIPUSH -8
// 1 LDC K
// 1 LDC 100000
// 1 SIPUSH 901
// 1 LDC false
// 1 LDC 36.6
// 1 LDC 42.4242
// 1 LDC ":J"
// 1 GETSTATIC JClass.BoxedInt : Ljava/lang/Integer;
// 1 GETSTATIC JClass.NonFinal : I
// 1 GETFIELD JClass.NonStatic : I
// 1 LDC "nullable"
// 1 GETSTATIC KoKobject.JvmStatic : I
// 1 GETSTATIC KoKobject.JvmStaticString : Ljava/lang/String
// 3 POP2
// 18 POP