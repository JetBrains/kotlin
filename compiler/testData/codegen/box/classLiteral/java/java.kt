// IGNORE_BACKEND_FIR: JVM_IR
// TARGET_BACKEND: JVM

// WITH_RUNTIME

import kotlin.reflect.KClass

fun checkPrimitive(clazz: Class<*>, expected: String) {
    assert (clazz!!.canonicalName == expected) {
        "clazz name: ${clazz.canonicalName}"
    }
}

fun checkPrimitive(kClass: KClass<*>, expected: String) {
    checkPrimitive(kClass.java, expected)
}

fun checkObject(clazz: Class<*>, expected: String) {
    assert (clazz.canonicalName == "$expected") {
        "clazz should be object, but found: ${clazz!!.canonicalName}"
    }
}

fun checkObject(kClass: KClass<*>, expected: String) {
    checkObject(kClass.java, expected)
}

fun box(): String {
    checkPrimitive(Boolean::class.java, "boolean")
    checkPrimitive(Boolean::class, "boolean")

    checkPrimitive(Char::class.java, "char")
    checkPrimitive(Char::class, "char")

    checkPrimitive(Byte::class.java, "byte")
    checkPrimitive(Byte::class, "byte")

    checkPrimitive(Short::class.java, "short")
    checkPrimitive(Short::class, "short")

    checkPrimitive(Int::class.java, "int")
    checkPrimitive(Int::class, "int")

    checkPrimitive(Float::class.java, "float")
    checkPrimitive(Float::class, "float")

    checkPrimitive(Long::class.java, "long")
    checkPrimitive(Long::class, "long")

    checkPrimitive(Double::class.java, "double")
    checkPrimitive(Double::class, "double")

    checkObject(String::class.java, "java.lang.String")
    checkObject(String::class, "java.lang.String")

    checkObject(Nothing::class.java, "java.lang.Void")
    checkObject(Nothing::class, "java.lang.Void")

    checkObject(java.lang.Void::class.java, "java.lang.Void")
    checkObject(java.lang.Void::class, "java.lang.Void")

    return "OK"
}
