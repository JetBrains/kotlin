// FILE: a.kt
package outer

private fun a() {}
private class B

// FILE: b.kt
package outer.p1

import outer.a

fun use() {
    <!INAPPLICABLE_CANDIDATE!>a<!>()
    outer.<!INAPPLICABLE_CANDIDATE!>B<!>()
}

// FILE: c.kt
package outer.p1.p2

import outer.a

fun use() {
    <!INAPPLICABLE_CANDIDATE!>a<!>()
    outer.<!INAPPLICABLE_CANDIDATE!>B<!>()
}