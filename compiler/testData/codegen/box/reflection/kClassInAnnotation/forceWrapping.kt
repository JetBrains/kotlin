// TARGET_BACKEND: JVM
// WITH_REFLECT

import kotlin.reflect.KClass
import kotlin.test.assertEquals

annotation class Anno(
        val klass: KClass<*>,
        val kClasses: Array<KClass<*>>,
        vararg val kClassesVararg: KClass<*>
)

@Anno(String::class, arrayOf(Int::class), Double::class)
fun foo() {}

fun box(): String {
    val k = ::foo.annotations.single() as Anno
    assertEquals(String::class, k.klass)
    assertEquals(Int::class, k.kClasses[0])
    assertEquals(Double::class, k.kClassesVararg[0])
    return "OK"
}
