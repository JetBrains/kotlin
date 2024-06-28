// FILE: dependency.kt
package one.two

open class BaseClass {
    val bar = 1
}

object TopLevelObject :BaseClass()

// FILE: main.kt
package another

import one.two.TopLevelObject.bar

fun usage() {
    <expr>bar</expr>
}
