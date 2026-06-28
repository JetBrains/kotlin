// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// WITH_STDLIB
// WITH_PLATFORM_LIBS
internal data class TerminalSettingsFloatValueImpl(
    private val rawIntValue: Int,
    private val digits: Int,
) {
    companion object {
        fun ofFloat(value: Float, digits: Int): TerminalSettingsFloatValueImpl =
            TerminalSettingsFloatValueImpl(rawIntValue = (value * multiplier(digits)).toInt(), digits = digits)

        fun parse(value: String, defaultValue: Float, digits: Int): TerminalSettingsFloatValueImpl =
            try {
                ofFloat(value.toFloat(), digits)
            }
            catch (_: Exception) {
                ofFloat(defaultValue, digits)
            }

        private fun multiplier(digits: Int): Float = 10f
    }

    private val multiplier: Float = multiplier(digits)

    private val actualDigits: Int
        get() {
            var actualDigits = digits
            var value = rawIntValue
            while (actualDigits > 1 && value % 10 == 0) {
                --actualDigits
                value /= 10
            }
            return actualDigits
        }

    fun coerceIn(range: ClosedFloatingPointRange<Float>): TerminalSettingsFloatValueImpl =
        ofFloat(toFloat().coerceIn(range), digits)

    fun toFloat(): Float = rawIntValue.toFloat() / multiplier
}

interface TerminalContrastRatio {
    val value: Float

    companion object {
        val MIN_VALUE: TerminalContrastRatio = ofFloat(1.0f)
        val MAX_VALUE: TerminalContrastRatio = ofFloat(21.0f)
        val DEFAULT_VALUE: TerminalContrastRatio = ofFloat(4.5f)

        fun ofFloat(value: Float): TerminalContrastRatio {
            val clampedValue = value.coerceIn(1.0f, 21.0f)
            return TerminalContrastRatioImpl(TerminalSettingsFloatValueImpl.ofFloat(clampedValue, 2))
        }
    }
}

internal data class TerminalContrastRatioImpl(private val impl: TerminalSettingsFloatValueImpl) : TerminalContrastRatio {
    override val value: Float
        get() = impl.toFloat()
}

/* GENERATED_FIR_TAGS: andExpression, assignment, classDeclaration, companionObject, comparisonExpression, data,
equalityExpression, functionDeclaration, getter, incrementDecrementExpression, integerLiteral, interfaceDeclaration,
localProperty, multiplicativeExpression, objectDeclaration, override, primaryConstructor, propertyDeclaration,
tryExpression, unnamedLocalVariable, whileLoop */
