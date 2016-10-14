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

fun test(a: <!API_NOT_AVAILABLE!>A<!>) {
    a.<!UNRESOLVED_REFERENCE!>v1<!>
    a.v2
    a.<!UNRESOLVED_REFERENCE!>v3<!>
}

// FILE: severalStarImports.kt
import p1.*
import p2.*
import p3.*

fun test(a: <!UNRESOLVED_REFERENCE!>A<!>) {
    <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>a<!>.<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>v1<!>
    <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>a<!>.<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>v2<!>
    <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>a<!>.<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>v3<!>
}
