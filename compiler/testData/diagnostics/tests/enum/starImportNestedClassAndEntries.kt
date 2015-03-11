// FILE: E.kt
package foo

enum class E {
    ENTRY
    ANOTHER

    class Nested {
        default object {
            fun foo() = 42
        }
    }
}

// FILE: main.kt
package bar

import foo.E.*

fun f1() = ENTRY
fun f2() = ANOTHER
fun f3() = Nested()
fun f4() = Nested.foo()
fun f5() = values()
