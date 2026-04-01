// DO_NOT_REQUIRE_SYMBOL_RESTORATION_K1
// FILE: dependency.kt
package one.two

object TopLevelObject {
    val bar = 1
}

// FILE: main.kt
package another

import one.two.TopLevelObject.b<caret>ar
