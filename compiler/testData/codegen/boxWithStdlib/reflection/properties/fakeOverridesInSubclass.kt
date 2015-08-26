import kotlin.reflect.*
import kotlin.test.*

open class Super(val r: String)

class Sub(r: String) : Super(r)

fun box(): String {
    val props = javaClass<Sub>().kotlin.declaredMemberProperties
    if (!props.isEmpty()) return "Fail $props"

    val allProps = javaClass<Sub>().kotlin.memberProperties
    assertEquals(listOf("r"), allProps.map { it.name })
    return allProps.single().get(Sub("OK")) as String
}
