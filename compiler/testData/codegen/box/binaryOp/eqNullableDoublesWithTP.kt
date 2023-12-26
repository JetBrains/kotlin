// !LANGUAGE: +ProperIeee754Comparisons

// JVM_ABI_K1_K2_DIFF: KT-63855

fun <A: Double, B: Double?> eq_double_doubleN(a: A, b: B) = a == b

fun <A: Double, B: Any> eq_double_any(a: A, b: B) = a == b

fun <A: Double, B: Any?> eq_double_anyN(a: A, b: B) = a == b

fun <A: Double?, B: Double> eq_doubleN_double(a: A, b: B) = a == b

fun <A: Double?, B: Double?> eq_doubleN_doubleN(a: A, b: B) = a == b

fun <A: Double?, B: Any> eq_doubleN_any(a: A, b: B) = a == b

fun <A: Double?, B: Any?> eq_doubleN_anyN(a: A, b: B) = a == b

fun box(): String {
    if (!eq_double_doubleN(0.0, -0.0)) throw AssertionError("!eq_double_doubleN(0.0, -0.0)")
    if (eq_double_doubleN(0.0, null)) throw AssertionError("eq_double_doubleN(0.0, null)")
    if (!eq_double_any(0.0, 0.0)) throw AssertionError("!eq_double_any(0.0, 0.0)")
    if (eq_double_any(0.0, -0.0)) throw AssertionError("eq_double_any(0.0, -0.0)")
    if (!eq_double_anyN(0.0, 0.0)) throw AssertionError("!eq_double_anyN(0.0, 0.0)")
    if (eq_double_anyN(0.0, -0.0)) throw AssertionError("eq_double_anyN(0.0, -0.0)")
    if (eq_double_anyN(0.0, null)) throw AssertionError("eq_double_anyN(0.0, null)")

    if (eq_doubleN_double(null, 0.0)) throw AssertionError("eq_doubleN_double(null, 0.0)")
    if (!eq_doubleN_doubleN(0.0, -0.0)) throw AssertionError("!eq_doubleN_doubleN(0.0, -0.0)")
    if (eq_doubleN_doubleN(0.0, null)) throw AssertionError("eq_doubleN_doubleN(0.0, null)")
    if (!eq_doubleN_any(0.0, 0.0)) throw AssertionError("!eq_doubleN_any(0.0, 0.0)")
    if (eq_doubleN_any(0.0, -0.0)) throw AssertionError("eq_doubleN_any(0.0, -0.0)")
    if (!eq_doubleN_anyN(0.0, 0.0)) throw AssertionError("!eq_doubleN_anyN(0.0, 0.0)")
    if (eq_doubleN_anyN(0.0, -0.0)) throw AssertionError("eq_doubleN_anyN(0.0, -0.0)")
    if (eq_doubleN_anyN(0.0, null)) throw AssertionError("eq_doubleN_anyN(0.0, null)")

    return "OK"
}