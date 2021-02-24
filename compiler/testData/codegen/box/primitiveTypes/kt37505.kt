fun testByte(x: Int?) = x?.toByte()?.hashCode()?.equals(x)

fun testShort(x: Int?) = x?.toShort()?.hashCode()?.equals(x)

fun box(): String {
    testByte(42)
    testShort(42)

    return "OK"
}