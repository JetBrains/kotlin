// !DIAGNOSTICS: -UNUSED_PARAMETER

fun testArgumentInCall() {
    fun bar(i: Int, s: String, x: Any) {}

    <!UNREACHABLE_CODE!>bar(<!>1, todo(), <!UNREACHABLE_CODE!>"")<!>
}

fun testArgumentInVariableAsFunctionCall(f: (Any) -> Unit) {
    f<!UNREACHABLE_CODE!>(<!>todo()<!UNREACHABLE_CODE!>)<!>
}

fun todo(): Nothing = throw Exception()