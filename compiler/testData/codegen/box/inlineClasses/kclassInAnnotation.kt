// WITH_REFLECT
// TARGET_BACKEND: JVM
package test

import kotlin.reflect.KClass

inline class IC(val i: Int)

annotation class Ann(val c: KClass<*>)

@Ann(IC::class)
class C

fun box(): String {
    val klass = (C::class.annotations.first() as Ann).c.toString()
    return if (klass == "class test.IC") "OK" else klass
}