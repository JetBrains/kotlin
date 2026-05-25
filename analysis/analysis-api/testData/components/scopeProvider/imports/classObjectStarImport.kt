// FILE: main.kt
import other.Holder.*

val a: Int = MEMBER

// FILE: other/other.kt
package other

object Holder {
    const val MEMBER: Int = 1
}
