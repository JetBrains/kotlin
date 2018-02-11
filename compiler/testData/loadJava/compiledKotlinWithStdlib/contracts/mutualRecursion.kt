// LANGUAGE_VERSION: 1.3
@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package test

import kotlin.internal.contracts.*

fun foo(x: Any?): Boolean {
    contract {
        returns() implies (x is String)
    }
    return bar(x)
}

fun bar(x: Any?): Boolean {
    contract {
        returns() implies (x is Int)
    }
    return foo(x)
}