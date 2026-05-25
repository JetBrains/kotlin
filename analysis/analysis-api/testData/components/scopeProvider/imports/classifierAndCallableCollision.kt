// FILE: main.kt
import other.Shared

val klass: Shared = Shared
val fromFun: Int = Shared()

// FILE: other/other.kt
package other

object Shared

fun Shared(): Int = 1
