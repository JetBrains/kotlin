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

// ORDER: foo2
// ORDER: foo3
// ORDER: foo6
// ORDER: foo4
// ORDER: foo1
// ORDER: fooLocal
// ORDER: foo5
