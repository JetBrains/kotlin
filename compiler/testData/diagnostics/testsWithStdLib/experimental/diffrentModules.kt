// OPT_IN: kotlin.RequiresOptIn

// MODULE: A
package main

@kotlin.RequiresOptIn
annotation class Marker

data class DataClass(@property:Marker val x: Int)

// MODULE: B(A)
package main

fun test(d: DataClass) {
    val (x) = d
    val c = d.component1()
}
