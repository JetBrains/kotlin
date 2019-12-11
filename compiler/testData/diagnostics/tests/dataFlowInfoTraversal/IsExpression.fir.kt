// !CHECK_TYPE

fun foo(x: Number) {
    if ((x as Int) is Int) {
        checkSubtype<Int>(x)
    }
    checkSubtype<Int>(x)
}
