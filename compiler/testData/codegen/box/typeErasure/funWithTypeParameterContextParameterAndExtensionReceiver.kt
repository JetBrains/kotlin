// DUMP_IR_OF_PREPROCESSED_INLINE_FUNCTIONS
// WITH_STDLIB
// LANGUAGE: +ContextParameters
context(c: Int)
inline fun <T : CharSequence> Int.foo(a: T) = a.length + c + this

context(c: Int)
fun <T : CharSequence> Int.bar(a: T) = a.length + c + this

fun box(): String = with(42) {
    val arguments = listOf<String>("0123456789", "", "\n")
    for (arg in arguments)
        if (foo(arg) != bar(arg))
            return arg
    return "OK"
}