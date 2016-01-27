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

val x: X = X()
val <!EXPOSED_PROPERTY_TYPE!>y<!>: <!INVISIBLE_REFERENCE!>Y<!> = <!INVISIBLE_MEMBER!>Y<!>()
