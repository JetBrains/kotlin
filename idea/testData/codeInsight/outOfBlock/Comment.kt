// OUT_OF_CODE_BLOCK: FALSE
// TYPE: '//'
// ERROR: A 'return' expression required in a function with a block body ('{...}')

fun comment(): String {
    <caret>return ""
}