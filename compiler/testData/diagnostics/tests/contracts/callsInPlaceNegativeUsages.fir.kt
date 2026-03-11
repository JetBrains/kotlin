// RUN_PIPELINE_TILL: BACKEND
// OPT_IN: kotlin.contracts.ExperimentalContracts
// DIAGNOSTICS: -UNUSED_VARIABLE

import kotlin.contracts.*

fun maybeString(): String? = "ok"

interface Box {
    fun f(): Int
}

fun <R> exactlyOnceResult(block: () -> R): R {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    return block()
}

fun exactlyOnce(block: () -> Unit) {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    block()
}

fun regular(block: () -> Unit) {
    block()
}

fun negativeExisting(): Box = exactlyOnceResult {
    object : Box {
        override fun f(): Int {
            var existing: String? = maybeString()
            if (existing != null) {
                existing.length
            }
            existing = null
            return 0
        }
    }
}

// maybe better exclude
fun negativeEffectivelyImmutableVar() {
    var x: String? = maybeString()
    if (x != null) {
        exactlyOnce {
            <!SMARTCAST_RELYING_ON_CALLS_IN_PLACE!>x<!>.length
        }
    }
}

fun negativeNestedRegularLambda() {
    exactlyOnce {
        regular {
            var x: String? = maybeString()
            if (x != null) {
                x.length
            }
            x = null
        }
    }
}

fun negativeLocalFunction() {
    exactlyOnce {
        fun local() {
            var x: String? = maybeString()
            if (x != null) {
                x.length
            }
            x = null
        }

        local()
    }
}

/* GENERATED_FIR_TAGS: anonymousObjectExpression, assignment, equalityExpression, functionDeclaration, functionalType,
ifExpression, integerLiteral, lambdaLiteral, localProperty, nullableType, override, propertyDeclaration, smartcast,
typeParameter */
