import kotlin.reflect.jvm.*
import kotlin.test.*

open class Super(val r: String)

class Sub(r: String) : Super(r)

fun box(): String {
    val props = javaClass<Sub>().kotlin.getProperties()
    assertEquals(listOf("r"), props.map { it.name })
    return props.single().get(Sub("OK")).toString()
}
