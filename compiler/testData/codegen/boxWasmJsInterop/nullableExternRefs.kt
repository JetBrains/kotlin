// TARGET_BACKEND: WASM
// WITH_STDLIB

// FILE: externals.js

function roundTrip(x) { return x; }

// FILE: externals.kt

external fun roundTrip(x: EI?): EI?

fun assertTrue(x: Boolean) {
    if (!x) error("assertTrue fail")
}

fun assertFalse(x: Boolean) {
    if (x) error("assertFalse fail")
}

external interface EI

@JsFun("() => null")
external fun getNull(): EI?

@JsFun("() => undefined")
external fun getUndefined(): EI?

@JsFun("(ref) => ref === null")
external fun isJsNull(ref: EI?): Boolean

@JsFun("(ref) => ref === undefined")
external fun isJsUndefined(ref: EI?): Boolean

@JsFun("() => null")
external fun getJsNullAsNonNullable(): EI

@JsFun("() => undefined")
external fun getJsUndefinedAsNonNullable(): EI

fun box(): String {
    val jsNull = getNull()
    val jsUndefined = getUndefined()

    assertTrue(jsNull == null)
    assertTrue((jsNull as Any?) == null)
    assertTrue((jsNull as Any?) === null)
    assertTrue(jsUndefined == null)

    assertTrue(isJsNull(null))
    assertTrue(isJsNull(null as EI?))
    assertTrue(isJsNull(null as? EI?))
    assertTrue(isJsNull(roundTrip(null)))
    assertTrue(isJsNull(jsNull))

    assertFalse(isJsUndefined(null))
    assertTrue(isJsUndefined(jsUndefined))

    // TODO: Should these fail?
    val n2 = getJsNullAsNonNullable()
    val ud2 = getJsUndefinedAsNonNullable()
    assertTrue(isJsNull(n2))
    assertTrue(isJsUndefined(ud2))

    return "OK"
}