// TARGET_BACKEND: JVM
// WITH_REFLECT
// FILE: box.kt

import kotlin.test.assertTrue
import kotlin.test.assertFalse

sealed class S {
    data class DataClass(val x: Int) : S()
    data object DataObject
    inner class InnerClass
    companion object {
        val anonymousObject = object : Any() {}
    }
    object RegularObject
    fun interface FunInterface { fun invoke() }
    annotation class Annotation
}

@JvmInline
value class V(val value: String)

fun box(): String {
    assertTrue(S::class.isRegularClass)
    assertFalse(S::class.isInterface)
    assertFalse(S::class.isEnumClass)
    assertTrue(S::class.isSealed)
    assertFalse(S::class.isFinal)
    assertFalse(S::class.isOpen)
    assertFalse(S::class.isAbstract)
    assertFalse(S::class.isData)
    assertFalse(S::class.isInner)
    assertFalse(S::class.isCompanion)
    assertFalse(S::class.isFun)
    assertFalse(S::class.isValue)

    assertTrue(S.DataClass::class.isRegularClass)
    assertFalse(S.DataClass::class.isInterface)
    assertFalse(S.DataClass::class.isEnumClass)
    assertFalse(S.DataClass::class.isSealed)
    assertTrue(S.DataClass::class.isData)
    assertFalse(S.DataClass::class.isInner)
    assertFalse(S.DataClass::class.isCompanion)
    assertFalse(S.DataClass::class.isFun)
    assertFalse(S.DataClass::class.isValue)

    assertFalse(S.DataObject::class.isRegularClass)
    assertFalse(S.DataObject::class.isInterface)
    assertFalse(S.DataObject::class.isEnumClass)
    assertFalse(S.DataObject::class.isSealed)
    assertTrue(S.DataObject::class.isData)
    assertFalse(S.DataObject::class.isInner)
    assertFalse(S.DataObject::class.isCompanion)
    assertFalse(S.DataObject::class.isFun)
    assertFalse(S.DataObject::class.isValue)

    assertTrue(S.InnerClass::class.isRegularClass)
    assertFalse(S.InnerClass::class.isInterface)
    assertFalse(S.InnerClass::class.isEnumClass)
    assertFalse(S.InnerClass::class.isSealed)
    assertFalse(S.InnerClass::class.isData)
    assertTrue(S.InnerClass::class.isInner)
    assertFalse(S.InnerClass::class.isCompanion)
    assertFalse(S.InnerClass::class.isFun)
    assertFalse(S.InnerClass::class.isValue)

    assertFalse(S.Companion::class.isRegularClass)
    assertFalse(S.Companion::class.isInterface)
    assertFalse(S.Companion::class.isEnumClass)
    assertFalse(S.Companion::class.isSealed)
    assertFalse(S.Companion::class.isData)
    assertFalse(S.Companion::class.isInner)
    assertTrue(S.Companion::class.isCompanion)
    assertFalse(S.Companion::class.isFun)
    assertFalse(S.Companion::class.isValue)

    assertFalse(S.RegularObject::class.isRegularClass)
    assertFalse(S.RegularObject::class.isInterface)
    assertFalse(S.RegularObject::class.isEnumClass)
    assertFalse(S.RegularObject::class.isSealed)
    assertFalse(S.RegularObject::class.isData)
    assertFalse(S.RegularObject::class.isInner)
    assertFalse(S.RegularObject::class.isCompanion)
    assertFalse(S.RegularObject::class.isFun)
    assertFalse(S.RegularObject::class.isValue)

    assertFalse(S.FunInterface::class.isRegularClass)
    assertTrue(S.FunInterface::class.isInterface)
    assertFalse(S.FunInterface::class.isEnumClass)
    assertFalse(S.FunInterface::class.isSealed)
    assertFalse(S.FunInterface::class.isData)
    assertFalse(S.FunInterface::class.isInner)
    assertFalse(S.FunInterface::class.isCompanion)
    assertTrue(S.FunInterface::class.isFun)
    assertFalse(S.FunInterface::class.isValue)

    assertFalse(JavaInterface::class.isRegularClass)
    assertTrue(JavaInterface::class.isInterface)
    assertFalse(JavaInterface::class.isEnumClass)
    assertFalse(JavaInterface::class.isSealed)
    assertFalse(JavaInterface::class.isData)
    assertFalse(JavaInterface::class.isInner)
    assertFalse(JavaInterface::class.isCompanion)
    assertFalse(JavaInterface::class.isFun)
    assertFalse(JavaInterface::class.isValue)

    assertFalse(S.Annotation::class.isRegularClass)
    assertFalse(S.Annotation::class.isInterface)
    assertFalse(S.Annotation::class.isEnumClass)
    assertFalse(S.Annotation::class.isSealed)
    assertFalse(S.Annotation::class.isData)
    assertFalse(S.Annotation::class.isInner)
    assertFalse(S.Annotation::class.isCompanion)
    assertFalse(S.Annotation::class.isFun)
    assertFalse(S.Annotation::class.isValue)

    assertFalse(S.anonymousObject::class.isRegularClass)
    assertFalse(S.anonymousObject::class.isInterface)
    assertFalse(S.anonymousObject::class.isEnumClass)
    assertFalse(S.anonymousObject::class.isSealed)
    assertFalse(S.anonymousObject::class.isData)
    assertFalse(S.anonymousObject::class.isInner)
    assertFalse(S.anonymousObject::class.isCompanion)
    assertFalse(S.anonymousObject::class.isFun)
    assertFalse(S.anonymousObject::class.isValue)

    assertTrue(V::class.isRegularClass)
    assertFalse(V::class.isInterface)
    assertFalse(V::class.isEnumClass)
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
