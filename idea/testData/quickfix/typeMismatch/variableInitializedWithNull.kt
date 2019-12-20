// "Change type of 'x' to 'String?'" "true"
fun foo(condition: Boolean) {
    var x = null
    if (condition) {
        x = "abc"<caret>
    }
}