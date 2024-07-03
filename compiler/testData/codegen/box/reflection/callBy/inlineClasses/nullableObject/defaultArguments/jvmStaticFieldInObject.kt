// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// WITH_REFLECT

import kotlin.reflect.KCallable
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KParameter
import kotlin.reflect.jvm.isAccessible
import kotlin.test.assertEquals

@JvmInline
value class S(val value: String?) {
    operator fun plus(other: S): S = S(this.value!! + other.value!!)
}

object C {
    @JvmStatic
    private var p1: S = S("")

    @JvmStatic
    private var p2: S? = S("")

    fun nonNullBoundRef() = this::p1.apply { isAccessible = true }
    fun nullableBoundRef() = this::p2.apply { isAccessible = true }
}

private fun <T> KCallable<T>.callBy(vararg args: Any?): T =
    callBy(parameters.associateWith { args[it.index] })

fun box(): String {
    val nonNullUnboundRef = C::class.members.single { it.name == "p1" } as KMutableProperty1<C, S>
    nonNullUnboundRef.isAccessible = true
    assertEquals(Unit, nonNullUnboundRef.setter.callBy(C, S("ab")))
    assertEquals(S("ab"), nonNullUnboundRef.callBy(C))
    assertEquals(S("ab"), nonNullUnboundRef.getter.callBy(C))

    val nullableUnboundRef = C::class.members.single { it.name == "p2" } as KMutableProperty1<C, S?>
    nullableUnboundRef.isAccessible = true
    assertEquals(Unit, nullableUnboundRef.setter.callBy(C, S("ab")))
    assertEquals(S("ab"), nullableUnboundRef.callBy(C))
    assertEquals(S("ab"), nullableUnboundRef.getter.callBy(C))

    val nonNullBoundRef = C.nonNullBoundRef()
    assertEquals(Unit, nonNullBoundRef.setter.callBy(S("cd")))
    assertEquals(S("cd"), nonNullBoundRef.callBy())
    assertEquals(S("cd"), nonNullBoundRef.getter.callBy())

    val nullableBoundRef = C.nullableBoundRef()
    assertEquals(Unit, nullableBoundRef.setter.callBy(S("cd")))
    assertEquals(S("cd"), nullableBoundRef.callBy())
    assertEquals(S("cd"), nullableBoundRef.getter.callBy())

    return "OK"
}
