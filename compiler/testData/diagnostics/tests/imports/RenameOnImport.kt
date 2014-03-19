// FILE: a.kt
package a

val x = 1

// FILE: b.kt
package b

import a.x as AX

val y = AX