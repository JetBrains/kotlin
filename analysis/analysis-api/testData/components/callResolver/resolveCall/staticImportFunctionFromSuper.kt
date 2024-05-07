// FILE: dependency.kt
package one.two

open class BaseClass {
    fun foo() {}
}

object TopLevelObject : BaseClass()

// FILE: main.kt
package another

import one.two.TopLevelObject.foo

fun usage() {
    <expr>foo()</expr>
}
