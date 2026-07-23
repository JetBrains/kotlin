// LANGUAGE: +ContextParameters +CallableReferencesToContextual
// TARGET_BACKEND: JVM_IR
// WITH_REFLECT
// ISSUE: KT-86452

import kotlin.reflect.KParameter
import kotlin.test.assertEquals
import kotlin.test.assertNull

context(c: String)
fun String.ext(y: Int): String = "$c-$this-$y"

fun box(): String {
    context("ctx") {
        val e = String::ext
        assertEquals(
            listOf(KParameter.Kind.EXTENSION_RECEIVER, KParameter.Kind.VALUE),
            e.parameters.map { it.kind },
        )
        // the extension receiver parameter has no name
        assertNull(e.parameters[0].name)
        assertEquals("ctx-R-1", e.call("R", 1))
    }
    return "OK"
}
