// TARGET_BACKEND: JVM_IR

// WITH_STDLIB
// !LANGUAGE: +InstantiationOfAnnotationClasses

import kotlin.reflect.KClass
import kotlin.test.assertEquals
import kotlin.test.assertTrue as assert

annotation class ZeroArg()

annotation class OneArg(val arg: String)

annotation class ManyArg(val i: Int, val o: OneArg, val z: Boolean, val k: KClass<*>, val e: IntArray)

@ZeroArg
@OneArg("a")
@ManyArg(42, OneArg("b"), true, OneArg::class, intArrayOf(1, 2, 3))
class Target

fun box(): String {
    val reflectiveZero = Target::class.java.getAnnotation(ZeroArg::class.java)
    val reflectiveOne = Target::class.java.getAnnotation(OneArg::class.java)
    val reflectiveMany = Target::class.java.getAnnotation(ManyArg::class.java)

    val createdZero = ZeroArg()
    val createdOne = OneArg("a")
    val createdMany = ManyArg(42, OneArg("b"), true, OneArg::class, intArrayOf(1, 2, 3))

    assertEquals(reflectiveZero.hashCode(), createdZero.hashCode(), "zero")
    assertEquals(reflectiveOne.hashCode(), createdOne.hashCode(), "one")
    assertEquals(reflectiveMany.hashCode(), createdMany.hashCode(), "many")
    return "OK"
}
