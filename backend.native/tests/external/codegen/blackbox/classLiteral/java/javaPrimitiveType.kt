// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS

// WITH_RUNTIME

import kotlin.reflect.KClass

fun check(clazz: Class<*>?, expected: String) {
    assert (clazz!!.canonicalName == expected) {
        "clazz name: ${clazz.canonicalName}"
    }
}

fun check(kClass: KClass<*>, expected: String) {
    check(kClass.javaPrimitiveType, expected)
}

fun checkNull(clazz: Class<*>?) {
    assert (clazz == null) {
        "clazz should be null: ${clazz!!.canonicalName}"
    }
}

fun checkNull(kClass: KClass<*>) {
    checkNull(kClass.javaPrimitiveType)
}

fun box(): String {
    check(Boolean::class.javaPrimitiveType, "boolean")
    check(Boolean::class, "boolean")

    check(Char::class.javaPrimitiveType, "char")
    check(Char::class, "char")

    check(Byte::class.javaPrimitiveType, "byte")
    check(Byte::class, "byte")

    check(Short::class.javaPrimitiveType, "short")
    check(Short::class, "short")

    check(Int::class.javaPrimitiveType, "int")
    check(Int::class, "int")

    check(Float::class.javaPrimitiveType, "float")
    check(Float::class, "float")

    check(Long::class.javaPrimitiveType, "long")
    check(Long::class, "long")

    check(Double::class.javaPrimitiveType, "double")
    check(Double::class, "double")

    checkNull(String::class.javaPrimitiveType)
    checkNull(String::class)

    checkNull(Nothing::class.javaPrimitiveType)
    checkNull(Nothing::class)

    checkNull(Void::class.javaPrimitiveType)
    checkNull(Void::class)

    return "OK"
}
