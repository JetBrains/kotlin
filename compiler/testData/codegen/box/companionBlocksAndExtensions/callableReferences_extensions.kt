// WITH_STDLIB
// LANGUAGE: +CompanionBlocksAndExtensions
// DUMP_KLIB_ABI: DEFAULT
import kotlin.test.assertEquals
import kotlin.reflect.*

class C

companion fun C.fooExt(): String = "OK"
companion fun C.barExt(s: String) = s
companion fun C.bazExt(s: String) = s

companion val C.readonlyExt = "OK"
companion var C.mutableExt = "FAIL"

fun box(): String {
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
