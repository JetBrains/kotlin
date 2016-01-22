package a

import a.A as ER

interface A {
    val a: <!UNRESOLVED_REFERENCE!>A<!>
    val b: ER
}
