// FILE: dependency.kt
package one.two

object TopLevelObject {
    fun foo() {}
    fun foo(int: Int) {}
}

// FILE: main.kt
package another

import one.two.TopLevelObject.foo

fun usage() {
    <expr>foo()</expr>
}
