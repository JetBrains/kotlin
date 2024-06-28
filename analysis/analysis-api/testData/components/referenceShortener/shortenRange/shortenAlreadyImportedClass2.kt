// FILE: main.kt
package a.b.c

import dependency.T

class T(a: Int)

fun test() {
    <expr>dependency.T::class.java</expr>
}

// FILE: dep.kt
package dependency

class T<E>(value: Int)