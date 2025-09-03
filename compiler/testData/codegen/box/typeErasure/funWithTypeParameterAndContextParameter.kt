// DUMP_IR_OF_PREPROCESSED_INLINE_FUNCTIONS
// WITH_STDLIB
// LANGUAGE: +ContextParameters
context(c: Int)
inline fun <T : CharSequence> foo(a: T) = a.length + c

context(c: Int)
fun <T : CharSequence> bar(a: T) = a.length + c

fun box(): String = with(42) {
    val arguments = listOf<String>("0123456789", "", "\n")
    for (arg in arguments)
        if (foo(arg) != bar(arg))
            return arg
    return "OK"
}