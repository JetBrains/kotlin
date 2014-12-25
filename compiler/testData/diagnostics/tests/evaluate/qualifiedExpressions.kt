// FILE: a.kt
package example.ns
val y: Any? = 2

// FILE: b.kt
package example

val x: Int = if (example.ns.y is Int) <!DEBUG_INFO_SMARTCAST!>example.ns.y<!> else 2