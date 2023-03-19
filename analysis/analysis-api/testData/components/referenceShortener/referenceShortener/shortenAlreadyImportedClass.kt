// FILE: main.kt
package a.b.c

import dependency.T

class T

fun test() {
    class T(a: Int)
    <expr>dependency.T<Int>(3)</expr>
}

// FILE: dep.kt
package dependency

class T<E>(value: Int)