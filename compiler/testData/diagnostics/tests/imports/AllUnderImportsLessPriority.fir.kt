// FILE: a.kt
package a

open class X

// FILE: b.kt
package b

class X

// FILE: c.kt
package c

import a.X
import b.*

class Y : X()