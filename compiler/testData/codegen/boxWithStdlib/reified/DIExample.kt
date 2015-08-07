import kotlin.test.assertEquals

class Project {
    fun <T> getInstance(cls: Class<T>): T =
        when (cls.getName()) {
            "java.lang.Integer" -> 1 as T
            "java.lang.String" -> "OK" as T
            else -> null!!
        }
}

inline fun <reified T : Any> Project.get(t: Any?, p: PropertyMetadata): T = getInstance(javaClass<T>())

val project = Project()
val x1: Int by project
val x2: String by project

fun box(): String {
    assertEquals(1, x1)
    assertEquals("OK", x2)

    return "OK"
}
