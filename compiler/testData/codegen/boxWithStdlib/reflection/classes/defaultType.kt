import kotlin.reflect.defaultType
import kotlin.reflect.jvm.javaType
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class Simple
class Generic<K, V> {
    fun thiz() = this
}

fun simple() = Simple()
fun genericIntString() = Generic<Int, String>()

fun box(): String {
    assertEquals(javaClass<Simple>(), Simple::class.defaultType.javaType)
    assertEquals(::simple.returnType, Simple::class.defaultType)

    assertEquals(javaClass<Generic<*, *>>(), Generic::class.defaultType.javaType)
    assertEquals(Generic<*, *>::thiz.returnType, Generic::class.defaultType)

    // Generic<Int, String> != Generic<K, V>
    assertNotEquals(::genericIntString.returnType, Generic::class.defaultType)

    return "OK"
}
