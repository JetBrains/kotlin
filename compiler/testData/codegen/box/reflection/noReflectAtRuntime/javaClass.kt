// TARGET_BACKEND: JVM
// WITH_RUNTIME

import kotlin.test.*

class Klass

fun box(): String {
    val kClass = Klass::class
    val jClass = kClass.java
    val kjClass = Klass::class.java
    val kkClass = jClass.kotlin
    val jjClass = kkClass.java

    assertEquals("Klass", jClass.getSimpleName())
    assertEquals("Klass", kjClass.getSimpleName())
    assertEquals("Klass", kkClass.java.simpleName)
    assertEquals(kjClass, jjClass)

    try { kClass.simpleName; return "Fail 1" } catch (e: Error) {}
    try { kClass.qualifiedName; return "Fail 2" } catch (e: Error) {}
    try { kClass.members; return "Fail 3" } catch (e: Error) {}

    val jlError = Error::class.java
    val kljError = Error::class
    val jljError = kljError.java
    val jlkError = jlError.kotlin

    assertEquals("Error", jlError.getSimpleName())
    assertEquals("Error", jljError.getSimpleName())
    assertEquals("Error", jlkError.java.simpleName)

    return "OK"
}
