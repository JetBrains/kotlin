// FILE: main.kt
package test

import dependency.component1
import dependency.component2

fun usage(target: dependency.Target) {
    val (c1, с2) = target
}

// FILE: dependency.kt
package dependency

class Target

operator fun Target.component1(): Int = 1
operator fun Target.component2(): Int = 2
