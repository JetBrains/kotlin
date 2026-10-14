// LANGUAGE: +ContextParameters +CallableReferencesToContextual
// TARGET_BACKEND: JVM
// WITH_REFLECT
// ISSUE: KT-86452

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
        assertEquals("ctx3", f.call(intArrayOf(1, 2)))
        assertEquals("ctx0", f.callBy(emptyMap()))
        assertEquals("ctx6", f.callBy(mapOf(f.parameters[0] to intArrayOf(1, 2, 3))))
    }

    return "OK"
}
