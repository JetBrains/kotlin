// IGNORE_BACKEND: JS_IR
// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS, NATIVE

// WITH_REFLECT

import kotlin.test.assertTrue
import kotlin.test.assertFalse

sealed class S {
    data class DataClass(val x: Int) : S()
    inner class InnerClass
    companion object
    object NonCompanionObject
}

fun box(): String {
    assertTrue(S::class.isSealed)
    assertFalse(S::class.isFinal)
    assertFalse(S::class.isOpen)
    assertFalse(S::class.isAbstract)
    assertFalse(S::class.isData)
    assertFalse(S::class.isInner)
    assertFalse(S::class.isCompanion)

    assertFalse(S.DataClass::class.isSealed)
    assertTrue(S.DataClass::class.isData)
    assertFalse(S.DataClass::class.isInner)
    assertFalse(S.DataClass::class.isCompanion)

    assertFalse(S.InnerClass::class.isSealed)
    assertFalse(S.InnerClass::class.isData)
    assertTrue(S.InnerClass::class.isInner)
    assertFalse(S.InnerClass::class.isCompanion)

    assertFalse(S.Companion::class.isSealed)
    assertFalse(S.Companion::class.isData)
    assertFalse(S.Companion::class.isInner)
    assertTrue(S.Companion::class.isCompanion)

    assertFalse(S.NonCompanionObject::class.isSealed)
    assertFalse(S.NonCompanionObject::class.isData)
    assertFalse(S.NonCompanionObject::class.isInner)
    assertFalse(S.NonCompanionObject::class.isCompanion)

    return "OK"
}
