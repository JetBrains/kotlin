// LANGUAGE: +ContextParameters +CallableReferencesToContextual
// TARGET_BACKEND: JVM_IR
// WITH_REFLECT
// ISSUE: KT-86452

import kotlin.test.assertEquals

class WithCompanion {
    companion object {
        context(c: String)
        fun greet(arg: String): String = c + arg

        context(c: String)
        val decorated: String
            get() = "[$c]"
    }
}

fun box(): String {
    context("ctx:") {
        val g = WithCompanion.Companion::greet
        assertEquals("ctx:arg", g.call("arg"))

        val d = WithCompanion.Companion::decorated
        assertEquals("[ctx:]", d.call())
        assertEquals("[ctx:]", d.getter.call())
    }
    return "OK"
}
