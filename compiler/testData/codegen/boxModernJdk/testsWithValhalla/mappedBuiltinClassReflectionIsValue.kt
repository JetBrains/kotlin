// WITH_REFLECT

import kotlin.reflect.KClass

fun box(): String {
    if (!Number::class.isValue) return "Fail: Number is not a value class"
    val nonValueClasses = listOf<KClass<*>>(
        Int::class, Int::class.javaObjectType.kotlin, Long::class, Short::class, Byte::class, Char::class, Boolean::class, Float::class,
        Double::class, Any::class, String::class, Comparable::class, Enum::class, Throwable::class, IntArray::class,
    )
    for (kClass in nonValueClasses) {
        if (kClass.isValue) return "Fail: $kClass is a value class"
    }
    return "OK"
}
