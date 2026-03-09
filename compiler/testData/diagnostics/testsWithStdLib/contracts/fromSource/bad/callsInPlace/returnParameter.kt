// RUN_PIPELINE_TILL: BACKEND
import kotlin.contracts.*

@ExperimentalContracts
fun foo(block: () -> Unit): () -> Unit {
    contract {
        <!LEAKED_IN_PLACE_LAMBDA!>callsInPlace(block, InvocationKind.UNKNOWN)<!>
    }
    <!LEAKED_LOCAL!>return block<!>
}

/* GENERATED_FIR_TAGS: contractCallsEffect, contracts, functionDeclaration, functionalType, lambdaLiteral */
