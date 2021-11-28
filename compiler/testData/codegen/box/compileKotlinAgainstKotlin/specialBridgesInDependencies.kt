// WITH_STDLIB
// MODULE: lib
// FILE: A.kt

package a

open class A : ArrayList<String>()

// MODULE: main(lib)
// FILE: B.kt

import a.A

class B : A()

fun box(): String {
    val b = B()
    b += "OK"
    return b.single()
}
