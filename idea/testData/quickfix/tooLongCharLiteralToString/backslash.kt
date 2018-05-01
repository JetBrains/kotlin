// "Convert too long character literal to string" "false"
// ACTION: Introduce local variable
// ERROR: Illegal escape: ''\''

fun foo() {
    '\'<caret>
}