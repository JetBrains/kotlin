// MODULE: m1
// FILE: a.kt

package p1

@Deprecated("Use p2.A instead", level = DeprecationLevel.HIDDEN)
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

<!CONFLICTING_OVERLOADS!>fun test(a: A)<!> {
    a.<!UNRESOLVED_REFERENCE!>m1<!>()
    a.m2()
}

// FILE: explicitlyImportP1.kt
import <!DEPRECATION_ERROR!>p1.A<!>
import p2.*

<!CONFLICTING_OVERLOADS!>fun test(a: A)<!> {
    a.<!UNRESOLVED_REFERENCE!>m1<!>()
    a.m2()
}
