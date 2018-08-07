// RUNTIME
package test

import bar.r
import bar.foo3

fun main() {
    val foo5 = 3
    r {
        foo<caret>
    }
}

// ORDER: foo2
// ORDER: foo4
// ORDER: fooloooooong
// ORDER: foo5
// ORDER: foo3
// ORDER: fooval
// ORDER: foo1
