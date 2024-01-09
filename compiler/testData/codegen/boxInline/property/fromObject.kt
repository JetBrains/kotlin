// WITH_STDLIB
// FILE: 1.kt
package test

open class A(var result: String) {

    var y
        inline get() = if (this is C) this else A(result)
        inline set(a: A) {
            if (this is C) this else A(a.result.also { this.result = it })
        }
}

object C : A("failA")

object B : A("failB")

// FILE: 2.kt
import test.A
import test.B.y

fun box(): String {
    y = A("OK")

    return y.result
}
