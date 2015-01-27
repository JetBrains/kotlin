//FILE:a.kt
package a

import <!CONFLICTING_IMPORT!>b.O<!>
import <!CONFLICTING_IMPORT!>c.O<!>

//FILE:b.kt
package b

object O {}

//FILE:c.kt
package c

object O {}
