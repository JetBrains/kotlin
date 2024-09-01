// LANGUAGE: +WhenGuards
// DIAGNOSTICS: -SENSELESS_COMPARISON, -USELESS_IS_CHECK, -USELESS_CAST, -DUPLICATE_LABEL_IN_WHEN

sealed class BooleanHolder(val value: Boolean)
object True : BooleanHolder(true)
object False : BooleanHolder(false)

fun MultipleConditionsWithGuards(x: Any) {
    return when (x) {
        is String -> Unit
        is True, is False <!UNSUPPORTED_FEATURE!>if x.value<!> -> Unit
        else -> Unit
    }
}

fun MultipleConditionsWithNoGuards(x: Any) {
    return when (x) {
        is String -> Unit
        is True, is False -> Unit
        is True <!UNSUPPORTED_FEATURE!>if x.value<!> -> Unit
        else -> Unit
    }
}

interface I
interface I2

fun OtherCommaOccurances(x: Any) {
    return when(x) {
        object : I, I2 {} <!UNSUPPORTED_FEATURE!>if true<!> -> Unit
        ',' <!UNSUPPORTED_FEATURE!>if true<!> -> Unit
        else -> Unit
    }
}

class Ambiguous1(val value: Int)
class Ambiguous2(val value: Int)

fun MatchingTypes(x: Any) {
    return <!NO_ELSE_IN_WHEN!>when<!>(x) {
        is Ambiguous1, is Ambiguous2 <!UNSUPPORTED_FEATURE!>if x.value > 0<!> -> Unit
        is Ambiguous1, is Ambiguous2 <!UNSUPPORTED_FEATURE!>if true<!> -> Unit
    }
}
