// ERROR: Cannot perform refactoring.\nCannot find a single definition to inline
fun foo() {
    var x = 1
    val t = <caret>x + 1
    x++
}