// FILE: a.kt
package a.x

class X

// FILE: b.kt
package b.x

class X

// FILE: c.kt
package c

import a.<!UNRESOLVED_REFERENCE!>x<!>
import b.<!UNRESOLVED_REFERENCE!>x<!>

class Y : <!UNRESOLVED_REFERENCE!>x<!>.<!DEBUG_INFO_MISSING_UNRESOLVED!>X<!>