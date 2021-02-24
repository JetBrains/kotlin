// FIR_IDENTICAL
// FILE: a.kt
// KT-26 Import namespaces defined in this file

import html.* // Must not be an error

// FILE: b.kt

package html

abstract class Factory<T: Any> {
fun create() : T? = null
}
