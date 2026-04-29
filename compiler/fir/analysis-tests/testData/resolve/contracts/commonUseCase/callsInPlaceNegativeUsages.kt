// RUN_PIPELINE_TILL: BACKEND
// OPT_IN: kotlin.contracts.ExperimentalContracts
// DIAGNOSTICS: -UNUSED_VARIABLE
// WITH_STDLIB

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

inline fun <R> run2(block: () -> R): R {
    return block()
}

inline fun inlineFunction(block: () -> Int) {
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

fun negativeEffectivelyImmutableVar() {
    var x: String? = maybeString()
    if (x != null) {
        exactlyOnce {
            x.length
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

fun badCastOnFunctionParameter(x: Any?) {
    exactlyOnceResult {
        if (x is String) {
            x.length
        }
    }
}

fun useCaseWithoutContractWithStable() {
    var x: String? = "hello"
    if (x != null) {
        regular {
            x.length
        }
        inlineFunction {
            x.length
        }
    }
}

fun sameAssignmentTest() {
    var x: Int? = null

    run2 {
        x = 1
    }

    checkNotNull(x)

    run2 {
        assertNotEquals(x, 2)
    }
}

fun assertNotEquals(unexpected: Any?, actual: Any?) {}

/* GENERATED_FIR_TAGS: anonymousObjectExpression, assignment, equalityExpression, functionDeclaration, functionalType,
ifExpression, integerLiteral, lambdaLiteral, localProperty, nullableType, override, propertyDeclaration, smartcast,
typeParameter */
