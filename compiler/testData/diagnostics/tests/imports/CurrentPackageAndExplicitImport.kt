// FILE: a.kt
package a

open class Y

// FILE: b.kt
package b

class X

// FILE: b1.kt
package b

import a.Y as X

class Y : X() // class from explicit import should take priority
