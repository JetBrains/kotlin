// DO_NOT_CHECK_SYMBOL_RESTORE_K1
// FILE: dependency.kt
package one.two

open class BaseClass {
    val bar = 1
}

object TopLevelObject :BaseClass()

// FILE: main.kt
package another

import one.two.TopLevelObject.b<caret>ar
