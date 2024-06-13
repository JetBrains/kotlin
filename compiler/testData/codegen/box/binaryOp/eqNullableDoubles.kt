// LANGUAGE: +ProperIeee754Comparisons

fun eq_double_doubleN(a: Double, b: Double?) = a == b

fun eq_double_any(a: Double, b: Any) = a == b

fun eq_double_anyN(a: Double, b: Any?) = a == b

fun eq_doubleN_double(a: Double?, b: Double) = a == b

fun eq_doubleN_doubleN(a: Double?, b: Double?) = a == b

fun eq_doubleN_any(a: Double?, b: Any) = a == b

fun eq_doubleN_anyN(a: Double?, b: Any?) = a == b

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