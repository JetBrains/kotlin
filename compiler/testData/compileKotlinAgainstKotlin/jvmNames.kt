// IGNORE_BACKEND_FIR: JVM_IR
// TARGET_BACKEND: JVM
// FILE: A.kt

package lib

@JvmName("bar")
fun foo() = "foo"

var v: Int = 1
    @JvmName("vget")
    get
    @JvmName("vset")
    set

fun consumeInt(x: Int) {}

open class A {
    val OK: String = "OK"
        @JvmName("OK") get

    @JvmName("g")
    fun <T> f(x: T, y: Int = 1) = x
}

annotation class Anno(@get:JvmName("uglyJvmName") val value: String)

// FILE: B.kt

import lib.*

class B : A()

@Anno("OK")
fun annotated() {}

fun box(): String {
    foo()
    v = 1
    consumeInt(v)

    val annoValue = (::annotated.annotations.single() as Anno).value
    if (annoValue != "OK") return "Fail annotation value: $annoValue"

    val b = B()
    if (b.f("OK") != "OK") return "Fail call of annotated method"

    return A().OK
}
