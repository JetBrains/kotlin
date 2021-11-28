fun test11() {
    fun Any.bar(<!UNUSED_PARAMETER!>i<!>: Int) {}
    todo().<!UNREACHABLE_CODE!>bar(1)<!>
}

fun test12() {
    fun Any.bar(<!UNUSED_PARAMETER!>i<!>: Int) {}
    <!SAFE_CALL_WILL_CHANGE_NULLABILITY!>todo()<!UNNECESSARY_SAFE_CALL!>?.<!><!UNREACHABLE_CODE!>bar(1)<!><!>
}

fun todo(): Nothing = throw Exception()
