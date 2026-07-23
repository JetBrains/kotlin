// LANGUAGE: +ContextParameters +CallableReferencesToContextual
// TARGET_BACKEND: JVM_IR
// WITH_REFLECT
// ISSUE: KT-86452

import kotlin.jvm.internal.Reflection
import kotlin.reflect.KFunction
import kotlin.test.assertEquals
import kotlin.test.assertTrue

context(c: String)
fun varargFun(vararg xs: Int): String {
    var sum = 0
    for (x in xs) sum += x
    return c + sum
}

fun box(): String {
    context("ctx") {
        val f = ::varargFun
        assertTrue(f.parameters.single().isVararg)
        // positional reflective call requires the whole vararg array
        assertEquals("ctx3", f.call(intArrayOf(1, 2)))
        // callBy: an absent vararg is replaced with an empty array (no default mask involved)
        assertEquals("ctx0", f.callBy(emptyMap()))
        assertEquals("ctx6", f.callBy(mapOf(f.parameters[0] to intArrayOf(1, 2, 3))))
    }

    // unbound through enumeration: the context argument is passed positionally before the vararg array
    val members = Reflection.getOrCreateKotlinPackage(object {}::class.java.enclosingClass).members
    val u = members.single { it.name == "varargFun" } as KFunction<*>
    assertEquals("A4", u.call("A", intArrayOf(4)))

    return "OK"
}
