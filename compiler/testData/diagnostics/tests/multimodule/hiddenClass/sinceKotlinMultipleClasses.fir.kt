// !API_VERSION: 1.0
// MODULE: m1
// FILE: a.kt

package p1

@SinceKotlin("1.1")
class A(val v1: Unit)

// MODULE: m2
// FILE: b.kt

package p2

@SinceKotlin("1.1")
class A(val v2: Unit)

// MODULE: m3
// FILE: c.kt

package p3

@SinceKotlin("1.1")
class A(val v3: Unit)

// MODULE: m4(m1, m2, m3)
// FILE: oneExplicitImportOtherStars.kt
import p1.*
import p2.<!API_NOT_AVAILABLE!>A<!>
import p3.*

fun test(a: <!NONE_APPLICABLE!>A<!>) {
    a.<!UNRESOLVED_REFERENCE!>v1<!>
    a.<!UNRESOLVED_REFERENCE!>v2<!>
    a.<!UNRESOLVED_REFERENCE!>v3<!>
}

// FILE: severalStarImports.kt
import p1.*
import p2.*
import p3.*

fun test(a: <!NONE_APPLICABLE!>A<!>) {
    a.<!UNRESOLVED_REFERENCE!>v1<!>
    a.<!UNRESOLVED_REFERENCE!>v2<!>
    a.<!UNRESOLVED_REFERENCE!>v3<!>
}
