// FILE: a.kt
package a.x

class X

// FILE: b.kt
package b.x

class X

// FILE: c.kt
package c

import a.<!PACKAGE_CANNOT_BE_IMPORTED!>x<!>
import b.<!PACKAGE_CANNOT_BE_IMPORTED!>x<!>

class Y : <!UNRESOLVED_REFERENCE!>x<!>.X
