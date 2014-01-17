// FILE: a.kt
package example.ns
val y: Any? = 2

// FILE: b.kt
package example

val x: Int = if (ns.y is Int) <!DEBUG_INFO_AUTOCAST!>ns.y<!> else 2