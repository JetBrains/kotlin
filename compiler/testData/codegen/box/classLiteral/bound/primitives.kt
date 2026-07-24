// WITH_STDLIB

import kotlin.reflect.KClass
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

private const val isKotlinJs: Boolean = BACKEND_UNDER_TEST == "JS_IR" || BACKEND_UNDER_TEST == "JS_IR_ES6"

// Circumvent constant folding —
// 21::class is always replaced with Int::class by the compiler.
private fun checkErased(expectedClass: KClass<*>, expr: Any) {
    // Test the symmetry of `equals` for KClass
    assertEquals(expectedClass, expr::class)
    assertEquals(expr::class, expectedClass)

    assertEquals(expectedClass.hashCode(), expr::class.hashCode())
}

fun box(): String {
    assertEquals(Boolean::class, true::class)
    assertEquals(Byte::class, 42.toByte()::class)
    assertEquals(Char::class, 'z'::class)
    assertEquals(Double::class, 3.14::class)
    assertEquals(Float::class, 2.72f::class)
    assertEquals(Int::class, 42::class)
    assertEquals(Long::class, 42L::class)
    assertEquals(Short::class, 42.toShort()::class)

    assertEquals(Boolean::class.hashCode(), true::class.hashCode())
    assertEquals(Byte::class.hashCode(), 42.toByte()::class.hashCode())
    assertEquals(Char::class.hashCode(), 'z'::class.hashCode())
    assertEquals(Double::class.hashCode(), 3.14::class.hashCode())
    assertEquals(Float::class.hashCode(), 2.72f::class.hashCode())
    assertEquals(Int::class.hashCode(), 42::class.hashCode())
    assertEquals(Long::class.hashCode(), 42L::class.hashCode())
    assertEquals(Short::class.hashCode(), 42.toShort()::class.hashCode())

    checkErased(Boolean::class, true)
    checkErased(if (isKotlinJs) Int::class else Byte::class, 42.toByte())
    checkErased(Char::class, 'z')
    checkErased(Double::class, 3.14)
    checkErased(if (isKotlinJs) Double::class else Float::class, 2.72f)
    checkErased(Int::class, 42)
    checkErased(Long::class, 42L)
    checkErased(if (isKotlinJs) Int::class else Short::class, 42.toShort())

    return "OK"
}
