// LANGUAGE: +ContextParameters +CallableReferencesToContextual
// OPT_IN: kotlin.ExperimentalContextParameters
// TARGET_BACKEND: JVM_IR
// WITH_REFLECT
// ISSUE: KT-86452

import kotlin.reflect.KParameter
import kotlin.reflect.jvm.javaGetter
import kotlin.reflect.jvm.javaMethod
import kotlin.reflect.jvm.kotlinFunction
import kotlin.test.assertEquals

context(c: String)
fun target(x: Int): String = c + x

context(c: String)
val decorated: String get() = "[$c]"

fun box(): String {
    context("ctx") {
        val f = ::target
        val jm = f.javaMethod ?: return "FAIL 1: no javaMethod for a bound contextual function reference"
        // the context parameter is a leading JVM parameter
        assertEquals(listOf(String::class.java, Int::class.java), jm.parameterTypes.toList())
        assertEquals("X5", jm.invoke(null, "X", 5))

        // round trip: the Method maps back to the unbound contextual function
        val kf = jm.kotlinFunction ?: return "FAIL 2: no kotlinFunction for the JVM method of a contextual function"
        assertEquals(listOf(KParameter.Kind.CONTEXT, KParameter.Kind.VALUE), kf.parameters.map { it.kind })
        assertEquals("Y3", kf.call("Y", 3))

        val p = ::decorated
        val jg = p.javaGetter ?: return "FAIL 3: no javaGetter for a bound contextual property reference"
        assertEquals(listOf(String::class.java), jg.parameterTypes.toList())
        assertEquals("[Z]", jg.invoke(null, "Z"))
    }
    return "OK"
}
