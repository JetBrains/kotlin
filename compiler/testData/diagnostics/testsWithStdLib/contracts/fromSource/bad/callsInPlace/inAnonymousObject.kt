// RUN_PIPELINE_TILL: BACKEND
// DUMP_CFG
import kotlin.contracts.*

@ExperimentalContracts
fun foo(a: () -> Unit, b: () -> Unit, c: () -> Unit, d: () -> Unit) {
    contract {
        <!LEAKED_IN_PLACE_LAMBDA!>callsInPlace(a, InvocationKind.AT_MOST_ONCE)<!>
        <!LEAKED_IN_PLACE_LAMBDA!>callsInPlace(b, InvocationKind.AT_MOST_ONCE)<!>
        <!LEAKED_IN_PLACE_LAMBDA!>callsInPlace(c, InvocationKind.AT_MOST_ONCE)<!>
        callsInPlace(d, InvocationKind.AT_MOST_ONCE)
    }

    val obj = object : Runnable {

        val leakedVal = <!LEAKED_LOCAL_THROUGH_CAPTURE!>a<!>
        val leaked: Any

        init {
            leaked = <!LEAKED_LOCAL_THROUGH_CAPTURE!>b<!>
        }

        override fun run() {
            <!LEAKED_LOCAL_THROUGH_CAPTURE!>c<!>()
        }

    }

    obj.run()

    d()
}

/* GENERATED_FIR_TAGS: anonymousObjectExpression, assignment, contractCallsEffect, contracts, functionDeclaration,
functionalType, init, lambdaLiteral, localProperty, override, propertyDeclaration */
