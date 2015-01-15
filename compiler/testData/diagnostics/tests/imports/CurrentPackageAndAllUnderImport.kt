// FILE: a.kt
package a

class X

// FILE: b.kt
package b

open class X

// FILE: b1.kt
package b

import a.*

class Y : X() // class from the current package should take priority
