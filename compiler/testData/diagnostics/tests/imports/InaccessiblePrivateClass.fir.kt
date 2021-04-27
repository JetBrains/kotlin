// FILE: a.kt
package p1

private class X
private class Y

// FILE: b.kt
package p2

class X

// FILE: c.kt
package p1

import p2.*

val <!EXPOSED_PROPERTY_TYPE!>x<!>: X = <!INITIALIZER_TYPE_MISMATCH, TYPE_MISMATCH!>X()<!>
val <!EXPOSED_PROPERTY_TYPE!>y<!>: Y = <!INVISIBLE_REFERENCE!>Y<!>()
