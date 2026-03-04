// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB
// OPT_IN: kotlin.contracts.ExperimentalContracts
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

fun barRegular(f: () -> Unit) {}

@OptIn(ExperimentalContracts::class)
fun barWithContractExactlyOnce(f: () -> Unit) {
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
    barWithContractExactlyOnce {
        y = 2
    }
    println(y)

    val x: Int
    barWithContractAtMostOnce {
        x = 2
        println(x)
    }
}

fun nestedExactlyOnceCase() {
    barRegular{
        val <!ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE!>nested<!>: Int
        barWithContractAtMostOnce {
            nested = 2
        }
    }
}

/* GENERATED_FIR_TAGS: assignment, classReference, functionDeclaration, functionalType, integerLiteral, lambdaLiteral,
localProperty, propertyDeclaration */
