// TARGET_BACKEND: JVM
// WITH_REFLECT

// FILE: Final.java
public final class Final {}

// FILE: Open.java
public class Open {}

// FILE: Abstract.java
public abstract class Abstract {}

// FILE: Interface.java
public interface Interface {}

// FILE: Anno.java
public @interface Anno {}

// FILE: E.java
public enum E {
    ENTRY {
        void foo() {}
    }
}

// FILE: box.kt
import kotlin.test.assertTrue
import kotlin.test.assertFalse

fun box(): String {
    assertFalse(Final::class.isSealed)
    assertTrue(Final::class.isFinal)
    assertFalse(Final::class.isOpen)
    assertFalse(Final::class.isAbstract)

    assertFalse(Open::class.isSealed)
    assertFalse(Open::class.isFinal)
    assertTrue(Open::class.isOpen)
    assertFalse(Open::class.isAbstract)

    assertFalse(Abstract::class.isSealed)
    assertFalse(Abstract::class.isFinal)
    assertFalse(Abstract::class.isOpen)
    assertTrue(Abstract::class.isAbstract)

    assertFalse(Interface::class.isSealed)
    assertFalse(Interface::class.isFinal)
    assertFalse(Interface::class.isOpen)
    assertTrue(Interface::class.isAbstract)

    assertFalse(Anno::class.isSealed)
    assertTrue(Anno::class.isFinal)
    assertFalse(Anno::class.isOpen)
    assertFalse(Anno::class.isAbstract)

    assertFalse(E::class.isSealed)
    assertTrue(E::class.isFinal)
    assertFalse(E::class.isOpen)
    assertFalse(E::class.isAbstract)

    assertFalse(E.ENTRY::class.isSealed)
    if (System.getProperty("java.specification.version") == "1.8") {
        // Enum entry classes compiled by javac 8 have inconsistent modifiers: ACC_FINAL is on the class, but not in the InnerClasses entry.
        assertFalse(E.ENTRY::class.isFinal)
        assertTrue(E.ENTRY::class.isOpen)
    } else {
        assertTrue(E.ENTRY::class.isFinal)
        assertFalse(E.ENTRY::class.isOpen)
    }
    assertFalse(E.ENTRY::class.isAbstract)

    return "OK"
}
