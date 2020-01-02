// FILE: 1.kt
package something

object N

class WC {
    companion object
}

typealias T = N
typealias TWC = WC

// FILE: 2.kt
import something.T
import something.TWC

val test1 = T.hashCode()
val test2 = TWC.hashCode()