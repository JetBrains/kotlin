// RUN_PIPELINE_TILL: FRONTEND
// SKIP_TXT
// DIAGNOSTICS: -USELESS_IS_CHECK, -DEBUG_INFO_SMARTCAST
// ISSUE: KT-57194

fun foo(arg: Double) {}
fun foo(arg: Float) {}
fun Double.bar() {}
fun Float.bar() {}

fun test(arg: Any) {
    ::foo

    if (arg is Double) {
        if (<!IMPOSSIBLE_IS_CHECK_ERROR!>arg is Float<!>) {
            foo(arg)
            arg.bar()
        }
    }
}

/* GENERATED_FIR_TAGS: callableReference, funWithExtensionReceiver, functionDeclaration, ifExpression, intersectionType,
isExpression, smartcast */
