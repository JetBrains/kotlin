// TARGET_BACKEND: JVM

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

    assertEquals("[Ljava.lang.Integer;", arrayClass<Int>().jvmName)
    assertEquals("[Ljava.lang.Integer;", arrayClass<Int?>().jvmName)
    assertEquals("[[Ljava.lang.Integer;", arrayClass<Array<Int>>().jvmName)
    assertEquals("[LKlass;", arrayClass<Klass>().jvmName)
    assertEquals("[LKlass;", arrayClass<Klass?>().jvmName)
    assertEquals("[[LKlass;", arrayClass<Array<Klass>>().jvmName)
    assertEquals("[[LKlass;", arrayClass<Array<Klass?>>().jvmName)

    return "OK"
}
