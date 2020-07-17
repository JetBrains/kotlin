// TARGET_BACKEND: JVM
// WITH_REFLECT
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.javaConstructor
import kotlin.reflect.jvm.kotlinFunction
import kotlin.test.assertEquals

inline class Z(val x: Int)

class Test(val x: Z)

fun box(): String {
    val kctor1 = Test::class.primaryConstructor ?: throw AssertionError("No primary constructor")
    val jctor1 = kctor1.javaConstructor ?: throw AssertionError("No javaConstructor for $kctor1")
    val kctor2 = jctor1.kotlinFunction ?: throw AssertionError("No kotlinFunction for $jctor1")

    assertEquals(kctor1, kctor2)
    assertEquals("[x]", kctor2.parameters.map { it.name }.toString())

    return "OK"
}