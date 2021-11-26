// IGNORE_BACKEND: JS_IR
// IGNORE_BACKEND: JS_IR_ES6
// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS, NATIVE, WASM

// WITH_REFLECT

import kotlin.reflect.full.*
import kotlin.test.*

open class Super(val r: String)

class Sub(r: String) : Super(r)

fun box(): String {
    val props = Sub::class.declaredMemberProperties
    if (!props.isEmpty()) return "Fail $props"

    val allProps = Sub::class.memberProperties
    assertEquals(listOf("r"), allProps.map { it.name })
    return allProps.single().get(Sub("OK")) as String
}
