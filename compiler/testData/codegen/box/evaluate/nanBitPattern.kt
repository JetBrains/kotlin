const val floatNaNBits = -4194304
const val doubleNaNBits = -2251799813685248L

const val floatNaN = Float.NaN
const val doubleNaN = Double.NaN

const val floatPositiveDivisionNaN = 0.0f / 0.0f
const val floatNegativeDivisionNaN = -(0.0f / 0.0f)
const val doublePositiveDivisionNaN = 0.0 / 0.0
const val doubleNegativeDivisionNaN = -(0.0 / 0.0)

fun box(): String {
    if (floatNaN.toRawBits() != floatNaNBits) return "Fail Float.NaN ${floatNaN.toRawBits()} != ${floatNaNBits}"
    if (doubleNaN.toRawBits() != doubleNaNBits) return "Fail Double.NaN ${doubleNaN.toRawBits()} != ${doubleNaNBits}"

    if (floatPositiveDivisionNaN.toRawBits() != floatNaNBits) return "Fail floatPositiveDivisionNaN ${floatPositiveDivisionNaN.toRawBits()} != ${floatNaNBits}"
    if (floatNegativeDivisionNaN.toRawBits() != floatNaNBits) return "Fail floatNegativeDivisionNaN ${floatNegativeDivisionNaN.toRawBits()} != ${floatNaNBits}"
    if (doublePositiveDivisionNaN.toRawBits() != doubleNaNBits) return "Fail doublePositiveDivisionNaN ${doublePositiveDivisionNaN.toRawBits()} != ${doubleNaNBits}"
    if (doubleNegativeDivisionNaN.toRawBits() != doubleNaNBits) return "Fail doubleNegativeDivisionNaN ${doubleNegativeDivisionNaN.toRawBits()} != ${doubleNaNBits}"

    return "OK"
}
