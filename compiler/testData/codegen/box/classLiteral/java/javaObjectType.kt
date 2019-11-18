// IGNORE_BACKEND_FIR: JVM_IR
// TARGET_BACKEND: JVM

// WITH_RUNTIME

import kotlin.reflect.KClass

fun check(clazz: Class<*>?, expected: String) {
    assert (clazz!!.canonicalName == expected) {
        "clazz name: ${clazz.canonicalName}"
    }
}

fun check(kClass: KClass<*>, expected: String) {
    check(kClass.javaObjectType, expected)
}

fun box(): String {
    check(Boolean::class.javaObjectType, "java.lang.Boolean")
    check(Boolean::class, "java.lang.Boolean")

    check(Char::class.javaObjectType, "java.lang.Character")
    check(Char::class, "java.lang.Character")

    check(Byte::class.javaObjectType, "java.lang.Byte")
    check(Byte::class, "java.lang.Byte")

    check(Short::class.javaObjectType, "java.lang.Short")
    check(Short::class, "java.lang.Short")

    check(Int::class.javaObjectType, "java.lang.Integer")
    check(Int::class, "java.lang.Integer")

    check(Float::class.javaObjectType, "java.lang.Float")
    check(Float::class, "java.lang.Float")

    check(Long::class.javaObjectType, "java.lang.Long")
    check(Long::class, "java.lang.Long")

    check(Double::class.javaObjectType, "java.lang.Double")
    check(Double::class, "java.lang.Double")

    check(String::class.javaObjectType, "java.lang.String")
    check(String::class, "java.lang.String")

    check(Nothing::class.javaObjectType, "java.lang.Void")
    check(Nothing::class, "java.lang.Void")

    check(Void::class.javaObjectType, "java.lang.Void")
    check(Void::class, "java.lang.Void")

    return "OK"
}
