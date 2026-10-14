// TARGET_BACKEND: JVM
// WITH_REFLECT
// LANGUAGE: +ContextParameters +CallableReferencesToContextual

import kotlin.test.assertEquals

object Obj {
    @JvmStatic
    context(c: String)
    fun greet(arg: String): String = c + arg

    @JvmStatic
    context(c: String)
    val decorated: String get() = "[$c]"
}

class WithCompanion {
    companion object {
        @JvmStatic
        context(c: String)
        fun greet(arg: String): String = c + arg
    }
}

fun box(): String {
    context("ctx:") {
        assertEquals("ctx:arg", Obj::greet.call("arg"))
        assertEquals("ctx:arg", WithCompanion.Companion::greet.call("arg"))
        assertEquals("[ctx:]", Obj::decorated.call())
        assertEquals("[ctx:]", Obj::decorated.get())
    }
    return "OK"
}
