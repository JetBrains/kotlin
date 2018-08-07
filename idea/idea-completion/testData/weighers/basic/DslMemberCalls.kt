// RUNTIME
package test

import bar.r
import bar.foo3

fun main() {
    val fooLocal = 3
    r {
        foo<caret>
    }
}

// ORDER: foo6
// ORDER: foo2
// ORDER: foo3
// ORDER: foo4
// ORDER: fooLocal
// ORDER: foo5
// ORDER: foo1
