// "Make type parameter reified and function inline" "false"
// ACTION: Change type arguments to <*>
// ACTION: Convert to block body
// ACTION: Introduce local variable
// ERROR: Cannot check for instance of erased type: List<Int>
fun <T> test(a: List<Any>) = a is List<Int><caret>