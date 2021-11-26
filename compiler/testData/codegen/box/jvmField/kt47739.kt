// TARGET_BACKEND: JVM
// WITH_STDLIB

// MODULE: lib
// FILE: Value.kt
package vv

sealed class Value<T>(@JvmField val value: T) {
    class StringValue(value: String) : Value<String>(value)
    class BooleanValue(value: Boolean): Value<Boolean>(value)
}


// MODULE: main(lib)
// FILE: kt47739.kt
import kotlin.test.*
import vv.*

fun test(v: Value<*>) {
    when (v) {
        is Value.StringValue ->
            assertEquals("a string", v.value)
        is Value.BooleanValue ->
            assertEquals(true, v.value)
    }
}

fun box(): String {
    test(Value.StringValue("a string"))
    test(Value.BooleanValue(true))
    return "OK"
}
