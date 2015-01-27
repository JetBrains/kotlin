// FILE: a.kt
package a

class X

// FILE: b.kt
package b

class X

// FILE: c.kt
package c

import <!CONFLICTING_IMPORT!>a.X<!>
import <!CONFLICTING_IMPORT!>b.X<!>

class Y : <!UNRESOLVED_REFERENCE!>X<!>