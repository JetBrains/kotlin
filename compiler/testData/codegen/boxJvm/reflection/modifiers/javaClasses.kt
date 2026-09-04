// TARGET_BACKEND: JVM
// WITH_REFLECT

// FILE: Interface.java
public interface Interface {
    int invoke(String s);
}

// FILE: Annotation.java
public @interface Annotation {

}

// FILE: J.java
public class J {
    public class Inner {}
    public static class Nested {}
    public enum Enum {
        ENTRY {

        }
    }
    public static final Object anonymousClass = new Object() {};
}

// FILE: box.kt

import kotlin.test.assertTrue
import kotlin.test.assertFalse

fun box(): String {
    assertFalse(Interface::class.isRegularClass)
    assertTrue(Interface::class.isInterface)
    assertFalse(Interface::class.isEnumClass)
    assertFalse(Interface::class.isData)
    assertFalse(Interface::class.isInner)
    assertFalse(Interface::class.isCompanion)
    assertFalse(Interface::class.isFun)
    assertFalse(Interface::class.isValue)

    assertFalse(Annotation::class.isRegularClass)
    assertFalse(Annotation::class.isInterface)
    assertFalse(Annotation::class.isEnumClass)
    assertFalse(Annotation::class.isData)
    assertFalse(Annotation::class.isInner)
    assertFalse(Annotation::class.isCompanion)
    assertFalse(Annotation::class.isFun)
    assertFalse(Annotation::class.isValue)

    assertTrue(J.Nested::class.isRegularClass)
    assertFalse(J.Nested::class.isInterface)
    assertFalse(J.Nested::class.isEnumClass)
    assertFalse(J.Nested::class.isData)
    assertFalse(J.Nested::class.isInner)
    assertFalse(J.Nested::class.isCompanion)
    assertFalse(J.Nested::class.isFun)
    assertFalse(J.Nested::class.isValue)

    assertTrue(J.Inner::class.isRegularClass)
    assertFalse(J.Inner::class.isInterface)
    assertFalse(J.Inner::class.isEnumClass)
    assertFalse(J.Inner::class.isData)
    assertTrue(J.Inner::class.isInner)
    assertFalse(J.Inner::class.isCompanion)
    assertFalse(J.Inner::class.isFun)
    assertFalse(J.Inner::class.isValue)

    assertFalse(J.Enum::class.isRegularClass)
    assertFalse(J.Enum::class.isInterface)
    assertTrue(J.Enum::class.isEnumClass)
    assertFalse(J.Enum::class.isData)
    assertFalse(J.Enum::class.isInner)
    assertFalse(J.Enum::class.isCompanion)
    assertFalse(J.Enum::class.isFun)
    assertFalse(J.Enum::class.isValue)

    assertFalse(J.Enum.ENTRY::class.isRegularClass)
    assertFalse(J.Enum.ENTRY::class.isInterface)
    assertFalse(J.Enum.ENTRY::class.isEnumClass)
    assertFalse(J.Enum.ENTRY::class.isData)
    assertFalse(J.Enum.ENTRY::class.isInner)
    assertFalse(J.Enum.ENTRY::class.isCompanion)
    assertFalse(J.Enum.ENTRY::class.isFun)
    assertFalse(J.Enum.ENTRY::class.isValue)

    assertFalse(J.anonymousClass::class.isRegularClass)
    assertFalse(J.anonymousClass::class.isInterface)
    assertFalse(J.anonymousClass::class.isEnumClass)
    assertFalse(J.anonymousClass::class.isData)
    assertFalse(J.anonymousClass::class.isInner)
    assertFalse(J.anonymousClass::class.isCompanion)
    assertFalse(J.anonymousClass::class.isFun)
    assertFalse(J.anonymousClass::class.isValue)

    return "OK"
}
