// "Change return type of enclosing function 'foo' to 'String?'" "true"

fun foo(): String {
    return <caret>null
}