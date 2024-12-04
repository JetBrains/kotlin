// TARGET_BACKEND: JVM
// WITH_REFLECT

import kotlin.reflect.*
import kotlin.reflect.full.*
import kotlin.test.assertEquals

val p0 = 1
val Int.p1: Int get() = this
class A {
    val Int.p2: Int get() = this
}

var globalCounter = 0

var mp0 = 1
    set(value) { globalCounter += value }
var Int.mp1: Int
    get() = this
    set(value) { globalCounter += value }
class B {
    var Int.mp2: Int
        get() = this
        set(value) { globalCounter += value }
}


fun box(): String {
    assertEquals(1, (::p0).call())
    assertEquals(1, (::p0).getter.call())
    assertEquals(2, (Int::p1).call(2))
    assertEquals(2, (Int::p1).getter.call(2))
    val p2 = A::class.memberExtensionProperties.single()
    assertEquals(3, p2.call(A(), 3))
    assertEquals(3, p2.getter.call(A(), 3))

    assertEquals(1, (::mp0).call())
    assertEquals(1, (::mp0).getter.call())
    assertEquals(2, (Int::mp1).call(2))
    assertEquals(2, (Int::mp1).getter.call(2))
    val mp2 = B::class.memberExtensionProperties.single() as KMutableProperty2
    assertEquals(3, mp2.call(B(), 3))
    assertEquals(3, mp2.getter.call(B(), 3))

    assertEquals(Unit, (::mp0).setter.call(1))
    assertEquals(Unit, (Int::mp1).setter.call(0, 3))
    assertEquals(Unit, mp2.setter.call(B(), 0, 5))
    if (globalCounter != 9) return "Fail: $globalCounter"

    return "OK"
}
