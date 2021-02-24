// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: SPREAD_OPERATOR
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

fun test(f: C.(Int) -> Unit, p: Int) = C(p).f(p)

fun box(): String {

    test(C::memberVararg, 43)
    test(C::memberDefault, 43)
    test(C::memberBoth, 43)
    test(C::extensionVararg, 43)
    test(C::extensionDefault, 43)
    test(C::extensionBoth, 43)

    return "OK"
}
