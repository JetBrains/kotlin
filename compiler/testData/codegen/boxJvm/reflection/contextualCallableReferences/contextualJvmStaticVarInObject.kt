// LANGUAGE: +ContextParameters +CallableReferencesToContextual
// TARGET_BACKEND: JVM_IR
// WITH_REFLECT
// ISSUE: KT-86452

import kotlin.test.assertEquals

var storage = ""

object Obj {
    @JvmStatic
    context(c: String)
    var prop: String
        get() = storage
        set(value) {
            storage = c + value
        }
}

fun box(): String {
    context("ctx-") {
        val p = Obj::prop
        p.setter.call("V")
        assertEquals("ctx-V", p.getter.call())
        assertEquals("ctx-V", p.call())
    }
    return "OK"
}
