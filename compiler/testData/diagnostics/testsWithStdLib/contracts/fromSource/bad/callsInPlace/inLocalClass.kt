// RUN_PIPELINE_TILL: BACKEND
// DUMP_CFG
import kotlin.contracts.*

@ExperimentalContracts
fun foo(a: () -> Unit, b: () -> Unit, c: () -> Unit, d: () -> Unit, e: () -> Unit) {
    contract {
        <!LEAKED_IN_PLACE_LAMBDA!>callsInPlace(a, InvocationKind.AT_MOST_ONCE)<!>
        <!LEAKED_IN_PLACE_LAMBDA!>callsInPlace(b, InvocationKind.AT_MOST_ONCE)<!>
        <!LEAKED_IN_PLACE_LAMBDA!>callsInPlace(c, InvocationKind.AT_MOST_ONCE)<!>
        <!LEAKED_IN_PLACE_LAMBDA!>callsInPlace(d, InvocationKind.AT_MOST_ONCE)<!>
        callsInPlace(e, InvocationKind.AT_MOST_ONCE)
    }

    class LocalClass {

        val leakedVal = <!LEAKED_LOCAL_THROUGH_CAPTURE!>a<!>
        val leaked: Any

        constructor() {
            <!LEAKED_LOCAL_THROUGH_CAPTURE!>b<!>()
        }

        init {
            leaked = <!LEAKED_LOCAL_THROUGH_CAPTURE!>c<!>
        }

        fun run() {
            <!LEAKED_LOCAL_THROUGH_CAPTURE!>d<!>()
        }
    }

    LocalClass().run()

    e()
}

/* GENERATED_FIR_TAGS: assignment, classDeclaration, contractCallsEffect, contracts, functionDeclaration, functionalType,
init, lambdaLiteral, localClass, propertyDeclaration, secondaryConstructor */
