// MODULE: m1
// FILE: a.kt

package p1

@Deprecated("1", level = DeprecationLevel.HIDDEN)
class A(val v1: Unit)

// MODULE: m2
// FILE: b.kt

package p2

@Deprecated("2", level = DeprecationLevel.HIDDEN)
class A(val v2: Unit)

// MODULE: m3
// FILE: c.kt

package p3

@Deprecated("3", level = DeprecationLevel.HIDDEN)
class A(val v3: Unit)

// MODULE: m4(m1, m2, m3)
// FILE: oneExplicitImportOtherStars.kt
import p1.*
import <!DEPRECATION_ERROR!>p2.A<!>
import p3.*

<!CONFLICTING_OVERLOADS!>fun test(a: <!UNRESOLVED_REFERENCE!>A<!>)<!> {
    a.<!UNRESOLVED_REFERENCE!>v1<!>
    a.<!UNRESOLVED_REFERENCE!>v2<!>
    a.<!UNRESOLVED_REFERENCE!>v3<!>
}

// FILE: severalStarImports.kt
import p1.*
import p2.*
import p3.*

<!CONFLICTING_OVERLOADS!>fun test(a: <!UNRESOLVED_REFERENCE!>A<!>)<!> {
    a.<!UNRESOLVED_REFERENCE!>v1<!>
    a.<!UNRESOLVED_REFERENCE!>v2<!>
    a.<!UNRESOLVED_REFERENCE!>v3<!>
}
