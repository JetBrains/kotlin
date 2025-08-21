// RUN_PIPELINE_TILL: BACKEND
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

fun higherOrder(f: () -> Unit) {}

@OptIn(ExperimentalContracts::class)
inline fun test_1(b: Boolean, bar: () -> Unit) {
    contract { callsInPlace(bar, InvocationKind.EXACTLY_ONCE) }

    bar()
    higherOrder { }
    if (b) higherOrder { }
}

@OptIn(ExperimentalContracts::class)
inline fun test_2(b: Boolean, bar: () -> Unit) {
    contract { callsInPlace(bar, InvocationKind.EXACTLY_ONCE) }

    higherOrder { }
    if (b) higherOrder { }
    bar()
}

@OptIn(ExperimentalContracts::class)
inline fun test_3(b: Boolean, bar: () -> Unit) {
    contract { callsInPlace(bar, InvocationKind.EXACTLY_ONCE) }

    higherOrder { }
    if (b) higherOrder { }
    bar()
    higherOrder { }
    if (b) higherOrder { }
}

/* GENERATED_FIR_TAGS: classReference, contractCallsEffect, contracts, functionDeclaration, functionalType, ifExpression,
inline, lambdaLiteral */
