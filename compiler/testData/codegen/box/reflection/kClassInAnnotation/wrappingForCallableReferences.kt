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

fun Anno.checkReference(expected: Any?, x: Anno.() -> Any?) {
    assertEquals(expected, x())
}

fun Anno.checkReferenceArray(expected: Any?, x: Anno.() -> Array<out Any?>) {
    assertEquals(expected, x()[0])
}

fun checkBoundReference(expected: Any?, x: () -> Any?) {
    assertEquals(expected, x())
}

fun checkBoundReferenceArray(expected: Any?, x: () -> Array<out Any?>) {
    assertEquals(expected, x()[0])
}

fun box(): String {
    val k = ::foo.annotations.single() as Anno
    k.checkReference(String::class, Anno::klass)
    k.checkReferenceArray(Int::class, Anno::kClasses)
    k.checkReferenceArray(Double::class, Anno::kClassesVararg)

    checkBoundReference(String::class, k::klass)
    checkBoundReferenceArray(Int::class, k::kClasses)
    checkBoundReferenceArray(Double::class, k::kClassesVararg)
    return "OK"
}
