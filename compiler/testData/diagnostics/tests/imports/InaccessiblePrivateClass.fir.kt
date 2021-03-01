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

// Bug:
// the type of property x is p1/X, the type of the initializer is p2/X
val <!EXPOSED_PROPERTY_TYPE!>x<!>: X = <!INITIALIZER_TYPE_MISMATCH!>X()<!>
val <!EXPOSED_PROPERTY_TYPE!>y<!>: Y = <!HIDDEN!>Y<!>()
