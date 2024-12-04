// FILE: dependency.kt
package one.two

object TopLevelObject {
    fun foo() {}
}

// FILE: main.kt
package another

import one.two.TopLevelObject.fo<caret>o
