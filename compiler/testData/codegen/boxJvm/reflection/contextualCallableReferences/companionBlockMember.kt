// LANGUAGE: +ContextParameters +CallableReferencesToContextual +CompanionBlocks +CompanionExtensions
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

interface WithInterfaceCompanionBlock {
    companion {
        context(c: String)
        fun greet(arg: String): String = "interface:" + c + arg
    }
}

context(c: String)
companion fun WithCompanionBlock.extGreet(arg: String): String = "ext:" + c + arg

fun box(): String {
    context("ctx:") {
        val g = WithCompanionBlock::greet
        assertEquals("ctx:arg", g.call("arg"))

        val d = WithCompanionBlock::decorated
        assertEquals("[ctx:]", d.call())
        assertEquals("[ctx:]", d.getter.call())

        assertEquals("interface:ctx:arg", WithInterfaceCompanionBlock::greet.call("arg"))
        assertEquals("ext:ctx:arg", WithCompanionBlock::extGreet.call("arg"))
    }
    return "OK"
}
