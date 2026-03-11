// TARGET_BACKEND: JVM
// WITH_REFLECT

import kotlin.reflect.full.*

open class A {
    fun mem() {}
    fun Int.memExt() {}
}

class B : A()

fun box(): String {
    val all = A::class.functions.map { it.name }.sorted()
    assert(all == listOf("equals", "hashCode", "mem", "memExt", "toString")) { "Fail A functions: ${A::class.functions}" }

    val declared = A::class.declaredFunctions.map { it.name }.sorted()
    assert(declared == listOf("mem", "memExt")) { "Fail A declaredFunctions: ${A::class.declaredFunctions}" }

    val declaredSubclass = B::class.declaredFunctions.map { it.name }.sorted()
    assert(declaredSubclass.isEmpty()) { "Fail B declaredFunctions: ${B::class.declaredFunctions}" }

    return "OK"
}
