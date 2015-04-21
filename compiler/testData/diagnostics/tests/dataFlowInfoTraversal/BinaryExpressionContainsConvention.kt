// !CHECK_TYPE

fun foo(x: Number): Boolean {
    val result = (x as Int) in 1..5
    checkSubtype<Int>(<!DEBUG_INFO_SMARTCAST!>x<!>)
    return result
}
