// WITH_REFLECT

import kotlin.test.assertTrue
import kotlin.test.assertFalse

inline fun inline() {}
class External { external fun external() }
operator fun Unit.invoke() {}
infix fun Unit.infix(unit: Unit) {}
tailrec fun tailrec() { tailrec() }
class Suspend { suspend fun suspend(c: Continuation<Unit>) {} }

val externalGetter = Unit
    external get

inline var inlineProperty: Unit
    get() = Unit
    set(value) {}

fun box(): String {
    assertTrue(::inline.isInline)
    assertFalse(::inline.isExternal)
    assertFalse(::inline.isOperator)
    assertFalse(::inline.isInfix)
    assertFalse(::inline.isTailrec)
    assertFalse(::inline.isSuspend)

    assertFalse(External::external.isInline)
    assertTrue(External::external.isExternal)
    assertFalse(External::external.isOperator)
    assertFalse(External::external.isInfix)
    assertFalse(External::external.isTailrec)
    assertFalse(External::external.isSuspend)

    assertFalse(Unit::invoke.isInline)
    assertFalse(Unit::invoke.isExternal)
    assertTrue(Unit::invoke.isOperator)
    assertFalse(Unit::invoke.isInfix)
    assertFalse(Unit::invoke.isTailrec)
    assertFalse(Unit::invoke.isSuspend)

    assertFalse(Unit::infix.isInline)
    assertFalse(Unit::infix.isExternal)
    assertFalse(Unit::infix.isOperator)
    assertTrue(Unit::infix.isInfix)
    assertFalse(Unit::infix.isTailrec)
    assertFalse(Unit::infix.isSuspend)

    assertFalse(::tailrec.isInline)
    assertFalse(::tailrec.isExternal)
    assertFalse(::tailrec.isOperator)
    assertFalse(::tailrec.isInfix)
    assertTrue(::tailrec.isTailrec)
    assertFalse(::tailrec.isSuspend)

    assertFalse(Suspend::suspend.isInline)
    assertFalse(Suspend::suspend.isExternal)
    assertFalse(Suspend::suspend.isOperator)
    assertFalse(Suspend::suspend.isInfix)
    assertFalse(Suspend::suspend.isTailrec)
    assertTrue(Suspend::suspend.isSuspend)

    assertTrue(::externalGetter.getter.isExternal)
    assertFalse(::externalGetter.getter.isInline)

    assertFalse(::inlineProperty.getter.isExternal)
    assertTrue(::inlineProperty.getter.isInline)
    assertTrue(::inlineProperty.setter.isInline)

    return "OK"
}
