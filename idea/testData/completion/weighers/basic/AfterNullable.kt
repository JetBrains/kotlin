fun String?.forNullableString(){}
fun Any?.forNullableAny(){}
fun String.forString(){}

fun foo(s: String?) {
    s.for<caret>
}

// ORDER: forNullableString
// ORDER: forNullableAny
// ORDER: forString
