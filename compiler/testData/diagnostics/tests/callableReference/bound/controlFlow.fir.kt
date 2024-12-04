// RUN_PIPELINE_TILL: BACKEND
// DIAGNOSTICS: +UNUSED_EXPRESSION
// WITH_EXTRA_CHECKERS

fun unusedExpression(s: String) {
    <!UNUSED_EXPRESSION!>s::hashCode<!>
    <!UNUSED_EXPRESSION!>s::class<!>
}

fun noUnusedParameter(s: String): Int {
    val f = s::hashCode
    return f()
}

fun unreachableCode(): Int {
    <!UNREACHABLE_CODE!>(if (<!>true<!UNREACHABLE_CODE!>)<!> return 1 <!UNREACHABLE_CODE!>else<!> return 0<!UNREACHABLE_CODE!>)::toString<!>
    <!UNREACHABLE_CODE!>return 0<!>
}

fun unreachableCodeInLoop(): Int {
    while (true) {
        <!UNREACHABLE_CODE!>(<!>break<!UNREACHABLE_CODE!>)::toString<!>
        <!UNREACHABLE_CODE!>return 1<!>
    }
    return 2
}
