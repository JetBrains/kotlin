// !CHECK_TYPE

fun foo(x: Number) {
    if (<!USELESS_IS_CHECK!>(x as Int) is Int<!>) {
        checkSubtype<Int>(x)
    }
    checkSubtype<Int>(x)
}
