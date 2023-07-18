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

fun getNull(): EI? =
    js("null")

fun getUndefined(): EI? =
    js("undefined")

// https://youtrack.jetbrains.com/issue/KT-59294/WASM-localStorage-Cannot-read-properties-of-undefined-reading-length
fun getStringUndefined(): String? =
    js("undefined")

fun isJsNull(ref: EI?): Boolean =
    js("ref === null")

fun isJsUndefined(ref: EI?): Boolean =
    js("ref === undefined")

fun getJsNullAsNonNullable(): EI =
    js("null")

fun getJsUndefinedAsNonNullable(): EI =
    js("undefined")

inline fun checkNPE(body: () -> Unit) {
    var throwed = false
    try {
        body()
    } catch (e: NullPointerException) {
        throwed = true
    }
    assertTrue(throwed)
}

fun box(): String {
    val jsNull = getNull()
    val jsUndefined = getUndefined()

    assertTrue(jsNull == null)
    assertTrue((jsNull as Any?) == null)
    assertTrue((jsNull as Any?) === null)
    assertTrue(jsUndefined == null)
    assertTrue(getStringUndefined() == null)

    assertTrue(isJsNull(null))
    assertTrue(isJsNull(null as EI?))
    assertTrue(isJsNull(null as? EI?))
    assertTrue(isJsNull(roundTrip(null)))
    assertTrue(isJsNull(jsNull))

    assertFalse(isJsUndefined(null))

    checkNPE(::getJsNullAsNonNullable)
    checkNPE(::getJsUndefinedAsNonNullable)

    return "OK"
}