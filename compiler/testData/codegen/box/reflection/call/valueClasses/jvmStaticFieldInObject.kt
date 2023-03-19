// TARGET_BACKEND: JVM_IR
// JVM_TARGET: 1.8
// WITH_REFLECT
// LANGUAGE: +ValueClasses

import kotlin.reflect.KMutableProperty1
import kotlin.reflect.jvm.isAccessible
import kotlin.test.assertEquals

@JvmInline
value class Z(val value1: UInt, val value2: Int) {
    operator fun plus(other: Z): Z = Z(this.value1 + other.value1, this.value2 + other.value2)
}

object C {
    @JvmStatic
    private var p1: Z = Z(UInt.MAX_VALUE, -10)

    @JvmStatic
    private var p2: Z? = Z(UInt.MAX_VALUE, -10)

    fun nonNullBoundRef() = this::p1.apply { isAccessible = true }
    fun nullableBoundRef() = this::p2.apply { isAccessible = true }
}

fun box(): String {
    val one = Z(1U, 10)
    val two = Z(2U, 20)

    val nonNullUnboundRef = C::class.members.single { it.name == "p1" } as KMutableProperty1<C, Z>
    nonNullUnboundRef.isAccessible = true
    assertEquals(Unit, nonNullUnboundRef.setter.call(C, one))
    assertEquals(one, nonNullUnboundRef.call(C))
    assertEquals(one, nonNullUnboundRef.getter.call(C))

    val nullableUnboundRef = C::class.members.single { it.name == "p2" } as KMutableProperty1<C, Z?>
    nullableUnboundRef.isAccessible = true
    assertEquals(Unit, nullableUnboundRef.setter.call(C, one))
    assertEquals(one, nullableUnboundRef.call(C))
    assertEquals(one, nullableUnboundRef.getter.call(C))

    val nonNullBoundRef = C.nonNullBoundRef()
    assertEquals(Unit, nonNullBoundRef.setter.call(two))
    assertEquals(two, nonNullBoundRef.call())
    assertEquals(two, nonNullBoundRef.getter.call())

    val nullableBoundRef = C.nullableBoundRef()
    assertEquals(Unit, nullableBoundRef.setter.call(two))
    assertEquals(two, nullableBoundRef.call())
    assertEquals(two, nullableBoundRef.getter.call())

    return "OK"
}
