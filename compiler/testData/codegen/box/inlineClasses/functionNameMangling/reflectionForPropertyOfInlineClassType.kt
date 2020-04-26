// !LANGUAGE: +InlineClasses
// WITH_RUNTIME
import kotlin.test.*

inline class S(val string: String)

var prop = S("")

fun box(): String {
    val propRef = ::prop

    assertEquals(S(""), propRef.get())

    propRef.set(S("abc"))
    assertEquals(S("abc"), propRef.get())

    assertEquals("prop", propRef.name)

    return "OK"
}
// DONT_TARGET_EXACT_BACKEND: WASM
 //DONT_TARGET_WASM_REASON: PROPERTY_REFERENCES
