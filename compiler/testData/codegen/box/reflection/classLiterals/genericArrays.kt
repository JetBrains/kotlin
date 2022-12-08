// TARGET_BACKEND: JVM
// IGNORE_INLINER: IR

// WITH_REFLECT

import kotlin.test.*
import kotlin.reflect.*
import kotlin.reflect.jvm.*

class Klass

inline fun <reified T> arrayClass(): KClass<Array<T>> = Array<T>::class

fun box(): String {
    assertEquals("Array", arrayClass<Int>().simpleName)
    assertEquals("Array", arrayClass<Int?>().simpleName)
    assertEquals("Array", arrayClass<Array<Int>>().simpleName)
    assertEquals("Array", arrayClass<Klass>().simpleName)
    assertEquals("Array", arrayClass<Klass?>().simpleName)
    assertEquals("Array", arrayClass<Array<Klass>>().simpleName)
    assertEquals("Array", arrayClass<Array<Klass?>>().simpleName)

    // Should not be that way. Fix this test when backend is fixed.
    assertEquals("[Ljava.lang.Object;", arrayClass<Int>().jvmName)
    assertEquals("[Ljava.lang.Object;", arrayClass<Int?>().jvmName)
    assertEquals("[Ljava.lang.Object;", arrayClass<Array<Int>>().jvmName)
    assertEquals("[Ljava.lang.Object;", arrayClass<Klass>().jvmName)
    assertEquals("[Ljava.lang.Object;", arrayClass<Klass?>().jvmName)
    assertEquals("[Ljava.lang.Object;", arrayClass<Array<Klass>>().jvmName)
    assertEquals("[Ljava.lang.Object;", arrayClass<Array<Klass?>>().jvmName)

    return "OK"
}
