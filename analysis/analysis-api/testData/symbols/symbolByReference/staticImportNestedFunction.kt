// FILE: dependency.kt
package one.two

object TopLevelObject {
    object Nested {
        fun foo() {}
    }
}

// FILE: main.kt
package another

import one.two.TopLevelObject.Nested.fo<caret>o
