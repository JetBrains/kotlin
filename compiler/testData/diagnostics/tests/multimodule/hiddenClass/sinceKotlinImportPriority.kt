// !API_VERSION: 1.0
// MODULE: m1
// FILE: a.kt

package p1

@SinceKotlin("1.1")
class A {
    fun m1() {}
}

// MODULE: m2
// FILE: b.kt

package p2

class A {
    fun m2() {}
}

// MODULE: m3(m1, m2)
// FILE: severalStarImports.kt
import p1.*
import p2.*

fun test(a: A) {
    a.<!UNRESOLVED_REFERENCE!>m1<!>()
    a.m2()
}

// FILE: explicitlyImportP1.kt
import p1.<!API_NOT_AVAILABLE!>A<!>
import p2.*

fun test(a: <!API_NOT_AVAILABLE!>A<!>) {
    a.m1()
    a.<!UNRESOLVED_REFERENCE!>m2<!>()
}
