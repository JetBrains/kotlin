import kotlin.reflect.jvm.*
import kotlin.reflect.*
import kotlin.test.*

open class Super(val r: String)

class Sub(r: String) : Super(r)

fun box(): String {
    val props = javaClass<Sub>().kotlin.declaredProperties
    if (!props.isEmpty()) return "Fail $props"

    val allProps = javaClass<Sub>().kotlin.properties
    assertEquals(listOf("r"), allProps.map { it.name })
    return allProps.single().get(Sub("OK")) as String
}
