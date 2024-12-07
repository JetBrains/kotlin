// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB
// ISSUE: KT-58149

fun <U> myRun(calc: () -> U): U {
    return calc()
}

fun foo() {
    // OK, works because we explicitly avoid adding last "return" as lambda result
    val dates1 = myRun {
        return@myRun buildList {
            add(1)
        }
    }

    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.List<kotlin.Int>")!>dates1<!>

    // OK, because the type of when is inferred to List<*>
    val dates2 = myRun {
        when {
            true -> return@myRun buildList {
                add(2)
            }
            else -> return@myRun buildList {
                add(3)
            }
        }
    }

    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.List<*>")!>dates2<!>

    // Doesn't work both in K1 and K2, but probably should (KT-58232 for tracking)
    val dates3 = <!NEW_INFERENCE_ERROR!>myRun {
        when {
            else -> return@myRun <!CANNOT_INFER_PARAMETER_TYPE!>buildList<!> {
                add(<!ARGUMENT_TYPE_MISMATCH!>4<!>)
            }
        }
    }<!>
}
