import kotlin.reflect.jvm.*
import kotlin.test.assertEquals

var topLevel = "123"

fun box(): String {
    val p = ::topLevel

    assert(p.javaField != null) { "Fail p field" }
    val field = p.javaField!!
    val className = field.getDeclaringClass().getName()
    assertEquals("TopLevelPropertyKt", className)

    val getter = p.javaGetter!!
    val setter = p.javaSetter!!

    assertEquals(getter, Class.forName("TopLevelPropertyKt").getMethod("getTopLevel"))
    assertEquals(setter, Class.forName("TopLevelPropertyKt").getMethod("setTopLevel", javaClass<String>()))

    assert(getter.invoke(null) == "123") { "Fail k getter" }
    setter.invoke(null, "456")
    assert(getter.invoke(null) == "456") { "Fail k setter" }

    return "OK"
}
