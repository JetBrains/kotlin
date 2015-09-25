//FILE:a.kt
package a

import b.<!CONFLICTING_IMPORT!>O<!>
import c.<!CONFLICTING_IMPORT!>O<!>

//FILE:b.kt
package b

object O {}

//FILE:c.kt
package c

object O {}