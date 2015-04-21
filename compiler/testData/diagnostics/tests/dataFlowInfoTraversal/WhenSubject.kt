// !CHECK_TYPE

fun foo(x: Number) {
    when (x as Int) {
        else -> checkSubtype<Int>(<!DEBUG_INFO_SMARTCAST!>x<!>)
    }
    checkSubtype<Int>(<!DEBUG_INFO_SMARTCAST!>x<!>)
}
