// WITH_STDLIB

fun box(): String {
    val min = 0U.toString()
    if ("0" != min) throw AssertionError(min)

    val middle = 2_147_483_647U.toString()
    if ("2147483647" != middle) throw AssertionError(middle)

    val max = 4_294_967_295U.toString()
    if ("4294967295" != max) throw AssertionError(max)

    return "OK"
}
