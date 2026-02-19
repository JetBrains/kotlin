// FILE: dependency.kt
package one.two

object TopLevelObject {
    val bar = 1
}

// FILE: main.kt
package another

import one.two.TopLevelObject.bar

fun usage() {
    <expr>bar</expr>
}
