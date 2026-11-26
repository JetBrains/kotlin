// LANGUAGE: +FullValueClasses
// WITH_STDLIB
// KT-90537

value class Complex(val real: Double, val imaginary: Double)
value class DoubleValue(val value: Double)
value class FloatValue(val value: Float)
value class NamedValue(val name: String, val value: Double)

fun box(): String {
    val failures = mutableListOf<String>()
    val missing = Double.NaN
    val flipped = -missing
    if (!missing.isNaN() || !flipped.isNaN() || missing.toRawBits() == flipped.toRawBits()) {
        failures += "Fail: Double NaNs must have different bit patterns"
    }

    if (Complex(flipped, 0.0) != Complex(missing, 0.0)) failures += "Fail: Complex equals"
    if (Complex(flipped, 0.0).hashCode() != Complex(missing, 0.0).hashCode()) failures += "Fail: Complex hashCode"
    if (Complex(0.0, 0.0) == Complex(-0.0, 0.0)) failures += "Fail: Complex signed zero"

    if (DoubleValue(flipped) != DoubleValue(missing)) failures += "Fail: full value class Double equals"
    if (DoubleValue(0.0) == DoubleValue(-0.0)) failures += "Fail: full value class Double signed zero"

    if (NamedValue("t", flipped) != NamedValue("t", missing)) failures += "Fail: mixed value class equals"
    if (NamedValue("t", flipped).hashCode() != NamedValue("t", missing).hashCode()) failures += "Fail: mixed value class hashCode"
    if (NamedValue("t", 0.0) == NamedValue("t", -0.0)) failures += "Fail: mixed value class signed zero"

    val floatMissing = Float.NaN
    val floatFlipped = -floatMissing
    if (!floatMissing.isNaN() || !floatFlipped.isNaN() || floatMissing.toRawBits() == floatFlipped.toRawBits()) {
        failures += "Fail: Float NaNs must have different bit patterns"
    }

    if (FloatValue(floatFlipped) != FloatValue(floatMissing)) failures += "Fail: full value class Float equals"
    if (FloatValue(floatFlipped).hashCode() != FloatValue(floatMissing).hashCode()) failures += "Fail: full value class Float hashCode"
    if (FloatValue(0.0f) == FloatValue(-0.0f)) failures += "Fail: full value class Float signed zero"

    return failures.joinToString("; ").ifEmpty { "OK" }
}
