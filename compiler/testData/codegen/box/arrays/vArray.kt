// WITH_STDLIB
// TARGET_BACKEND: JVM_IR
// LANGUAGE: +ValueClasses
@JvmInline
value class ICDoubleNullable(val x: Double?)

@JvmInline
value class ICChar(val x: Char)

@JvmInline
value class ICICChar(val x: ICChar)

@JvmInline
value class MFVC(val x: Int, val y: Int)

fun box(): String {
    val aInt = VArray(2) { it + 1 }
    if (aInt[1] != 2) return "Fail 1"

    val aStr = VArray(2) { "a" }
    if (aStr[1] != "a") return "Fail 2"

    val aUByte = VArray<UByte>(2) { UByte.MAX_VALUE }
    if (aUByte[1] != UByte.MAX_VALUE) return "Fail 3"

    val aULongNullable = VArray<ULong?>(2) { null }
    if (aULongNullable[1] != null) return "Fail 4"

    val aICDoubleNullable = VArray<ICDoubleNullable>(2) { ICDoubleNullable(42.0) }
    if (aICDoubleNullable[1].x != 42.0) return "Fail 5"

    val aICChar = VArray(2) { ICICChar(ICChar('a')) }
    if (aICChar[1].x.x != 'a') return "Fail 6"

    val aMFVC = VArray(2) { MFVC(0, 0) }
    if (aMFVC[0].x != 0) return "Fail 7"

    return "OK"
}
