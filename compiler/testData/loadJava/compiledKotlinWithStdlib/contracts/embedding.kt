// LANGUAGE_VERSION: 1.3
@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package test

import kotlin.internal.contracts.*

// this tests specifically use primitive condition (predicate/value) as the
// first argument of sequence, so that it would be optimized and embedded into message

fun embedVariable(x: Any, b: Boolean) {
    contract {
        returns() implies (b && x is String)
    }
}

fun embedInstancePredicate(x: Any, y: Any?) {
    contract {
        returns() implies (x is String && y is String)
    }
}

fun embedNullCheckPredicate(x: Any?, y: Int?) {
    contract {
        returns() implies (y != null && x is String)
    }
}

fun Boolean.embedReceiverReference(b: Boolean) {
    contract {
        returns() implies (!this@embedReceiverReference && b)
    }
}