// TARGET_BACKEND: JVM
// WITH_STDLIB
// WITH_REFLECT

// JVM_ABI_K1_K2_DIFF: KT-63843, KT-63984

// MODULE: lib
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

// MODULE: main(lib)
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
