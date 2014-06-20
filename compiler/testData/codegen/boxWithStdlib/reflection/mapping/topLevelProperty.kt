import kotlin.reflect.jvm.*
import kotlin.test.assertEquals

var topLevel = "123"

fun box(): String {
    val p = ::topLevel

    // TODO: uncomment when fields are loaded from package parts
    // assert(p.javaField != null, "Fail p field")

    val getter = p.javaGetter!!
    val setter = p.javaSetter!!

    assertEquals(getter, Class.forName("_DefaultPackage").getMethod("getTopLevel"))
    assertEquals(setter, Class.forName("_DefaultPackage").getMethod("setTopLevel", javaClass<String>()))

    assert(getter.invoke(null) == "123", "Fail k getter")
    setter.invoke(null, "456")
    assert(getter.invoke(null) == "456", "Fail k setter")

    return "OK"
}
