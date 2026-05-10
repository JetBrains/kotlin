// WITH_STDLIB
// LANGUAGE: +CompanionBlocksAndExtensions
// DUMP_KLIB_ABI: DEFAULT
// IGNORE_BACKEND: WASM_JS, WASM_WASI, JS_IR, JS_IR_ES6
import kotlin.test.assertEquals
import kotlin.reflect.*

class C {
    fun baz(x: Int) {}

    companion {
        fun foo(): String = "OK"
        fun bar(s: String) = s
        fun baz(s: String) = s

        val readonly = "OK"
        var mutable = "FAIL"
    }
}

companion fun C.fooExt(): String = "OK"
companion fun C.barExt(s: String) = s
companion fun C.bazExt(s: String) = s

companion val C.readonlyExt = "OK"
companion var C.mutableExt = "FAIL"

fun box(): String {
    assertEquals("OK", (C::foo)())
    assertEquals("OK", inline(C::foo))
    assertEquals("OK", (C::bar)("OK"))
    assertEquals("OK", inline(C::bar))
    assertEquals("OK", inline(C::baz))

    assertEquals("OK", (C::readonly)())
    assertEquals("OK", inline(C::readonly))

    C::mutable.set("OK")

    assertEquals("OK", (C::mutable)())
    assertEquals("OK", inline(C::mutable))

    assertEquals("OK", (C::fooExt)())
    assertEquals("OK", inline(C::fooExt))
    assertEquals("OK", (C::barExt)("OK"))
    assertEquals("OK", inline(C::barExt))
    assertEquals("OK", inline(C::bazExt))

    assertEquals("OK", (C::readonlyExt)())
    assertEquals("OK", inline(C::readonlyExt))

    C::mutableExt.set("OK")

    assertEquals("OK", (C::mutableExt)())
    assertEquals("OK", inline(C::mutableExt))

    return "OK"
}

inline fun inline(s: () -> String): String = s()
inline fun inline(s: (String) -> String): String = s("OK")
