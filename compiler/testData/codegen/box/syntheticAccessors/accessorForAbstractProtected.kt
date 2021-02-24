// FILE: 1.kt
import a.*

abstract class B : A() {
    fun g() = { f() }()
}

fun box() = object : B() {
    override fun f(): String = "OK"
}.g()

// FILE: 2.kt
package a

abstract class A {
    protected abstract fun f(): String
}
