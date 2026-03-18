// TARGET_BACKEND: JVM

// WITH_REFLECT

package test

import kotlin.reflect.full.*

class A {
    var String.id: String
        get() = this
        set(value) {}

    fun Int.foo(): Double = toDouble()
}

fun box(): String {
    val p = A::class.memberExtensionProperties.single()
    return if ("$p" == "var test.A.(kotlin.String.)id: kotlin.String") "OK" else "Fail $p"

    val q = A::class.declaredFunctions.single()
    if ("$q" != "fun test.A.(kotlin.Int.)foo(): kotlin.Double") return "Fail q $q"

    return "OK"
}
