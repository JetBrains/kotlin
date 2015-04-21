// !CHECK_TYPE

fun foo(x: Number) {
    if ((x as Int) is Int) {
        checkSubtype<Int>(<!DEBUG_INFO_SMARTCAST!>x<!>)
    }
    checkSubtype<Int>(<!DEBUG_INFO_SMARTCAST!>x<!>)
}
