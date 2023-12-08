// !LANGUAGE: +AllowAssigningArrayElementsToVarargsInNamedFormForFunctions
// ISSUE: KT-63514
// WITH_STDLIB

fun foo(vararg arr: Int): Int {
    return arr.sum()
}

fun bar(vararg arr: UInt): UInt {
    return arr.sum()
}

fun baz(vararg arr: ULong): ULong {
    return arr.sum()
}

fun quas(vararg arr: UShort): UInt {
    return arr.sum()
}

fun wex(vararg arr: UByte): UInt {
    return arr.sum()
}

fun box(): String {
    val x = foo(arr = intArrayOf(1, 2, 3))
    val y = bar(arr = uintArrayOf(1u, 2u, 3u))
    val z = baz(arr = ulongArrayOf(1uL, 2uL, 3uL))
    val q = quas(arr = ushortArrayOf(1u, 2u, 3u))
    val w = wex(arr = ubyteArrayOf(1u, 2u, 3u))
    if (x + y.toInt() + z.toInt() + q.toInt() + w.toInt() == 30) {
        return "OK"
    }
    return "Fail"
}
