// !CHECK_TYPE

fun foo(x: Number) {
    if (<!USELESS_IS_CHECK!>(x as Int) is Int<!>) {
        checkSubtype<Int>(<!DEBUG_INFO_SMARTCAST!>x<!>)
    }
    checkSubtype<Int>(<!DEBUG_INFO_SMARTCAST!>x<!>)
}
