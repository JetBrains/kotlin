// DIAGNOSTICS: +UNUSED_EXPRESSION

fun unusedExpression(s: String) {
    s::hashCode
    s::class
}

fun noUnusedParameter(s: String): Int {
    val f = s::hashCode
    return f()
}

fun unreachableCode(): Int {
    (if (true) return 1 else return 0)::toString
    return 0
}

fun unreachableCodeInLoop(): Int {
    while (true) {
        (break)::toString
        return 1
    }
    return 2
}
