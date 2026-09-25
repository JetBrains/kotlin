// IGNORE_BACKEND: JVM
// WITH_REFLECT

import kotlin.reflect.KClass

fun box(): String {
    val valueClasses = listOf<KClass<*>>(
        Int::class, Int::class.javaObjectType.kotlin, Long::class, Short::class, Byte::class, Char::class, Boolean::class, Float::class,
        Double::class, Number::class,
    )
    for (kClass in valueClasses) {
        if (!kClass.isValue) return "$kClass is not a value class"
    }
    for (kClass in listOf<KClass<*>>(Any::class, String::class, Comparable::class, Enum::class, Throwable::class, IntArray::class)) {
        if (kClass.isValue) return "$kClass is a value class"
    }
    return "OK"
}
