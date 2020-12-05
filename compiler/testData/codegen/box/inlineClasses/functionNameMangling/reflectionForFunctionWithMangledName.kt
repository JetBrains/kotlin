// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: BRIDGE_ISSUES
// !LANGUAGE: +InlineClasses
// WITH_RUNTIME
import kotlin.test.*

inline class S(val string: String)

fun foo(s: S) = s

fun box(): String {
    val fooRef = ::foo

    assertEquals("abc", fooRef.invoke(S("abc")).string)
    assertEquals("foo", fooRef.name)

    return "OK"
}