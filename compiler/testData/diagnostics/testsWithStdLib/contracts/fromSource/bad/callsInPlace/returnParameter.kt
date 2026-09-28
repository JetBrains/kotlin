// RUN_PIPELINE_TILL: CODEGEN
import kotlin.contracts.*

@ExperimentalContracts
fun foo(block: () -> Unit): () -> Unit {
    contract {
        <!LEAKED_IN_PLACE_LAMBDA!>callsInPlace(block, InvocationKind.UNKNOWN)<!>
    }
    return <!LEAKED_IN_PLACE_LAMBDA!>block<!>
}

/* GENERATED_FIR_TAGS: contractCallsEffect, contracts, functionDeclaration, functionalType, lambdaLiteral */
