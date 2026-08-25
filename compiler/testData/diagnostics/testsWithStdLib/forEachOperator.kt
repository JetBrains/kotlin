// RUN_PIPELINE_TILL: FRONTEND
inline operator fun IntRange.forEachWhile(action: (Int) -> Boolean): Unit =
    forEach { if (!action(it)) return else Unit }

inline fun foo(block: () -> String): String = block()

fun bar(block: () -> Unit): Unit = block()

fun test() {
    outer@ foreach (i in 1..10) {
        foo {
            foreach (j in 1..10) {
                bar {
                    <!RETURN_NOT_ALLOWED!>return<!>
                    <!RETURN_NOT_ALLOWED!>return@foo<!> "test"
                    return@bar
                    <!BREAK_OR_CONTINUE_JUMPS_ACROSS_FUNCTION_BOUNDARY!>break<!>
                    <!BREAK_OR_CONTINUE_JUMPS_ACROSS_FUNCTION_BOUNDARY!>continue<!>
                    <!BREAK_OR_CONTINUE_JUMPS_ACROSS_FUNCTION_BOUNDARY!>break@outer<!>
                }
            }
            "<unreachable>"
        }
    }
}

/* GENERATED_FIR_TAGS: break, continue, funWithExtensionReceiver, functionDeclaration, functionalType, ifExpression,
inline, integerLiteral, lambdaLiteral, operator, rangeExpression, stringLiteral */
