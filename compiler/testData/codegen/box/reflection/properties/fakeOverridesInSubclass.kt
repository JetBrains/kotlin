// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS

// WITH_REFLECT

import kotlin.reflect.*
import kotlin.test.*

open class Super(val r: String)

class Sub(r: String) : Super(r)

fun box(): String {
    val props = Sub::class.java.kotlin.declaredMemberProperties
    if (!props.isEmpty()) return "Fail $props"

    val allProps = Sub::class.java.kotlin.memberProperties
    assertEquals(listOf("r"), allProps.map { it.name })
    return allProps.single().get(Sub("OK")) as String
}
