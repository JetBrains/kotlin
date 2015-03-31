// "Create secondary constructor" "false"
// ERROR: Too many arguments for public constructor G() defined in G
// ACTION: Add parameter to constructor 'G'
// ACTION: Convert to block body
// ACTION: Create function 'G'

fun test() = G(<caret>1)