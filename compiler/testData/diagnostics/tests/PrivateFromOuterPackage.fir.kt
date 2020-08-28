// FILE: a.kt
package outer

private fun a() {}
private class B

// FILE: b.kt
package outer.p1

import outer.a

fun use() {
    <!HIDDEN!>a<!>()
    outer.<!HIDDEN!>B<!>()
}

// FILE: c.kt
package outer.p1.p2

import outer.a

fun use() {
    <!HIDDEN!>a<!>()
    outer.<!HIDDEN!>B<!>()
}
