// LANGUAGE: +ContextParameters +CallableReferencesToContextual +CompanionBlocks
// TARGET_BACKEND: JVM
// WITH_REFLECT
// ISSUE: KT-86452

import kotlin.test.assertEquals

class WithCompanionBlock {
    companion {
        context(c: String)
        fun greet(arg: String): String = c + arg

        context(c: String)
        val decorated: String
            get() = "[$c]"
    }
}

fun box(): String {
    context("ctx:") {
        val g = WithCompanionBlock::greet
        assertEquals("ctx:arg", g.call("arg"))

        val d = WithCompanionBlock::decorated
        assertEquals("[ctx:]", d.call())
        assertEquals("[ctx:]", d.getter.call())
    }
    return "OK"
}
