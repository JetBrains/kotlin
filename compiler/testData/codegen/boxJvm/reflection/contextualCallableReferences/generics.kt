// LANGUAGE: +ContextParameters +CallableReferencesToContextual
// TARGET_BACKEND: JVM
// WITH_REFLECT
// ISSUE: KT-86452

import kotlin.reflect.KFunction
import kotlin.reflect.jvm.javaMethod
import kotlin.reflect.jvm.kotlinFunction
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
        val d: (Int) -> String = ::describe
        assertEquals("ctx1", (d as KFunction<*>).call(1))

        val describe = d.javaMethod!!.kotlinFunction!!
        assertEquals(listOf("T"), describe.typeParameters.map { it.name })
        assertEquals("A5", describe.call("A", 5))

        val r: (Box<Int>) -> String = Box<Int>::render
        assertEquals("ctx7", (r as KFunction<*>).call(Box(7)))
    }

    context(9) {
        val cs: () -> String = ::contextToString
        assertEquals("9", (cs as KFunction<*>).call())

        val cts = cs.javaMethod!!.kotlinFunction!!
        assertEquals("true", cts.call(true))
    }

    return "OK"
}
