// FILE: File1.kt
package pack1

private open class SomeClass

// FILE: Main.kt
package a

import pack1.*

class X : <!INVISIBLE_REFERENCE, INVISIBLE_MEMBER!>SomeClass<!>()