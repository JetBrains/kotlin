// !OPT_IN: kotlin.RequiresOptIn
// FILE: api.kt


// MODULE: A
package main

@kotlin.RequiresOptIn
annotation class Marker

data class DataClass(@property:Marker val x: Int)

// MODULE: B(A)
package main

fun test(d: DataClass) {
    val (<!OPT_IN_USAGE_ERROR!>x<!>) = d
    val c = d.<!OPT_IN_USAGE_ERROR!>component1<!>()
}
