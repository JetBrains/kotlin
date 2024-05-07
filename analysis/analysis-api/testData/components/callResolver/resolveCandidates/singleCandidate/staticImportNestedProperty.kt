// FILE: dependency.kt
package one.two

object TopLevelObject {
    object Nested {
        val bar = 1
    }
}

// FILE: main.kt
package another

import one.two.TopLevelObject.Nested.bar

fun usage() {
    <expr>bar</expr>
}