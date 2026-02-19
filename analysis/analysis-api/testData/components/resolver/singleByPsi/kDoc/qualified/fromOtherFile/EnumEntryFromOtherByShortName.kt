// FILE: a.kt

import b.*

/**
 * [E.A<caret>A]
 */
fun x() {}

// FILE: b.kt
package b

enum class E {
    AA, BB
}
