// IS_APPLICABLE: false
fun foo(): Any {
    return { x: String<caret> -> 42 }
}