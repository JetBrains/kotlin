// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB
// OPT_IN: kotlin.contracts.ExperimentalContracts
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

@OptIn(ExperimentalContracts::class)
fun barWithContract(f: () -> Unit) {
    contract {
        callsInPlace(f, InvocationKind.EXACTLY_ONCE)
    }
    f()
}

@OptIn(ExperimentalContracts::class)
fun barWithContractAtMostOnce(f: () -> Unit) {
    contract {
        callsInPlace(f, InvocationKind.AT_MOST_ONCE)
    }
    f()
}

fun foo() {
    val y: Int
    barWithContract {
        <!EO_DIAGNOSTIC!>y<!> = 2
    }
    println(y)

    val x: Int
    barWithContractAtMostOnce {
        x = 2
    }
}

/* GENERATED_FIR_TAGS: assignment, classReference, functionDeclaration, functionalType, integerLiteral, lambdaLiteral,
localProperty, propertyDeclaration */
