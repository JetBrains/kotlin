// MODULE: lib
// MODULE_KIND: LibraryBinary
// FILE: E.kt
package lib

enum class E {
    @Deprecated(level = DeprecationLevel.WARNING, message = "")
    Entry
}

// MODULE: dep(lib)
// FILE: usage.kt
package usage

import lib.*

fun usage() = E.Entry
