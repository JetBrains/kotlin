// FILE: a.kt
package example.ns
val y: Any? = 2

// FILE: b.kt
package example

val x: Int = if (example.ns.y is Int) example.ns.y else 2