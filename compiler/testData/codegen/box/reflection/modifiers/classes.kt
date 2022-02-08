// TARGET_BACKEND: JVM
// WITH_REFLECT
// FILE: box.kt

import kotlin.test.assertTrue
import kotlin.test.assertFalse

sealed class S {
    data class DataClass(val x: Int) : S()
    inner class InnerClass
    companion object
    object NonCompanionObject
    fun interface FunInterface { fun invoke() }
}

@JvmInline
value class V(val value: String)

fun box(): String {
    assertTrue(S::class.isSealed)
    assertFalse(S::class.isFinal)
    assertFalse(S::class.isOpen)
    assertFalse(S::class.isAbstract)
    assertFalse(S::class.isData)
    assertFalse(S::class.isInner)
    assertFalse(S::class.isCompanion)
    assertFalse(S::class.isFun)
    assertFalse(S::class.isValue)

    assertFalse(S.DataClass::class.isSealed)
    assertTrue(S.DataClass::class.isData)
    assertFalse(S.DataClass::class.isInner)
    assertFalse(S.DataClass::class.isCompanion)
    assertFalse(S.DataClass::class.isFun)
    assertFalse(S.DataClass::class.isValue)

    assertFalse(S.InnerClass::class.isSealed)
    assertFalse(S.InnerClass::class.isData)
    assertTrue(S.InnerClass::class.isInner)
    assertFalse(S.InnerClass::class.isCompanion)
    assertFalse(S.InnerClass::class.isFun)
    assertFalse(S.InnerClass::class.isValue)

    assertFalse(S.Companion::class.isSealed)
    assertFalse(S.Companion::class.isData)
    assertFalse(S.Companion::class.isInner)
    assertTrue(S.Companion::class.isCompanion)
    assertFalse(S.Companion::class.isFun)
    assertFalse(S.Companion::class.isValue)

    assertFalse(S.NonCompanionObject::class.isSealed)
    assertFalse(S.NonCompanionObject::class.isData)
    assertFalse(S.NonCompanionObject::class.isInner)
    assertFalse(S.NonCompanionObject::class.isCompanion)
    assertFalse(S.NonCompanionObject::class.isFun)
    assertFalse(S.NonCompanionObject::class.isValue)

    assertFalse(S.FunInterface::class.isSealed)
    assertFalse(S.FunInterface::class.isData)
    assertFalse(S.FunInterface::class.isInner)
    assertFalse(S.FunInterface::class.isCompanion)
    assertTrue(S.FunInterface::class.isFun)
    assertFalse(S.FunInterface::class.isValue)

    assertFalse(JavaInterface::class.isSealed)
    assertFalse(JavaInterface::class.isData)
    assertFalse(JavaInterface::class.isInner)
    assertFalse(JavaInterface::class.isCompanion)
    assertFalse(JavaInterface::class.isFun)
    assertFalse(JavaInterface::class.isValue)

    assertFalse(V::class.isSealed)
    assertFalse(V::class.isData)
    assertFalse(V::class.isInner)
    assertFalse(V::class.isCompanion)
    assertFalse(V::class.isFun)
    assertTrue(V::class.isValue)

    return "OK"
}

// FILE: JavaInterface.java

public interface JavaInterface {
    int invoke(String s);
}
