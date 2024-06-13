// CHECK_TYPE

fun foo(x: Number) {
    when (x as Int) {
        else -> checkSubtype<Int>(x)
    }
    checkSubtype<Int>(x)
}
