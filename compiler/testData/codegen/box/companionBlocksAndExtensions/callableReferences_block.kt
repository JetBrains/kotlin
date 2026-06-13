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

    return "OK"
}

inline fun inline(s: () -> String): String = s()
inline fun inline(s: (String) -> String): String = s("OK")
