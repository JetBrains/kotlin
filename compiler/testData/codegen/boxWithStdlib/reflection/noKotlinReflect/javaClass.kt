// NO_KOTLIN_REFLECT

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
    assertEquals("Klass", kkClass.simpleName)
    assertEquals(kjClass, jjClass)

    failsWith(Error::class.java) { kClass.simpleName!! }
    failsWith(Error::class.java) { kClass.qualifiedName!! }
    failsWith(Error::class.java) { kClass.members }

    val jlError = Error::class.java
    val kljError = Error::class
    val jljError = kljError.java
    val jlkError = jlError.kotlin

    assertEquals("Error", jlError.getSimpleName())
    assertEquals("Error", jljError.getSimpleName())
    assertEquals("Error", jlkError.simpleName)

    return "OK"
}
