fun unusedExpression(s: String) {
    <!UNUSED_EXPRESSION!>s::hashCode<!>
    <!UNUSED_EXPRESSION!>s::class<!>
}

fun noUnusedParameter(s: String): Int {
    val f = s::hashCode
    return f()
}

fun unreachableCode(): Int {
    (if (true) return 1 else return 0)<!UNREACHABLE_CODE!>::toString<!>
    <!UNREACHABLE_CODE!>return 0<!>
}

fun unreachableCodeInLoop(): Int {
    while (true) {
        (break)<!UNREACHABLE_CODE!>::toString<!>
        <!UNREACHABLE_CODE!>return 1<!>
    }
    return 2
}
