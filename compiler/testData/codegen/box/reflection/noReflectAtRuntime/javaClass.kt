// IGNORE_BACKEND_FIR: JVM_IR
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
    assertEquals("Klass", kClass.simpleName)
    assertEquals(kjClass, jjClass)

    try { kClass.members; return "Fail members" } catch (e: Error) {}

    val jlError = Error::class.java
    val kljError = Error::class
    val jljError = kljError.java
    val jlkError = jlError.kotlin

    assertEquals("Error", jlError.getSimpleName())
    assertEquals("Error", jljError.getSimpleName())
    assertEquals("Error", jlkError.java.simpleName)
    assertEquals("Error", kljError.simpleName)

    return "OK"
}
