import kotlin.reflect.jvm.*
import kotlin.test.assertEquals

fun free(s: String): Int = s.length()

fun Any.extension() {}

fun box(): String {
    assertEquals(java.lang.Integer.TYPE, ::free.returnType.javaType)
    assertEquals(javaClass<String>(), ::free.parameters.single().type.javaType)

    assertEquals(javaClass<Any>(), Any::extension.parameters.single().type.javaType)

    return "OK"
}
