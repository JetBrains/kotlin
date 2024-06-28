// ISSUE: KT-56614

// FILE: package1.kt

package p1

fun interface SamUnit {
    fun foo()
}

fun bar(x: String, y: String, z: SamUnit) {
}

// FILE: package2.kt

package p2

fun bar(x: Any, y: Any, z: () -> Unit) {
}

// FILE: main.kt

import p1.*
import p2.*

fun main() {
    bar("", "") {} // Resolves to p1.bar in K1, but to p2.bar in K2
}
