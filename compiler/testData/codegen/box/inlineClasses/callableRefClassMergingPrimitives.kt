// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// ISSUE: KT-86123

// Tests that callable reference class deduplication correctly handles primitives and unsigned types,
// particularly the nullable vs non-nullable variants. In Wasm, nullable primitives are boxed,
// so they must not be merged with their non-nullable (unboxed) counterparts.

// --- Primitives: nullable vs non-nullable ---

fun captureInt(n: Int): () -> String = { n.toString() }
fun captureIntNullable(n: Int?): () -> String = { n.toString() }

fun captureLong(n: Long): () -> String = { n.toString() }
fun captureLongNullable(n: Long?): () -> String = { n.toString() }

fun captureDouble(n: Double): () -> String = { n.toString() }
fun captureDoubleNullable(n: Double?): () -> String = { n.toString() }

fun captureBoolean(b: Boolean): () -> String = { b.toString() }
fun captureBooleanNullable(b: Boolean?): () -> String = { b.toString() }

// --- Unsigned types: nullable vs non-nullable ---

fun captureUInt(n: UInt): () -> String = { n.toString() }
fun captureUIntNullable(n: UInt?): () -> String = { n.toString() }

fun captureULong(n: ULong): () -> String = { n.toString() }
fun captureULongNullable(n: ULong?): () -> String = { n.toString() }

// --- Value class wrapping unsigned ---

OPTIONAL_JVM_INLINE_ANNOTATION
value class WrapUInt(val u: UInt)

fun captureWrapUInt(w: WrapUInt): () -> String = { w.toString() }
fun captureWrapUIntNullable(w: WrapUInt?): () -> String = { w.toString() }

fun box(): String {
    // Int nullable vs non-nullable
    val i = captureInt(10)
    val iN = captureIntNullable(10)
    val iNull = captureIntNullable(null)
    if (i() != "10") return "FAIL i: ${i()}"
    if (iN() != "10") return "FAIL iN: ${iN()}"
    if (iNull() != "null") return "FAIL iNull: ${iNull()}"

    // Long
    val l = captureLong(20L)
    val lN = captureLongNullable(20L)
    val lNull = captureLongNullable(null)
    if (l() != "20") return "FAIL l: ${l()}"
    if (lN() != "20") return "FAIL lN: ${lN()}"
    if (lNull() != "null") return "FAIL lNull: ${lNull()}"

    // Double
    val d = captureDouble(3.14)
    val dN = captureDoubleNullable(3.14)
    val dNull = captureDoubleNullable(null)
    if (d() != "3.14") return "FAIL d: ${d()}"
    if (dN() != "3.14") return "FAIL dN: ${dN()}"
    if (dNull() != "null") return "FAIL dNull: ${dNull()}"

    // Boolean
    val b = captureBoolean(true)
    val bN = captureBooleanNullable(true)
    val bNull = captureBooleanNullable(null)
    if (b() != "true") return "FAIL b: ${b()}"
    if (bN() != "true") return "FAIL bN: ${bN()}"
    if (bNull() != "null") return "FAIL bNull: ${bNull()}"

    // UInt
    val ui = captureUInt(42u)
    val uiN = captureUIntNullable(42u)
    val uiNull = captureUIntNullable(null)
    if (ui() != "42") return "FAIL ui: ${ui()}"
    if (uiN() != "42") return "FAIL uiN: ${uiN()}"
    if (uiNull() != "null") return "FAIL uiNull: ${uiNull()}"

    // ULong
    val ul = captureULong(99uL)
    val ulN = captureULongNullable(99uL)
    val ulNull = captureULongNullable(null)
    if (ul() != "99") return "FAIL ul: ${ul()}"
    if (ulN() != "99") return "FAIL ulN: ${ulN()}"
    if (ulNull() != "null") return "FAIL ulNull: ${ulNull()}"

    // Value class wrapping UInt
    val wu = captureWrapUInt(WrapUInt(7u))
    val wuN = captureWrapUIntNullable(WrapUInt(7u))
    val wuNull = captureWrapUIntNullable(null)
    if (wu() != "WrapUInt(u=7)") return "FAIL wu: ${wu()}"
    if (wuN() != "WrapUInt(u=7)") return "FAIL wuN: ${wuN()}"
    if (wuNull() != "null") return "FAIL wuNull: ${wuNull()}"

    return "OK"
}
