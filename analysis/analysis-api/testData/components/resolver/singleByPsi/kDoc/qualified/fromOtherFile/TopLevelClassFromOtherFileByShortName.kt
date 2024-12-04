// FILE: a.kt
import b.*

/**
 * [B<caret>B]
 */
fun x() {}

// FILE: b.kt
package b

class BB {
    class XX
}