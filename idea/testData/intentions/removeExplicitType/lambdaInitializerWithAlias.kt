// IS_APPLICABLE: false

typealias ValidationRule2 = (String, String) -> String
typealias ValidationRule3 = ValidationRule2
typealias ValidationRule4 = ValidationRule3

fun getValidator(): ValidationRule4<caret> = { text, eventType ->
    "abc"
}