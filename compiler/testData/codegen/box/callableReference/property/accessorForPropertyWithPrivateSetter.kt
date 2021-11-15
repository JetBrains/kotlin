// WITH_STDLIB
// FILE: b.kt
import a.A

class B {
    fun getValue() = sequenceOf(A()).map(A::value).first()
}

fun box() = B().getValue()

// FILE: a.kt
package a

class A {
    var value: String = "OK"
        private set
}