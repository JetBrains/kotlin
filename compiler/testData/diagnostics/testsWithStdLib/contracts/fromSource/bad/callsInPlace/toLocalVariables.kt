// RUN_PIPELINE_TILL: BACKEND
// DUMP_CFG
import kotlin.contracts.*

fun bar(x: () -> Unit) {

}

@ExperimentalContracts
fun foo(x: () -> Unit, y: () -> Unit, z: () -> Unit) {
    contract {
        <!LEAKED_IN_PLACE_LAMBDA!>callsInPlace(x, InvocationKind.AT_MOST_ONCE)<!>
        callsInPlace(y, InvocationKind.AT_MOST_ONCE)
        callsInPlace(z, InvocationKind.AT_MOST_ONCE)
    }

    if (true) {
        bar(<!LEAKED_LOCAL_THROUGH_CALL!>x<!>)
    } else {
        val yCopy = y
        yCopy()
    }

    val zCopy: () -> Unit
    zCopy = z
    zCopy()
}

/* GENERATED_FIR_TAGS: assignment, contractCallsEffect, contracts, functionDeclaration, functionalType, ifExpression,
lambdaLiteral, localProperty, propertyDeclaration */
