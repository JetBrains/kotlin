// TARGET_BACKEND: JVM

// WITH_RUNTIME

import kotlin.test.assertEquals

fun check(name: String, c: Class<*>) {
    assertEquals(name, c.simpleName)
}

fun box(): String {
    check("boolean", Boolean::class.java)
    check("byte", Byte::class.java)
    check("char", Char::class.java)
    check("short", Short::class.java)
    check("int", Int::class.java)
    check("float", Float::class.java)
    check("long", Long::class.java)
    check("double", Double::class.java)

    return "OK"
}
