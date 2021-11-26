// FILE: main.kt
package test

import dependency.Bar
import dependency.invoke

fun usage(b: Bar) {
    b()
}

// FILE: dependency.kt
package dependency

class Bar

operator fun Bar.invoke(i: Int) {}