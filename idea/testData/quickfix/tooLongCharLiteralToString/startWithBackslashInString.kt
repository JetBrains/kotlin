// "Convert too long character literal to string" "false"
// ACTION: Introduce local variable
// ACTION: To raw string literal
// ACTION: Convert to 'buildString' call
// ERROR: Illegal escape: '\ '

fun foo() {
    "\ <caret>"
}