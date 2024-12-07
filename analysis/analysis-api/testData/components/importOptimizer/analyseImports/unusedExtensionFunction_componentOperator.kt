// FILE: main.kt
package test

import dependency.component1
import dependency.component2
import dependency.component3

fun usage(target: dependency.Target) {
    val (c1, _) = target
}

// FILE: dependency.kt
package dependency

class Target

operator fun Target.component1(): Int = 1
operator fun Target.component2(): Int = 2
operator fun Target.component3(): Int = 2
