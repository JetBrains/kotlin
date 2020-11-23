// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: IGNORED_IN_JS
// !LANGUAGE: +NewInference +FunctionReferenceWithDefaultValueAsOtherType
// WITH_RUNTIME

import kotlin.test.assertEquals

class C(val expected: Int) {
    fun memberVararg(i: Int, vararg s: String) {
        assertEquals(expected, i)
        assertEquals(0, s.size)
    }

    fun memberDefault(i: Int, s: String = "") {
        assertEquals(expected, i)
        assertEquals("", s)
    }

    fun memberBoth(i: Int, s: String = "", vararg t: String) {
        assertEquals(expected, i)
        assertEquals("", s)
        assertEquals(0, t.size)
    }
}

fun C.extensionVararg(i: Int, vararg s: String) {
    memberVararg(i, *s)
}

fun C.extensionDefault(i: Int, s: String = "") {
    memberDefault(i, s)
}

fun C.extensionBoth(i: Int, s: String = "", vararg t: String) {
    memberBoth(i, s, *t)
}

fun test(f: (Int) -> Unit, p: Int) = f(p)

fun box(): String {
    test(C(42)::memberVararg, 42)
    test(C(42)::memberDefault, 42)
    test(C(42)::memberBoth, 42)
    test(C(42)::extensionVararg, 42)
    test(C(42)::extensionDefault, 42)
    test(C(42)::extensionBoth, 42)

    return "OK"
}
