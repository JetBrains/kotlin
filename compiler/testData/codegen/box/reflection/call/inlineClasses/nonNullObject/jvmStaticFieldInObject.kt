// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// WITH_REFLECT

import kotlin.reflect.KMutableProperty1
import kotlin.reflect.jvm.isAccessible
import kotlin.test.assertEquals

@JvmInline
value class S(val value: String) {
    operator fun plus(other: S): S = S(this.value + other.value)
}

object C {
    @JvmStatic
    private var p1: S = S("")

    @JvmStatic
    private var p2: S? = S("")

    fun nonNullBoundRef() = this::p1.apply { isAccessible = true }
    fun nullableBoundRef() = this::p2.apply { isAccessible = true }
}

fun box(): String {
    val nonNullUnboundRef = C::class.members.single { it.name == "p1" } as KMutableProperty1<C, S>
    nonNullUnboundRef.isAccessible = true
    assertEquals(Unit, nonNullUnboundRef.setter.call(C, S("ab")))
    assertEquals(S("ab"), nonNullUnboundRef.call(C))
    assertEquals(S("ab"), nonNullUnboundRef.getter.call(C))

    val nullableUnboundRef = C::class.members.single { it.name == "p2" } as KMutableProperty1<C, S?>
    nullableUnboundRef.isAccessible = true
    assertEquals(Unit, nullableUnboundRef.setter.call(C, S("ab")))
    assertEquals(S("ab"), nullableUnboundRef.call(C))
    assertEquals(S("ab"), nullableUnboundRef.getter.call(C))

    val nonNullBoundRef = C.nonNullBoundRef()
    assertEquals(Unit, nonNullBoundRef.setter.call(S("cd")))
    assertEquals(S("cd"), nonNullBoundRef.call())
    assertEquals(S("cd"), nonNullBoundRef.getter.call())

    val nullableBoundRef = C.nullableBoundRef()
    assertEquals(Unit, nullableBoundRef.setter.call(S("cd")))
    assertEquals(S("cd"), nullableBoundRef.call())
    assertEquals(S("cd"), nullableBoundRef.getter.call())

    return "OK"
}
