// "Add '<*, *>'" "false"
// "Add '<*>'" "false"
// ERROR: 2 type arguments expected
// ACTION: Disable 'Eliminate Argument of 'when''
// ACTION: Disable 'Replace 'when' with 'if''
// ACTION: Edit intention settings
// ACTION: Edit intention settings
// ACTION: Eliminate argument of 'when'
// ACTION: Replace 'when' with 'if'
public fun foo(a: Any) {
    when (a) {
        is <caret>Map<Int> -> {}
        else -> {}
    }
}