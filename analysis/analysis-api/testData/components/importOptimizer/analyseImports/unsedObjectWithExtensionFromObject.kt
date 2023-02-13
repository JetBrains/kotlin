// FILE: main.kt
import dependency.Bar.Companion.minutes
import dependency.Bar.Companion

fun test() {
    10.minutes
}

// FILE: dependency.kt
package dependency

class Bar {
    companion object {
        val Int.minutes: Int get() = this
    }
}
