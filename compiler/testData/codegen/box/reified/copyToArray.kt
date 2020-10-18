// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: STDLIB_GENERATED
// KJS_WITH_FULL_RUNTIME
// WITH_RUNTIME

import kotlin.test.assertEquals

inline fun <reified T> copy(c: Collection<T>): Array<T> {
    return c.toTypedArray()
}

fun box(): String {
    val a: Array<String> = copy(listOf("a", "b", "c"))
    assertEquals("abc", a.joinToString(""))

    val b: Array<Int> = copy(listOf(1,2,3))
    assertEquals("123", b.map { it.toString() }.joinToString(""))

    return "OK"
}
