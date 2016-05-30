fun unusedExpression(s: String) {
    // TODO: report UNUSED_EXPRESSION (KT-12551)
    s::hashCode
    s::class
}

fun noUnusedParameter(s: String): Int {
    val f = s::hashCode
    return f()
}

fun unreachableCode(): Int {
    (if (true) return 1 else return 0)::toString
    <!UNREACHABLE_CODE!>return 0<!>
}

fun unreachableCodeInLoop(): Int {
    while (true) {
        (break)::toString
        <!UNREACHABLE_CODE!>return 1<!>
    }
    return 2
}
