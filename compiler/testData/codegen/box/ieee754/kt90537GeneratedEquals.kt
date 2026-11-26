// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// KT-90537

data class Reading(val sensor: String, val value: Double)
data class FloatReading(val sensor: String, val value: Float)

OPTIONAL_JVM_INLINE_ANNOTATION
value class Celsius(val value: Double)

OPTIONAL_JVM_INLINE_ANNOTATION
value class FloatValue(val value: Float)

fun box(): String {
    val failures = mutableListOf<String>()
    val missing = Double.NaN
    val flipped = -missing
    if (!missing.isNaN() || !flipped.isNaN() || missing.toRawBits() == flipped.toRawBits()) {
        failures += "Fail: Double NaNs must have different bit patterns"
    }

    val reading = Reading("t", missing)
    val flippedReading = Reading("t", flipped)
    if (flippedReading != reading) failures += "Fail: data class Double equals"
    if (setOf(flippedReading, reading).size != 1) failures += "Fail: data class Double set"
    if (!listOf(flippedReading).contains(reading)) failures += "Fail: data class Double list contains"
    if (flippedReading.hashCode() != reading.hashCode()) failures += "Fail: data class Double hashCode"
    if (!flipped.equals(missing)) failures += "Fail: Double.equals control"
    if (Reading("t", 0.0) == Reading("t", -0.0)) failures += "Fail: data class Double signed zero"

    val floatMissing = Float.NaN
    val floatFlipped = -floatMissing
    if (!floatMissing.isNaN() || !floatFlipped.isNaN() || floatMissing.toRawBits() == floatFlipped.toRawBits()) {
        failures += "Fail: Float NaNs must have different bit patterns"
    }

    val floatReading = FloatReading("t", floatMissing)
    val flippedFloatReading = FloatReading("t", floatFlipped)
    if (flippedFloatReading != floatReading) failures += "Fail: data class Float equals"
    if (flippedFloatReading.hashCode() != floatReading.hashCode()) failures += "Fail: data class Float hashCode"
    if (FloatReading("t", 0.0f) == FloatReading("t", -0.0f)) failures += "Fail: data class Float signed zero"

    if (Celsius(flipped) != Celsius(missing)) failures += "Fail: value class Double equals"
    if (Celsius(flipped).hashCode() != Celsius(missing).hashCode()) failures += "Fail: value class Double hashCode"
    if (Celsius(0.0) == Celsius(-0.0)) failures += "Fail: value class Double signed zero"

    if (FloatValue(floatFlipped) != FloatValue(floatMissing)) failures += "Fail: value class Float equals"
    if (FloatValue(floatFlipped).hashCode() != FloatValue(floatMissing).hashCode()) failures += "Fail: value class Float hashCode"
    if (FloatValue(0.0f) == FloatValue(-0.0f)) failures += "Fail: value class Float signed zero"

    return failures.joinToString("; ").ifEmpty { "OK" }
}
