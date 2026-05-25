// FILE: main.kt
import other.overloaded

val a: Int = overloaded(1)
val b: String = overloaded("x")

// FILE: other/other.kt
package other

fun overloaded(x: Int): Int = x
fun overloaded(x: String): String = x
