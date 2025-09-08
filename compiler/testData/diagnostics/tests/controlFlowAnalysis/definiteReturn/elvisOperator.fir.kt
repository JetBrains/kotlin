// RUN_PIPELINE_TILL: BACKEND
fun error(): Nothing = null!!

fun test0(): String {
    <!USELESS_ELVIS_LEFT_IS_NULL!>null ?:<!> return ""
}

fun test1(): String {
    run { null } ?: return ""
}

fun test2(): String {
    run<Nothing?> { null } ?: return ""
}

fun test3(): String {
    run { error() } <!USELESS_ELVIS!>?: return ""<!>
}

fun test4(): String {
    run { run { null } ?: return "" }
}

/* GENERATED_FIR_TAGS: checkNotNullCall, elvisExpression, functionDeclaration, lambdaLiteral, nullableType,
stringLiteral */
