// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// ISSUE: KT-86123

// Tests callable reference class deduplication with nested value classes.
// A value class wrapping another value class, which in turn wraps a primitive.
// Also tests value classes wrapping non-primitive types (String).

// --- Nested value class: Inner wraps primitive, Outer wraps Inner ---

OPTIONAL_JVM_INLINE_ANNOTATION
value class Inner(val x: Long)

OPTIONAL_JVM_INLINE_ANNOTATION
value class Outer(val i: Inner)

fun captureOuter(o: Outer): () -> String = { o.toString() }
fun captureOuterNullable(o: Outer?): () -> String = { o.toString() }
fun captureInner(i: Inner): () -> String = { i.toString() }
fun captureInnerNullable(i: Inner?): () -> String = { i.toString() }

// --- Value class wrapping non-primitive (String) ---

OPTIONAL_JVM_INLINE_ANNOTATION
value class WrapString(val s: String)

fun captureWrapString(w: WrapString): () -> String = { w.s }
fun captureWrapStringNullable(w: WrapString?): () -> String = { w?.s ?: "null" }

// --- Nested value class wrapping non-primitive ---

OPTIONAL_JVM_INLINE_ANNOTATION
value class WrapWrapString(val w: WrapString)

fun captureWrapWrapString(w: WrapWrapString): () -> String = { w.w.s }
fun captureWrapWrapStringNullable(w: WrapWrapString?): () -> String = { w?.w?.s ?: "null" }

fun box(): String {
    // Inner (value class wrapping Long): non-nullable vs nullable
    val i = captureInner(Inner(10))
    val iN = captureInnerNullable(Inner(10))
    val iNull = captureInnerNullable(null)
    if (i() != "Inner(x=10)") return "FAIL i: ${i()}"
    if (iN() != "Inner(x=10)") return "FAIL iN: ${iN()}"
    if (iNull() != "null") return "FAIL iNull: ${iNull()}"

    // Outer (value class wrapping Inner wrapping Long): non-nullable vs nullable
    val o = captureOuter(Outer(Inner(20)))
    val oN = captureOuterNullable(Outer(Inner(20)))
    val oNull = captureOuterNullable(null)
    if (o() != "Outer(i=Inner(x=20))") return "FAIL o: ${o()}"
    if (oN() != "Outer(i=Inner(x=20))") return "FAIL oN: ${oN()}"
    if (oNull() != "null") return "FAIL oNull: ${oNull()}"

    // WrapString (value class wrapping String/non-primitive): non-nullable vs nullable
    val ws = captureWrapString(WrapString("hello"))
    val wsN = captureWrapStringNullable(WrapString("hello"))
    val wsNull = captureWrapStringNullable(null)
    if (ws() != "hello") return "FAIL ws: ${ws()}"
    if (wsN() != "hello") return "FAIL wsN: ${wsN()}"
    if (wsNull() != "null") return "FAIL wsNull: ${wsNull()}"

    // WrapWrapString (nested value class wrapping non-primitive)
    val wws = captureWrapWrapString(WrapWrapString(WrapString("nested")))
    val wwsN = captureWrapWrapStringNullable(WrapWrapString(WrapString("nested")))
    val wwsNull = captureWrapWrapStringNullable(null)
    if (wws() != "nested") return "FAIL wws: ${wws()}"
    if (wwsN() != "nested") return "FAIL wwsN: ${wwsN()}"
    if (wwsNull() != "null") return "FAIL wwsNull: ${wwsNull()}"

    return "OK"
}
