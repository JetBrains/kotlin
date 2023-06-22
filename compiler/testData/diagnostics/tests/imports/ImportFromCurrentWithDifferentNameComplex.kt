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

    <!UNRESOLVED_REFERENCE!>x<!> <!DEBUG_INFO_MISSING_UNRESOLVED!>+<!> 1
    y + 1

    <!UNRESOLVED_REFERENCE!>foo<!>() <!DEBUG_INFO_MISSING_UNRESOLVED!>+<!> 1
    bar() + 1
}
