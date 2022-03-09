// FILE: a.kt
package a

class X

// FILE: b.kt
package b

class X

// FILE: c.kt
package c

import a.*
import b.*

class Y : <!OVERLOAD_RESOLUTION_AMBIGUITY!>X<!>
