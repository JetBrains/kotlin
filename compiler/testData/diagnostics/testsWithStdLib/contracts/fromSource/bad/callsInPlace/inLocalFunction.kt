// RUN_PIPELINE_TILL: BACKEND
// DUMP_CFG
import kotlin.contracts.*

@ExperimentalContracts
fun foo(a: () -> Unit, b: () -> Unit) {
    contract {
        <!LEAKED_IN_PLACE_LAMBDA!>callsInPlace(a, InvocationKind.AT_MOST_ONCE)<!>
    }

    fun localFun() {
        <!LEAKED_LOCAL_THROUGH_CAPTURE!>a<!>.invoke()
        <!LEAKED_LOCAL_THROUGH_CAPTURE!>a<!>()
    }

    localFun()

    b()
}

/* GENERATED_FIR_TAGS: contractCallsEffect, contracts, functionDeclaration, functionalType, lambdaLiteral, localFunction */
