// TARGET_BACKEND: JVM
// WITH_STDLIB

import kotlin.test.assertEquals
import kotlin.reflect.KProperty

class Project {
    fun <T> getInstance(cls: Class<T>): T =
        when (cls.getName()) {
            "java.lang.Integer" -> 1 as T
            "java.lang.String" -> "OK" as T
            else -> null!!
        }
}

inline operator fun <reified T : Any> Project.getValue(t: Any?, p: KProperty<*>): T = getInstance(T::class.java)

val project = Project()
val x1: Int by project
val x2: String by project

fun box(): String {
    assertEquals(1, x1)
    assertEquals("OK", x2)

    return "OK"
}
