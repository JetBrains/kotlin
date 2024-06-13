// FIR_IDENTICAL
// OPT_IN: kotlin.RequiresOptIn
// FILE: api.kt

package api

@RequiresOptIn(level = RequiresOptIn.Level.WARNING)
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
annotation class ExperimentalAPI1

@RequiresOptIn(level = RequiresOptIn.Level.WARNING)
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
annotation class ExperimentalAPI2

@ExperimentalAPI1
fun compilation() {}

@ExperimentalAPI2
fun runtime() {}

// FILE: usage.kt

@file:OptIn(ExperimentalAPI1::class)
package usage

import api.*

fun use() {
    compilation()
    <!OPT_IN_USAGE!>runtime<!>()
}

class Use {
    fun use() {
        compilation()
        <!OPT_IN_USAGE!>runtime<!>()
    }
}
