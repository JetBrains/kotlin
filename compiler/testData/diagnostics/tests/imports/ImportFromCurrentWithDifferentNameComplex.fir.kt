// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-54854
package a

import a.A as ER
import a.x as y
import a.foo as bar

val x: Int = 1

fun foo(): Int = 1

class A

interface B {
    val a: <!UNRESOLVED_REFERENCE!>A<!>
    val b: ER
}

fun main() {
    <!UNRESOLVED_REFERENCE!>A<!>()
    ER()
    a.A()

    <!UNRESOLVED_REFERENCE!>x<!> + 1
    y + 1

    <!UNRESOLVED_REFERENCE!>foo<!>() + 1
    bar() + 1
}
