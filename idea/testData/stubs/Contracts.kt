@file:Suppress("INVISIBLE_MEMBER")
package test

import kotlin.internal.contracts.*

fun myRequire(x: Boolean) {
    contract {
        returns() implies x
    }
}