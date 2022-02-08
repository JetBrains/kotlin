fun test11() {
    fun Any.bar(i: Int) {}
    todo()<!UNREACHABLE_CODE!>.bar(1)<!>
}

fun test12() {
    fun Any.bar(i: Int) {}
    <!SAFE_CALL_WILL_CHANGE_NULLABILITY!>todo()<!UNREACHABLE_CODE!><!UNNECESSARY_SAFE_CALL!>?.<!>bar(1)<!><!>
}

fun todo(): Nothing = throw Exception()
