// FILE: dependency.kt
package one.two

object TopLevelObject {
    object Nested {
        fun foo() {}
        fun foo(i: Int) {}
    }
}

// FILE: main.kt
package another

import one.two.TopLevelObject.Nested.foo

fun usage() {
    <expr>foo()</expr>
}
