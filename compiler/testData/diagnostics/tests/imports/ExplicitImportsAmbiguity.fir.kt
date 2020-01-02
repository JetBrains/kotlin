// FILE: a.kt
package a

class X

// FILE: b.kt
package b

class X

// FILE: c.kt
package c

import a.X
import b.X

class Y : X