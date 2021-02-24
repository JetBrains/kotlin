// FILE: a.kt
package a.x

class X

// FILE: b.kt
package b.x

class X

// FILE: c.kt
package c

import a.x
import b.x

class Y : <!UNRESOLVED_REFERENCE!>x.X<!>