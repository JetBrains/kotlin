// LANGUAGE: +ContextParameters +CallableReferencesToContextual
// TARGET_BACKEND: JVM_IR
// WITH_REFLECT
// ISSUE: KT-86452

import kotlin.jvm.internal.Reflection
import kotlin.reflect.KFunction
import kotlin.test.assertEquals

context(c: String)
fun <T> describe(x: T): String = c + x

context(c: C)
fun <C> contextToString(): String = c.toString()

class Box<T>(val v: T) {
    context(c: String)
    fun render(): String = c + v
}

fun box(): String {
    context("ctx") {
        // generic value parameter, T := Int from the expected type; the bound context argument is replayed by `call`
        val d: (Int) -> String = ::describe
        assertEquals("ctx1", (d as KFunction<*>).call(1))

        // member of a generic class, unbound dispatch receiver passed to `call`
        val r: (Box<Int>) -> String = Box<Int>::render
        assertEquals("ctx7", (r as KFunction<*>).call(Box(7)))
    }

    context(9) {
        // generic *context* parameter, C := Int, the bound context argument is replayed by `call`
        val cs: () -> String = ::contextToString
        assertEquals("9", (cs as KFunction<*>).call())
    }

    // unbound generic contextual functions through enumeration
    val members = Reflection.getOrCreateKotlinPackage(object {}::class.java.enclosingClass).members
    val describe = members.single { it.name == "describe" } as KFunction<*>
    assertEquals(listOf("T"), describe.typeParameters.map { it.name })
    assertEquals("A5", describe.call("A", 5))

    val cts = members.single { it.name == "contextToString" } as KFunction<*>
    assertEquals("true", cts.call(true))

    return "OK"
}
