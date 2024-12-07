// TARGET_BACKEND: JVM
// WITH_STDLIB

import kotlin.test.assertEquals

class Klass

inline fun <reified T : Any> simpleName(): String =
        T::class.java.getSimpleName()

inline fun <reified T : Any> simpleName2(): String {
    val kClass = T::class // Intrinsic for T::class.java is not used
    return kClass.java.getSimpleName()
}


fun box(): String {
    assertEquals("Integer", simpleName<Int>())
    assertEquals("Integer", simpleName2<Int>())
    assertEquals("Klass", simpleName<Klass>())

    return "OK"
}
