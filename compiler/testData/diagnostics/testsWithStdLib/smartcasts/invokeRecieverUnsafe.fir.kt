// SKIP_TXT
// ISSUE: KT-63709

operator fun Int.invoke(unused: Int) {}

fun test1(a: Int?) {
    <!UNSAFE_IMPLICIT_INVOKE_CALL!>a<!>(a!!)
}

fun test2(a: Int?) {
    with (a) {
        <!UNSAFE_CALL!>invoke<!>(this!!)
    }
}
