// TARGET_BACKEND: JVM
// WITH_REFLECT
// FILE: J.java
public class J {
    public J() {}
    public static native void external();
}

// FILE: JImpl.java
public class JImpl extends J {}

// FILE: box.kt
import kotlin.test.assertTrue
import kotlin.test.assertFalse

inline fun inline() {}
class External { external fun external() }
operator fun Unit.invoke() {}
infix fun Unit.infix(unit: Unit) {}
class Suspend { suspend fun suspend() {} }

var externalGetter = Unit
    external get

inline var inlineProperty: Unit
    get() = Unit
    set(value) {}

var inlineGetter: Unit
    inline get() = Unit
    set(value) {}

var inlineSetter: Unit
    get() = Unit
    inline set(value) {}

class Ctor

fun box(): String {
    assertTrue(::inline.isInline)
    assertFalse(::inline.isExternal)
    assertFalse(::inline.isOperator)
    assertFalse(::inline.isInfix)
    assertFalse(::inline.isSuspend)

    assertFalse(External::external.isInline)
    assertTrue(External::external.isExternal)
    assertFalse(External::external.isOperator)
    assertFalse(External::external.isInfix)
    assertFalse(External::external.isSuspend)

    assertFalse(Unit::invoke.isInline)
    assertFalse(Unit::invoke.isExternal)
    assertTrue(Unit::invoke.isOperator)
    assertFalse(Unit::invoke.isInfix)
    assertFalse(Unit::invoke.isSuspend)

    assertFalse(Unit::infix.isInline)
    assertFalse(Unit::infix.isExternal)
    assertFalse(Unit::infix.isOperator)
    assertTrue(Unit::infix.isInfix)
    assertFalse(Unit::infix.isSuspend)

    assertFalse(Suspend::suspend.isInline)
    assertFalse(Suspend::suspend.isExternal)
    assertFalse(Suspend::suspend.isOperator)
    assertFalse(Suspend::suspend.isInfix)
    assertTrue(Suspend::suspend.isSuspend)

    assertTrue(::externalGetter.getter.isExternal)
    assertFalse(::externalGetter.getter.isInline)
    assertFalse(::externalGetter.setter.isExternal)

    assertFalse(::inlineProperty.getter.isExternal)
    assertFalse(::inlineProperty.setter.isExternal)
    assertTrue(::inlineProperty.getter.isInline)
    assertTrue(::inlineProperty.setter.isInline)
    assertFalse(::inlineProperty.isSuspend)

    assertTrue(::inlineGetter.getter.isInline)
    assertFalse(::inlineGetter.setter.isInline)
    assertFalse(::inlineSetter.getter.isInline)
    assertTrue(::inlineSetter.setter.isInline)

    for (p in listOf(::externalGetter, ::inlineProperty, ::inlineGetter, ::inlineSetter)) {
        assertFalse(p.getter.isOperator)
        assertFalse(p.setter.isOperator)
        assertFalse(p.getter.isInfix)
        assertFalse(p.setter.isInfix)
        assertFalse(p.getter.isSuspend)
        assertFalse(p.setter.isSuspend)
    }

    assertFalse(::J.isInline)
    assertFalse(::J.isExternal)
    assertFalse(::J.isOperator)
    assertFalse(::J.isInfix)
    assertFalse(::J.isSuspend)

    assertFalse(J::external.isInline)
    assertTrue(J::external.isExternal)
    assertFalse(J::external.isOperator)
    assertFalse(J::external.isInfix)
    assertFalse(J::external.isSuspend)
    assertTrue(JImpl::external.isExternal)

    assertFalse(::Ctor.isInline)
    assertFalse(::Ctor.isExternal)
    assertFalse(::Ctor.isOperator)
    assertFalse(::Ctor.isInfix)
    assertFalse(::Ctor.isSuspend)

    return "OK"
}
