// !CHECK_TYPE

operator fun Int.component1() = "a"

fun foo(a: Number) {
    val (x) = a as Int
    checkSubtype<Int>(<!DEBUG_INFO_SMARTCAST!>a<!>)
    checkSubtype<String>(x)
}
