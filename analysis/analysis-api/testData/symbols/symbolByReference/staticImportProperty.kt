// DO_NOT_CHECK_SYMBOL_RESTORE_K1
// FILE: dependency.kt
package one.two

object TopLevelObject {
    val bar = 1
}

// FILE: main.kt
package another

import one.two.TopLevelObject.b<caret>ar
