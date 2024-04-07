// FILE: a.kt
import b.BB

/**
 * [B<caret>B]
 */
fun x() {}

// FILE: b.kt
package b

typealias BB = Int