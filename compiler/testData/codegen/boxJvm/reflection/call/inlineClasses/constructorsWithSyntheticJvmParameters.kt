// TARGET_BACKEND: JVM
// WITH_REFLECT
// FULL_JDK

// Constructors whose JVM signature contains synthetic parameters: sealed class constructors and constructors with
// value class parameters get an extra DefaultConstructorMarker, constructors with default values get a bit mask.
// A mix of value class and primitive parameters used to break parameter counting, see KT-86008.

import kotlin.reflect.full.IllegalCallableAccessException
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.isAccessible
import kotlin.test.*

@JvmInline
value class Ordinal(val value: Int)

@JvmInline
value class Name(val value: String)

sealed class Base(val name: Name, val ordinal: Ordinal, val active: Boolean, val visible: Boolean = true)

class Impl(name: Name, ordinal: Ordinal, active: Boolean, visible: Boolean = false) : Base(name, ordinal, active, visible)

abstract class AbstractBase(val ordinal: Ordinal, val flag: Boolean)

class PrivateCtor private constructor(val ordinal: Ordinal, val flag: Boolean, val d: Double) {
    constructor(flag: Boolean) : this(Ordinal(0), flag, 0.0)
}

private fun Impl.render(): String = "${name.value}:${ordinal.value}:$active:$visible"

fun box(): String {
    val base = Base::class.constructors.single()
    assertEquals(listOf(Name::class, Ordinal::class, Boolean::class, Boolean::class), base.parameters.map { it.type.classifier })
    base.isAccessible = true
    assertFailsWith<InstantiationException> { base.call(Name("a"), Ordinal(1), true, false) }
    assertFailsWith<InstantiationException> {
        base.callBy(base.parameters.take(3).zip(listOf(Name("a"), Ordinal(1), true)).toMap())
    }

    val impl = Impl::class.primaryConstructor!!
    assertEquals("a:1:true:true", impl.call(Name("a"), Ordinal(1), true, true).render())
    assertEquals("b:2:false:false", impl.callBy(impl.parameters.take(3).zip(listOf(Name("b"), Ordinal(2), false)).toMap()).render())

    val abstract = AbstractBase::class.constructors.single()
    assertFailsWith<InstantiationException> { abstract.call(Ordinal(1), true) }

    val private = PrivateCtor::class.primaryConstructor!!
    assertFailsWith<IllegalCallableAccessException> { private.call(Ordinal(3), true, 1.5) }
    private.isAccessible = true
    val instance = private.call(Ordinal(3), true, 1.5)
    assertEquals(3, instance.ordinal.value)
    assertTrue(instance.flag)
    assertEquals(1.5, instance.d)

    val secondary = PrivateCtor::class.constructors.single { it != private }
    assertEquals(0, secondary.call(false).ordinal.value)

    return "OK"
}
