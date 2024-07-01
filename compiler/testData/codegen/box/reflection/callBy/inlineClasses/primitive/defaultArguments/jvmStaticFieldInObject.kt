// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// WITH_REFLECT

import kotlin.reflect.KCallable
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KParameter
import kotlin.reflect.jvm.isAccessible
import kotlin.test.assertEquals

@JvmInline
value class Z(val value: Int) {
    operator fun plus(other: Z): Z = Z(this.value + other.value)
}

object C {
    @JvmStatic
    private var p1: Z = Z(-1)

    @JvmStatic
    private var p2: Z? = Z(-1)

    fun nonNullBoundRef() = this::p1.apply { isAccessible = true }
    fun nullableBoundRef() = this::p2.apply { isAccessible = true }
}

private fun <T> KCallable<T>.callBy(vararg args: Any?): T {
    val params = parameters
    return params.fold(HashMap<KParameter, Any?>()) { acc, cur ->
        acc.apply {
            acc[cur] = args[cur.index]
        }
    }.let { callBy(it) }
}

fun box(): String {
    val one = Z(1)
    val two = Z(2)

    val nonNullUnboundRef = C::class.members.single { it.name == "p1" } as KMutableProperty1<C, Z>
    nonNullUnboundRef.isAccessible = true
    assertEquals(Unit, nonNullUnboundRef.setter.callBy(C, one))
    assertEquals(one, nonNullUnboundRef.callBy(C))
    assertEquals(one, nonNullUnboundRef.getter.callBy(C))

    val nullableUnboundRef = C::class.members.single { it.name == "p2" } as KMutableProperty1<C, Z?>
    nullableUnboundRef.isAccessible = true
    assertEquals(Unit, nullableUnboundRef.setter.callBy(C, one))
    assertEquals(one, nullableUnboundRef.callBy(C))
    assertEquals(one, nullableUnboundRef.getter.callBy(C))

    val nonNullBoundRef = C.nonNullBoundRef()
    assertEquals(Unit, nonNullBoundRef.setter.callBy(two))
    assertEquals(two, nonNullBoundRef.callBy())
    assertEquals(two, nonNullBoundRef.getter.callBy())

    val nullableBoundRef = C.nullableBoundRef()
    assertEquals(Unit, nullableBoundRef.setter.callBy(two))
    assertEquals(two, nullableBoundRef.callBy())
    assertEquals(two, nullableBoundRef.getter.callBy())

    return "OK"
}
