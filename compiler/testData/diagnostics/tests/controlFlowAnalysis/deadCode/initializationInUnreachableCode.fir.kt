// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-40851

fun error(): Nothing = throw Exception()

class Some() {
    <!MUST_BE_INITIALIZED_OR_BE_ABSTRACT!>var x: Int<!>
    val y: Int = error()

    init {
        <!UNREACHABLE_CODE!>x = 1<!>;
    }
}
