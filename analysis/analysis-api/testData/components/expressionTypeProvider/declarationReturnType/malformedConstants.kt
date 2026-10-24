// WITH_STDLIB
// Malformed literals are compiled to error expressions, so their declarations have error types
val leadingZeros = 01
val leadingZerosWithUnderscore = 0_1
val lowercaseLongSuffix = 1l
val lowercaseUnsignedLongSuffix = 1ul
val outOfLongRange = 9223372036854775808
val illegalUnderscore = 1_
val emptyExponent = 1e
val emptyFloatExponent = 1.0e+f
val emptyCharacter = ''
val tooManyCharacters = 'ab'
val illegalEscape = '\q'
val incompleteUnicodeEscape = '\u12'

fun leadingZerosFunction() = 01
fun lowercaseLongSuffixFunction() = 1l
fun emptyExponentFunction() = 1e
fun tooManyCharactersFunction() = 'ab'

val leadingZerosGetter get() = 01
val tooManyCharactersGetter get() = 'ab'

fun container() {
    val leadingZerosLocal = 01
    val tooManyCharactersLocal = 'ab'
    fun lowercaseLongSuffixLocal() = 1l
}

// Valid corner cases
val zero = 0
val hexZero = 0x0
val binary = 0b101
val intMaxValue = 2147483647
val intMaxValuePlusOne = 2147483648
val longMaxValue = 9223372036854775807
val uintMaxValue = 0xFFFF_FFFFu
val uintMaxValuePlusOne = 0x1_0000_0000u
val ulongMaxValue = 0xFFFF_FFFF_FFFF_FFFFu
val upperCaseFloatSuffix = 1F
val exponent = 1e10
val floatExponent = 1e10f
val floatOutOfRange = 1e100f
val doubleOutOfRange = 1e1000
val escapedCharacter = '\n'
val unicodeEscape = 'A'
